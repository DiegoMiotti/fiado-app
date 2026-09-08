# Fiado para Android 15 y 16

Primera prueba de una app nativa para preparar el mensaje del fiado de hoy. Ingresás un importe, elegís un teléfono desde contactos y copiás el texto o abrís WhatsApp con el mensaje preparado. **El envío se confirma dentro de WhatsApp.**

## Uso

### Opción Complicados

Activá **Complicados**, cargá el **Saldo anterior** y un **Monto a sumar**.
Podés tocar **Agregar otro monto** para sumar más importes y **Quitar** para sacar una fila.
Ingresá pesos enteros: los puntos de miles se agregan automáticamente. Completá todos los campos para habilitar la copia.
El total se calcula automáticamente. La vista previa, **Copiar mensaje** y **Abrir WhatsApp** usan este formato, sin productos:

```text
David, entonces sería:
13.800 anterior
+ 9.200
----------
23.000
```

El nombre se incluye cuando hay un contacto elegido. El borrador detallado se conserva al recrear la pantalla;
**Limpiar** borra los importes y vuelve al modo habitual.

### Mensaje habitual

1. Tocá **Elegir contacto** y elegí un número.
2. La app completa **+54 9 11** para los 8 dígitos locales y normaliza formatos como 11…, 011… y 011 15…. Revisá el destino mostrado. Esto no modifica la agenda y aplica a celulares del área 11.
3. Ingresá el importe sin puntos de miles: `1500`, `1500,50` o `1500.50`.
4. Revisá el mensaje. **Copiar mensaje** también funciona sin contacto y sin Internet.
5. Tocá **Abrir WhatsApp**, revisá el destinatario y pulsá Enviar allí.
6. **Limpiar** inicia otra operación.

El saludo usa la hora del teléfono: días de 06:00 a 11:59, tardes de 12:00 a 19:59 y noches el resto. El importe se muestra en pesos argentinos con dos decimales. No hay historial, servidor ni cuenta. El borrador se conserva al rotar o cambiar de app mediante el estado de Android; no constituye un registro permanente.

## Abrir y compilar

Requisitos: JDK 17, Android SDK Platform 36, Build Tools 35.0.0 y Android Studio compatible con AGP 8.13.0. La primera sincronización requiere Internet para descargar dependencias. Las versiones están fijadas en los archivos Gradle.

Abrir esta carpeta como proyecto en Android Studio y sincronizar Gradle. Elegir JDK 17 para Gradle. Configurar `local.properties` con la ruta del SDK si Android Studio no lo crea; este archivo no se versiona.

Ejemplo del equipo de desarrollo, en PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --console plain
```

APK de depuración: [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk).
Es un artefacto local generado, excluido de Git. No es un APK de publicación.

## Instalar en el teléfono

El dispositivo debe tener Android 15 o superior. El teléfono conectado reportó Android 15; se mantiene Android 16 como versión objetivo. Transferir el APK al teléfono, abrirlo y autorizar la instalación desde la app usada para abrirlo si Android lo solicita.

Alternativa con depuración USB habilitada y el equipo autorizado:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## Decisiones técnicas

- Kotlin 2.1.20, Gradle 8.13, AGP 8.13.0, Compose BOM 2025.04.01.
- `applicationId`: `ar.com.fiado`; min SDK 35; target/compile SDK 36.
- Selección de teléfono con el proveedor de contactos y permiso temporal sobre la URI seleccionada; sin permiso general de agenda.
- libphonenumber 8.13.55 valida el número normalizado con +54 9 11. Esto no comprueba si está registrado en WhatsApp.
- Se prueban manejadores públicos de enlaces de WhatsApp y WhatsApp Business. Con ambos, se muestra selector. Si ninguno resuelve, se abre el enlace HTTPS con el sistema (puede abrir el navegador).
- Portapapeles solo al pulsar Copiar; sin lectura del portapapeles.
- Sin permisos de Internet, contactos completos, notificaciones o accesibilidad.
- Plantilla determinista, dinero con BigDecimal y reloj inyectable. ViewModel con SavedStateHandle.
- No se registran teléfonos, importes o mensajes en logs. No hay confirmación de entrega ni automatización de Enviar.

## Validación

Consultar [VALIDACION.md](VALIDACION.md) para los resultados reales y los pasos pendientes en el teléfono. Los tests unitarios cubren límites horarios, zona horaria, formato y rechazo de importes, teléfonos internacionales y codificación del enlace. Lint y compilación no sustituyen la prueba real de contactos/WhatsApp.

## Referencias de desarrollo

- `151b823`: especificación inicial.
- `281f398`: proyecto y reglas de generación con pruebas unitarias.
- `41e86c1`: pantalla Compose, borrador, contactos, WhatsApp y Gradle Wrapper.
- Consultar `git log --oneline` para los cambios posteriores de validación y documentación.

Alcance completo: [especs.md](especs.md).

Fuentes técnicas: [Android 16 SDK](https://developer.android.com/about/versions/16/setup-sdk), [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [intents y contactos](https://developer.android.com/guide/components/intents-common), [WhatsApp click-to-chat](https://faq.whatsapp.com/5913398998672934).

Versión 0.2.1: el mensaje incluye el nombre completo del contacto, por ejemplo «Hola buen día Juan, lo de hoy es: $1.500,00». Sin contacto se omite el nombre. APK listo para instalar: [Fiado-0.2.1.apk](Fiado-0.2.1.apk).

