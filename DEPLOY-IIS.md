# Publicación en IIS

1. Instala el **.NET 8 Hosting Bundle** en el servidor y reinicia IIS.
2. En GitHub Actions descarga el artefacto **CavaApi-iis** y descomprímelo, por ejemplo en:
   `C:\inetpub\CavaApi`
3. Crea un App Pool **CavaApi** con **No Managed Code**.
4. Publica el sitio o aplicación con HTTPS.
5. Si SQL Server está local y usarás autenticación integrada:

```sql
CREATE LOGIN [IIS APPPOOL\CavaApi] FROM WINDOWS;
GO
USE Cava;
GO
CREATE USER [IIS APPPOOL\CavaApi] FOR LOGIN [IIS APPPOOL\CavaApi];
GO
ALTER ROLE db_datareader ADD MEMBER [IIS APPPOOL\CavaApi];
GO
```

6. Prueba:
   - `https://TU-DOMINIO/api/health`
   - `https://TU-DOMINIO/api/vinos`

7. En Android abre **API**, captura `https://TU-DOMINIO` y pulsa **Guardar y sincronizar**.

La API no tiene endpoints de escritura; las nuevas botellas se cargan mediante SQL revisado.
