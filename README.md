# StopReels

StopReels es una aplicación nativa Android en Kotlin y Jetpack Compose que cubre, de forma local, señales de Reels dentro de Instagram para ayudar a reducir el desplazamiento compulsivo. Los vídeos compartidos en conversaciones privadas se dejan visibles.

## Estado del proyecto

El proyecto contiene:

- UI en Compose Material 3 con tema claro/oscuro, inglés por defecto y selector multidioma.
- Arquitectura sencilla MVVM con `StopReelsViewModel`.
- Preferencias y estadísticas locales mediante Preferences DataStore.
- `AccessibilityService` limitado al paquete `com.instagram.android`.
- Overlay opaco mediante `TYPE_ACCESSIBILITY_OVERLAY`, con desbloqueo temporal confirmable.
- Detección pura y testeable para feed, pestaña Reels, Direct, publicaciones normales, historias y cambios de pantalla.
- Sin permiso de Internet, sin cuentas, sin anuncios, sin analítica y sin capturas de pantalla.
- Copyright y licencia propietaria documentados en [COPYRIGHT.md](COPYRIGHT.md) y [LICENSE](LICENSE).

## Compilar

Requisitos: JDK 17 y Android SDK con la plataforma API requerida por Google Play.

Con Android Studio, abre esta carpeta, deja que sincronice Gradle y ejecuta `app`
en un emulador o dispositivo con Android 8.0 (API 26) o superior.

También se puede compilar sin Android Studio:

```bash
./gradlew test
./gradlew assembleDebug
```

El APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Probar en un dispositivo físico

1. Instala la variante debug desde Android Studio o con `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Abre StopReels y lee la pantalla de bienvenida.
3. Entra en **Abrir configuración de accesibilidad** y activa StopReels.
4. Vuelve a la aplicación y comprueba que el estado indica **Servicio activo**.
5. Abre Instagram y prueba la pestaña Reels, un Reel insertado en el feed y un vídeo compartido desde Direct.
6. Comprueba que los Reels fuera de Direct quedan cubiertos y que el vídeo de una conversación privada no queda cubierto.
7. Pulsa **Ver durante … segundos** si está activado y comprueba que el overlay vuelve al terminar el tiempo.
8. Desactiva el servicio desde Ajustes > Accesibilidad para verificar el flujo de apagado.

La activación del servicio y el overlay solo pueden probarse de forma real en Android; la lógica de detección está cubierta por tests JVM.

## Segundo plano y Xiaomi/POCO/Redmi

StopReels no mantiene un proceso propio ni necesita un `ForegroundService`: el trabajo lo realiza el `AccessibilityService` gestionado por Android mientras el usuario utiliza Instagram. La pantalla de StopReels puede estar cerrada.

En dispositivos Xiaomi, POCO o Redmi con MIUI/HyperOS, el sistema puede restringir servicios habilitados para ahorrar batería. Tras instalar la aplicación:

1. Abre StopReels y activa el servicio en **Ajustes > Accesibilidad**.
2. En **Ajustes > Aplicaciones > Gestionar aplicaciones > StopReels**, busca **Batería** y selecciona **Sin restricciones** cuando exista esa opción.
3. En **Ajustes > Aplicaciones > Permisos > Autoinicio en segundo plano**, activa StopReels si esa pantalla aparece en tu versión.
4. Evita deslizar StopReels fuera de Recientes si tu versión de HyperOS usa esa acción para limpiar procesos.

Los nombres exactos cambian entre versiones de MIUI/HyperOS. La aplicación incluye botones para abrir los ajustes de StopReels y la pantalla de batería. No se solicita `INTERNET`, no se inicia un servicio adicional y no se pide ignorar la optimización de batería de forma automática.

## Cómo diferencia Reels de Direct

`ReelDetectionEngine` trabaja sobre una copia pequeña de la jerarquía de accesibilidad de la ventana activa. Busca señales explícitas de Reel en texto, descripción de contenido o `viewIdResourceName`, y considera la posición inferior de un elemento para identificar accesos directos.

Antes de bloquear, calcula señales de contexto privado: Direct, mensajes, chat, conversación, el campo de escribir un mensaje y equivalentes en inglés. Si hay dos señales de conversación, o una señal junto con un campo editable de mensaje, el contexto se considera privado y no se crea ningún overlay, aunque el vídeo compartido mencione un Reel.

No se bloquea un vídeo genérico sin una señal de Reel. Esta decisión reduce falsos positivos sobre publicaciones normales, pero significa que Instagram podría mostrar un Reel que no exponga ninguna señal accesible reconocible.

## Publicar en Google Play

Consulta [GOOGLE_PLAY_CHECKLIST.md](GOOGLE_PLAY_CHECKLIST.md). Google Play requiere un
Android App Bundle firmado, una política de privacidad pública, el formulario Data safety
y una declaración específica porque la aplicación utiliza `AccessibilityService`.

## Privacidad

Consulta la [política de privacidad local](PRIVACY_POLICY.md). El servicio no solicita Internet ni captura la pantalla. Solo procesa temporalmente la jerarquía visible de Instagram y guarda preferencias/contadores en DataStore local.

## Limitaciones conocidas

- Instagram puede cambiar textos, IDs de recursos, jerarquía o ventanas sin aviso.
- Android y las versiones de Instagram pueden exponer distinta información a los servicios de accesibilidad.
- Ninguna heurística puede garantizar un bloqueo perfecto. El diseño prioriza no tapar Direct, historias desactivadas y publicaciones normales.
- Un overlay de accesibilidad puede comportarse de forma distinta según fabricante, modo multiventana, rotación o permisos de accesibilidad.
- En Xiaomi/POCO/Redmi, el ahorro de batería o el autoinicio pueden detener o retrasar el servicio hasta que el usuario marque la aplicación como **Sin restricciones**.
- El desbloqueo temporal se basa en tiempo de proceso y puede reiniciarse si Android mata el servicio.
- El contador representa detecciones locales del overlay, no una medición exacta del número de vídeos vistos.

## Estructura

```text
app/src/main/java/com/stopreels/
├── data/PreferencesRepository.kt
├── domain/ReelDetection.kt
├── service/StopReelsAccessibilityService.kt
├── ui/StopReelsApp.kt
├── ui/theme/Theme.kt
├── MainActivity.kt
├── StopReelsApplication.kt
└── StopReelsViewModel.kt
```
