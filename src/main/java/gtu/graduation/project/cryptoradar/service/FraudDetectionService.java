package gtu.graduation.project.cryptoradar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.entity.FraudCheckHistoryEntity;
import gtu.graduation.project.cryptoradar.model.FraudCheckRequest;
import gtu.graduation.project.cryptoradar.model.FraudCheckResponse;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import gtu.graduation.project.cryptoradar.repository.FraudCheckHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AddressFeatureRepository addressFeatureRepository;
    private final FraudCheckHistoryRepository fraudCheckHistoryRepository;
    private final RiskScoringService riskScoringService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ml.api.url:http://localhost:7900/predict}")
    private String mlApiUrl;

    /**
     * Adres için fraud kontrolü yapar
     * 1. DB'den address feature'larını çeker
     * 2. Python FastAPI'ye gönderir
     * 3. Risk skoru hesaplar
     * 4. Geçmişe kaydeder
     * 5. Sonucu döner
     */
    public FraudCheckResponse checkFraud(String address) {
        address = address.toLowerCase();

        Optional<AddressFeatureEntity> featureOpt = addressFeatureRepository.findById(address);

        if (featureOpt.isEmpty()) {
            return FraudCheckResponse.builder()
                    .address(address)
                    .fraud(false)
                    .message("Address not found in watchlist. Please add it first via /api/watchlist/add")
                    .build();
        }

        AddressFeatureEntity feature = featureOpt.get();
        FraudCheckRequest request = buildRequest(feature);

        // ML API'ye gönder
        FraudCheckResponse response;
        try {
            response = callMlApi(request);
            response.setAddress(address);
        } catch (Exception e) {
            log.error("ML API call failed for address {}: {}", address, e.getMessage());
            response = FraudCheckResponse.builder()
                    .address(address)
                    .fraud(false)
                    .message("ML service unavailable: " + e.getMessage())
                    .build();
        }

        // Risk skoru hesapla
        RiskScoringService.RiskResult riskResult = riskScoringService.calculateRiskScore(
                feature, response.getFraudProbability());
        
        response.setRiskScore(riskResult.score());
        response.setRiskLevel(riskResult.level());
        response.setRiskFactors(riskResult.factors());

        // Geçmişe kaydet
        saveToHistory(address, response, riskResult);

        return response;
    }

    private FraudCheckRequest buildRequest(AddressFeatureEntity feature) {
        return FraudCheckRequest.builder()
                .address(feature.getAddress())
                // Transaction counts
                .sentTnx(feature.getSentTnx())
                .receivedTnx(feature.getReceivedTnx())
                .totalTransactions(feature.getTotalTransactions())
                // Timing
                .avgMinBetweenSentTnx(feature.getAvgMinBetweenSentTnx())
                .avgMinBetweenReceivedTnx(feature.getAvgMinBetweenReceivedTnx())
                .timeDiffFirstLastMins(feature.getTimeDiffFirstLastMins())
                // Network
                .uniqueSentToAddresses(feature.getUniqueSentToAddresses())
                .uniqueReceivedFromAddresses(feature.getUniqueReceivedFromAddresses())
                .numberOfCreatedContracts(feature.getNumberOfCreatedContracts())
                // Sent values
                .minValSent(feature.getMinValSent())
                .maxValSent(feature.getMaxValSent())
                .avgValSent(feature.getAvgValSent())
                .totalEtherSent(feature.getTotalEtherSent())
                // Received values
                .minValueReceived(feature.getMinValueReceived())
                .maxValueReceived(feature.getMaxValueReceived())
                .avgValReceived(feature.getAvgValReceived())
                .totalEtherReceived(feature.getTotalEtherReceived())
                // Balance
                .totalEtherBalance(feature.getTotalEtherBalance())
                // Contract values
                .minValueSentToContract(feature.getMinValueSentToContract())
                .maxValSentToContract(feature.getMaxValSentToContract())
                .avgValueSentToContract(feature.getAvgValueSentToContract())
                .totalEtherSentContracts(feature.getTotalEtherSentContracts())
                // ERC20 basic
                .totalErc20Tnxs(feature.getTotalErc20Tnxs())
                .erc20TotalEtherReceived(feature.getErc20TotalEtherReceived())
                .erc20TotalEtherSent(feature.getErc20TotalEtherSent())
                .build();
    }

    private FraudCheckResponse callMlApi(FraudCheckRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FraudCheckRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling ML API for address: {}", request.getAddress());

        return restTemplate.postForObject(mlApiUrl, entity, FraudCheckResponse.class);
    }

    private void saveToHistory(String address, FraudCheckResponse response, RiskScoringService.RiskResult riskResult) {
        try {
            String riskFactorsJson = objectMapper.writeValueAsString(riskResult.factors());
            
            FraudCheckHistoryEntity history = FraudCheckHistoryEntity.builder()
                    .address(address)
                    .checkedAt(Instant.now())
                    .isFraud(response.getFraud() != null && response.getFraud())
                    .fraudProbability(response.getFraudProbability() != null 
                            ? BigDecimal.valueOf(response.getFraudProbability()) : null)
                    .normalProbability(response.getNormalProbability() != null 
                            ? BigDecimal.valueOf(response.getNormalProbability()) : null)
                    .riskScore(riskResult.score())
                    .riskFactors(riskFactorsJson)
                    .message(response.getMessage())
                    .build();

            fraudCheckHistoryRepository.save(history);
            log.info("Saved fraud check history for address: {}", address);
        } catch (JsonProcessingException e) {
            log.error("Failed to save fraud check history: {}", e.getMessage());
        }
    }
}
