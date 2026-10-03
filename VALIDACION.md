# Validación y pendientes

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
