package com.example.demo.saga.orchestrator;

/**
 * Exceção lançada pelo SagaOrchestrator quando um passo falha.
 *
 * Indica que a saga foi abortada e as compensações dos passos
 * anteriores foram disparadas (ou tentadas, caso tenham falhado).
 */
public class SagaException extends Exception {

    public SagaException(String message, Throwable cause) {
        super(message, cause);
    }
}
