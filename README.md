# isi-2026ii-SaberPro

Prototipo del primer requerimiento del sistema de gestión de pruebas Saber Pro: registrar estudiantes próximos a finalizar Ingeniería de Sistemas.

## Requisitos

- JDK 17 o superior.
- Navegador web moderno.

## Ejecutar

Desde la carpeta raíz del proyecto, compila y ejecuta:

```powershell
javac -d out src\Main.java
java -cp out Main
```

Abre [http://localhost:8080](http://localhost:8080). Para usar otro puerto, indícalo al iniciar la aplicación: `java -cp out Main 8090`.

## Archivo de estudiantes

La carga acepta archivos `.txt` en UTF-8, separados por tabulaciones. La fila de encabezados debe incluir, en este orden, `codigo`, `nombre`, `correo`, `telefono`, `cohorte`, `pensum` y `creditos_aprobados`. Hay un archivo funcional en `web/ejemplo.txt`.

El sistema valida campos vacíos, formato de correo, créditos, columnas y códigos duplicados. Si una fila no es válida, no guarda ningún estudiante del archivo. Los registros aprobados quedan con estado **Pendiente de evaluación** y se almacenan localmente en `data/estudiantes.tsv`.