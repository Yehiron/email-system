interface Env {
  WORKER_TOKEN: string;
  SENDGRID_API_KEY: string;
  FROM_EMAIL: string;
}
const json = (status: number, body: object) => Response.json(body, { status });

async function matches(actual: string, expected: string): Promise<boolean> {
  const encode = new TextEncoder();
  const [a, b] = await Promise.all([actual, expected].map(s => crypto.subtle.digest('SHA-256', encode.encode(s))));
  const aa = new Uint8Array(a), bb = new Uint8Array(b);
  let difference = 0;
  for (let i = 0; i < aa.length; i++) difference |= aa[i] ^ bb[i];
  return difference === 0;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (new URL(request.url).pathname !== '/api/send-email') return json(404, { message: 'Ruta desconocida.' });
    if (request.method !== 'POST') return new Response(null, { status: 405, headers: { Allow: 'POST' } });
    if (!env.WORKER_TOKEN || !env.SENDGRID_API_KEY || !env.FROM_EMAIL) {
      return json(503, { message: 'Servicio sin configurar.' });
    }
    if (!(await matches(request.headers.get('Authorization') ?? '', `Bearer ${env.WORKER_TOKEN}`))) {
      return json(401, { message: 'No autorizado.' });
    }
    if (request.headers.get('Content-Type')?.split(';')[0].trim() !== 'application/json') {
      return json(415, { message: 'Se requiere application/json.' });
    }
    let data: Record<string, unknown>;
    try {
      // Limitación en streaming: no cargamos cuerpos arbitrariamente grandes en memoria.
      const reader = request.body?.getReader();
      if (!reader) return json(400, { message: 'Falta el cuerpo.' });
      let size = 0;
      const chunks: Uint8Array[] = [];
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        size += value.byteLength;
        if (size > 40_000) { await reader.cancel(); return json(413, { message: 'Solicitud demasiado grande.' }); }
        chunks.push(value);
      }
      const bytes = new Uint8Array(size);
      let offset = 0;
      for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
      const parsed: unknown = JSON.parse(new TextDecoder().decode(bytes));
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return json(400, { message: 'JSON inválido.' });
      data = parsed as Record<string, unknown>;
    } catch { return json(400, { message: 'JSON inválido.' }); }
    const { to, subject, message, requestId } = data;
    if (typeof to !== 'string' || to.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(to)
      || typeof subject !== 'string' || !subject.trim() || subject.length > 150
      || typeof message !== 'string' || !message.trim() || message.length > 5000
      || typeof requestId !== 'string' || !/^[0-9a-f-]{36}$/i.test(requestId)) {
      return json(400, { message: 'Datos de correo inválidos.' });
    }
    try {
      const response = await fetch('https://api.sendgrid.com/v3/mail/send', {
        method: 'POST', signal: AbortSignal.timeout(10_000),
        headers: { Authorization: `Bearer ${env.SENDGRID_API_KEY}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ personalizations: [{ to: [{ email: to }] }],
          from: { email: env.FROM_EMAIL }, subject, content: [{ type: 'text/plain', value: message }] })
      });
      if (response.status !== 202) return json(502, { message: 'El proveedor rechazó el envío.' });
      return json(202, { requestId, status: 'ACCEPTED' });
    } catch { return json(502, { message: 'No se pudo confirmar el envío. No reintentar automáticamente.' }); }
  }
};
