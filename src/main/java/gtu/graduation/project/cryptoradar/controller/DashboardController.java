package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import gtu.graduation.project.cryptoradar.entity.WatchlistTransactionEntity;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import gtu.graduation.project.cryptoradar.repository.NativeTransactionRepository;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import gtu.graduation.project.cryptoradar.repository.WatchlistTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final NativeTransactionRepository transactionRepository;
    private final AddressFeatureRepository addressFeatureRepository;
    private final BlockRepository blockRepository;
    private final WatchlistTransactionRepository watchlistTransactionRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalTransactions = transactionRepository.count();
        long totalWatchedAddresses = addressFeatureRepository.count();
        long totalBlocks = blockRepository.count();

        // Toplam ETH hacmi hesapla
        BigInteger totalVolume = transactionRepository.findAll().stream()
                .map(TransactionNativeTransferEntity::getValue)
                .filter(Objects::nonNull)
                .reduce(BigInteger.ZERO, BigInteger::add);

        stats.put("totalTransactions", totalTransactions);
        stats.put("watchedAddresses", totalWatchedAddresses);
        stats.put("totalBlocks", totalBlocks);
        stats.put("totalVolumeWei", totalVolume.toString());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/transactions/recent")
    public ResponseEntity<List<TransactionNativeTransferEntity>> getRecentTransactions(
            @RequestParam(defaultValue = "20") int limit) {
        List<TransactionNativeTransferEntity> transactions = transactionRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "hash")))
                .getContent();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/by-address")
    public ResponseEntity<List<Map<String, Object>>> getTransactionsByAddress(
            @RequestParam String address) {
        String normalizedAddress = address.toLowerCase();

        // Watchlist transaction tablosundan çek
        List<WatchlistTransactionEntity> watchlistTxs =
                watchlistTransactionRepository.findTop20ByWatchedAddressOrderByBlockTimestampDesc(normalizedAddress);

        if (!watchlistTxs.isEmpty()) {
            List<Map<String, Object>> result = watchlistTxs.stream()
                    .map(tx -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("hash", tx.getHash());
                        map.put("fromAddress", tx.getFromAddress());
                        map.put("toAddress", tx.getToAddress());
                        map.put("value", tx.getValue()); // ETH cinsinden
                        map.put("asset", tx.getAsset());
                        map.put("category", tx.getCategory());
                        map.put("isOutgoing", tx.isOutgoing());
                        map.put("blockTimestamp", tx.getBlockTimestamp());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        }

        // Watchlist'te yoksa native transaction tablosundan bak
        List<TransactionNativeTransferEntity> localTxs = transactionRepository.findAllByAddress(normalizedAddress);

        if (!localTxs.isEmpty()) {
            List<Map<String, Object>> result = localTxs.stream()
                    .map(tx -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("hash", tx.getHash());
                        map.put("fromAddress", tx.getFromAddress());
                        map.put("toAddress", tx.getToAddress());
                        map.put("value", tx.getValue().toString()); // Wei cinsinden
                        map.put("gasPrice", tx.getGasPrice());
                        map.put("gasLimit", tx.getGasLimit());
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/addresses/top")
    public ResponseEntity<List<AddressFeatureEntity>> getTopAddresses(
            @RequestParam(defaultValue = "10") int limit) {
        List<AddressFeatureEntity> addresses = addressFeatureRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "totalTransactions")))
                .getContent();
        return ResponseEntity.ok(addresses);
    }
}
