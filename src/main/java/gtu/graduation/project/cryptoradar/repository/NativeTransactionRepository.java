package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NativeTransactionRepository extends JpaRepository<TransactionNativeTransferEntity, String> { }