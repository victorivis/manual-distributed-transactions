package com.example.demo.controller;

import com.example.demo.dao.UserDao;
import com.example.demo.entity.UserEntity;
import com.example.demo.saga.orchestrator.SagaException;
import com.example.demo.saga.orchestrator.UserSagaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gerenciamento de usuários.
 *
 * Criação (POST) → UserSagaService
 *   Persiste nos dois bancos (H2 + MongoDB) via padrão Saga.
 *   Se o segundo banco falhar, o primeiro é compensado automaticamente.
 *
 * Leitura/atualização/remoção → UserDao
 *   Operações simples de banco único (implementação selecionada por app.dao.impl).
 *
 * @author DAC
 * @version 2.0
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserDao         userDao;
    private final UserSagaService sagaService;

    public UserController(UserDao userDao, UserSagaService sagaService) {
        this.userDao     = userDao;
        this.sagaService = sagaService;
    }

    /**
     * Cria um novo usuário nos dois bancos via Saga.
     *
     * Retorna 201 em caso de sucesso.
     * Retorna 500 se a saga falhar (compensações já terão sido executadas).
     */
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody UserEntity user) {
        try {
            sagaService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso.");
        } catch (SagaException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falha na saga: " + e.getMessage());
        }
    }

    /**
     * Recupera um usuário específico por ID.
     */
    @GetMapping("/{id}")
    public UserEntity getUser(@PathVariable Long id) {
        return userDao.findById(id);
    }

    /**
     * Recupera todos os usuários cadastrados.
     */
    @GetMapping
    public List<UserEntity> getAllUsers() {
        return userDao.findAll();
    }

    /**
     * Atualiza um usuário existente.
     */
    @PutMapping("/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody UserEntity user) {
        user.setId(id);
        userDao.update(user);
    }

    /**
     * Remove um usuário do banco de dados.
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userDao.delete(id);
    }
}
