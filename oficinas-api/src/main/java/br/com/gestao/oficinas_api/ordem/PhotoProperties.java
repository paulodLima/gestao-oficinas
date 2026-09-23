package br.com.gestao.oficinas_api.ordem;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.photos")
public record PhotoProperties(String storagePath) {}
