package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.BlockCheckpoint;
import gtu.graduation.project.cryptoradar.entity.Checkpoint;
import gtu.graduation.project.cryptoradar.model.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockCheckpointRepository extends JpaRepository<BlockCheckpoint, NetworkType> { }