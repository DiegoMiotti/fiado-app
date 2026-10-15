# Validación y pendientes

## Cámara y suma de Complicados — 14 de octubre de 2026

- `testDebugUnitTest`: 31 pruebas aprobadas, 0 fallos y 0 errores. Incluyen suma alineada, nombre del contacto, total superior al límite individual, rechazo de filas incompletas y conservación del texto en la URL de WhatsApp.
- `lintDebug`, `assembleDebug` y `assembleDebugAndroidTest`: BUILD SUCCESSFUL. Lint: 0 errores y 17 advertencias sobre versiones/SDK y sugerencias de KTX.
- APK actualizado: `app/build/outputs/apk/debug/app-debug.apk`.
- Se agregó una prueba instrumentada que prepara una captura ficticia mediante FileProvider y comprueba la eliminación del archivo temporal. La prueba EXIF comprueba además que no se borra el archivo de entrada externo. Estas pruebas se compilaron, pero no se ejecutaron en esta revisión.
- El teléfono fue autorizado posteriormente. La actualización de `ar.com.fiado` y de la copia OCR existente fue rechazada por firmas diferentes. Se instaló correctamente una copia nueva **Fiado (cámara)**, paquete `ar.com.fiado.camera20261014`, y se abrió mediante ADB; las instalaciones anteriores permanecen. Esta copia usa datos y credenciales separados. Quedan pendientes la captura con la cámara real, cancelación y recreación durante la captura, inspección visual y apertura de WhatsApp con el texto alineado. No se enviaron mensajes ni consultas al backend.

## Integración OCR + IA — 14 de octubre de 2026

Validación local en la rama `codex/mejora-deteccion-ocr-ia`:

| Comprobación | Resultado | Límite |
|---|---|---|
| Pruebas unitarias | 30 aprobadas: 13 de MessageRules, 9 de reglas/JSON y 8 de ViewModels; 0 errores y 0 fallos | Backend y fotos simulados; sin consultas facturables |
| `lintDebug` | 0 errores y 13 advertencias | Versiones de dependencias y 3 sugerencias de KTX; no sustituye una prueba en teléfono |
| `assembleDebug` | APK generado en `app/build/outputs/apk/debug/app-debug.apk` | La app instalada tenía otra firma; se validó una copia separada |
| `assembleDebugAndroidTest` y pruebas instrumentadas | 6 aprobadas, 0 omitidas y 0 fallos en Motorola Edge 50 Fusion, Android 16 (API 36) | 4 tests del flujo manual y 2 de cifrado/EXIF; no consultan al backend |
| `git diff --check` | Sin errores de espacios | No valida el comportamiento de la app |

Comando final: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console plain --max-workers=2` — **BUILD SUCCESSFUL**.

La instalación habitual fue rechazada con `INSTALL_FAILED_UPDATE_INCOMPATIBLE` por una firma distinta. La limpieza automática del primer intento retiró la app habitual: se confirmó su ausencia y el usuario confirmó que no la había desinstalado. El borrador que pudiera haber tenido esa instalación no se pudo recuperar. Se reinstaló el APK habitual con la versión nueva.

Después se ejecutó `./gradlew.bat connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2` — **BUILD SUCCESSFUL**, 6/6 tests aprobados. La copia se llama **Fiado (pruebas OCR)**, paquete `ar.com.fiado.ocrtest`, con datos separados. El ejecutor también la retiró al terminar; se reinstaló su APK para dejarla disponible. Su APK se guardó en `app/build/outputs/apk/ocrtest/Fiado-OCR-pruebas.apk`. Las instrucciones ahora recomiendan siempre la copia aislada para pruebas en teléfonos personales y las tareas `connected…` la activan automáticamente para prevenir otro borrado accidental de la app habitual.

Los tests comprobaron carga/copia de mensaje, rechazo de importe inválido, conservación del borrador al recrear la actividad, suma en Complicados, persistencia cifrada y borrado de una credencial ficticia aislada, y una foto JPEG ficticia con EXIF de 90°, reducida a 1200 × 2400 píxeles dentro del límite de bytes.

Pendiente: prueba real de una foto ficticia contra el backend/Gemini, revisión visual del flujo OCR, selección de contactos, cancelación de consulta en dispositivo y traspaso del mensaje a WhatsApp. No se ingresó ni solicitó el token del teléfono en el chat. [Instrucciones y archivos cambiados](docs/LECTURA_IA.md).

## Evidencia histórica de la versión manual

Resumen de la revisión del **3 de octubre de 2026**, sobre Fiado 0.2.1 y código base `5278c60`. Es evidencia de esa revisión; no implica una nueva ejecución después de cada cambio de documentación.

| Comprobación | Resultado registrado | Límite |
|---|---|---|
| Pruebas unitarias | 13 aprobadas; 0 errores y 0 fallos, con ejecución forzada | No prueban contactos ni WhatsApp |
| Lint y compilación debug | BUILD SUCCESSFUL; 0 errores y 10 advertencias de lint | No equivale a una publicación en Google Play |
| Inspección visual en Motorola Edge 50 Fusion, Android 16 | Mensaje habitual de $1.500 y suma de $13.800 + $9.200 = $23.000 | App instalada 0.2.1; no se identificó el commit exacto de ese APK |
| Release v0.2.1 | Metadatos y presencia de APK y checksum confirmados | No se descargó ni instaló su APK en esa sesión |

Hay evidencia histórica de cuatro pruebas instrumentadas aprobadas en Android 15 el 9 de septiembre de 2026, y de selección de contacto y apertura de WhatsApp con texto prellenado en la versión 0.2.0 el 5 de septiembre. Esos resultados no verifican el traspaso del mensaje actual ni envío o entrega.

## Pendientes

- Repetir selección de contacto y traspaso a WhatsApp de los mensajes habitual y detallado con un destinatario autorizado, sin pulsar Enviar.
- Repetir pruebas instrumentadas en Android 16; probar fuente grande, proveedor de contactos alternativo, WhatsApp Business y ausencia de WhatsApp.
- Se retiraron las rutas personales de `.vscode/settings.json`. Los commits anteriores conservan esa configuración; no se reescribió el historial. La búsqueda previa de patrones de credenciales no tuvo coincidencias, pero no certifica ausencia de secretos.

Comandos, requisitos e informes: [guía de desarrollo](docs/DESARROLLO.md).
