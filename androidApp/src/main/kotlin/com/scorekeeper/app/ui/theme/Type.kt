package com.scorekeeper.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scorekeeper.app.R

/**
 * Matches the wireframes' two-typeface system: Fraunces (serif display, for
 * headings/titles) over Work Sans (sans body, everything else). Both are
 * bundled as static-weight OFL fonts under res/font rather than loaded from
 * Google Fonts at runtime, so the app looks right offline and on first launch.
 */
val FrauncesFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold)
)

val WorkSansFamily = FontFamily(
    Font(R.font.work_sans_regular, FontWeight.Normal),
    Font(R.font.work_sans_medium, FontWeight.Medium),
    Font(R.font.work_sans_semibold, FontWeight.SemiBold),
    Font(R.font.work_sans_bold, FontWeight.Bold)
)

val ScoreKeeperTypography = Typography(
    displayLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = WorkSansFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
