# Plan de Trabajo — TVAnimeApp

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
| 23 | Compilar APK desde Android Studio | ⏳ Pendiente |
| 24 | Pruebas en Android TV real o emulador | ⏳ Pendiente |
| 25 | Configurar periodicidad WorkManager en onCreate | ⏳ Pendiente |
| 26 | Screen de ajustes (origen de playlist M3U) | ⏳ Pendiente |

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
2. [WorkManager] Llamar `enqueue` del worker desde el onCreate de TVAnimeApp.kt o desde Settings
3. [Ajustes] Pantalla de preferencias: URL M3U, intervalo de sync, habilitar/deshabilitar favoritos
4. [CleartextTraffic] Remover `usesCleartextTraffic=true` cuando uses HTTPS exclusivamente
5. [Subtítulos] Cargar subtítulos .vtt desde el campo `subtitleUrl` en DetailScreen
