# Cuenta por audio original e IA

Rama experimental: `codex/cuenta-por-audio`, basada en `codex/fiado-candidata-oficial`.

Conserva la lectura por foto y suma **Grabar cuenta por audio**. El micrófono graba un archivo AAC en contenedor MP4 (`audio/mp4`): se envían sus bytes originales al backend, sin reconocimiento de voz ni interpretación local.

1. Configurá la conexión del backend y el token del teléfono.
2. Tocá **Grabar cuenta por audio**, permití el micrófono y tocá **Empezar a grabar**.
3. Decí: «Leo me debe 4000 de la cerveza más 300 de las salchichas».
4. Detené la grabación y tocá **Enviar audio a IA**.
5. Revisá la cuenta devuelta por IA, corregí si hace falta y elegí el contacto.
6. Comprobá el mensaje antes de abrir WhatsApp. El envío se confirma allí.

La lectura del audio reemplaza la cuenta cuando el backend devuelve una respuesta válida; conserva la foto, si había una, como referencia. Foto y audio se consultan por separado: la foto no se envía junto al audio en la misma solicitud. Cancelar el diálogo no modifica la cuenta. Si falla la consulta, la cuenta anterior se conserva y se puede reintentar explícitamente. El audio no se adjunta al mensaje de WhatsApp.

El mensaje usa el formato de Complicados con detalle al lado de cada importe:

```text
+      $ 4.000  cerveza
+      $   300  salchichas
--------------
TOTAL  $ 4.300
```

## Contrato y backend

`POST /api/v1/readings`, con el mismo Bearer token que las fotos:

```json
{"audio":{"mimeType":"audio/mp4","base64":"ARCHIVO_ORIGINAL_EN_BASE64"},"clients":[]}
```

La respuesta mantiene el esquema de lecturas existente: `clientes[].operaciones[]`, con `importe`, `detalle`, `tipo`, indicadores de revisión y total calculado por el servidor. La app no calcula movimientos a partir de una transcripción: solamente valida y suma los movimientos recibidos.

La implementación del backend está en la copia local `.tools/fiado-backend-audio`, en su rama `codex/cuenta-por-audio`. El parche `backend-audio.patch` junto a esta guía permite transportar el cambio al repositorio del backend. Esa copia parte de `origin/master`; no modifica el checkout original del backend ni publica cambios en Vercel.

El audio se envía directamente a la API nativa de Gemini con `inlineData`, siguiendo la [documentación de entrada de audio](https://ai.google.dev/gemini-api/docs/generate-content/audio). Configurar en el servidor:

- `AUDIO_API_KEY`: clave de Gemini, guardada únicamente en el servidor.
- `AUDIO_MODEL`: identificador de un modelo disponible en esa cuenta con entrada de audio y salida JSON.

El proveedor de fotos/texto conserva su configuración `AI_*`. El audio usa la misma autenticación, cuotas, validación de resultados y cálculo exacto del backend. Si falta configuración de audio, devuelve `AUDIO_NOT_CONFIGURED` antes de reservar cuota. Es necesario desplegar esta versión del backend antes de usar el audio contra el servidor público.

Si la configuración de fotos ya apunta al dominio oficial `generativelanguage.googleapis.com`, el backend reutiliza `AI_API_KEY` y `AI_MODEL` cuando no se especifican las variables `AUDIO_*`, siempre que el modelo también acepte audio. No reutiliza claves de otros proveedores.

## Límites y comprobación

Máximo 60 segundos por grabación, 2.000.000 bytes de audio y 4.000.000 bytes de cuerpo HTTP. El archivo temporal se elimina al preparar el audio o cerrar el diálogo. Al salir de la app o girar la pantalla durante la grabación, se descarta la toma y se libera el micrófono. Una consulta ya enviada mantiene su audio en memoria para permitir un reintento; cancelar la cuenta lo descarta. No hay historial ni reintentos automáticos.

Las pruebas usan respuestas y archivos simulados sin consumir IA. Cubren envío de bytes originales, detalles junto al precio, suma 4.300, errores, límites, cancelación, configuración ausente y conservación de la foto. La grabación real y la respuesta del modelo quedan pendientes de probar en teléfono con el backend configurado.

Verificaciones locales: 52 pruebas unitarias de Android, lint y compilación del APK y de las pruebas instrumentadas; 22 pruebas del backend y comprobación de sintaxis. No se ejecutaron pruebas instrumentadas en teléfono.
