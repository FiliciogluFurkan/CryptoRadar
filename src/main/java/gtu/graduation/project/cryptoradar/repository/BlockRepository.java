package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, Long> { }