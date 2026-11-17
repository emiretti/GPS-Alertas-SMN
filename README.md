# GPS Alertas SMN - Android

Aplicación Android nativa en Kotlin que consulta de forma periódica el feed oficial GeoRSS del Servicio Meteorológico Nacional Argentino (SMN) para detectar avisos a corto plazo en tiempo real, basándose en la ubicación GPS actual del usuario o en sus departamentos seleccionados.

## Características
- **Monitoreo Persistente:** Consulta en segundo plano usando `WorkManager` y `CoroutineWorker`.
- **Geocercas Nativas:** Cálculo de posición matemática (*Point-in-Polygon*) para determinar de forma exacta si las coordenadas del usuario se interceptan con el área poligonal afectada.
- **Alertas Tempranas:** Emisión de alertas sonoras personalizadas y notificaciones de alta prioridad al detectar amenazas meteorológicas inminentes.

## Requisitos de instalación
1. Clonar el repositorio.
2. Configurar tu clave de API de Google Maps en el archivo `local.properties` del proyecto.

## Atribución de Datos y Agradecimientos

- **Servicio Meteorológico Nacional (SMN):** Un agradecimiento especial al **Servicio Meteorológico Nacional de la República Argentina** por proveer de forma libre, pública y abierta sus canales oficiales de alimentación de datos GeoRSS de Avisos a Corto Plazo, permitiendo el desarrollo de herramientas de prevención ciudadana.

## Créditos y Dependencias Externas

- **Cálculo de Polígonos:** Para el algoritmo de *Point-in-Polygon* (Ray-casting) utilizado en la validación geográfica de áreas afectadas, se integró el código matemático de la librería de código abierto [polygon-contains-point](https://github.com/sromku/polygon-contains-point) desarrollada por [Roman Kushnarenko (sromku)](https://github.com/sromku).
- **Mapas:** Google Play Services Maps API para la renderización de las capas vectoriales y polígonos.
- **Conexión de Red y Deserialización:** Retrofit 2 combinado con SimpleXML Converter para el parseo del feed estructurado.

