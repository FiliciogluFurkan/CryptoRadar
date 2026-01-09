package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.entity.FraudCheckHistoryEntity;
import gtu.graduation.project.cryptoradar.repository.FraudCheckHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FraudHistoryController {

    private final FraudCheckHistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<Page<FraudCheckHistoryEntity>> getAllHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudCheckHistoryEntity> history = historyRepository.findAllByOrderByCheckedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkedAt")));
        return ResponseEntity.ok(history);
    }

    @GetMapping("/address/{address}")
    public ResponseEntity<List<FraudCheckHistoryEntity>> getHistoryByAddress(@PathVariable String address) {
        List<FraudCheckHistoryEntity> history = historyRepository
                .findByAddressOrderByCheckedAtDesc(address.toLowerCase());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/fraudulent")
    public ResponseEntity<List<FraudCheckHistoryEntity>> getFraudulentOnly() {
        return ResponseEntity.ok(historyRepository.findAllFraudulent());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", historyRepository.count());
        stats.put("fraudCount", historyRepository.countByIsFraud(true));
        stats.put("safeCount", historyRepository.countByIsFraud(false));
        return ResponseEntity.ok(stats);
    }
}
