package com.example.demo.dao;

import com.example.demo.entity.UserEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementação JDBC do UserDao.
 *
 * Usa DataSource/Connection diretamente, sem JPA, para tornar visível
 * o que o EntityManager abstrai: abertura de conexão, preparação de
 * statements, mapeamento de ResultSet e controle manual de transação.
 *
 * @author DAC
 * @version 2.0
 */
@Repository
@Primary
@Qualifier("jdbc")
@ConditionalOnProperty(name = "app.dao.impl", havingValue = "jdbc")
public class UserJdbcDao implements UserDao {

    private final DataSource dataSource;

    public UserJdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(UserEntity user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        String sql = "INSERT INTO user_entity (id, name, email) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar usuário", e);
        }
    }

    @Override
    public UserEntity findById(UUID id) {
        String sql = "SELECT id, name, email FROM user_entity WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário", e);
        }
    }

    @Override
    public List<UserEntity> findAll() {
        String sql = "SELECT id, name, email FROM user_entity";
        List<UserEntity> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar usuários", e);
        }
    }

    @Override
    public void update(UserEntity user) {
        String sql = "UPDATE user_entity SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getId().toString());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuário", e);
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM user_entity WHERE id = ?";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar usuário", e);
        }
    }

    private UserEntity mapRow(ResultSet rs) throws SQLException {
        UserEntity user = new UserEntity();
        String rawId = rs.getString("id");
        if (rawId != null && !rawId.isBlank()) {
            user.setId(UUID.fromString(rawId.trim()));
        }
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        return user;
    }
}
