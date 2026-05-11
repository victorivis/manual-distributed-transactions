package com.example.demo.outbox.relay;

import com.example.demo.outbox.OutboxEvent;
import com.example.demo.outbox.repository.OutboxRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Relay do padrão Outbox.
 *
 * Roda em segundo plano (polling) a cada 5 segundos.
 * Para cada evento pendente na tabela outbox:
 *   1. Tenta aplicar no MongoDB (insertOne idempotente)
 *   2. Marca o evento como processado no H2
 *
 * Idempotência:
 *   Antes de inserir, verifica se já existe um documento com o mesmo sqlId.
 *   Isso garante que, se o relay processar o mesmo evento duas vezes
 *   (ex: crash após insertOne mas antes do marcarComoProcessado),
 *   o MongoDB não ficará com dados duplicados.
 *
 * Comparativo com Saga:
 *   Na Saga, SaveToMongoStep é chamado inline durante o request HTTP.
 *   Se o processo cair após o SQL commitar e antes do Mongo ser escrito,
 *   não há registro de que o Mongo ainda precisa ser atualizado.
 *
 *   No Outbox, esse registro existe na tabela outbox (processado = false).
 *   O relay encontra o evento pendente na próxima rodada e aplica.
 */
@Component
public class OutboxRelay {

    private final OutboxRepository          outboxRepository;
    private final MongoCollection<Document> collection;

    public OutboxRelay(OutboxRepository outboxRepository,
                       MongoClient mongoClient,
                       @Value("${app.mongo.database}") String database) {
        this.outboxRepository = outboxRepository;
        this.collection       = mongoClient.getDatabase(database).getCollection("users");
    }

    /**
     * Ciclo de polling: executa a cada 5 segundos.
     * fixedDelay garante que o próximo ciclo só começa após o anterior terminar,
     * evitando sobreposição de execuções.
     */
    @Scheduled(fixedDelayString = "${app.outbox.relay.interval-ms:5000}")
    public void processar() {
        List<OutboxEvent> pendentes = outboxRepository.findPendentes();

        if (pendentes.isEmpty()) return;

        log("Processando " + pendentes.size() + " evento(s) pendente(s)...");

        for (OutboxEvent event : pendentes) {
            try {
                aplicarNoMongo(event);
                outboxRepository.marcarComoProcessado(event.getId());
                log("✓ evento id=" + event.getId() + " aplicado e marcado como processado");
            } catch (Exception e) {
                // Não relança — o evento permanece pendente e será retentado no próximo ciclo
                log("✗ falha ao processar evento id=" + event.getId()
                        + ": " + e.getMessage() + " — será retentado");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aplicação idempotente no MongoDB
    // ─────────────────────────────────────────────────────────────────────────

    private void aplicarNoMongo(OutboxEvent event) {
        if (!"USER_CREATED".equals(event.getEventType())) {
            log("Tipo de evento desconhecido: " + event.getEventType() + " — ignorado");
            return;
        }

        Document payload = Document.parse(event.getPayload());
        String sqlId = payload.getString("sqlId");

        // Idempotência: só insere se ainda não existe documento com este sqlId
        boolean jaExiste = collection.find(Filters.eq("sqlId", sqlId)).first() != null;
        if (jaExiste) {
            log("Evento id=" + event.getId() + " já aplicado (sqlId=" + sqlId + " existe no Mongo) — idempotência");
            return;
        }

        Document doc = new Document("sqlId", sqlId)
                .append("name",  payload.getString("name"))
                .append("email", payload.getString("email"));

        collection.insertOne(doc);
        log("insertOne no Mongo: sqlId=" + sqlId + ", name=" + payload.getString("name"));
    }

    private void log(String msg) {
        System.out.println("[OUTBOX][Relay] " + msg);
    }
}
