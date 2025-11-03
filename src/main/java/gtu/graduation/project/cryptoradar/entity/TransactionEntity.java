package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.Token;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_hash", columnList = "hash"),
        @Index(name = "idx_tx_from", columnList = "fromAddress"),
        @Index(name = "idx_tx_to", columnList = "toAddress"),
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(nullable = false, unique = true, length = 66)
    private String hash;

    @Column(nullable = false)
    private String nonce;

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(nullable = false, precision = 38, scale = 0)
    private BigInteger value;

    @Column(name = "gas_price", precision = 38, scale = 0)
    private BigInteger gasPrice;

    @Column(name = "gas_limit", nullable = false, precision = 38, scale = 0)
    private BigInteger gasLimit;

    @Column(name = "gas_used", precision = 38, scale = 0)
    private BigInteger gasUsed;

    @Column(name = "max_fee_per_gas", precision = 38, scale = 0)
    private BigInteger maxFeePerGas;

    @Column(name = "max_priority_fee_per_gas", precision = 38, scale = 0)
    private BigInteger maxPriorityFeePerGas;

    @Column(name = "effective_fee_per_gas", precision = 38, scale = 0)
    private BigInteger effectiveFeePerGas; // Actual fee paid per gas

    @Column(name = "total_fee", precision = 38, scale = 18)
    private BigDecimal totalFee; // effectiveFeePerGas * gasUsed in ETH

    @Column(name = "token")
    @Enumerated(value = EnumType.STRING)
    private Token token;

    @Column(length = 10)
    private String type; // "0x0", "0x1", "0x2" (legacy, EIP-2930, EIP-1559)

    public TransactionEntity(UUID id, Long blockNumber, String hash, String nonce, String fromAddress, String toAddress,
                             BigInteger value, BigInteger gasPrice, BigInteger gasLimit,
                             BigInteger gasUsed, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas,
                             Token token, String type, BigInteger baseFeePerGas) {
        this.id = id;
        this.blockNumber = blockNumber;
        this.hash = hash;
        this.nonce = nonce;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.gasUsed = gasUsed;
        this.maxFeePerGas = maxFeePerGas;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.token = token;
        this.type = type;
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

        // Calculate total fee in ETH
        if (effectiveFeePerGas != null && gasUsed != null) {
            BigDecimal feeInWei = new BigDecimal(effectiveFeePerGas.multiply(gasUsed));
            this.totalFee = feeInWei.divide(new BigDecimal("1000000000000000000"), 18, RoundingMode.HALF_UP);
        }
    }
}