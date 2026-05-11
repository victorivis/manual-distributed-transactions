package com.example.demo.controller;

import com.example.demo.dao.UserDao;
import com.example.demo.entity.UserEntity;
import com.example.demo.outbox.UserOutboxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gerenciamento de usuários.
 *
 * Criação (POST) → UserOutboxService
 *   Persiste usuário + evento outbox atomicamente no H2.
 *   O relay aplica o evento no MongoDB de forma assíncrona.
 *
 * Leitura/atualização/remoção → UserDao
 *   Operações simples de banco único (implementação selecionada por app.dao.impl).
 *
 * @author DAC
 * @version 3.0
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserDao            userDao;
    private final UserOutboxService  outboxService;

    public UserController(UserDao userDao, UserOutboxService outboxService) {
        this.userDao       = userDao;
        this.outboxService = outboxService;
    }

    /**
     * Cria um novo usuário.
     *
     * O usuário é persistido no H2 junto com um evento outbox na mesma
     * transação local. O MongoDB será atualizado pelo relay de forma assíncrona.
     *
     * Retorna 202 Accepted (não 201 Created) porque a consistência com o
     * MongoDB ainda não aconteceu — ela é eventual.
     */
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody UserEntity user) {
        try {
            outboxService.createUser(user);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Usuário aceito. Será propagado ao MongoDB pelo relay em instantes.");
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falha ao persistir usuário: " + e.getMessage());
        }
    }

    /**
     * Recupera um usuário específico por ID.
     */
    @GetMapping("/{id}")
    public UserEntity getUser(@PathVariable UUID id) {
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
    public void updateUser(@PathVariable UUID id, @RequestBody UserEntity user) {
        user.setId(id);
        userDao.update(user);
    }

    /**
     * Remove um usuário.
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        userDao.delete(id);
    }
}
