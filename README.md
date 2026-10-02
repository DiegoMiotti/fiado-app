# Fiado para Android

App nativa para preparar mensajes de cobro de fiado: permite ingresar un importe o sumar un saldo anterior con nuevos montos, elegir un contacto y abrir WhatsApp con el texto listo.

**El envío se confirma dentro de WhatsApp.** La app no envía mensajes automáticamente ni lleva un historial de deudas.

## Descargar e instalar

- **Android 15 o superior** (min SDK 35; versión objetivo Android 16).
- [Descargar Fiado 0.2.1 — APK de prueba](https://github.com/DiegoMiotti/fiado-app/releases/download/v0.2.1/Fiado-0.2.1-debug.apk).
- [Notas de la versión y archivos de descarga](https://github.com/DiegoMiotti/fiado-app/releases/tag/v0.2.1).

El APK es una compilación **debug**, firmada con una clave de depuración para probar la app; no es una distribución de Google Play. Descargalo en el teléfono, abrilo y autorizá la instalación desde la app utilizada para abrirlo si Android lo solicita. Si una instalación anterior tiene otra firma, Android puede exigir desinstalarla antes; se perderá el borrador.

Mientras el repositorio sea privado, la descarga requiere una cuenta con acceso al proyecto.

## Uso

### Mensaje habitual

1. Tocá **Elegir contacto** y seleccioná un número.
2. Revisá el destino: la normalización está orientada a **celulares del área 11 de Argentina**. Completa `+54 9 11` para los ocho dígitos locales y reconoce formatos como `11…`, `011…` y `011 15…`. No modifica la agenda.
3. Ingresá **pesos enteros**, por ejemplo `1500`. La pantalla agrega los puntos de miles y muestra `1.500`. No admite centavos; ingresá solamente dígitos, ya que la pantalla elimina los demás caracteres.
4. Revisá la vista previa. **Copiar mensaje** funciona sin contacto y sin Internet.
5. Tocá **Abrir WhatsApp**, revisá el destinatario y confirmá el envío allí.
6. **Limpiar** inicia otra operación.

Ejemplo con un contacto ficticio:

```text
Hola buen día Juan, lo de hoy es: $1.500
```

El mensaje habitual incluye el nombre completo del contacto seleccionado; si no hay contacto, omite el nombre. El saludo usa la hora del teléfono: buen día de 06:00 a 11:59, buenas tardes de 12:00 a 19:59 y buenas noches el resto del día.

### Suma detallada: Complicados

Activá **Complicados** y completá **Saldo anterior** y **Monto a sumar**. **Agregar otro monto** añade filas y **Quitar** elimina una fila. Todos los campos deben contener importes positivos en pesos enteros para habilitar el mensaje.

El total se calcula automáticamente. La vista previa, **Copiar mensaje** y **Abrir WhatsApp** usan este formato, sin nombres de productos:

```text
Hola buenas tardes, hasta el día de hoy sería:
13.800
+ 9.200
----------
23.000
```

Cada importe admite hasta nueve dígitos, de $1 a $999.999.999. **Limpiar** borra los importes y vuelve al modo habitual.

## Alcance y privacidad

- Sin cuenta, servidor ni registro permanente de operaciones.
- El borrador se conserva mediante el estado de Android al recrear la pantalla; no reemplaza un historial ni garantiza persistencia permanente.
- Selecciona un teléfono mediante el proveedor de contactos, con acceso temporal a la URI elegida; no pide permiso general para leer la agenda.
- No solicita permisos de Internet, notificaciones o accesibilidad.
- Escribe en el portapapeles solamente al pulsar **Copiar mensaje**.
- No registra teléfonos, importes o mensajes en logs.
- Valida el formato del teléfono, pero no comprueba si está registrado en WhatsApp.
- Intenta abrir WhatsApp o WhatsApp Business; si ambos están disponibles, ofrece un selector. Si ninguno resuelve el enlace, lo abre mediante el sistema, que puede usar un navegador. WhatsApp o el navegador requieren su propia conexión para continuar.

## Tecnologías y organización

Kotlin 2.1.20, Jetpack Compose (BOM 2025.04.01), Material 3, ViewModel con SavedStateHandle y libphonenumber 8.13.55. Gradle 8.13 y Android Gradle Plugin 8.13.0.

| Archivo | Responsabilidad |
|---|---|
| `MainActivity.kt` | Pantalla Compose, selección de contactos, portapapeles e integración con WhatsApp |
| `FiadoViewModel.kt` | Estado de la pantalla y conservación del borrador |
| `MessageRules.kt` | Importes con BigDecimal, saludo con reloj inyectable, mensajes y normalización de teléfonos |
| `MessageRulesTest.kt` | Pruebas unitarias de las reglas |
| `FiadoDeviceTest.kt` | Pruebas instrumentadas de la interfaz |

Los archivos Kotlin están bajo `app/src/main/java/ar/com/fiado/`; las pruebas, bajo `app/src/test/` y `app/src/androidTest/`.

## Abrir y compilar

Requisitos: **JDK 17**, Android SDK Platform 36, Build Tools 36.0.0 (como en CI) y Android Studio compatible con AGP 8.13.0. La primera sincronización requiere Internet para descargar las dependencias.

Abrí el proyecto en Android Studio, seleccioná JDK 17 para Gradle y sincronizá. Configurá `local.properties` con la ruta del SDK si Android Studio no lo crea; este archivo no se versiona.

Linux/macOS:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug --console plain
```

Windows, desde PowerShell con JDK 17 configurado:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console plain
```

El APK local se genera en `app/build/outputs/apk/debug/app-debug.apk`; está excluido de Git. Para instalarlo con depuración USB habilitada y el equipo autorizado:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Pruebas y validación

Las pruebas unitarias cubren horarios y zona horaria, formato y validación de importes, suma detallada, normalización de teléfonos y codificación del enlace de WhatsApp.

El [workflow de compilación](.github/workflows/build-apk.yml) ejecuta pruebas y genera un APK temporal en Actions. El [workflow de la Release 0.2.1](.github/workflows/release.yml) ejecuta pruebas unitarias, lint y compilación antes de publicar el APK y su checksum SHA-256 en una Release.

Las pruebas instrumentadas requieren un emulador o dispositivo conectado:

```bash
./gradlew connectedDebugAndroidTest
```

Consultar [VALIDACION.md](VALIDACION.md) para la evidencia de pruebas en teléfono y los resultados históricos. La compilación, lint y los tests no sustituyen la comprobación real del selector de contactos y del traspaso a WhatsApp.

Especificación e historial de decisiones: [especs.md](especs.md). Ante diferencias con documentos históricos, este README describe el comportamiento actual.

## Referencias técnicas

- [Android 16 SDK](https://developer.android.com/about/versions/16/setup-sdk)
- [Android Gradle Plugin 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Intents y contactos](https://developer.android.com/guide/components/intents-common)
- [WhatsApp click-to-chat](https://faq.whatsapp.com/5913398998672934)
