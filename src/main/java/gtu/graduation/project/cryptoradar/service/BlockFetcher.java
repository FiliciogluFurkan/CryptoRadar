package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.entity.BlockStatus;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.mapper.Mapper;
import gtu.graduation.project.cryptoradar.model.*;
import gtu.graduation.project.cryptoradar.repository.BlockCheckpointRepository;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.BlockStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.*;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockFetcher {

    private final BlockRepository blockRepo;
    private final BlockCheckpointRepository blockCheckpointRepository;
    private final ProcessorConfig config;
    private final Web3j web3j;
    private final BlockStatusRepository blockStatusRepository;
    private final Mapper<Block.Log, Log> logMapper;
    private final Mapper<Block.Transaction, EthBlock.TransactionObject> transactionMapper;


    public List<Block> fetch(BigInteger start, BigInteger end, LogFilter filter) throws IOException {
        EthFilter ethFilter = convertFilter(filter);
        BatchRequest request = createRequest(ethFilter, start, end);
        BatchResponse response = request.send();
        return processBatchResponse(start.longValue(), end.longValue(), response);
    }

    public List<Block> fetch(BigInteger start, BigInteger end) throws IOException {
        EthFilter ethFilter = createFilter(start, end);
        BatchRequest request = createRequest(ethFilter, start, end);
        BatchResponse response = request.send();
        return processBatchResponse(start.longValue(), end.longValue(), response);
    }

    private EthFilter convertFilter(LogFilter filter) {
        EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(filter.start())), DefaultBlockParameter.valueOf(BigInteger.valueOf(filter.end())), filter.addresses());
        return ethFilter.addSingleTopic(config.getTransferTopic());
    }

    private EthFilter createFilter(BigInteger start, BigInteger end) {
        EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(start), DefaultBlockParameter.valueOf(end), (List<String>) null);
        return filter.addSingleTopic(config.getTransferTopic());
    }

    private Map<BigInteger, List<Block.Log>> processLogsResponse(EthLog ethLog) {
        return Optional.ofNullable(ethLog.getLogs()).orElse(Collections.emptyList()).stream().collect(Collectors.groupingBy(logResult -> ((Log) logResult.get()).getBlockNumber(), Collectors.mapping(logResult -> logMapper.map((Log) logResult.get()), Collectors.toList())));
    }

    private List<Block> processBatchResponse(Long startBlock, Long endBlock, BatchResponse response) {
        List<Response<?>> responses = new LinkedList<>(response.getResponses());

        // Check if first response (eth_getLogs) has error
        Response<?> logResponse = responses.removeFirst();
        if (logResponse.hasError()) {
            return Collections.emptyList();
        }

        EthLog ethLog = (EthLog) logResponse;
        Map<BigInteger, List<Block.Log>> logsByBlock = processLogsResponse(ethLog);

        List<Block> blocks = new ArrayList<>();
        List<BlockStatusEntity> failed = new ArrayList<>();

        long i = startBlock;
        for (Response<?> blockResponse : responses) {
            // Check for errors in individual block requests
            if (blockResponse.hasError()) {
                Response.Error error = blockResponse.getError();
                log.warn("Failed to fetch block: {} - {}", error.getCode(), error.getMessage());

                failed.add(new BlockStatusEntity(i, NetworkType.MAINNET, BlockStatus.FAILED));
                continue;
            }

            EthBlock ethBlock = (EthBlock) blockResponse;
            EthBlock.Block block = ethBlock.getBlock();

            if (block == null) {
                log.warn("Received null block in response");
                continue;
            }

            Block.Info info = getInfo(block);

            // Build transactions map
            Map<String, Block.Transaction> transactions = getTransactions(block).stream()
                    .collect(Collectors.toMap(Block.Transaction::hash, tx -> tx));

            // Build logs map grouped by transaction hash
            Map<String, List<Block.Log>> logsByTx = logsByBlock
                    .getOrDefault(block.getNumber(), Collections.emptyList())
                    .stream()
                    .collect(Collectors.groupingBy(Block.Log::transactionHash));

            blocks.add(new Block(info, transactions, logsByTx));
            i++;
        }

        blockStatusRepository.saveAll(failed);
        return blocks;
    }

    private Block.Info getInfo(EthBlock.Block block) {
        return new Block.Info(block.getNumber().longValue(), block.getHash(), Instant.ofEpochSecond(block.getTimestamp().longValue()), block.getBaseFeePerGas());
    }

    private List<Block.Transaction> getTransactions(EthBlock.Block block) {
        Optional<EthBlock.TransactionResult> tx1 =  block.getTransactions().stream().filter(transactionResult -> ((EthBlock.TransactionObject) transactionResult.get()).getHash().equals("0x010ddc53a34a0ff66ea43c1e5715383e5bbc9afa5aca056bda2da185b2841f40")).findAny();
        if(tx1.isPresent()) {
            EthBlock.TransactionObject tx2 = (EthBlock.TransactionObject) tx1.get();
            log.info("Transaction: {}, {}", tx2.getValue(), tx2.getTo());
        }
        return block.getTransactions().stream()
                .filter(tx -> ((EthBlock.TransactionObject) tx.get()).getTo() != null)
                .map((tx -> transactionMapper.map((EthBlock.TransactionObject) tx.get())))
                .toList();
    }

    public BatchRequest createRequest(EthFilter logFilter, BigInteger start, BigInteger end) {
        BatchRequest request = web3j.newBatch();
        Request<?, EthLog> logsRequest = web3j.ethGetLogs(logFilter);
        request.add(logsRequest);
        LongStream.rangeClosed(start.longValue(), end.longValue()).forEach(i -> request.add(createBlockRequest(web3j, i)));
        return request;
    }

    public BatchRequest createRequest(BigInteger start, BigInteger end) {
        BatchRequest request = web3j.newBatch();
        LongStream.rangeClosed(start.longValue(), end.longValue()).forEach(i -> request.add(createBlockRequest(web3j, i)));
        return request;
    }

    private Request<?, ? extends Response<?>> createBlockRequest(Web3j web3j, Long blockNumber) {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), true);
    }
}
