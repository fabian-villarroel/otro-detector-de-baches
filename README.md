# 📍 Detector de Baches - Iquique & Alto Hospicio

Aplicación móvil nativa en Android diseñada para el reporte, almacenamiento y visualización geolocalizada de infraestructura vial dañada en las comunas de Iquique y Alto Hospicio.

---

##  Tecnologías Utilizadas
 
* **Frontend**: Android (Java), Google Maps SDK, Volley.
* **Backend**: PHP 8.x ejecutado sobre servidor Apache (XAMPP).
* **Base de Datos**: MySQL / MariaDB (`baches_db`).
* **Control de Versiones**: Git.

---

##  Auditoría de Seguridad & Buenas Prácticas

La aplicación cumple con las defensas técnicas requeridas en la evaluación:

1. **Prevención de Inyección SQL**: Comunicación cliente-servidor estructurada mediante la librería Volley y sentencias SQL parametrizadas en PHP.
2. **Principio de Menor Privilegio**: Declaración estricta de permisos en `AndroidManifest.xml` (ubicación y cámara exclusivamente para fines funcionales), manteniendo Activities no públicas con `android:exported="false"`.
3. **Gestión de Multimedia**: Procesamiento y transmisión de capturas en formato Base64 hacia el backend local, evitando la exposición de URIs de archivos físicos (`file://`).

---

##  Configuración del Backend Local (XAMPP)

1. **Base de Datos**:
    * Crear la base de datos `baches_db` en phpMyAdmin.
    * Tablas requeridas:
        * `usuarios`: `id` (INT, AI, PK), `correo` (VARCHAR 100), `clave` (VARCHAR 255).
        * `baches`: `id` (INT, AI, PK), `latitud` (DOUBLE), `longitud` (DOUBLE), `descripcion` (VARCHAR 255), `foto_base64` (LONGTEXT).

2. **Endpoints PHP**:
    * Copiar los archivos PHP a la ruta: `C:\xampp\htdocs\baches_api\`
    * Archivos incluidos: `conexion.php`, `login.php`, `registro.php`, `guardar_bache.php`, `obtener_baches.php`.

3. **Conexión Android**:
    * Actualizar la constante de URL en los Java (`RegisterActivity.java`, `LoginActivity.java`, `MapActivity.java`) reemplazando por tu IP IPv4:
      ```java
      private static final String URL_BASE = "http://<TU_IP_LOCAL>/baches_api/";
      ```

---

##  Funcionalidades Principales

* Registro e inicio de sesión de usuarios.
* Geolocalización mediante mapas interactivos de Google Maps.
* Captura y reporte de baches con foto y descripción en tiempo real.
* Renderizado dinámico de marcadores en el mapa.