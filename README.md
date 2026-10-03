<div align="center">

# Fiado

### Del importe al mensaje, en unos pocos toques.

Prepará el cobro de hoy, sumá lo que quedó pendiente y llevá un mensaje claro a WhatsApp.

**Para comercios, emprendimientos y personas que cobran fiado por mensaje.**

[**📲 Descargar Fiado para Android**](https://github.com/DiegoMiotti/fiado-app/releases/download/v0.2.1/Fiado-0.2.1-debug.apk)

[Ver la versión y sus novedades](https://github.com/DiegoMiotti/fiado-app/releases/tag/v0.2.1)

**Android 15 o superior · Pesos argentinos enteros · Sin crear una cuenta**

</div>

---

## Menos cuentas a mano. Más claridad al cobrar.

Un importe de hoy. Un saldo que quedó de antes. Otro monto para sumar. Y después, escribir todo de nuevo en un mensaje.

**Fiado te ayuda a preparar esa cuenta y comunicarla.** Ingresás los importes, revisás el resultado y elegís entre copiar el mensaje o abrir WhatsApp con el texto listo. Si seleccionás un contacto, el mensaje habitual también incorpora su nombre.

Vos revisás el destinatario y confirmás el envío en WhatsApp. La app no envía mensajes automáticamente ni lleva un historial de deudas.

## Así se ve Fiado

| El cobro de hoy | Saldo anterior + nuevos importes | La cuenta lista para compartir |
|:---:|:---:|:---:|
| <img src="docs/screenshots/mensaje-habitual.png" alt="Pantalla de Fiado con un importe ficticio de 1.500 pesos y el mensaje preparado" width="250"> | <img src="docs/screenshots/suma-detallada.png" alt="Modo Complicados con saldo anterior de 13.800 y un nuevo importe de 9.200 pesos" width="250"> | <img src="docs/screenshots/vista-previa.png" alt="Vista previa del total de 23.000 pesos y acciones para copiar o abrir WhatsApp" width="250"> |
| Ingresá un importe y revisá el mensaje. | Sumá lo pendiente y los nuevos montos. | Comprobá el detalle antes de compartirlo. |

Capturas reales tomadas en un Motorola Edge 50 Fusion con Android 16. Todos los importes son ficticios; no muestran nombres ni teléfonos. El botón de WhatsApp aparece deshabilitado porque no se seleccionó un contacto.

## Lo que podés hacer

| Función | Cómo te ayuda |
|---|---|
| **Mensaje habitual** | Prepará el importe de hoy con el nombre del contacto seleccionado. |
| **Suma detallada** | Combiná un saldo anterior con uno o varios importes nuevos y obtené el total automáticamente. |
| **Vista previa** | Revisá exactamente qué texto vas a copiar o llevar a WhatsApp. |
| **Saludo según la hora** | El mensaje usa buen día, buenas tardes o buenas noches según la hora del teléfono. |
| **Copiar mensaje** | Usá el texto donde prefieras, incluso sin elegir un contacto. |
| **Abrir WhatsApp** | Continuá en WhatsApp con el destinatario y el mensaje preparados. |
| **Limpiar** | Empezá otra operación cuando termines. |

## Dos formas de preparar tu cobro

### Para el importe de hoy

Elegí un contacto, ingresá el importe y revisá la vista previa. Fiado agrega los puntos de miles: escribís `1500` y ves `1.500`.

Con un contacto ficticio llamado Juan, el mensaje de la mañana queda así:

```text
Hola buen día Juan, lo de hoy es: $1.500
```

Sin contacto, el mensaje omite el nombre y se puede copiar igualmente.

### Para sumar lo pendiente

Activá **Complicados**, cargá el **Saldo anterior** y un **Monto a sumar**. Si hay más importes, tocá **Agregar otro monto**; podés quitar las filas que ya no necesites.

Por ejemplo, un saldo de $13.800 más $9.200 produce este mensaje por la tarde:

```text
Hola buenas tardes, hasta el día de hoy sería:
13.800
+ 9.200
----------
23.000
```

El detalle permite ver de dónde sale el total. Completá todos los campos con importes positivos para habilitar el mensaje.

## Empezá a usarla

1. **Descargá el APK** con el botón de abajo desde tu teléfono.
2. **Abrí el archivo** y, si Android lo solicita, permití la instalación desde la app que usaste para descargarlo.
3. **Abrí Fiado**, elegí un contacto e ingresá el importe. También podés preparar y copiar un mensaje sin contacto.
4. **Revisá y compartí.** Tocá **Abrir WhatsApp**, comprobá el chat y confirmá el envío allí.

<div align="center">

[**📲 Descargar Fiado 0.2.1 — versión de prueba**](https://github.com/DiegoMiotti/fiado-app/releases/download/v0.2.1/Fiado-0.2.1-debug.apk)

[Notas de la versión y archivos de descarga](https://github.com/DiegoMiotti/fiado-app/releases/tag/v0.2.1)

</div>

El botón apunta directamente al APK de la Release: no necesitás compilar la app ni buscar archivos en el código. Mientras el repositorio sea privado, GitHub requiere una cuenta con acceso para descargarlo.

**Esta es una versión de prueba**, distribuida como APK debug con firma de depuración, fuera de Google Play. Si tenés una instalación con otra firma, Android puede impedir actualizarla. Desinstalar la anterior borra su borrador; tenelo en cuenta antes de hacerlo.

## Lo que necesitás saber

- **Android 15 o superior.** Las capturas de esta página se tomaron en Android 16.
- **Celulares del área 11 de Argentina.** La normalización está orientada a esos números: completa `+54 9 11` para los ocho dígitos locales y reconoce formatos como `11…`, `011…` y `011 15…`. Revisá siempre el destino; la app no modifica tu agenda.
- **Importes en pesos enteros.** No admite centavos. Ingresá solamente dígitos; los puntos de miles aparecen automáticamente. Cada campo admite de $1 a $999.999.999.
- **El envío lo confirmás vos.** Fiado prepara el texto; no verifica si el número tiene WhatsApp ni confirma envío, recepción o pago.
- **Preparar y copiar funciona sin Internet.** WhatsApp o el navegador necesitan su propia conexión para continuar. La app no solicita permiso de Internet.
- **Sin historial de deudas.** El borrador puede conservarse al recrear la pantalla mediante el estado de Android, pero no es un registro permanente de tus cuentas.

Si WhatsApp y WhatsApp Business están disponibles, la app intenta ofrecer un selector. Si ninguno resuelve el enlace, lo abre mediante el sistema, que puede usar un navegador.

## Tu información, con un alcance claro

Fiado no requiere una cuenta ni un servidor propio. Elegís un teléfono desde el selector de contactos del sistema; la app accede temporalmente al contacto elegido y no pide permiso general para leer toda tu agenda.

El portapapeles se usa cuando tocás **Copiar mensaje**. La app no registra nombres, teléfonos, importes ni mensajes en logs, y no solicita permisos de notificaciones ni accesibilidad.

## Una app pequeña, con trabajo detrás

Desarrollada en **Kotlin y Jetpack Compose**, con una interfaz nativa y reglas de importes, mensajes y teléfonos separadas del estado de la pantalla.

La revisión local del 3 de octubre de 2026 dejó **13 pruebas unitarias aprobadas**, compilación exitosa y lint con **0 errores y 10 advertencias**. También se revisaron las tres pantallas mostradas arriba en un teléfono real. La integración con WhatsApp tiene evidencia de una versión anterior y queda pendiente repetirla con el mensaje actual.

Podés consultar [la validación y sus pendientes](VALIDACION.md) y [las decisiones del proyecto](especs.md).

## ¿Encontraste algo para mejorar?

[Contalo en Issues](https://github.com/DiegoMiotti/fiado-app/issues): indicá qué intentabas hacer, qué ocurrió y qué versión de Android usás. Si adjuntás una captura, ocultá nombres, teléfonos y cualquier información personal.

---

<div align="center">

**Prepará la cuenta. Revisá el mensaje. Compartilo con Fiado.**

[**Descargar para Android**](https://github.com/DiegoMiotti/fiado-app/releases/download/v0.2.1/Fiado-0.2.1-debug.apk)

</div>
