# HX Launcher

Launcher de **Minecraft: Java Edition** para Android, hecho por **h07x**.

- GitHub: https://github.com/h07x
- Discord: https://discord.gg/tQ4mZstsxA

> ⚠️ **Unofficial Modified Version** — HX Launcher es una versión modificada y **no oficial** de
> [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2) de MovTery y colaboradores.
> No está afiliado ni respaldado por el proyecto original.

## Cambios respecto al original
- Nuevo nombre, ícono, banner y tema azul HX (modo oscuro por defecto).
- Nueva tipografía (Inter, licencia SIL OFL 1.1: `assets/fonts/INTER_LICENSE.txt`).
- Fondo personalizado por imagen/video (Ajustes del launcher).
- Importación de controles de PojavLauncher y derivados (MojoLauncher, etc.): ver `PojavLayoutConverter.kt`.
- Actualizaciones automáticas del proyecto original desactivadas.

## Compilar
Requiere Android Studio / JDK y SDK de Android.
```
./gradlew assembleDebug
```
El `applicationId` es `com.h07x.hxlauncher` (debug: `.debug`). El nombre se configura en `ZalithLauncher/gradle.properties`.

## Licencia
GPL-3.0 (ver `LICENSE`) con los términos adicionales (Sección 7) del proyecto original,
que se conservan en `README_ZALITH_ORIGINAL.md`:
1. Las versiones modificadas no pueden usar el nombre "ZalithLauncher" / "ZL" y deben indicar "Unofficial Modified Version".
2. No se deben eliminar los avisos de copyright que muestra el programa.

Créditos: Zalith Launcher 2 © 2025-2026 MovTery y colaboradores. Las librerías de terceros conservan sus licencias
(pantalla *Acerca de* dentro de la app).
