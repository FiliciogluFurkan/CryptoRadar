package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.model.FraudCheckRequest;
import gtu.graduation.project.cryptoradar.model.FraudCheckResponse;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AddressFeatureRepository addressFeatureRepository;
    private final RestTemplate restTemplate;

    @Value("${ml.api.url:http://localhost:8000/predict}")
    private String mlApiUrl;

    /**
     * Adres için fraud kontrolü yapar
     * 1. DB'den address feature'larını çeker
     * 2. Python FastAPI'ye gönderir
     * 3. Sonucu döner
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

        // 3. Python FastAPI'ye gönder
        try {
            FraudCheckResponse response = callMlApi(request);
            response.setAddress(address);
            return response;
        } catch (Exception e) {
            log.error("ML API call failed for address {}: {}", address, e.getMessage());
            return FraudCheckResponse.builder()
                    .address(address)
                    .fraud(false)
                    .message("ML service unavailable: " + e.getMessage())
                    .build();
        }
    }

    private FraudCheckRequest buildRequest(AddressFeatureEntity feature) {
        return FraudCheckRequest.builder()
                .address(feature.getAddress())
                .sentTnx(feature.getSentTnx())
                .receivedTnx(feature.getReceivedTnx())
                .totalTransactions(feature.getTotalTransactions())
                .avgValSent(feature.getAvgValSent())
                .avgValReceived(feature.getAvgValReceived())
                .totalEtherSent(feature.getTotalEtherSent())
                .totalEtherReceived(feature.getTotalEtherReceived())
                .totalEtherBalance(feature.getTotalEtherBalance())
                .uniqueSentToAddresses(feature.getUniqueSentToAddresses())
                .uniqueReceivedFromAddresses(feature.getUniqueReceivedFromAddresses())
                .timeDiffFirstLastMins(feature.getTimeDiffFirstLastMins())
                .avgMinBetweenSentTnx(feature.getAvgMinBetweenSentTnx())
                .avgMinBetweenReceivedTnx(feature.getAvgMinBetweenReceivedTnx())
                .numberOfCreatedContracts(feature.getNumberOfCreatedContracts())
                .totalEtherSentContracts(feature.getTotalEtherSentContracts())
                .totalErc20Tnxs(feature.getTotalErc20Tnxs())
                .build();
    }

    private FraudCheckResponse callMlApi(FraudCheckRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FraudCheckRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling ML API for address: {}", request.getAddress());

        return restTemplate.postForObject(mlApiUrl, entity, FraudCheckResponse.class);
    }
}
