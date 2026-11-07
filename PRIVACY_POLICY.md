# Política de privacidad local de StopReels

Última actualización: 5 de octubre de 2026.

StopReels funciona sin cuentas, sin servidores, sin anuncios y sin analítica. La aplicación no tiene una función de red y el proyecto no declara el permiso `INTERNET`.

## Servicio de accesibilidad

StopReels solicita que el usuario active voluntariamente un `AccessibilityService` de Android. El servicio se ejecuta únicamente para observar la jerarquía de accesibilidad de la aplicación `com.instagram.android` cuando está visible. Puede leer temporalmente textos visibles, descripciones de contenido, identificadores de recursos y límites de los elementos para estimar si algo es un Reel.

La aplicación no mantiene un servicio propio de primer plano ni solicita permiso de autoinicio o de exclusión de optimización de batería de forma automática. En algunos teléfonos Xiaomi/POCO/Redmi el usuario puede elegir manualmente “Sin restricciones” o habilitar el autoinicio para evitar que el sistema detenga un servicio de accesibilidad ya autorizado.

El servicio no solicita ni usa capturas de pantalla, pulsaciones, contraseñas, gestos, micrófono, cámara, contactos o ubicación. Ignora las demás aplicaciones y no realiza scraping de servidores de Instagram.

## Datos guardados

Preferences DataStore guarda solo en el dispositivo:

- preferencias de bloqueo;
- duración del desbloqueo temporal;
- si se vio la bienvenida;
- contador local de Reels cubiertos;
- contador local de desbloqueos temporales.

No se guarda texto de Instagram, imágenes, vídeos, nombres de usuario, conversaciones ni el árbol de accesibilidad.

## Compartición y conservación

No se comparte ningún dato con StopReels, OpenAI, Meta, Instagram ni terceros. Los datos permanecen hasta que el usuario borra los datos de la aplicación o reinstala la aplicación. El usuario puede restablecer los dos contadores desde Configuración.

## Limitaciones

La detección depende de la interfaz que Instagram exponga a Android y puede dejar de reconocer algunos elementos cuando Instagram cambie su diseño. StopReels explica esta limitación dentro de la aplicación y usa una heurística conservadora para evitar bloquear conversaciones privadas.

## Desactivación

El servicio puede desactivarse desde StopReels apagando **Bloquear Reels**, o desde **Ajustes de Android > Accesibilidad > Aplicaciones instaladas > StopReels**. También se puede desinstalar la aplicación desde Android.
