# Minimal Time

Réplica educativa, hecha con código propio, de un launcher minimalista con control
de tiempo de pantalla (estilo *minimalist phone: Screen Time*). Sin anuncios, sin
conexión a internet y sin recopilar ningún dato: todo se queda en tu móvil.

## Funciones

- **Launcher minimalista**: pantalla de inicio en texto plano (sin iconos) con reloj,
  fecha y tus apps favoritas. Puedes establecerla como launcher predeterminado o
  usarla como una app normal.
- **Tiempo de pantalla real**: lee las estadísticas de uso del sistema
  (UsageStatsManager): total de hoy, comparación con ayer, desbloqueos del día,
  gráfica de los últimos 7 días y desglose por aplicación.
- **Límites diarios por app**: 5 min / 15 min / 30 min / 1 h / 2 h. Al superarlo,
  aparece una pantalla de bloqueo encima de la app.
- **Bloqueo manual de apps**: bloquea cualquier app al instante.
- **Modo monje**: bloquea de golpe todas las apps que hayas marcado como
  «distractoras».
- **Servicio de bloqueo** en segundo plano con notificación silenciosa; se
  reactiva solo al reiniciar el móvil.

## Instalación

1. Copia `app/build/outputs/apk/debug/app-debug.apk` al móvil (cable, Quick Share,
   Telegram «mensajes guardados»…) y ábrelo. Acepta «instalar apps desconocidas».
   - Con cable también puedes: `adb install app-debug.apk`
     (adb está en `%LOCALAPPDATA%\Android\Sdk\platform-tools`).
2. Abre **Minimal Time** y entra en **ajustes**. Concede:
   - **Acceso a datos de uso** → necesario para las estadísticas.
   - **Mostrar sobre otras apps** → necesario para la pantalla de bloqueo.
   - **Notificaciones** (Android 13+) → para la notificación del servicio.
3. Activa el **servicio de bloqueo** en ajustes.
4. (Opcional) **Usar como launcher** → elige Minimal Time como app de inicio.

## Uso

- En **aplicaciones**: toque = abrir; **pulsación larga** = añadir a favoritas,
  marcar como distractora, bloquear o poner límite diario.
- En la pantalla de inicio, pulsación larga sobre una favorita abre las mismas opciones.
- **tiempo** muestra las estadísticas; **ajustes**, permisos y modo monje.

## Compilar de nuevo

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

Proyecto Kotlin puro (sin dependencias externas), compileSdk 34, minSdk 26
(Android 8.0+). El contador de desbloqueos requiere Android 9+.
