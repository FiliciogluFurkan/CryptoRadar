package gtu.graduation.project.cryptoradar.mapper;

import gtu.graduation.project.cryptoradar.model.Block;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
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
                parseHexToBigInt(entity.getNonceRaw()),
                entity.getBlockHash(),
                entity.getBlockNumber().longValue(),
                entity.getFrom().toUpperCase(Locale.ENGLISH),
                entity.getTo().toUpperCase(Locale.ENGLISH),
                entity.getValue(),
                entity.getGasPrice(),
                entity.getGas(),
                entity.getType(),
                entity.getMaxFeePerGas(),
                parseHexToBigInt(entity.getMaxPriorityFeePerGasRaw())
        );
    }

    public static BigInteger parseHexToBigInt(String hexValue) {
        if (hexValue == null || hexValue.isEmpty()) return BigInteger.ZERO;
        if (hexValue.startsWith("0x") || hexValue.startsWith("x")) {
            hexValue = hexValue.substring(hexValue.indexOf("x") + 1);
        }
        if (hexValue.isEmpty()) return BigInteger.ZERO;
        return new BigInteger(hexValue, 16);
    }
}
