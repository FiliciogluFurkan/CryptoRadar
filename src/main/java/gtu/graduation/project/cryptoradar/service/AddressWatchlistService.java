package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.entity.WatchlistTransactionEntity;
import gtu.graduation.project.cryptoradar.model.AlchemyTransferResponse.Transfer;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import gtu.graduation.project.cryptoradar.repository.WatchlistTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressWatchlistService {

    private final AlchemyService alchemyService;
    private final AddressFeatureRepository addressFeatureRepository;
    private final WatchlistTransactionRepository watchlistTransactionRepository;

    /**
     * Yeni bir adresi izleme listesine ekler.
     * Alchemy'den tüm geçmiş transfer'ları çeker ve feature'ları hesaplar.
     */
    @Transactional
    public AddressFeatureEntity addAddressToWatchlist(String address) {
        address = address.toLowerCase();

        // Zaten var mı kontrol et
        Optional<AddressFeatureEntity> existing = addressFeatureRepository.findById(address);
        if (existing.isPresent()) {
            log.info("Address {} already in watchlist, refreshing data...", address);
            // Eski transaction'ları sil
            watchlistTransactionRepository.deleteByWatchedAddress(address);
        }

        // Alchemy'den transfer'ları çek
        log.info("Fetching transfers from Alchemy for address: {}", address);
        List<Transfer> outgoing = alchemyService.getOutgoingTransfers(address);
        List<Transfer> incoming = alchemyService.getIncomingTransfers(address);

        // Feature'ları hesapla
        AddressFeatureEntity feature = calculateFeatures(address, outgoing, incoming);

        // Feature'ları kaydet
        addressFeatureRepository.save(feature);

        // Transaction'ları kaydet
        saveTransactions(address, outgoing, true);
        saveTransactions(address, incoming, false);

        log.info("Address {} added to watchlist with {} sent and {} received transactions",
                address, outgoing.size(), incoming.size());

        return feature;
    }

    /**
     * Transaction'ları veritabanına kaydeder
     */
    private void saveTransactions(String watchedAddress, List<Transfer> transfers, boolean isOutgoing) {
        List<WatchlistTransactionEntity> entities = new ArrayList<>();

        for (Transfer tx : transfers) {
            WatchlistTransactionEntity entity = WatchlistTransactionEntity.builder()
                    .hash(tx.getHash())
                    .watchedAddress(watchedAddress)
                    .fromAddress(tx.getFrom() != null ? tx.getFrom().toLowerCase() : null)
                    .toAddress(tx.getTo() != null ? tx.getTo().toLowerCase() : null)
                    .value(tx.getValue() != null ? BigDecimal.valueOf(tx.getValue()) : BigDecimal.ZERO)
                    .asset(tx.getAsset())
                    .category(tx.getCategory())
                    .blockTimestamp(parseBlockTimestamp(tx))
                    .blockNum(tx.getBlockNum())
                    .isOutgoing(isOutgoing)
                    .build();
            entities.add(entity);
        }

        if (!entities.isEmpty()) {
            watchlistTransactionRepository.saveAll(entities);
            log.info("Saved {} {} transactions for address {}",
                    entities.size(), isOutgoing ? "outgoing" : "incoming", watchedAddress);
        }
    }

    /**
     * Adresi izleme listesinden çıkarır
     */
    @Transactional
    public void removeFromWatchlist(String address) {
        address = address.toLowerCase();
        // Önce transaction'ları sil
        watchlistTransactionRepository.deleteByWatchedAddress(address);
        // Sonra adresi sil
        addressFeatureRepository.deleteById(address);
        log.info("Address {} removed from watchlist", address);
    }

    /**
     * İzlenen tüm adresleri döner
     */
    public List<AddressFeatureEntity> getWatchlist() {
        return addressFeatureRepository.findAll();
    }

    /**
     * Adres izleniyor mu?
     */
    public boolean isWatched(String address) {
        return addressFeatureRepository.existsById(address.toLowerCase());
    }

    /**
     * Transfer listesinden feature'ları hesaplar
     */
    private AddressFeatureEntity calculateFeatures(String address, List<Transfer> outgoing, List<Transfer> incoming) {
        Instant now = Instant.now();

        AddressFeatureEntity feature = AddressFeatureEntity.builder()
                .address(address)
                .createdAt(now)
                .lastUpdated(now)
                .updateCount(0)
                // Default değerler - null olmaması için
                .sentTnx(0L)
                .receivedTnx(0L)
                .totalTransactions(0L)
                .totalEtherSent(BigDecimal.ZERO)
                .totalEtherReceived(BigDecimal.ZERO)
                .totalEtherBalance(BigDecimal.ZERO)
                .minValSent(BigDecimal.ZERO)
                .maxValSent(BigDecimal.ZERO)
                .avgValSent(BigDecimal.ZERO)
                .minValueReceived(BigDecimal.ZERO)
                .maxValueReceived(BigDecimal.ZERO)
                .avgValReceived(BigDecimal.ZERO)
                .uniqueSentToAddresses(0)
                .uniqueReceivedFromAddresses(0)
                .numberOfCreatedContracts(0)
                .totalEtherSentContracts(BigDecimal.ZERO)
                .minValueSentToContract(BigDecimal.ZERO)
                .maxValSentToContract(BigDecimal.ZERO)
                .avgValueSentToContract(BigDecimal.ZERO)
                .totalErc20Tnxs(0L)
                .erc20TotalEtherReceived(BigDecimal.ZERO)
                .erc20TotalEtherSent(BigDecimal.ZERO)
                .timeDiffFirstLastMins(0.0)
                .avgMinBetweenSentTnx(0.0)
                .avgMinBetweenReceivedTnx(0.0)
                .build();

        // Sent features
        calculateSentFeatures(feature, outgoing);

        // Received features
        calculateReceivedFeatures(feature, incoming);

        // Total
        feature.setTotalTransactions(feature.getSentTnx() + feature.getReceivedTnx());
        
        // Gerçek bakiyeyi Alchemy'den al (eth_getBalance)
        BigDecimal realBalance = alchemyService.getBalance(address);
        feature.setTotalEtherBalance(realBalance);
        log.info("Real balance for {}: {} ETH", address, realBalance);

        // Timing
        calculateTimingFeatures(feature, outgoing, incoming);

        return feature;
    }

    private void calculateSentFeatures(AddressFeatureEntity feature, List<Transfer> outgoing) {
        if (outgoing.isEmpty()) {
            feature.setSentTnx(0L);
            feature.setTotalEtherSent(BigDecimal.ZERO);
            feature.setMinValSent(BigDecimal.ZERO);
            feature.setMaxValSent(BigDecimal.ZERO);
            feature.setAvgValSent(BigDecimal.ZERO);
            feature.setUniqueSentToAddresses(0);
            feature.setNumberOfCreatedContracts(0);
            feature.setTotalEtherSentContracts(BigDecimal.ZERO);
            return;
        }

        feature.setSentTnx((long) outgoing.size());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = BigDecimal.ZERO;
        Set<String> uniqueTo = new HashSet<>();
        int contractCount = 0;
        BigDecimal contractTotal = BigDecimal.ZERO;

        for (Transfer tx : outgoing) {
            BigDecimal value = tx.getValue() != null ? BigDecimal.valueOf(tx.getValue()) : BigDecimal.ZERO;
            total = total.add(value);

            if (min == null || value.compareTo(min) < 0) min = value;
            if (value.compareTo(max) > 0) max = value;

            if (tx.getTo() != null) {
                uniqueTo.add(tx.getTo().toLowerCase());
            }

            // Contract interaction (erc20, erc721, etc.)
            if (tx.getCategory() != null && !tx.getCategory().equals("external")) {
                contractCount++;
                contractTotal = contractTotal.add(value);
            }
        }

        feature.setTotalEtherSent(total);
        feature.setMinValSent(min != null ? min : BigDecimal.ZERO);
        feature.setMaxValSent(max);
        feature.setAvgValSent(total.divide(BigDecimal.valueOf(outgoing.size()), 18, RoundingMode.HALF_UP));
        feature.setUniqueSentToAddresses(uniqueTo.size());
        feature.setNumberOfCreatedContracts(contractCount);
        feature.setTotalEtherSentContracts(contractTotal);
    }

    private void calculateReceivedFeatures(AddressFeatureEntity feature, List<Transfer> incoming) {
        if (incoming.isEmpty()) {
            feature.setReceivedTnx(0L);
            feature.setTotalEtherReceived(BigDecimal.ZERO);
            feature.setMinValueReceived(BigDecimal.ZERO);
            feature.setMaxValueReceived(BigDecimal.ZERO);
            feature.setAvgValReceived(BigDecimal.ZERO);
            feature.setUniqueReceivedFromAddresses(0);
            return;
        }

        feature.setReceivedTnx((long) incoming.size());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = BigDecimal.ZERO;
        Set<String> uniqueFrom = new HashSet<>();

        for (Transfer tx : incoming) {
            BigDecimal value = tx.getValue() != null ? BigDecimal.valueOf(tx.getValue()) : BigDecimal.ZERO;
            total = total.add(value);

            if (min == null || value.compareTo(min) < 0) min = value;
            if (value.compareTo(max) > 0) max = value;

            if (tx.getFrom() != null) {
                uniqueFrom.add(tx.getFrom().toLowerCase());
            }
        }

        feature.setTotalEtherReceived(total);
        feature.setMinValueReceived(min != null ? min : BigDecimal.ZERO);
        feature.setMaxValueReceived(max);
        feature.setAvgValReceived(total.divide(BigDecimal.valueOf(incoming.size()), 18, RoundingMode.HALF_UP));
        feature.setUniqueReceivedFromAddresses(uniqueFrom.size());
    }

    private void calculateTimingFeatures(AddressFeatureEntity feature, List<Transfer> outgoing, List<Transfer> incoming) {
        Instant firstTx = null;
        Instant lastTx = null;
        List<Instant> sentTimestamps = new ArrayList<>();
        List<Instant> receivedTimestamps = new ArrayList<>();

        // Outgoing transfer'lardan timestamp'leri topla
        for (Transfer tx : outgoing) {
            Instant ts = parseBlockTimestamp(tx);
            if (ts != null) {
                sentTimestamps.add(ts);
                if (firstTx == null || ts.isBefore(firstTx)) firstTx = ts;
                if (lastTx == null || ts.isAfter(lastTx)) lastTx = ts;
            }
        }

        // Incoming transfer'lardan timestamp'leri topla
        for (Transfer tx : incoming) {
            Instant ts = parseBlockTimestamp(tx);
            if (ts != null) {
                receivedTimestamps.add(ts);
                if (firstTx == null || ts.isBefore(firstTx)) firstTx = ts;
                if (lastTx == null || ts.isAfter(lastTx)) lastTx = ts;
            }
        }

        feature.setFirstTxTimestamp(firstTx);
        feature.setLastTxTimestamp(lastTx);

        if (firstTx != null && lastTx != null) {
            long diffSeconds = lastTx.getEpochSecond() - firstTx.getEpochSecond();
            feature.setTimeDiffFirstLastMins(diffSeconds / 60.0);
        } else {
            feature.setTimeDiffFirstLastMins(0.0);
        }

        // Gönderimler arası ortalama süre
        feature.setAvgMinBetweenSentTnx(calculateAvgTimeBetween(sentTimestamps));
        
        // Alımlar arası ortalama süre
        feature.setAvgMinBetweenReceivedTnx(calculateAvgTimeBetween(receivedTimestamps));
    }

    private double calculateAvgTimeBetween(List<Instant> timestamps) {
        if (timestamps.size() < 2) return 0.0;
        
        // Sırala
        timestamps.sort(Instant::compareTo);
        
        long totalDiffSeconds = 0;
        for (int i = 1; i < timestamps.size(); i++) {
            totalDiffSeconds += timestamps.get(i).getEpochSecond() - timestamps.get(i - 1).getEpochSecond();
        }
        
        double avgSeconds = (double) totalDiffSeconds / (timestamps.size() - 1);
        return avgSeconds / 60.0; // Dakikaya çevir
    }

    private Instant parseBlockTimestamp(Transfer tx) {
        if (tx.getMetadata() != null && tx.getMetadata().getBlockTimestamp() != null) {
            try {
                return Instant.parse(tx.getMetadata().getBlockTimestamp());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
