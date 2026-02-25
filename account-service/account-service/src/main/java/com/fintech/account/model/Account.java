package com.fintech.account.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
