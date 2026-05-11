package com.example.demo.saga.step;

import com.example.demo.entity.UserEntity;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Passo 1 da Saga: persiste o usuário no banco SQL (H2).
 *
 * execute()    → Gera um UUID, executa INSERT na tabela user_entity, commita localmente, salva o ID no contexto.
 * compensate() → DELETE pelo ID gerado, desfazendo o INSERT de forma idempotente.
 *
 * Usa JDBC diretamente (sem JPA) para tornar o controle transacional explícito
 * e didaticamente claro: cada execute() é uma transação local isolada.
 */
@Component
public class SaveToSqlStep implements SagaStep<UserSagaContext> {

    private final DataSource dataSource;

    public SaveToSqlStep(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "SaveToSqlStep (H2)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // execute — transação local: INSERT + commit imediato
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void execute(UserSagaContext ctx) throws Exception {
        UserEntity user = ctx.getUser();
        user.setId(UUID.randomUUID()); // Gera o ID antes do INSERT
        ctx.setSqlGeneratedId(user.getId()); // guarda para possível compensação

        String sql = "INSERT INTO user_entity (id, name, email) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getId().toString());
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.executeUpdate();
                conn.commit(); // ← commit local imediato (diferença central da Saga vs 2PC)
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        log("execute() → INSERT commitado, id=" + ctx.getSqlGeneratedId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compensate — transação de compensação: DELETE pelo ID gerado
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void compensate(UserSagaContext ctx) {
        UUID id = ctx.getSqlGeneratedId();
        if (id == null) {
            log("compensate() → nada a desfazer (execute não chegou a commitar)");
            return;
        }

        String sql = "DELETE FROM user_entity WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                int rows = ps.executeUpdate();
                conn.commit();
                log("compensate() → DELETE id=" + id + " (" + rows + " linha(s) removida(s))");
            } catch (SQLException e) {
                conn.rollback();
                log("compensate() → ERRO ao deletar id=" + id + ": " + e.getMessage());
                // Lançar aqui causaria recursão de compensação; apenas loga.
                // Em produção: registrar em dead-letter table para retentativa.
            }
        } catch (SQLException e) {
            log("compensate() → falha na conexão: " + e.getMessage());
        }
    }

    private void log(String msg) {
        System.out.println("[SAGA][" + name() + "] " + msg);
    }
}
