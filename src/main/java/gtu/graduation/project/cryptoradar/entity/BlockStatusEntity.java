package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.NetworkType;
import jakarta.annotation.Generated;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "block_status",
        indexes = {
                @Index(name = "idx_network_type_block_number", columnList = "network_type, block_number")
        }
)
public class BlockStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "network_type")
    private NetworkType networkType;

    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;

    @Column(name = "status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Status status;
}

enum Status {
    PROCESSED,
    FAILED
}