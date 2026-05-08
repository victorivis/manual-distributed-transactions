package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação Spring Boot.
 *
 * Demonstra o padrão Saga para garantir consistência eventual
 * entre dois bancos de dados (H2 e MongoDB) sem uso de 2PC/XA.
 *
 * @author DAC
 * @version 2.0
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
