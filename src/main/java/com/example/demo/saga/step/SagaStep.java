package com.example.demo.saga.step;

/**
 * Contrato de um passo da Saga.
 *
 * Cada passo representa uma transação local em um único banco de dados.
 * Se qualquer passo falhar, todos os passos anteriores que já executaram
 * devem ser compensados via compensate().
 *
 * Comparativo com 2PC:
 * ┌──────────────────────┬──────────────────────────────────────────────────┐
 * │ 2PC                  │ Saga                                             │
 * ├──────────────────────┼──────────────────────────────────────────────────┤
 * │ Trava recursos (lock)│ Não trava — cada passo commita imediatamente     │
 * │ Rollback atômico     │ Compensa com transações inversas (undo)          │
 * │ Requer XA/driver XA  │ Funciona com qualquer banco/driver               │
 * │ Falha: bloqueio total│ Falha: estado parcial visível durante compensação│
 * └──────────────────────┴──────────────────────────────────────────────────┘
 *
 * @param <C> Tipo do contexto compartilhado entre os passos da saga
 */
public interface SagaStep<C> {

    /**
     * Executa a transação local deste passo.
     * Deve commitar imediatamente — sem espera por outros participantes.
     *
     * @param context Contexto da saga, contém dados produzidos pelos passos anteriores
     * @throws Exception se a execução falhar (dispara compensação nos passos anteriores)
     */
    void execute(C context) throws Exception;

    /**
     * Compensa (desfaz) o efeito do execute() caso um passo posterior falhe.
     *
     * A compensação NÃO é um rollback técnico de banco — é uma nova transação
     * local que inverte o efeito (ex: DELETE do registro inserido, ou INSERT
     * de registro de estorno).
     *
     * Compensações devem ser idempotentes: executar duas vezes não causa
     * efeito duplicado (importante para retentativas em caso de falha).
     *
     * @param context Contexto da saga com os dados necessários para desfazer
     */
    void compensate(C context);

    /**
     * Nome descritivo do passo para logging.
     */
    String name();
}
