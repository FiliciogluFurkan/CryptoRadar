package gtu.graduation.project.cryptoradar.config;

import gtu.graduation.project.cryptoradar.model.Token;
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

    // This map is automatically built after tokens are set
    private Map<String, Token> tokenMap = new HashMap<>();

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
        // Build the lookup map
        this.tokenMap = tokens.stream()
                .filter(t -> t.getAddress() != null)
                .collect(Collectors.toMap(
                        t -> t.getAddress().toLowerCase(),  // normalize for consistent lookup
                        t -> t
                ));
    }

    public Token get(String contractAddress) {
        if (contractAddress == null) {
            return Token.NATIVE; // return native ETH for null address
        }
        return tokenMap.getOrDefault(contractAddress.toLowerCase(), Token.NATIVE);
    }
}
