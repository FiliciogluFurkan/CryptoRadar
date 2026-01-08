package gtu.graduation.project.cryptoradar.controller;

import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import gtu.graduation.project.cryptoradar.service.NativeTransactionService;
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
public class NativeTransactionController {

    private final NativeTransactionService service;

    @GetMapping("/native")
    public List<TransactionNativeTransferEntity> listNativeTransactions() {
        return service.listFirst10Transactions();
    }
}
