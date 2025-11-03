package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.config.TokenConfiguration;
import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionEntity;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import gtu.graduation.project.cryptoradar.model.Token;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BlockProcessor {

    private final ProcessorConfig config;
    private final BlockStatusRepository blockStatusRepository;
    private final TokenConfiguration tokenConfiguration;

    private final NetworkType networkType = NetworkType.MAINNET;

    @Async
    public void process(List<Block> blocks) {
        for(Block block : blocks) {
            process(block);
        }
    }

    /**
     * Process a Block record and convert it to BlockEntity with transactions
     * Filters logs to only include ERC20 transfer events
     */
    public void process(Block block) {
        if (blockStatusRepository.findByNetworkTypeAndBlockNumber(networkType, block.info().blockNumber()).isPresent()) {
            return;
        }
        List<TransactionEntity> transactionEntities = new ArrayList<>();
        for (Map.Entry<String, Block.Transaction> entry : block.transactions().entrySet()) {
            String transactionHash = entry.getKey();
            Block.Transaction transaction = entry.getValue();
            List<Block.Log> transactionLogs = block.logs().getOrDefault(transactionHash, List.of());
            Token token = determineToken(transaction.to());
            TransactionEntity txEntity = createTx(transaction, transactionLogs, token, block.info().baseFeePerGas());
            transactionEntities.add(txEntity);
        }


        // Create block entity
        BlockEntity blockEntity = new BlockEntity(
                block.info().blockNumber(),
                null, // blockHash not in info, would need to be added
                block.info().timestamp(),
                block.info().baseFeePerGas(),
                transactionEntities
        );

        return blockEntity;
    }

    private TransactionEntity createTx(Block.Transaction tx, List<Block.Log> logs, Token token, BigInteger baseFeePerGas) {
        if(token.equals(Token.NATIVE)) {
            return new TransactionEntity(
                    UUID.randomUUID(),
                    tx.blockNumber(),
                    tx.hash(),
                    tx.nonce(),
                    tx.from(),
                    tx.to(),
                    tx.value(),
                    tx.gasPrice(),
                    tx.gas(), // gasLimit
                    null, // gasUsed - needs to be set from receipt if available
                    tx.maxFeePerGas(),
                    tx.maxPriorityFeePerGas(),
                    token,
                    tx.type(),
                    baseFeePerGas
            );
        } else {
            // create tx which token type is another and value is the token transferred like 500 is 500 usdt is transferred
        }
        return null;
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
