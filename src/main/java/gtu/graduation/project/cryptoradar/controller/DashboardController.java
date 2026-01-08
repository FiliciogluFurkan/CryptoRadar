package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import gtu.graduation.project.cryptoradar.repository.AddressFeatureRepository;
import gtu.graduation.project.cryptoradar.repository.NativeTransactionRepository;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final NativeTransactionRepository transactionRepository;
    private final AddressFeatureRepository addressFeatureRepository;
    private final BlockRepository blockRepository;

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
    public ResponseEntity<List<TransactionNativeTransferEntity>> getTransactionsByAddress(
            @RequestParam String address) {
        List<TransactionNativeTransferEntity> transactions = transactionRepository.findAllByAddress(address.toLowerCase());
        return ResponseEntity.ok(transactions);
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
