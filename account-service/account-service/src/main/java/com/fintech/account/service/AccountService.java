package com.fintech.account.service;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.model.Account;
import com.fintech.account.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Service
public class AccountService {

    private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponseDto createAccount(AccountCreateDto dto) {
        Account acc = Account.builder()
                .document(dto.document())
                .balance(dto.balance())
                .holderName(dto.HolderName())
                .status(dto.status())
                .build();

        Account saved = accountRepository.save(acc);

        return mapToAccountResponseDto(saved);
    }

    public AccountResponseDto getAccountByDocument(String document) {
        Account acc = accountRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return mapToAccountResponseDto(acc);
    }

    public Page<AccountResponseDto> listAccounts(Pageable pageable) {
        Page<Account> accounts = accountRepository.listAccounts(pageable);

        return accounts.map(this::mapToAccountResponseDto);
    }

    public AccountResponseDto mapToAccountResponseDto(Account acc) {

        return new AccountResponseDto(
                acc.getId(),
                acc.getDocument(),
                acc.getBalance(),
                acc.getHolderName(),
                acc.getStatus());
    }
}
