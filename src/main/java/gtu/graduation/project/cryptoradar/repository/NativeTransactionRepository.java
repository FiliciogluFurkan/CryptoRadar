package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NativeTransactionRepository extends JpaRepository<TransactionNativeTransferEntity, String> {

    @Query(value = "SELECT * FROM transaction_native_transfers ORDER BY hash ASC LIMIT :limit", nativeQuery = true)
    List<TransactionNativeTransferEntity> findFirstN(@Param("limit") int limit);


    List<TransactionNativeTransferEntity> findByFromAddress(String fromAddress);

    List<TransactionNativeTransferEntity> findByToAddress(String toAddress);

    @Query("SELECT t FROM TransactionNativeTransferEntity t WHERE t.fromAddress = :address OR t.toAddress = :address")
    List<TransactionNativeTransferEntity> findAllByAddress(@Param("address") String address);

    @Query("SELECT COUNT(t) FROM TransactionNativeTransferEntity t WHERE t.fromAddress = :address")
    Long countByFromAddress(@Param("address") String address);

    @Query("SELECT COUNT(t) FROM TransactionNativeTransferEntity t WHERE t.toAddress = :address")
    Long countByToAddress(@Param("address") String address);

}
