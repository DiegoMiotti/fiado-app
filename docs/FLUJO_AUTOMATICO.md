# Foto → contacto → WhatsApp

Rama: `codex/flujo-foto-whatsapp-automatico`, basada en `e53c677` (cámara y suma alineada).

Esta variante parte de una foto y genera siempre un mensaje detallado con el formato monoespaciado de Complicados. La pantalla principal no muestra campos de importes ni selector de modo. Si hace falta remendar la lectura, **Corregir lectura** abre un editor opcional.

## Uso

1. Configurar una vez URL y token en **Conexión del backend**. Se reutiliza el almacenamiento cifrado existente; actualizar el APK con el mismo identificador y firma conserva esa configuración. Las copias con otro identificador tienen su propia configuración.
2. Pulsar **Sacar foto** y confirmar la captura en la cámara. La preparación y la consulta al backend arrancan automáticamente. **Elegir foto guardada** permite usar una foto existente con el mismo flujo. Antes de ambos botones se informa que confirmar la foto la envía al backend/proveedor y puede consumir cuota y saldo.
3. Si se detectan varias cuentas, elegir cuál cobrar. Nunca se suman cuentas de personas distintas.
4. Pulsar **Elegir contacto** y comprobar el número local. El saludo usa el nombre del contacto seleccionado; si está vacío, usa el nombre del papel.
5. Comprobar la foto, los movimientos y **Mensaje listo**. Pulsar **Confirmar y abrir WhatsApp** confirma esa lectura y abre el chat con el mensaje completo. El envío se confirma dentro de WhatsApp.

### Remendar una lectura

- **Corregir lectura** permite editar nombre, tipo, importe, referencia y detalle de cada movimiento. Se pueden agregar filas y excluir las incorrectas o los totales duplicados. La foto permanece visible en el editor.
- El editor recalcula saldo + cargos − pagos sin consultar al backend. **Descartar correcciones** conserva la cuenta anterior, su mensaje y el contacto.
- **Guardar correcciones** confirma los movimientos revisados y usa su total local, aunque el total de la IA sea nulo o distinto. Se conserva el total original de IA en el modelo; no se presenta el total corregido como validado por el servidor.
- Si la consulta falla o la IA no devuelve una cuenta utilizable, **Completar cuenta a mano** permite transcribir los movimientos mirando la misma foto, sin otra consulta de IA.
- La confirmación manual vale únicamente para el bloque corregido y esa foto. Otra foto vuelve a requerir una lectura o revisión propia.

El flujo no inicia consultas por recomposición, rotación o regreso desde WhatsApp. No hay reintentos automáticos. Si falla una consulta, **Reintentar lectura** hace una nueva consulta solo al tocarlo; puede consumir cuota. Cancelar no abre WhatsApp y descarta la cuenta en memoria.

## Mensaje y límites

Se genera la suma alineada de Complicados para cualquier combinación válida de movimientos:

- Un saldo anterior se muestra como `Saldo`, únicamente cuando aparece en el papel.
- Cada cargo se muestra con `+`.
- Cada pago se muestra como `- Pago` y se resta del total.
- Sin saldo anterior, se muestran los cargos/pagos reales: no se inventa un saldo cero.
- Se admiten cuentas de un solo movimiento y totales mayores al máximo del antiguo modo simple, siempre con cada importe dentro del límite existente de 1 a 999.999.999.
- El cálculo local usa `BigDecimal` y se compara con `total_calculado` del backend.

No se permite abrir WhatsApp si hay un importe incluido vacío/inválido, más de un saldo anterior, total cero/negativo, bloque de cliente pendiente de elección o teléfono inválido. Un total del backend nulo o distinto del cálculo local exige corregir/revisar y guardar expresamente la cuenta antes de continuar; no se ignora por sí solo. Se puede optar por otra foto. Las advertencias de la IA se muestran junto con la foto y el detalle para la confirmación final.

La foto y la cuenta sobreviven a rotación dentro del ViewModel. Se pierden si Android mata el proceso; en ese caso se toma otra foto y no se repite la consulta por sí sola. La configuración de conexión sí persiste cifrada.

## Código y pruebas

- `MainActivity.kt`: entrada exclusiva al flujo de fotos y apertura manual de WhatsApp.
- `readings/ReadingPanel.kt`: cámara/galería, resumen sin campos, contactos y mensaje listo.
- `readings/ReadingViewModel.kt`: preparación y consulta automáticas, selección explícita de cuenta y protección contra solicitudes duplicadas.
- `readings/AutomaticMessageRules.kt`: validación y mensaje detallado exacto con pagos y saldos opcionales; autorización explícita del total corregido localmente.
- `AutomaticMessageRulesTest.kt`, `ReadingViewModelTest.kt`: casos de cálculo, nulos, múltiples clientes, deduplicación y consulta automática con servicios simulados.
- `FiadoDeviceTest.kt`, `AutomaticMainDeviceTest.kt`: pantalla sin campos, conexión faltante, generación detallada, cancelación, contacto inválido y recreación de actividad. El callback de WhatsApp se simula: los tests no envían mensajes ni llaman a Gemini.

La lógica y las pruebas del modo manual anterior se conservan como código de compatibilidad, pero el modo manual no aparece en esta interfaz. La rama anterior mantiene su interfaz original.

Compilar: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console plain --max-workers=2`.

Las pruebas en teléfono usan la copia separada: `./gradlew.bat connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2`. El ejecutor desinstala esa copia al terminar y no apunta a la app habitual. Ver [desarrollo](DESARROLLO.md) y [validación](../VALIDACION.md).
