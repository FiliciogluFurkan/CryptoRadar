package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.model.AlchemyTransferResponse;
import gtu.graduation.project.cryptoradar.model.AlchemyTransferResponse.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlchemyService {

    private final RestTemplate restTemplate;

    @Value("${alchemy.api.key:ZvfGfPEBrKJGcqrlxDJFG}")
    private String apiKey;

    @Value("${alchemy.api.url:https://eth-mainnet.g.alchemy.com/v2/}")
    private String baseUrl;

    /**
     * Bir adresin TÜM gönderdiği transfer'ları çeker
     */
    public List<Transfer> getOutgoingTransfers(String address) {
        return getTransfers(address, true);
    }

    /**
     * Bir adresin TÜM aldığı transfer'ları çeker
     */
    public List<Transfer> getIncomingTransfers(String address) {
        return getTransfers(address, false);
    }

    /**
     * Bir adresin tüm transfer'larını çeker (hem gelen hem giden)
     */
    public List<Transfer> getAllTransfers(String address) {
        List<Transfer> all = new ArrayList<>();
        all.addAll(getOutgoingTransfers(address));
        all.addAll(getIncomingTransfers(address));
        return all;
    }

    private List<Transfer> getTransfers(String address, boolean isOutgoing) {
        List<Transfer> allTransfers = new ArrayList<>();
        String pageKey = null;

        do {
            AlchemyTransferResponse response = fetchTransferPage(address, isOutgoing, pageKey);

            if (response != null && response.getResult() != null && response.getResult().getTransfers() != null) {
                allTransfers.addAll(response.getResult().getTransfers());
                pageKey = response.getResult().getPageKey();
            } else {
                break;
            }

        } while (pageKey != null && !pageKey.isEmpty());

        log.info("Fetched {} {} transfers for address {}",
                allTransfers.size(), isOutgoing ? "outgoing" : "incoming", address);

        return allTransfers;
    }

    private AlchemyTransferResponse fetchTransferPage(String address, boolean isOutgoing, String pageKey) {
        try {
            String url = baseUrl + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> params = new HashMap<>();
            params.put("fromBlock", "0x0");
            params.put("excludeZeroValue", true);
            params.put("withMetadata", true);
            params.put("category", List.of("external", "erc20", "erc721", "erc1155"));

            if (isOutgoing) {
                params.put("fromAddress", address);
            } else {
                params.put("toAddress", address);
            }

            if (pageKey != null && !pageKey.isEmpty()) {
                params.put("pageKey", pageKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("jsonrpc", "2.0");
            body.put("method", "alchemy_getAssetTransfers");
            body.put("params", List.of(params));
            body.put("id", 1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            return restTemplate.postForObject(url, entity, AlchemyTransferResponse.class);

        } catch (Exception e) {
            log.error("Alchemy API call failed: {}", e.getMessage());
            return null;
        }
    }
}
