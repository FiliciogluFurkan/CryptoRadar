package gtu.graduation.project.cryptoradar.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Block(Block.Info info, Map<String, Transaction> transactions, Map<String, List<Block.Log>> logs) {

    public record Info(Long blockNumber, String hash, Instant timestamp, BigInteger baseFeePerGas) {
    }

    public record Transaction(String hash,
                              BigInteger nonce,
                              String blockHash,
                              Long blockNumber,
                              String from,
                              String to,
                              BigInteger value,
                              BigInteger gasPrice,
                              BigInteger gas,
                              String type,
                              BigInteger maxFeePerGas,
                              BigInteger maxPriorityFeePerGas
    ) {
    }

    public record Log(BigInteger logIndex,
                      BigInteger transactionIndex,
                      String transactionHash,
                      String blockHash,
                      BigInteger blockNumber,
                      String address,
                      String data,
                      String type,
                      List<String> topics
    ) {}
}