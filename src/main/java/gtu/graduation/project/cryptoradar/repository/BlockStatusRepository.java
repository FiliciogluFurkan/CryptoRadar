package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.BlockCheckpoint;
import gtu.graduation.project.cryptoradar.entity.BlockStatusEntity;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

public interface BlockStatusRepository extends JpaRepository<BlockStatusEntity, UUID> {

    Optional<BlockStatusEntity> findByNetworkTypeAndBlockNumber(NetworkType networkType, BigInteger blockNumber);
}