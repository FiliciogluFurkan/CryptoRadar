package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.TransactionTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionTransferEntity, String> { }