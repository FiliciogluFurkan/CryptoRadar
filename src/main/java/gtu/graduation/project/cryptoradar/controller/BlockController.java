package gtu.graduation.project.cryptoradar.controller;


import gtu.graduation.project.cryptoradar.entity.BlockEntity;
import gtu.graduation.project.cryptoradar.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class BlockController {

    private final BlockService service;

    @GetMapping
    public List<BlockEntity> listBlocks() {
        return service.listBlock();
    }
}
