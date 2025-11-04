package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.config.TokenConfiguration;
import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.entity.BlockStatus;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionEntity;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        List<TransactionEntity> transactionEntities = new ArrayList<>();
        BlockEntity blockEntity = new BlockEntity();
        for (Map.Entry<String, Block.Transaction> entry : block.transactions().entrySet()) {
            String transactionHash = entry.getKey();
            Block.Transaction transaction = entry.getValue();
            List<Block.Log> transactionLogs = block.logs().getOrDefault(transactionHash, List.of());
            Token token = determineToken(transaction.to());
            TransactionEntity txEntity = createTx(blockEntity, transaction, transactionLogs, token, block.info().baseFeePerGas());
            txEntity.calculateEffectiveFee(block.info().baseFeePerGas());
            transactionEntities.add(txEntity);
        }

        blockEntity.setTimestamp(block.info().timestamp());
        blockEntity.setBlockNumber(block.info().blockNumber());
        blockEntity.setBlockHash(block.info().hash());
        blockEntity.setBaseFeePerGas(block.info().baseFeePerGas());
        blockEntity.setTransactions(transactionEntities);

        blockEntity.calculateGasStatistics();

        blockRepository.save(blockEntity);
        blockStatusRepository.save(new BlockStatusEntity( block.info().blockNumber(), networkType, BlockStatus.PROCESSED));
    }

    private TransactionEntity createTx(BlockEntity blockRef, Block.Transaction tx, List<Block.Log> logs, Token token, BigInteger baseFeePerGas) {
        if (token.equals(Token.NATIVE)) {
            return new TransactionEntity(blockRef, tx.hash(), tx.nonce(), tx.from(), tx.to(), tx.value(), tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.fromToken(token), tx.type(), baseFeePerGas);
        } else {
            // For ERC20 tokens, extract transfer information from logs
            // ERC20 Transfer event signature: Transfer(address,address,uint256)
            // Topic0: 0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef

            BigInteger tokenValue = BigInteger.ZERO;
            String actualSender = tx.from(); // default to transaction sender
            String actualRecipient = tx.to(); // default to contract address

            // Find the Transfer event log for this token
            for (Block.Log log : logs) {

                actualSender = "0x" + log.topics().get(1).substring(26); // Remove padding from address
                actualRecipient = "0x" + log.topics().get(2).substring(26); // Remove padding from address

                // Parse the transfer amount from log data
                if (log.data() != null && !log.data().equals("0x")) {
                    tokenValue = new BigInteger(log.data().substring(2), 16);
                }
                break;

            }

            return new TransactionEntity(blockRef, tx.hash(), tx.nonce(), actualSender, // The actual token sender from logs
                    actualRecipient, // The actual token recipient from logs
                    tokenValue, // The token amount transferred
                    tx.gasPrice(), tx.gas(), // gasLimit
                    tx.maxFeePerGas(), tx.maxPriorityFeePerGas(), TokenType.fromToken(token), tx.type(), baseFeePerGas);
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
