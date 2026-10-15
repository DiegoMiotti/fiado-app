# Fiado 0.3.0-rc1

Rama de integración: `codex/fiado-candidata-oficial`. Fecha: 15 de octubre de 2026.

Esta versión candidata reúne la generación automática del mensaje desde una foto con IA/OCR y el rediseño visual de Fiado. Es la base para las próximas funcionalidades; todavía no es una publicación oficial ni una nueva Release de GitHub.

## Integración

- Base funcional: `codex/flujo-foto-whatsapp-automatico`, commit `9ecfbea`, que ya incluye `codex/mejora-deteccion-ocr-ia` y la corrección opcional de lecturas.
- Interfaz: `codex/rediseno-visual-fiado`, commit `3e912b1`, construido sobre esa misma base funcional.
- Se guardó el rediseño en su rama y se creó la rama candidata desde ese commit. Al integrar la última referencia de la rama automática, Git confirmó `Already up to date`: su contenido ya era antecesor del rediseño. No hicieron falta resoluciones de conflictos ni un commit de merge artificial.
- Identificador de versión: `0.3.0-rc1`, código Android `4`. El nombre de la aplicación sigue siendo **Fiado** y el paquete habitual sigue siendo `ar.com.fiado`, para conservar la identidad y permitir actualizaciones compatibles.

## Funciones incluidas

Foto de cámara o galería → lectura automática → revisión del detalle y total → contacto → mensaje listo para WhatsApp. Corrección opcional sin otra consulta a la IA. El diseño destaca importes, destinatario y acción principal, con soporte para cifras largas y fuentes ampliadas.

Las reglas de cálculo, controles, backend, credenciales y apertura de WhatsApp conservan su implementación. No se agregaron funcionalidades.

## Compilación y pruebas

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console plain --max-workers=2
.\gradlew.bat connectedDebugAndroidTest -PisolatedDeviceTests=true --console plain --max-workers=2
```

Las pruebas conectadas usan `ar.com.fiado.ocrtest`, separado de la instalación personal. Resultados efectivos y pendientes: [VALIDACION.md](../VALIDACION.md). El workflow histórico de publicación está fijado a `0.2.1`: publicar oficialmente esta candidata requerirá preparar un proceso de release para su versión; esta integración no publica ni reemplaza una release.

APK candidato: `app/build/outputs/apk/candidate/Fiado-0.3.0-rc1.apk`. APK de prueba aislada: `app/build/outputs/apk/candidate/Fiado-0.3.0-rc1-pruebas.apk`.

Validación de la candidata: 47 pruebas unitarias aprobadas (resultados reutilizados sin cambios de código), lint con 0 errores y 42 advertencias existentes, compilación correcta y **16 pruebas instrumentadas aprobadas** en Motorola Edge 50 Fusion con Android 16. Tras reconectar ADB, se actualizó la instalación habitual con `adb install -r` y se abrió correctamente; el teléfono informa versión `0.3.0-rc1`, código `4`. No se desinstaló la app ni se borraron sus datos.
