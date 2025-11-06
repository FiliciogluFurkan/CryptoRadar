package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_native_transfers", indexes = {
        @Index(name = "idx_tx_hash", columnList = "hash"),
        @Index(name = "idx_tx_from", columnList = "fromAddress"),
        @Index(name = "idx_tx_to", columnList = "toAddress"),
        @Index(name = "idx_tx_block", columnList = "block_block_number"),
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionNativeTransferEntity {

    @Id
    @Column(nullable = false, unique = true, length = 66)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    private BlockEntity block;

    @Column(nullable = false)
    private Long nonce; // BIGINT

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(nullable = false, precision = 78)
    private BigInteger value; // keep full wei range

    @Column(name = "gas_price")
    private Long gasPrice; // BIGINT

    @Column(name = "gas_limit", nullable = false)
    private Long gasLimit; // BIGINT

    @Column(name = "max_fee_per_gas")
    private Long maxFeePerGas; // BIGINT

    @Column(name = "max_priority_fee_per_gas")
    private Long maxPriorityFeePerGas; // BIGINT

    @Column(name = "effective_fee_per_gas")
    private Long effectiveFeePerGas; // BIGINT

    @Column(name = "token", length = 10)
    @Enumerated(EnumType.STRING) // int storage instead of string
    private TokenType token;

    @Column(length = 3)
    private String type; // "0x0", "0x1", "0x2"

    @Column(name = "priority_fee_ratio", precision = 18, scale = 8)
    private BigDecimal priorityFeeRatio;

    @Column(name = "base_fee_ratio", precision = 18, scale = 8)
    private BigDecimal baseFeeRatio;

    @Column(name = "is_contract_interaction")
    private boolean isContractInteraction;

    @Column(name = "value_z_score")
    private Double valueZScore; // double enough

    @Column(name = "gas_z_score")
    private Double gasZScore;

    @Column(name = "priority_fee_percentile")
    private Double priorityFeePercentile;

    @Transient
    private List<TransactionERC20TransferEntity> transactions = new ArrayList<>();

    public TransactionNativeTransferEntity(BlockEntity block, String hash, BigInteger nonce, String fromAddress, String toAddress,
                                           BigInteger value, BigInteger gasPrice, BigInteger gasLimit, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas,
                                           TokenType token, String type, BigInteger baseFeePerGas, boolean isContractInteraction) {
        this.block = block;
        this.hash = hash;
        this.nonce = nonce.longValue();
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.value = value;
        this.gasPrice = gasPrice != null ? gasPrice.longValue() : 0;
        this.gasLimit = gasLimit != null ? gasLimit.longValue() : 0;
        this.maxFeePerGas = maxFeePerGas != null ? maxFeePerGas.longValue() : 0;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas != null ? maxPriorityFeePerGas.longValue() : 0;
        this.token = token;
        this.type = type != null ? type : "0x0";
        if(this.maxFeePerGas != 0 && this.maxPriorityFeePerGas != 0) {
            this.priorityFeeRatio = BigDecimal.valueOf(this.maxPriorityFeePerGas)
                    .divide(BigDecimal.valueOf(this.maxFeePerGas), 8, RoundingMode.HALF_DOWN);
        } else {
            this.priorityFeeRatio = BigDecimal.ZERO;
        }
        if(this.maxFeePerGas != 0 && baseFeePerGas != null && baseFeePerGas.compareTo(BigInteger.ZERO) > 0) {
            this.baseFeeRatio = BigDecimal.valueOf(this.maxFeePerGas)
                    .divide(new BigDecimal(baseFeePerGas), 8, RoundingMode.HALF_DOWN);
        } else {
            this.baseFeeRatio = BigDecimal.ZERO;
        }
        this.isContractInteraction = isContractInteraction;
        calculateEffectiveFee(baseFeePerGas);
    }

    public void calculateEffectiveFee(BigInteger baseFeePerGas) {
        if ("0x2".equals(type) && maxFeePerGas != 0 && maxPriorityFeePerGas != 0 && baseFeePerGas != null) {
            long basePlusPriority = baseFeePerGas.longValue() + maxPriorityFeePerGas;
            this.effectiveFeePerGas = Math.min(maxFeePerGas, basePlusPriority);
        } else {
            this.effectiveFeePerGas = gasPrice;
        }
    }
}