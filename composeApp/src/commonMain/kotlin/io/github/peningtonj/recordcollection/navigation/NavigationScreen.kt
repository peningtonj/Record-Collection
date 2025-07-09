// commonMain/navigation/Screen.kt
package io.github.peningtonj.recordcollection.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Profile : Screen("profile")
    data object Library : Screen("library")
    data class Album(val albumId: String) : Screen("album/$albumId")
    data class Collection(val collectionName: String) : Screen("collection/$collectionName")

    companion object {
        fun fromRoute(route: String): Screen = when(route) {
            Login.route -> Login
            Profile.route -> Profile
            Library.route -> Library
            else -> throw IllegalArgumentException("Unknown route: $route")
        }
    }
}