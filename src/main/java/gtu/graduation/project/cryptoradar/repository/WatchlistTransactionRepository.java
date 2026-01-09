package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.WatchlistTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistTransactionRepository extends JpaRepository<WatchlistTransactionEntity, Long> {

    List<WatchlistTransactionEntity> findByWatchedAddressOrderByBlockTimestampDesc(String watchedAddress);

    List<WatchlistTransactionEntity> findTop20ByWatchedAddressOrderByBlockTimestampDesc(String watchedAddress);

    void deleteByWatchedAddress(String watchedAddress);

    boolean existsByWatchedAddress(String watchedAddress);
}
