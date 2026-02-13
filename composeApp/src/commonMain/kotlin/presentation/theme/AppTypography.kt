package presentation.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.platform.Font

val InterFontFamily = FontFamily(
    Font(resource = "font/InterDisplay-Thin.otf", weight = FontWeight.Thin, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-ThinItalic.otf", weight = FontWeight.Thin, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-ExtraLight.otf", weight = FontWeight.ExtraLight, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-ExtraLightItalic.otf", weight = FontWeight.ExtraLight, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-Light.otf", weight = FontWeight.Light, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-LightItalic.otf", weight = FontWeight.Light, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-Regular.otf", weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-Italic.otf", weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-Medium.otf", weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-MediumItalic.otf", weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-SemiBold.otf", weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-SemiBoldItalic.otf", weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-Bold.otf", weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-BoldItalic.otf", weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-ExtraBold.otf", weight = FontWeight.ExtraBold, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-ExtraBoldItalic.otf", weight = FontWeight.ExtraBold, style = FontStyle.Italic),
    Font(resource = "font/InterDisplay-Black.otf", weight = FontWeight.Black, style = FontStyle.Normal),
    Font(resource = "font/InterDisplay-BlackItalic.otf", weight = FontWeight.Black, style = FontStyle.Italic),
)

val AppTypography = Typography(
    defaultFontFamily = InterFontFamily
) 