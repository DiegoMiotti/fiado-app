# Validación de Complicados — 2026-09-09

Prueba posterior en Motorola Edge 50 Fusion, Android 15, conectado por USB:
`gradlew.bat connectedDebugAndroidTest --console plain` terminó con **BUILD SUCCESSFUL**.
Pasaron las 4 pruebas de pantalla: copia habitual, rechazo de importe cero, conservación del borrador al recrear
la Activity y limpieza, y flujo de Complicados (varias filas, campos incompletos, eliminación de monto,
restauración del borrador y copia exacta de 13.800 + 9.200 = 23.000).
Se actualizaron las expectativas antiguas de centavos al formato actual de pesos enteros.
No se enviaron mensajes ni se probó el traspaso real a WhatsApp en esta ejecución.

Ejecutado `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console plain` con JDK 17.0.20.1,
Android SDK Platform 36 y Build Tools 35.0.0: **BUILD SUCCESSFUL**.
Las 13 pruebas unitarias pasaron: 0 fallos, 0 errores, 0 omitidas. Lint terminó con 0 errores y 8 advertencias.
APK generado: `app/build/outputs/apk/debug/app-debug.apk`.

El mensaje detallado usa el saludo según la hora y «hasta el día de hoy sería:», seguido de la suma.
No se ejecutaron pruebas de pantalla ni la integración real con WhatsApp: ADB no detectó dispositivos conectados.
Este resultado resuelve el bloqueo de Java registrado abajo.

---

# Opción Complicados — 2026-09-08

Implementados saldo anterior, múltiples montos editables, eliminación de filas y suma automática.
Vista previa, portapapeles y WhatsApp comparten el mismo generador de texto.
Agregadas cuatro pruebas unitarias: ejemplo 13.800 + 9.200, varios importes y total grande,
rechazo de campos incompletos o inválidos, y preservación de líneas y signos + en el enlace de WhatsApp.

Se intentó ejecutar `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console plain`.
No pudo iniciarse: JAVA_HOME no está configurado y no hay java en PATH. Las nuevas pruebas no se ejecutaron
y no se generó un APK. Quedan pendientes compilación, lint y verificación en teléfono.

---

# Validación de Fiado 0.2.1

Nueva plantilla con nombre del contacto: «Hola buen día Juan, lo de hoy es: $1.500,00». Por la tarde y noche usa «Hola buenas tardes» y «Hola buenas noches». Sin contacto, omite el nombre.

Compilación, 8 pruebas unitarias y lint aprobados. El nombre se incorpora a vista previa, copia y enlace de WhatsApp. Esta versión no volvió a probarse en el dispositivo; la validación física de 0.2.0 queda registrada abajo.

APK para instalar: Fiado-0.2.1.apk en la raíz. Se conserva Fiado-0.2.0.apk como versión anterior.

---

# Validación de Fiado 0.2.0 — teléfono real

Fecha: 2026-09-05. Dispositivo: Motorola Edge 50 Fusion, Android 15 (API 35), conectado por USB. WhatsApp estándar instalado.

## Resultados actuales

- 7 pruebas unitarias aprobadas: saludo, zona horaria, importes, formatos argentinos y URL.
- 3 pruebas instrumentadas aprobadas en el Motorola: generación/copia sin contacto, rechazo de importe inválido, recreación de Activity con borrador y acción Limpiar.
- La primera ejecución de copia falló al perder el foco. El test ahora espera foco/portapapeles; la ejecución completa pasó con el teléfono desbloqueado y sin interacción concurrente.
- Selección real de contacto y normalización al prefijo +54 9 11 verificadas en pantalla.
- Apertura de WhatsApp y texto prellenado verificados visualmente. El usuario confirmó que el chat abierto correspondía al destinatario esperado.
- No se pulsó Enviar. Puede quedar el borrador de prueba en WhatsApp.
- Corregido el contraste de los iconos de las barras del sistema con el tema oscuro del teléfono. Corrección instalada y revisada visualmente.
- Compilación final y tests unitarios aprobados; lint: 0 errores y 10 advertencias de actualización de versiones.
- Firma del APK final verificada con apksigner. Instalación final por ADB exitosa.
- Las pruebas instrumentadas y de WhatsApp se ejecutaron antes del último ajuste exclusivamente visual de las barras; después se repitieron compilación, tests unitarios, lint e inspección visual.

APK final: app/build/outputs/apk/debug/app-debug.apk, versión 0.2.0, min SDK 35, target/compile SDK 36.

SHA-256:
```text
88BC6BDA3EAE71E9D47879C95A710D4FDE8AEF135B945DDB07F6D6EC9CFD83C3
```

Comandos usados:
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --console plain
.\gradlew.bat connectedDebugAndroidTest lintDebug --console plain
```

Informes regenerables en app/build/reports/tests, app/build/reports/androidTests y app/build/reports/lint-results-debug.html. Capturas con datos personales excluidas de Git y eliminadas tras la revisión; no se guardan nombres ni teléfonos de la prueba en esta documentación.

## Pendientes

Prueba en Android 16, WhatsApp Business/selector entre variantes, dispositivo sin WhatsApp, proveedor de contactos alternativo, fuente grande y cambios horarios reales. Las reglas horarias sí están cubiertas por tests unitarios. No se validó envío ni entrega de mensajes.

---

## Registro histórico de Fiado 0.1.0 (antes de conectar el teléfono)

Fecha: 2026-09-05. Entorno: Windows, JDK 17.0.11, Gradle 8.13, SDK Platform 36 revisión 2.

## Ejecutado

- `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console plain`: **BUILD SUCCESSFUL**.
- 6 tests unitarios, 0 errores, 0 fallos, 0 omitidos.
- Lint: 0 errores y 7 advertencias informativas de versiones más recientes de dependencias/Gradle y target Android. Se mantiene API 36 por el alcance solicitado; no se ocultaron advertencias mediante baseline.
- APK compilado: `app/build/outputs/apk/debug/app-debug.apk`, 24.948.972 bytes.
- Firma de depuración: verificada con `apksigner verify`.
- Metadatos inspeccionados con `aapt dump badging`: paquete `ar.com.fiado`, versión 0.1.0, min/target/compile API 36, actividad principal presente.
- El manifiesto final no pide Internet ni lectura general de contactos. AndroidX incorpora su permiso interno de receptor no exportado.
- `git diff --check`: sin errores de whitespace.
- ADB: ningún dispositivo conectado. No se encontraron AVDs ni imágenes de sistema instaladas.

SHA-256 del APK validado:

```text
4EDFF64CB75E3BB3390FE76EE859D3CBBD938E5EB13308592FDF3ED208E17382
```

Informes locales regenerables: `app/build/reports/tests/testDebugUnitTest/index.html` y `app/build/reports/lint-results-debug.html`. No se versionan los binarios ni los informes de build.

## Pendiente en teléfono Android 16

No se ejecutó la app en un dispositivo o emulador. No se ha verificado visualmente la pantalla ni el comportamiento del proveedor de contactos o de WhatsApp. La compilación y los tests no demuestran esas integraciones.

1. Instalar el APK y abrir Fiado. Registrar modelo, versión de Android y versión/variante de WhatsApp usada.
2. Ingresar `1500,50`; comprobar el saludo de la hora actual y el importe `$1.500,50`.
3. Copiar sin contacto, pegar en un campo de texto y comparar el contenido. Repetir sin conexión.
4. Elegir un contacto de prueba con varios teléfonos, confirmar el número seleccionado y cancelar una segunda selección: el borrador debe conservarse.
5. Probar un teléfono local: debe pedir formato internacional. Corregirlo sin modificar la agenda.
6. Abrir WhatsApp con un contacto de prueba autorizado: confirmar chat correcto y texto completo; no es necesario pulsar Enviar para validar la transferencia.
7. Si hay WhatsApp y Business, comprobar el selector. Sin WhatsApp, comprobar navegador o aviso con alternativa de copia.
8. Probar número sin WhatsApp, regreso a Fiado, rotación, fuente grande y teclado abierto. Confirmar que no se pierde el borrador y los botones son accesibles.
9. Probar el cambio de franja horaria: el saludo debe actualizarse al volver y al copiar/abrir.
10. Pulsar Limpiar y comprobar que se borren contacto e importe.

No se enviaron mensajes reales. No se declara confirmación de envío, recepción o entrega.
