package com.example.demo.saga;

import com.example.demo.dao.UserDao;
import com.example.demo.entity.UserEntity;
import com.example.demo.saga.orchestrator.SagaException;
import com.example.demo.saga.orchestrator.SagaOrchestrator;
import com.example.demo.saga.orchestrator.UserSagaService;
import com.example.demo.saga.step.SaveToSqlStep;
import com.example.demo.saga.step.UserSagaContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do padrão Saga.
 *
 * Cada teste torna visível no log o fluxo da saga:
 *
 *   [SAGA][CriarUsuario] Iniciando saga...
 *   [SAGA][SaveToSqlStep]   execute() → INSERT commitado, id=1
 *   [SAGA][SaveToMongoStep] execute() → insertOne commitado, mongoId=1
 *   [SAGA][CriarUsuario] Saga concluída com sucesso
 *
 * Comparativo com os testes do 2PC (UserXaTest):
 * ┌─────────────────────────────┬─────────────────────────────────────────┐
 * │ UserXaTest (2PC)            │ UserSagaTest (Saga)                     │
 * ├─────────────────────────────┼─────────────────────────────────────────┤
 * │ coordinator.begin()         │ sagaService.createUser()                │
 * │ coordinator.enlist()        │ (implícito em cada SagaStep)            │
 * │ coordinator.prepare() [F1]  │ (não existe — sem votação)              │
 * │ coordinator.commit()  [F2]  │ (implícito após todos os passos)        │
 * │ coordinator.rollback()      │ SagaOrchestrator.compensateAll()        │
 * └─────────────────────────────┴─────────────────────────────────────────┘
 */
@SpringBootTest
class UserSagaTest {

    @Autowired private UserSagaService sagaService;
    @Autowired private UserDao         userDao;       // lê do H2 (app.dao.impl=jpa)
    @Autowired private SaveToSqlStep   saveToSqlStep; // usado no cenário de falha simulada

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 1: fluxo feliz — ambos os bancos persistem com sucesso
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void devePersistirNosDoisBancosComSucesso() throws SagaException {
        int antes = userDao.findAll().size();

        sagaService.createUser(new UserEntity("Alice Saga", "alice@saga.com"));

        int depois = userDao.findAll().size();
        assertEquals(antes + 1, depois,
                "Após saga bem-sucedida, o usuário deve estar no banco SQL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 2: falha no passo 2 — compensação do passo 1 deve ser executada
    //
    // Simula o MongoDB falhando: injeta um passo falso que lança exceção.
    // O orquestrador deve compensar o SaveToSqlStep (deletar o INSERT do H2).
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deveCompensarSqlQuandoMongoFalha() {
        int antes = userDao.findAll().size();

        UserEntity user = new UserEntity("Bob Saga", "bob@saga.com");
        UserSagaContext ctx = new UserSagaContext(user);

        SagaOrchestrator<UserSagaContext> saga =
                new SagaOrchestrator<UserSagaContext>("CriarUsuario-FalhaSimulada")
                        .addStep(saveToSqlStep)
                        .addStep(new com.example.demo.saga.step.SagaStep<>() {
                            @Override
                            public void execute(UserSagaContext c) throws Exception {
                                throw new RuntimeException("MongoDB indisponível (simulado)");
                            }
                            @Override
                            public void compensate(UserSagaContext c) { /* nada a compensar */ }
                            @Override
                            public String name() { return "MongoStep-Falso"; }
                        });

        assertThrows(SagaException.class, () -> saga.run(ctx),
                "Deve lançar SagaException quando o passo 2 falha");

        int depois = userDao.findAll().size();
        assertEquals(antes, depois,
                "Após compensação, o INSERT do SQL deve ter sido desfeito");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 3: falha no passo 1 — nenhum banco é afetado
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void naoDeveAfestarNenhumBancosePassoUmFalha() {
        int antes = userDao.findAll().size();

        UserSagaContext ctx = new UserSagaContext(new UserEntity("Carol Saga", "carol@saga.com"));

        SagaOrchestrator<UserSagaContext> saga =
                new SagaOrchestrator<UserSagaContext>("CriarUsuario-FalhaNoPasso1")
                        .addStep(new com.example.demo.saga.step.SagaStep<>() {
                            @Override
                            public void execute(UserSagaContext c) throws Exception {
                                throw new RuntimeException("SQL indisponível (simulado)");
                            }
                            @Override
                            public void compensate(UserSagaContext c) { /* nada a compensar */ }
                            @Override
                            public String name() { return "SqlStep-Falso"; }
                        });

        assertThrows(SagaException.class, () -> saga.run(ctx));

        int depois = userDao.findAll().size();
        assertEquals(antes, depois,
                "Nenhum dado deve ter sido persistido se o passo 1 falhou");
    }
}
