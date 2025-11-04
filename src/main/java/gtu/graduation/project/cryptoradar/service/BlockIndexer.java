package gtu.graduation.project.cryptoradar.service;


import com.google.common.util.concurrent.RateLimiter;
import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockIndexer {

    private final ProcessorConfig processorConfig;
    private final BlockFetcher blockFetcher;
    private final BlockProcessor blockProcessor;
    private final BlockStatusRepository blockStatusRepository;
    private final Web3j web3j;
    private final RateLimiter rateLimiter;

    public void run() {
        Long startBlock = getStartBlock();
        Long endBlock = getLatestBlockNumber().orElseThrow(() -> new RuntimeException("Could not fetch latest block number"));
        Integer batchSize = processorConfig.getBatchSize(); // e.g., 100 blocks

        // Use thread pool for parallel processing
        Integer parallelism = processorConfig.getParallelism(); // Process 5 batches concurrently
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        log.info("Processing blocks from {} to {} with batch size {}", startBlock, endBlock, batchSize);

        for (Long currentStart = startBlock; currentStart < endBlock; currentStart += batchSize) {
            Long batchStart = currentStart;
            Long batchEnd = Math.min(currentStart + batchSize - 1, endBlock);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processBatchWithRetry(batchStart, batchEnd, 3);
            }, executor);

            futures.add(future);
        }

        // Wait for all batches to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Successfully processed all blocks from {} to {}", startBlock, endBlock);
        } catch (Exception e) {
            log.error("Error during batch processing", e);
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processBatchWithRetry(Long startBlock, Long endBlock, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                // Acquire rate limit permit
                rateLimiter.acquire();

                // Fetch and process
                List<Block> blocks = blockFetcher.fetch(BigInteger.valueOf(startBlock), BigInteger.valueOf(endBlock));

                blockProcessor.process(blocks);

                log.info("Successfully processed blocks {} to {}", startBlock, endBlock);
                return; // Success!

            } catch (IOException e) {
                lastException = e;
                attempt++;
                log.warn("Attempt {}/{} failed for blocks {}-{}: {}", attempt, maxRetries, startBlock, endBlock, e.getMessage());

                if (attempt < maxRetries) {
                    // Exponential backoff
                    try {
                        Thread.sleep((long) Math.pow(2, attempt) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error processing blocks {}-{}", startBlock, endBlock, e);
                throw new RuntimeException("Failed to process batch", e);
            }
        }

        throw new RuntimeException(String.format("Failed to process blocks %d-%d after %d attempts", startBlock, endBlock, maxRetries), lastException);
    }

    private Long getStartBlock() {
        // Resume from last processed block or start from config
        return blockStatusRepository.findTopByNetworkTypeOrderByBlockNumberDesc(NetworkType.MAINNET).map(status -> status.getBlockNumber() + 1).orElse(processorConfig.getStartBlock());
    }

    private Optional<Long> getLatestBlockNumber() {
        try {
            return Optional.of(web3j.ethBlockNumber().send().getBlockNumber().longValue());
        } catch (IOException e) {
            log.error("Failed to fetch latest block number", e);
            return Optional.empty();
        }
    }
}