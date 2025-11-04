package gtu.graduation.project.cryptoradar.config;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.abi.datatypes.Int;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.List;

@Configuration
@Getter
public class ProcessorConfig {

    private final String rpcUrl;
    private final Integer batchSize;
    private final Long startBlock;
    private final String transferTopic;
    private final List<String> trackingAddresses;
    private final Integer rpcRateLimit;
    private final Integer parallelism;

    public ProcessorConfig(TrackingAddressConfig addressConfig,
                           @Value("${processor.network.rpc-url}") String rpcUrl,
                           @Value("${processor.batch-size}") Integer batchSize,
                           @Value("${processor.parallelism}") Integer parallelism,
                           @Value("${processor.network.rpc-rate-limit:20}") Integer rpcRateLimit,
                           @Value("${processor.network.start-block}") Long startBlock,
                           @Value("${processor.network.transfer-topic}") String transferTopic) {
        this.rpcUrl = rpcUrl;
        this.batchSize = batchSize;
        this.rpcRateLimit = rpcRateLimit;
        this.startBlock = startBlock;
        this.transferTopic = transferTopic;
        this.parallelism = parallelism;
        this.trackingAddresses = addressConfig.getAddresses().stream().map(String::toLowerCase).toList();
    }

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public RateLimiter rateLimiter() { return RateLimiter.create(rpcRateLimit.doubleValue());}
}
