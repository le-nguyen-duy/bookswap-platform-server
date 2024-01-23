package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Notification;
import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import com.example.bookswapplatform.entity.Payment.Transaction.TransactionType;
import com.example.bookswapplatform.entity.Payment.UserWallet;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.TransactionRepository;
import com.example.bookswapplatform.repository.UserRepository;
import com.example.bookswapplatform.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    @Override
    public ResponseEntity<BaseResponseDTO> getAllTransaction(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        String email = user.getEmail();
        UserWallet userWallet = user.getUserWallet();

        List<TransactionType> transactionTypes = Arrays.asList(TransactionType.FEE,
                TransactionType.SEND,
                TransactionType.ADD,
                TransactionType.RECEIVE);
        List<Transaction> transactions = transactionRepository.findAllByCreateByAndTransactionType(email, transactionTypes);


        transactions.sort(Comparator.comparing(Transaction::getCreateDate).reversed());
        if (transactions.isEmpty()) {
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null, null));
        } else {
            List<TransactionGeneralDTO> transactionGeneralDTOS = new ArrayList<>();
            for (Transaction transaction : transactions
                 ) {
                transactionGeneralDTOS.add(convertToTransactionGeneralDTO(transaction));
            }
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null, transactionGeneralDTOS));
        }
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getDetailTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found!"));

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null, convertToTransactionDTO(transaction)));
    }

    public TransactionGeneralDTO convertToTransactionGeneralDTO(Transaction transaction) {
        if(transaction == null) {
            return null;
        }
        return modelMapper.map(transaction, TransactionGeneralDTO.class);
    }

    public TransactionDTO convertToTransactionDTO(Transaction transaction) {
        if(transaction == null) {
            return null;
        }

        return modelMapper.map(transaction, TransactionDTO.class);
    }
}
