import { cp, mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
let backend;
try {
  backend = new URL(process.argv[2]);
  if (backend.protocol !== 'https:' || backend.username || backend.password || backend.pathname !== '/' || backend.search || backend.hash
    || /(^|\.)example\.(com|org|net)$/.test(backend.hostname) || ['localhost', '127.0.0.1', '[::1]'].includes(backend.hostname)) throw new Error();
} catch {
  console.error('Uso: node scripts/prepare-netlify.mjs https://DOMINIO-REAL-DEL-BACKEND');
  console.error('Indica el origen HTTPS público de Spring Boot, sin rutas ni credenciales.');
  process.exit(1);
}
const built = path.join(root, 'frontend/dist/email-frontend/browser');
try { await readFile(path.join(built, 'index.html')); }
catch { console.error('Primero compila Angular: cd frontend && npm ci && npm run build'); process.exit(1); }
// Cada preparación crea una carpeta nueva; no elimina salidas previas.
const folder = path.join(root, '..', 'netlify-email-' + new Date().toISOString().replace(/[:.]/g, '-'));
await mkdir(folder, { recursive: true });
await cp(built, folder, { recursive: true });
await writeFile(path.join(folder, '_redirects'), `/api/* ${backend.origin}/api/:splat 200!\n/* /index.html 200\n`);
await writeFile(path.join(folder, '_headers'), '/*\n  X-Content-Type-Options: nosniff\n  Referrer-Policy: strict-origin-when-cross-origin\n');
console.log('Arrastra esta carpeta a Netlify:\n' + folder);
