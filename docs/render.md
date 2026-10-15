# GitHub y Render

El repositorio debe contener directamente backend/, frontend/, worker/, render.yaml y netlify.toml. Publicar únicamente el contenido de email-system, sin node_modules, target ni archivos de secretos.

## Accesos pendientes

Conectar los complementos GitHub y Render permite continuar la creación del repositorio y del servicio desde el chat. Ninguna cuenta se ha conectado automáticamente.

## Configuración preparada

render.yaml crea un solo Web Service con plan free y Docker. No crea una base de datos ni servicios pagos. Usa el Dockerfile existente, Java 21, el perfil production y /actuator/health. El puerto se obtiene de PORT, proporcionado por Render. El heap máximo de Java se ajusta al 60% de la memoria disponible; esto no garantiza que cualquier carga quepa en el plan gratuito.

Render genera APP_PASSWORD. Después del despliegue podrás verla en sus variables de entorno para entrar desde el formulario. Nunca uses la contraseña local en el despliegue público.

Variables que debes completar al crear el Blueprint:

- FRONTEND_ORIGIN: ya configurada como https://email-lab-wg19.netlify.app.
- WORKER_TOKEN: el mismo secreto configurado en Cloudflare.

Puedes crear primero el sitio de Netlify para obtener su URL, y actualizarlo con la carpeta final una vez que Render entregue la URL del backend. Mientras tanto el envío no estará disponible.

## Flujo de publicación

1. Crear un repositorio privado en GitHub con el contenido de email-system.
2. En Render, New → Blueprint y seleccionar ese repositorio.
3. Revisar que el servicio muestre Free, completar las variables pendientes y desplegar.
4. Comprobar https://URL-DEL-BACKEND/actuator/health.
5. Ejecutar el empaquetado de Netlify con la URL HTTPS del backend y arrastrar la carpeta generada.
6. Configurar los secretos de Cloudflare y realizar una prueba de envío autorizada con un destinatario elegido.

El plan gratuito suspende el servicio tras 15 minutos sin tráfico. La siguiente solicitud puede tardar aproximadamente un minuto en arrancarlo. El frontend puede mostrar un error durante ese arranque; espera a que /actuator/health responda antes de intentar un envío. No reintentar automáticamente un envío de estado desconocido.

## Plantilla de SendGrid pendiente

El usuario proporcionó el identificador d-b7d0c1091c0a4a66bcff10c2d226610b y el remitente verificado yeiron12@gmail.com. La integración actual envía texto plano; todavía no utiliza esa plantilla. Antes de integrarla, necesitamos los nombres de sus variables dinámicas. El Template ID no es una credencial.

Referencia: https://render.com/docs/blueprint-spec

## Conexiones comprobadas

GitHub y Render aparecen instalados, pero esta sesión todavía no expone sus herramientas. No existe repositorio remoto ni servicio Render creado desde este proyecto.

Sitio de Netlify creado: email-lab-wg19 (email-lab no estaba disponible). ID: 863c4946-76c5-41ae-afc3-bb0b0d148dcc. Dominio asignado: https://email-lab-wg19.netlify.app. El código no se ha desplegado: falta primero obtener la URL del backend para reemplazar el proxy de ejemplo. La vinculación local .netlify/state.json está excluida de Git.

Siguiente ejecución: comprobar herramientas GitHub y Render; crear repositorio privado email-system, subir main y crear el Web Service Docker gratuito; configurar WORKER_TOKEN con el valor de Cloudflare; sustituir proxy en netlify.toml por URL Render comprobada; desplegar Angular al sitio existente 863c4946-76c5-41ae-afc3-bb0b0d148dcc, sin crear otro; probar salud y recorrido HTTP. SENDGRID_API_KEY y las variables dinámicas de la plantilla siguen sin confirmarse.
