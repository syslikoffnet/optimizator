package com.duchamp.nav

/** Navigation destinations for the bottom bar. */
enum class DuchampDestination(
    val route: String,
    val label: String,
) {
    GAME_MODE("gamemode", "Игра"),
    MONITOR("monitor", "Панель"),
    SESSIONS("sessions", "Сеансы"),
    HEALTH("health", "Батарея"),
    SYSTEM("system", "Система"),
    NETWORK("network", "Сеть"),
}

val destinations = DuchampDestination.entries
