package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.Id;

@Entity
@Table(name = "checkpoints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Checkpoint {

    @Id
    private String id;
    private Long lastProcessedBlock;

}
