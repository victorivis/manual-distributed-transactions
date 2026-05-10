package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação Spring Boot.
 *
 * Demonstra o padrão Outbox para garantir consistência eventual
 * entre dois bancos de dados (H2 e MongoDB) sem uso de 2PC/XA.
 *
 * @EnableScheduling é necessário para o OutboxRelay funcionar.
 *
 * @author DAC
 * @version 3.0
 */
@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
