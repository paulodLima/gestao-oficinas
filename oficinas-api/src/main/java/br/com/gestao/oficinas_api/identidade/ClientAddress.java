package br.com.gestao.oficinas_api.identidade;

import java.net.InetAddress;
import java.net.UnknownHostException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Only the explicitly configured proxy may supply the client address. */
@Component
public class ClientAddress {
    private final String trustedHost;
    public ClientAddress(@Value("${app.auth.trusted-proxy-host:}") String trustedHost) {
        this.trustedHost=trustedHost;
    }
    public String resolve(HttpServletRequest request) {
        String peer=request.getRemoteAddr();
        if(!trustedHost.isBlank()) {
            try {
                for(var address:InetAddress.getAllByName(trustedHost)) {
                    if(address.equals(InetAddress.getByName(peer))) {
                        String client=request.getHeader("X-Oficinas-Client-IP");
                        if(client!=null && client.length()<=45 && client.matches("[0-9a-fA-F:.]+")) return client;
                    }
                }
            } catch(UnknownHostException ignored) { /* Fail closed: never trust an unknown peer. */ }
        }
        return peer;
    }
}
