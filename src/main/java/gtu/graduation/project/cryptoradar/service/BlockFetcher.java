package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.config.ProcessorConfig;
import gtu.graduation.project.cryptoradar.mapper.Mapper;
import gtu.graduation.project.cryptoradar.model.Block;
import gtu.graduation.project.cryptoradar.model.LogFilter;
import gtu.graduation.project.cryptoradar.repository.BlockCheckpointRepository;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.TxRepository;
import lombok.RequiredArgsConstructor;
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
public class BlockFetcher {

    private final BlockRepository blockRepo;
    private final TxRepository txRepo;
    private final BlockCheckpointRepository blockCheckpointRepository;
    private final ProcessorConfig config;
    private final Web3j web3j;
    private final Mapper<Block.Log, Log> logMapper;
    private final Mapper<Block.Transaction, EthBlock.TransactionObject> transactionMapper;


    public List<Block> fetch(BigInteger start, BigInteger end, LogFilter filter) throws IOException {
        EthFilter ethFilter = convertFilter(filter);
        BatchRequest request = createRequest(ethFilter, start, end);
        BatchResponse response = request.send();
        return processBatchResponse(response);
    }

    public List<Block> fetch(BigInteger start, BigInteger end) throws IOException {
        EthFilter ethFilter = createFilter(start, end);
        BatchRequest request = createRequest(ethFilter, start, end);
        BatchResponse response = request.send();
        return processBatchResponse(response);
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

    private List<Block> processBatchResponse(BatchResponse response) {
        List<Response<?>> responses = new LinkedList<>(response.getResponses());
        EthLog ethLog = (EthLog) responses.removeFirst();
        Map<BigInteger, List<Block.Log>> logsByBlock = processLogsResponse(ethLog);

        List<Block> blocks = new ArrayList<>();
        for (Response<?> blockResponse : responses) {
            EthBlock.Block block = ((EthBlock) blockResponse).getBlock();
            Block.Info info = getInfo(block);

            // Build transactions map
            Map<String, Block.Transaction> transactions = getTransactions(block).stream().collect(Collectors.toMap(Block.Transaction::hash, // assuming your Transaction record has a method 'hash()'
                    tx -> tx));

            // Build logs map grouped by transaction hash
            Map<String, List<Block.Log>> logsByTx = logsByBlock.getOrDefault(block.getNumber(), Collections.emptyList()).stream().collect(Collectors.groupingBy(Block.Log::transactionHash)); // assuming your Log record has 'transactionHash()'

            blocks.add(new Block(info, transactions, logsByTx));
        }

        return blocks;

    }

    private Block.Info getInfo(EthBlock.Block block) {
        return new Block.Info(block.getNumber(), Instant.ofEpochSecond(block.getTimestamp().longValue()), block.getBaseFeePerGas());
    }

    private List<Block.Transaction> getTransactions(EthBlock.Block block) {
        return block.getTransactions().stream().map((tx -> transactionMapper.map((EthBlock.TransactionObject) tx.get()))).toList();
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
