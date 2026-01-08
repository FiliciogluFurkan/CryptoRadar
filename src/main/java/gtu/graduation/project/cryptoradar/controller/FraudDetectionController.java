package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.model.FraudCheckResponse;
import gtu.graduation.project.cryptoradar.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;


    @GetMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkFraud(@RequestParam String address) {
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().body(
                    FraudCheckResponse.builder()
                            .fraud(false)
                            .message("Address parameter is required")
                            .build()
            );
        }

        FraudCheckResponse response = fraudDetectionService.checkFraud(address);
        return ResponseEntity.ok(response);
    }
}
