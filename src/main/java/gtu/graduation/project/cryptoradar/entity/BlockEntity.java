package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
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
    private List<TransactionTransferEntity> transactions = new ArrayList<>();

    @Column(name = "avg_gas_price", precision = 78)
    private BigDecimal avgGasPrice;

    @Column(name = "avg_max_fee_per_gas", precision = 78)
    private BigDecimal avgMaxFeePerGas;

    @Column(name = "avg_max_priority_fee_per_gas", precision = 78)
    private BigDecimal avgMaxPriorityFeePerGas;

    @Column(name = "avg_effective_fee_per_gas", precision = 78)
    private BigDecimal avgEffectiveFeePerGas;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "average_value",  precision = 78)
    private BigDecimal averageValue;

    @Column(name = "value_standard_deviation", precision = 78, scale = 18)
    private BigDecimal valueStandardDeviation;

    @Column(name = "gas_price_standard_deviation", precision = 78, scale = 18)
    private BigDecimal gasPriceStandardDeviation;

    public BlockEntity() {
    }

    public BlockEntity(Long blockNumber, String blockHash, Instant timestamp, BigInteger baseFeePerGas, List<TransactionTransferEntity> transactions) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.timestamp = timestamp;
        this.baseFeePerGas = baseFeePerGas;
        this.transactions = transactions;
        calculateGasStatistics();
    }

    public BlockEntity setTransactions(List<TransactionTransferEntity> transactions) {
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
        BigDecimal sumValue = BigDecimal.ZERO;
        BigDecimal valueStandardDeviation = BigDecimal.ZERO;
        BigDecimal gasPriceStandardDeviation = BigDecimal.ZERO;

        int gasPriceCount = 0;
        int maxFeeCount = 0;
        int maxPriorityFeeCount = 0;
        int effectiveFeeCount = 0;
        int valueCount = 0;

        for (TransactionTransferEntity tx : transactions) {
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

            if(tx.getValue() != null && !tx.getValue().equals(BigInteger.ZERO) && !tx.isContractInteraction()) {
                sumValue = sumValue.add(new BigDecimal(tx.getValue()));
                valueCount++;
            }
        }

        this.averageValue = valueCount > 0 ? sumValue.divide(BigDecimal.valueOf(valueCount), 18, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        this.avgGasPrice = gasPriceCount > 0 ? sumGasPrice.divide(BigDecimal.valueOf(gasPriceCount), 18, RoundingMode.HALF_UP) : null;

        List<TransactionTransferEntity> filteredValues = transactions.stream()
                .filter(tx -> tx.getValue() != null && !tx.getValue().equals(BigInteger.ZERO))
                .filter(tx -> !tx.isContractInteraction())
                .toList();
        BigDecimal valueVariance = filteredValues.stream()
                .map(v -> new BigDecimal(v.getValue()).subtract(this.averageValue).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(filteredValues.size()), RoundingMode.HALF_UP);

        List<TransactionTransferEntity> filteredGases = transactions.stream()
                .filter(tx -> tx.getGasPrice() != null && !tx.getGasPrice().equals(BigInteger.ZERO))
                .toList();
        BigDecimal gasVariance = filteredGases.stream()
                .map(v -> new BigDecimal(v.getGasPrice()).subtract(this.avgGasPrice).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(filteredGases.size()), RoundingMode.HALF_UP);

        this.gasPriceStandardDeviation = gasVariance.sqrt(MathContext.DECIMAL128);

        this.valueStandardDeviation = valueVariance.sqrt(MathContext.DECIMAL128);


        this.avgMaxFeePerGas = maxFeeCount > 0 ? sumMaxFeePerGas.divide(BigDecimal.valueOf(maxFeeCount), 18, RoundingMode.HALF_UP) : null;

        this.avgMaxPriorityFeePerGas = maxPriorityFeeCount > 0 ? sumMaxPriorityFeePerGas.divide(BigDecimal.valueOf(maxPriorityFeeCount), 18, RoundingMode.HALF_UP) : null;

        this.avgEffectiveFeePerGas = effectiveFeeCount > 0 ? sumEffectiveFeePerGas.divide(BigDecimal.valueOf(effectiveFeeCount), 18, RoundingMode.HALF_UP) : null;
    }
}