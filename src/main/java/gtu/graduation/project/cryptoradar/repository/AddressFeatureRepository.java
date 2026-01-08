package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.AddressFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressFeatureRepository extends JpaRepository<AddressFeatureEntity, String> {

    List<AddressFeatureEntity> findByLastUpdatedAfter(Instant timestamp);

    @Query("SELECT a FROM AddressFeatureEntity a ORDER BY a.totalTransactions DESC")
    List<AddressFeatureEntity> findTopActiveAddresses();

    @Query("SELECT a FROM AddressFeatureEntity a WHERE a.totalTransactions >= :minTx")
    List<AddressFeatureEntity> findAddressesWithMinTransactions(@Param("minTx") Long minTx);

    /**
     * Pessimistic lock ile address feature getir - concurrent update'leri önler
     */
    @Query("SELECT a FROM AddressFeatureEntity a WHERE a.address = :address")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<AddressFeatureEntity> findByIdWithLock(@Param("address") String address);

    /**
     * Native UPSERT - kayıt yoksa oluştur, varsa hiçbir şey yapma
     * Race condition'ı database seviyesinde önler
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO address_features (address, created_at, last_updated, update_count, 
            sent_tnx, received_tnx, total_transactions, unique_sent_to_addresses, 
            unique_received_from_addresses, number_of_created_contracts, total_erc20tnxs,
            min_val_sent, max_val_sent, avg_val_sent, total_ether_sent,
            min_value_received, max_value_received, avg_val_received, total_ether_received,
            total_ether_balance, min_value_sent_to_contract, max_val_sent_to_contract,
            avg_value_sent_to_contract, total_ether_sent_contracts,
            erc20_total_ether_received, erc20_total_ether_sent,
            time_diff_first_last_mins, avg_min_between_sent_tnx, avg_min_between_received_tnx)
        VALUES (:address, :now, :now, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ON CONFLICT (address) DO NOTHING
        """, nativeQuery = true)
    void insertIfNotExists(@Param("address") String address, @Param("now") Instant now);

}
