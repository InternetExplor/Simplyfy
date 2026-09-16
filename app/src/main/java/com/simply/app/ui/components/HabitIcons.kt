package com.simply.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.NoDrinks
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Набор значков для привычек — только векторные иконки, без эмодзи.
 * Порядок ключей = порядок в сетке выбора: сначала здоровье и режим,
 * потом спорт, учёба и творчество, потом дом и остальное.
 */
object HabitIcons {

    val keys: List<String> = listOf(
        // здоровье и режим
        "water", "coffee", "food", "pills", "heart", "care",
        "nodrinks", "nosmoke", "sleep", "night", "alarm", "sun",
        // спорт
        "run", "walk", "bike", "gym", "swim", "hike",
        "football", "basketball", "tennis", "martial", "yoga", "spa",
        // голова и творчество
        "book", "write", "study", "language", "code", "idea",
        "art", "paint", "music", "headphones", "piano", "photo",
        // дом и жизнь
        "nature", "garden", "pets", "clean", "laundry", "shopping",
        "money", "call", "friends", "help", "movie", "games",
        "plan", "timer", "spark"
    )

    /** Что показываем в свёрнутом виде — самые ходовые привычки. */
    val common: List<String> = listOf("water", "run", "book", "yoga", "spark")

    fun vector(key: String): ImageVector = when (key) {
        "water" -> Icons.Rounded.WaterDrop
        "coffee" -> Icons.Rounded.LocalCafe
        "food" -> Icons.Rounded.Restaurant
        "pills" -> Icons.Rounded.Medication
        "heart" -> Icons.Rounded.MonitorHeart
        "care" -> Icons.Rounded.Favorite
        "nodrinks" -> Icons.Rounded.NoDrinks
        "nosmoke" -> Icons.Rounded.SmokeFree
        "sleep" -> Icons.Rounded.Bedtime
        "night" -> Icons.Rounded.Nightlight
        "alarm" -> Icons.Rounded.Alarm
        "sun" -> Icons.Rounded.WbSunny
        "run" -> Icons.Rounded.DirectionsRun
        "walk" -> Icons.Rounded.DirectionsWalk
        "bike" -> Icons.Rounded.DirectionsBike
        "gym" -> Icons.Rounded.FitnessCenter
        "swim" -> Icons.Rounded.Pool
        "hike" -> Icons.Rounded.Hiking
        "football" -> Icons.Rounded.SportsSoccer
        "basketball" -> Icons.Rounded.SportsBasketball
        "tennis" -> Icons.Rounded.SportsTennis
        "martial" -> Icons.Rounded.SportsMartialArts
        "yoga" -> Icons.Rounded.SelfImprovement
        "spa" -> Icons.Rounded.Spa
        "book" -> Icons.Rounded.MenuBook
        "write" -> Icons.Rounded.EditNote
        "study" -> Icons.Rounded.School
        "language" -> Icons.Rounded.Language
        "code" -> Icons.Rounded.Code
        "idea" -> Icons.Rounded.Lightbulb
        "art" -> Icons.Rounded.Brush
        "paint" -> Icons.Rounded.Palette
        "music" -> Icons.Rounded.MusicNote
        "headphones" -> Icons.Rounded.Headphones
        "piano" -> Icons.Rounded.Piano
        "photo" -> Icons.Rounded.CameraAlt
        "nature" -> Icons.Rounded.Park
        "garden" -> Icons.Rounded.Yard
        "pets" -> Icons.Rounded.Pets
        "clean" -> Icons.Rounded.CleaningServices
        "laundry" -> Icons.Rounded.LocalLaundryService
        "shopping" -> Icons.Rounded.ShoppingBasket
        "money" -> Icons.Rounded.Savings
        "call" -> Icons.Rounded.Phone
        "friends" -> Icons.Rounded.Groups
        "help" -> Icons.Rounded.VolunteerActivism
        "movie" -> Icons.Rounded.Movie
        "games" -> Icons.Rounded.SportsEsports
        "plan" -> Icons.Rounded.Checklist
        "timer" -> Icons.Rounded.Timer
        else -> Icons.Rounded.AutoAwesome
    }

    /**
     * Перенос значков из старых версий данных, где хранились эмодзи.
     * Символы записаны escape-кодами, чтобы в исходниках не было эмодзи.
     */
    fun fromLegacyEmoji(raw: String): String = when (raw.replace("\uFE0F", "")) {
        "\uD83D\uDCA7" -> "water"
        "\uD83D\uDCD6" -> "book"
        "\uD83C\uDFC3" -> "run"
        "\uD83E\uDDD8" -> "yoga"
        "\uD83E\uDD57" -> "food"
        "\uD83D\uDE34" -> "sleep"
        "\u270D" -> "write"
        "\uD83C\uDFA7" -> "music"
        "\uD83C\uDF3F" -> "nature"
        "\uD83C\uDFCB" -> "gym"
        "\u2600" -> "sun"
        "\u2615" -> "coffee"
        else -> "spark"
    }
}
