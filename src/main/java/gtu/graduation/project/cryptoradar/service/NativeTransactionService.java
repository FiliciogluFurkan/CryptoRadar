package gtu.graduation.project.cryptoradar.service;

import gtu.graduation.project.cryptoradar.entity.TransactionNativeTransferEntity;
import gtu.graduation.project.cryptoradar.repository.NativeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class NativeTransactionService {

    private final NativeTransactionRepository nativeTransactionRepository;

    public List<TransactionNativeTransferEntity> listFirst10Transactions() {
        return nativeTransactionRepository.findFirstN(10);
    }
}
