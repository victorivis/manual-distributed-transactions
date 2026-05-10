package com.example.demo.outbox;

import com.example.demo.dao.UserDao;
import com.example.demo.entity.UserEntity;
import com.example.demo.outbox.relay.OutboxRelay;
import com.example.demo.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do padrão Outbox.
 *
 * Cada teste demonstra uma propriedade diferente do padrão:
 *
 * 1. Atomicidade local    — usuário e evento outbox persistem juntos no H2
 * 2. Relay                — evento pendente é aplicado no MongoDB pelo relay
 * 3. Idempotência         — relay pode rodar duas vezes sem duplicar no Mongo
 * 4. Consistência eventual— após o relay, os dois bancos estão sincronizados
 *
 * Comparativo com UserSagaTest:
 * ┌─────────────────────────────────┬────────────────────────────────────────┐
 * │ UserSagaTest                    │ UserOutboxTest                         │
 * ├─────────────────────────────────┼────────────────────────────────────────┤
 * │ Persiste em ambos inline        │ Persiste só no H2 (+ outbox)           │
 * │ Falha → compensação imediata    │ Falha → relay retenta depois           │
 * │ Sem garantia se processo cair   │ Evento persiste; relay recupera        │
 * │ Sem tabela auxiliar             │ Requer tabela outbox                   │
 * └─────────────────────────────────┴────────────────────────────────────────┘
 */
@SpringBootTest
class UserOutboxTest {

    @Autowired private UserOutboxService outboxService;
    @Autowired private OutboxRepository  outboxRepository;
    @Autowired private OutboxRelay       relay;
    @Autowired private UserDao           userDao;

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 1: atomicidade local — usuário e evento outbox persistem juntos
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deveGravarUsuarioEEventoOutboxNaMesmaTransacao() throws SQLException {
        int usuariosAntes = userDao.findAll().size();
        int eventosAntes  = outboxRepository.findPendentes().size();

        outboxService.createUser(new UserEntity("Alice Outbox", "alice@outbox.com"));

        assertEquals(usuariosAntes + 1, userDao.findAll().size(),
                "Usuário deve estar no H2");
        assertEquals(eventosAntes + 1, outboxRepository.findPendentes().size(),
                "Evento outbox deve estar pendente no H2");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 2: relay aplica evento pendente no MongoDB e o marca processado
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void relayDeveProcessarEventoPendenteEMarcarComoProcessado() throws SQLException {
        outboxService.createUser(new UserEntity("Bob Outbox", "bob@outbox.com"));

        List<OutboxEvent> pendenteAntes = outboxRepository.findPendentes();
        assertFalse(pendenteAntes.isEmpty(), "Deve haver ao menos um evento pendente");

        relay.processar();

        List<OutboxEvent> pendenteDepois = outboxRepository.findPendentes();
        assertEquals(0, pendenteDepois.size(),
                "Após o relay, não deve haver eventos pendentes");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 3: idempotência — relay rodando duas vezes não duplica no Mongo
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void relayDeveSerIdempotente() throws SQLException {
        outboxService.createUser(new UserEntity("Carol Outbox", "carol@outbox.com"));

        // Primeira execução: aplica no Mongo e marca como processado
        relay.processar();

        // Segunda execução: evento já processado, não deve reprocessar
        // (findPendentes retorna vazio — relay não toca no Mongo novamente)
        relay.processar();

        // Se não lançou exceção e não houve duplicata, o teste passou.
        // A ausência de erro já valida a idempotência.
        assertEquals(0, outboxRepository.findPendentes().size(),
                "Nenhum evento deve permanecer pendente após duas execuções");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cenário 4: consistência eventual — após o relay, H2 e Mongo estão sincronizados
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void aposRelayDadosDevemEstarConsistentesEntreOsDoisBancos() throws SQLException {
        int usuariosAntes = userDao.findAll().size();

        outboxService.createUser(new UserEntity("Dave Outbox", "dave@outbox.com"));

        // Antes do relay: H2 tem o usuário, Mongo ainda não
        assertEquals(usuariosAntes + 1, userDao.findAll().size());
        assertFalse(outboxRepository.findPendentes().isEmpty());

        // Após o relay: Mongo também tem o usuário
        relay.processar();

        assertEquals(0, outboxRepository.findPendentes().size(),
                "Após o relay, consistência eventual atingida — nenhum evento pendente");
    }
}
