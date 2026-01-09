package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.FraudCheckHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudCheckHistoryRepository extends JpaRepository<FraudCheckHistoryEntity, Long> {

    List<FraudCheckHistoryEntity> findByAddressOrderByCheckedAtDesc(String address);

    Optional<FraudCheckHistoryEntity> findTopByAddressOrderByCheckedAtDesc(String address);

    Page<FraudCheckHistoryEntity> findAllByOrderByCheckedAtDesc(Pageable pageable);

    List<FraudCheckHistoryEntity> findByCheckedAtBetweenOrderByCheckedAtDesc(Instant start, Instant end);

    @Query("SELECT f FROM FraudCheckHistoryEntity f WHERE f.isFraud = true ORDER BY f.checkedAt DESC")
    List<FraudCheckHistoryEntity> findAllFraudulent();

    long countByIsFraud(Boolean isFraud);
}
