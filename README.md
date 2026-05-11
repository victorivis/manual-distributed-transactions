# Manual Distributed Transactions

Este repositorio contem dois projetos Spring Boot que implementam manualmente dois modelos de transacoes distribuidas: Outbox e Saga.

## Requisitos

- Java 16
- Gradle (ou use o wrapper incluido)

## Configuracao

Para cada projeto (outbox e saga), substitua o arquivo `application.properties.example` por uma versao real com a chave do MongoDB.

Passos sugeridos:

1. Copie o exemplo para `application.properties`:
   - outbox/src/main/resources/application.properties.example
   - saga/src/main/resources/application.properties.example
2. Edite o `application.properties` criado e informe os valores reais de `app.mongo.uri` e `app.mongo.database`.

## Como iniciar

Em cada projeto, execute o Spring Boot com o wrapper do Gradle:

Terminal (Windows, Mac, Linux):

```
cd outbox
.\gradlew.bat bootRun
```

```
cd saga
.\gradlew.bat bootRun
```

## Observacoes de uso

- O codigo implementa manualmente dois modelos de transacoes distribuidas: Outbox e Saga.
- Apenas a rota de postagem (POST) grava nos dois bancos de dados ao mesmo tempo.
- As demais rotas operam apenas no banco de dados atual.
- Essas rotas podem ser incluidas utilizando o arquivo exportado do postman que está na pasta data/