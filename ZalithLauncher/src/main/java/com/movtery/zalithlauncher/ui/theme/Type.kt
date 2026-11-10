/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.movtery.zalithlauncher.R

/**
 * HX Launcher: fuente global de la interfaz.
 * Para cambiarla, pon tus .ttf/.otf (nombres en minúscula) en res/font y edita esta lista.
 * Inter se distribuye bajo SIL OFL 1.1 (ver assets/fonts/INTER_LICENSE.txt).
 */
val HxFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun TextStyle.hx() = copy(fontFamily = HxFontFamily)

private val BaseTypography = Typography()

val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.hx(),
    displayMedium = BaseTypography.displayMedium.hx(),
    displaySmall = BaseTypography.displaySmall.hx(),
    headlineLarge = BaseTypography.headlineLarge.hx(),
    headlineMedium = BaseTypography.headlineMedium.hx(),
    headlineSmall = BaseTypography.headlineSmall.hx(),
    titleLarge = BaseTypography.titleLarge.hx(),
    titleMedium = BaseTypography.titleMedium.hx(),
    titleSmall = BaseTypography.titleSmall.hx(),
    bodyLarge = BaseTypography.bodyLarge.hx(),
    bodyMedium = BaseTypography.bodyMedium.hx(),
    bodySmall = BaseTypography.bodySmall.hx(),
    labelLarge = BaseTypography.labelLarge.hx(),
    labelMedium = BaseTypography.labelMedium.hx(),
    labelSmall = BaseTypography.labelSmall.hx(),
)
