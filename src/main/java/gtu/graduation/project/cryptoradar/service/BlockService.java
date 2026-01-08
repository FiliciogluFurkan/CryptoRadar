package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
@Service
public class BlockService {
    private final BlockRepository blockRepository;

    public List<BlockEntity> listBlock() {
        Pageable first100 = PageRequest.of(0, 10);
        return blockRepository.findAll(first100).getContent();
    }
}