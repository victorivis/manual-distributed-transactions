package com.example.demo.dao;

import com.example.demo.entity.UserEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * Implementação JPA do UserDao.
 *
 * Usa EntityManager diretamente (sem Spring Data JPA) para demonstrar
 * o funcionamento de baixo nível do JPA: ciclo de vida das entidades,
 * contexto de persistência e gerenciamento de transações via @Transactional.
 *
 * @author DAC
 * @version 2.0
 */
@Repository
@Primary
@Qualifier("jpa")
@ConditionalOnProperty(name = "app.dao.impl", havingValue = "jpa", matchIfMissing = true)
public class UserJpaDao implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(UserEntity user) {
        entityManager.persist(user);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public UserEntity findById(UUID id) {
        return entityManager.find(UserEntity.class, id);
    }

    @Override
    public List<UserEntity> findAll() {
        return entityManager
                .createQuery("SELECT u FROM UserEntity u", UserEntity.class)
                .getResultList();
    }

    @Override
    public void update(UserEntity user) {
        UserEntity managed = findById(user.getId());
        managed.setName(user.getName());
        managed.setEmail(user.getEmail());
        entityManager.merge(managed);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        UserEntity user = findById(id);
        if (user != null) {
            entityManager.remove(user);
        }
    }
}
