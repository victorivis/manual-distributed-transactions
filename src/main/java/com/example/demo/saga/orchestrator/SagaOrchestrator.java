package com.example.demo.saga.orchestrator;

import com.example.demo.saga.step.SagaStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquestrador da Saga.
 *
 * Responsável por executar os passos em ordem e, em caso de falha,
 * disparar as compensações em ordem inversa (último passo executado
 * é o primeiro a ser compensado — pilha LIFO).
 *
 * Fluxo feliz (todos os passos bem-sucedidos):
 *
 *   execute(passo1) → commit local BD1
 *   execute(passo2) → commit local BD2
 *   Saga concluída com sucesso ✓
 *
 * Fluxo de falha (passo2 lança exceção):
 *
 *   execute(passo1) → commit local BD1
 *   execute(passo2) → FALHA ✗
 *   compensate(passo1) → desfaz BD1   ← ordem inversa
 *   SagaException lançada
 *
 * Comparativo com XATransactionCoordinator (2PC):
 * ┌─────────────────────┬──────────────────────────────────────────────────┐
 * │ XATransactionCoord. │ SagaOrchestrator                                 │
 * ├─────────────────────┼──────────────────────────────────────────────────┤
 * │ begin()             │ run() inicia                                     │
 * │ enlist(xaConn)      │ addStep(step)                                    │
 * │ delistAll()         │ (implícito — cada passo commita em execute())    │
 * │ prepare() [Fase 1]  │ (não existe — Saga não vota)                     │
 * │ commit()  [Fase 2]  │ todos os execute() bem-sucedidos                 │
 * │ rollback()          │ compensate() em ordem inversa                    │
 * └─────────────────────┴──────────────────────────────────────────────────┘
 *
 * @param <C> Tipo do contexto compartilhado entre os passos
 */
public class SagaOrchestrator<C> {

    private final List<SagaStep<C>> steps = new ArrayList<>();
    private final String sagaName;

    public SagaOrchestrator(String sagaName) {
        this.sagaName = sagaName;
    }

    /**
     * Registra um passo na saga. Os passos são executados na ordem de registro.
     */
    public SagaOrchestrator<C> addStep(SagaStep<C> step) {
        steps.add(step);
        return this;
    }

    /**
     * Executa todos os passos em ordem.
     *
     * Se qualquer passo falhar, todos os passos anteriores que já executaram
     * com sucesso são compensados em ordem inversa.
     *
     * @param context Contexto compartilhado da saga
     * @throws SagaException se algum passo falhar (após compensações)
     */
    public void run(C context) throws SagaException {
        List<SagaStep<C>> executed = new ArrayList<>();

        log("══════════════════════════════════════════");
        log("Iniciando saga: " + sagaName);
        log("══════════════════════════════════════════");

        for (SagaStep<C> step : steps) {
            try {
                log("→ executando: " + step.name());
                step.execute(context);
                executed.add(step);
                log("✓ concluído:  " + step.name());
            } catch (Exception e) {
                log("✗ FALHA em:   " + step.name() + " — " + e.getMessage());
                log("Iniciando compensações em ordem inversa...");
                compensateAll(executed, context);
                throw new SagaException(
                        "Saga '" + sagaName + "' falhou em '" + step.name() + "'", e);
            }
        }

        log("══════════════════════════════════════════");
        log("Saga concluída com sucesso: " + sagaName);
        log("══════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compensação em ordem LIFO (último a executar → primeiro a compensar)
    // ─────────────────────────────────────────────────────────────────────────

    private void compensateAll(List<SagaStep<C>> executed, C context) {
        for (int i = executed.size() - 1; i >= 0; i--) {
            SagaStep<C> step = executed.get(i);
            try {
                log("↩ compensando: " + step.name());
                step.compensate(context);
                log("✓ compensado:  " + step.name());
            } catch (Exception e) {
                // Falha na compensação é crítica: dado ficou parcialmente inconsistente.
                // Em produção: alertar via monitoramento e registrar em dead-letter table.
                log("✗ FALHA na compensação de: " + step.name() + " — " + e.getMessage());
            }
        }
    }

    private void log(String msg) {
        System.out.println("[SAGA][" + sagaName + "] " + msg);
    }
}
