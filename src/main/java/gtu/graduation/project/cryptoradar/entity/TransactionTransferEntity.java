package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Entity
@Table(name = "transaction_transfers", indexes = {
        @Index(name = "idx_tx_hash", columnList = "hash"),
        @Index(name = "idx_tx_from", columnList = "fromAddress"),
        @Index(name = "idx_tx_to", columnList = "toAddress"),
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionTransferEntity {


    @Column(nullable = false, unique = true, length = 66)
    @Id
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    private BlockEntity block;

    @Column(nullable = false)
    private BigInteger nonce;

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(nullable = false, precision = 78)
    private BigInteger value;

    @Column(nullable = true, precision = 78)
    private BigInteger tokenValue;

    @Column(name = "gas_price", precision = 38, scale = 0)
    private BigInteger gasPrice;

    @Column(name = "gas_limit", nullable = false, precision = 38, scale = 0)
    private BigInteger gasLimit;

    @Column(name = "max_fee_per_gas", precision = 38, scale = 0)
    private BigInteger maxFeePerGas;

    @Column(name = "max_priority_fee_per_gas", precision = 38, scale = 0)
    private BigInteger maxPriorityFeePerGas;

    @Column(name = "effective_fee_per_gas", precision = 38, scale = 0)
    private BigInteger effectiveFeePerGas; // Actual fee paid per gas

    @Column(name = "token")
    @Enumerated(EnumType.STRING)
    private TokenType token;

    @Column(length = 10)
    private String type; // "0x0", "0x1", "0x2" (legacy, EIP-2930, EIP-1559)

    @Column(name = "priority_fee_ratio",  precision = 38, scale = 18)
    private BigDecimal priorityFeeRatio;

    @Column(name = "base_fee_ratio")
    private BigDecimal baseFeeRatio;

    @Column(name = "is_contract_interaction")
    private boolean isContractInteraction;

    @Column(name = "value_z_score", nullable = true)
    private BigDecimal valueZScore;

    @Column(name = "gas_z_score")
    private BigDecimal gasZScore;

    @Column(name = "priority_fee_percentile")
    private BigDecimal priorityFeePercentile;

    public TransactionTransferEntity(BlockEntity block, String hash, BigInteger nonce, String fromAddress, String toAddress,
                                     BigInteger value, BigInteger gasPrice, BigInteger gasLimit, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas,
                                     TokenType token, String type, BigInteger baseFeePerGas, boolean isContractInteraction, BigInteger tokenValue) {
        this.block = block;
        this.hash = hash;
        this.nonce = nonce;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.maxFeePerGas = maxFeePerGas;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.token = token;
        this.tokenValue = tokenValue;
        this.type = type;
        if(maxFeePerGas != null && !maxFeePerGas.equals(BigInteger.ZERO) && maxPriorityFeePerGas != null && !maxPriorityFeePerGas.equals(BigInteger.ZERO)) {
            this.priorityFeeRatio = new BigDecimal(maxPriorityFeePerGas, 18).divide(new BigDecimal(maxFeePerGas, 18), RoundingMode.HALF_DOWN);
        } else {
            this.priorityFeeRatio = null;
        }
        if(maxFeePerGas != null && !maxFeePerGas.equals(BigInteger.ZERO)) {
            this.baseFeeRatio = new BigDecimal(maxFeePerGas, 18).divide(new BigDecimal(block.getBaseFeePerGas(), 18), RoundingMode.HALF_DOWN);
        } else {
            this.baseFeeRatio = null;
        }
        this.isContractInteraction = isContractInteraction;
        calculateEffectiveFee(baseFeePerGas);
    }

    /**
     * Calculate effective fee per gas based on transaction type and block base fee
     * For EIP-1559 (type 2): min(maxFeePerGas, baseFeePerGas + maxPriorityFeePerGas)
     * For legacy/EIP-2930: gasPrice
     */
    public void calculateEffectiveFee(BigInteger baseFeePerGas) {

        if ("0x2".equals(type) && maxFeePerGas != null && maxPriorityFeePerGas != null) {
            // EIP-1559 transaction
            BigInteger basePlusPriority = baseFeePerGas.add(maxPriorityFeePerGas);
            this.effectiveFeePerGas = maxFeePerGas.min(basePlusPriority);
        } else if (gasPrice != null) {
            // Legacy or EIP-2930 transaction
            this.effectiveFeePerGas = gasPrice;
        }
    }
}