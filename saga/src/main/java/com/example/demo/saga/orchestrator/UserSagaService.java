package com.example.demo.saga.orchestrator;

import com.example.demo.entity.UserEntity;
import com.example.demo.saga.step.SaveToMongoStep;
import com.example.demo.saga.step.SaveToSqlStep;
import com.example.demo.saga.step.UserSagaContext;
import org.springframework.stereotype.Service;

/**
 * Serviço que monta e executa a Saga de criação de usuário em dois bancos.
 *
 * Substitui o fluxo begin/enlist/delistAll/prepare/commit do 2PC por uma
 * sequência de passos locais com compensação.
 *
 * Fluxo de sucesso:
 *   SaveToSqlStep.execute()   → INSERT no H2, commit local
 *   SaveToMongoStep.execute() → insertOne no Mongo, commit local
 *
 * Fluxo de falha no passo 2:
 *   SaveToSqlStep.execute()      → INSERT no H2, commit local
 *   SaveToMongoStep.execute()    → FALHA
 *   SaveToSqlStep.compensate()   → DELETE no H2 (desfaz o passo 1)
 */
@Service
public class UserSagaService {

    private final SaveToSqlStep   saveToSqlStep;
    private final SaveToMongoStep saveToMongoStep;

    public UserSagaService(SaveToSqlStep saveToSqlStep,
                           SaveToMongoStep saveToMongoStep) {
        this.saveToSqlStep   = saveToSqlStep;
        this.saveToMongoStep = saveToMongoStep;
    }

    /**
     * Executa a saga de criação de usuário nos dois bancos.
     *
     * @param user Entidade a ser persistida
     * @throws SagaException se qualquer passo falhar (compensações já terão sido executadas)
     */
    public void createUser(UserEntity user) throws SagaException {
        UserSagaContext context = new UserSagaContext(user);

        new SagaOrchestrator<UserSagaContext>("CriarUsuario")
                .addStep(saveToSqlStep)    // passo 1: SQL (H2)
                .addStep(saveToMongoStep)  // passo 2: MongoDB
                .run(context);
    }
}
