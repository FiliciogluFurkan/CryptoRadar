package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.entity.Checkpoint;
import gtu.graduation.project.cryptoradar.entity.TxEntity;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.CheckpointRepository;
import gtu.graduation.project.cryptoradar.repository.TxRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.time.Instant;

@Service
public class EthereumBlockProcessor implements Runnable {

    private final BlockRepository blockRepo;
    private final TxRepository txRepo;
    private final CheckpointRepository checkpointRepo;
    private final Web3j web3j;
    private static final String CHECKPOINT_ID = "ETH_BLOCK_INDEX";

    public EthereumBlockProcessor(BlockRepository blockRepo,
                                  TxRepository txRepo,
                                  CheckpointRepository checkpointRepo,
                                  @Value("${ethereum.node-url}") String nodeUrl) {
        this.blockRepo = blockRepo;
        this.txRepo = txRepo;
        this.checkpointRepo = checkpointRepo;
        this.web3j = Web3j.build(new HttpService(nodeUrl));
    }

    @PostConstruct
    public void start() {
        checkpointRepo.deleteById(CHECKPOINT_ID);
        Thread t = new Thread(this, "eth-sync-thread");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        try {

            Checkpoint cp = checkpointRepo.findById(CHECKPOINT_ID)
                    .orElseGet(() -> {
                        long defaultBlock = 9000000L;

                        Checkpoint c = Checkpoint.builder()
                                .id(CHECKPOINT_ID)
                                .lastProcessedBlock(defaultBlock - 1)
                                .build();
                        checkpointRepo.save(c);
                        return c;
                    });


            while (true) {
                BigInteger latest = web3j.ethBlockNumber().send().getBlockNumber();

                long from = cp.getLastProcessedBlock() + 1;
                long to = latest.longValue();

                if (from <= to) {
                    for (long i = from; i <= to; i++) {
                        processBlock(BigInteger.valueOf(i));
                        cp.setLastProcessedBlock(i);
                        checkpointRepo.save(cp);
                    }
                }
                Thread.sleep(2000);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Transactional
    public void processBlock(BigInteger blockNumber) throws Exception {
        EthBlock ethBlock = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber), true).send();
        EthBlock.Block block = ethBlock.getBlock();

        if (block == null) {
            return;
        }

        BlockEntity be = BlockEntity.builder()
                .number(block.getNumber().longValue())
                .hash(block.getHash())
                .parentHash(block.getParentHash())
                .timestamp(Instant.ofEpochSecond(block.getTimestamp().longValue()))
                .txCount(block.getTransactions().size())
                .gasUsed(block.getGasUsed().longValue())
                .gasLimit(block.getGasLimit().longValue())
                .build();

        blockRepo.save(be);

        for (Object txObj : block.getTransactions()) {
            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txObj;

            Long gasPrice = 0L;
            if (tx.getGasPrice() != null) {
                gasPrice = tx.getGasPrice().longValue();
            } else if (tx.getMaxFeePerGas() != null) {
                gasPrice = tx.getMaxFeePerGas().longValue();
            }

            TxEntity te = TxEntity.builder()
                    .hash(tx.getHash())
                    .blockNumber(block.getNumber().longValue())
                    .fromAddress(tx.getFrom())
                    .toAddress(tx.getTo() != null ? tx.getTo() : "")
                    .input(tx.getInput() != null ? tx.getInput() : "0x")
                    .value(tx.getValue() != null ? tx.getValue().toString() : "0")
                    .gas(tx.getGas() != null ? tx.getGas().longValue() : 0L)
                    .gasPrice(gasPrice)
                    .build();

            txRepo.save(te);
        }
    }
}
