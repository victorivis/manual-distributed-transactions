package com.example.demo.outbox;

import com.example.demo.entity.UserEntity;
import com.example.demo.outbox.repository.OutboxRepository;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

/**
 * Serviço responsável pela escrita atômica no H2:
 *   INSERT INTO users   (...)  -- grava o usuário
 *   INSERT INTO outbox  (...)  -- grava o evento pendente
 *   COMMIT                     -- os dois juntos ou nenhum
 *
 * Esta atomicidade é a garantia central do padrão Outbox.
 * Comparado à Saga, não existe janela entre a escrita no banco 1
 * e a "intenção" de escrever no banco 2 — a intenção está gravada
 * na mesma transação.
 *
 * O MongoDB não é tocado aqui. Quem aplica o evento no Mongo é o OutboxRelay.
 */
@Service
public class UserOutboxService {

    private final DataSource       dataSource;
    private final OutboxRepository outboxRepository;

    public UserOutboxService(DataSource dataSource,
                             OutboxRepository outboxRepository) {
        this.dataSource       = dataSource;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Persiste o usuário e o evento outbox em uma única transação local no H2.
     *
     * @param user Entidade a ser criada
     * @throws SQLException se qualquer parte da transação falhar (rollback automático)
     */
    public void createUser(UserEntity user) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Passo 1 — INSERT do usuário
                insertUser(conn, user);

                // Passo 2 — INSERT do evento outbox (mesma conexão = mesma transação)
                OutboxEvent event = new OutboxEvent("USER_CREATED", toJson(user));
                outboxRepository.save(conn, event);

                // Commit atômico: usuário + evento persistem juntos
                conn.commit();
                log("Transação local commitada: userId=" + user.getId()
                        + ", outboxEventId=" + event.getId());

            } catch (SQLException e) {
                conn.rollback();
                log("Rollback executado: " + e.getMessage());
                throw e;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void insertUser(Connection conn, UserEntity user) throws SQLException {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getLong(1));
            }
        }
    }

    /**
     * Serialização manual para evitar dependência de Jackson neste serviço.
     * Em produção, use ObjectMapper.
     */
    private String toJson(UserEntity user) {
        return String.format(
                "{\"sqlId\":%d,\"name\":\"%s\",\"email\":\"%s\"}",
                user.getId(), user.getName(), user.getEmail());
    }

    private void log(String msg) {
        System.out.println("[OUTBOX][UserOutboxService] " + msg);
    }
}
