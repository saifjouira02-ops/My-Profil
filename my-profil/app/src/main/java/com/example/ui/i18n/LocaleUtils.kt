package com.example.ui.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.R
import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val nativeLabelRes: Int,
    val isRtl: Boolean,
)

object AppLanguages {
    val supported = listOf(
        SupportedLanguage(code = "ar", nativeLabelRes = R.string.language_arabic, isRtl = true),
        SupportedLanguage(code = "en", nativeLabelRes = R.string.language_english, isRtl = false),
    )

    fun isRtl(languageCode: String): Boolean = languageCode == "ar"
}

@Composable
fun LocalizedAppWrapper(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val locale = remember(languageCode) { Locale.forLanguageTag(languageCode) }
    val localizedConfig = remember(baseContext, locale) {
        Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localizedContext: Context = remember(baseContext, localizedConfig) {
        baseContext.createConfigurationContext(localizedConfig)
    }
    val layoutDirection = if (AppLanguages.isRtl(languageCode)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        LocalLayoutDirection provides layoutDirection,
    ) {
        content()
    }
}

object LocaleFormatters {
    fun formatNumber(value: Long, languageCode: String): String {
        val locale = Locale.forLanguageTag(languageCode)
        return NumberFormat.getIntegerInstance(locale).format(value)
    }

    fun formatTimestamp(timestamp: Timestamp?, languageCode: String, justNowLabel: String): String {
        val date = timestamp?.toDate() ?: return justNowLabel
        val diffMillis = System.currentTimeMillis() - date.time
        if (diffMillis in 0..59_999L) {
            return justNowLabel
        }
        val locale = Locale.forLanguageTag(languageCode)
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        return formatter.format(Date(date.time))
    }
}
