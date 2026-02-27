package com.fintech.account.controller;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.dto.TransferRequestDto;
import com.fintech.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping()
    public ResponseEntity<AccountResponseDto> createAccount(@RequestBody @Valid AccountCreateDto accountCreateDto) {
        return ResponseEntity.ok(accountService.createAccount(accountCreateDto));
    }

    @GetMapping("/{document}")
    public ResponseEntity<AccountResponseDto> findAccount(@PathVariable String document) {
        return ResponseEntity.ok(accountService.getAccountByDocument(document));
    }

    @GetMapping()
    public ResponseEntity<Page<AccountResponseDto>> findAllAccount(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @PatchMapping("/{document}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable String document, @RequestParam BigDecimal amount) {
        accountService.deposit(amount, document);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{document}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String document, @RequestParam BigDecimal amount) {
        accountService.withdraw(amount, document);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@RequestBody @Valid TransferRequestDto request) {

        accountService.transfer(
                request.amount(),
                request.fromDocument(),
                request.toDocument()
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{document}/set_blocked")
    public ResponseEntity<Void> setBlocked(@PathVariable String document) {
        accountService.setBlocked(document);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{document}/set_active")
    public ResponseEntity<Void> setActive(@PathVariable String document) {
        accountService.setActive(document);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{document}/set_closed")
    public ResponseEntity<Void> setClosed(@PathVariable String document) {
        accountService.setClosed(document);
        return ResponseEntity.noContent().build();
    }
}
