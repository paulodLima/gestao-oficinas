export function buildTrackingUrl(origin: string, token: string, officeName = ''): string {
  const office = officeName.trim() ? `&oficina=${encodeURIComponent(officeName.trim())}` : '';
  return `${origin}/acompanhar#token=${encodeURIComponent(token)}${office}`;
}

// Deliberately accepts no customer or order details: the capability link is enough.
export function buildTrackingMessage(url: string, officeName = ''): string {
  const office = officeName.trim() ? ` na ${officeName.trim()}` : '';
  return `Olá! Acompanhe o serviço do seu veículo${office} pelo link seguro:\n${url}\n\n` +
    'O acesso é exclusivo desta ordem de serviço, por até 7 dias ou até o encerramento. ' +
    'Não encaminhe este link. Aprovar serviços adicionais exige uma nova confirmação por código.';
}

export function buildWhatsAppUrl(message: string): string {
  return `https://wa.me/?text=${encodeURIComponent(message)}`;
}
