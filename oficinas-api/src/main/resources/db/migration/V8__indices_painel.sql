CREATE INDEX ordem_painel_status_idx ON ordem_servico (oficina_id, status, updated_at DESC, id)
 WHERE encerrada_em IS NULL;
CREATE INDEX ordem_painel_previsao_idx ON ordem_servico (oficina_id, previsao_em, id)
 WHERE encerrada_em IS NULL AND previsao_em IS NOT NULL;
CREATE INDEX ordem_evento_etapa_idx ON ordem_servico_evento (oficina_id, ordem_servico_id, created_at DESC)
 WHERE tipo='STATUS';
