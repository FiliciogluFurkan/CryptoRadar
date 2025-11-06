package gtu.graduation.project.cryptoradar.mapper;

import gtu.graduation.project.cryptoradar.model.Block;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.Log;

import java.util.Locale;

@Component
public class EthereumLogMapper implements Mapper<Block.Log, Log> {

    @Override
    public Block.Log map(Log entity) {
        if (entity == null) {
            return null;
        }

        return new Block.Log(
                entity.getLogIndex(),
                entity.getTransactionIndex(),
                entity.getTransactionHash(),
                entity.getBlockHash(),
                entity.getBlockNumber(),
                entity.getAddress().toUpperCase(Locale.ENGLISH),
                entity.getData(),
                entity.getType(),
                entity.getTopics()
        );
    }
}
