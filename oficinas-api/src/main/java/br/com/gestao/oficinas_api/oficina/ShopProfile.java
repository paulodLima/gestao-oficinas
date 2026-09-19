package br.com.gestao.oficinas_api.oficina;

import java.util.UUID;

public record ShopProfile(UUID id, String slug, String nome, String telefone, String emailContato,
        String endereco, String horario, String fuso, boolean perfilPublico, boolean temLogo, long versao) {
    public PublicProfile publicView() {
        return new PublicProfile(nome, telefone, emailContato, endereco, horario, fuso,
            temLogo ? "/api/publico/oficinas/" + slug + "/logo?v=" + versao : null);
    }
    public record PublicProfile(String nome, String telefone, String emailContato, String endereco,
                                String horario, String fuso, String logoUrl) {}
}
