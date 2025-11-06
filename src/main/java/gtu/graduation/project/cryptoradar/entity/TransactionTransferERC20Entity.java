package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(name = "transaction_transfers", indexes = {@Index(name = "idx_tx_hash", columnList = "hash"), @Index(name = "idx_tx_from", columnList = "fromAddress"), @Index(name = "idx_tx_to", columnList = "toAddress"),})
@Getter
@Setter
@NoArgsConstructor
public class TransactionTransferERC20Entity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TransactionTransferEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    private BlockEntity block;

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(nullable = false, precision = 78)
    private BigInteger value;

    @Column(name = "token")
    @Enumerated(EnumType.STRING)
    private TokenType token;

    @Column(length = 10)
    private String type; // "0x0", "0x1", "0x2" (legacy, EIP-2930, EIP-1559)

    public TransactionTransferERC20Entity(BlockEntity block, String fromAddress, String toAddress, BigInteger value, BigInteger gasPrice, BigInteger gasLimit, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, TokenType token, String type, BigInteger baseFeePerGas, boolean isContractInteraction, BigInteger tokenValue) {
        this.block = block;
        this.transaction = transaction;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.value = value;
        this.token = token;
        this.type = type;
    }
}