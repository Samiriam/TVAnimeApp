# Plan de Trabajo — TVAnimeApp

## Objetivo rector

La app debe girar en torno a este flujo principal:

1. El usuario ingresa una URL de una pagina publica.
2. La app inspecciona el contenido remoto.
3. Detecta enlaces de video y audio potencialmente reproducibles.
4. Presenta los resultados en interfaz Android TV.
5. Reproduce el recurso seleccionado dentro de la app.

## Brecha actual

La implementacion existente esta centrada sobre todo en listas M3U y catalogo local/remoto sincronizado. Eso deja ya resuelta una base importante de UI TV, almacenamiento y reproduccion, pero todavia faltan piezas para el objetivo real:

1. Entrada manual de URL de pagina.
2. Capa de extraccion/deteccion de medios desde HTML publico.
3. Modelo de datos enfocado en enlaces detectados, no solo items de playlist.
4. Flujo principal de analisis -> resultados -> reproduccion.

## Indice de carpetas real

```
TVAnimeApp/
├── build.gradle.kts                   ← Config raiz (AGP 8 / versiones centralizadas)
├── settings.gradle.kts
├── gradle.properties
├── libs.versions.toml                 ← Catalogo de versiones (AGP, Kotlin, Media3, Hilt, Room…)
├── README.md                          ← Guia de inicio y flujo de fuentes M3U
├── PLAN_TRABAJO.md                    ← Este archivo
└── app/
    ├── build.gradle.kts               ← App: Compose TV + Room + Retrofit + Hilt + Media3 + WorkManager
    ├── proguard-rules.pro
    ├── src/main/
    │   ├── AndroidManifest.xml        ← Leanback / landscape / INTERNET / CleartextTraffic
    │   ├── assets/playlist_demo.m3u   ← Lista M3U demostracion (sample URLs de Google)
    │   ├── java/com/tvanime/app/
    │   │   ├── TVAnimeApp.kt               ← @HiltAndroidApp + WorkManager Configuration.Provider
    │   │   ├── presentation/MainActivity.kt ← Actividad principal Compose
    │   │   ├── player/PlayerConfig.kt      ← Configuracion de tracks Media3
    │   │   ├── worker/ContentSyncWorker.kt  ← Sync M3U via WorkManager
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── ContentItem.kt      ← Modelo de dominio (Parcelable)
    │   │   │   │   └── MediaType.kt         ← ANIME / MOVIE / SERIES / OTHER
    │   │   │   └── usecase/                 ← 9 casos de uso puros
    │   │   ├── data/
    │   │   │   ├── local/
    │   │   │   │   ├── database/TVAnimeDatabase.kt  ← Room v1
    │   │   │   │   ├── dao/                  ← ContentDao, HistoryDao, FavoriteDao
    │   │   │   │   └── entity/               ← ContentEntity, HistoryEntity, FavoriteEntity, Deps
    │   │   │   ├── remote/
    │   │   │   │   ├── api/SourceApi.kt
    │   │   │   │   ├── dto/RemoteContentItem.kt
    │   │   │   │   └── interceptor/UserAgentInterceptor.kt
    │   │   │   ├── parser/M3uPlaylistParser.kt   ← Parser de listas M3U locales o remotas
    │   │   │   └── repository/               ← Interfaces + Implementaciones
    │   │   ├── ui/
    │   │   │   ├── components/
    │   │   │   │   ├── ContentCard.kt        ← Tarjeta TV con foco D-pad
    │   │   │   │   └── CategoryRow.kt        ← Fila horizontal LazyRow
    │   │   │   ├── navigation/TVAnimeNavHost.kt  ← Navegacion Compose NavController
    │   │   │   ├── screens/Screens.kt         ← Home / Detail / Player funcionales
    │   │   │   └── theme/Theme.kt
    │   │   └── di/AppModule.kt               ← Hilt DI completo
    │   └── res/
    │       ├── mipmap-{mdpi…xxxhdpi}/ic_launcher.xml   ← Icono adaptive
    │       ├── drawable/ic_banner.xml
    │       ├── drawable/ic_launcher.xml                 ← Play/pause icon sources
    │       └── values/{strings,colors,styles}.xml
```

## Estado del Proyecto

| # | Item | Estado |
|---|---|---|
| 1 | Configuracion Gradle raiz + AGP 8.2 | ✅ |
| 2 | Catalogo de versiones / libs.versions.toml | ✅ |
| 3 | AndroidManifest (Leanback, landscape, INTERNET) | ✅ |
| 4 | @HiltAndroidApp + WorkManager HiltWorkerFactory | ✅ |
| 5 | Room v1 – TVAnimeDatabase + 3 DAOs | ✅ |
| 6 | Entidades Room (Content/Favorite/History) | ✅ |
| 7 | Mapper dominio ↔ entidad | ✅ |
| 8 | Hilt AppModule (todos los providers) | ✅ |
| 9 | SourceApi + UserAgentInterceptor | ✅ |
| 10 | M3uPlaylistParser (parsea URLs y archivos M3U) | ✅ |
| 11 | Repositorios (interfaces + implementaciones) | ✅ |
| 12 | 9 casos de uso (catálogo, detalle, favoritos, historial…) | ✅ |
| 13 | PlayerConfig (Media3 track selector) | ✅ |
| 14 | ContentSyncWorker (WorkManager 2.9 + HiltWorker) | ✅ |
| 15 | Icono adaptive ic_launcher + banner TV | ✅ |
| 16 | Theme Material3 TV (colores oscuros por defecto) | ✅ |
| 17 | ContentCard + CategoryRow (D-pad nativo) | ✅ |
| 18 | NavHost (Home / Detail / Player) | ✅ |
| 19 | HomeViewModel + HomeScreen funcional | ✅ |
| 20 | DetailViewModel + DetailScreen funcional | ✅ |
| 21 | PlayerScreen con ExoPlayer / Media3 | ✅ |
| 22 | ProGuard rules release | ✅ |
| 23 | Compilar APK desde Android Studio | ✅ |
| 24 | Pruebas en Android TV real o emulador | ⏳ Pendiente |
| 25 | Configurar periodicidad WorkManager en onCreate | ✅ |
| 26 | Screen de ajustes (origen de playlist M3U) | ✅ |

## Errores, Hallazgos y Soluciones

| Fecha | Problema | Evidencia | Solucion | Resultado |
|---|---|---|---|---|
| 2026-05-17 | Directorios duplicados `tavanime` / `tvanime` | Create-Item genero dos paquetes | Eliminada carpeta duplicada | ✅ Resuelto |
| 2026-05-17 | ContentDao sin metodos @Insert funcion | Error compilacion | Reescribi DAOs sin anotaciones redundantes | ✅ Resuelto |
| 2026-05-17 | AppModule con imports inexistentes | Error compilacion | Reescribi AppModule con imports directos | ✅ Resuelto |
| 2026-05-17 | Paquete `tavanime` mezclado con `tvanime` | 28 referencias rotas | Reescribi todos los archivos afectados | ✅ Resuelto |
| 2026-05-17 | PlayerScreen reemplazo isPlaying en apply{} | Error Kotlin | Cambiado a playWhenReady = true | ✅ Resuelto |
| 2026-05-17 | MediaType enum con @Parcelize redundante | Warning KSP | Eliminado, movido a archivo separado | ✅ Resuelto |
| 2026-05-17 | Falta ic_launcher.xml (mipmap) y @drawable/ic_banner | Error AAPT2 | Creados adaptive icons y banner XML | ✅ Resuelto |
| 2026-05-17 | Falta proguard-rules.pro | Error release build | Creado con reglas Room/Hilt/ViewModel | ✅ Resuelto |

## Flujo de fuentes M3U

```
Fuente autorizada (URL M3U o archivo local)
        │
        ▼
 M3uPlaylistParser.parseFromUrl(url)
        │
        ▼
 Lista de PlaylistItem { title, url, group }
        │
        ▼
 ContentSyncWorker  (WorkManager, cada 4h por defecto)
   o llamado manual desde Ajustes
        │
        ▼
 RemoteContentItem  →  ContentsRepository.syncCatalog()
        │
        ▼
 ContentEntity insertadas en Room
        │
        ▼
 HomeViewModel.collect catalog()
        │
        ▼
 HomeScreen ── CategoryRow ── ContentCard
        │
        ▼  (click Enter)
   navController → DetailScreen
        │
        ▼
   boton REPRODUCIR → navController → PlayerScreen
        │
        ▼
   ExoPlayer reproduce videoUrl directo
```

## Comando para compilar el APK

```bash
# En Android Studio:
# 1. Abrir carpeta TVAnimeApp
# 2. Sync Now
# 3. Build → Build Bundle(s) / APK(s) → Build APK(s)
# El APK queda en: app/build/outputs/apk/debug/app-debug.apk

# O desde terminal si tienes gradle en el PATH:
cd "C:\Users\informatica\AndroidStudioProjects\TVAnimeApp"
.\gradlew.bat assembleDebug
```

## Pendiente Inmediato

1. [Compilar APK] Verificar en Android Studio y corregir errores de build si existieran
2. [URL de entrada] Crear flujo para que el usuario ingrese una pagina publica a analizar
3. [Extraccion] Diseñar una capa de deteccion de enlaces multimedia desde HTML publico
4. [Flujo principal] Reordenar Home/Detail para priorizar analizar URL y mostrar resultados
5. [Compatibilidad] Definir que formatos y hosts publicos se soportaran primero

## Plan de Implementacion: Scraping de Fuentes Autorizadas

### Decision recomendada

El entregable principal es un APK nativo para Android TV. Implementar dentro del APK un extractor HTML liviano para paginas publicas autorizadas, usando el cliente HTTP existente y un parser HTML compatible con Android. Conservar el flujo M3U actual como alternativa compatible.

No incorporar un navegador headless, FFmpeg ni logica extensa de scraping dentro del APK. Esas dependencias aumentan el peso de la app, el consumo de memoria, la complejidad de mantenimiento y el riesgo operativo en dispositivos Android TV. Si en una fase futura un dominio autorizado exige procesamiento pesado, evaluar un servicio auxiliar opcional sin convertirlo en requisito para el primer APK funcional.

### Alcance permitido

El extractor debe operar solo sobre paginas publicas y fuentes que el usuario este autorizado a consultar. No debe evadir controles de acceso, DRM, paywalls, CAPTCHAs, protecciones anti-bot ni restricciones contractuales del sitio fuente.

Alcance inicial:

1. Recibir una URL publica ingresada por el usuario.
2. Validar que use `https` y pertenezca a una lista de dominios autorizados.
3. Descargar HTML con timeout, limite de tamaño y redirecciones acotadas.
4. Detectar enlaces multimedia publicos declarados en HTML: `<video>`, `<source>`, `iframe`, enlaces `.m3u8`, `.mp4` y audio soportado.
5. Normalizar, deduplicar y devolver candidatos reproducibles.
6. Permitir que Android TV presente resultados y abra el candidato seleccionado con Media3.
7. Mantener el importador M3U existente sin mezclar su responsabilidad con el extractor HTML.

Fuera de alcance inicial:

1. Descargas de archivos al disco.
2. Resolucion de enlaces protegidos o cifrados.
3. Automatizacion de navegador para saltar protecciones.
4. Extraccion desde sitios no autorizados.
5. Soporte multi-proveedor masivo antes de validar un primer dominio.

### Arquitectura propuesta

```text
Android TV App
    |
    +-- UrlPolicyValidator
    |      valida https, allowlist y bloquea destinos privados
    |
    +-- HttpPageFetcher
    |      timeout, limite de bytes, redirects limitados
    |
    +-- ExtractorRegistry
    |      selecciona extractor por dominio autorizado
    |
    +-- GenericHtmlMediaExtractor
    |      video/source/iframe y extensiones publicas conocidas
    |
    +-- CandidateNormalizer
           URL absoluta, tipo, calidad, origen y deduplicacion
```

### Contrato interno inicial

Entrada y resultado esperado del caso de uso Android:

```json
{
  "pageUrl": "https://sitio-autorizado.example/pagina",
  "sourceHost": "sitio-autorizado.example",
  "title": "Titulo detectado",
  "candidates": [
    {
      "url": "https://cdn-autorizado.example/video/stream.m3u8",
      "mediaType": "video",
      "format": "hls",
      "quality": null,
      "referer": "https://sitio-autorizado.example/pagina",
      "sourceName": "generic-html"
    }
  ]
}
```

Reglas del contrato:

1. `url` debe ser absoluta.
2. `mediaType` debe aceptar inicialmente `video` o `audio`.
3. `format` debe aceptar inicialmente `hls`, `mp4`, `audio` o `embed`.
4. `referer` debe tratarse como dato sensible de integracion y no persistirse en logs completos.
5. La capa Android debe devolver errores tipados: URL invalida, dominio no permitido, timeout, respuesta demasiado grande y sin candidatos.

### Cambios previstos en TVAnimeApp

Crear:

1. `data/extraction/UrlPolicyValidator.kt`: validacion de URL y allowlist.
2. `data/extraction/HttpPageFetcher.kt`: descarga HTML segura con OkHttp.
3. `data/extraction/HtmlMediaExtractor.kt`: deteccion de `video`, `source`, `iframe`, HLS, MP4 y audio.
4. `data/extraction/CandidateNormalizer.kt`: URL absoluta, tipo y deduplicacion.
5. `data/repository/ExtractionRepository.kt`: contrato de dominio.
6. `data/repository/ExtractionRepositoryImpl.kt`: orquestacion local dentro del APK.
7. `domain/model/DetectedMedia.kt`: candidato reproducible separado de `ContentItem`.
8. `domain/usecase/ExtractMediaFromPageUseCase.kt`: caso de uso principal.
9. `ui/viewmodel/ExtractMediaViewModel.kt`: loading, resultados y errores.
10. `ui/screens/ExtractMediaScreen.kt`: formulario URL y lista de candidatos TV-first.

Modificar:

1. `app/build.gradle.kts`: agregar parser HTML liviano compatible con Android, por ejemplo Jsoup.
2. `di/AppModule.kt`: proveer extractor y `ExtractionRepository`.
3. `ui/navigation/TVAnimeNavHost.kt`: agregar ruta `extract` y navegacion al player.
4. `ui/screens/Screens.kt`: priorizar acceso a analizar URL desde Home.
5. `domain/model/ContentItem.kt`: no sobrecargar este modelo con el estado temporal del scraping.

### Servicio extractor auxiliar opcional

No es necesario para el primer APK. Considerarlo solo si un dominio autorizado necesita procesamiento que no convenga ejecutar en Android TV. En ese caso, crear un repositorio o modulo independiente, por ejemplo `TVAnimeExtractorService`, para evitar acoplar Node.js o un navegador headless al proyecto Android.

Estructura recomendada:

```text
TVAnimeExtractorService/
|-- src/
|   |-- server.*
|   |-- routes/extractions.*
|   |-- services/extraction.service.*
|   |-- extractors/generic-html.extractor.*
|   |-- extractors/provider-registry.*
|   |-- security/url-policy.*
|   `-- utils/api-error.*
|-- tests/
|   |-- fixtures/
|   |-- generic-html.extractor.test.*
|   `-- url-policy.test.*
`-- README.md
```

### Controles obligatorios del extractor Android

1. Lista explicita de dominios autorizados para paginas y CDN.
2. Bloqueo SSRF: rechazar `localhost`, IPs privadas, metadata cloud y redirecciones hacia destinos bloqueados.
3. Timeout, limite de tamaño de HTML y maximo de redirecciones.
4. Rate limit por cliente.
5. Logs sin tokens, cookies ni URLs privadas completas.
6. User-Agent identificable del producto, sin intentar simular navegadores para evadir restricciones.
7. Cache corta por URL para evitar solicitudes repetidas innecesarias.
8. Pruebas con fixtures HTML locales antes de consultar sitios externos.

## Auditoria de Referencia: FxxMorgan/anime1v-api

Referencia revisada el `2026-05-30`:

- Decision confirmada por el usuario el `2026-05-30`: tomar como referencia tecnica el repositorio `https://github.com/FxxMorgan/anime1v-api.git` para la etapa de scraping, adaptando solo lo util al contexto Android TV y sin copiar componentes fuera de alcance del APK.
- Referencia secundaria confirmada por el usuario el `2026-05-30`: revisar `https://github.com/pedroparkeralrescate-code/balandro-stremio.git` para extraer patrones reutilizables de scraping/adaptacion, evitando dependencias o flujos especificos de Stremio que no correspondan al APK Android TV.

- Repositorio: `https://github.com/FxxMorgan/anime1v-api.git`
- Licencia formal: MIT. Si se copia codigo sustancial, conservar aviso de copyright y licencia.
- Stack observado: Node.js, Express, Axios, Cheerio, Puppeteer y FFmpeg.
- Enfoque observado: API HTTP, registro multi-proveedor, extractores por proveedor, normalizacion de resultados, resolvers de hosts, autenticacion basica y rate limit.

### Ideas que conviene adaptar

1. Separar el scraper de la app cliente mediante una API HTTP.
2. Usar un registro de extractores por dominio en lugar de condicionales dispersos.
3. Mantener un extractor generico como primer intento y extractores especializados solo cuando un dominio autorizado lo necesite.
4. Normalizar resultados a un contrato comun antes de enviarlos al cliente.
5. Deduplicar URLs encontradas.
6. Aplicar timeouts y errores tipados.
7. Mantener rate limit y health check.
8. Separar busqueda, informacion de pagina y candidatos de reproduccion si el producto crece.

### Partes que no se deben copiar al alcance inicial

1. Puppeteer y navegacion headless para atravesar protecciones anti-bot.
2. Resolvers especificos para evadir protecciones de hosts externos.
3. Descarga con FFmpeg y almacenamiento de archivos.
4. Logica especifica de proveedores no autorizados.
5. API keys por query string; usar headers para evitar filtraciones en logs.
6. Evaluacion de scripts remotos con `vm.runInNewContext`; preferir parsing estructurado y extractores acotados.
7. Estado en memoria para rate limit si el servicio se despliega con multiples instancias; usar almacenamiento compartido cuando corresponda.

### Opciones avanzadas para evaluar despues del primer APK

Estas capacidades no forman parte del primer hito, pero deben conservarse como lineas de trabajo posibles. Antes de implementarlas, documentar el caso real, la autorizacion de la fuente, el impacto sobre Android TV y la alternativa mas simple descartada.

| Opcion | Uso posible | Condicion para habilitarla | Ubicacion recomendada |
|---|---|---|---|
| Navegador automatizado con Puppeteer | Procesar paginas autorizadas que requieren JavaScript y no exponen HTML suficiente | Confirmar que scraping HTML local y WebView Android no resuelven el caso | Servicio auxiliar separado del APK |
| WebView Android controlado | Ejecutar JavaScript dentro del APK para una pagina autorizada concreta | Limitar dominios, bloquear navegacion externa y medir memoria en Android TV | APK, como adaptador especializado opcional |
| Manejo de desafios anti-bot | Resolver integraciones autorizadas afectadas por controles automatizados | Obtener permiso explicito del proveedor y documentar limites tecnicos y legales | Servicio auxiliar; no usar como bypass general |
| Resolvers especializados por host | Convertir embeds autorizados en candidatos reproducibles | Implementar solo para hosts permitidos y con fixtures de regresion | Modulos aislados por host |
| FFmpeg | Transcodificar, unir segmentos HLS o preparar descargas autorizadas | Media3 no cubre el caso y existe una necesidad funcional aprobada | Preferir servicio auxiliar; evaluar binario Android solo con pruebas de peso y compatibilidad |
| Descarga local en Android TV | Guardar contenido autorizado para reproduccion offline | Definir espacio maximo, permisos, limpieza, UX y compatibilidad del dispositivo | APK, usando almacenamiento administrado |
| Descarga en backend | Preparar archivos autorizados antes de entregarlos al cliente | Requerir procesamiento pesado o colas que no conviene ejecutar en TV | Servicio auxiliar separado |
| Extractores especificos por fuente | Soportar estructuras HTML particulares | Fuente autorizada, selector estable y pruebas con fixtures versionados | APK si es liviano; servicio auxiliar si requiere navegador o procesamiento pesado |

Reglas para estas opciones:

1. No implementar evasiones genericas ni copiar resolvers masivamente.
2. Mantener cada extractor o resolver aislado por dominio o host.
3. Registrar autorizacion, fixtures, pruebas y criterio de retiro.
4. Preferir Media3 para reproduccion directa antes de introducir FFmpeg.
5. Mantener el APK funcional aunque una integracion especializada falle.

### Archivos de referencia utiles para estudio

1. `src/services/anime.service.js`: registro y seleccion de proveedor.
2. `src/services/animeav1.service.js`: extraccion HTML, normalizacion y deduplicacion.
3. `src/routes/anime.routes.js`: separacion de endpoints.
4. `src/middlewares/auth.js`: autenticacion basica, con la correccion de no aceptar API key por query.
5. `src/middlewares/rate-limit.js`: limite diario sencillo, util solo como punto de partida local.
6. `src/services/download.service.js`: estudiar unicamente la separacion de responsabilidades; no copiar resolvers, automatizacion ni descarga al primer hito.

## Fases Para El Programador

### Fase 0: confirmar base Android

1. Ejecutar `.\gradlew.bat assembleDebug`.
2. Corregir errores existentes antes de agregar scraping.
3. Probar el APK actual en emulador Android TV.
4. Registrar resultado y ruta del APK en este archivo.

Criterio de salida: build Android reproducible y Home/Detail/Player verificados.

### Fase 1: extractor generico con fixtures

1. Implementar el extractor dentro del proyecto Android.
2. Agregar un parser HTML liviano compatible con Android.
3. Implementar `UrlPolicyValidator`.
4. Implementar fetch HTTP seguro con OkHttp.
5. Implementar parsing HTML para `<video>`, `<source>`, `iframe`, `.m3u8`, `.mp4` y audio.
6. Crear fixtures HTML locales con casos positivos, duplicados, URL relativa, URL invalida y sin candidatos.
7. Agregar tests unitarios.

Criterio de salida: el codigo Android devuelve candidatos normalizados desde fixtures sin acceder a red privada.

### Fase 2: integracion Android

1. Crear repositorio local y caso de uso.
2. Crear pantalla TV-first para ingresar URL.
3. Mostrar estado de analisis, resultados y errores legibles.
4. Navegar al player al elegir un candidato compatible.
5. Mantener el flujo M3U actual disponible.

Criterio de salida: desde el APK Android TV se analiza una pagina autorizada de prueba y se reproduce un HLS o MP4 publico de prueba.

### Fase 3: primer dominio autorizado especializado

1. Elegir un solo dominio autorizado.
2. Documentar su estructura HTML y condiciones de uso.
3. Crear extractor especializado solo si el generico no alcanza.
4. Agregar fixtures versionados y tests de regresion.
5. Definir monitoreo para detectar cambios de HTML.

Criterio de salida: extractor especializado aislado y cubierto por tests.

### Fase 4: endurecimiento

1. Agregar cache local corta si las pruebas muestran solicitudes repetidas.
2. Agregar metricas locales de diagnostico: latencia, errores por dominio y cantidad de candidatos.
3. Revisar privacidad de logs.
4. Crear pruebas de integracion y smoke test en emulador Android TV.
5. Evaluar servicio auxiliar solo si existe una necesidad demostrada.

Criterio de salida: APK verificable, limitado y sin secretos en el repo.

## Pruebas Minimas

1. Unitarias de `UrlPolicyValidator`: https valido, http rechazado, dominio fuera de allowlist, IP privada y redirect bloqueado.
2. Unitarias de extractor: video, source, iframe, HLS, MP4, audio, duplicados y URL relativa.
3. Integracion Android: respuesta valida, timeout y HTML demasiado grande.
4. Android: URL vacia, loading, resultado vacio, error de red y candidato reproducible.
5. Regresion: importacion M3U existente sigue funcionando.

## Cambios Realizados

| Fecha | Archivos / modulo | Cambio | Verificacion | Estado |
|---|---|---|---|---|
| 2026-05-30 | `TVAnimeApp.kt`, `worker/ContentSyncWorker.kt`, `data/settings/*` | Se agrego configuracion persistente del origen M3U y programacion de sync inmediata + periodica con WorkManager al iniciar la app | `assembleDebug` exitoso | ✅ |
| 2026-05-30 | `ui/navigation/TVAnimeNavHost.kt`, `ui/screens/Screens.kt`, `ui/viewmodel/SettingsViewModel.kt` | Se agrego pantalla de ajustes para elegir demo local o URL remota M3U y lanzar sincronizacion | `assembleDebug` exitoso | ✅ |
| 2026-05-30 | `AppModule.kt`, `libs.versions.toml`, `app/build.gradle.kts`, `local.properties`, `gradle.properties` | Se corrigio la configuracion de Hilt/WorkManager, binding del parser y el entorno local de build para compilar el APK de prueba | `assembleDebug` exitoso | ✅ |
| 2026-05-30 | `RemoteContentItem.kt`, `UserAgentInterceptor.kt`, `SyncCatalogUseCase.kt`, `PlayerConfig.kt`, `ContentCard.kt` | Se corrigieron errores previos de compilacion Kotlin/KSP que bloqueaban el build del proyecto | `assembleDebug` exitoso | ✅ |
| 2026-05-30 | `data/extraction/*`, `data/repository/ExtractionRepository*`, `domain/model/DetectedMedia.kt`, `domain/usecase/ExtractMediaFromPageUseCase.kt`, `ui/viewmodel/ExtractMediaViewModel.kt`, `ui/screens/Screens.kt`, `ui/navigation/TVAnimeNavHost.kt` | Se integro el primer extractor HTML generico con validacion https, bloqueo basico de hosts locales/privados, deteccion de HLS/MP4/audio/embed y pantalla TV-first para analizar URL | `testDebugUnitTest` y `assembleDebug` exitosos | ✅ |

## Pruebas Y Builds

| Fecha | Comando / prueba | Resultado | Artefacto | Pendiente |
|---|---|---|---|---|
| 2026-05-30 | `JAVA_HOME=C:\Users\informatica\AppData\Local\Temp\kilo\jdk17\jdk-17.0.19+10` + `./gradlew.bat assembleDebug` | Build exitoso | `app/build/outputs/apk/debug/app-debug.apk` (~13.5 MB, generado el 2026-05-30) | Probar instalacion y flujo en Android TV real o emulador |
| 2026-05-30 | `JAVA_HOME=C:\Users\informatica\AppData\Local\Temp\kilo\jdk17\jdk-17.0.19+10` + `./gradlew.bat testDebugUnitTest` | Tests unitarios exitosos para extractor HTML y validador URL | No aplica | Agregar mas fixtures por dominio autorizado |
| 2026-05-30 | `JAVA_HOME=C:\Users\informatica\AppData\Local\Temp\kilo\jdk17\jdk-17.0.19+10` + `./gradlew.bat assembleDebug` | Build exitoso con scraping generico integrado | `app/build/outputs/apk/debug/app-debug.apk` (~13.7 MB, generado el 2026-05-30) | Probar pantalla Analizar URL en Android TV real o emulador |

## Estado Actual

### Funciona

- Compilacion `debug` reproducible con JDK 17 local temporal.
- APK `debug` generado en `app/build/outputs/apk/debug/app-debug.apk`.
- Configuracion del origen M3U desde UI con opcion demo local o URL remota.
- Sincronizacion inmediata y periodica con WorkManager al guardar ajustes y al iniciar la app.
- Pantalla `Analizar URL` disponible desde Home.
- Extractor HTML generico detecta candidatos `.m3u8`, `.mp4`, audio y `iframe` declarados en HTML publico.

### No Funciona

- No se ejecuto aun prueba manual en Android TV real o emulador durante esta sesion.
- El extractor especializado por dominio todavia no existe; la implementacion actual es generica.

### Falta Realizar

- Instalar el APK en emulador/dispositivo Android TV y verificar Home, Ajustes, sync demo y reproduccion.
- Probar `Analizar URL` contra una pagina publica autorizada con HLS/MP4 directo.
- Definir el primer dominio autorizado para crear extractor especializado con fixtures versionados.

## Decisiones Pendientes Antes De Implementar

1. Definir el primer dominio autorizado de prueba.
2. Confirmar si el primer hito debe aceptar solo HLS/MP4 o tambien audio.
3. Definir si los candidatos detectados se reproducen directamente o se guardan temporalmente en Room.
4. Definir politica de cache y expiracion.
5. Decidir si algun dominio autorizado requiere un servicio auxiliar en una fase posterior.
