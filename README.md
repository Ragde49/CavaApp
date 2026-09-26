# CavaApp

Repositorio único para **Android + API ASP.NET Core 8**.

## Arquitectura
Android -> HTTPS -> Cava API (IIS) -> SQL Server Cava

La app **ya no usa un catálogo embebido**. Consume:
- GET /api/vinos
- GET /api/vinos/{id}
- POST /api/maridajes/calcular

La URL de la API se configura dentro de la app en la pestaña **API**.

## Altas de nuevas etiquetas
Por decisión del proyecto, no se insertan desde el teléfono. Se envían fotografías frontal/reverso para investigación y se genera un INSERT/UPDATE SQL revisado. Tras ejecutarlo en SQL Server, la app lo ve al sincronizar.

## Builds
- Android: workflow `Android APK`
- API IIS: workflow `Cava API IIS`

Consulta `DEPLOY-IIS.md` para publicar la API.
