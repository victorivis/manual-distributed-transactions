package com.example.demo.saga.step;

import com.example.demo.entity.UserEntity;

import java.util.UUID;

/**
 * Contexto compartilhado entre todos os passos da saga de criação de usuário.
 *
 * O contexto carrega os dados de entrada e os IDs gerados por cada banco,
 * permitindo que os passos de compensação saibam exatamente o que desfazer.
 *
 * Analogia com 2PC:
 * No 2PC, o TM guarda o Xid para identificar a transação em cada RM.
 * Na Saga, o contexto guarda os IDs locais para que cada passo saiba
 * como compensar (qual registro deletar, qual operação inverter).
 */
public class UserSagaContext {

    /** Dados da entidade a ser criada */
    private final UserEntity user;

    /**
     * ID gerado pelo banco SQL (H2) após o INSERT do passo 1.
     * Preenchido por SaveToSqlStep.execute() e lido por SaveToSqlStep.compensate().
     */
    private UUID sqlGeneratedId;

    /**
     * ID gerado pelo MongoDB após o INSERT do passo 2.
     * Preenchido por SaveToMongoStep.execute() e lido por SaveToMongoStep.compensate().
     */
    private UUID mongoGeneratedId;

    public UserSagaContext(UserEntity user) {
        this.user = user;
    }

    public UserEntity getUser()               { return user; }

    public UUID getSqlGeneratedId()           { return sqlGeneratedId; }
    public void setSqlGeneratedId(UUID id)    { this.sqlGeneratedId = id; }

    public UUID getMongoGeneratedId()         { return mongoGeneratedId; }
    public void setMongoGeneratedId(UUID id)  { this.mongoGeneratedId = id; }
}
