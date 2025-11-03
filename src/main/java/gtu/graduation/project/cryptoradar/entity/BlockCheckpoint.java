package gtu.graduation.project.cryptoradar.entity;

import gtu.graduation.project.cryptoradar.model.NetworkType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockCheckpoint {

    @Id
    @Enumerated(value = EnumType.STRING)
    @Column(name = "network_type")
    private NetworkType networkType;

    @Column(name = "last_processed_block", nullable = false)
    private Long lastProcessedBlock;

}
