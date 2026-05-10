package com.example.demo.config;

import com.example.demo.dao.UserDao;
import com.example.demo.dao.UserJdbcDao;
import com.example.demo.dao.UserJpaDao;
import com.example.demo.dao.UserMongoDao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração para selecionar a implementação de UserDao em runtime.
 *
 * O Spring resolve as propriedades placeholder apenas em tempo de execução,
 * não em anotações. Este bean factory seleciona qual implementação
 * (jpa, jdbc, mongo) será injetada com base na propriedade app.dao.impl.
 *
 * Usado pelos endpoints de leitura (GET) do UserController.
 * A escrita (POST) passa pelo UserSagaService, que persiste nos dois bancos.
 *
 * @author DAC
 * @version 2.0
 */
@Configuration
public class UserDaoConfig {

    @Bean
    public UserDao userDao(
            ObjectProvider<UserJpaDao>   jpaDao,
            ObjectProvider<UserJdbcDao>  jdbcDao,
            ObjectProvider<UserMongoDao> mongoDao) {

        if (jpaDao.getIfAvailable() != null)   return jpaDao.getObject();
        if (jdbcDao.getIfAvailable() != null)  return jdbcDao.getObject();
        if (mongoDao.getIfAvailable() != null) return mongoDao.getObject();

        throw new IllegalStateException("Nenhuma implementação de UserDao disponível. "
                + "Configure app.dao.impl=jpa|jdbc|mongo em application.properties.");
    }
}
