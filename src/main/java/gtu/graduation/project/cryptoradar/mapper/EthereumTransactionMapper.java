package gtu.graduation.project.cryptoradar.mapper;

import gtu.graduation.project.cryptoradar.model.Block;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.utils.Numeric;

import java.util.Locale;

@Component
public class EthereumTransactionMapper implements Mapper<Block.Transaction, EthBlock.TransactionObject> {

    @Override
    public Block.Transaction map(EthBlock.TransactionObject entity) {
        if (entity == null) {
            return null;
        }

        return new Block.Transaction(
                entity.getHash(),
                Numeric.encodeQuantity(entity.getNonce()),
                entity.getBlockHash(),
                entity.getBlockNumber(),
                entity.getFrom().toLowerCase(Locale.ENGLISH),
                entity.getTo().toLowerCase(Locale.ENGLISH),
                entity.getValue(),
                entity.getGasPrice(),
                entity.getGas(),
                entity.getType(),
                entity.getMaxFeePerGas(),
                entity.getMaxPriorityFeePerGas()
        );
    }
}
