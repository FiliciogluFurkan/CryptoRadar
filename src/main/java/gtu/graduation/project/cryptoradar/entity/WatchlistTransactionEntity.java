package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "watchlist_transactions", indexes = {
        @Index(name = "idx_wt_hash", columnList = "hash"),
        @Index(name = "idx_wt_from", columnList = "fromAddress"),
        @Index(name = "idx_wt_to", columnList = "toAddress"),
        @Index(name = "idx_wt_address", columnList = "watchedAddress"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 66)
    private String hash;

    @Column(nullable = false, length = 42)
    private String watchedAddress; // Hangi watchlist adresi için

    @Column(nullable = false, length = 42)
    private String fromAddress;

    @Column(length = 42)
    private String toAddress;

    @Column(precision = 38, scale = 18)
    private BigDecimal value; // ETH cinsinden (Alchemy'den öyle geliyor)

    @Column(length = 20)
    private String asset; // ETH, USDT, etc.

    @Column(length = 20)
    private String category; // external, erc20, erc721, etc.

    @Column
    private Instant blockTimestamp;

    @Column(length = 20)
    private String blockNum;

    @Column
    private boolean isOutgoing; // true = sent, false = received
}
