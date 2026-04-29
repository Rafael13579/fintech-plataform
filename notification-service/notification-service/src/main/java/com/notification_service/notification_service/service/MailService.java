package com.notification_service.notification_service.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMail(String fromAccountId, BigDecimal amount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom("nao-responda@fintech.com");
        mailMessage.setTo("cliente@teste.com");

        mailMessage.setSubject("Transferência recebida");
        mailMessage.setText("Transferencia recebida da conta " + fromAccountId + " no valor de R$" + amount);

        mailSender.send(mailMessage);

        System.out.println("Email disparado com sucesso");
    }
}
