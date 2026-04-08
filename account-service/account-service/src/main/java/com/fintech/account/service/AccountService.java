package com.fintech.account.service;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.event.TransferCompletedEvent;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.exception.InvalidTransactionException;
import com.fintech.account.model.*;
import com.fintech.account.outbox.model.OutboxEvent;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.outbox.repository.OutboxRepository;
import com.fintech.account.repository.TransactionRepository;
import com.fintech.account.repository.TransactionRequestRepository;
import com.fintech.account.transaction.model.Transaction;
import com.fintech.account.transaction.model.TransactionStatus;
import com.fintech.account.transaction.request.RequestStatus;
import com.fintech.account.transaction.request.TransactionRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;


import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AccountRepository accountRepository;
    private final TransactionRequestRepository transactionRequestRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxRepository outboxRepository;

    public AccountService(AccountRepository accountRepository, OutboxRepository outboxRepository, TransactionRepository transactionRepository, TransactionRequestRepository transactionRequestRepository) {
        this.accountRepository = accountRepository;
        this.transactionRequestRepository = transactionRequestRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public AccountResponseDto createAccount(AccountCreateDto dto) {

        Account account = Account.builder()
                .document(dto.document())
                .holderName(dto.holderName())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        Account saved = accountRepository.save(account);

        return mapToDto(saved);
    }

    public AccountResponseDto getAccountById(UUID accountId) {
        return mapToDto(findAccountOrThrow(accountId));
    }

    public Page<AccountResponseDto> findAll(Pageable pageable) {
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
                .status(RequestStatus.APPROVED)
                .transactionId(transaction.getId())
                .createdAt(Instant.now())
                .build();

        transactionRequestRepository.save(request);


        TransferCompletedEvent event = new TransferCompletedEvent(
                transaction.getId().toString(),
                fromAccountId.toString(),
                toAccountId.toString(),
                amount.toString()
        );


        String payload;


        try {
            payload = MAPPER.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        OutboxEvent outbox = OutboxEvent.builder()
                .eventType("TransferCompletedEvent")
                .payload(payload)
                .published(false)
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(outbox);
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
        List<Transaction> transactions = transactionRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedAt(accountId, accountId);

        return transactions != null ? transactions : Collections.emptyList();
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
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