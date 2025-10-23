package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, String> { }