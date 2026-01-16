package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RiskScoringService {

    /**
     * Adres özelliklerinden risk skoru hesaplar (0-100)
     * Yüksek skor = Yüksek risk
     */
    public RiskResult calculateRiskScore(AddressFeatureEntity feature, Double fraudProbability) {
        List<String> riskFactors = new ArrayList<>();
        double totalScore = 0;
        double minScore = 0; // Fraud probability'ye göre minimum skor garantisi

        // 1. Fraud Probability (0-50 puan) - ML model sonucu - EN ÖNEMLİ FAKTÖR
        if (fraudProbability != null) {
            // Non-linear scaling: yüksek fraud probability daha fazla puan alsın
            double fraudScore;
            if (fraudProbability > 0.7) {
                // %70+ fraud → 45-50 puan arası + minimum 55 skor garantisi
                fraudScore = 45 + (fraudProbability - 0.7) * 16.67; // 0.7->45, 1.0->50
                minScore = 55; // En az MEDIUM-HIGH
                riskFactors.add("High fraud probability from AI model (" + String.format("%.1f%%", fraudProbability * 100) + ")");
            } else if (fraudProbability > 0.5) {
                // %50-70 fraud → 30-45 puan arası + minimum 40 skor garantisi
                fraudScore = 30 + (fraudProbability - 0.5) * 75; // 0.5->30, 0.7->45
                minScore = 40; // En az MEDIUM
                riskFactors.add("Elevated fraud probability (" + String.format("%.1f%%", fraudProbability * 100) + ")");
            } else if (fraudProbability > 0.3) {
                // %30-50 fraud → 15-30 puan arası
                fraudScore = 15 + (fraudProbability - 0.3) * 75; // 0.3->15, 0.5->30
                riskFactors.add("Moderate fraud probability (" + String.format("%.1f%%", fraudProbability * 100) + ")");
            } else {
                // %0-30 fraud → 0-15 puan arası
                fraudScore = fraudProbability * 50; // 0->0, 0.3->15
                if (fraudProbability > 0.15) {
                    riskFactors.add("Low fraud probability (" + String.format("%.1f%%", fraudProbability * 100) + ")");
                }
            }
            totalScore += fraudScore;
        }

        // 2. Transaction Pattern Analysis (0-20 puan)
        double patternScore = analyzeTransactionPattern(feature, riskFactors);
        totalScore += patternScore;

        // 3. Value Analysis (0-15 puan)
        double valueScore = analyzeValuePattern(feature, riskFactors);
        totalScore += valueScore;

        // 4. Timing Analysis (0-10 puan)
        double timingScore = analyzeTimingPattern(feature, riskFactors);
        totalScore += timingScore;

        // 5. Network Analysis (0-5 puan)
        double networkScore = analyzeNetworkPattern(feature, riskFactors);
        totalScore += networkScore;

        // Minimum skor garantisini uygula (yüksek fraud probability için)
        totalScore = Math.max(totalScore, minScore);

        // Skoru 0-100 arasında tut
        int finalScore = (int) Math.min(100, Math.max(0, Math.round(totalScore)));
        
        log.info("Risk score for {}: {} (fraud={}, pattern={}, value={}, timing={}, network={}, minGuarantee={})",
                feature.getAddress(), finalScore, 
                fraudProbability != null ? String.format("%.2f", fraudProbability) : "N/A",
                String.format("%.2f", patternScore),
                String.format("%.2f", valueScore),
                String.format("%.2f", timingScore),
                String.format("%.2f", networkScore),
                String.format("%.0f", minScore));

        return new RiskResult(finalScore, riskFactors, getRiskLevel(finalScore));
    }

    private double analyzeTransactionPattern(AddressFeatureEntity f, List<String> factors) {
        double score = 0;

        long sent = f.getSentTnx() != null ? f.getSentTnx() : 0;
        long received = f.getReceivedTnx() != null ? f.getReceivedTnx() : 0;
        long total = sent + received;

        if (total > 0) {
            double sentRatio = (double) sent / total;
            
            // Sent/Received dengesizliği - gradual scoring
            if (sentRatio > 0.95 && sent > 5) {
                score += 8;
                factors.add("Almost exclusively sends (" + String.format("%.0f%%", sentRatio * 100) + " sent)");
            } else if (sentRatio > 0.85 && sent > 10) {
                score += 5;
                factors.add("Mostly sends transactions (" + String.format("%.0f%%", sentRatio * 100) + " sent)");
            } else if (sentRatio < 0.05 && received > 5) {
                score += 6;
                factors.add("Almost exclusively receives (" + String.format("%.0f%%", (1 - sentRatio) * 100) + " received)");
            } else if (sentRatio < 0.15 && received > 10) {
                score += 3;
                factors.add("Mostly receives transactions");
            }
        }

        // Transaction count based scoring
        if (total > 5000) {
            score += 6;
            factors.add("Extremely high transaction count (" + total + ")");
        } else if (total > 1000) {
            score += 4;
            factors.add("Very high transaction count (" + total + ")");
        } else if (total > 500) {
            score += 2;
            factors.add("High transaction count (" + total + ")");
        }

        // Çok az transaction ama yüksek değer
        BigDecimal balance = f.getTotalEtherBalance() != null ? f.getTotalEtherBalance() : BigDecimal.ZERO;
        if (total < 5 && balance.compareTo(BigDecimal.valueOf(10)) > 0) {
            score += 4;
            factors.add("Few transactions but high balance (" + balance.setScale(2, java.math.RoundingMode.HALF_UP) + " ETH)");
        } else if (total < 10 && balance.compareTo(BigDecimal.valueOf(50)) > 0) {
            score += 6;
            factors.add("Very few transactions with very high balance");
        }

        return Math.min(15, score);
    }

    private double analyzeValuePattern(AddressFeatureEntity f, List<String> factors) {
        double score = 0;

        BigDecimal avgSent = f.getAvgValSent() != null ? f.getAvgValSent() : BigDecimal.ZERO;
        BigDecimal maxSent = f.getMaxValSent() != null ? f.getMaxValSent() : BigDecimal.ZERO;
        BigDecimal minSent = f.getMinValSent() != null ? f.getMinValSent() : BigDecimal.ZERO;
        BigDecimal totalSent = f.getTotalEtherSent() != null ? f.getTotalEtherSent() : BigDecimal.ZERO;
        BigDecimal totalReceived = f.getTotalEtherReceived() != null ? f.getTotalEtherReceived() : BigDecimal.ZERO;

        // Max ve Min arasında çok büyük fark (değer manipülasyonu)
        if (maxSent.compareTo(BigDecimal.ZERO) > 0 && minSent.compareTo(BigDecimal.valueOf(0.0001)) > 0) {
            BigDecimal ratio = maxSent.divide(minSent, 2, java.math.RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(10000)) > 0) {
                score += 8;
                factors.add("Extreme value variance (max/min ratio: " + ratio.setScale(0, java.math.RoundingMode.HALF_UP) + "x)");
            } else if (ratio.compareTo(BigDecimal.valueOf(1000)) > 0) {
                score += 5;
                factors.add("High value variance in transactions");
            } else if (ratio.compareTo(BigDecimal.valueOf(100)) > 0) {
                score += 2;
            }
        }

        // Çok küçük değerli çok fazla transaction (spam/dust attack)
        long sentTnx = f.getSentTnx() != null ? f.getSentTnx() : 0;
        if (avgSent.compareTo(BigDecimal.valueOf(0.0001)) < 0 && sentTnx > 20) {
            score += 8;
            factors.add("Many micro-transactions (avg: " + avgSent.toPlainString() + " ETH)");
        } else if (avgSent.compareTo(BigDecimal.valueOf(0.001)) < 0 && sentTnx > 50) {
            score += 5;
            factors.add("Frequent small transactions");
        }

        // Gönderilen ve alınan arasında büyük fark
        if (totalSent.compareTo(BigDecimal.ZERO) > 0 && totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal flowRatio = totalSent.divide(totalReceived.max(BigDecimal.valueOf(0.0001)), 2, java.math.RoundingMode.HALF_UP);
            if (flowRatio.compareTo(BigDecimal.valueOf(10)) > 0) {
                score += 4;
                factors.add("Sends much more than receives (outflow heavy)");
            } else if (flowRatio.compareTo(BigDecimal.valueOf(0.1)) < 0) {
                score += 3;
                factors.add("Receives much more than sends (inflow heavy)");
            }
        }

        return Math.min(15, score);
    }

    private double analyzeTimingPattern(AddressFeatureEntity f, List<String> factors) {
        double score = 0;

        Double avgMinBetweenSent = f.getAvgMinBetweenSentTnx();
        Double avgMinBetweenReceived = f.getAvgMinBetweenReceivedTnx();
        Double accountAge = f.getTimeDiffFirstLastMins();
        long totalTx = f.getTotalTransactions() != null ? f.getTotalTransactions() : 0;

        // Çok hızlı ardışık işlemler (bot davranışı)
        if (avgMinBetweenSent != null && avgMinBetweenSent < 0.5 && f.getSentTnx() > 10) {
            score += 6;
            factors.add("Very rapid transaction frequency (" + String.format("%.1f", avgMinBetweenSent) + " min avg)");
        } else if (avgMinBetweenSent != null && avgMinBetweenSent < 2 && f.getSentTnx() > 20) {
            score += 3;
            factors.add("Fast transaction frequency");
        }

        // Hesap çok yeni ama çok aktif
        if (accountAge != null && accountAge < 1440 && totalTx > 100) { // 1 günden az
            score += 6;
            factors.add("New account with unusually high activity (" + totalTx + " txs in <1 day)");
        } else if (accountAge != null && accountAge < 10080 && totalTx > 500) { // 1 haftadan az
            score += 4;
            factors.add("Young account with high activity");
        }

        // Çok eski hesap ama az aktivite (dormant sonra aktif)
        if (accountAge != null && accountAge > 525600 && totalTx < 10) { // 1 yıldan fazla
            score += 3;
            factors.add("Old dormant account with few transactions");
        }

        return Math.min(10, score);
    }

    private double analyzeNetworkPattern(AddressFeatureEntity f, List<String> factors) {
        double score = 0;

        int uniqueSentTo = f.getUniqueSentToAddresses() != null ? f.getUniqueSentToAddresses() : 0;
        int uniqueReceivedFrom = f.getUniqueReceivedFromAddresses() != null ? f.getUniqueReceivedFromAddresses() : 0;
        long sentTnx = f.getSentTnx() != null ? f.getSentTnx() : 0;
        long receivedTnx = f.getReceivedTnx() != null ? f.getReceivedTnx() : 0;

        // Aynı adrese çok fazla gönderim (wash trading)
        if (sentTnx > 10 && uniqueSentTo > 0) {
            double txPerAddress = (double) sentTnx / uniqueSentTo;
            if (txPerAddress > 10) {
                score += 5;
                factors.add("Concentrated sends (" + String.format("%.1f", txPerAddress) + " txs per address)");
            }
        }
        
        if (sentTnx > 20 && uniqueSentTo < 3) {
            score += 4;
            factors.add("Sends to very few unique addresses (" + uniqueSentTo + ")");
        }

        // Çok fazla farklı adresten alım (mixer pattern)
        if (uniqueReceivedFrom > 200) {
            score += 5;
            factors.add("Receives from many unique addresses (" + uniqueReceivedFrom + " - possible mixer)");
        } else if (uniqueReceivedFrom > 100) {
            score += 3;
            factors.add("Receives from many addresses (" + uniqueReceivedFrom + ")");
        }

        return Math.min(5, score);
    }

    private String getRiskLevel(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        if (score >= 20) return "LOW";
        return "SAFE";
    }

    public record RiskResult(int score, List<String> factors, String level) {}
}
