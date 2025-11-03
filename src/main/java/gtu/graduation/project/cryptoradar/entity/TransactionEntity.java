package gtu.graduation.project.cryptoradar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    private String hash;

    private Long blockNumber;
    private String fromAddress;
    private String toAddress;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String value;

    private Long gas;
    private Long gasPrice;
    private Boolean suspicious = false;
}