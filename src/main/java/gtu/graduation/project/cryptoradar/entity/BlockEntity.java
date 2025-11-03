package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blocks")
@Getter
@Setter
public class BlockEntity {

    @Id
    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_hash", nullable = false)
    private String blockHash;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "base_fee_per_gas", nullable = false)
    private BigInteger baseFeePerGas;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntity> transactions = new ArrayList<>();

    @Column(name = "avg_gas_price", precision = 38, scale = 18)
    private BigDecimal avgGasPrice;

    @Column(name = "avg_max_fee_per_gas", precision = 38, scale = 18)
    private BigDecimal avgMaxFeePerGas;

    @Column(name = "avg_max_priority_fee_per_gas", precision = 38, scale = 18)
    private BigDecimal avgMaxPriorityFeePerGas;

    @Column(name = "avg_effective_fee_per_gas", precision = 38, scale = 18)
    private BigDecimal avgEffectiveFeePerGas;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "total_gas_used")
    private BigInteger totalGasUsed;

    protected BlockEntity() {
    }

    public BlockEntity(Long blockNumber, String blockHash, Instant timestamp, BigInteger baseFeePerGas, List<TransactionEntity> transactions) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.baseFeePerGas = baseFeePerGas;
        this.transactions = transactions;
        calculateGasStatistics();
    }

    public BlockEntity setTransactions(List<TransactionEntity> transactions) {
        this.transactions = transactions;
        calculateGasStatistics();
        return this;
    }

    /**
     * Calculate and set average gas fee values based on all transactions in the block
     */
    public void calculateGasStatistics() {
        if (transactions.isEmpty()) {
            this.totalTransactions = 0;
            this.totalGasUsed = BigInteger.ZERO;
            this.avgGasPrice = BigDecimal.ZERO;
            this.avgMaxFeePerGas = BigDecimal.ZERO;
            this.avgMaxPriorityFeePerGas = BigDecimal.ZERO;
            this.avgEffectiveFeePerGas = BigDecimal.ZERO;
            return;
        }

        this.totalTransactions = transactions.size();

        BigDecimal sumGasPrice = BigDecimal.ZERO;
        BigDecimal sumMaxFeePerGas = BigDecimal.ZERO;
        BigDecimal sumMaxPriorityFeePerGas = BigDecimal.ZERO;
        BigDecimal sumEffectiveFeePerGas = BigDecimal.ZERO;
        BigInteger totalGas = BigInteger.ZERO;

        int gasPriceCount = 0;
        int maxFeeCount = 0;
        int maxPriorityFeeCount = 0;
        int effectiveFeeCount = 0;

        for (TransactionEntity tx : transactions) {
            if (tx.getGasPrice() != null) {
                sumGasPrice = sumGasPrice.add(new BigDecimal(tx.getGasPrice()));
                gasPriceCount++;
            }

            if (tx.getMaxFeePerGas() != null) {
                sumMaxFeePerGas = sumMaxFeePerGas.add(new BigDecimal(tx.getMaxFeePerGas()));
                maxFeeCount++;
            }

            if (tx.getMaxPriorityFeePerGas() != null) {
                sumMaxPriorityFeePerGas = sumMaxPriorityFeePerGas.add(new BigDecimal(tx.getMaxPriorityFeePerGas()));
                maxPriorityFeeCount++;
            }

            if (tx.getEffectiveFeePerGas() != null) {
                sumEffectiveFeePerGas = sumEffectiveFeePerGas.add(new BigDecimal(tx.getEffectiveFeePerGas()));
                effectiveFeeCount++;
            }

            if (tx.getGasUsed() != null) {
                totalGas = totalGas.add(tx.getGasUsed());
            }
        }

        this.avgGasPrice = gasPriceCount > 0 ? sumGasPrice.divide(BigDecimal.valueOf(gasPriceCount), 18, RoundingMode.HALF_UP) : null;

        this.avgMaxFeePerGas = maxFeeCount > 0 ? sumMaxFeePerGas.divide(BigDecimal.valueOf(maxFeeCount), 18, RoundingMode.HALF_UP) : null;

        this.avgMaxPriorityFeePerGas = maxPriorityFeeCount > 0 ? sumMaxPriorityFeePerGas.divide(BigDecimal.valueOf(maxPriorityFeeCount), 18, RoundingMode.HALF_UP) : null;

        this.avgEffectiveFeePerGas = effectiveFeeCount > 0 ? sumEffectiveFeePerGas.divide(BigDecimal.valueOf(effectiveFeeCount), 18, RoundingMode.HALF_UP) : null;

        this.totalGasUsed = totalGas;
    }
}