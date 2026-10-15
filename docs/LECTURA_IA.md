# Primera versión de lectura de papel

La integración vive en `codex/mejora-deteccion-ocr-ia`. Conserva la carga manual y WhatsApp con confirmación del envío. No crea historial ni catálogo de clientes.

## Instalar y configurar

1. Compilar desde PowerShell: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console plain`.
2. Instalar `app/build/outputs/apk/debug/app-debug.apk` en Android 15 o superior. Con depuración USB: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Si cambia la firma, revisar el borrador antes de desinstalar.
3. Abrir **Conexión del backend**. URL inicial: `https://fiado-backend.vercel.app`.
4. Ingresar el token privado del teléfono directamente en la app y guardar. No usar la clave de Gemini ni la de Redis. Un campo vacío conserva el token en el mismo servidor; para otro servidor se exige uno nuevo.
5. **Borrar credencial** elimina la configuración guardada. No escribir el token en archivos del repositorio, comandos, chats ni capturas.

El diálogo de conexión bloquea capturas de Android y oculta el token. La configuración se cifra con AES-GCM y una clave de Android Keystore, en `noBackupFilesDir`. El manifiesto y las reglas existentes deshabilitan backup y transferencia de datos. El token no entra en `SavedStateHandle`, BuildConfig ni registros de red.

Si la app instalada tiene otra firma, se puede compilar una copia independiente con `./gradlew.bat assembleDebug -PisolatedDeviceTests=true --console plain`. Se instala como **Fiado (pruebas OCR)** (`ar.com.fiado.ocrtest`), con su propia configuración y sin reemplazar la instalación existente. Para correr las pruebas en esa copia, usar `connectedDebugAndroidTest` con la misma propiedad. El APK debug habitual se regenera sin esa propiedad.

## Probar una foto ficticia

Preparar una hoja sin información real: «Leo, saldo anterior 10000, lunes 4200, martes 13000, pagó 3000». Fotografiarla con buena luz y guardar la imagen.

1. Cargar primero un importe manual para comprobar que cancelar conserva el borrador.
2. Pulsar **Elegir foto**, revisar la vista reducida y enviar para lectura. Esta acción consulta al backend y puede consumir cuota/saldo. Las pruebas automatizadas no realizan esta llamada.
3. Revisar el nombre, los cargos, el saldo y el pago. El total esperado del ejemplo es **24.200 pesos**. Corregir letras y cifras; excluir filas que sean totales repetidos. Resolver todo importe vacío o excluirlo.
4. Elegir el bloque correspondiente si hay varios clientes. Elegir y comprobar el contacto con el selector de Android; corregir el teléfono si hace falta. Los candidatos de IA no asignan contactos automáticamente: no existe un catálogo local para resolver sus IDs.
5. Marcar la revisión completa y pulsar **Confirmar y reemplazar cobro**. El ejemplo contiene un pago y se aplica como total simple. Los campos anteriores se reemplazan.
6. Revisar el mensaje y abrir WhatsApp. Comprobar el destinatario y confirmar manualmente allí.

También verificar: cancelar antes y durante la consulta, token faltante/incorrecto, foto inválida, falta de red, avisos de cuota y proveedor, importe nulo, total cero/negativo y varios saldos anteriores. No repetir consultas automáticamente tras un error: una solicitud fallida o cancelada puede haber consumido cuota.

## Decisiones y límites

- Selector de fotos de Android, sin permisos amplios de almacenamiento ni `READ_CONTACTS`. Contactos mediante `ACTION_PICK`; se consulta solo el contacto elegido.
- Se envía JPEG con Base64 sin saltos de línea y `clients=[]`; nunca teléfonos. No hay OCR local: el backend interpreta la imagen mediante IA.
- `ImageDecoder` aplica orientación EXIF y limita a 2400 píxeles de lado mayor antes de decodificar. La compresión reduce calidad/dimensiones hasta cumplir 2.800.000 bytes; el cuerpo también se valida contra 4.000.000 bytes. No se guardan copias de la foto en disco.
- Red y preparación fuera del hilo principal. Límite global de 65 segundos por consulta, además de timeouts HTTP de conexión y lectura. Sin redirecciones ni reintentos automáticos; una petición simultánea por flujo.
- Cálculo local exacto con `BigDecimal`: saldos + cargos − pagos. Cada importe admite los límites manuales existentes (1 a 999.999.999); cifras mayores se muestran completas y requieren corregir/excluir, sin truncarlas.
- Modo simple hasta 999.999.999 de total positivo. Complicados solo con exactamente un saldo positivo, al menos un cargo y ningún pago incluido. Puede representar un total mayor conservando cada importe válido. Varios saldos generan un aviso y requieren revisión manual.
- Foto y revisión viven en memoria del ViewModel: sobreviven a rotación, pero se descartan si Android mata el proceso. El cobro confirmado sigue usando el SavedStateHandle existente. Cancelar/liberar la revisión conserva el cobro anterior.
- La URL admite un origen HTTPS sin rutas, credenciales, query ni fragmento. Cambiar de servidor exige ingresar otro token para evitar enviar el anterior a otro destino.
- Errores del proveedor y del token del teléfono tienen mensajes diferentes. No se muestran cuerpos de error arbitrarios que puedan contener datos sensibles.

## Validación

Las pruebas unitarias de `ReadingRulesTest` y `ReadingViewModelTest` usan JSON y servicios simulados, sin acceso al backend. Cubren parseo, nulos, pagos, límites, saldos múltiples, confirmación, reemplazo del borrador, errores, cancelación, credenciales y prevención de consultas duplicadas. Las pruebas anteriores de MessageRules se conservan.

Una compilación o una prueba simulada no verifica Gemini con fotos. La prueba real de esta sección debe hacerse en el teléfono con una foto ficticia y token ingresado en la app.

## Archivos de la integración

| Archivo | Responsabilidad |
|---|---|
| `readings/ConnectionStore.kt` | URL HTTPS y credencial cifrada en Keystore |
| `readings/PhotoPreparer.kt` | Decodificación orientada, dimensiones y compresión JPEG |
| `readings/ReadingBackend.kt` | Solicitud HTTP cancelable, límites y errores |
| `readings/ReadingJson.kt` | Contrato JSON, nulos y mensajes de error |
| `readings/ReadingModels.kt` | Modelos y reglas exactas de revisión/aplicación |
| `readings/ReadingViewModel.kt` | Estado del flujo, confirmación, timeout y exclusión de solicitudes duplicadas |
| `readings/ReadingPanel.kt` | Configuración, selectores de foto/contacto y revisión Compose |
| `MainActivity.kt`, `FiadoViewModel.kt` | Entrada al flujo y reemplazo validado del borrador |
| `AndroidManifest.xml`, `app/build.gradle.kts` | Permiso INTERNET y dependencias de pruebas |
| `ReadingRulesTest.kt`, `ReadingViewModelTest.kt`, `ReadingDeviceTest.kt` | Pruebas simuladas y pruebas de cifrado/EXIF en Android |

Los archivos Kotlin de producción están bajo `app/src/main/java/ar/com/fiado/`; los tests están en sus respectivos árboles `test` y `androidTest`. `README.md` enlaza esta guía y `VALIDACION.md` distingue la evidencia de esta integración de las validaciones históricas.

## Sacar una foto desde la app

Tocá **Sacar foto** para abrir la cámara del teléfono, o **Elegir foto** para usar la galería. La captura se muestra en la revisión; no se consulta al backend hasta tocar **Enviar foto para lectura**. Si cancelás la cámara, el cobro se conserva.

La cámara guarda una captura temporal en la caché privada mediante FileProvider. Se elimina al preparar la imagen o cancelar la cámara; las capturas abandonadas se limpian después de 24 horas al iniciar otra captura. Las fotos elegidas de la galería no se borran. El URI pendiente se conserva durante la recreación de la actividad. Se usa la aplicación de cámara del teléfono mediante `ActivityResultContracts.TakePicture`.
