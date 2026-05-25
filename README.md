# TVAnimeApp — Proyecto Android TV

App para Android TV orientada a **detectar, captar y reproducir enlaces públicos de video y audio** a partir de la URL de una pagina ingresada por el usuario, desarrollada en **Kotlin + Jetpack Compose + Media3**.

## Objetivo del producto

El centro de la app ya no debe ser solo un catálogo o una playlist M3U fija.

La dirección del proyecto es:

1. Recibir una URL pública ingresada por el usuario.
2. Analizar esa página para localizar recursos multimedia reproducibles.
3. Presentar esos enlaces encontrados de forma clara en TV.
4. Reproducir video o audio directamente dentro de la app.

## Estado actual

La base actual del repositorio ya resuelve la parte de reproducción en Android TV, pero hoy está más enfocada en **importar listas M3U** y mostrarlas como catálogo. Ese flujo servirá como base técnica, pero deberá evolucionar para que la extracción desde páginas públicas sea el caso principal de uso.

## Índice de carpetas

```
TVAnimeApp/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/tvanime/app/
│   │       │   ├── TVAnimeApp.kt           ← Hilt Application
│   │       │   ├── presentation/
│   │       │   │   └── MainActivity.kt     ← Actividad principal
│   │       │   ├── domain/
│   │       │   │   ├── model/ContentItem.kt
│   │       │   │   └── usecase/             ← Casos de uso
│   │       │   ├── data/
│   │       │   │   ├── local/
│   │       │   │   │   ├── database/TVAnimeDatabase.kt
│   │       │   │   │   ├── entity/           ← Entity de Room
│   │       │   │   │   ├── dao/              ← DAOs de Room
│   │       │   │   │   └── entity/DomainMapper.kt
│   │       │   │   ├── remote/
│   │       │   │   │   ├── api/SourceApi.kt
│   │       │   │   │   └── interceptor/UserAgentInterceptor.kt
│   │       │   │   ├── parser/M3uPlaylistParser.kt
│   │       │   │   └── repository/ContentsRepository.kt
│   │       │   ├── ui/
│   │       │   │   ├── screens/              ← Home, Detail, Player
│   │       │   │   ├── components/            ← ContentCard, CategoryRow
│   │       │   │   ├── navigation/TVAnimeNavHost.kt
│   │       │   │   └── theme/Theme.kt
│   │       │   ├── player/PlayerConfig.kt    ← Config Media3
│   │       │   └── di/AppModule.kt           ← Módulos Hilt
│   │       ├── res/                          ← Recursos gráficos
│   │       ├── assets/playlist_demo.m3u      ← Lista demo para pruebas
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── libs.versions.toml                        ← Catálogo de versiones
```

## Cómo ejecutar

1. Abrir Android Studio → **Open** → seleccionar carpeta `TVAnimeApp`
2. Sincronizar Gradle (botón *Sync Now*)
3. Elegir emulador **Android TV** (API 26+) y pulsar ▶ Run

## Estructura de fuente

| Capa | Responsabilidad |
|---|---|
| `data/local` | Room (catálogo, historial, favoritos) |
| `data/remote` | Retrofit + interceptores |
| `data/parser` | M3uPlaylistParser — parsea listas M3U |
| `data/repository` | TrafficRules: mezcla fuente remota + Room |
| `domain` | `ContentItem` + casos de uso puros |
| `ui` | Pantallas Compose TV-first / navegación D-pad |
| `player` | Configuración de Media3/ExoPlayer |
| `di` | Módulos Hilt inyectables |

## Fuentes de video

Las fuentes se importan mediante **listas M3U** (archivo `.m3u` local o URL):

### Formato M3U esperado

```m3u
#EXTM3U
#EXTINF:-1 group-title="Anime",My Anime
https://mi-servidor.com/stream.m3u8
```

Puedes colocar tu playlist en:

- `app/src/main/assets/playlist.m3u` (local)
- `app/src/main/res/raw/playlist.m3u` (recursos empaquetados)
- Una URL accesible por HTTP.

### Agregar una fuente

1. Agrega la URL o el archivo M3U.
2. Completa `ContentConfig` con sus datos.
3. Ajusta `ContentConfig.m3uUrl` o `ContentConfig.m3uAsset`.
4. El parser completa el catálogo.

## Dependencias principales

| Librería | Uso |
|---|---|
| `androidx.tv:tv-material` | UI TV-first, tarjetas con foco |
| `androidx.media3` | Reproductor HLS/DASH/MP4 |
| `androidx.room` | Base de datos local |
| `retrofit2` | Consumo de APIs/feeds |
| `dagger.hilt` | Inyección de dependencias |
| `coil-compose` | Carga de imágenes en Compose |
