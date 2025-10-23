package gtu.graduation.project.cryptoradar.repository;

import gtu.graduation.project.cryptoradar.entity.TxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxRepository extends JpaRepository<TxEntity, String> { }