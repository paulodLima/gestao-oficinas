package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.*;
import java.time.Year;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerVehicleService {
    private static final Set<String> CUSTOMER_FIELDS = Set.of("nome", "cpf", "telefone", "email", "ativo", "versao");
    private static final Set<String> VEHICLE_FIELDS = Set.of("placa", "marca", "modelo", "ano", "cor", "versao");
    private final CustomerVehicleRepository repository;
    public CustomerVehicleService(CustomerVehicleRepository repository) { this.repository = repository; }

    public PageResult<Customer> customers(Identidade owner, String query, int page, int size) {
        return repository.customers(owner.oficinaId(), query(query), page(page), size(size));
    }
    public Customer customer(Identidade owner, UUID id) { return repository.customer(owner.oficinaId(), id); }

    @Transactional
    public Customer createCustomer(Identidade owner, CustomerInput input) {
        var data = customerData(input.nome(), input.cpf(), input.telefone(), input.email());
        return repository.createCustomer(owner.oficinaId(), owner.id(), data.name, data.cpf, data.phone, data.email);
    }

    @Transactional
    public Customer updateCustomer(Identidade owner, UUID id, Map<String, Object> fields) {
        if (!CUSTOMER_FIELDS.containsAll(fields.keySet())) throw invalid("Campo não permitido.");
        var current = customer(owner, id);
        var data = customerData(text(fields, "nome", current.nome()), text(fields, "cpf", current.cpf()),
            text(fields, "telefone", current.telefone()), text(fields, "email", current.email()));
        Object activeValue = fields.getOrDefault("ativo", current.ativo());
        if (!(activeValue instanceof Boolean active)) throw invalid("Situação do cliente inválida.");
        return repository.updateCustomer(owner.oficinaId(), owner.id(), id, version(fields.get("versao")),
            data.name, data.cpf, data.phone, data.email, active);
    }

    public PageResult<Vehicle> vehicles(Identidade owner, String query, int page, int size) {
        return repository.vehicles(owner.oficinaId(), query(query), page(page), size(size));
    }
    public Vehicle vehicle(Identidade owner, UUID id) { return repository.vehicle(owner.oficinaId(), id); }

    @Transactional
    public Vehicle createVehicle(Identidade owner, VehicleInput input) {
        var data = vehicleData(input.placa(), input.marca(), input.modelo(), input.ano(), input.cor());
        if (input.clienteId() == null) throw invalid("Selecione o responsável pelo veículo.");
        return repository.createVehicle(owner.oficinaId(), owner.id(), data.plate, data.brand, data.model,
            data.year, data.color, input.clienteId());
    }

    @Transactional
    public Vehicle updateVehicle(Identidade owner, UUID id, Map<String, Object> fields) {
        if (!VEHICLE_FIELDS.containsAll(fields.keySet())) throw invalid("Campo não permitido.");
        var current = vehicle(owner, id);
        var data = vehicleData(text(fields, "placa", current.placa()), text(fields, "marca", current.marca()),
            text(fields, "modelo", current.modelo()), integer(fields, "ano", current.ano()), text(fields, "cor", current.cor()));
        return repository.updateVehicle(owner.oficinaId(), owner.id(), id, version(fields.get("versao")),
            data.plate, data.brand, data.model, data.year, data.color);
    }

    @Transactional
    public Vehicle transfer(Identidade owner, UUID id, UUID customerId, long version) {
        if (customerId == null || version < 0) throw invalid("Informe o novo responsável e a versão.");
        return repository.transfer(owner.oficinaId(), owner.id(), id, customerId, version);
    }

    private CustomerData customerData(String name, String cpf, String phone, String email) {
        name = required(name, 120, "nome");
        cpf = Cpf.normalize(cpf);
        phone = optional(phone, 30, "telefone");
        email = optional(email, 254, "e-mail").toLowerCase(Locale.ROOT);
        if (!phone.isEmpty() && !phone.matches("[+0-9() .-]{8,30}")) throw invalid("Informe um telefone válido.");
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw invalid("Informe um e-mail válido.");
        return new CustomerData(name, cpf, phone, email);
    }
    private VehicleData vehicleData(String plate, String brand, String model, Integer year, String color) {
        plate = Plate.normalize(plate);
        brand = required(brand, 80, "marca");
        model = required(model, 120, "modelo");
        color = optional(color, 50, "cor");
        int max = Year.now().getValue() + 1;
        if (year != null && (year < 1886 || year > max)) throw invalid("Informe um ano entre 1886 e " + max + ".");
        return new VehicleData(plate, brand, model, year, color);
    }
    private String query(String value) {
        String result = value == null ? "" : value.strip();
        if (result.length() > 100) throw invalid("A busca deve ter até 100 caracteres.");
        return result;
    }
    private int page(int value) { if (value < 0) throw invalid("Página inválida."); return value; }
    private int size(int value) { if (value < 1 || value > 100) throw invalid("Tamanho de página inválido."); return value; }
    private String text(Map<String, Object> fields, String key, String fallback) {
        if (!fields.containsKey(key)) return fallback;
        if (!(fields.get(key) instanceof String value)) throw invalid("Confira o campo " + key + ".");
        return value;
    }
    private Integer integer(Map<String, Object> fields, String key, Integer fallback) {
        if (!fields.containsKey(key)) return fallback;
        Object value = fields.get(key);
        if (value == null) return null;
        if (!(value instanceof Number number) || number.doubleValue() != number.intValue()) throw invalid("Confira o campo " + key + ".");
        return number.intValue();
    }
    private long version(Object value) {
        if (!(value instanceof Number number) || number.longValue() < 0 || number.doubleValue() != number.longValue())
            throw invalid("Informe a versão dos dados.");
        return number.longValue();
    }
    private String required(String value, int limit, String field) {
        String normalized = optional(value, limit, field);
        if (normalized.isBlank()) throw invalid("Informe " + field + ".");
        return normalized;
    }
    private String optional(String value, int limit, String field) {
        if (value == null || value.strip().length() > limit) throw invalid("Confira o campo " + field + ".");
        return value.strip();
    }
    private ApiException invalid(String message) { return new ApiException(400, "DADOS_INVALIDOS", message); }

    public record CustomerInput(String nome, String cpf, String telefone, String email) {}
    public record VehicleInput(String placa, String marca, String modelo, Integer ano, String cor, UUID clienteId) {}
    private record CustomerData(String name, String cpf, String phone, String email) {}
    private record VehicleData(String plate, String brand, String model, Integer year, String color) {}
}
