# Estado de despliegue

Cloudflare: Worker `learning-email-worker` creado desde el complemento conectado. URL:
https://learning-email-worker.yeiron12.workers.dev/api/send-email

El código está publicado, pero rechaza el envío con HTTP 503 mientras falten los secretos. No se ha enviado ningún correo ni se han modificado los otros Workers.

## Siguiente paso: secretos

En el panel de Cloudflare → Workers & Pages → learning-email-worker → Settings → Variables and Secrets, añade:

- `SENDGRID_API_KEY`, tipo Secret: clave de SendGrid con permiso de envío.
- `FROM_EMAIL`, tipo Secret: dirección verificada en SendGrid.
- `WORKER_TOKEN`, tipo Secret: secreto largo y aleatorio compartido exclusivamente con Spring Boot.

No pegues las claves en el chat. El backend necesitará el mismo WORKER_TOKEN como variable de entorno.

## Spring Boot

Pendiente elegir el host Java y obtener su URL HTTPS pública. Variables de producción en README.md. Netlify y el Worker no reemplazan este servidor Java en la arquitectura acordada.

## Netlify mediante arrastrar carpeta

No arrastres el repositorio ni el código fuente de Angular. La carpeta debe contener directamente `index.html`, JavaScript, CSS y `_redirects`.

Cuando tengamos la URL del backend, desde la raíz de email-system:

```sh
node scripts/prepare-netlify.mjs https://DOMINIO-REAL-DEL-BACKEND
```

El script usa la compilación de Angular existente y crea una carpeta `netlify-email-FECHA` junto al proyecto. Incluye `_redirects` con el proxy a Spring Boot y `_headers`. No contiene credenciales. Rechaza dominios de ejemplo y localhost para evitar un despliegue incorrecto.

Si se cambia Angular, recompilar antes de empaquetar: `cd frontend`, `npm ci`, `npm run build` con Node.js 22.

La carpeta final queda pendiente de la URL del backend; publicar solamente los archivos de interfaz no habilita los envíos.
