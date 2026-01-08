package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressFeatureService {

    private final AddressFeatureUpdater addressFeatureUpdater;

    /**
     * Transaction listesi için address feature'larını günceller.
     * Her transaction için from ve to adreslerinin feature'ları incremental olarak güncellenir.
     */
    public void updateAddressFeaturesBatch(List<TransactionNativeTransferEntity> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        log.info("Updating address features for {} transactions", transactions.size());

        int successCount = 0;
        int failCount = 0;

        for (TransactionNativeTransferEntity tx : transactions) {
            try {
                addressFeatureUpdater.updateForTransaction(tx);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.debug("Error updating features for tx {}: {}", tx.getHash(), e.getMessage());
            }
        }

        log.info("Updated features: {} success, {} failed out of {} transactions",
                successCount, failCount, transactions.size());
    }
}
