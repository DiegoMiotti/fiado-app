# Desarrollo de Fiado

## Preparar el entorno

- JDK 17.
- Android SDK Platform 36 y Build Tools 36.0.0, como en CI.
- Android Studio compatible con Android Gradle Plugin 8.13.0.

Abrí la raíz del proyecto en Android Studio, seleccioná JDK 17 para Gradle y sincronizá. Configurá la ubicación del SDK desde el IDE; `local.properties` es local y no se versiona. La primera sincronización necesita Internet para descargar Gradle y las dependencias.

El proyecto usa Kotlin 2.1.20, Jetpack Compose y Gradle Wrapper 8.13. No hace falta instalar Gradle por separado.

## Compilar y comprobar

Desde la raíz, en PowerShell con JDK 17 configurado:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console plain
```

En Linux o macOS:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug --console plain
```

Estas tareas ejecutan las pruebas unitarias, revisan el código con lint y generan el APK debug en `app/build/outputs/apk/debug/app-debug.apk`. Los resultados están en `app/build/reports/tests/testDebugUnitTest/` y `app/build/reports/lint-results-debug.html`; no se versionan.

## Probar en un teléfono

Requiere Android 15 o superior, depuración USB habilitada y autorización de la computadora. Con `adb` disponible en PATH:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para ejecutar las pruebas de interfaz en PowerShell:

```powershell
.\gradlew.bat connectedDebugAndroidTest --console plain
```

En Linux/macOS, usá `./gradlew connectedDebugAndroidTest --console plain`. Los informes quedan en `app/build/reports/androidTests/connected/`. Mantené el teléfono desbloqueado y evitá interactuar durante las pruebas.

Si Android rechaza la actualización por una firma diferente, no desinstales sin revisar el borrador: al desinstalar se pierde. El APK local y el de CI pueden tener firmas de depuración diferentes.

Las pruebas automatizadas no demuestran el traspaso real a WhatsApp. Para comprobarlo, elegí un destinatario autorizado, revisá el chat y el texto prellenado sin pulsar Enviar. Usá datos ficticios para capturas públicas.

## Organización del código

| Archivo | Responsabilidad |
|---|---|
| `app/src/main/java/ar/com/fiado/MainActivity.kt` | Interfaz Compose, contactos, copia y apertura de WhatsApp |
| `app/src/main/java/ar/com/fiado/FiadoViewModel.kt` | Estado de la pantalla y borrador |
| `app/src/main/java/ar/com/fiado/MessageRules.kt` | Importes, saludo, mensajes y normalización de teléfonos |
| `app/src/test/java/ar/com/fiado/MessageRulesTest.kt` | Pruebas unitarias |
| `app/src/androidTest/java/ar/com/fiado/FiadoDeviceTest.kt` | Pruebas de interfaz en dispositivo |

El [workflow habitual](../.github/workflows/build-apk.yml) ejecuta tests, lint y compilación; su APK temporal se conserva 14 días. La [Release 0.2.1](https://github.com/DiegoMiotti/fiado-app/releases/tag/v0.2.1) ofrece el APK de prueba y su checksum. El [workflow de Release](../.github/workflows/release.yml) está fijado a esa versión y evita reemplazarla: no sirve para publicar otra versión sin actualizarlo.

Consultá [validación y pendientes](../VALIDACION.md) para distinguir comprobaciones reales de pruebas pendientes.
