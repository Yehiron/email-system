# Verificación local

Primera versión, verificada el 14 de octubre de 2026 (America/Bogota).

- Backend: `mvn test`, 7 pruebas correctas. Incluyen autenticación, validación, salud, CORS, simulación, frecuencia y cliente HTTP del Worker contra un servidor de prueba.
- Worker: TypeScript sin errores y 2 pruebas correctas con SendGrid simulado. Se comprueban autenticación, datos inválidos, tamaño, remitente fijo, aceptación y fallos del proveedor.
- Worker: empaquetado `wrangler deploy --dry-run` correcto; no se ha publicado.
- Angular: compilación de producción correcta usando Node.js 22; bundle inicial de unos 177 kB sin comprimir.
- Navegador: Chrome ejecutó el formulario contra la API local a través del proxy de Angular, recibió HTTP 200 y estado SIMULATED. Sin errores JavaScript; sin desbordamiento horizontal a 390 px.
- Captura: `preview.png`.

No se han enviado correos reales ni desplegado servicios. Falta configurar las cuentas, remitente, secretos y alojamiento de Spring Boot. El recorrido con Cloudflare y SendGrid reales queda pendiente de esas credenciales.
