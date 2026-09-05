# Fiado — especificación del MVP para Android 16

Estado: lista para que GPT Sol o Terra complete las decisiones técnicas e implemente la primera prueba.
Fecha: 2026-09-05.

## 1. Objetivo

Crear una app Android sencilla que permita ingresar el importe del fiado de hoy, generar un mensaje con un saludo según la hora y copiarlo o abrir el chat de WhatsApp del teléfono elegido desde los contactos de Android.

Ejemplo de mensaje final:

> Buenos días, el fiado de hoy es $1.500,00.

La generación del texto es automática mediante una plantilla local: no requiere inteligencia artificial ni servicios externos. En esta prueba, abrir WhatsApp prepara el mensaje; el usuario pulsa Enviar allí. No prometer envío automático ni confirmación de entrega.

## 2. Alcance y valores iniciales

- Plataforma requerida: Android 16 (API 36), teléfono físico como destino de la prueba.
- Idioma: español. Nombre visible provisional: Fiado.
- Moneda inicial asumida: pesos argentinos (ARS), símbolo `$`, formato `es-AR`.
- Un destinatario y un importe por operación.
- Importe ingresado manualmente: no se calcula un saldo ni se suman deudas anteriores.
- Sin cuenta, servidor, base de datos, publicidad ni analítica.
- Sin historial persistente en esta versión. Conservar el borrador al rotar y al volver de contactos o WhatsApp; ofrecer Limpiar para comenzar otro.
- Copiar funciona sin seleccionar un contacto y sin conexión a Internet.
- La frase «por el momento vamos viendo cómo sigue la prueba» describe el alcance del proyecto; no forma parte del mensaje.

Estos valores son decisiones provisionales para poder avanzar. El implementador debe documentar cualquier ajuste sin ampliar el alcance por iniciativa propia.

## 3. Flujo principal y pantalla

Una pantalla con los siguientes elementos, en orden:

1. Título «Fiado».
2. Botón «Elegir contacto», que abre el selector de teléfonos de Android.
3. Nombre y número seleccionados, con acción para cambiar el contacto. El número debe quedar visible antes de abrir WhatsApp.
4. Campo «Importe de hoy», con teclado decimal, símbolo de moneda y validación visible.
5. Vista previa de solo lectura del mensaje completo, seleccionable para copiar manualmente si fuera necesario.
6. Botón «Copiar mensaje».
7. Botón «Abrir WhatsApp», acompañado por «Revisá el mensaje y tocá Enviar en WhatsApp».
8. Acción «Limpiar» que borra contacto, importe y mensaje.

Actualizar la vista previa al cambiar el importe. Deshabilitar Copiar si el importe no es válido; deshabilitar Abrir WhatsApp si además falta un número válido. Mostrar el motivo cerca del campo correspondiente. Cancelar el selector conserva el borrador previo.

## 4. Reglas del mensaje

Plantilla exacta: `{saludo}, el fiado de hoy es ${importeFormateado}.`

El saludo toma la hora local y zona horaria actuales del dispositivo:

| Intervalo local | Saludo |
| --- | --- |
| 06:00 inclusive a 12:00 exclusive | Buenos días |
| 12:00 inclusive a 20:00 exclusive | Buenas tardes |
| 20:00 inclusive a 06:00 exclusive | Buenas noches |

Son franjas iniciales ajustables en código, sin pantalla de configuración para el MVP. Recalcular al volver al primer plano, al cambiar de franja con la pantalla abierta y justo antes de copiar o abrir WhatsApp. Actualizar la vista previa con el mismo texto que se transfiere para evitar un saludo desactualizado.

Importes:

- Aceptar valores mayores que cero, hasta 999.999.999,99, con un máximo de dos decimales.
- Entrada sin separador de miles; admitir coma o punto como separador decimal. Ejemplos válidos: `1500`, `1500,5`, `1500.50`.
- Rechazar vacío, cero, negativos, letras, separadores múltiples y más de dos decimales. No redondear silenciosamente ni interpretar `1.500` como mil quinientos.
- Mostrar siempre dos decimales y miles con punto: `1500` produce `$1.500,00`.
- Representar el dinero con decimal exacto o centavos enteros; no usar Float/Double.
- No agregar comillas, nombre del contacto ni otras frases al mensaje.

## 5. Contactos de Android

Usar el selector del sistema para elegir un número telefónico, mediante `Intent.ACTION_PICK` y `ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE`, con la API de resultados de actividad. Consultar únicamente el resultado seleccionado; no cargar toda la agenda.

La selección debe identificar un teléfono concreto incluso si una persona tiene varios. Manejar cancelación, resultado nulo, ausencia de proveedor, número vacío y fallos al consultar la URI sin cerrar la app. Evitar solicitar acceso general `READ_CONTACTS` para este flujo y comprobar el comportamiento del proveedor en el dispositivo de prueba.

Mostrar el teléfono original y permitir corregir el número destinado a WhatsApp sin modificar el contacto de Android. Pedir formato internacional con prefijo de país si no se puede determinar de forma segura. No inventar un prefijo, ni transformar automáticamente números argentinos locales ambiguos. Un número bien formado no garantiza que tenga WhatsApp.

## 6. Integración con WhatsApp y portapapeles

Construir el enlace oficial de click-to-chat:

```text
https://wa.me/{numeroInternacionalSoloDigitos}?text={mensajeCodificado}
```

El destino debe incluir el código de país y solo dígitos, sin `+`, espacios, paréntesis ni guiones. Validar el número internacional antes de construir la URL; no basta con eliminar todos los caracteres no numéricos de una entrada arbitraria. Codificar el mensaje completo como parámetro URL en UTF-8 exactamente una vez, preservando tildes, espacios y `$`.

Al pulsar Abrir WhatsApp, intentar abrir el enlace mediante un intent. Priorizar WhatsApp si hay un manejador compatible; si hay varias variantes disponibles, permitir elegir. No depender de actividades internas ni identificadores privados de chats. La resolución puede involucrar un selector o navegador según el dispositivo: comprobar la ruta real y documentarla.

Si no se puede abrir, conservar el borrador y mostrar «No se pudo abrir WhatsApp. Podés copiar el mensaje y pegarlo manualmente». Capturar el fallo de lanzamiento; si se consulta disponibilidad de paquetes, configurar únicamente la visibilidad necesaria. Nunca marcar el mensaje como enviado: este flujo no devuelve una confirmación de envío o entrega.

Copiar usa el portapapeles del sistema solo cuando el usuario pulsa el botón. Copiar exactamente el texto de la vista previa actualizada, sin copiar el número ni la URL. No leer el portapapeles. Evitar avisos duplicados si Android ya muestra la confirmación de copia.

## 7. Implementación propuesta

- App nativa: Kotlin, Jetpack Compose y Material 3, una Activity.
- `minSdk = 36`, `targetSdk = 36`, `compileSdk = 36` para limitar esta primera prueba al entorno pedido. Compatibilidad con versiones anteriores queda fuera de alcance.
- Elegir y fijar versiones estables y compatibles de JDK, Gradle, Android Gradle Plugin y Kotlin tras revisar el entorno. Incluir Gradle Wrapper.
- Separar funciones de saludo, validación/formato monetario y construcción del enlace de las operaciones Android; inyectar reloj para probar límites horarios.
- Estado de pantalla en ViewModel y mecanismo de restauración del borrador apropiado; sin persistencia duradera del historial.
- Recursos de texto en español, contraste legible, etiquetas accesibles y controles utilizables con fuente grande.
- Respetar insets de barras del sistema y teclado en Android 16; permitir desplazamiento en pantallas pequeñas.
- Generación y copia locales. No agregar permiso de Internet por abrir un intent externo; WhatsApp gestiona su propia conectividad.
- No registrar nombres, teléfonos, importes ni URLs con mensajes en logs. No incluir contactos reales en fixtures o capturas de ejemplo.

## 8. Fuera de alcance

Envíos programados, masivos o en segundo plano; automatización mediante accesibilidad o simulación de toques; WhatsApp Business Platform; lectura de chats; comprobantes; cuentas corrientes; sincronización; IA generativa; publicación en Play Store. Si luego se pide envío sin intervención, diseñarlo como otra etapa y evaluar la integración oficial y sus requisitos en ese momento.

## 9. Criterios de aceptación y pruebas

| Caso | Resultado esperado |
| --- | --- |
| Hora 05:59 / 06:00 | Buenas noches / Buenos días |
| Hora 11:59 / 12:00 | Buenos días / Buenas tardes |
| Hora 19:59 / 20:00 / 00:00 | Buenas tardes / Buenas noches / Buenas noches |
| Cambia la franja con la pantalla abierta o la app en segundo plano | Vista previa y próxima acción usan el saludo actualizado |
| Importe `1500`, a las 09:00 | `Buenos días, el fiado de hoy es $1.500,00.` |
| Importe `1500,5` o `1500.50` | Mismo importe final: `$1.500,50` |
| Entrada inválida o fuera del límite | Error claro; copiar y abrir deshabilitados |
| Copiar sin contacto y sin red | Texto exacto disponible para pegar |
| Elegir persona con varios teléfonos | Se identifica explícitamente el teléfono elegido |
| Cancelar selector, URI ilegible o teléfono ausente | Sin cierre inesperado ni pérdida del borrador |
| Número local ambiguo | Solicita corrección internacional antes de abrir el chat |
| WhatsApp disponible y destinatario de prueba válido | Abre el chat correcto con texto completo; falta pulsar Enviar |
| WhatsApp ausente o sin manejador compatible | Alternativa de copia disponible; sin cierre inesperado |
| Número sin WhatsApp o sin conexión | No afirma envío exitoso; permite volver y copiar |
| Rotar, volver de WhatsApp, usar fuente grande y teclado abierto | Borrador conservado y controles accesibles |
| Pulsar Limpiar | Contacto e importe vacíos; mensaje y acciones reiniciados |

Automatizar pruebas unitarias de reglas horarias, dinero y codificación/validación del enlace. Probar contactos, portapapeles e intents en Android 16; usar un teléfono real con WhatsApp para comprobar el chat y el texto prellenado. No enviar mensajes reales como parte de pruebas automatizadas. Diferenciar siempre pruebas ejecutadas de pruebas pendientes: compilar un APK no demuestra que la integración con WhatsApp funcione.

## 10. Encargo y entregables para GPT Sol o Terra

Leer este archivo, inspeccionar el entorno e implementar el MVP sin expandir el alcance. Completar las siguientes decisiones con valores y evidencia reales durante la implementación:

- [ ] Identificador de aplicación y versiones de herramientas elegidas.
- [ ] Mecanismo de validación de teléfonos y tratamiento de entradas ambiguas.
- [ ] Estrategia de resolución de WhatsApp y variantes instalada(s) probada(s).
- [ ] Proyecto Android completo y Gradle Wrapper.
- [ ] Pruebas unitarias ejecutadas, comando y resultado.
- [ ] APK de prueba compilado, con ruta exacta; si falta SDK/JDK, documentar el bloqueo sin declarar compilación exitosa.
- [ ] README con requisitos, apertura en Android Studio, compilación, instalación del APK y pasos de prueba manual.
- [ ] Registro breve de dispositivo/API, pruebas reales de integración y limitaciones pendientes.

Trabajar primero en generación/copia; después integrar contactos y apertura de WhatsApp. No crear infraestructura adicional ni requerir credenciales para esta prueba.

## 11. Referencias oficiales

Consultadas el 2026-09-05; verificar nuevamente al implementar si cambian las APIs o herramientas.

- [WhatsApp: click-to-chat y mensaje prellenado](https://faq.whatsapp.com/5913398998672934).
- [Android: intents comunes y selección de un teléfono de contactos](https://developer.android.com/guide/components/intents-common).
- [Android: configuración del SDK de Android 16](https://developer.android.com/about/versions/16/setup-sdk).

Estas referencias sustentan la integración propuesta; el funcionamiento en el teléfono todavía debe probarse. Los horarios, moneda, límites de importe y alcance son decisiones de producto de esta especificación.
