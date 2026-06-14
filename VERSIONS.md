# Versiones (historial de APKs)

El **apartado oficial de versiones** de este proyecto es la página de *Releases* de GitHub.
Cada compilación en GitHub Actions publica automáticamente una **versión nueva con nombre único**
(`v1.0.<n>`) y adjunta su archivo `.apk` (el nombre del archivo también cambia en cada versión).

## Descargar

- Última versión (siempre la más reciente):
  https://github.com/MauricioR8/MI_APP_MUSICA/releases/latest
- Historial completo de todas las versiones:
  https://github.com/MauricioR8/MI_APP_MUSICA/releases

## Convención de nombres

| Elemento            | Formato                                   | Ejemplo                                 |
|---------------------|-------------------------------------------|-----------------------------------------|
| Etiqueta (tag)      | `v1.0.<n>`                                | `v1.0.7`                                |
| Nombre de release   | `MI APP MUSICA v1.0.<n>`                  | `MI APP MUSICA v1.0.7`                 |
| Archivo APK (debug) | `MI_APP_MUSICA-v1.0.<n>-debug.apk`        | `MI_APP_MUSICA-v1.0.7-debug.apk`       |
| Archivo APK (release) | `MI_APP_MUSICA-v1.0.<n>-release.apk`    | `MI_APP_MUSICA-v1.0.7-release.apk`     |

`<n>` es el número de ejecución del workflow, por lo que siempre incrementa y nunca se repite.

## Instalación

1. Descarga el `.apk` desde la release deseada.
2. Transfiérelo al teléfono Android (o descárgalo directamente desde el móvil).
3. Ábrelo e instálalo. Si el sistema lo pide, habilita
   *"Instalar apps de origen desconocido"* para tu navegador/gestor de archivos.

> Nota: el APK **debug** siempre se publica. El **release** (optimizado) se publica
> cuando su compilación termina con éxito.
