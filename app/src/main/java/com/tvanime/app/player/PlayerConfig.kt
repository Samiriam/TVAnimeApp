package com.tvanime.app.player

import android.content.Context
import androidx.media3.common.Tracks
import androidx.media3.ui.DefaultTrackSelector

/**
 * Wrapper de configuración para Media3 / ExoPlayer.
 *
 * Responsable de preparar el track selector,
 * manejar subtítulos y controlar la pausa/reanudación.
 */
object PlayerConfig {

    fun buildTrackSelector(context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            // Prioriza calidad alta, luego adapta al ancho de banda disponible
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setForceHighestSupportedBitrate(false)
                    .build()
            )
        }
    }
}
