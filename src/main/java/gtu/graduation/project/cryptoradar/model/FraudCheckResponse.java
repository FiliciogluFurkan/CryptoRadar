package gtu.graduation.project.cryptoradar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python ML modelinden dönen cevap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {
    private String address;
    private boolean fraud;
    private Double fraudProbability;
    private Double normalProbability;
    private String message;
}
