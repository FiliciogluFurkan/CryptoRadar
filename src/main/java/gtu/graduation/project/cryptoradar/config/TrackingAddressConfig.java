package gtu.graduation.project.cryptoradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "processor.network.tracking")
@Data
public class TrackingAddressConfig {

    private List<String> addresses = new ArrayList<>();

}
