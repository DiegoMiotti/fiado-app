# Validación y pendientes

## Candidata unificada 0.3.0-rc1 — 15 de octubre de 2026

Rama `codex/fiado-candidata-oficial`. [Alcance y genealogía de la integración](docs/VERSION_CANDIDATA.md).

- Referencias remotas actualizadas con `git fetch origin --prune`. La última rama automática es `origin/codex/flujo-foto-whatsapp-automatico`, commit `9ecfbea`.
- El rediseño se guardó en `3e912b1`; ya contiene la rama automática como antecesora. El merge confirmó `Already up to date`, sin conflictos. Se conserva el flujo por foto, corrección opcional, contacto y WhatsApp con la nueva interfaz.
- Versión Android `0.3.0-rc1`, `versionCode = 4`, nombre **Fiado**, paquete `ar.com.fiado`. No se modificaron reglas de negocio ni se agregaron funcionalidades. `master` conserva su commit anterior.
- `testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**. Las 47 pruebas unitarias siguen aprobadas (tarea sin cambios, resultados reutilizados); lint mantiene 0 errores y 42 advertencias existentes.
- APK normal verificado por metadatos (`ar.com.fiado`, versión `0.3.0-rc1`, código 4) y conservado en `app/build/outputs/apk/candidate/Fiado-0.3.0-rc1.apk`. APK aislado generado: `app/build/outputs/apk/candidate/Fiado-0.3.0-rc1-pruebas.apk`.
- El primer intento de `connectedDebugAndroidTest` de la candidata se interrumpió antes de ejecutar pruebas con `No connected devices!`. Tras reconectar el teléfono, la ejecución completa terminó en **BUILD SUCCESSFUL: 16 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**, en Motorola Edge 50 Fusion con Android 16. Comando: `connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2`, usando exclusivamente la copia aislada `ar.com.fiado.ocrtest`.
- Después de las pruebas, el APK candidato habitual se instaló mediante `adb install -r` (**Success**) y se abrió `ar.com.fiado/.MainActivity` (**Status: ok**). Se verificaron en el dispositivo `versionName=0.3.0-rc1`, `versionCode=4` y la actividad en primer plano. Se actualizó la app existente sin desinstalarla ni borrar sus datos.
- Las pruebas usan cuentas, contactos, fotos y credenciales ficticios y simulan la apertura de WhatsApp. No se hicieron consultas reales de IA ni envíos de mensajes. Los pendientes de inspección manual del rediseño siguen indicados abajo.

## Rediseño visual — 15 de octubre de 2026

Rama `codex/rediseno-visual-fiado`, creada desde `codex/flujo-foto-whatsapp-automatico`. [Propuesta, implementación y criterios de accesibilidad](docs/REDISENO_VISUAL.md).

- `testDebugUnitTest`: **47 pruebas aprobadas**, 0 fallos y 0 errores (11 de mensaje automático, 14 de MessageRules, 9 de reglas/JSON y 13 de ViewModel).
- `lintDebug`, `assembleDebug` y `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. Lint: **0 errores y 42 advertencias**, sin advertencias en los archivos del rediseño; corresponden a dependencias, recursos históricos y sugerencias KTX existentes.
- APK generado: `app/build/outputs/apk/debug/app-debug.apk`. APK de pruebas compilado: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Se agregaron tres pruebas instrumentadas: importe habitual en 280 dp, total largo con fuente al 200 % y desplazamiento, y acceso al total/contacto/WhatsApp con fuente al 200 %. Las pruebas existentes usan ahora el tema real de Fiado.
- Tras conectar el Motorola Edge 50 Fusion (Android 16), `connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2`: **BUILD SUCCESSFUL, 16 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**. Se comprobaron importes largos desplazables, tarjeta de 280 dp y flujo con fuente al 200 %, además de los casos existentes de corrección, contacto, bloqueos, recreación, cifrado y fotos ficticias.
- Las pruebas se ejecutaron con el paquete aislado `ar.com.fiado.ocrtest`. Se reinstaló y abrió esa copia como **Fiado (pruebas OCR)** para dejar el rediseño disponible en el teléfono. APK conservado en `app/build/outputs/apk/visual/Fiado-visual-pruebas.apk`; la app habitual no se reinstaló ni desinstaló.
- Inspección visual de la pantalla inicial en vertical: encabezado, tarjeta, botones, aviso de conexión y barras del sistema sin recortes visibles. [Captura real](docs/screenshots/rediseno-inicio.png). La copia aislada no tiene token configurado; las acciones de captura y WhatsApp están deshabilitadas como corresponde.
- Pendientes de revisión manual: TalkBack, orientación horizontal, fuente al 150 %, teclado de corrección y animaciones desactivadas. La prueba al 200 % fue instrumentada; no sustituye la inspección visual completa de todos los estados. No se hicieron consultas reales de IA ni envíos de WhatsApp.
- No se modificaron reglas, cálculos, ViewModels, backend, IA, almacenamiento ni integración con contactos/WhatsApp. Las pruebas unitarias no consultan al proveedor real.
- `git diff --check`: sin errores de espacios.

## Corrección opcional de lecturas — 15 de octubre de 2026

Rama `codex/flujo-foto-whatsapp-automatico`.

- 47 tests unitarios aprobados, 0 fallos y 0 errores: se sumaron 3 casos de reglas y 3 de ViewModel para confirmar totales corregidos, conservar la lectura al descartar, completar una consulta fallida sin reintentar la IA y aislar las correcciones por cliente/foto.
- 13 pruebas instrumentadas aprobadas en Motorola Edge 50 Fusion, Android 16: incluyen corregir un importe ilegible, descartar ediciones conservando el mensaje anterior y completar la cuenta tras un fallo de IA.
- `lintDebug`, `assembleDebug` y `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, lint con 0 errores y 42 advertencias (dependencias, recursos antiguos sin uso y KTX).
- La corrección se edita en un borrador separado. Solo **Guardar correcciones** autoriza usar el total local frente a un total de IA nulo o diferente. Importes inválidos, múltiples saldos y totales no positivos siguen bloqueados. Otra foto no hereda esa autorización.
- Los tests usan una copia aislada y servicios simulados; no se envían mensajes ni se consume cuota de IA. La prueba real del proveedor sigue pendiente.
- APK normal con correcciones instalado correctamente mediante `adb install -r` y abierto en el Motorola (`ar.com.fiado/.MainActivity`). Se comprobó únicamente la existencia del archivo cifrado, sin leer su contenido: esta instalación no tiene una credencial guardada y requiere ingresar el token en la app. No se desinstaló ninguna app para instalarla.

Ver [flujo y corrección manual](docs/FLUJO_AUTOMATICO.md).

## Variante automática por foto — 15 de octubre de 2026

Rama local `codex/flujo-foto-whatsapp-automatico`, basada en `e53c677`. [Flujo y archivos cambiados](docs/FLUJO_AUTOMATICO.md).

| Comprobación | Resultado | Límite |
|---|---|---|
| Tests unitarios | 41 aprobados, 0 errores y 0 fallos: 8 de mensaje automático, 14 de MessageRules, 9 de reglas/JSON y 10 de ViewModel | Respuestas y fotografías simuladas; sin consultas facturables |
| Compilación debug y APK de tests | `BUILD SUCCESSFUL` | No prueba Gemini con una foto real |
| Tests en Motorola Edge 50 Fusion, Android 16 | 10 aprobados: 6 de pantalla automática, 1 de actividad/recreación y 3 de cifrado/fotos | Copia aislada `ar.com.fiado.ocrtest`; contactos, lecturas y apertura de WhatsApp simulados en los tests de flujo |
| `lintDebug` | 0 errores y 42 advertencias en la validación final | Versiones de dependencias, recursos del modo manual que ya no usa esta pantalla y sugerencias KTX |

La primera ejecución de dispositivo se interrumpió al retirar la copia desde el lanzador durante la prueba; la repetición completa con la copia aislada terminó correctamente. Los tests no envían mensajes, no capturan fotos personales ni llaman al backend real.

La pantalla de esta rama no tiene entrada manual de montos ni selector de modo. Confirmar una foto inicia una sola consulta; el contacto produce un mensaje detallado con saldo opcional, cargos y pagos que se restan. El botón final confirma la cuenta visible y abre WhatsApp con el texto listo. Un importe sin leer, total nulo/inconsistente o no positivo, múltiples saldos anteriores o teléfono inválido bloquea la apertura y pide otra foto/contacto.

Pendiente: prueba real de foto → Gemini → contacto → chat de WhatsApp. Para esa prueba se requiere el token ingresado directamente en la app. No se hizo una llamada real ni se consumió cuota durante esta validación.

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
