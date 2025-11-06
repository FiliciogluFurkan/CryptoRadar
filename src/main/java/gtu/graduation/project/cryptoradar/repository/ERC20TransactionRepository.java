package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.TransactionERC20TransferEntity;
import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ERC20TransactionRepository extends JpaRepository<TransactionERC20TransferEntity, UUID> { }