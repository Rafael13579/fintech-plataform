package com.fintech.account.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String document) {
        super("Account not found with document: " + document);
    }
}
