package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_check_history", indexes = {
        @Index(name = "idx_fch_address", columnList = "address"),
        @Index(name = "idx_fch_checked_at", columnList = "checkedAt"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(nullable = false)
    private Instant checkedAt;

    @Column(nullable = false)
    private Boolean isFraud;

    @Column(precision = 10, scale = 6)
    private BigDecimal fraudProbability;

    @Column(precision = 10, scale = 6)
    private BigDecimal normalProbability;

    @Column(nullable = false)
    private Integer riskScore; // 0-100

    @Column(length = 500)
    private String riskFactors; // JSON string of risk factors

    @Column(length = 200)
    private String message;
}
