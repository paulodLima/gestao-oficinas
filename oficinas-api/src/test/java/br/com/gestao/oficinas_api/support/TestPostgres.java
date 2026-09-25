package br.com.gestao.oficinas_api.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startable;

/** PostgreSQL 17 in Docker by default, or native isolated PostgreSQL with -Dtest.database=embedded. */
public final class TestPostgres implements Startable {
    private PostgreSQLContainer<?> container;
    private EmbeddedPostgres embedded;
    @Override public void start() {
        if ("embedded".equals(System.getProperty("test.database"))) {
            try { embedded = EmbeddedPostgres.builder().setServerConfig("listen_addresses", "127.0.0.1").start(); }
            catch (java.io.IOException failure) { throw new IllegalStateException("Cannot start test PostgreSQL", failure); }
        } else {
            container = new PostgreSQLContainer<>("postgres:17-alpine");
            container.start();
        }
    }
    @Override public void stop() {
        if (container != null) container.stop();
        if (embedded != null) {
            try { embedded.close(); }
            catch (java.io.IOException failure) { throw new IllegalStateException("Cannot stop test PostgreSQL", failure); }
        }
    }
    public String getJdbcUrl() { return embedded == null ? container.getJdbcUrl()
        : "jdbc:postgresql://localhost:" + embedded.getPort() + "/postgres"; }
    public String getUsername() { return embedded == null ? container.getUsername() : "postgres"; }
    public String getPassword() { return embedded == null ? container.getPassword() : "postgres"; }
}
