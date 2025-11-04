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

    @Column(name = "block_number", nullable = false)
    @Id
    private Long blockNumber;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "network_type")
    private NetworkType networkType;

    @Column(name = "block_status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private BlockStatus status;
}