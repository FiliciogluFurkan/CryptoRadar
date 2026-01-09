package gtu.graduation.project.cryptoradar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python ML modelinden dönen cevap + Risk skoru
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {
    private String address;
    private Boolean fraud;
    private Double fraudProbability;
    private Double normalProbability;
    private String message;
    
    // Risk scoring
    private Integer riskScore;        // 0-100
    private String riskLevel;         // SAFE, LOW, MEDIUM, HIGH
    private List<String> riskFactors; // Risk faktörleri listesi
}
