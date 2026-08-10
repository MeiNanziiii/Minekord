package ua.bonfiremc.minekord.util

class PlaceholderBuilder<T> {
    val map: MutableMap<String, T> = mutableMapOf()

    infix fun String.to(component: T) {
        map[this] = component
    }
}