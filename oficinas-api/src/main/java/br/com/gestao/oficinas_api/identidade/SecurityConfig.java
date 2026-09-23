package br.com.gestao.oficinas_api.identidade;

import java.time.*;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.session.web.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableConfigurationProperties({AuthProperties.class, br.com.gestao.oficinas_api.ordem.PhotoProperties.class})
public class SecurityConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        // Authentication is handled by AuthService; do not create a default generated account.
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use o login da aplicação."); };
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CookieSerializer cookieSerializer(AuthProperties properties) {
        var cookie=new DefaultCookieSerializer();
        cookie.setCookieName("OFICINAS_SESSION"); cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true); cookie.setSameSite("Lax"); cookie.setUseSecureCookie(properties.secureCookie());
        return cookie;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository contexts,
                                      IdentidadeRepository repository, Clock clock) throws Exception {
        var csrf=new HttpSessionCsrfTokenRepository();
        csrf.setHeaderName("X-CSRF-TOKEN");
        http.securityContext(c->c.securityContextRepository(contexts))
            .csrf(c->c.csrfTokenRepository(csrf))
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/csrf","/api/auth/cadastro","/api/auth/login",
                    "/api/auth/recuperacao","/api/auth/redefinicao").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/publico/oficinas/*", "/api/publico/oficinas/*/logo").permitAll()
                .requestMatchers("/api/painel","/api/auth/me","/api/auth/logout","/api/oficina","/api/oficina/logo",
                    "/api/clientes/**","/api/veiculos/**","/api/ordens-servico/**").hasRole("PROPRIETARIO")
                .anyRequest().denyAll())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->ApiErrors.write(res,401,"NAO_AUTENTICADO","Entre para continuar."))
                .accessDeniedHandler((req,res,ex)->ApiErrors.write(res,403,"ACESSO_NEGADO","Acesso negado ou sessão de formulário expirada.")))
            .addFilterAfter(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
                    throws ServletException,IOException {
                    response.setHeader("Cache-Control","no-store");
                    response.setHeader("Referrer-Policy","no-referrer");
                    var auth=SecurityContextHolder.getContext().getAuthentication();
                    if(auth!=null && auth.getDetails() instanceof Identidade identity) {
                        var owner=repository.porIdentidade(identity);
                        boolean valid=owner.isPresent() && owner.get().ativo()
                            && owner.get().authVersion()==identity.authVersion()
                            && clock.instant().isBefore(identity.autenticadoEm().plus(Duration.ofHours(12)));
                        if(!valid) {
                            var session=request.getSession(false);
                            if(session!=null) session.invalidate();
                            SecurityContextHolder.clearContext();
                            ApiErrors.write(response,401,"SESSAO_EXPIRADA","Sua sessão expirou. Entre novamente."); return;
                        }
                    }
                    chain.doFilter(request,response);
                }
            },SecurityContextHolderFilter.class);
        return http.build();
    }
}
