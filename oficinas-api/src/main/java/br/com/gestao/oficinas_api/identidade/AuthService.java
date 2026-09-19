package br.com.gestao.oficinas_api.identidade;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final IdentidadeRepository repository;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final TransactionalEmail email;
    private final AuthProperties properties;
    private final Clock clock;
    private final String dummyHash;
    private final SecureRandom random=new SecureRandom();
    public AuthService(IdentidadeRepository repository, JdbcTemplate jdbc, PasswordEncoder passwords,
                       TransactionalEmail email, AuthProperties properties, Clock clock) {
        this.repository=repository; this.jdbc=jdbc; this.passwords=passwords; this.email=email;
        this.properties=properties; this.clock=clock; this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public IdentidadeRepository.Proprietario register(String name,String mail,String password,String shop) {
        PasswordPolicy.require(password);
        return repository.cadastrar(name.strip(),normalize(mail),passwords.encode(password),shop.strip());
    }
    public IdentidadeRepository.Proprietario authenticate(String mail,String password) {
        var owner=repository.porEmail(normalize(mail));
        boolean matched=password.getBytes(StandardCharsets.UTF_8).length<=72
            && passwords.matches(password,owner.map(IdentidadeRepository.Proprietario::senhaHash).orElse(dummyHash));
        if(!matched || owner.isEmpty() || !owner.get().ativo())
            throw new ApiException(401,"CREDENCIAIS_INVALIDAS","E-mail ou senha inválidos.");
        return owner.get();
    }
    @Transactional
    public void recover(String mail) {
        var owner=repository.porEmail(normalize(mail));
        if(owner.isEmpty() || !owner.get().ativo()) return;
        var o=owner.get();
        jdbc.queryForObject("SELECT id FROM proprietario WHERE id=? FOR UPDATE",UUID.class,o.id());
        jdbc.update("DELETE FROM recuperacao_senha WHERE proprietario_id=?",o.id());
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO recuperacao_senha(token_hash,proprietario_id,expires_at) VALUES (?,?,?)",
            hash(token),o.id(),Timestamp.from(clock.instant().plus(Duration.ofMinutes(30))));
        String link=properties.publicUrl().toString().replaceAll("/+$","")+"/redefinir-senha#token="+token;
        email.send(o.email(),"Redefina sua senha · Gestão Oficinas",
            "Recebemos uma solicitação para redefinir sua senha.\n\n"+link
            +"\n\nEste link vale por 30 minutos e pode ser usado uma vez. Se não foi você, ignore este e-mail.");
    }
    @Transactional
    public void reset(String token,String password) {
        PasswordPolicy.require(password);
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidToken();
        var ids=jdbc.query("SELECT proprietario_id FROM recuperacao_senha WHERE token_hash=?",
            (rs,n)->rs.getObject(1,UUID.class),hash(token));
        if(ids.isEmpty()) throw invalidToken();
        UUID id=ids.getFirst();
        jdbc.queryForObject("SELECT id FROM proprietario WHERE id=? FOR UPDATE",UUID.class,id);
        int consumed=jdbc.update("""
            UPDATE recuperacao_senha SET used_at=? WHERE token_hash=? AND used_at IS NULL AND expires_at>?
            """,Timestamp.from(clock.instant()),hash(token),Timestamp.from(clock.instant()));
        if(consumed!=1) throw invalidToken();
        jdbc.update("UPDATE proprietario SET senha_hash=?,auth_version=auth_version+1,updated_at=now() WHERE id=?",
            passwords.encode(password),id);
        jdbc.update("UPDATE recuperacao_senha SET used_at=? WHERE proprietario_id=? AND used_at IS NULL",
            Timestamp.from(clock.instant()),id);
        jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?",id.toString());
        UUID shop=jdbc.queryForObject("SELECT oficina_id FROM proprietario WHERE id=?",UUID.class,id);
        repository.auditar(id,shop,"SENHA_REDEFINIDA");
    }
    private ApiException invalidToken() { return new ApiException(400,"LINK_INVALIDO","Link inválido ou expirado. Solicite outro."); }
}

