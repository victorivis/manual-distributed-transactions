package com.example.demo.saga.step;

import com.example.demo.entity.UserEntity;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Passo 2 da Saga: persiste o usuário no MongoDB.
 *
 * execute()    → insertOne na coleção users, commita localmente, salva o ID no contexto.
 * compensate() → deleteOne pelo ID gerado, desfazendo o insertOne de forma idempotente.
 *
 * Este passo é executado APÓS o SaveToSqlStep ter commitado com sucesso.
 * Se este passo falhar, o orquestrador dispara compensate() no SaveToSqlStep.
 *
 * Isso demonstra o coração do padrão Saga:
 *   cada banco commita na sua própria transação local, sem coordenação de lock global.
 *   A consistência eventual é garantida pela sequência execute/compensate.
 */
@Component
public class SaveToMongoStep implements SagaStep<UserSagaContext> {

    private final MongoCollection<Document> collection;

    // ID incremental para compatibilidade com a interface Long
    private final AtomicLong idCounter = new AtomicLong(1);

    public SaveToMongoStep(MongoClient mongoClient,
                           @Value("${app.mongo.database}") String database) {
        this.collection = mongoClient.getDatabase(database).getCollection("users");
    }

    @Override
    public String name() {
        return "SaveToMongoStep (MongoDB)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // execute — transação local: insertOne + commit implícito do Mongo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void execute(UserSagaContext ctx) throws Exception {
        UserEntity user = ctx.getUser();
        long mongoId = idCounter.getAndIncrement();
        ctx.setMongoGeneratedId(mongoId);  // guarda para possível compensação

        Document doc = new Document("id",    mongoId)
                .append("sqlId", ctx.getSqlGeneratedId()) // referência cruzada ao SQL
                .append("name",  user.getName())
                .append("email", user.getEmail());

        collection.insertOne(doc);  // MongoDB commita imediatamente (sem 2PC)

        log("execute() → insertOne commitado, mongoId=" + mongoId
                + ", sqlId=" + ctx.getSqlGeneratedId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compensate — transação de compensação: deleteOne pelo mongoId
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void compensate(UserSagaContext ctx) {
        Long mongoId = ctx.getMongoGeneratedId();
        if (mongoId == null) {
            log("compensate() → nada a desfazer (execute não chegou a commitar)");
            return;
        }

        try {
            long deleted = collection.deleteOne(Filters.eq("id", mongoId)).getDeletedCount();
            log("compensate() → deleteOne mongoId=" + mongoId + " (" + deleted + " doc(s) removido(s))");
        } catch (Exception e) {
            log("compensate() → ERRO ao deletar mongoId=" + mongoId + ": " + e.getMessage());
            // Em produção: registrar em dead-letter collection para retentativa.
        }
    }

    private void log(String msg) {
        System.out.println("[SAGA][" + name() + "] " + msg);
    }
}
