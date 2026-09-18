package com.nuvio.app.features.simkl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.simkl_logo_glyph
import nuvio.composeapp.generated.resources.simkl_logo_wordmark
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun simklBrandPainter(asset: SimklBrandAsset): Painter =
    painterResource(
        when (asset) {
            SimklBrandAsset.Glyph -> Res.drawable.simkl_logo_glyph
            SimklBrandAsset.Wordmark -> Res.drawable.simkl_logo_wordmark
        },
    )
