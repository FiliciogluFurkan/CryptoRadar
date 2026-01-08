package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * Address feature güncelleme - SADECE izlenen adresler için incremental update yapar.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AddressFeatureUpdater {

    private final AddressFeatureRepository addressFeatureRepository;
    private static final BigDecimal WEI_TO_ETH = new BigDecimal("1000000000000000000");

    /**
     * Yeni bir transaction için izlenen adreslerin feature'larını günceller.
     * İzlenmeyen adresler skip edilir.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateForTransaction(TransactionNativeTransferEntity tx) {
        BigDecimal valueInEth = weiToEth(tx.getValue());
        Instant txTimestamp = tx.getBlock() != null ? tx.getBlock().getTimestamp() : Instant.now();
        boolean isContractInteraction = tx.isContractInteraction();

        // FROM adresi izleniyor mu?
        if (tx.getFromAddress() != null) {
            String fromAddress = tx.getFromAddress().toLowerCase();
            Optional<AddressFeatureEntity> fromFeature = addressFeatureRepository.findById(fromAddress);

            if (fromFeature.isPresent()) {
                updateSenderFeatures(fromFeature.get(), valueInEth, txTimestamp,
                        tx.getToAddress(), isContractInteraction);
                log.debug("Updated sender features for watched address: {}", fromAddress);
            }
            // İzlenmiyorsa skip
        }

        // TO adresi izleniyor mu?
        if (tx.getToAddress() != null) {
            String toAddress = tx.getToAddress().toLowerCase();
            Optional<AddressFeatureEntity> toFeature = addressFeatureRepository.findById(toAddress);

            if (toFeature.isPresent()) {
                updateReceiverFeatures(toFeature.get(), valueInEth, txTimestamp, tx.getFromAddress());
                log.debug("Updated receiver features for watched address: {}", toAddress);
            }
            // İzlenmiyorsa skip
        }
    }

    /**
     * Gönderen adres için incremental update
     */
    private void updateSenderFeatures(AddressFeatureEntity feature, BigDecimal value, Instant timestamp,
                                       String toAddress, boolean isContractInteraction) {
        // Transaction sayısı
        feature.setSentTnx(feature.getSentTnx() + 1);
        feature.setTotalTransactions(feature.getTotalTransactions() + 1);

        // Değer istatistikleri
        feature.setTotalEtherSent(feature.getTotalEtherSent().add(value));

        if (value.compareTo(feature.getMaxValSent()) > 0) {
            feature.setMaxValSent(value);
        }
        if (feature.getMinValSent().compareTo(BigDecimal.ZERO) == 0 ||
                value.compareTo(feature.getMinValSent()) < 0) {
            feature.setMinValSent(value);
        }

        // Ortalama hesapla
        if (feature.getSentTnx() > 0) {
            feature.setAvgValSent(feature.getTotalEtherSent()
                    .divide(BigDecimal.valueOf(feature.getSentTnx()), 18, RoundingMode.HALF_UP));
        }

        // Bakiye güncelle
        feature.setTotalEtherBalance(feature.getTotalEtherReceived().subtract(feature.getTotalEtherSent()));

        // Contract interaction
        if (isContractInteraction) {
            feature.setNumberOfCreatedContracts(feature.getNumberOfCreatedContracts() + 1);
            feature.setTotalEtherSentContracts(feature.getTotalEtherSentContracts().add(value));

            if (value.compareTo(feature.getMaxValSentToContract()) > 0) {
                feature.setMaxValSentToContract(value);
            }
            if (feature.getMinValueSentToContract().compareTo(BigDecimal.ZERO) == 0 ||
                    value.compareTo(feature.getMinValueSentToContract()) < 0) {
                feature.setMinValueSentToContract(value);
            }

            if (feature.getNumberOfCreatedContracts() > 0) {
                feature.setAvgValueSentToContract(feature.getTotalEtherSentContracts()
                        .divide(BigDecimal.valueOf(feature.getNumberOfCreatedContracts()), 18, RoundingMode.HALF_UP));
            }
        }

        // Timestamp güncelle
        updateTimestamps(feature, timestamp, true);

        feature.setLastUpdated(Instant.now());
        feature.setUpdateCount(feature.getUpdateCount() + 1);

        addressFeatureRepository.save(feature);
    }

    /**
     * Alıcı adres için incremental update
     */
    private void updateReceiverFeatures(AddressFeatureEntity feature, BigDecimal value, Instant timestamp,
                                         String fromAddress) {
        // Transaction sayısı
        feature.setReceivedTnx(feature.getReceivedTnx() + 1);
        feature.setTotalTransactions(feature.getTotalTransactions() + 1);

        // Değer istatistikleri
        feature.setTotalEtherReceived(feature.getTotalEtherReceived().add(value));

        if (value.compareTo(feature.getMaxValueReceived()) > 0) {
            feature.setMaxValueReceived(value);
        }
        if (feature.getMinValueReceived().compareTo(BigDecimal.ZERO) == 0 ||
                value.compareTo(feature.getMinValueReceived()) < 0) {
            feature.setMinValueReceived(value);
        }

        // Ortalama hesapla
        if (feature.getReceivedTnx() > 0) {
            feature.setAvgValReceived(feature.getTotalEtherReceived()
                    .divide(BigDecimal.valueOf(feature.getReceivedTnx()), 18, RoundingMode.HALF_UP));
        }

        // Bakiye güncelle
        feature.setTotalEtherBalance(feature.getTotalEtherReceived().subtract(feature.getTotalEtherSent()));

        // Timestamp güncelle
        updateTimestamps(feature, timestamp, false);

        feature.setLastUpdated(Instant.now());
        feature.setUpdateCount(feature.getUpdateCount() + 1);

        addressFeatureRepository.save(feature);
    }

    /**
     * Timestamp ve timing feature'larını güncelle
     */
    private void updateTimestamps(AddressFeatureEntity feature, Instant timestamp, boolean isSent) {
        // İlk transaction
        if (feature.getFirstTxTimestamp() == null || timestamp.isBefore(feature.getFirstTxTimestamp())) {
            feature.setFirstTxTimestamp(timestamp);
        }

        // Son transaction
        Instant previousLast = feature.getLastTxTimestamp();
        if (feature.getLastTxTimestamp() == null || timestamp.isAfter(feature.getLastTxTimestamp())) {
            feature.setLastTxTimestamp(timestamp);
        }

        // İlk-son arası fark
        if (feature.getFirstTxTimestamp() != null && feature.getLastTxTimestamp() != null) {
            long diffSeconds = feature.getLastTxTimestamp().getEpochSecond() -
                    feature.getFirstTxTimestamp().getEpochSecond();
            feature.setTimeDiffFirstLastMins(diffSeconds / 60.0);
        }

        // Ortalama süre hesaplama (running average)
        if (previousLast != null) {
            long diffSeconds = Math.abs(timestamp.getEpochSecond() - previousLast.getEpochSecond());
            double diffMins = diffSeconds / 60.0;

            if (isSent && feature.getSentTnx() > 1) {
                double oldAvg = feature.getAvgMinBetweenSentTnx();
                double newAvg = oldAvg + (diffMins - oldAvg) / feature.getSentTnx();
                feature.setAvgMinBetweenSentTnx(newAvg);
            } else if (!isSent && feature.getReceivedTnx() > 1) {
                double oldAvg = feature.getAvgMinBetweenReceivedTnx();
                double newAvg = oldAvg + (diffMins - oldAvg) / feature.getReceivedTnx();
                feature.setAvgMinBetweenReceivedTnx(newAvg);
            }
        }
    }

    private BigDecimal weiToEth(BigInteger wei) {
        if (wei == null) return BigDecimal.ZERO;
        return new BigDecimal(wei).divide(WEI_TO_ETH, 18, RoundingMode.HALF_UP);
    }
}
