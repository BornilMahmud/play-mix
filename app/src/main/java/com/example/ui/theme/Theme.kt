package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AppLanguage

enum class PlayMixThemeMode {
    AMOLED_BLACK,
    DARK_CHARCOAL,
    LIGHT,
    SYSTEM
}

data class PlayMixStrings(
    val navHome: String,
    val navLibrary: String,
    val navOnline: String,
    val navEqualizer: String,
    val navSettings: String,
    val homeGreetingMorning: String,
    val homeGreetingAfternoon: String,
    val homeGreetingEvening: String,
    val homeGreetingNight: String,
    val playAll: String,
    val shuffleMix: String,
    val smartMixes: String,
    val albums: String,
    val artists: String,
    val playlists: String,
    val yourPlaylists: String,
    val quickPlayTracks: String,
    val nowPlaying: String,
    val onlineDiscoveryTitle: String,
    val onlineDiscover: String,
    val onlineTrending: String,
    val onlineNewReleases: String,
    val onlineProviders: String,
    val settingsTitle: String,
    val languageLabel: String,
    val backupAndRestore: String
)

val EnglishPlayMixStrings = PlayMixStrings(
    navHome = "Home",
    navLibrary = "Library",
    navOnline = "Online",
    navEqualizer = "Equalizer",
    navSettings = "Settings",
    homeGreetingMorning = "Good Morning",
    homeGreetingAfternoon = "Good Afternoon",
    homeGreetingEvening = "Good Evening",
    homeGreetingNight = "Late Night Session",
    playAll = "Play All",
    shuffleMix = "Shuffle Mix",
    smartMixes = "Smart Mixes",
    albums = "Albums",
    artists = "Artists",
    playlists = "Your Playlists",
    yourPlaylists = "Your Playlists",
    quickPlayTracks = "Quick Play Tracks",
    nowPlaying = "NOW PLAYING",
    onlineDiscoveryTitle = "Online Music Discovery",
    onlineDiscover = "Discover",
    onlineTrending = "Trending",
    onlineNewReleases = "New Releases",
    onlineProviders = "Providers",
    settingsTitle = "Settings & Engine",
    languageLabel = "App Language",
    backupAndRestore = "Library Backup & Restore"
)

val BengaliPlayMixStrings = PlayMixStrings(
    navHome = "হোম",
    navLibrary = "লাইব্রেরি",
    navOnline = "অনলাইন",
    navEqualizer = "ইকুয়ালাইজার",
    navSettings = "সেটিংস",
    homeGreetingMorning = "শুভ সকাল",
    homeGreetingAfternoon = "শুভ অপরাহ্ন",
    homeGreetingEvening = "শুভ সন্ধ্যা",
    homeGreetingNight = "রাতের মিউজিক সেশন",
    playAll = "সব চালান",
    shuffleMix = "শাফল মিক্স",
    smartMixes = "স্মার্ট মিক্স",
    albums = "অ্যালবাম",
    artists = "শিল্পী",
    playlists = "প্লেলিস্ট",
    yourPlaylists = "আপনার প্লেলিস্ট",
    quickPlayTracks = "দ্রুত গান চালান",
    nowPlaying = "এখন চলছে",
    onlineDiscoveryTitle = "অনলাইন মিউজিক ডিসকভারি",
    onlineDiscover = "ডিসকভার",
    onlineTrending = "ট্রেন্ডিং",
    onlineNewReleases = "নতুন রিলিজ",
    onlineProviders = "প্রোভাইডার",
    settingsTitle = "সেটিংস ও ইঞ্জিন",
    languageLabel = "অ্যাপের ভাষা",
    backupAndRestore = "ব্যাকআপ এবং রিস্টোর"
)

val LocalPlayMixStrings = staticCompositionLocalOf { EnglishPlayMixStrings }

val PlayMixShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun PlayMixTheme(
    themeMode: PlayMixThemeMode = PlayMixThemeMode.AMOLED_BLACK,
    dynamicColor: Boolean = false,
    accentColor: AccentColorOption = AccentColorOption.ELECTRIC_VIOLET,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        PlayMixThemeMode.AMOLED_BLACK -> true
        PlayMixThemeMode.DARK_CHARCOAL -> true
        PlayMixThemeMode.LIGHT -> false
        PlayMixThemeMode.SYSTEM -> systemDark
    }

    val primaryColor = Color(accentColor.primaryHex)
    val secondaryColor = Color(accentColor.secondaryHex)

    val amoledScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = secondaryColor,
        onSecondary = Color.Black,
        secondaryContainer = DeepBlueContainer,
        onSecondaryContainer = SoftCyanLight,
        tertiary = DeepBlueLight,
        onTertiary = Color.White,
        tertiaryContainer = DeepBlue,
        onTertiaryContainer = TextWhite,
        background = AmoledBlack,
        onBackground = TextWhite,
        surface = AmoledSurface,
        onSurface = TextWhite,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = TextMutedGrey,
        surfaceContainer = AmoledSurface,
        surfaceContainerHigh = AmoledSurfaceElevated,
        outline = DividerDark,
        error = ErrorCrimson
    )

    val charcoalScheme = amoledScheme.copy(
        background = CharcoalBackground,
        surface = CharcoalSurface,
        surfaceVariant = CharcoalSurfaceVariant,
        surfaceContainer = CharcoalSurface,
        surfaceContainerHigh = CharcoalSurfaceElevated
    )

    val lightScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.16f),
        onPrimaryContainer = Color(0xFF1A1033),
        secondary = DeepBlue,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD8E2FF),
        onSecondaryContainer = Color(0xFF001A41),
        tertiary = Color(0xFF006874),
        onTertiary = Color.White,
        background = LightBackground,
        onBackground = Color(0xFF14141F),
        surface = LightSurface,
        onSurface = Color(0xFF14141F),
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = Color(0xFF49454F),
        error = ErrorCrimson
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) {
                val dyn = dynamicDarkColorScheme(context)
                if (themeMode == PlayMixThemeMode.AMOLED_BLACK) {
                    dyn.copy(
                        background = AmoledBlack,
                        surface = AmoledSurface,
                        surfaceContainer = AmoledSurface
                    )
                } else dyn
            } else {
                dynamicLightColorScheme(context)
            }
        }
        themeMode == PlayMixThemeMode.AMOLED_BLACK -> amoledScheme
        themeMode == PlayMixThemeMode.DARK_CHARCOAL -> charcoalScheme
        themeMode == PlayMixThemeMode.LIGHT -> lightScheme
        else -> if (systemDark) amoledScheme else lightScheme
    }

    val strings = when (appLanguage) {
        AppLanguage.ENGLISH -> EnglishPlayMixStrings
        AppLanguage.BENGALI -> BengaliPlayMixStrings
    }

    CompositionLocalProvider(LocalPlayMixStrings provides strings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PlayMixTypography,
            shapes = PlayMixShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PlayMixTheme(
        themeMode = if (darkTheme) PlayMixThemeMode.AMOLED_BLACK else PlayMixThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
