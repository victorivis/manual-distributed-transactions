package com.example.demo.dao;

import com.example.demo.entity.UserEntity;

import java.util.List;

/**
 * Contrato de acesso a dados para UserEntity.
 *
 * Implementado por:
 * - UserJpaDao:   usa JPA/EntityManager   (qualificador "jpa")
 * - UserJdbcDao:  usa JDBC/DataSource     (qualificador "jdbc")
 * - UserMongoDao: usa MongoDB driver sync (qualificador "mongo")
 *
 * A implementação ativa é selecionada pela propriedade app.dao.impl
 * em application.properties.
 */
public interface UserDao {
    void save(UserEntity user);
    UserEntity findById(Long id);
    List<UserEntity> findAll();
    void update(UserEntity user);
    void delete(Long id);
}
