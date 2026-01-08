package gtu.graduation.project.cryptoradar.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Python ML modeline gönderilecek feature'lar
 */
@Data
@Builder
public class FraudCheckRequest {
    private String address;
    private Long sentTnx;
    private Long receivedTnx;
    private Long totalTransactions;
    private BigDecimal avgValSent;
    private BigDecimal avgValReceived;
    private BigDecimal totalEtherSent;
    private BigDecimal totalEtherReceived;
    private BigDecimal totalEtherBalance;
    private Integer uniqueSentToAddresses;
    private Integer uniqueReceivedFromAddresses;
    private Double timeDiffFirstLastMins;
    private Double avgMinBetweenSentTnx;
    private Double avgMinBetweenReceivedTnx;
    private Integer numberOfCreatedContracts;
    private BigDecimal totalEtherSentContracts;
    private Long totalErc20Tnxs;
}
