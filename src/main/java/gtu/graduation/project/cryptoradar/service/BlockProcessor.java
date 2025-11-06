package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.config.TokenConfiguration;
import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.entity.BlockStatus;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionTransferEntity;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import gtu.graduation.project.cryptoradar.model.Token;
import gtu.graduation.project.cryptoradar.model.TokenType;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class BlockProcessor {

    private final ProcessorConfig config;
    private final BlockStatusRepository blockStatusRepository;
    private final TokenConfiguration tokenConfiguration;
    private final BlockRepository blockRepository;

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
        List<TransactionTransferEntity> transactionEntities = new ArrayList<>();

        BlockEntity blockEntity = new BlockEntity();
        blockEntity.setBaseFeePerGas(block.info().baseFeePerGas());
        blockEntity.setTimestamp(block.info().timestamp());
        blockEntity.setBlockNumber(block.info().blockNumber());
        blockEntity.setBlockHash(block.info().hash());

        for (Map.Entry<String, Block.Transaction> entry : block.transactions().entrySet()) {
            String transactionHash = entry.getKey();
            Block.Transaction transaction = entry.getValue();
            List<Block.Log> transactionLogs = block.logs().getOrDefault(transactionHash, List.of());
            TransactionTransferEntity txEntity = createTx(blockEntity, transaction, transactionLogs, block.info().baseFeePerGas());
            txEntity.calculateEffectiveFee(block.info().baseFeePerGas());
            transactionEntities.add(txEntity);
        }

        blockEntity.setTransactions(transactionEntities);

        blockEntity.calculateGasStatistics();

        calculateZScores(blockEntity, transactionEntities);

        blockRepository.save(blockEntity);
        blockStatusRepository.save(new BlockStatusEntity( block.info().blockNumber(), networkType, BlockStatus.PROCESSED));
    }

    private void calculateZScores(BlockEntity block, List<TransactionTransferEntity> transactions) {
        for(TransactionTransferEntity transaction : transactions) {
            if(!transaction.isContractInteraction()) {
                BigDecimal valueDecimal = new BigDecimal(transaction.getValue());
                BigDecimal valueZScore = valueDecimal.subtract(block.getAverageValue())
                        .divide(block.getValueStandardDeviation(), RoundingMode.HALF_UP);
                transaction.setValueZScore(valueZScore);
            }

            BigDecimal gasZscore = new BigDecimal(transaction.getEffectiveFeePerGas())
                    .subtract(block.getAvgGasPrice())       // you already store avgGasPrice
                    .divide(block.getGasPriceStandardDeviation(), RoundingMode.HALF_UP);
            transaction.setGasZScore(gasZscore);
        }

        List<BigInteger> priorityFees = transactions.stream()
                .map(TransactionTransferEntity::getMaxPriorityFeePerGas)
                .sorted()
                .toList();

        int size = priorityFees.size();

        for (TransactionTransferEntity tx : transactions) {
            int rank = Collections.binarySearch(priorityFees, tx.getMaxPriorityFeePerGas());
            if (rank < 0) rank = -rank - 1; // standard fix if no exact match

            // percentile formula
            BigDecimal percentile = new BigDecimal(rank)
                    .divide(new BigDecimal(size - 1), 5, RoundingMode.HALF_UP);

            tx.setPriorityFeePercentile(percentile);
        }
    }

    private List<TransactionTransferEntity> createTx(BlockEntity blockRef, Block.Transaction tx, List<Block.Log> logs, BigInteger baseFeePerGas) {
        if (logs.isEmpty()) {
            return List.of(new TransactionTransferEntity(blockRef, tx.hash(), tx.nonce(), tx.from(), tx.to(), tx.value(), tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.ETH, tx.type(), baseFeePerGas, false, null));
        } else {
            // For ERC20 tokens, extract transfer information from logs
            // ERC20 Transfer event signature: Transfer(address,address,uint256)
            // Topic0: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
            List<TransactionTransferEntity> txs = new ArrayList<>();

            BigInteger tokenValue = BigInteger.ZERO;
            String actualSender = tx.from(); // default to transaction sender
            String actualRecipient = tx.to(); // default to contract address
            String contractAddress = null;

            // Find the Transfer event log for this token
            for (Block.Log log : logs) {

                if(log.topics().size() != 3) {
                    continue;
                }

                actualSender = "0x" + log.topics().get(1).substring(26); // Remove padding from address
                actualRecipient = "0x" + log.topics().get(2).substring(26); // Remove padding from address

                contractAddress = log.address();

                // Parse the transfer amount from log data
                if (log.data() != null && !log.data().equals("0x")) {
                    tokenValue = new BigInteger(log.data().substring(2), 16);
                }

                TransactionTransferEntity entity = new TransactionTransferEntity(blockRef, tx.hash(), tx.nonce(), actualSender, // The actual token sender from logs
                        actualRecipient, // The actual token recipient from logs
                        tx.value(),// The token amount transferred
                        tx.gasPrice(), tx.gas(), // gasLimit
                        tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.fromToken(determineToken(contractAddress)), tx.type(), baseFeePerGas, true, tokenValue);


            }

            return new TransactionTransferEntity(blockRef, tx.hash(), tx.nonce(), actualSender, // The actual token sender from logs
                    actualRecipient, // The actual token recipient from logs
                    tx.value(),// The token amount transferred
                    tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.fromToken(determineToken(contractAddress)), tx.type(), baseFeePerGas, true, tokenValue);
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
