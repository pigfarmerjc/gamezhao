package com.example.worddefense.data

import java.util.UUID

data class General(
    val name: String,
    val characters: List<String>,
    val skillName: String,
    val skillDesc: String,
    val baseDamage: Int,
    val baseRange: Float, // in grid cells
    val baseAtkSpeed: Float // attacks per second
)

data class Tower(
    val id: String = UUID.randomUUID().toString(),
    val char: String,
    val gridX: Int,
    val gridY: Int,
    val star: Int = 1,
    val lastAttackTime: Long = 0L,
    val damage: Int,
    val range: Float,
    val attackIntervalMs: Long
)

data class Enemy(
    val id: String = UUID.randomUUID().toString(),
    val char: String,
    val pathId: Int, // 1 or 2
    var pathProgress: Float = 0f, // 0.0 to 1.0
    var currentHp: Int,
    val maxHp: Int,
    val speed: Float, // increment per tick
    var x: Float = 0f,
    var y: Float = 0f,
    val reward: Int,
    val isBoss: Boolean = false
)

data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    var currentX: Float,
    var currentY: Float,
    val targetEnemyId: String,
    val damage: Int,
    val speed: Float, // speed per tick
    val type: String // "arrow", "slash", "lightning"
)

enum class GameScreenState {
    LINEUP_SELECTION,
    BATTLE,
    GAME_OVER
}
