import { buildTrackingMessage, buildTrackingUrl, buildWhatsAppUrl } from './manual-share';

describe('Compartilhamento manual', () => {
  it('coloca token e nome da oficina somente no fragmento', () => {
    const url = new URL(buildTrackingUrl('https://oficina.example', 'token+&=/á', 'Oficina Central'));
    expect(url.pathname).toBe('/acompanhar');
    expect(url.search).toBe('');
    expect(new URLSearchParams(url.hash.slice(1)).get('token')).toBe('token+&=/á');
    expect(new URLSearchParams(url.hash.slice(1)).get('oficina')).toBe('Oficina Central');
  });

  it('compõe mensagem mínima e codifica acentos, quebras de linha e link uma única vez', () => {
    const url = buildTrackingUrl('https://oficina.example', 'synthetic-token');
    const message = buildTrackingMessage(url, 'Oficina Central');
    const destination = new URL(buildWhatsAppUrl(message));
    expect(destination.origin).toBe('https://wa.me');
    expect(destination.searchParams.get('text')).toBe(message);
    expect(destination.hash).toBe('');
    expect(message).toContain('Olá!');
    expect(message).toContain('\n' + url + '\n');
    expect(message).toContain('7 dias');
    expect(message).toContain('Oficina Central');
    expect(message).not.toMatch(/CPF|52998224725|Ana Souza|BRA1E23|R\$/);
    expect(destination.searchParams.has('phone')).toBeFalse();
  });
});
