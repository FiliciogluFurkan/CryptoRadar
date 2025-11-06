package gtu.graduation.project.cryptoradar.config;

import gtu.graduation.project.cryptoradar.model.Token;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "processor.network")
@Getter
public class TokenConfiguration {

    private List<Token> tokens = new ArrayList<>();

    private Map<String, Token> tokenMap = new HashMap<>();

    @PostConstruct
    private void init() {
        setTokens(tokens);
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
        // Build the lookup map
        this.tokenMap = tokens.stream()
                .filter(t -> t.getAddress() != null)
                .collect(Collectors.toMap(
                        t -> t.getAddress().toUpperCase(),  // normalize for consistent lookup
                        t -> new Token(t.name().toUpperCase(Locale.ENGLISH), t.address().toUpperCase(Locale.ENGLISH))
                ));
    }

    public Token get(String contractAddress) {
        if (contractAddress == null) {
            return Token.ETH; // return native ETH for null address
        }
        return tokenMap.getOrDefault(contractAddress.toUpperCase(), Token.OTHER);
    }
}
