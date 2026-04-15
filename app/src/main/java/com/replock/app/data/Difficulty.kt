package com.replock.app.data

data class Difficulty(
    val name: String,
    val targetReps: Int,
    val strictness: Float
) {
    companion object {
        val EASY   = Difficulty("Easy",   5,  0.3f)
        val MEDIUM = Difficulty("Medium", 10, 0.6f)
        val HARD   = Difficulty("Hard",   20, 1.0f)

        val ALL = listOf(EASY, MEDIUM, HARD)

        fun fromName(name: String): Difficulty =
            ALL.firstOrNull { it.name == name } ?: MEDIUM
    }
}