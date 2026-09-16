package com.simply.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/** Значки проектов — только векторные иконки. */
object ProjectIcons {

    val keys: List<String> = listOf(
        "folder", "inbox", "work", "home", "study", "sport",
        "money", "cart", "travel", "idea", "code", "art",
        "health", "family", "pets", "car", "build", "garden",
        "music", "book", "photo", "food", "star", "rocket"
    )

    /** Что показываем в свёрнутом виде. */
    val common: List<String> = listOf("folder", "work", "home", "study", "star")

    fun vector(key: String): ImageVector = when (key) {
        "inbox" -> Icons.Rounded.Inbox
        "work" -> Icons.Rounded.Work
        "home" -> Icons.Rounded.Home
        "study" -> Icons.Rounded.School
        "sport" -> Icons.Rounded.FitnessCenter
        "money" -> Icons.Rounded.AccountBalanceWallet
        "cart" -> Icons.Rounded.ShoppingCart
        "travel" -> Icons.Rounded.Flight
        "idea" -> Icons.Rounded.Lightbulb
        "code" -> Icons.Rounded.Code
        "art" -> Icons.Rounded.Brush
        "health" -> Icons.Rounded.MonitorHeart
        "family" -> Icons.Rounded.Groups
        "pets" -> Icons.Rounded.Pets
        "car" -> Icons.Rounded.DirectionsCar
        "build" -> Icons.Rounded.Handyman
        "garden" -> Icons.Rounded.Yard
        "music" -> Icons.Rounded.MusicNote
        "book" -> Icons.Rounded.MenuBook
        "photo" -> Icons.Rounded.CameraAlt
        "food" -> Icons.Rounded.Restaurant
        "star" -> Icons.Rounded.Star
        "rocket" -> Icons.Rounded.RocketLaunch
        else -> Icons.Rounded.FolderOpen
    }
}
