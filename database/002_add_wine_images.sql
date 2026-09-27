USE Cava;
GO

IF COL_LENGTH('dbo.Vinos','ImagenUrl') IS NULL
BEGIN
    ALTER TABLE dbo.Vinos ADD ImagenUrl NVARCHAR(500) NULL;
END
GO

UPDATE dbo.Vinos
SET ImagenUrl = CONCAT('/images/vinos/', VinoID, '.jpg')
WHERE VinoID BETWEEN 1 AND 21
  AND Activo = 1;
GO

-- El registro 22 no tiene botella/foto confirmada.
UPDATE dbo.Vinos
SET ImagenUrl = NULL
WHERE VinoID = 22;
GO

SELECT VinoID, Nombre, ImagenUrl
FROM dbo.Vinos
ORDER BY VinoID;
GO
