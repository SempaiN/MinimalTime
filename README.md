# Minimal Time

Launcher minimalista para Android con control de tiempo de pantalla, hecho por y
para Nacho ([@SempaiN](https://github.com/SempaiN)). Sin anuncios, sin conexión a
internet y sin recopilar ningún dato: todo se queda en tu móvil.

Kotlin puro, sin ninguna dependencia externa. La UI se construye por código.

**APKs listas para instalar en [Releases](https://github.com/SempaiN/MinimalTime/releases).**

## Funciones

### Launcher
- Pantalla de inicio en texto plano (sin iconos): saludo personalizado, reloj,
  fecha y tus apps favoritas.
- Lista de todas las apps con buscador; **apps ocultas** para las que no quieres ver.
- Puedes establecerla como launcher predeterminado o usarla como app normal.

### Tiempo de pantalla (datos reales del sistema)
- Total de hoy, comparación con ayer y **objetivo diario** configurable con % de progreso.
- Desbloqueos del día y **aperturas por app**.
- Gráfica de los últimos 7 días y desglose por aplicación.
- **Exportación a CSV** en Descargas (minutos y aperturas por app).

### Protecciones contra distracciones
- **Límites diarios por app** (5 min – 2 h) con pantalla de bloqueo.
- **Bloqueo manual** de cualquier app.
- **Modo monje**: bloquea todas las apps marcadas como distractoras.
- **Horario de modo monje**: se activa solo cada día (p. ej. 22:00 – 07:00).
- **Modo concentración**: sesiones de 15/25/50/90 min que bloquean las distractoras.
- **Respiro consciente**: pausa de 10 segundos con cuenta atrás antes de entrar en
  una app distractora — decide si de verdad quieres entrar.

## Instalación

1. Descarga la APK desde [Releases](https://github.com/SempaiN/MinimalTime/releases)
   y ábrela en el móvil (acepta «instalar apps desconocidas»).
2. Abre **Minimal Time** → *ajustes* y concede:
   - **Acceso a datos de uso** (estadísticas)
   - **Mostrar sobre otras apps** (pantalla de bloqueo)
   - **Notificaciones** (Android 13+)
3. Activa el **servicio de bloqueo**.
4. En *aplicaciones*: toque = abrir; **pulsación larga** = favorita, distractora,
   bloquear, límite diario u ocultar.

Requiere Android 8.0+ (contador de desbloqueos: Android 9+).

## Compilar

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"   # o cualquier JDK 17+
.\gradlew.bat :app:assembleDebug
```

La APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Historial de versiones

- **v2.0** — saludo personalizado, objetivo diario, modo concentración,
  respiro consciente, horario de modo monje, apps ocultas, aperturas por app,
  frases propias en el bloqueo y exportación CSV.
- **v1.0** — réplica base: launcher de texto, estadísticas, límites, bloqueo,
  modo monje y servicio en primer plano.
