package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import gtu.graduation.project.cryptoradar.model.Token;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BlockProcessor {

    private final ProcessorConfig config;
    private final BlockStatusRepository blockStatusRepository;

    private final NetworkType networkType = NetworkType.MAINNET;

    @Async
    public void process(List<Block> blocks) {
        for(Block block : blocks) {
            processBlock(block);
        }
    }

    public void processBlock(Block block) {
        if(blockStatusRepository.findByNetworkTypeAndBlockNumber(networkType, block.info().blockNumber()).isPresent()) {
            return;
        }
        Set<String> txHashes = block.logs().stream().map(Block.Log::transactionHash).collect(Collectors.toSet());
        for(Block.Transaction transaction : block.transactions()) {
            if(txHashes.contains(transaction.to())) {
                createNewTransaction(transaction, block.logs().)
            }
        }



    }

}
