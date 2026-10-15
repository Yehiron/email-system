# Email Lab

Proyecto de aprendizaje: **Angular → Spring Boot → Cloudflare Workers → SendGrid**.

## Primera versión

Formulario con autenticación HTTP Basic, destinatario, asunto y mensaje de texto plano. API con validación, errores y límite de un intento cada cinco segundos por instancia. Worker autenticado con secreto entre servidores y remitente fijo. Sin base de datos, historial, adjuntos, colas ni reintentos automáticos.

La configuración inicial es **simulation**: no contacta Cloudflare ni envía correos. No necesitas cuentas externas para probar el formulario.

## Requisitos

- Java 21 y Maven 3.9 o superior.
- Node.js 22 LTS (22.12 o superior) y npm. Archivo `frontend/.nvmrc` incluido.
- Tres terminales si vas a ejecutar el Worker local; dos para simulación.

## Ejecutar localmente

Terminal 1:

```sh
cd backend
mvn spring-boot:run
```

Terminal 2:

```sh
cd frontend
npm ci
npm start
```

Abre http://localhost:4200. Usuario: `alumno`. Contraseña de desarrollo: `local-learning-only`. Introduce la contraseña en el formulario; no se guarda en almacenamiento del navegador. Un resultado `SIMULATED` confirma la prueba y no representa un correo real.

API: http://127.0.0.1:8080. Estado público: `/actuator/health`. El backend solo escucha en loopback por defecto.

## Contrato de la API

`POST /api/v1/emails`, `Content-Type: application/json`, autenticación HTTP Basic.

```json
{
  "to": "destinatario@example.com",
  "subject": "Mi primer correo",
  "message": "Hola desde Email Lab"
}
```

- `to`: correo obligatorio, máximo 254 caracteres.
- `subject`: obligatorio, máximo 150 caracteres.
- `message`: obligatorio, máximo 5000 caracteres.

Respuesta: `requestId`, `status`, `message`.

| HTTP | Significado |
| --- | --- |
| 200 | `SIMULATED`: no se envió correo |
| 202 | `ACCEPTED`: SendGrid aceptó el envío; entrega sin confirmar |
| 400 | Campos o JSON inválidos |
| 401 | Credenciales incorrectas o ausentes |
| 429 | Intentos demasiado seguidos |
| 502 | Fallo de integración o aceptación sin confirmar |

No reintentes automáticamente un 502: el proveedor podría haber recibido el correo antes de que se perdiera la respuesta. No hay deduplicación persistente en esta versión.

## Envío real: configuración pendiente

1. Crea una cuenta de SendGrid, verifica un remitente (o dominio) y crea una API key con permiso de envío de correo.
2. Crea una cuenta de Cloudflare con acceso a Workers.
3. Genera un secreto largo y aleatorio para `WORKER_TOKEN`. Usa el mismo valor en el backend y el Worker.
4. En `worker`, ejecuta `npm ci`, copia `.dev.vars.example` a `.dev.vars` y configura `WORKER_TOKEN`, `SENDGRID_API_KEY` y `FROM_EMAIL`. Después ejecuta `npm run dev`.
5. Reinicia el backend con estas variables en su entorno:

```sh
EMAIL_MODE=real
WORKER_URL=http://localhost:8787/api/send-email
WORKER_TOKEN=el-mismo-secreto-del-worker
```

Estas líneas describen las variables: debes exportarlas en la terminal o definirlas en tu IDE antes de iniciar Maven. Nunca pegues claves reales en el chat ni las subas a Git.

## Despliegue (preparado, no ejecutado)

**Cloudflare Workers:** desde `worker`, `npx wrangler login`. Carga los secretos interactivamente:

```sh
npx wrangler secret put WORKER_TOKEN
npx wrangler secret put SENDGRID_API_KEY
npx wrangler secret put FROM_EMAIL
npm run deploy
```

Usa la URL HTTPS resultante más `/api/send-email` como `WORKER_URL`.

**Spring Boot:** requiere un host para Java o Docker, separado de Netlify. Incluimos `backend/Dockerfile`. Configura `SPRING_PROFILES_ACTIVE=production`, `APP_USERNAME`, `APP_PASSWORD` (larga y única), `FRONTEND_ORIGIN` (URL exacta de Netlify), `WORKER_URL` y `WORKER_TOKEN`. El Dockerfile activa producción automáticamente. El perfil production exige configuración real y escucha en todas las interfaces. El host debe servir HTTPS.

**Netlify:** importa el repositorio con `netlify.toml` en la raíz. Antes de publicar, sustituye `https://api.example.com` en ese archivo por la URL HTTPS real del backend. El navegador llama a `/api`; Netlify redirige internamente al backend. No pongas secretos en variables Angular ni en archivos públicos.

Basic es suficiente para esta práctica con un solo usuario y HTTPS. No hay registro, recuperación de contraseña ni cierre de sesiones del servidor. El límite es local a cada instancia y se reinicia con el proceso: no sustituye cuotas distribuidas para un servicio público de producción.

## Verificación

```sh
cd backend
mvn test
```

```sh
cd worker
npm ci
npm run check
npm test
npx wrangler deploy --dry-run
```

```sh
cd frontend
npm ci
npm run build
```

Las pruebas usan simulaciones del proveedor; no envían correos reales.

## Estructura y recorrido

- `frontend/src/app.ts`: formulario y llamada autenticada a la API.
- `backend/.../EmailController.java`: endpoint y validación.
- `backend/.../EmailService.java`: simulación, reglas y coordinación.
- `backend/.../WorkerClient.java`: integración HTTP, sin reintentos.
- `worker/src/index.ts`: autenticación entre servidores y llamada a SendGrid.
- `docs/arquitectura.png`: diagrama conceptual. Sus logos son recreaciones generadas con IA.

Referencias oficiales: [Angular](https://angular.dev/reference/versions), [Spring Boot](https://docs.spring.io/spring-boot/), [secretos de Workers](https://developers.cloudflare.com/workers/configuration/secrets/), [SendGrid Mail Send](https://www.twilio.com/docs/sendgrid/api-reference/mail-send/mail-send).

## Actualización de despliegue

Worker creado en Cloudflare. Consulta [estado y pasos pendientes](docs/despliegue.md) para configurar secretos y preparar la carpeta para arrastrar a Netlify.

## Render

Configuración `render.yaml` incluida con plan gratuito. Consulta [GitHub y Render](docs/render.md) para completar las variables y publicar el backend.
