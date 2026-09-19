package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerVehicleRepository {
    private final JdbcTemplate jdbc;
    public CustomerVehicleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Customer customer(UUID shopId, UUID id) {
        return jdbc.query("""
            SELECT id,nome,cpf,telefone,email,email_verificado_em,ativo,versao
              FROM cliente WHERE oficina_id=? AND id=?
            """, (rs, row) -> new Customer(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("cpf"),
                rs.getString("telefone"), rs.getString("email"), instant(rs.getTimestamp("email_verificado_em")),
                rs.getBoolean("ativo"), rs.getLong("versao")), shopId, id).stream().findFirst().orElseThrow(CustomerVehicleRepository::notFound);
    }

    public PageResult<Customer> customers(UUID shopId, String query, int page, int size) {
        String term = "%" + escape(query.toLowerCase(Locale.ROOT)) + "%";
        String digits = "%" + query.replaceAll("\\D", "") + "%";
        String where = "oficina_id=? AND (lower(nome) LIKE ? OR cpf LIKE ? OR lower(email) LIKE ? OR telefone LIKE ?)";
        long total = jdbc.queryForObject("SELECT count(*) FROM cliente WHERE " + where, Long.class,
            shopId, term, digits, term, term);
        var items = jdbc.query("SELECT id,nome,cpf,telefone,email,email_verificado_em,ativo,versao FROM cliente WHERE " + where
                + " ORDER BY lower(nome),id LIMIT ? OFFSET ?",
            (rs, row) -> new Customer(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("cpf"),
                rs.getString("telefone"), rs.getString("email"), instant(rs.getTimestamp("email_verificado_em")),
                rs.getBoolean("ativo"), rs.getLong("versao")), shopId, term, digits, term, term, size, page * size);
        return PageResult.of(items, page, size, total);
    }

    public Customer createCustomer(UUID shopId, UUID ownerId, String name, String cpf, String phone, String email) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO cliente(id,oficina_id,nome,cpf,telefone,email) VALUES (?,?,?,?,?,?)",
                id, shopId, name, cpf, phone, email);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "CLIENTE_DUPLICADO", "Já existe um cliente com este CPF na oficina.");
        }
        audit(shopId, ownerId, "CLIENTE", id, "CLIENTE_CRIADO");
        return customer(shopId, id);
    }

    public Customer updateCustomer(UUID shopId, UUID ownerId, UUID id, long version,
                                   String name, String cpf, String phone, String email, boolean active) {
        try {
            int rows = jdbc.update("""
                UPDATE cliente SET nome=?,cpf=?,telefone=?,email=?,
                  email_verificado_em=CASE WHEN email=? THEN email_verificado_em ELSE NULL END,
                  ativo=?,versao=versao+1,updated_at=now()
                WHERE oficina_id=? AND id=? AND versao=?
                """, name, cpf, phone, email, email, active, shopId, id, version);
            if (rows != 1) staleCustomer(shopId, id);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "CLIENTE_DUPLICADO", "Já existe um cliente com este CPF na oficina.");
        }
        jdbc.update("DELETE FROM verificacao_email_cliente WHERE oficina_id=? AND cliente_id=? AND usado_em IS NULL", shopId, id);
        audit(shopId, ownerId, "CLIENTE", id, "CLIENTE_EDITADO");
        return customer(shopId, id);
    }

    public Vehicle vehicle(UUID shopId, UUID id) {
        return jdbc.query("""
            SELECT v.id,v.placa,v.marca,v.modelo,v.ano,v.cor,v.versao,
                   c.id cliente_id,c.nome cliente_nome,l.inicio_em
              FROM veiculo v
              JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id AND l.veiculo_id=v.id AND l.fim_em IS NULL
              JOIN cliente c ON c.oficina_id=l.oficina_id AND c.id=l.cliente_id
             WHERE v.oficina_id=? AND v.id=?
            """, (rs, row) -> mapVehicle(rs), shopId, id).stream().findFirst().orElseThrow(CustomerVehicleRepository::notFound);
    }

    public PageResult<Vehicle> vehicles(UUID shopId, String query, int page, int size) {
        String term = "%" + escape(query.toLowerCase(Locale.ROOT)) + "%";
        String normalizedPlate = "%" + query.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT) + "%";
        String joins = " FROM veiculo v JOIN vinculo_cliente_veiculo l ON l.oficina_id=v.oficina_id AND l.veiculo_id=v.id AND l.fim_em IS NULL "
            + "JOIN cliente c ON c.oficina_id=l.oficina_id AND c.id=l.cliente_id ";
        String where = "WHERE v.oficina_id=? AND (lower(v.placa) LIKE ? OR lower(v.marca) LIKE ? "
            + "OR lower(v.modelo) LIKE ? OR lower(c.nome) LIKE ?)";
        long total = jdbc.queryForObject("SELECT count(*)" + joins + where, Long.class,
            shopId, normalizedPlate, term, term, term);
        var items = jdbc.query("SELECT v.id,v.placa,v.marca,v.modelo,v.ano,v.cor,v.versao,c.id cliente_id,c.nome cliente_nome,l.inicio_em"
                + joins + where + " ORDER BY v.placa,v.id LIMIT ? OFFSET ?",
            (rs, row) -> mapVehicle(rs), shopId, normalizedPlate, term, term, term, size, page * size);
        return PageResult.of(items, page, size, total);
    }

    public Vehicle createVehicle(UUID shopId, UUID ownerId, String plate, String brand, String model,
                                 Integer year, String color, UUID customerId) {
        customer(shopId, customerId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO veiculo(id,oficina_id,placa,marca,modelo,ano,cor) VALUES (?,?,?,?,?,?,?)",
                id, shopId, plate, brand, model, year, color);
            jdbc.update("INSERT INTO vinculo_cliente_veiculo(id,oficina_id,cliente_id,veiculo_id,criado_por) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), shopId, customerId, id, ownerId);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "VEICULO_DUPLICADO", "Já existe um veículo com esta placa na oficina.");
        }
        audit(shopId, ownerId, "VEICULO", id, "VEICULO_CRIADO");
        return vehicle(shopId, id);
    }

    public Vehicle updateVehicle(UUID shopId, UUID ownerId, UUID id, long version, String plate,
                                 String brand, String model, Integer year, String color) {
        try {
            int rows = jdbc.update("""
                UPDATE veiculo SET placa=?,marca=?,modelo=?,ano=?,cor=?,versao=versao+1,updated_at=now()
                 WHERE oficina_id=? AND id=? AND versao=?
                """, plate, brand, model, year, color, shopId, id, version);
            if (rows != 1) staleVehicle(shopId, id);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "VEICULO_DUPLICADO", "Já existe um veículo com esta placa na oficina.");
        }
        audit(shopId, ownerId, "VEICULO", id, "VEICULO_EDITADO");
        return vehicle(shopId, id);
    }

    public Vehicle transfer(UUID shopId, UUID ownerId, UUID id, UUID newCustomerId, long expectedVersion) {
        customer(shopId, newCustomerId);
        Vehicle current = vehicle(shopId, id);
        if (current.versao() != expectedVersion) throw stale();
        if (current.clienteId().equals(newCustomerId))
            throw new ApiException(400, "RESPONSAVEL_INALTERADO", "Selecione outro responsável para o veículo.");
        int locked = jdbc.update("UPDATE veiculo SET versao=versao+1,updated_at=now() WHERE oficina_id=? AND id=? AND versao=?",
            shopId, id, expectedVersion);
        if (locked != 1) throw stale();
        int closed = jdbc.update("UPDATE vinculo_cliente_veiculo SET fim_em=now() WHERE oficina_id=? AND veiculo_id=? AND fim_em IS NULL",
            shopId, id);
        if (closed != 1) throw new ApiException(409, "VINCULO_DESATUALIZADO", "O responsável mudou. Recarregue os dados.");
        jdbc.update("INSERT INTO vinculo_cliente_veiculo(id,oficina_id,cliente_id,veiculo_id,criado_por) VALUES (?,?,?,?,?)",
            UUID.randomUUID(), shopId, newCustomerId, id, ownerId);
        audit(shopId, ownerId, "VEICULO", id, "RESPONSAVEL_ALTERADO");
        return vehicle(shopId, id);
    }

    public void audit(UUID shopId, UUID ownerId, String resource, UUID resourceId, String action) {
        jdbc.update("INSERT INTO cadastro_auditoria(id,oficina_id,proprietario_id,recurso,recurso_id,acao) VALUES (?,?,?,?,?,?)",
            UUID.randomUUID(), shopId, ownerId, resource, resourceId, action);
    }

    private void staleCustomer(UUID shopId, UUID id) {
        customer(shopId, id);
        throw stale();
    }
    private void staleVehicle(UUID shopId, UUID id) {
        vehicle(shopId, id);
        throw stale();
    }
    private static ApiException stale() {
        return new ApiException(409, "CADASTRO_DESATUALIZADO", "Os dados mudaram. Recarregue antes de salvar.");
    }
    private static ApiException notFound() {
        return new ApiException(404, "CADASTRO_NAO_ENCONTRADO", "Cadastro não encontrado.");
    }
    private static String escape(String value) { return value.replace("%", "").replace("_", ""); }
    private static Instant instant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }
    private static Vehicle mapVehicle(java.sql.ResultSet rs) throws java.sql.SQLException {
        Integer year = rs.getObject("ano") == null ? null : rs.getInt("ano");
        return new Vehicle(rs.getObject("id", UUID.class), rs.getString("placa"), rs.getString("marca"),
            rs.getString("modelo"), year, rs.getString("cor"), rs.getObject("cliente_id", UUID.class),
            rs.getString("cliente_nome"), rs.getTimestamp("inicio_em").toInstant(), rs.getLong("versao"));
    }
}
