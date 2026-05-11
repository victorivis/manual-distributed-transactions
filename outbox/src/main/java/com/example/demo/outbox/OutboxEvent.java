package com.example.demo.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um evento pendente na tabela outbox.
 *
 * Cada linha é gravada atomicamente junto com o INSERT do usuário
 * na mesma transação local do H2. Isso garante que ou o usuário
 * e o evento existem juntos, ou nenhum dos dois existe.
 *
 * O relay lê os eventos com processado = false e os aplica no MongoDB.
 *
 * Comparativo com Saga:
 *   Na Saga, se o processo cair entre o passo 1 (SQL) e o passo 2 (Mongo),
 *   o estado fica inconsistente para sempre — não há registro de que o
 *   passo 2 ainda precisa acontecer.
 *
 *   No Outbox, esse registro existe: é esta linha com processado = false.
 *   O relay a encontra na próxima rodada e aplica o passo 2.
 */
public class OutboxEvent {

    private UUID          id;
    private String        eventType;   // ex: "USER_CREATED"
    private String        payload;     // JSON do usuário
    private boolean       processado;
    private LocalDateTime criadoEm;

    public OutboxEvent() {}

    public OutboxEvent(String eventType, String payload) {
        this.eventType  = eventType;
        this.payload    = payload;
        this.processado = false;
        this.criadoEm   = LocalDateTime.now();
    }

    public UUID          getId()          { return id; }
    public String        getEventType()   { return eventType; }
    public String        getPayload()     { return payload; }
    public boolean       isProcessado()   { return processado; }
    public LocalDateTime getCriadoEm()    { return criadoEm; }

    public void setId(UUID id)                    { this.id         = id; }
    public void setEventType(String eventType)    { this.eventType  = eventType; }
    public void setPayload(String payload)        { this.payload    = payload; }
    public void setProcessado(boolean processado) { this.processado = processado; }
    public void setCriadoEm(LocalDateTime dt)     { this.criadoEm   = dt; }

    @Override
    public String toString() {
        return "OutboxEvent[id=" + id + ", type=" + eventType
                + ", processado=" + processado + "]";
    }
}
