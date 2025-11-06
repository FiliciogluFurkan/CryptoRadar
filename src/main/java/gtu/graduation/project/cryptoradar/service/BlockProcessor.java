package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.config.TokenConfiguration;
import gtu.graduation.project.cryptoradar.entity.*;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import gtu.graduation.project.cryptoradar.model.Token;
import gtu.graduation.project.cryptoradar.model.TokenType;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import gtu.graduation.project.cryptoradar.repository.ERC20TransactionRepository;
import gtu.graduation.project.cryptoradar.repository.NativeTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class BlockProcessor {

    private final ProcessorConfig config;
    private final BlockStatusRepository blockStatusRepository;
    private final TokenConfiguration tokenConfiguration;
    private final BlockRepository blockRepository;
    private final ERC20TransactionRepository erc20TransactionRepository;
    private final NativeTransactionRepository nativeTransactionRepository;

    private final NetworkType networkType = NetworkType.MAINNET;

    @Async
    public void process(List<Block> blocks) {
        for (Block block : blocks) {
            process(block);
        }
    }

    /**
     * Process a Block record and convert it to BlockEntity with transactions
     * Filters logs to only include ERC20 transfer events
     */
    @Transactional
    public void process(Block block) {
        if (blockStatusRepository.findByNetworkTypeAndBlockNumber(networkType, block.info().blockNumber()).isPresent()) {
            return;
        }
        List<TransactionNativeTransferEntity> transactionEntities = new ArrayList<>();

        BlockEntity blockEntity = new BlockEntity();
        blockEntity.setBaseFeePerGas(block.info().baseFeePerGas());
        blockEntity.setTimestamp(block.info().timestamp());
        blockEntity.setBlockNumber(block.info().blockNumber());
        blockEntity.setBlockHash(block.info().hash());

        try {
            for (Map.Entry<String, Block.Transaction> entry : block.transactions().entrySet()) {
                String transactionHash = entry.getKey();
                Block.Transaction transaction = entry.getValue();
                List<Block.Log> transactionLogs = block.logs().getOrDefault(transactionHash, List.of());
                TransactionNativeTransferEntity txEntity = createTx(blockEntity, transaction, transactionLogs, block.info().baseFeePerGas());
                txEntity.calculateEffectiveFee(block.info().baseFeePerGas());
                transactionEntities.add(txEntity);
            }

            blockEntity.setTransactions(transactionEntities);

            blockEntity.calculateGasStatistics();

            calculateZScores(blockEntity, transactionEntities);

            blockRepository.save(blockEntity);
            nativeTransactionRepository.saveAll(blockEntity.getTransactions());
            List<TransactionERC20TransferEntity> allErc20Transfers = blockEntity.getTransactions().stream()
                    .flatMap(transaction -> transaction.getTransactions().stream())
                    .collect(Collectors.toList());
            erc20TransactionRepository.saveAll(allErc20Transfers);
            blockStatusRepository.save(new BlockStatusEntity( block.info().blockNumber(), networkType, BlockStatus.PROCESSED));
        } catch (Exception e) {
            log.error("An error occurred during processing block: {}, error: {}", block.info().blockNumber(), e.getMessage(), e);
            blockStatusRepository.save(new BlockStatusEntity(block.info().blockNumber(), networkType, BlockStatus.FAILED));
        }

    }

    private void calculateZScores(BlockEntity block, List<TransactionNativeTransferEntity> transactions) {
        for(TransactionNativeTransferEntity transaction : transactions) {
            if(!transaction.isContractInteraction()) {
                BigDecimal valueDecimal = new BigDecimal(transaction.getValue());
                BigDecimal valueZScore = valueDecimal.subtract(block.getAverageValue())
                        .divide(block.getValueStandardDeviation(), RoundingMode.HALF_UP);
                transaction.setValueZScore(valueZScore.doubleValue());
            }

            BigDecimal gasZscore = new BigDecimal(transaction.getEffectiveFeePerGas())
                    .subtract(block.getAvgGasPrice())       // you already store avgGasPrice
                    .divide(block.getGasPriceStandardDeviation(), RoundingMode.HALF_UP);
            transaction.setGasZScore(gasZscore.doubleValue());
        }

        List<Long> priorityFees = transactions.stream()
                .map(TransactionNativeTransferEntity::getMaxPriorityFeePerGas)
                .sorted()
                .toList();

        int size = priorityFees.size();

        for (TransactionNativeTransferEntity tx : transactions) {
            int rank = Collections.binarySearch(priorityFees, tx.getMaxPriorityFeePerGas());
            if (rank < 0) rank = -rank - 1; // standard fix if no exact match

            // percentile formula
            BigDecimal percentile = new BigDecimal(rank)
                    .divide(new BigDecimal(size - 1), 5, RoundingMode.HALF_UP);

            tx.setPriorityFeePercentile(percentile.doubleValue());
        }
    }

    private TransactionNativeTransferEntity createTx(BlockEntity blockRef, Block.Transaction tx, List<Block.Log> logs, BigInteger baseFeePerGas) {
        if (logs.isEmpty()) {
            return new TransactionNativeTransferEntity(blockRef, tx.hash(), tx.nonce(), tx.from(), tx.to(), tx.value(), tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.ETH, tx.type(), baseFeePerGas, false);
        } else {
            // For ERC20 tokens, extract transfer information from logs
            // ERC20 Transfer event signature: Transfer(address,address,uint256)
            // Topic0: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
            TransactionNativeTransferEntity transaction = new TransactionNativeTransferEntity(blockRef, tx.hash(), tx.nonce(), tx.from(), tx.to(), tx.value(), tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.ETH, tx.type(), baseFeePerGas, false);

            // Find the Transfer event log for this token
            for (Block.Log log : logs) {

                if(log.topics().size() != 3) {
                    continue;
                }

                String actualSender = "0x" + log.topics().get(1).substring(26); // Remove padding from address
                String actualRecipient = "0x" + log.topics().get(2).substring(26); // Remove padding from address
                String contractAddress = log.address();

                BigInteger tokenValue = BigInteger.ZERO;
                if (log.data() != null && !log.data().equals("0x")) {
                    tokenValue = new BigInteger(log.data().substring(2), 16);
                }

                transaction.getTransactions().add(TransactionERC20TransferEntity.builder()
                        .id(UUID.randomUUID())
                        .block(blockRef)
                        .token(TokenType.fromToken(determineToken(contractAddress)))
                        .fromAddress(actualSender)
                        .toAddress(actualRecipient)
                        .transaction(transaction)
                        .type(tx.type())
                        .value(tokenValue)
                        .build());
            }
            return transaction;
        }
    }


    /**
     * Determine the token type based on ERC20 transfer logs
     * This is a placeholder - implement your logic to identify specific tokens
     * based on contract addresses in the logs
     */
    private Token determineToken(String to) {
        return tokenConfiguration.get(to);
    }


}
