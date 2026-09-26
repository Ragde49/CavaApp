# CavaApp

MVP Android para registrar una cava, visualizar perfiles sensoriales y proponer maridajes por estructura.

## Stack
- Kotlin + Jetpack Compose
- Material 3 con lenguaje visual expresivo
- Catálogo inicial de 22 vinos
- Radar sensorial dibujado nativamente
- Motor de maridaje local
- Alta manual de nuevas etiquetas con almacenamiento local
- GitHub Actions genera APK debug en cada push a `main`

## Regla de calidad de datos
- `CONFIRMADO`: dato explícito de etiqueta/productor/ficha.
- `DERIVADO`: descriptor del vino concreto convertido a escala 1–5.
- `ESTIMADO`: inferencia por variedad, región, crianza o radar no cuantificado.
- `NULL`: no hay evidencia suficiente; nunca se convierte a cero en lógica de negocio.

## Próxima fase: nuevas etiquetas
La app no debe conectarse directamente a SQL Server. Se propone un API HTTPS:

1. App toma foto frontal y trasera.
2. `POST /api/labels` guarda solicitud e imágenes.
3. Backend intenta identificar el vino en catálogo.
4. Si existe, devuelve el perfil existente.
5. Si no existe, crea registro `PENDIENTE_REVISION`.
6. Un proceso de investigación/IA completa datos con fuente y confianza.
7. Un administrador valida y publica como `CONFIRMADO`, `DERIVADO` o `ESTIMADO`.
8. App sincroniza catálogo publicado.

Esto permite que cada etiqueta investigada se procese una sola vez y después quede reutilizable por todos los usuarios.


Build automático activo en GitHub Actions.
