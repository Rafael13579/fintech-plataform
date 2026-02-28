package com.fintech.account.service;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.exception.InvalidTransactionException;
import com.fintech.account.model.Account;
import com.fintech.account.model.AccountStatus;
import com.fintech.account.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponseDto createAccount(AccountCreateDto dto) {

        Account account = Account.builder()
                .document(dto.document())
                .holderName(dto.holderName())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);

        return mapToDto(saved);
    }

    public AccountResponseDto getAccountByDocument(String document) {
        return mapToDto(findAccountOrThrow(document));
    }

    public Page<AccountResponseDto> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deposit(BigDecimal amount, String document) {

        validateAmount(amount);

        Account account = findAccountOrThrow(document);
        validateAccountIsActive(account);

        account.setBalance(account.getBalance().add(amount));
    }

    @Transactional
    public void withdraw(BigDecimal amount, String document) {

        validateAmount(amount);

        Account account = findAccountOrThrow(document);
        validateAccountIsActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        account.setBalance(account.getBalance().subtract(amount));
    }

    @Transactional
    public void transfer(BigDecimal amount, String fromDocument, String toDocument) {

        validateAmount(amount);

        if (fromDocument.equals(toDocument)) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        Account sender = findAccountOrThrow(fromDocument);
        Account receiver = findAccountOrThrow(toDocument);

        validateAccountIsActive(sender);
        validateAccountIsActive(receiver);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
    }

    @Transactional
    public void setBlocked(String document) {
        Account account = findAccountOrThrow(document);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Closed account cannot be modified");
        }

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new InvalidTransactionException("Account is already blocked");
        }

        account.setStatus(AccountStatus.BLOCKED);
    }

    @Transactional
    public void setActive(String document) {
        Account account = findAccountOrThrow(document);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Closed account cannot be reactivated");
        }

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Account is already active");
        }

        account.setStatus(AccountStatus.ACTIVE);
    }

    @Transactional
    public void setClosed(String document) {
        Account account = findAccountOrThrow(document);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Account is already closed");
        }

        account.setStatus(AccountStatus.CLOSED);
    }

    private Account findAccountOrThrow(String document) {
        return accountRepository.findByDocument(document)
                .orElseThrow(() -> new AccountNotFoundException(document));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }
    }

    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(
                    "Account is not active. Current status: " + account.getStatus()
            );
        }
    }

    private AccountResponseDto mapToDto(Account account) {
        return new AccountResponseDto(
                account.getId(),
                account.getDocument(),
                account.getHolderName(),
                account.getBalance(),
                account.getStatus()
        );
    }
}