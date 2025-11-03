package gtu.graduation.project.cryptoradar.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.List;

@Configuration
@Getter
public class ProcessorConfig {

    private final String rpcUrl;
    private final Long batchSize;
    private final Long startBlock;
    private final String transferTopic;
    private final List<String> trackingAddresses;

    public ProcessorConfig(@Value("${processor.network.rpc-url}") String rpcUrl,
                           @Value("${processor.batch-size}") Long batchSize,
                           @Value("${processor.network.start-block}") Long startBlock,
                           @Value("${processor.network.transfer-topic}") String transferTopic,
                           @Value("${processor.network.tracking.addresses}") List<String> trackingAddresses) {
        this.rpcUrl = rpcUrl;
        this.batchSize = batchSize;
        this.startBlock = startBlock;
        this.transferTopic = transferTopic;
        this.trackingAddresses = trackingAddresses.stream().map(String::toLowerCase).toList();
    }

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }
}
