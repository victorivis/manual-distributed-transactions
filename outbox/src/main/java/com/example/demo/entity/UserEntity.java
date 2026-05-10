package com.example.demo.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidade JPA que representa um usuário no banco de dados.
 *
 * Mapeia a tabela "users" do banco de dados, contendo informações básicas
 * de um usuário como identificador, nome e endereço de email.
 *
 * @author DAC
 * @version 2.0
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    public UserEntity() {}

    public UserEntity(String name, String email) {
        this.name  = name;
        this.email = email;
    }

    public Long   getId()    { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }

    public void setId(Long id)       { this.id    = id; }
    public void setName(String name)  { this.name  = name; }
    public void setEmail(String email){ this.email = email; }

    @Override
    public String toString() {
        return "UserEntity [id=" + id + ", name=" + name + ", email=" + email + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserEntity other = (UserEntity) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
