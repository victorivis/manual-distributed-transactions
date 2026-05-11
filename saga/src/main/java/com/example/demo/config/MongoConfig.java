package com.example.demo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o MongoClient a partir da URI definida em application.properties.
 *
 * Em produção, a URI (com credenciais) deve vir de variável de ambiente:
 *   export APP_MONGO_URI=mongodb+srv://user:pass@host/...
 *   app.mongo.uri=${APP_MONGO_URI}
 */
@Configuration
public class MongoConfig {

    @Value("${app.mongo.uri}")
    private String uri;

    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        return MongoClients.create(settings);
    }
}
