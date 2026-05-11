-- Tabela de usuários (já existia no projeto original)
CREATE TABLE IF NOT EXISTS users (
    id    UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

-- Tabela outbox: coração do padrão.
-- Cada linha representa um evento ainda não propagado ao MongoDB.
-- Gravada atomicamente junto com o INSERT em users na mesma transação local.
CREATE TABLE IF NOT EXISTS outbox (
    id          UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    event_type  VARCHAR(100)  NOT NULL,
    payload     VARCHAR(4000) NOT NULL,  -- JSON da entidade
    processado  BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP     NOT NULL
);
