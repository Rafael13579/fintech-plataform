package com.fintech.account.service;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.dto.TransferCompletedEvent;
import com.fintech.account.dto.TransferRequestDto;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.exception.InvalidTransactionException;
import com.fintech.account.model.*;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.repository.TransactionRepository;
import com.fintech.account.repository.TransactionRequestRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRequestRepository transactionRequestRepository;
    private final TransactionRepository transactionRepository;
    private final TransferEventProducer eventProducer;
    private final TransferEventProducer transferEventProducer;

    public AccountService(AccountRepository accountRepository, TransferEventProducer eventProducer , TransactionRepository transactionRepository, TransactionRequestRepository transactionRequestRepository, TransferEventProducer transferEventProducer) {
        this.accountRepository = accountRepository;
        this.transactionRequestRepository = transactionRequestRepository;
        this.transactionRepository = transactionRepository;
        this.eventProducer = eventProducer;
        this.transferEventProducer = transferEventProducer;
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

    public AccountResponseDto getAccountById(UUID accountId) {
        return mapToDto(findAccountOrThrow(accountId));
    }

    public Page<AccountResponseDto> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void deposit(BigDecimal amount, UUID accountId) {

        validateAmount(amount);

        Account account = findAccountOrThrow(accountId);
        validateAccountIsActive(account);

        account.setBalance(account.getBalance().add(amount));
    }

    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void withdraw(BigDecimal amount, UUID accountId) {

        validateAmount(amount);

        Account account = findAccountOrThrow(accountId);
        validateAccountIsActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        account.setBalance(account.getBalance().subtract(amount));
    }

    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void transfer(String idempotencyKey, BigDecimal amount, UUID fromAccountId, UUID toAccountId) {

        Optional<TransactionRequest> existing = transactionRequestRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return;
        }

        validateAmount(amount);

        Account sender = findAccountOrThrow(fromAccountId);
        Account receiver = findAccountOrThrow(toAccountId);

        validateAccountIsActive(sender);
        validateAccountIsActive(receiver);

        Transaction transaction = Transaction.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .createdAt(Instant.now())
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);

        if (sender.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            throw new InsufficientBalanceException();
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        transaction.setCompletedAt(Instant.now());
        transaction.setStatus(TransactionStatus.COMPLETED);

        TransactionRequest request = TransactionRequest.builder()
                .idempotencyKey(idempotencyKey)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .createdAt(Instant.now())
                .build();

        transactionRequestRepository.save(request);


        TransferCompletedEvent event = new TransferCompletedEvent(
                transaction.getId().toString(),
                fromAccountId.toString(),
                toAccountId.toString(),
                amount.toString()
        );

        transferEventProducer.publish(event);
    }

    @Transactional
    public void setBlocked(UUID accountId) {
        Account account = findAccountOrThrow(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Closed account cannot be modified");
        }

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new InvalidTransactionException("Account is already blocked");
        }

        account.setStatus(AccountStatus.BLOCKED);
    }

    @Transactional
    public void setActive(UUID accountId) {
        Account account = findAccountOrThrow(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Closed account cannot be reactivated");
        }

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Account is already active");
        }

        account.setStatus(AccountStatus.ACTIVE);
    }

    @Transactional
    public void setClosed(UUID  accountId) {
        Account account = findAccountOrThrow(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Account is already closed");
        }

        account.setStatus(AccountStatus.CLOSED);
    }

    public List<Transaction> findAllByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAt(accountId).orElse(null);
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
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