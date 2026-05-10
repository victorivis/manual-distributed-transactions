package com.example.demo.dao;

import com.example.demo.entity.UserEntity;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementação MongoDB do UserDao.
 *
 * Usa o driver MongoDB (mongodb-driver-sync) diretamente, sem Spring Data
 * MongoDB, mantendo o padrão do projeto de expor as operações de persistência
 * sem abstrações adicionais.
 *
 * @author DAC
 * @version 2.0
 */
@Repository
@Primary
@Qualifier("mongo")
@ConditionalOnProperty(name = "app.dao.impl", havingValue = "mongo")
public class UserMongoDao implements UserDao {

    private final MongoCollection<Document> collection;

    // Gerador de ID simples para compatibilidade com a interface (Long id)
    private final AtomicLong idCounter = new AtomicLong(1);

    public UserMongoDao(MongoClient mongoClient,
                        @Value("${app.mongo.database}") String database) {
        this.collection = mongoClient.getDatabase(database).getCollection("users");
    }

    @Override
    public void save(UserEntity user) {
        if (user.getId() == null) {
            user.setId(idCounter.getAndIncrement());
        }
        collection.insertOne(toDocument(user));
    }

    @Override
    public UserEntity findById(Long id) {
        Document doc = collection.find(Filters.eq("id", id)).first();
        return doc != null ? toEntity(doc) : null;
    }

    @Override
    public List<UserEntity> findAll() {
        List<UserEntity> result = new ArrayList<>();
        for (Document doc : collection.find()) {
            result.add(toEntity(doc));
        }
        return result;
    }

    @Override
    public void update(UserEntity user) {
        collection.updateOne(
                Filters.eq("id", user.getId()),
                Updates.combine(
                        Updates.set("name",  user.getName()),
                        Updates.set("email", user.getEmail())
                )
        );
    }

    @Override
    public void delete(Long id) {
        collection.deleteOne(Filters.eq("id", id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapeamento UserEntity ↔ Document
    // ─────────────────────────────────────────────────────────────────────────

    private Document toDocument(UserEntity user) {
        return new Document("id",    user.getId())
                .append("name",  user.getName())
                .append("email", user.getEmail());
    }

    private UserEntity toEntity(Document doc) {
        UserEntity user = new UserEntity();
        user.setId(doc.getLong("id"));
        user.setName(doc.getString("name"));
        user.setEmail(doc.getString("email"));
        return user;
    }
}
