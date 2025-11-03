package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, Long> { }