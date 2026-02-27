package com.fintech.account.controller;

import com.fintech.account.dto.AccountCreateDto;
import com.fintech.account.dto.AccountResponseDto;
import com.fintech.account.dto.TransferRequestDto;
import com.fintech.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Account", description = "Operações relacionadas ao gerenciamento de contas bancárias")
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Criar nova conta",
            description = "Cria uma nova conta bancária com saldo inicial."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Conta já existente")
    })
    @PostMapping()
    public ResponseEntity<AccountResponseDto> createAccount(@RequestBody @Valid AccountCreateDto accountCreateDto) {
        return ResponseEntity.ok(accountService.createAccount(accountCreateDto));
    }

    @Operation(
            summary = "Buscar conta por documento",
            description = "Retorna os dados de uma conta a partir do documento (CPF/CNPJ)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conta encontrada"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    @GetMapping("/{document}")
    public ResponseEntity<AccountResponseDto> findAccount(
            @Parameter(description = "Documento do titular da conta", example = "12345678900")
            @PathVariable String document) {
        return ResponseEntity.ok(accountService.getAccountByDocument(document));
    }

    @Operation(
            summary = "Listar contas",
            description = "Retorna uma lista paginada de contas cadastradas."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping()
    public ResponseEntity<Page<AccountResponseDto>> findAllAccount(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @Operation(
            summary = "Depositar valor",
            description = "Realiza um depósito na conta informada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Depósito realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor inválido"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    @PatchMapping("/{document}/deposit")
    public ResponseEntity<Void> deposit(
            @Parameter(description = "Documento da conta", example = "12345678900")
            @PathVariable String document,
            @Parameter(description = "Valor do depósito", example = "100.00")
            @RequestParam BigDecimal amount) {
        accountService.deposit(amount, document);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Realizar saque",
            description = "Efetua um saque da conta informada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saque realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente ou valor inválido"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    @PatchMapping("/{document}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String document, @RequestParam BigDecimal amount) {
        accountService.withdraw(amount, document);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Transferência entre contas",
            description = "Realiza transferência de valores entre duas contas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente ou dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Conta origem ou destino não encontrada")
    })
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@RequestBody @Valid TransferRequestDto request, @RequestParam String fromDocument) {

        accountService.transfer(
                request.amount(),
                request.fromDocument(),
                request.toDocument()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bloquear conta")
    @ApiResponse(responseCode = "204", description = "Conta bloqueada com sucesso")
    @PatchMapping("/{document}/set_blocked")
    public ResponseEntity<Void> setBlocked(@PathVariable String document) {
        accountService.setBlocked(document);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar conta")
    @ApiResponse(responseCode = "204", description = "Conta ativada com sucesso")
    @PatchMapping("/{document}/set_active")
    public ResponseEntity<Void> setActive(@PathVariable String document) {
        accountService.setActive(document);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Encerrar conta")
    @ApiResponse(responseCode = "204", description = "Conta encerrada com sucesso")
    @PatchMapping("/{document}/set_closed")
    public ResponseEntity<Void> setClosed(@PathVariable String document) {
        accountService.setClosed(document);
        return ResponseEntity.noContent().build();
    }
}
