package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import gtu.graduation.project.cryptoradar.service.AddressWatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AddressWatchlistController {

    private final AddressWatchlistService watchlistService;

    @PostMapping("/add")
    public ResponseEntity<AddressFeatureEntity> addAddress(@RequestParam String address) {
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AddressFeatureEntity feature = watchlistService.addAddressToWatchlist(address);
        return ResponseEntity.ok(feature);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, String>> removeAddress(@RequestParam String address) {
        watchlistService.removeFromWatchlist(address);
        return ResponseEntity.ok(Map.of("message", "Address removed from watchlist"));
    }

    @GetMapping
    public ResponseEntity<List<AddressFeatureEntity>> getWatchlist() {
        return ResponseEntity.ok(watchlistService.getWatchlist());
    }


    @GetMapping("/{address}")
    public ResponseEntity<AddressFeatureEntity> getAddress(@PathVariable String address) {
        if (!watchlistService.isWatched(address)) {
            return ResponseEntity.notFound().build();
        }

        return watchlistService.getWatchlist().stream()
                .filter(a -> a.getAddress().equalsIgnoreCase(address))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
