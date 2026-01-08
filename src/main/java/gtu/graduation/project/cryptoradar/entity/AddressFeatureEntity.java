package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "address_features", indexes = {
        @Index(name = "idx_address", columnList = "address"),
        @Index(name = "idx_last_updated", columnList = "lastUpdated")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressFeatureEntity {

    @Id
    @Column(length = 42, nullable = false)
    private String address;

    // ========== TRANSACTION COUNTS ==========
    @Column(nullable = false)
    private Long sentTnx = 0L;

    @Column(nullable = false)
    private Long receivedTnx = 0L;

    @Column(nullable = false)
    private Long totalTransactions = 0L;

    // ========== SENT VALUES ==========
    @Column(precision = 38, scale = 18)
    private BigDecimal minValSent = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal maxValSent = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal avgValSent = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal totalEtherSent = BigDecimal.ZERO;

    // ========== RECEIVED VALUES ==========
    @Column(precision = 38, scale = 18)
    private BigDecimal minValueReceived = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal maxValueReceived = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal avgValReceived = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal totalEtherReceived = BigDecimal.ZERO;

    // ========== BALANCE ==========
    @Column(precision = 38, scale = 18)
    private BigDecimal totalEtherBalance = BigDecimal.ZERO;

    // ========== NETWORK ==========
    @Column(nullable = false)
    private Integer uniqueSentToAddresses = 0;

    @Column(nullable = false)
    private Integer uniqueReceivedFromAddresses = 0;

    // ========== TIMING ==========
    private Instant firstTxTimestamp;

    private Instant lastTxTimestamp;

    private Double timeDiffFirstLastMins = 0.0;

    private Double avgMinBetweenSentTnx = 0.0;

    private Double avgMinBetweenReceivedTnx = 0.0;

    // ========== CONTRACT ==========
    @Column(nullable = false)
    private Integer numberOfCreatedContracts = 0;

    @Column(precision = 38, scale = 18)
    private BigDecimal minValueSentToContract = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal maxValSentToContract = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal avgValueSentToContract = BigDecimal.ZERO;

    @Column(precision = 38, scale = 18)
    private BigDecimal totalEtherSentContracts = BigDecimal.ZERO;

    // ========== ERC20 (Basic) ==========
    @Column(name = "total_erc20tnxs", nullable = false)
    private Long totalErc20Tnxs = 0L;

    @Column(name = "erc20_total_ether_received", precision = 38, scale = 18)
    private BigDecimal erc20TotalEtherReceived = BigDecimal.ZERO;

    @Column(name = "erc20_total_ether_sent", precision = 38, scale = 18)
    private BigDecimal erc20TotalEtherSent = BigDecimal.ZERO;

    // ========== METADATA ==========
    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastUpdated;

    @Column(nullable = false)
    private Integer updateCount = 0;
}