package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockEntity {
    @Id
    private Long number;

    private String hash;
    private String parentHash;
    private Instant timestamp;
    private Integer txCount;
    private Long gasUsed;
    private Long gasLimit;

}
