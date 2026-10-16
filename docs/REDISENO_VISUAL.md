# Rediseño visual de Fiado

Fecha: 15 de octubre de 2026. Rama: `codex/rediseno-visual-fiado`.

## Inspección y propuesta

La base es la rama `codex/flujo-foto-whatsapp-automatico`: `MainActivity` presenta `ReadingPanel`, con lectura de foto, revisión de movimientos, corrección manual opcional, selección de contacto y mensaje detallado. No hay un selector de modo manual/Complicados en esta versión. Se conserva ese flujo.

La UI anterior tenía un encabezado ocre dominante, importes con el tamaño del texto corriente, contacto sin una superficie propia y botones de jerarquía similar. El total local no tenía un lugar destacado fuera del diálogo de corrección.

Propuesta implementada: priorizar los números, dividir el recorrido en tres etapas y conservar el verde de Fiado. La [referencia de cuenta digital de Mercado Pago Argentina](https://www.mercadopago.com.ar/cuenta) orienta la claridad de la jerarquía; no se utilizan sus marcas, recursos gráficos ni su paleta.

## Cambios

- Tema Material 3 propio, con verde oscuro, verde claro para el total, tarjetas blancas y fondo gris verdoso. Tipografía del sistema Android: cuerpo de 18 sp, botones de 20 sp, títulos de 24–28 sp y nombres de contacto de 28 sp. Sin fuentes ni dependencias nuevas.
- Recorrido visible: **Cargá la cuenta**, **Revisá los importes**, **Prepará el mensaje**. Acciones de cámara/galería agrupadas y corrección junto a los movimientos.
- Total calculado y total de corrección en una tarjeta destacada; se usa exclusivamente `ReviewRules.total`, sin cambiar reglas ni validaciones. Un total calculado no implica que esté aprobado: se conserva el bloqueo de WhatsApp y los avisos existentes.
- Total de 48 sp cuando entra. Medición del texto con la densidad y el escalado de fuentes actuales; alternativas de 44, 40, 36 y 32 sp. Si el valor todavía no entra, permanece íntegro, con desplazamiento horizontal y una indicación visible. El lector de pantalla recibe el valor completo en pesos. No se abrevia, redondea ni limita el total a nueve dígitos.
- Importes individuales con separadores de miles, signo y texto de 24 sp; entrada de corrección de 32 sp con desplazamiento nativo del campo. La vista previa conserva su bloque monoespaciado y desplazamiento horizontal.
- Contacto en una tarjeta propia con nombre grande y teléfono. Botón principal **Confirmar y abrir WhatsApp** de al menos 72 dp de alto, texto adaptable a varias líneas y relleno generoso. Cámara de al menos 64 dp y contacto de 60 dp.
- Contenido desplazable, ancho máximo de 600 dp en la pantalla principal, espacio para barras del sistema y teclado. Alturas mínimas en lugar de alturas fijas para permitir fuente ampliada.
- Transiciones de opacidad de 120–180 ms para la lectura y de tamaño de 180 ms para el contacto, mediante animaciones Compose que siguen la escala de animación de Android. Errores con texto, superficie de contraste y región accesible de anuncios; encabezados con semántica de título.

## Alcance

Solo se modifican UI, tema y pruebas de interfaz. `ReadingViewModel`, modelos, reglas monetarias, generación de mensajes, backend, preparación de fotos, almacenamiento de credenciales y funciones de contactos/WhatsApp conservan su implementación. El mensaje que recibe WhatsApp no cambia por el formato visual de los importes.

## Verificación

Comando de compilación, pruebas unitarias y análisis:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --console plain --max-workers=2
```

Las pruebas existentes de flujo usan ahora `FiadoTheme`. Se agregaron tres casos instrumentados: total habitual en tarjeta de 280 dp, total de diez dígitos con fuente al 200 % y desplazamiento horizontal, y recorrido de total/contacto/botón de WhatsApp con fuente al 200 % y contacto largo. Usan datos ficticios y apertura de WhatsApp simulada.

Los resultados efectivos se registran en [VALIDACION.md](../VALIDACION.md). Compilar el APK de tests no equivale a ejecutarlos.

Para ejecutar en dispositivo sin usar el paquete personal:

```powershell
.\gradlew.bat connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2
```

Resultado en Motorola Edge 50 Fusion, Android 16: **16/16 pruebas instrumentadas aprobadas**, sin fallos, errores ni omisiones. Se ejecutaron con el paquete aislado `ar.com.fiado.ocrtest`, incluyendo los tres casos nuevos de accesibilidad y tamaños. El APK aislado quedó instalado y abierto; su copia se conserva en `app/build/outputs/apk/visual/Fiado-visual-pruebas.apk`.

Se inspeccionó visualmente la pantalla inicial en vertical, sin recortes visibles: [captura del teléfono](screenshots/rediseno-inicio.png). La conexión sin configurar de la copia de pruebas muestra correctamente el aviso y los botones deshabilitados. No se modificó la instalación habitual.

Queda pendiente la revisión manual de los demás estados, orientación horizontal, fuente al 150 %, TalkBack, teclado de corrección y animaciones desactivadas. La fuente al 200 %, la tarjeta angosta y el total largo desplazable están cubiertos por pruebas instrumentadas. No se realizaron consultas al proveedor de IA ni envíos de WhatsApp durante esta implementación.
