package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(name = "transaction_erc20_transfers", indexes = {
        @Index(name = "idx_erc20_tx_from", columnList = "fromAddress"),
        @Index(name = "idx_erc20_tx_to", columnList = "toAddress"),
        @Index(name = "idx_erc20_transaction", columnList = "transaction_hash"),
        @Index(name = "idx_erc20_block", columnList = "block_block_number"),
})
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class TransactionERC20TransferEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TransactionNativeTransferEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    private BlockEntity block;

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(nullable = false, precision = 78)
    private BigInteger value;

    @Column(name = "token", length = 10)
    @Enumerated(EnumType.STRING)
    private TokenType token;

    @Column(length = 3)
    private String type; // "0x0", "0x1", "0x2" (legacy, EIP-2930, EIP-1559)

    public TransactionERC20TransferEntity(BlockEntity block, String fromAddress, String toAddress, BigInteger value, TokenType token, String type, TransactionNativeTransferEntity transaction) {
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