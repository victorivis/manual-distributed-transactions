package com.example.demo.outbox.repository;

import com.example.demo.outbox.OutboxEvent;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso JDBC à tabela outbox no H2.
 *
 * Operações intencionalmente simples e explícitas — sem JPA — para deixar
 * claro que a gravação do evento acontece na mesma conexão/transação
 * que o INSERT do usuário (ver UserOutboxService).
 *
 * A tabela é criada pelo schema.sql no startup da aplicação.
 */
@Repository
public class OutboxRepository {

    private final DataSource dataSource;

    public OutboxRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Grava um evento outbox usando uma conexão já aberta.
     *
     * Recebe a Connection externamente para garantir que o INSERT do evento
     * participa da MESMA transação local que o INSERT do usuário.
     * Esse é o ponto central do padrão Outbox.
     */
    public void save(Connection conn, OutboxEvent event) throws SQLException {
        String sql = "INSERT INTO outbox (event_type, payload, processado, criado_em) "
                   + "VALUES (?, ?, false, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.getEventType());
            ps.setString(2, event.getPayload());
            ps.setTimestamp(3, Timestamp.valueOf(
                    event.getCriadoEm() != null ? event.getCriadoEm() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) event.setId(keys.getLong(1));
            }
        }
    }

    /**
     * Retorna todos os eventos ainda não processados, em ordem de criação.
     * Usado pelo OutboxRelay a cada ciclo de polling.
     */
    public List<OutboxEvent> findPendentes() {
        String sql = "SELECT id, event_type, payload, processado, criado_em "
                   + "FROM outbox WHERE processado = false ORDER BY criado_em ASC";
        List<OutboxEvent> eventos = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) eventos.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar eventos pendentes", e);
        }
        return eventos;
    }

    /**
     * Marca um evento como processado após o relay aplicá-lo com sucesso no MongoDB.
     * Usa SELECT FOR UPDATE via UPDATE direto para evitar double-processing.
     */
    public void marcarComoProcessado(Long id) {
        String sql = "UPDATE outbox SET processado = true WHERE id = ? AND processado = false";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar evento como processado", e);
        }
    }

    private OutboxEvent mapRow(ResultSet rs) throws SQLException {
        OutboxEvent e = new OutboxEvent();
        e.setId(rs.getLong("id"));
        e.setEventType(rs.getString("event_type"));
        e.setPayload(rs.getString("payload"));
        e.setProcessado(rs.getBoolean("processado"));
        e.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        return e;
    }
}
