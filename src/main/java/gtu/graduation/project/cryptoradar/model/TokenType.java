package gtu.graduation.project.cryptoradar.model;

import java.util.Arrays;
import java.util.Locale;

public enum TokenType {
    ETH,
    OTHER,
    USDT,
    USDC,
    DAI,
    WETH,
    LINK,
    UNI,
    STEHT,
    WSTETH,
    WBTC,
    WBETH,
    WEETH,
    CBBTC,
    SHIB,
    CRO,
    MNT,
    OKB,
    INVALID;

    public static TokenType fromToken(Token token) {
        return Arrays.stream(TokenType.values()).filter(tokenType -> tokenType.name().toUpperCase(Locale.ENGLISH).equals(token.name())).findAny().orElse(INVALID);
    }
}
