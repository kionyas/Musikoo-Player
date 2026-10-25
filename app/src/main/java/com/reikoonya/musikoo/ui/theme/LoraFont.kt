package com.reikoonya.musikoo.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.reikoonya.musikoo.R

/**
 * Font for the "Lora (adaptive)" option.
 */
val LoraFamily: FontFamily = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.lora_semibold, FontWeight.SemiBold),
    Font(R.font.lora_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.lora_bold, FontWeight.Bold),
    Font(R.font.lora_bolditalic, FontWeight.Bold, FontStyle.Italic),
)
