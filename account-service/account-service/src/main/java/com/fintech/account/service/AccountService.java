package com.fintech.account.service;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.exception.InvalidTransactionException;
import com.fintech.account.model.Account;
import com.fintech.account.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


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
                .orElseThrow(() -> new AccountNotFoundException(document));

        return mapToAccountResponseDto(acc);
    }

    public Page<AccountResponseDto> listAccounts(Pageable pageable) {
        Page<Account> accounts = accountRepository.listAccounts(pageable);

        return accounts.map(this::mapToAccountResponseDto);
    }

    @Transactional
    public void deposit(BigDecimal amount, String document) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account acc = accountRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        acc.setBalance(acc.getBalance().add(amount));
    }

    @Transactional
    public void withdraw(BigDecimal amount, String document) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be greater than zero");
        }

        Account acc = accountRepository.findByDocument(document)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (acc.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        acc.setBalance(acc.getBalance().subtract(amount));
    }

    @Transactional
    public void transfer(BigDecimal amount, String fromDocument, String toDocument) {

        if (fromDocument.equals(toDocument)) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        Account sender = accountRepository.findByDocument(fromDocument)
                .orElseThrow(() -> new AccountNotFoundException(fromDocument));

        Account receiver = accountRepository.findByDocument(toDocument)
                .orElseThrow(() -> new AccountNotFoundException(toDocument));

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
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
