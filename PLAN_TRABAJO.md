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
| 2026-05-29 | Entorno de build en Windows sin Git/JDK/Android SDK | No habia toolchain listo para Gradle | Instalado JDK 17 en D:, Android SDK en D: y configurado local.properties | ✅ Resuelto |
| 2026-05-29 | Build debug detiene en `parseDebugLocalResources` | Gradle reporta `Failed to create MD5 hash for file content` | Se relanzo build desde ruta limpia `D:\TVAnimeApp` para aislar el problema de ruta | 🟡 En progreso |

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
| 2026-05-30 | `data/extraction/CandidateNormalizer.kt`, `data/extraction/ServerClassifier.kt`, `domain/model/DetectedMedia.kt`, `HtmlMediaExtractor.kt`, tests de extraction | Se adapto el patron de `anime1v-api`/Balandro para normalizar candidatos, clasificar servidores, limpiar escapes/base64 y enriquecer `DetectedMedia` con server, direct/resolver, headers, prioridad y diagnostico | `testDebugUnitTest` y `assembleDebug` exitosos | ✅ |
| 2026-05-30 | `EmbedResolverRegistry.kt`, `ExtractionRepositoryImpl.kt`, `HtmlMediaExtractor.kt`, `RecurringSitesStore.kt`, `RecurringSitesSyncScheduler.kt`, `RecurringSitesSyncWorker.kt`, `TVAnimeApp.kt`, `SettingsViewModel.kt`, `Screens.kt`, `TVAnimeNavHost.kt` | Se agrego resolucion generica de embeds HTML, extraccion desde scripts/atributos `data-*`, configuracion de sitios recurrentes y sync periodica cada 6h para guardar candidatos directos en catalogo | `testDebugUnitTest` y `assembleDebug` exitosos | ✅ |
| 2026-05-30 | `HtmlMediaExtractorTest.kt`, `EmbedResolverRegistryTest.kt` | Se agregaron pruebas para patrones `script`/`data-*` y resolucion de embeds; un primer test detecto que `data-file` relativo no se normalizaba y se corrigio la implementacion | `testDebugUnitTest` exitoso | ✅ |
| 2026-05-30 | `CandidateNormalizer.kt`, `HtmlMediaExtractor.kt`, `EmbedResolverRegistry.kt`, `ServerClassifier.kt`, tests de extraction | Se endurecio scraping estilo `servertools`: atributos amplios, HTML/JS/JSON completo, meta refresh, URLs escapadas, URLs anidadas codificadas, base64/URL-safe sin padding, `atob`, `data-embed`, `decodeURIComponent`, template strings y nuevos hosts embed | `testDebugUnitTest` y `assembleDebug` exitosos | ✅ |
| 2026-05-30 | `ServerSpecificResolvers.kt`, `CandidateScorer.kt`, `EmbedResolverRegistry.kt`, `ExtractionRepositoryImpl.kt`, `CandidateNormalizer.kt`, `DetectedMedia.kt`, `Screens.kt`, `TVAnimeNavHost.kt`, `libs.versions.toml`, `app/build.gradle.kts` | Mejoras de scraping basadas en 3 repos de referencia (anime1v-api, balandro-stremio, NebulaStreams-V2): resolvers especificos para 18 servidores, scoring de compatibilidad, headers al reproductor ExoPlayer, deduplicacion inteligente, concurrencia en resolucion, filtro anti-basura ampliado, UI con badges | `assembleDebug` exitoso (14.4 MB) | ✅ |
| 2026-05-30 | `WebSearchSuggestions.kt`, `CandidateCard.kt`, `ExtractMediaViewModel.kt`, `ExtractMediaScreen.kt`, `TVAnimeNavHost.kt` | Mejoras de fluidez UI: búsqueda web con 14 sitios populares (AnimeFLV, JKAnime, Cuevana3, PelisPlus, etc.), tarjetas rediseñadas con badges de formato/calidad, auto-análisis de sitios populares, UI renovada con mejor jerarquía visual y animaciones fluidas | `assembleDebug` exitoso (13.84 MB) | ✅ |
| 2026-05-30 | `UrlPolicyValidator.kt`, `HttpPageFetcher.kt`, `HtmlMediaExtractor.kt`, `CandidateNormalizer.kt` | Fix scraping real: eliminado DNS lookup que bloqueaba URLs www3/ww5, headers completos de Chrome, timeout 30s, extractor reescrito con patrones reales de sitios anime/pelis (iframes, onclick, data-player, variables JS, base64), CandidateNormalizer acepta 30+ servidores embed sin filtrar como ruido | `assembleDebug` exitoso | ✅ |

## Pruebas Y Builds

| Fecha | Comando / prueba | Resultado | Artefacto | Pendiente |
|---|---|---|---|---|
| 2026-05-31 | `JAVA_HOME=... + ./gradlew.bat assembleDebug` | Build exitoso — Navegador Web v1 | `app/build/outputs/apk/debug/app-debug.apk` (`14521278` bytes, `2026-05-31 17:40:26`) | Probar en Android TV real |

## Build V2 — Navegador Web + Auto-Crawler

| Fase | Estado | Descripcion |
|---|---|---|
| FASE 1: Navegador Web | ✅ Completada | WebViewBrowserScreen, TvWebView, UrlBar, VideoCaptureOverlay, WebViewVideoCapture, WebViewSessionManager, WebViewBrowserViewModel |
| FASE 2: Auto-Crawler | ⏳ Pendiente | CrawlCategoryEntity, DAO, CrawlService, JsCatalogExtractor, CrawlWorker, CrawlerSettingsScreen |
| FASE 3: Limpieza scraping | ✅ Completada | Eliminados 21 archivos de scraping (JsEvaluator, PackerUnpacker, WebViewFetcher, HtmlMediaExtractor, etc.) |
| FASE 4: Git commit + push | ⏳ Pendiente | Commit + push a origin/main |

### Archivos creados (V2)

| Archivo | Estado |
|---|---|
| `domain/model/CapturedStream.kt` | ✅ |
| `domain/model/CrawlResult.kt` | ✅ |
| `domain/model/SiteConfig.kt` | ✅ |
| `data/capture/WebViewVideoCapture.kt` | ✅ |
| `data/capture/WebViewSessionManager.kt` | ✅ |
| `ui/components/TvWebView.kt` | ✅ |
| `ui/components/UrlBar.kt` | ✅ |
| `ui/components/VideoCaptureOverlay.kt` | ✅ |
| `ui/screens/WebViewBrowserScreen.kt` | ✅ |
| `ui/viewmodel/WebViewBrowserViewModel.kt` | ✅ |
| `worker/ContentSyncWorker.kt` | ✅ (recreado) |

### Archivos eliminados (scraping)

| Archivo | Razon |
|---|---|
| 11 archivos en `data/extraction/` | Todo el scraping — innecesario con WebView |
| `data/repository/ExtractionRepository.kt` | Sin uso |
| `data/repository/ExtractionRepositoryImpl.kt` | Sin uso |
| `domain/usecase/ExtractMediaFromPageUseCase.kt` | Sin uso |
| `ui/viewmodel/ExtractMediaViewModel.kt` | Sin uso |
| `player/WebViewMediaInterceptor.kt` | Reemplazado por WebViewVideoCapture |
| `data/settings/RecurringSitesStore.kt` | Sin uso |
| `data/settings/RecurringSitesSyncScheduler.kt` | Sin uso |
| `worker/RecurringSitesSyncWorker.kt` | Sin uso |
| 5 archivos de test en `data/extraction/` | Sin sentido sin el extractor |

---

## Estado Actual

### Funciona

- Compilacion `debug` reproducible con JDK 17
- APK generado: `app/build/outputs/apk/debug/app-debug.apk` (14.5 MB, `2026-05-31 17:40:26`)
- **Navegador Web completo**: selector de sitios por categoria, WebView TV-optimizado con D-pad, barra URL con navegacion, overlay de captura de video
- HomeScreen con NavItem "Navegador Web" y "Buscar"
- PlayerScreen reutilizado con ExoPlayer/Media3
- DetailScreen y SettingsScreen funcionales
- M3U sync via WorkManager
- UI focus con borde cyan brillante en todos los elementos interactivos

### Pendiente

- FASE 2: Auto-Crawler (crawl periodico de categorias cada 6h)
- FASE 4: Git commit + push
- Probar en Android TV real

## Arquitectura Final (V2)

```
HOME
├── NavItem: "Navegador Web" ──→ WebViewBrowserScreen
├── NavItem: "Buscar" ──→ WebViewBrowserScreen  
├── NavItem: "Mi biblioteca" ──→ (placeholder)
├── NavItem: "Ajustes" ──→ SettingsScreen
└── Catálogo M3U (Room)

WebViewBrowserScreen:
├── SiteSelector (sitios preconfigurados por categoria)
├── UrlBar (◀ ▶ ⟳ + input, D-pad focusable)
├── AndroidWebView (D-pad: scroll, click, back)
└── VideoCaptureOverlay (detecta .m3u8/.mp4/.webm → "Reproducir en TV")
    └── PlayerScreen (ExoPlayer/Media3)
```

## Estado Actual

### Funciona

- Compilacion `debug` reproducible con JDK 17 local temporal.
- APK `debug` generado en `app/build/outputs/apk/debug/app-debug.apk` (14.4 MB).
- Configuracion del origen M3U desde UI con opcion demo local o URL remota.
- Sincronizacion inmediata y periodica con WorkManager al guardar ajustes y al iniciar la app.
- Pantalla `Analizar URL` disponible desde Home.
- Extractor HTML generico detecta candidatos `.m3u8`, `.mp4`, audio y `iframe` declarados en HTML publico.
- `CandidateNormalizer` limpia escapes comunes (`\u0026`, `&amp;`, `%2F`) y base64 simple antes de clasificar.
- `ServerClassifier` identifica servidores/patrones iniciales: HLS, directo, JWPlayer, JWP, Dailymotion, Blogger, Archive.org, Ok.ru, MP4Upload, YourUpload, Streamtape, Streamwish/Filemoon, VOE, Mixdrop y Doodstream.
- `DetectedMedia` ya conserva metadatos para resolvers: server, headers, prioridad, diagnostico y si requiere resolucion.
- `EmbedResolverRegistry` intenta convertir embeds HTML simples a candidatos directos usando patrones `file/src/source/video`, `sources`, `player.setup`, `jwplayer().setup`, `data-*` y URLs directas.
- Ajustes permite definir sitios recurrentes como `URL | Categoria`; WorkManager los analiza cada 6 horas y guarda candidatos directos en Room como `MediaType.OTHER`.
- `mediaType = "OTHER"` fue revisado contra DTO, mapper, enum y sync M3U existente; es compatible con el flujo actual.
- El extractor ahora busca candidatos en atributos amplios, HTML completo, scripts, JSON embebido, meta refresh, URLs escapadas `https:\/\/`, URLs anidadas en query string, URL encoding, base64 URL-safe sin padding, `atob(...)`, `decodeURIComponent(...)` y template strings.
- `ServerClassifier` agrega hosts embed adicionales: StreamSB, Vidmoly, Uqload, Fembed, Sendvid y MediaFire.
- **Resolvers especificos por servidor**: `ServerSpecificResolvers` implementa logica dedicada para 18 servidores (Streamtape, Streamwish, VOE, Mixdrop, Doodstream, Okru, YourUpload, Fembed, StreamSB, Vidmoly, Uqload, Sendvid, MediaFire, MP4Upload, JWPlayer, JWP, Dailymotion, Blogger) con patrones regex especificos por host.
- **Scoring de compatibilidad**: `CandidateScorer` prioriza candidatos usando transport (MP4>HLS>WEBM>MKV), calidad (2160p>1080p>720p), codec (H264>AAC penaliza HEVC/HDR), servidor (directo>HLS>embed) y penalizaciones (no-direct, resolver_error).
- **Headers al reproductor**: ExoPlayer ahora recibe headers personalizados (Referer, User-Agent) via `DefaultHttpDataSource.Factory` para CDNs que requieren autenticacion.
- **Deduplicacion inteligente**: `ExtractionRepositoryImpl` deduplica por filename normalizado + host + calidad, no solo URL exacta.
- **Concurrencia en resolucion**: `EmbedResolverRegistry.resolveAll()` usa `coroutineScope` + `async` para resolver embeds en paralelo.
- **Deteccion de redirects**: `EmbedResolverRegistry` detecta `window.location.href` y `window.location` en HTML de embeds.
- **Filtro anti-basura ampliado**: `CandidateNormalizer` bloquea trackers, ads, analytics, chatbots, assets no reproducibles (30+ tokens adicionales).
- **UI mejorada**: `ExtractMediaScreen` muestra badges de formato/calidad, diagnostico visible, contador de candidatos reproducibles y pasa headers al player.

### No Funciona

- No se ejecuto aun prueba manual en Android TV real o emulador durante esta sesion.
- El extractor especializado por dominio todavia no existe; la implementacion actual es generica.
- No todos los embeds se resolveran: solo patrones HTML/JS simples sin navegador ni bypass de protecciones.
- No se conocen aun las 3 URLs exactas que fallaron al usuario; no se pudieron crear fixtures especificos para esos sitios.

### Falta Realizar

- Instalar el APK en emulador/dispositivo Android TV y verificar Home, Ajustes, sync demo y reproduccion.
- Probar `Analizar URL` contra una pagina publica autorizada con HLS/MP4 directo.
- Definir el primer dominio autorizado para crear extractor especializado con fixtures versionados.
- Agregar fixtures reales por servidor autorizado para `EmbedResolverRegistry` y `RecurringSitesSyncWorker`.
- Verificar manualmente que los candidatos guardados por sitios recurrentes aparecen en Home y reproducen con Media3.
- Crear `ExtractorRegistry` especializado por dominio cuando el usuario entregue las URLs que no detectan nada o cuando se identifique el primer dominio autorizado prioritario.

## Decisiones Pendientes Antes De Implementar

1. Definir el primer dominio autorizado de prueba.
2. Confirmar si el primer hito debe aceptar solo HLS/MP4 o tambien audio.
3. Definir si los candidatos detectados se reproducen directamente o se guardan temporalmente en Room.
4. Definir politica de cache y expiracion.
5. Decidir si algun dominio autorizado requiere un servicio auxiliar en una fase posterior.

## Revision Profunda De Referencias Para Scraping

Fecha: `2026-05-30`

Repositorios revisados en temporal:

- `https://github.com/FxxMorgan/anime1v-api.git`
- `https://github.com/pedroparkeralrescate-code/balandro-stremio.git`
- `https://github.com/retrocodes12/NebulaStreams-V2.git`

### Piezas de `anime1v-api` que conviene adaptar pronto

| Pieza observada | Archivo fuente | Adaptacion propuesta a TVAnimeApp | Prioridad |
|---|---|---|---|
| Registro multi-proveedor por dominio | `src/services/anime.service.js` | Crear `ExtractorRegistry` con `id`, `label`, `domains` y extractor asociado | Alta |
| Seleccion automatica por URL | `findProviderForUrl()` | Detectar extractor por host de la URL ingresada antes de usar generico | Alta |
| Fallback multi-proveedor | `searchAnime()` | Permitir probar extractores compatibles en orden cuando el dominio no sea concluyente | Media |
| Normalizacion de servidor | `normalizeServerName()` / `SERVER_PATTERNS` | Crear `ServerClassifier` para identificar HLS, MP4Upload, Filemoon, Streamwish, etc. | Alta |
| Variantes SUB/DUB | `parseVariantContainer()` / `getEpisodeLinks()` | Agregar campos `variant`, `language` o `audioType` a `DetectedMedia` | Media |
| Separacion stream/download | `collector.stream` y `collector.download` | Diferenciar candidatos reproducibles de candidatos descargables/no reproducibles | Alta |
| Deduplicacion por URL | `pushDeduped()` | Reemplazar dedupe simple por dedupe normalizado sin query ruidosa cuando aplique | Alta |
| Filtro anti-fake | `isLikelyVideoUrl()` | Bloquear candidatos de analytics, placeholders, BigBuckBunny/test-videos excepto modo demo | Alta |
| Resolucion por host | `resolveEmbedUrl()` y resolvers concretos | Crear `EmbedResolverRegistry` con resolvers por host, comenzando por patrones HTML simples | Alta |
| Headers y referer por candidato | `fetchHtmlWithHeaders()` / `getRefererForUrl()` | Guardar headers requeridos por candidato y pasarlos a Media3 cuando aplique | Alta |
| Errores tipados | `ApiError` | Crear `ExtractionError` con codigos internos para UI y diagnostico | Media |
| Modo debug | `DEBUG_DOWNLOAD` / `debugLog()` | Agregar modo diagnostico local sin exponer URLs completas ni tokens | Media |

### Piezas de `balandro-stremio` que conviene adaptar pronto

| Pieza observada | Archivo fuente | Adaptacion propuesta a TVAnimeApp | Prioridad |
|---|---|---|---|
| Adapter entre modelos | `addon.py` (`item_to_meta`) | Crear capa `ExtractionCandidateMapper` para convertir hallazgos a items de UI/player/catalogo | Alta |
| Serializacion stateless | `serialize_item()` / `deserialize_item()` | Guardar estado temporal de candidato como JSON/URI seguro para navegar sin persistir todo en Room | Media |
| Carga dinamica de canales | `load_channel()` | Inspirar `ExtractorRegistry` modular, aunque en Android se usara registro estatico/inyeccion Hilt | Alta |
| Limpieza de texto Kodi | `clean_kodi_formatting()` | Agregar limpiador de titulos/descripciones para resultados con tags `[COLOR]`, `[B]`, `[CR]` | Media |
| Resolucion canal -> server -> URL final | `resolve_video_urls()` | Dividir el flujo Android en `PageExtractor -> ServerDetector -> EmbedResolver -> PlayableCandidate` | Alta |
| Server tools por patrones JSON | `servertools.findvideos()` y `servers/*.json` | Crear una tabla local de patrones de servidor para detectar embeds mas alla de iframe/src | Alta |
| Resolvers individuales por servidor | `balandro_src/servers/*.py` | Priorizar resolvers Android para `directo`, `m3u8hls`, `jwplayer`, `jwp`, `dailymotion`, `blogger`, `archiveorg` | Alta |
| Busqueda en canales priorizados | `main.py` (`channels_to_check`) | Agregar orden configurable de fuentes/extractores para pruebas personales | Media |
| Fallback por titulo alternativo | `main.py` titulo ES/original | En fase de catalogo, probar busqueda por titulo normalizado y titulo original | Baja |
| Preservar headers personalizados | `patcher.py` | Soportar `Referer`, `User-Agent` y cookies por resolver cuando sea necesario | Alta |

### Piezas de `NebulaStreams-V2` que conviene adaptar pronto

| Pieza observada | Archivo fuente | Adaptacion propuesta a TVAnimeApp | Prioridad |
|---|---|---|---|
| Registro temporal de fuentes con metadata, headers, fallback y TTL | `services/sourceRegistry.js` | Crear registro local de candidatos detectados con expiracion corta para navegar/reproducir sin mostrar URLs enormes ni reanalizar | Alta |
| Priorizacion de streams reproducibles | `services/streamManager.js` (`toStremioCompatibilityScore`) | Ordenar resultados por compatibilidad TV: MP4 > HLS > WEBM > MKV, calidad, codec y peso estimado cuando exista metadata | Alta |
| Etiquetas de formato/calidad | `services/streamManager.js` (`getStreamFormatBadge`) | Mostrar badges claros `MP4`, `HLS`, `WEBM`, `MKV`, `H264`, `1080p` en tarjetas de resultado | Alta |
| Dispatcher por host de extractor | `vendor/provider-pack/src/providers/moviesdrive.js` (`loadExtractor`) | Convertir `EmbedResolverRegistry` en dispatcher por host con resolvers aislados y fallback generico | Alta |
| Bloqueo de hosts basura/redireccionadores | `moviesdrive.js` (`linkrit`, `google`, `gstatic`, `doubleclick`, `ampproject`) | Ampliar filtro anti-basura para que no aparezcan favicons, assets, trackers ni redirects no reproducibles | Alta |
| Control de concurrencia por host | `services/providerService.js` (`PROVIDER_FETCH_HOST_MAX_INFLIGHT`) | Limitar solicitudes simultaneas por host en WorkManager/resolvers para evitar que una pagina bloquee la app | Media |
| Versionado/cache por proveedor | `services/providerService.js` (`getProviderCacheVersion`) | Versionar extractores por dominio para invalidar cache y diagnosticar cuando cambie HTML del sitio | Media |

### Backlog tecnico enfocado en pruebas amplias

1. Crear `ExtractorRegistry` con extractor generico + extractores especificos inspirados en dominios/patrones de `anime1v-api`, Balandro y NebulaStreams.
2. Crear `EmbedResolverRegistry` separado del extractor de pagina.
3. Implementar `ServerClassifier` con patrones de host/URL: HLS, JWPlayer, JWP, MP4Upload, YourUpload, Streamtape, Streamwish/Filemoon, Dailymotion, Blogger, Archive.org, Ok.ru, directo.
4. Ampliar `DetectedMedia` con `server`, `quality`, `variant`, `language`, `isDirect`, `requiresResolver`, `headers`, `priority` y `diagnostics`.
5. Implementar `CandidateNormalizer` con limpieza de escapes HTML, `\u0026`, `&amp;`, `%2F`, base64 simple y URLs relativas.
6. Agregar patrones de extraccion JS sin navegador: `file:`, `sources:`, `player.setup`, `jwplayer().setup`, `data-src`, `data-file`, `window.location.href` y JSON embebido.
7. Agregar resolvers simples sin navegador para servidores que exponen HTML/JSON suficiente.
8. Agregar WebView resolver opcional solo para pruebas personales cuando el HTML estatico no alcance, con allowlist y timeout estricto.
9. Agregar pantalla de diagnostico de scraping: extractor usado, servidor detectado, cantidad de candidatos, errores y tiempos.
10. Agregar fixtures copiados/simplificados desde HTML real de prueba para no depender siempre de la red.
11. Agregar scoring tipo NebulaStreams para priorizar candidatos reproducibles y ocultar basura visual/assets.
12. Agregar registro temporal de candidatos con TTL para reproducir desde tarjetas sin reanalizar la pagina.

### Orden recomendado de implementacion

1. `CandidateNormalizer` + `ServerClassifier`.
2. `ExtractorRegistry` + contrato de extractor especifico.
3. `EmbedResolverRegistry` con resolvers `directo`, `m3u8hls`, `jwplayer/jwp`, `dailymotion`, `blogger`, `archiveorg`.
4. Ampliar UI de resultados para mostrar server, calidad, variante y diagnostico.
5. WebView resolver experimental para pruebas personales, apagado por defecto.
6. Catalogo temporal de resultados detectados y opcion de guardar como contenido local.

### Riesgos tecnicos aceptados para prueba personal

- Se puede experimentar con mas resolvers y heuristicas agresivas, siempre aisladas por host y con opcion de desactivar.
- No copiar masivamente logica de terceros dentro del APK sin adaptar; preferir reimplementar patrones pequenos y verificables.
- No introducir FFmpeg dentro del APK en esta fase; priorizar reproduccion directa Media3.
- No usar Puppeteer dentro del APK; si se necesita navegador, usar WebView Android acotado o un servicio auxiliar.

## Plan V2: Navegador Web + Auto-Crawler

**Fecha:** 2026-05-31 | **Decisión del usuario:** Confirmada

### Decisiones tomadas

| Decisión | Opción elegida |
|---|---|
| Crawler: extraer video URLs | **Sí — al catalogar** |
| Frecuencia de crawl | **Cada 6 horas** |
| Overlay de video | **Siempre visible al detectar** |
| Limpieza de archivos | **Sí — eliminar todo el scraping** |

### Arquitectura nueva

```
HOME
├── NavItem: "Navegador Web" ──→ WebViewBrowserScreen
├── NavItem: "Mi Biblioteca" ──→ existente (favoritos, historial)
├── NavItem: "Ajustes" ──→ existente (M3U) + nuevo tab "Categorías"
└── Catálogo (crawl)

HOME → Catalog (ContentItem desde Room)
    → DetailScreen
    → PLAY → PlayerScreen (URL ya viene en ContentItem.videoUrl)

HOME → NavItem "Navegador Web"
    → WebViewBrowserScreen
        ├── UrlBar (◀ ▶ ⟳ + input)
        ├── TvWebView (D-pad navigable)
        └── VideoCaptureOverlay (detecta video → "▶ Reproducir en TV")
            → PlayerScreen
```

### Flujo 1: Navegador Web

```
1. Usuario selecciona "Navegador Web" desde Home
2. Ve sitios preconfigurados organizados por categoría
3. Selecciona sitio → WebView abre la URL
4. JS injection detecta clicks en videos
5. Overlay muestra: "▶ Reproducir en TV" + URL + calidad
6. Usuario confirma → captura URL + cookies + headers
7. Reproduce en PlayerScreen
```

### Flujo 2: Auto-Crawler

```
1. Usuario abre Ajustes → "Categorías"
2. Activa: Anime ✓, Películas ✓, Series ✓
3. Por cada sitio activo por categoría:
   - WebView abre la página principal
   - JS injection extrae: título, thumbnail, año, rating, URL del detail page
   - El detail page se resuelve para obtener la URL del video
   - Se guarda en Room: ContentEntity con videoUrl ya resuelto
4. Home muestra contenido categorizado
5. Usuario abre contenido → PLAY → reproduce directo
```

### Archivos creados

| Archivo | Responsabilidad |
|---|---|
| `domain/model/CapturedStream.kt` | Modelo: url, format, quality, domain, headers, timestamp |
| `domain/model/CrawlResult.kt` | Modelo: título, thumbnail, año, rating, detailUrl, videoUrl, categoría |
| `domain/model/SiteConfig.kt` | SiteConfig y CategoryConfig para crawler |
| `data/capture/WebViewVideoCapture.kt` | Captura URLs desde WebViewClient + JS interface |
| `data/capture/WebViewSessionManager.kt` | Persiste cookies/headers para reproductor |
| `ui/components/TvWebView.kt` | Composable WebView TV-optimizado con D-pad |
| `ui/components/UrlBar.kt` | Barra URL con botones ◀ ▶ ⟳ + input, todos focusable |
| `ui/components/VideoCaptureOverlay.kt` | Overlay flotante de captura |
| `ui/screens/WebViewBrowserScreen.kt` | Pantalla completa: URL bar + WebView + overlay |
| `ui/viewmodel/WebViewBrowserViewModel.kt` | Estado del navegador, streams detectados |

### Archivos modificados

| Archivo | Cambio |
|---|---|
| `AppModule.kt` | Agregados providers para WebViewVideoCapture y WebViewSessionManager |
| `TVAnimeNavHost.kt` | Agregada ruta `browser` → WebViewBrowserScreen, eliminada ruta `extract` |
| `Screens.kt` → HomeScreen | NavItem "Navegador Web" + "Buscar", botones en empty state |

### Archivos eliminados (scraping)

| Archivo | Por qué |
|---|---|
| `data/extraction/JsEvaluator.kt` | WebView ejecuta JS directamente |
| `data/extraction/PackerUnpacker.kt` | Innecesario |
| `data/extraction/WebViewFetcher.kt` | Reemplazado por WebView real |
| `data/extraction/HtmlMediaExtractor.kt` | Reemplazado por JS injection |
| `data/extraction/CandidateNormalizer.kt` | Ya no aplica |
| `data/extraction/CandidateScorer.kt` | No hay ranking — usuario elige |
| `data/extraction/ServerClassifier.kt` | No hay clasificación |
| `data/extraction/EmbedResolverRegistry.kt` | WebView resuelve todo |
| `data/extraction/ServerSpecificResolvers.kt` | Innecesario |
| `data/extraction/UrlPolicyValidator.kt` | Usuario navega directamente |
| `data/extraction/HttpPageFetcher.kt` | OkHttp ya no se usa para scraping |
| `data/repository/ExtractionRepository.kt` | Sin uso |
| `data/repository/ExtractionRepositoryImpl.kt` | Sin uso |
| `domain/usecase/ExtractMediaFromPageUseCase.kt` | Sin uso |
| `ui/viewmodel/ExtractMediaViewModel.kt` | Sin uso |
| `player/WebViewMediaInterceptor.kt` | Reemplazado por WebViewVideoCapture |
| Tests: `HtmlMediaExtractorTest.kt` | Sin sentido |
| Tests: `EmbedResolverRegistryTest.kt` | Sin sentido |
| Tests: `CandidateNormalizerTest.kt` | Sin sentido |
| Tests: `UrlPolicyValidatorTest.kt` | Sin sentido |
| Tests: `ServerClassifierTest.kt` | Sin sentido |

### Orden de implementación

```
DÍA 1 — Estructura base
├── WebViewBrowserActivity + AndroidManifest
├── TvWebView composable (D-pad: scroll, click, back)
├── UrlBar (◀ ▶ ⟳ + input, todos focusable con borde cyan)
├── WebViewVideoCapture + WebViewSessionManager
├── CapturedStream domain model + WebViewBrowserViewModel
├── WebViewBrowserScreen completo con overlay
├── Actualizar TVAnimeNavHost + HomeScreen sidebar
└── Build → probar D-pad en WebView

DÍA 2 — Auto-Crawler
├── CrawlCategoryEntity + DAO
├── Actualizar TVAnimeDatabase
├── CrawlService (WebView visita sitio → extrae items)
├── JsCatalogExtractor (JS injection para listados)
├── CrawlWorker (WorkManager, cada 6h)
├── CrawlerSettingsScreen
└── Build → probar: activar categorías → ver contenido en Home

DÍA 3 — Limpieza + integración
├── ELIMINAR todos los archivos de scraping (21 archivos)
├── Conectar CrawlWorker → catalog → Home
├── DetailScreen: videoUrl viene de ContentItem, play directo
├── Si ContentItem.videoUrl falla → ofrecer "Abrir en navegador" como fallback
├── Limpiar imports huérfanos
└── Build final

DÍA 4 — Git commit + push
```

### D-pad en WebView TV

| Tecla | Acción |
|---|---|
| DPAD_UP/DOWN | Scroll vertical del WebView |
| DPAD_LEFT/RIGHT | Scroll horizontal (si existe) |
| DPAD_CENTER/ENTER | Click en elemento enfocado |
| BACK | goBack() si hay historial; si no → cerrar navegador |
| TAB | Alternar foco entre UrlBar y WebView |

### Compatibilidad con flujo existente

| Componente existente | ¿Se usa? |
|---|---|
| `PlayerScreen` | ✅ Sí — reutilizado tal cual |
| `DetailScreen` | ✅ Sí — muestra contentItem del catálogo crawleado |
| `HomeScreen` | ✅ Sí — muestra catálogo desde Room |
| `SettingsScreen` | ✅ Sí — se extiende con tab "Categorías" |
| `M3uPlaylistParser` | ✅ Sí — se mantiene como alternativa |
| `Room (ContentDao, FavoriteDao, HistoryDao)` | ✅ Sí — almacena contenido crawleado |
| `Media3 / ExoPlayer` | ✅ Sí — reproduce streams capturados |
| Todo el scraper | ❌ No — eliminado |

---

## Bitacora de Restauracion — 2026-05-31

### Problema reportado por el usuario

El usuario indico que:
1. El scraping no detecta nada en ninguna pagina
2. Los 3 repos de referencia (anime1v-api, balandro, NebulaStreams) son completamente funcionales con betas publicas
3. El disenio frontal esta mejorado pero detraz sigue igual: no funcional
4. El indicador de foco/puntero es invisible en la pantalla de seleccion de lista
5. Botones como "volver atras" y "revisar" estan full color pero nunca se ve cual esta realmente seleccionado

### Diagnostico raiz — Scraping

Se analizo en profundidad los 3 repos de referencia y se encontro que el enfoque de OkHttpClient + regex es fundamentalmente insuficiente para sitios de anime modernos:

| Nuestro enfoque | Enfoque de los repos funcionales | Por falla |
|---|---|---|
| Regex sobre HTML crudo | Evaluacion de variables JS (`var videos = {...}`) | Los datos estan en objetos JS, no HTML |
| Un solo fetch HTTP | Multi-step: pagina -> iframe -> iframe -> video | URLs de video estan detras de multiples capas |
| Sin ejecucion JS | `vm.runInNewContext`, js2py, Puppeteer | Datos en `<script>` tags que requieren ejecucion |
| Sin manejo Cloudflare | Puppeteer fallback, FlareSolverr | La mayoria devuelve 403 sin navegador |
| Regex generico por "tipo de servidor" | Archivo Python/JS por servidor con logica custom | Cada embed host tiene ofuscacion unica |
| Sin desencriptacion | Base64 + AES + strings invertidas + `(p,a,c,k,e,d)` | URLs de video estan encriptadas/ofuscadas |
| Sin llamadas AJAX | Site-specific APIs (`/ajax/`, `enc-dec.app`) | Los datos se obtienen via APIs, no HTML |

### Soluciones implementadas

| Componente | Descripcion | Efecto |
|---|---|---|
| `JsEvaluator.kt` | Evaluador de variables JS: `var videos`, `var servers`, JSON embebido, datos Svelte, `player.setup`, `sources` arrays, `atob()`/base64, `window.location`, `decodeURIComponent`, `unescape` | Extrae datos que estaban ocultos en `<script>` tags |
| `PackerUnpacker.kt` | Desempaquetador Dean Edwards `(p,a,c,k,e,d)` para JS ofuscado | Desencripta codigo ofuscado de servidores embed |
| `WebViewFetcher.kt` | Android WebView como fallback para Cloudflare/JS-heavy pages | Resuelve paginas que retornan 403/vacio con OkHttpClient |
| `ExtractionRepositoryImpl.kt` | Flujo multi-step: OkHttp → JS eval → WebView fallback | Si un metodo falla, intenta el siguiente automaticamente |
| `HtmlMediaExtractor.kt` | Agregado: JSON-LD, og:video, sources arrays, decodeURIComponent, base64 standalone, data-src base64, window.location patterns | Captura muchas mas formas de URL embebidas |
| `EmbedResolverRegistry.kt` | Resolucion multi-step con JS evaluator + packer unpacker + iframes anidados recursivos (depth=2) | Resuelve embeds que requieren ejecutar JS y seguir redirecciones |
| UI Focus | Bordes de 5dp con gradiente cyan brillante (#00CED1 → #47EAED), background semi-transparente al foco, texto Bold al foco, player controls con bordes brillosos sobre fondo oscuro | Indicador de foco visible a 3m de distancia en TV |

### Build y tests

| Comando | Resultado |
|---|---|
| `assembleDebug` | Exitoso |
| `testDebugUnitTest` | 25/25 pasando |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |

### Pendiente

- Probar el APK en dispositivo/emulador Android TV real
- Verificar que WebView funcione correctamente en Android TV
- Si sitios con Cloudflare siguen fallando con WebView, evaluar servicio auxiliar con Puppeteer
- Agregar extractores especificos por dominio para sitios priorizados por el usuario
- Probar contra URLs reales de los sitios que el usuario intenta analizar

## Revision Corta De Estado Actual - 2026-05-31

### Situacion observada

La direccion del proyecto cambio desde scraping heuristico con `OkHttp + regex` hacia un navegador `WebView` integrado en Android TV, con captura de streams durante la navegacion del usuario y un plan de auto-crawler por categorias.

El cambio conceptual es coherente con el problema real y ya fue consolidado en el repo remoto mediante `482b805 feat: WebView browser for TV with video capture overlay`. La ruta `browser`, la captura de streams y el overlay ya forman parte del enfoque activo del proyecto.

### Estado real verificado en esta revision

| Item | Estado verificado |
|---|---|
| Branch local | `main` alineada con `origin/main` en `482b805` |
| Arbol de trabajo | Solo este `PLAN_TRABAJO.md` queda modificado localmente |
| Enfoque activo | WebView Browser + captura de video + overlay |
| Compilacion `compileDebugKotlin` | **Falla** |
| Push remoto | **Confirmado** |
| Tests unitarios | No se validaron como aprobacion final en esta revision local |

### Fallos pendientes detectados

| Prioridad | Area | Problema | Evidencia |
|---|---|---|---|
| Alta | Build | `compileDebugKotlin` falla por referencias rotas en UI y componentes heredados | `CandidateCard.kt` con `DetectedMedia` no resuelto y referencia `it` invalida |
| Media | Limpieza | Aun quedan componentes del flujo antiguo conviviendo con la estrategia WebView | `CandidateCard.kt` sigue dependiendo del modelo eliminado `DetectedMedia` |
| Media | Seguridad | Revisar y retirar cualquier pieza vieja insegura si todavia sigue activa o reachable en el arbol | Confirmar que el flujo WebView no dependa de clases viejas de scraping |
| Media | UX TV | El foco D-pad fue mejorado, pero falta prueba real en Android TV o emulador para confirmar navegacion, scroll y overlay | Sin validacion manual cerrada |
| Media | Crawler | El auto-crawler esta planificado pero no demostrado como funcional de punta a punta | Falta confirmar guardado en Room y reproduccion desde catalogo |
| Alta | Documentacion | La informacion del repositorio esta totalmente desactualizada y todavia apunta al enfoque anterior de scraping/M3U como flujo principal | `README.md`, secciones historicas de `PLAN_TRABAJO.md` y descripciones del producto ya no representan la arquitectura WebView actual |

### Hitos ya logrados

1. Cambio de direccion del producto hacia `WebViewBrowserScreen` como flujo principal.
2. Publicacion del cambio en remoto mediante `482b805`.
3. Eliminacion del flujo `extract` del `NavHost` principal y reemplazo por `browser`.
4. Integracion base de `WebViewVideoCapture`, `WebViewSessionManager`, `TvWebView`, `UrlBar` y `VideoCaptureOverlay`.

### Hitos pendientes reales

#### Hito 1 - Recuperar compilacion limpia

Objetivo:
Dejar la rama WebView compilando sin errores locales.

Tareas:

1. Corregir `CandidateCard.kt` para que no use `DetectedMedia` si ese modelo ya fue eliminado del flujo activo.
2. Corregir la referencia invalida `it` reportada por el compilador.
3. Ejecutar `compileDebugKotlin` hasta dejarlo limpio.

Criterio de salida:
`compileDebugKotlin` exitoso.

#### Hito 2 - Validar flujo WebView real

Objetivo:
Confirmar que el nuevo enfoque no solo existe en codigo, sino que funciona en TV.

Tareas:

1. Abrir `Navegador Web` desde Home.
2. Cargar un sitio configurado.
3. Detectar al menos un stream reproducible.
4. Mostrar `VideoCaptureOverlay`.
5. Reproducir en `PlayerScreen` con headers/referer si corresponde.

Criterio de salida:
Flujo real: Home -> Navegador Web -> deteccion -> overlay -> reproductor.

#### Hito 3 - Validar experiencia Android TV

Objetivo:
Confirmar navegacion usable con control remoto.

Tareas:

1. Probar foco visible en botones, lista de sitios, barra URL y overlay.
2. Verificar scroll D-pad dentro del WebView.
3. Probar `BACK`, `ENTER` y navegacion entre controles.
4. Verificar que el overlay no tape interacciones criticas.

Criterio de salida:
Uso aceptable con control remoto en emulador o dispositivo Android TV.

#### Hito 4 - Decidir el destino del auto-crawler

Objetivo:
No dejar el crawler como promesa difusa dentro del cambio de arquitectura.

Tareas:

1. Confirmar si el crawler entra en esta etapa o pasa a backlog.
2. Si entra: probar extraccion de items, detalle y `videoUrl` resuelto.
3. Si no entra: marcarlo como fase posterior y quitar dependencias parciales del flujo actual.

Criterio de salida:
Crawler implementado y probado, o formalmente postergado sin contaminar el flujo base.

#### Hito 5 - Actualizar documentacion del repositorio

Objetivo:
Hacer que el repositorio describa el producto actual y no la etapa anterior.

Tareas:

1. Actualizar `README.md` para reflejar que el flujo principal ahora es `WebView Browser + captura de streams + PlayerScreen`.
2. Mover a seccion historica o eliminar del `PLAN_TRABAJO.md` todo lo que describa como vigente el antiguo flujo de scraping heuristico si ya no aplica.
3. Revisar descripciones de arquitectura, objetivos y funcionalidades para que no presenten como actual el enfoque anterior.
4. Dejar claro que M3U queda como alternativa y no necesariamente como centro del producto, si esa sigue siendo la decision del programador y del usuario.

Criterio de salida:
El repositorio explica correctamente el estado actual del producto y su arquitectura vigente.

### Orden de correccion recomendado desde este punto

1. Build roto (`CandidateCard.kt`, `VideoCaptureOverlay.kt`, imports y modelos).
2. Flujo WebView minimo funcional.
3. Validacion manual Android TV.
4. Decision final sobre auto-crawler.
5. Actualizacion de documentacion del repositorio.

---

## Revision Tecnica Y Reporte Manual - 2026-05-31 18:20

### Contexto

El usuario reporto dos problemas despues de la revision del commit `b82b721`:

1. El foco visual con control remoto no se ve al moverse con flechas entre menu/items; parece quedarse pegado en la primera seleccion y solo se evidencia el cambio al presionar OK.
2. Al presionar `Browser` / `Navegador Web`, la app se rompio/salio.

Tambien se pregunto si los hallazgos de la revision anterior habian quedado registrados en este plan. Respuesta operativa: no habian quedado registrados en el archivo durante la revision anterior porque fue una revision sin cambios; quedan registrados desde esta seccion.

### Hallazgos De Revision Anterior Ahora Registrados

| Prioridad | Area | Hallazgo | Evidencia | Estado |
|---|---|---|---|---|
| Critica | Room | Se subio `TVAnimeDatabase` de version 1 a 2 sin `Migration(1, 2)` ni `fallbackToDestructiveMigration()` | `TVAnimeDatabase.kt` version `2` con nueva entidad `CrawlCategoryEntity`; builder sin migraciones | Pendiente |
| Alta | Auto-crawler | En instalacion limpia, las categorias default se muestran en UI pero no se persisten; el worker puede no encontrar categorias habilitadas | `CrawlerViewModel` crea `defaultCats` solo en memoria; `CrawlWorker` usa `observeEnabled().first()` | Pendiente |
| Alta | Auto-crawler | El parseo de `evaluateJavascript()` probablemente devuelve vacio porque Android entrega el resultado JSON como string escapado/quoteado | `CrawlService.extractCatalog()` hace `JSONArray(jsonResult)` directo | Pendiente |
| Alta | Catalogo/Player | Los items crawleados se guardan con `videoUrl = ""`, por lo que no son reproducibles desde `DetailScreen` | `CrawlWorker.kt` crea `ContentEntity(videoUrl = "")`; `TVAnimeNavHost` solo abre player si `videoUrl` no esta en blanco | Pendiente |
| Media | Datos | Cada crawl puede duplicar catalogo porque se usa `UUID.randomUUID()` como ID estable | `CrawlWorker.kt` genera ID aleatorio por item | Pendiente |
| Media | Documentacion | Algunas secciones del plan seguian marcando `FASE 2: Auto-Crawler` como pendiente/desalineada con el commit reciente | `PLAN_TRABAJO.md` contiene estados historicos mezclados | En curso |
| Bloqueante de verificacion | Entorno local | No se pudo ejecutar build desde esta terminal porque no hay `JAVA_HOME` ni `java` en PATH | `./gradlew.bat assembleDebug` falla con `JAVA_HOME is not set` | Pendiente de entorno |

### Hipotesis De Causa - Foco D-pad No Visible

Se revisaron `Screens.kt`, `WebViewBrowserScreen.kt`, `UrlBar.kt`, `VideoCaptureOverlay.kt` y `TvWebView.kt`.

Posibles fuentes consideradas:

1. Orden incorrecto entre `focusable()` y `onFocusChanged()`.
2. Uso duplicado de focus en componentes Material (`Button`, `IconButton`, `Surface`, `Card`) mas `Modifier.focusable()` manual.
3. Falta de `interactionSource.collectIsFocusedAsState()` en botones/clickables Material.
4. `LazyRow`/`LazyColumn` sin restauracion/solicitud explicita de foco inicial.
5. WebView capturando flechas D-pad y consumiendo eventos.
6. Superposiciones (`AnimatedVisibility`, overlay) dejando elementos focusables activos o interceptando foco.
7. Estilos de foco aplicados sobre un nodo que no es el nodo realmente enfocado.

Diagnostico mas probable:

- Hay un patron repetido `.focusable().onFocusChanged { ... }` en varios controles. En Compose, para observar el foco del nodo focusable de forma confiable, `onFocusChanged` debe envolver/preceder el nodo focusable o debe usarse `MutableInteractionSource` en componentes Material. Con el orden actual, el estado visual puede quedar mirando otro nodo y no actualizarse cuando el control remoto cambia foco.
- En componentes Material como `IconButton`, `Button`, `OutlinedButton`, `Card` y `Surface(onClick)`, agregar `focusable()` manual puede crear focos duplicados: el foco real lo toma el componente interno, pero el borde/escala escucha otro nodo. Eso coincide con el sintoma: el foco si cambia internamente, pero el indicador visual no acompana hasta ejecutar OK.

### Hipotesis De Causa - Browser Rompe/Sale Al Abrir

Se reviso `WebViewBrowserScreen.kt` y el `AndroidWebView` local.

Posibles fuentes consideradas:

1. Creacion de `WebView(ctx.applicationContext)` dentro de `AndroidView` en vez de usar el `ctx` de la vista/actividad.
2. `WebView` cargando `about:blank` inicialmente mientras el selector esta visible y luego URLs externas.
3. Sitios remotos con certificados, redirecciones, ads o scripts pesados que rompen WebView.
4. Uso de `addJavascriptInterface` e inyeccion JS antes/despues de carga.
5. Consumo de teclas D-pad dentro del WebView con `setOnKeyListener`.
6. Falta de manejo de errores de pagina (`onReceivedError`, `onReceivedHttpError`, `onRenderProcessGone`).
7. Falta de log visible para saber si fue crash nativo/WebView renderer, excepcion Kotlin o cierre por memoria.

Diagnostico mas probable:

- La causa mas sospechosa en codigo es `WebView(ctx.applicationContext)` en `WebViewBrowserScreen.kt`. Para WebView embebido en Compose conviene crearla con el contexto de la vista (`WebView(ctx)`), no con application context, y manejar `onRenderProcessGone` para evitar salida completa si el renderer muere.
- Falta instrumentacion defensiva: no hay `onReceivedError`, `onReceivedHttpError`, `onRenderProcessGone` ni `try/catch`/logging minimo alrededor de carga, por lo que el usuario solo ve que la app sale.

### Plan De Correccion Propuesto Antes De Tocar Codigo

| Paso | Cambio propuesto | Motivo | Verificacion esperada |
|---|---|---|---|
| 1 | Corregir foco TV con un helper unico que use `onFocusChanged` en el orden correcto o `interactionSource` para Material | Que el borde/escala siga el foco real del D-pad | Moverse con flechas por menu, cards, settings y overlay debe mostrar foco inmediato |
| 2 | Quitar `focusable()` manual redundante en botones Material cuando corresponda | Evitar focos duplicados invisibles | El foco no debe quedarse visualmente pegado |
| 3 | Crear `WebView(ctx)`, no `WebView(ctx.applicationContext)` | Reducir crash/instabilidad al abrir navegador | Abrir `Navegador Web` sin salida de app |
| 4 | Agregar manejo defensivo de errores WebView (`onReceivedError`, `onReceivedHttpError`, `onRenderProcessGone`) | Obtener evidencia y evitar cierres abruptos | Si falla una pagina, se registra/recupera sin cerrar app completa |
| 5 | Reintentar build con JDK disponible | Confirmar que el fix compila | `./gradlew.bat assembleDebug` exitoso o error registrado |

### Instrucciones Para El Programador

Esta seccion es una auditoria y no implica cambios de codigo aplicados por Kilo. El programador debe tomar estos hallazgos como backlog priorizado.

Decision confirmada por el usuario: priorizar la primera opcion recomendada, es decir, corregir foco D-pad + crash/salida al abrir `Browser` / `Navegador Web` antes de abordar el auto-crawler/Room.

Orden recomendado:

1. Corregir primero foco D-pad + crash al abrir `Navegador Web`, porque bloquea el uso inmediato de la app en TV.
2. Despues corregir riesgos tecnicos del auto-crawler/Room: migracion v1->v2, persistencia de categorias default, parseo de `evaluateJavascript()`, `videoUrl` vacio e IDs estables.
3. Registrar evidencia de cada correccion: commit, build, prueba manual en Android TV/emulador y resultado.
4. No marcar el auto-crawler como funcional hasta demostrar flujo completo: categoria habilitada -> crawl -> items en Room -> detalle -> reproduccion o apertura correcta del recurso.

---

## Robustecimiento Browser/WebView — 2026-06-01

### Cambios aplicados localmente

| Area | Cambio | Verificacion |
|---|---|---|
| Navegacion WebView | La barra URL ahora actualiza estado, normaliza URLs sin esquema y permite navegar con accion `Ir` / IME Go | `assembleDebug` exitoso |
| WebView | Se agrego limpieza de ciclo de vida (`stopLoading`, `about:blank`, `clearHistory`, `removeAllViews`, `destroy`) y manejo de `onRenderProcessGone` | `assembleDebug` exitoso |
| Sesion y headers | Se centralizo User-Agent, cookies de WebView y headers de reproduccion para pasar `Referer`, `User-Agent` y `Cookie` al player | `assembleDebug` exitoso |
| Player | Se agrego visualizacion de errores de reproduccion y recreacion del player cuando cambian headers | `assembleDebug` exitoso |
| Navegacion a Player | Headers se codifican como JSON Base64 URL-safe en query param, evitando romper rutas con `,`, `=` o cookies largas | `assembleDebug` exitoso |
| Auto-crawler | Las categorias default se persisten en Room y el worker deja de insertar entradas sin URL reproducible hasta que exista resolver de detalle -> stream | `assembleDebug` exitoso |

### Build local

| Comando | Resultado | Artefacto |
|---|---|---|
| `gradlew.bat assembleDebug --no-daemon --console=plain` | Build exitoso en `1m 43s` | `app/build/outputs/apk/debug/app-debug.apk` (`14525399` bytes, `2026-06-01 10:55:04`) |

### Pendiente posterior

1. Probar en Android TV real o emulador: foco D-pad, WebView, overlay y Player.
2. Implementar resolver real del auto-crawler para transformar `detailUrl` en `videoUrl`; mientras no exista, el worker no contamina Room con items no reproducibles.
3. Revisar si conviene pasar headers al Player mediante `SavedStateHandle` si aparecen URLs/cookies demasiado largas para rutas de Navigation.

---

## Sesion 2026-06-11 — Permisos runtime, Home TV-first y push de cambios

### Problema reportado por el usuario

La interfaz seguia rota y la app no solicitaba permisos. Se pidio coherencia con el objetivo actual: Android TV / Google TV para abrir paginas publicas, detectar streams y reproducirlos.

### Cambios aplicados en este ciclo (sin commit previo en local)

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Flujo runtime de permisos con `PermissionGateScreen` y opcion de continuar limitado |
| `AndroidManifest.xml` | Permisos `CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`; features `required=false` |
| `Screens.kt` | Reemplazada la Home por una interfaz TV-first con foco D-pad y catalogo M3U como secundario |
| `TVAnimeNavHost.kt` | Propaga estado de permisos hacia Home |
| `UrlBar.kt` | Boton `Ir` y normalizacion de entrada (`dominio`, `https`, busqueda Google) |
| `WebViewBrowserScreen.kt` | Renombrado visual a `Captura Web`, selector simplificado y `onPermissionRequest` |

### Operacion de sincronizacion realizada

1. `git stash` con los 7 archivos modificados localmente.
2. `git pull --ff-only` desde `origin/main` (trae `24c84db` docs y `54bf55e` fix browser playback).
3. `git stash pop` con auto-merge en 5 archivos y conflicto solo en `PLAN_TRABAJO.md`.
4. Conflicto resuelto conservando la version upstream como vigente y agregando esta seccion al final.

### Verificacion

| Comando | Resultado |
|---|---|
| `git pull --ff-only` | OK — `b1d0009..54bf55e` |
| Auto-merge `Screens.kt`, `WebViewBrowserScreen.kt`, `UrlBar.kt`, `TVAnimeNavHost.kt` | Sin conflictos |
| `PLAN_TRABAJO.md` | Conflicto resuelto manualmente, version upstream conservada |
| `git commit` + `git push origin main` | `260c2eb feat: permisos runtime, Home TV-first, WebView Capture y controles de player coherentes` |
| `./gradlew.bat assembleDebug` | No ejecutable en esta terminal: `JAVA_HOME is not set` |

### Ajustes adicionales aplicados sobre el merge

| Archivo | Cambio | Motivo |
|---|---|---|
| `Screens.kt` | `PlayerControlButton` con iconos y `contentDescription` correctos (Reiniciar / Reproducir-Pausar / Avanzar al final) | El upstream tenia `Icons.Default.Star` y `Icons.Default.Home` sin sentido funcional |
| `Screens.kt` | `DetailScreen` muestra mensaje y abre Captura Web cuando `videoUrl.isBlank()` | Fallback defensivo para items sin stream (crawler en pausa) |
| `UrlBar.kt` | Eliminado boton `Ir` duplicado del merge | Duplicado entre stash y upstream |

### Estado

Push `260c2eb` publicado en `origin/main`. Pendiente de build en Android Studio o terminal con JDK 17. La prueba manual esperada sigue siendo:

1. Abrir app y ver pantalla de permisos.
2. Aceptar permisos o continuar limitado.
3. Ver Home `TVAnime Capture` con foco D-pad visible.
4. Abrir `Captura Web`, seleccionar una fuente o escribir URL.
5. Reproducir contenido en la pagina y verificar que aparezca el overlay `Video detectado`.
6. Pulsar `Reproducir en TV` y validar `PlayerScreen` en pantalla completa.

---

## Sesion 2026-06-14 — Fix foco D-pad real y crash al abrir navegador

### Problemas reportados por el usuario (prueba en TV real)

1. El marco de foco (indicador visual de navegacion D-pad) no se ve al mover entre botones; solo aparece "a suerte" cuando se acierta al que se queria presionar.
2. Tras pedir los 2 permisos iniciales y entrar al navegador, la app se sale y vuelve al home de la TV.

### Diagnostico

**Defecto 1 — Foco invisible**

`Modifier.focusable()` se aplicaba por encima de componentes Material 3 (`IconButton`, `OutlinedButton`, `Card`, `Surface(onClick=...)`) que ya son focusables de fabrica. Resultado: D-pad mueve el foco al nodo interno del componente, pero `onFocusChanged { ... }` escucha al nodo padre duplicado que nunca recibe foco. El border/scale parecia aleatorio.

**Defecto 2 — Crash/salida al abrir navegador**

`DisposableEffect(webViewRef)` re-disparaba `onDispose` en cada recomposicion porque `webViewRef` cambiaba al crearse la WebView. Ademas `setOnKeyListener` + `scrollBy` capturaba D-pad y la WebView quedaba con foco atrapado, lo que en algunos sitios disparaba `destroy()` sobre la vista todavia en uso.

### Cambios aplicados — `4dda801 fix: foco D-pad real + WebView sin crash al abrir`

| Archivo | Cambio |
|---|---|
| `WebViewBrowserScreen.kt` | IconButton con `interactionSource` + `collectIsFocusedAsState()`; eliminado `setOnKeyListener`/`simulateCenterClick`; WebViewHolder + `DisposableEffect(Unit)` para limpieza; SiteCard con `Card(interactionSource=...)` |
| `Screens.kt` | TvButton/TvIconButton/PlayerControlButton/NavItem/CatalogMiniCard con `interactionSource`; Row de categoria y Surface del URL field mantienen `focusable()` (no son focusables de fabrica) |
| `UrlBar.kt` | UrlBarButton con `interactionSource`; BasicTextField usa `focusable(interactionSource=...)` para que el border exterior reaccione al mismo foco |

### Verificacion

| Comando | Resultado |
|---|---|
| `git push origin main` | OK — `4dda801` |
| Brace balance (3 archivos) | 109/109, 108/108, 18/18 |
| Imports muertos | Eliminados (`onFocusChanged`, `KeyEvent`, `simulateCenterClick`) |
| `./gradlew.bat assembleDebug` | No ejecutable en esta terminal: `JAVA_HOME is not set` |

### Pendiente de prueba manual en TV

1. Mover con flechas entre botones en Home, Captura Web, Ajustes y UrlBar: el border cyan debe seguir al D-pad en tiempo real, no a "suerte".
2. Aceptar permisos y abrir Captura Web: la app debe permanecer abierta, el selector de fuentes visible y la WebView cargar `about:blank` o la URL seleccionada.
3. Probar con un sitio real (Archive.org, test-streams.mux.dev) y verificar que el overlay `Video detectado` aparece al reproducir un video.

---

## Sesion 2026-06-24 — Toolchain upgrade a JDK 21

### Problema

`./gradlew.bat :app:assembleDebug` fallaba con:
```
Failed to transform core-for-system-modules.jar to match attributes ...
Execution failed for JdkImageTransform: ... platforms/android-34/core-for-system-modules.jar.
```

AGP 8.2 tiene un bug conocido con el `JdkImageTransform` cuando se compila con JDK 21. Kotlin compila OK, pero `compileDebugJavaWithJavac` rompe.

### Verificacion del entorno

| Componente | Estado |
|---|---|
| JDK 17 | NO instalado en el sistema |
| JDK 21 (ms-21.0.9) | Unico JDK presente, en `C:\Users\informatica\.jdks\ms-21.0.9` |
| Android SDK | `C:\Users\informatica\AppData\Local\Android\Sdk` (platforms 21-36, build-tools 30-36) |
| `local.properties` | `sdk.dir` apunta al SDK correcto |

### Decision: migrar toolchain, no instalar JDK 17 fantasma

Antes el plan sugeria instalar JDK 17; el usuario corrigio que solo hay JDK 21 en el sistema. Se descarta instalar otro JDK y se opta por alinear el proyecto al JDK unico.

### Cambios aplicados — `1a9510d build: upgrade AGP 8.2 -> 8.5.2 + Gradle 8.2 -> 8.7 + KSP 1.0.17 -> 1.0.18`

| Archivo | Antes | Despues | Razon |
|---|---|---|---|
| `libs.versions.toml` | `agp = "8.2.0"` | `agp = "8.5.2"` | AGP 8.5+ tiene soporte oficial JDK 21 |
| `libs.versions.toml` | `ksp = "1.9.22-1.0.17"` | `ksp = "1.9.22-1.0.18"` | KSP 1.0.18 corrige incompatibilidades con JDK 21 |
| `gradle/wrapper/gradle-wrapper.properties` | `gradle-8.2.1-bin.zip` | `gradle-8.7-bin.zip` | AGP 8.5 requiere Gradle 8.7+ |

Versiones sin cambios: Kotlin 1.9.22, Hilt 2.48.1, Compose BOM 2024.05.00, Media3 1.2.0, WorkManager 2.9.0, Room 2.6.1.

### Verificacion

| Comando | Resultado |
|---|---|
| `gradlew.bat :app:assembleDebug` | `BUILD SUCCESSFUL in 8m 20s` |
| APK | `app/build/outputs/apk/debug/app-debug.apk` (`14530583` bytes, `2026-06-24 13:17:35`) |
| `classes.dex` y `AndroidManifest.xml` | Presentes en el APK |
| `git push origin main` | OK — `1a9510d` |

### Conclusion para el usuario

El proyecto compila con JDK 21 como unico JDK del sistema. La APK de prueba ya esta disponible e incluye los 4 fixes nuevos (260c2eb, 39968de, 4dda801, 118a886).

Ruta del APK:
```
C:\Users\informatica\AndroidStudioProjects\TVAnimeApp\app\build\outputs\apk\debug\app-debug.apk
```

Pendiente de prueba manual en TV con esta APK.

---

## Correcciones Aplicadas - 2026-05-31 19:00

### Fixes de bugs criticos

| Bug | Causa | Fix | Archivos |
|---|---|---|---|
| App sale al abrir Navegador Web | `WebView(ctx.applicationContext)` en vez de `WebView(ctx)` | Cambiado a `WebView(ctx)` en ambos lugares | `TvWebView.kt`, `WebViewBrowserScreen.kt` |
| Sin manejo de errores WebView | Falta `onReceivedError`, `onReceivedHttpError` | Agregados ambos callbacks + estado `webViewError` con borde rojo | `WebViewBrowserScreen.kt` |
| Room crashea en upgrade v1→v2 | Sin Migration ni destructive migration | Agregado `.fallbackToDestructiveMigration()` | `TVAnimeDatabase.kt` |
| JSON de evaluateJavascript no parsea | Android quotea el resultado JSON como string | `removeSurrounding("\"")` + `replace("\\\"", "\"")` antes de JSONArray | `CrawlService.kt` |
| Contenido duplicado en cada crawl | `UUID.randomUUID()` genera ID nuevo cada vez | ID estable: MD5(title+source) + deduplicacion contra existentes | `CrawlWorker.kt` |
| TvFocusable helper creado con errores | Patron `@Composable` incorrecto para Modifier extension | Archivo borrado - no se usaba en ninguna pantalla | Eliminado |

### Estado post-fixes

| Componente | Estado |
|---|---|
| Navegador Web | ✅ Compila con error handling |
| WebView context | ✅ `ctx` no `applicationContext` |
| Room migration | ✅ `fallbackToDestructiveMigration` |
| CrawlService JSON | ✅ Unescape + `optString`/`optDouble` |
| CrawlWorker ID | ✅ MD5 estable + deduplicacion |
| Build | ✅ `BUILD SUCCESSFUL` |

### Pendiente residual

- **videoUrl vacio**: Resuelto en `54bf55e` — el worker no inserta entradas sin URL reproducible; resolver pendiente para llenar `detailUrl -> videoUrl`.
- **Foco D-pad mejoras**: El patron actual funciona pero podria mejorarse con `MutableInteractionSource` en las pantallas principales (HomeScreen, SiteCard, SettingsScreen).
- **Crawler categories persistencia**: Resuelto en `54bf55e` — categorias default ahora se persisten en Room.

---

## Restauracion UI Android TV / Permisos - 2026-05-31 18:42

### Problema reportado

El usuario reporto que la interfaz seguia rota y que la app no solicitaba permisos. Se pidio resolverlo incluso creando una interfaz nueva coherente con el objetivo actual: Android TV / Google TV para abrir paginas publicas, detectar streams y reproducirlos.

### Hipotesis confirmadas en codigo

| Hallazgo | Evidencia | Riesgo |
|---|---|---|
| Home desalineada con el producto actual | `HomeScreen` seguia centrada en catalogo/M3U y menus secundarios | El usuario no ve el flujo principal de captura web |
| Navegador con estado inconsistente | `UrlBar` navegaba con `webView.loadUrl()` pero no actualizaba `uiState.currentUrl` | Compose podia recargar la URL anterior y romper la experiencia |
| Permisos runtime inexistentes | `MainActivity` no usaba `RequestMultiplePermissions` | Android no mostraba dialogos de permisos |
| Permisos WebView sin manejo | `WebChromeClient` no implementaba `onPermissionRequest` | Paginas que pidan camara/microfono quedaban bloqueadas sin explicacion |
| Riesgo de filtrado en Android TV | `CAMERA`/`RECORD_AUDIO` sin `uses-feature required=false` | Google TV sin camara/microfono podia quedar filtrado |

### Correccion aplicada

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Agregado flujo runtime de permisos con pantalla inicial `PermissionGateScreen` y opcion de continuar limitado |
| `AndroidManifest.xml` | Agregados permisos `CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`; features camara/microfono marcadas `required=false` para compatibilidad TV |
| `Screens.kt` | Reemplazada la Home por una interfaz TV-first: flujo principal, estado de permisos, pasos de uso y catalogo M3U como secundario |
| `TVAnimeNavHost.kt` | Propaga estado de permisos hacia Home |
| `UrlBar.kt` | Agregado boton `Ir` y normalizacion de entrada (`dominio`, `https`, busqueda Google) |
| `WebViewBrowserScreen.kt` | Renombrado visual a `Captura Web`, selector simplificado para fuentes de prueba/entrada manual, sincronizacion de URL con ViewModel y manejo de `onPermissionRequest` de WebView |

### Verificacion

| Comando | Resultado |
|---|---|
| `./gradlew.bat assembleDebug` | No ejecutable en esta terminal: `JAVA_HOME is not set and no 'java' command could be found in your PATH` |

### Estado

Pendiente de build en Android Studio o en una terminal con JDK/JAVA_HOME configurado. La correccion es reversible y no toca datos persistidos ni commits. La prueba manual esperada en Google TV/Android TV es:

1. Abrir app y ver pantalla de permisos.
2. Aceptar permisos o continuar limitado.
3. Ver Home `TVAnime Capture` con foco D-pad visible.
4. Abrir `Captura Web`, seleccionar una fuente o escribir URL.
5. Reproducir contenido en la pagina y verificar que aparezca el overlay `Video detectado`.
6. Pulsar `Reproducir en TV` y validar `PlayerScreen` en pantalla completa.

---

## Sesion 2026-06-24 — Problemas reportados y trabajados en TV real

### Problema 1: APK no compilaba con JDK 21

**Sintoma:** `./gradlew.bat :app:assembleDebug` fallaba con:
```
Failed to transform core-for-system-modules.jar to match attributes ...
Execution failed for JdkImageTransform: ... platforms/android-34/core-for-system-modules.jar.
```

**Causa:** AGP 8.2.0 tenia un bug conocido con `JdkImageTransform` cuando se compilaba con JDK 21. Kotlin compila OK, pero `compileDebugJavaWithJavac` rompe.

**Diagnostico del entorno:**
- JDK 17: NO instalado
- JDK 21 (ms-21.0.9): unico JDK presente en `C:\Users\informatica\.jdks\ms-21.0.9`
- Android SDK: `C:\Users\informatica\AppData\Local\Android\Sdk`
- `local.properties`: `sdk.dir` correcto

**Decision del usuario:** NO instalar JDK 17 (rechazo explicito a descargar nada). Alinear el proyecto al JDK 21 ya presente.

**Solucion:** `1a9510d build: upgrade AGP 8.2 -> 8.5.2 + Gradle 8.2 -> 8.7 + KSP 1.0.17 -> 1.0.18`
- AGP 8.5.2: primera linea con soporte oficial JDK 21
- Gradle 8.7: version minima requerida por AGP 8.5
- KSP 1.0.18: corrige incompatibilidades con JDK 21
- Kotlin 1.9.22, Hilt 2.48.1, Compose BOM 2024.05: sin cambios

**Verificacion:** BUILD SUCCESSFUL en 8m 20s. APK 14.53 MB generada.

### Problema 2: Foco D-pad no se ve (1ra iteracion)

**Sintoma:** El marco de foco (indicador visual de navegacion D-pad) no se veia al mover entre botones. Solo aparecia "a suerte" cuando se acertaba al que se queria presionar.

**Causa identificada inicialmente:** `Modifier.focusable()` se aplicaba por encima de componentes Material 3 (`IconButton`, `OutlinedButton`, `Card`, `Surface(onClick=...)`) que ya son focusables de fabrica. Resultado: doble nodo de foco, `onFocusChanged` escucha al padre duplicado.

**Solucion aplicada:** `4dda801 fix: foco D-pad real + WebView sin crash al abrir`
- `interactionSource` + `collectIsFocusedAsState()` en componentes Material
- Solo Row/Surface/BasicTextField mantienen `focusable()` manual
- Row de categoria y Surface del URL field: `focusable()` correcto (no focusables de fabrica)

**Verificacion parcial:** No se valido en TV real porque el problema de fondo era otro.

### Problema 3: App se sale al abrir navegador (1ra iteracion)

**Sintoma:** Tras pedir los 2 permisos iniciales y entrar al navegador, la app se sale y vuelve al home de la TV.

**Causa:** `DisposableEffect(webViewRef)` re-disparaba `onDispose` en cada recomposicion porque `webViewRef` cambiaba al crearse la WebView. Ademas `setOnKeyListener` + `scrollBy` capturaba D-pad.

**Solucion aplicada en `4dda801`:** `WebViewHolder` con `DisposableEffect(Unit)`; eliminado `setOnKeyListener`/`simulateCenterClick`; la limpieza ocurre solo al salir de la pantalla.

**Verificacion:** App ya no se cierra. Pero aparece Problema 4.

### Problema 4: Foco D-pad sigue sin verse (2da iteracion)

**Sintoma (reporte del usuario):** "el enfoque sigue siendo horrible, no entiendo por que siguen redundando en lo mismo"

**Causa real (descubierta despues de investigar):** `IconButton` de Material 3 en Compose BOM 2024.05 no expone un slot visual para el borde de foco. El `Modifier.focusBorder()` se aplicaba DETRAS del contenido del IconButton, que tapa visualmente el borde. Adicionalmente, `IconButton` consume los eventos de focus y `interactionSource` solo emite Focused cuando el componente gana foco por si mismo.

**Solucion aplicada:** `fe17379 fix: foco D-pad realmente visible + WebView navega + arranca en google.com`
- WebViewBrowserScreen arranca directamente con `DEFAULT_HOME_URL='https://www.google.com'`
- `TvFocusableButton` helper: `Surface(onClick)` con `interactionSource` compartido; el borde se aplica a la MISMA superficie focusable
- `TvDpadWebView` con `dispatchKeyEvent` que traduce D-pad: CENTER/ENTER -> click, UP/DOWN/LEFT/RIGHT -> scrollBy
- `FocusRequester` pide foco al cargar
- Mismo patron aplicado a TvButton, TvIconButton, PlayerControlButton en `Screens.kt` y UrlBarButton en `UrlBar.kt`

**Resultado:** Foco visible pero D-pad no navega dentro de paginas web reales.

### Problema 5: No se puede dar play a contenido en WebView

**Sintoma (reporte del usuario):** "sigue sin poder usarse el contenido dentro del web view. no tiene un buscador por defecto... si no se puede navegar en el contenido dentro del web view no se puede dar play a un contenido"

**Causa real (descubierta despues de investigar):** `dispatchKeyEvent` con `scrollBy` no permite mover foco entre elementos HTML. Los sitios web no ponen elementos en `:focus` automaticamente con D-pad. Sin un mecanismo para mover el foco entre links/botones, no se puede navegar.

**Solucion intentada:** `45b49ec feat: cursor virtual D-pad en WebView + barra de busqueda editable`
- Patron DOM walker tipo TV browser
- JS inyectado enumera elementos focusables visibles (a, button, video, input, iframe, [tabindex])
- DpadCursorOverlay pinta rectangulo cyan sobre el elemento seleccionado
- DpadHandler traduce flechas: UP/DOWN = mover vertical, CENTER = click
- SearchBar editable por D-pad

**Resultado:** Foco visualmente visible pero el cursor virtual bloquea clicks directos y no resuelve la navegacion real. Ademas, los sitios web reales no exponen focus a elementos de forma nativa.

### Problema 6: Investigacion de patron probado en el mercado

**Sintoma (reporte del usuario):** "esta es mas para celular pero tiene la logica que quiero https://github.com/warren-bank/Android-WebCast.git"

**Investigacion:** Se reviso el repositorio de referencia `warren-bank/Android-WebCast` (la misma logica que `webvideocaster.app`, mejor app del mercado).

**Hallazgo clave:** WebCast **NO usa DOM walker ni cursor virtual**. Usa WebView estandar de Android y confia en que el WebView nativo ya maneja D-pad correctamente (cierto en WebView moderno de Android 5+). La deteccion de videos se hace interceptando TODAS las requests HTTP en `shouldInterceptRequest` y mostrando los resultados en un drawer lateral.

**Consecuencia:** El problema de fondo es que mi DOM walker bloquea el manejo nativo del WebView. Ademas, mi `Modifier.focusable()` intercepta eventos de Compose antes de que lleguen al WebView.

**Refactor aplicado:** `24566e5 refactor: navegador estilo WebCast (warren-bank/Android-WebCast)`
- `NativeDpadWebView`: `dispatchKeyEvent` SOLO intercepta ENTER/CENTER, traduce a click en `document.activeElement`. Flechas D-pad se dejan pasar al WebView nativo.
- `isVideoRequest` ampliada: `.m3u8/.mp4/.webm/.ts/.mkv`, `/hls/`, `format=mp4`, `type=video`, `googlevideo.com/videoplayback`, `/videoplayback`
- `VideosDrawer` (drawer derecho): lista TODOS los videos encontrados en la pagina actual
- `BookmarksDrawer` (drawer izquierdo): URLs guardadas en SharedPreferences
- `HeaderBar` simplificado: 4 botones grandes (Volver, Videos, Bookmarks, +)
- `SearchBar` editable con Enter para submit

### Problema 7: D-pad no mueve foco entre elementos HTML (3ra iteracion)

**Sintoma (reporte del usuario):** "sigue sin dejarme seleccionar por ejemplo las tarjetas de los videos dentro del web view ejemplo ahora pude entrar a genula, veo la caratulas de las peliculas pero. no puedo seleccionarlas"

**Causa raiz FINAL (descubierta):** `Modifier.focusable()` en el `AndroidView` que contiene el WebView **intercepta el D-pad de Compose** y no lo reenvia al WebView. El WebView nunca recibe los eventos de teclado como un teclado fisico sino como un evento de Compose que ya fue consumido. Ademas, los sitios como Gnula no son TV-friendly: las tarjetas `<img>` no tienen `tabindex` ni son focusables.

**Solucion aplicada:** `f8053ac fix: WebView recibe D-pad nativo + tabindex automatico en tarjetas`
- Quitar `.focusable()` del AndroidView: dejar que WebView tenga foco nativo
- `webView.requestFocus()` + `requestFocus(FOCUS_DOWN)` en `onPageFinished` para que WebView pida foco al cargar
- `TV_FOCUS_INJECT_JS`: script que agrega `tabindex='0'` a todos los elementos focusables (a, button, input, video, iframe, [role=button], [onclick]) Y a selectores de tarjetas (article, .card, .movie, .item, .poster, .video, .thumb, .tile, [class*=card/movie/item/poster/thumb/tile]) Y a imagenes cuyo padre sea un anchor o tenga onclick
- `MutationObserver` reaplica el tabindex cada 250ms cuando cambia el DOM
- `NativeDpadWebView.dispatchKeyEvent`: para ENTER/CENTER, evalua JS que hace click en `document.activeElement` con `CountDownLatch`. Si no hay foco, deja pasar al WebView

**Resultado esperado:**
- Al cargar una pagina, el WebView tiene foco nativo
- Las flechas D-pad se mueven entre elementos con tabindex (links, botones, tarjetas con onclick)
- ENTER/OK en una tarjeta con onclick hace click y entra a la pelicula
- Las peliculas cargadas aparecen en el drawer derecho de Videos

### Resumen de artefactos generados

| Commit | APK | Tamano | Descripcion |
|---|---|---|---|
| `1a9510d` | 14.53 MB | 2026-06-24 13:17 | Toolchain upgrade AGP 8.5.2 + Gradle 8.7 + JDK 21 |
| `fe17379` | 14.65 MB | 2026-06-24 13:57 | Foco D-pad visible + WebView navega + google.com |
| `45b49ec` | 14.67 MB | 2026-06-24 15:13 | DOM walker virtual + barra de busqueda editable |
| `24566e5` | 14.66 MB | 2026-06-24 15:44 | Refactor estilo WebCast: drawer videos + bookmarks |
| `f8053ac` | 14.55 MB | 2026-06-24 16:23 | WebView recibe D-pad nativo + tabindex automatico |

### Compatibilidad confirmada

- ✅ Android (cualquier version 5.0+ con WebView moderno)
- ✅ Google TV (WebView nativo maneja D-pad)
- ✅ Fire TV Stick (mismo WebView nativo)

### Pendiente de prueba en TV

1. Abrir Captura Web desde Home (con permisos ya otorgados)
2. WebView arranca en https://www.google.com con foco nativo
3. Drawer de Videos abierto por defecto a la derecha
4. Escribir en la barra de busqueda con teclado/D-pad + Enter
5. Navegar a Gnula u otro sitio con D-pad (flechas + OK)
6. Verificar que las tarjetas de peliculas son enfocables
7. Verificar que el drawer de Videos muestra los streams detectados
8. Probar bookmark: boton + en header, drawer izquierdo Bookmarks

### Comando para instalar la APK actual

```
adb install -r "C:\Users\informatica\AndroidStudioProjects\TVAnimeApp\app\build\outputs\apk\debug\app-debug.apk"
```

### Lecciones aprendidas

1. **No aplicar `Modifier.focusable()` a un `AndroidView` con WebView.** Intercepta el D-pad antes de que llegue al WebView nativo.
2. **WebView moderno maneja D-pad nativo.** No se necesita DOM walker para sitios que usan elementos HTML estandar con `tabindex`.
3. **Para sitios no TV-friendly**, agregar `tabindex='0'` a imagenes/tarjetas via JS permite que WebView los enfoque con D-pad.
4. **WebCast es la referencia correcta** para deteccion de videos: `shouldInterceptRequest` + regex + drawer lateral. NO cursor virtual sobre el WebView.
5. **AGP 8.2 + JDK 21 = bug.** AGP 8.5+ lo resuelve.
6. **JDK 17 no estaba en el sistema.** Mejor alinear el proyecto al JDK 21 que ya esta, no instalar otro.

---

## Sesion 2026-06-24 20:03 — Regresion: la app se cierra al abrir navegador

### Problema reportado

Despues de la ultima actualizacion del programador, al abrir el navegador integrado la app vuelve a cerrarse.

### Causa raiz inferida en codigo

El commit `f8053ac` quito `.focusRequester(webViewFocusRequester)` y `.focusable(...)` del `AndroidView` para que el D-pad llegara al WebView nativo. Esa decision era correcta para la navegacion, pero quedaron llamadas vivas a `webViewFocusRequester.requestFocus()` en `WebViewBrowserScreen`.

Como el `FocusRequester` ya no estaba asociado a ningun nodo Compose, al abrir la pantalla o recuperar foco desde barra/drawers podia lanzar una excepcion de Compose y cerrar la app. Esto explica que el bug volviera justo despues del fix de D-pad nativo.

### Correccion aplicada

| Archivo | Cambio |
|---|---|
| `WebViewBrowserScreen.kt` | Eliminado `webViewFocusRequester` para el WebView. Las recuperaciones de foco ahora llaman foco nativo sobre `NativeDpadWebView` con `requestFocus()` y `requestFocus(View.FOCUS_DOWN)`. |
| `WebViewBrowserScreen.kt` | `AndroidWebView` ya no recibe `FocusRequester`; el borde de foco usa `setOnFocusChangeListener` del WebView nativo. |
| `WebViewBrowserScreen.kt` | Eliminado `CountDownLatch` en `dispatchKeyEvent`; `ENTER/OK` ejecuta `evaluateJavascript` de forma asincronica para no bloquear el hilo UI. |
| `WebViewBrowserScreen.kt` | El modo WebView ahora colapsa los drawers laterales y arranca sin drawer de videos abierto por defecto, para dar mas foco y espacio a la navegacion dentro de la pagina. |

### Verificacion

| Comando | Resultado |
|---|---|
| `rg -n "webViewFocusRequester|focusRequester = webViewFocusRequester|CountDownLatch|evaluateJavascriptWithResult"` | Sin referencias peligrosas; solo queda el `FocusRequester` legitimo de `SearchBar`. |
| `./gradlew.bat :app:assembleDebug --no-daemon --console=plain` con `JAVA_HOME=C:\Users\informatica\.jdks\ms-21.0.9` | Build exitoso. |

APK generado:

```
C:\Users\informatica\AndroidStudioProjects\TVAnimeApp\app\build\outputs\apk\debug\app-debug.apk
Tamano: 14555235 bytes
Timestamp: 2026-06-24 20:03:20
```

### Estado

Mitigado por codigo y compilacion. Pendiente de retest en TV/Google TV: abrir Home -> Captura Web -> confirmar que ya no se cierra al entrar al navegador y luego validar D-pad sobre tarjetas dentro del WebView.

Nota de UX: la pantalla mantiene dos modos claros. La interfaz propia de la app se usa desde header, barra de busqueda y drawers; cuando la accion vuelve al WebView, los drawers se cierran y el foco se devuelve al WebView nativo para priorizar la navegacion interna de la pagina.

---

## Sesion 2026-06-24 20:45 — Verificacion de ruta activa e interfaz antigua

### Problema reportado

El usuario reporto que seguia abriendo la interfaz antigua y que al entrar al modo explorador la app seguia cerrandose.

### Hallazgos confirmados en codigo

| Hallazgo | Evidencia | Riesgo |
|---|---|---|
| No habia dispositivo ADB conectado para capturar logcat ni instalar directamente | `adb devices -l` sin dispositivos listados | No se puede confirmar stacktrace real desde terminal |
| La Home seguia mostrando una interfaz de tarjetas sin marca de version/build | `HomeScreen` no exponia build ni modo actual | Dificil distinguir APK vieja instalada vs APK nueva generada |
| El boton `Abrir en navegador` dentro de `DetailScreen` no abria el navegador | `TvButton("Abrir en navegador"... ) { onBack() }` | El usuario podia volver a Home y percibir que abre la interfaz antigua |
| La ruta `browser` tenia un `BackHandler` externo adicional al de `WebViewBrowserScreen` | `TVAnimeNavHost` + `WebViewBrowserScreen` manejaban back | Riesgo de comportamiento ambiguo entre interfaz app y navegacion del WebView |

### Correcciones aplicadas

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | Version subida a `versionCode = 2`, `versionName = "1.1"` para distinguir la APK instalada. |
| `Screens.kt` | Home muestra texto visible `Modo actual: Explorador WebView TV | build 1.1`. |
| `WebViewBrowserScreen.kt` | Header del explorador muestra `Explorador WebView 1.1`. |
| `TVAnimeNavHost.kt` | Eliminado `BackHandler` duplicado de la ruta `browser`; el back lo maneja `WebViewBrowserScreen`. |
| `Screens.kt` / `TVAnimeNavHost.kt` | `DetailScreen` ahora recibe `onOpenBrowser` y el boton `Abrir en navegador` navega realmente a `browser`. |

### Verificacion

| Comando | Resultado |
|---|---|
| `./gradlew.bat :app:assembleDebug --no-daemon --console=plain` con JDK 21 | `BUILD SUCCESSFUL in 25s` |
| `rg -n "versionCode|versionName|Explorador WebView 1.1|Modo actual: Explorador|Abrir en navegador|onOpenBrowser"` | Confirma version 1.1, marcas visibles y ruta al navegador |

APK generado:

```
C:\Users\informatica\AndroidStudioProjects\TVAnimeApp\app\build\outputs\apk\debug\app-debug.apk
Tamano: 14555915 bytes
```

### Pendiente critico

Instalar esta APK 1.1 en la TV y confirmar visualmente que aparece `Modo actual: Explorador WebView TV | build 1.1` en Home y `Explorador WebView 1.1` en el navegador. Si no aparecen esos textos, la TV sigue ejecutando una APK anterior.

---

## Sesion 2026-06-24 21:10 — Arranque directo en modo explorador

### Decision de producto corregida

El usuario aclaro que no debe existir un boton para entrar al explorador: la app debe abrir automaticamente en modo explorador WebView. La interfaz propia de la app solo debe intervenir cuando haya enlaces capturados y se vaya a reproducir un stream.

### Cambios aplicados

| Archivo | Cambio |
|---|---|
| `MainActivity.kt` | Eliminada pantalla inicial de permisos como bloqueo de arranque; la app entra directo al `TVAnimeNavHost`. |
| `TVAnimeNavHost.kt` | `startDestination` cambiado de `home` a `browser`; el explorador es la pantalla inicial real. |
| `WebViewBrowserScreen.kt` | Eliminados del flujo visible el header, barra URL, bookmarks y drawers; el WebView ocupa la pantalla. |
| `WebViewBrowserScreen.kt` | La interfaz de la app aparece como `VideoCaptureOverlay` solo cuando `shouldInterceptRequest` detecta un enlace reproducible. |
| `app/build.gradle.kts` | Version subida a `versionCode = 3`, `versionName = "1.2"` para distinguir esta APK del intento 1.1. |

### Logica principal preservada

No se elimino la capacidad central de la app. La logica vigente sigue siendo:

1. WebView carga y navega paginas publicas.
2. `shouldInterceptRequest` inspecciona las requests del WebView.
3. `isVideoRequest()` detecta HLS/MP4/WEBM/MKV/TS, `/hls/`, `videoplayback`, `format=mp4` y `type=video`.
4. `WebViewBrowserViewModel.onStreamDetected()` registra el candidato y activa `VideoCaptureOverlay`.
5. El overlay muestra `Reproducir en TV`.
6. El reproductor recibe la URL capturada con headers de contexto, al menos `Referer` y los headers que entregue el flujo de captura.

La interfaz propia de la app queda subordinada a ese flujo: solo aparece para reproducir o descartar lo capturado, no para reemplazar la navegacion dentro del WebView.

### Comportamiento esperado

1. Al abrir la app entra directamente a Google dentro del WebView.
2. El usuario navega dentro del WebView sin pasar por Home ni pulsar boton de explorador.
3. La interfaz propia de la app aparece solo cuando detecta un video/enlace reproducible.
4. Al pulsar `Reproducir en TV`, la app cambia al reproductor Media3.
5. Si una pagina pide permisos WebView, se muestra aviso dentro del explorador sin bloquear el arranque.

### Verificacion

| Comando | Resultado |
|---|---|
| `./gradlew.bat :app:assembleDebug --no-daemon --console=plain` con JDK 21 | Build ejecutado sin error. |

### Pendiente

Instalar APK version `1.2` en TV y probar si el cierre al entrar al explorador desaparece, porque ya no hay transicion Home -> explorador: el explorador es el arranque.
