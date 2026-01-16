package gtu.graduation.project.cryptoradar.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Python ML modeline gönderilecek feature'lar
 * Dataset sütun isimleriyle eşleşmeli
 */
@Data
@Builder
public class FraudCheckRequest {
    private String address;
    
    // Transaction counts
    private Long sentTnx;                    // "Sent tnx"
    private Long receivedTnx;                // "Received Tnx"
    private Long totalTransactions;          // "total transactions"
    
    // Timing
    private Double avgMinBetweenSentTnx;     // "Avg min between sent tnx"
    private Double avgMinBetweenReceivedTnx; // "Avg min between received tnx"
    private Double timeDiffFirstLastMins;    // "Time Diff between first and last (Mins)"
    
    // Network
    private Integer uniqueSentToAddresses;        // "Unique Sent To Addresses"
    private Integer uniqueReceivedFromAddresses;  // "Unique Received From Addresses"
    private Integer numberOfCreatedContracts;     // "Number of Created Contracts"
    
    // Sent values
    private BigDecimal minValSent;           // "min val sent"
    private BigDecimal maxValSent;           // "max val sent"
    private BigDecimal avgValSent;           // "avg val sent"
    private BigDecimal totalEtherSent;       // "total Ether sent"
    
    // Received values
    private BigDecimal minValueReceived;     // "min value received"
    private BigDecimal maxValueReceived;     // "max value received"
    private BigDecimal avgValReceived;       // "avg val received"
    private BigDecimal totalEtherReceived;   // "total ether received"
    
    // Balance
    private BigDecimal totalEtherBalance;    // "total ether balance"
    
    // Contract values
    private BigDecimal minValueSentToContract;   // "min value sent to contract"
    private BigDecimal maxValSentToContract;     // "max val sent to contract"
    private BigDecimal avgValueSentToContract;   // "avg value sent to contract"
    private BigDecimal totalEtherSentContracts;  // "total ether sent contracts"
    
    // ERC20 basic
    private Long totalErc20Tnxs;                  // "Total ERC20 tnxs"
    private BigDecimal erc20TotalEtherReceived;  // "ERC20 total Ether received"
    private BigDecimal erc20TotalEtherSent;      // "ERC20 total ether sent"
}
