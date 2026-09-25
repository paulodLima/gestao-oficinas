export function buildTrackingUrl(origin: string, token: string): string {
  return `${origin}/acompanhar#token=${encodeURIComponent(token)}`;
}

// Deliberately accepts no customer or order details: the capability link is enough.
export function buildTrackingMessage(url: string): string {
  return `Olá! Acompanhe o serviço do seu veículo pelo link seguro:\n${url}\n\n` +
    'O acesso é exclusivo desta ordem de serviço, por até 7 dias ou até o encerramento. ' +
    'Não encaminhe este link. Aprovar serviços adicionais exige uma nova confirmação por código.';
}

export function buildWhatsAppUrl(message: string): string {
  return `https://wa.me/?text=${encodeURIComponent(message)}`;
}
