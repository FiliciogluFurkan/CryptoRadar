package gtu.graduation.project.cryptoradar;

import gtu.graduation.project.cryptoradar.service.BlockIndexer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "gtu.graduation.project.cryptoradar")
@EntityScan(basePackages = "gtu.graduation.project.cryptoradar")
@RequiredArgsConstructor
public class CryptoradarApplication {

    private final BlockIndexer blockIndexer;

    public static void main(String[] args) {
        SpringApplication.run(CryptoradarApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        blockIndexer.run();
    }
}
