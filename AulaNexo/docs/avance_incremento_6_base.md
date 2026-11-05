# Incremento 6: primera base de validación de paquetes

5 de noviembre de 2026. Implementación parcial; no habilita aún importación ni exportación.

Se añadió `data/packageio/SafePackageReader.kt`, un lector ZIP que prepara los archivos en memoria bajo límites de entradas, tamaño individual y tamaño total descomprimido. No extrae archivos al dispositivo ni modifica la base de datos. Rechaza rutas ascendentes/absolutas, nombres ambiguos, duplicados sin distinción de mayúsculas y conflictos entre archivos y directorios. Los directorios no pueden contener datos.

Se añadieron pruebas de lectura válida, rutas maliciosas, colisiones, límites reales de descompresión y manifiesto ausente en `SafePackageReaderTest.kt`.

Esta base solo comprueba la presencia de `manifest.json`; todavía no valida su JSON, versiones, hashes, contenido pedagógico ni recursos. Tampoco limita el tamaño comprimido de entrada ni implementa publicación recuperable de recursos. No debe exponerse como importador completo.

Siguiente trabajo: contrato `.aula`, validación semántica e integridad, staging en almacenamiento privado y activación transaccional; después exportación de contenido/resultados y selector nativo de archivos. El APK funcional anterior no cambia su interfaz en esta entrega.
