package com.example.worddefense.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worddefense.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

class GameViewModel : ViewModel() {

    // Define Paths
    val path1Points = listOf(
        0f to 8f,
        0f to 4f,
        4f to 4f,
        4f to 6f,
        6f to 6f,
        6f to 8f
    )
    val path2Points = listOf(
        6f to 0f,
        6f to 4f,
        2f to 4f,
        2f to 2f,
        0f to 2f,
        0f to 0f
    )

    // Grid details
    val colsCount = 7
    val rowsCount = 9

    // Check if cell is path cell
    fun isPathCell(x: Int, y: Int): Boolean {
        // Simple bounding box checks or cell checks for coordinate line segments
        // Segment 1: (0,8) -> (0,4)
        if (x == 0 && y in 4..8) return true
        // Segment 2: (0,4) -> (4,4)
        if (y == 4 && x in 0..4) return true
        // Segment 3: (4,4) -> (4,6)
        if (x == 4 && y in 4..6) return true
        // Segment 4: (4,6) -> (6,6)
        if (y == 6 && x in 4..6) return true
        // Segment 5: (6,6) -> (6,8)
        if (x == 6 && y in 6..8) return true

        // Path 2 segments
        // Segment 1: (6,0) -> (6,4)
        if (x == 6 && y in 0..4) return true
        // Segment 2: (6,4) -> (2,4)
        if (y == 4 && x in 2..6) return true
        // Segment 3: (2,4) -> (2,2)
        if (x == 2 && y in 2..4) return true
        // Segment 4: (2,2) -> (0,2)
        if (y == 2 && x in 0..2) return true
        // Segment 5: (0,2) -> (0,0)
        if (x == 0 && y in 0..2) return true

        return false
    }

    // Check if cell is a base cell
    fun isBaseCell(x: Int, y: Int): Boolean {
        return (x == 0 && y == 0) || (x == 6 && y == 8)
    }

    // Check if cell is a spawn cell
    fun isSpawnCell(x: Int, y: Int): Boolean {
        return (x == 0 && y == 8) || (x == 6 && y == 0)
    }

    // Check if cell is buildable plot (white square in screen)
    fun isBuildablePlot(x: Int, y: Int): Boolean {
        if (isPathCell(x, y) || isBaseCell(x, y) || isSpawnCell(x, y)) return false
        // Let's place buildable zones:
        // Area 1: rows 1-3, cols 3-5
        if (x in 3..5 && y in 1..3) return true
        // Area 2: rows 5-7, cols 1-3
        if (x in 1..3 && y in 5..7) return true
        return false
    }

    // Game state streams
    private val _screenState = MutableStateFlow(GameScreenState.LINEUP_SELECTION)
    val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

    private val _selectedGenerals = MutableStateFlow<Set<General>>(emptySet())
    val selectedGenerals: StateFlow<Set<General>> = _selectedGenerals.asStateFlow()

    private val _towers = MutableStateFlow<List<Tower>>(emptyList())
    val towers: StateFlow<List<Tower>> = _towers.asStateFlow()

    private val _enemies = MutableStateFlow<List<Enemy>>(emptyList())
    val enemies: StateFlow<List<Enemy>> = _enemies.asStateFlow()

    private val _projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val projectiles: StateFlow<List<Projectile>> = _projectiles.asStateFlow()

    private val _shopQueue = MutableStateFlow<List<String?>>(listOf(null, null, null, null, null))
    val shopQueue: StateFlow<List<String?>> = _shopQueue.asStateFlow()

    private val _peaches = MutableStateFlow(30) // gold representation
    val peaches: StateFlow<Int> = _peaches.asStateFlow()

    private val _baseHp1 = MutableStateFlow(6) // A-Dou 1
    val baseHp1: StateFlow<Int> = _baseHp1.asStateFlow()

    private val _baseHp2 = MutableStateFlow(6) // A-Dou 2
    val baseHp2: StateFlow<Int> = _baseHp2.asStateFlow()

    private val _waveNumber = MutableStateFlow(1)
    val waveNumber: StateFlow<Int> = _waveNumber.asStateFlow()

    private val _waveInProgress = MutableStateFlow(false)
    val waveInProgress: StateFlow<Boolean> = _waveInProgress.asStateFlow()

    private val _hasWon = MutableStateFlow(false)
    val hasWon: StateFlow<Boolean> = _hasWon.asStateFlow()

    // UI selections
    private val _selectedHandIndex = MutableStateFlow<Int?>(null)
    val selectedHandIndex: StateFlow<Int?> = _selectedHandIndex.asStateFlow()

    private val _selectedMapTowerId = MutableStateFlow<String?>(null)
    val selectedMapTowerId: StateFlow<String?> = _selectedMapTowerId.asStateFlow()

    private val _activeSkill = MutableStateFlow<String?>(null) // "shovel", "rockets", "upgrade", "health", "speed"
    val activeSkill: StateFlow<String?> = _activeSkill.asStateFlow()

    private val _targetGeneral = MutableStateFlow<String?>(null) // Destiny Lock general name
    val targetGeneral: StateFlow<String?> = _targetGeneral.asStateFlow()

    private val _peachCount = MutableStateFlow(10) // Peach items count (to restore base HP)
    val peachCount: StateFlow<Int> = _peachCount.asStateFlow()

    private var gameLoopJob: Job? = null
    private var spawnQueue = mutableListOf<Enemy>()
    private var nextSpawnTime = 0L

    fun toggleGeneralSelection(general: General) {
        val current = _selectedGenerals.value
        if (current.contains(general)) {
            _selectedGenerals.value = current - general
        } else if (current.size < 5) {
            _selectedGenerals.value = current + general
        }
    }

    fun startBattle() {
        if (_selectedGenerals.value.size < 5) return // Must select 5 generals
        _screenState.value = GameScreenState.BATTLE
        resetGame()
    }

    private fun resetGame() {
        _towers.value = emptyList()
        _enemies.value = emptyList()
        _projectiles.value = emptyList()
        _shopQueue.value = listOf(null, null, null, null, null)
        _peaches.value = 40
        _baseHp1.value = 6
        _baseHp2.value = 6
        _waveNumber.value = 1
        _waveInProgress.value = false
        _hasWon.value = false
        _selectedHandIndex.value = null
        _selectedMapTowerId.value = null
        _activeSkill.value = null
        _targetGeneral.value = null
        _peachCount.value = 10

        // Perform initial conscription
        conscript(free = true)
    }

    fun selectHandSlot(index: Int) {
        _activeSkill.value = null
        _selectedMapTowerId.value = null
        if (_selectedHandIndex.value == index) {
            _selectedHandIndex.value = null
        } else {
            _selectedHandIndex.value = index
        }
    }

    fun selectActiveSkill(skill: String) {
        _selectedHandIndex.value = null
        _selectedMapTowerId.value = null
        if (_activeSkill.value == skill) {
            _activeSkill.value = null
        } else {
            _activeSkill.value = skill
            if (skill == "rockets") {
                triggerRockets()
            }
        }
    }

    fun selectMapCell(x: Int, y: Int) {
        val buildable = isBuildablePlot(x, y)
        if (!buildable) return

        val existingTower = _towers.value.find { it.gridX == x && it.gridY == y }
        val handIndex = _selectedHandIndex.value
        val skill = _activeSkill.value

        if (skill == "shovel") {
            if (existingTower != null) {
                // Refund peaches
                _peaches.value += 5 * existingTower.star
                _towers.value = _towers.value - existingTower
                _activeSkill.value = null
            }
            return
        }

        if (skill == "upgrade") {
            if (existingTower != null && existingTower.star < 3) {
                val upgradeCost = existingTower.star * 15
                if (_peaches.value >= upgradeCost) {
                    _peaches.value -= upgradeCost
                    val (dmg, rng, interval) = SynthesisEngine.getCharacterAttributes(existingTower.char, existingTower.star + 1)
                    val upgraded = existingTower.copy(
                        star = existingTower.star + 1,
                        damage = dmg,
                        range = rng,
                        attackIntervalMs = interval
                    )
                    _towers.value = _towers.value.map { if (it.id == existingTower.id) upgraded else it }
                    _activeSkill.value = null
                }
            }
            return
        }

        if (handIndex != null) {
            val charInHand = _shopQueue.value[handIndex] ?: return

            if (existingTower == null) {
                // Place new tower
                val (dmg, rng, interval) = SynthesisEngine.getCharacterAttributes(charInHand, 1)
                val newTower = Tower(
                    char = charInHand,
                    gridX = x,
                    gridY = y,
                    damage = dmg,
                    range = rng,
                    attackIntervalMs = interval
                )
                _towers.value = _towers.value + newTower
                clearHandSlot(handIndex)
                _selectedHandIndex.value = null
            } else {
                // Try synthesis / merge
                val synthesisResult = SynthesisEngine.checkSynthesis(
                    charA = existingTower.char,
                    starA = existingTower.star,
                    charB = charInHand,
                    starB = 1
                )
                if (synthesisResult != null) {
                    if (synthesisResult == existingTower.char && existingTower.star == 1) {
                        // Level up same character
                        val (dmg, rng, interval) = SynthesisEngine.getCharacterAttributes(existingTower.char, 2)
                        val upgraded = existingTower.copy(
                            star = 2,
                            damage = dmg,
                            range = rng,
                            attackIntervalMs = interval
                        )
                        _towers.value = _towers.value.map { if (it.id == existingTower.id) upgraded else it }
                    } else {
                        // Transform to new character
                        val (dmg, rng, interval) = SynthesisEngine.getCharacterAttributes(synthesisResult, existingTower.star)
                        val transformed = existingTower.copy(
                            char = synthesisResult,
                            damage = dmg,
                            range = rng,
                            attackIntervalMs = interval
                        )
                        _towers.value = _towers.value.map { if (it.id == existingTower.id) transformed else it }
                    }
                    clearHandSlot(handIndex)
                    _selectedHandIndex.value = null
                }
            }
            return
        }

        // Click to select tower on map (for relocation or swap)
        val selectedMapId = _selectedMapTowerId.value
        if (selectedMapId != null) {
            val selectedTower = _towers.value.find { it.id == selectedMapId } ?: return
            if (existingTower == null) {
                // Move tower
                val updated = selectedTower.copy(gridX = x, gridY = y)
                _towers.value = _towers.value.map { if (it.id == selectedMapId) updated else it }
                _selectedMapTowerId.value = null
            } else if (existingTower.id != selectedMapId) {
                // Try merge map towers
                val synthesisResult = SynthesisEngine.checkSynthesis(
                    charA = existingTower.char,
                    starA = existingTower.star,
                    charB = selectedTower.char,
                    starB = selectedTower.star
                )
                if (synthesisResult != null) {
                    val finalStar = if (synthesisResult == existingTower.char && existingTower.star == selectedTower.star) {
                        kotlin.math.min(3, existingTower.star + 1)
                    } else {
                        kotlin.math.max(existingTower.star, selectedTower.star)
                    }
                    val (dmg, rng, interval) = SynthesisEngine.getCharacterAttributes(synthesisResult, finalStar)
                    val transformed = existingTower.copy(
                        char = synthesisResult,
                        star = finalStar,
                        damage = dmg,
                        range = rng,
                        attackIntervalMs = interval
                    )
                    _towers.value = _towers.value.map { if (it.id == existingTower.id) transformed else it }
                    _towers.value = _towers.value - selectedTower
                    _selectedMapTowerId.value = null
                } else {
                    // Swap
                    val updatedSel = selectedTower.copy(gridX = x, gridY = y)
                    val updatedExist = existingTower.copy(gridX = selectedTower.gridX, gridY = selectedTower.gridY)
                    _towers.value = _towers.value.map {
                        when (it.id) {
                            selectedMapId -> updatedSel
                            existingTower.id -> updatedExist
                            else -> it
                        }
                    }
                    _selectedMapTowerId.value = null
                }
            } else {
                _selectedMapTowerId.value = null
            }
        } else {
            if (existingTower != null) {
                _selectedMapTowerId.value = existingTower.id
            }
        }
    }

    private fun clearHandSlot(index: Int) {
        val current = _shopQueue.value.toMutableList()
        current[index] = null
        _shopQueue.value = current
    }

    fun setDestinyLock(genName: String?) {
        _targetGeneral.value = genName
    }

    fun usePeachItem() {
        if (_peachCount.value > 0) {
            _peachCount.value -= 1
            _baseHp1.value = kotlin.math.min(6, _baseHp1.value + 1)
            _baseHp2.value = kotlin.math.min(6, _baseHp2.value + 1)
        }
    }

    fun conscript(free: Boolean = false) {
        val cost = getConscriptionCost()
        if (!free && _peaches.value < cost) return

        val emptySlotIndex = _shopQueue.value.indexOf(null)
        if (emptySlotIndex == -1) return // No room in hand queue

        if (!free) {
            _peaches.value -= cost
        }

        // Generate character based on selected generals and basic components
        val character = rollCharacter()
        val current = _shopQueue.value.toMutableList()
        current[emptySlotIndex] = character
        _shopQueue.value = current
    }

    fun getConscriptionCost(): Int {
        val wave = _waveNumber.value
        return 10 + wave * 2
    }

    private fun rollCharacter(): String {
        // Roll algorithm:
        // Basic: "兵", "马", "弓", "刀", "盾", "枪", "平"
        // Generals character pool (from the 5 selected generals)
        val selected = _selectedGenerals.value
        val generalChars = selected.flatMap { it.characters }

        val rollPool = mutableListOf<String>()
        // Fill basic items
        rollPool.addAll(listOf("兵", "兵", "马", "弓", "刀", "盾", "枪", "平", "平"))

        // Add general chars
        rollPool.addAll(generalChars)

        // Destiny lock guarantee: If targetGeneral is set, add additional copies of their characters
        val target = _targetGeneral.value
        if (target != null) {
            val gen = SynthesisEngine.getGeneralByName(target)
            if (gen != null) {
                rollPool.addAll(gen.characters) // double weight
            }
        }

        return rollPool.random()
    }

    fun startWave() {
        if (_waveInProgress.value) return
        _waveInProgress.value = true
        _selectedHandIndex.value = null
        _selectedMapTowerId.value = null
        _activeSkill.value = null

        setupWaveEnemies()
        startGameLoop()
    }

    private fun setupWaveEnemies() {
        val wave = _waveNumber.value
        spawnQueue.clear()

        val enemyTypes = when (wave) {
            1 -> List(8) { "卒" }
            2 -> List(10) { "卒" } + List(2) { "骑" }
            3 -> List(10) { "卒" } + List(4) { "骑" } + List(2) { "先锋" }
            4 -> List(12) { "卒" } + List(6) { "骑" } + List(4) { "先锋" }
            5 -> List(15) { "卒" } + List(8) { "骑" } + List(6) { "先锋" } + listOf("曹") // mini boss
            else -> List(15 + wave) { "卒" } + List(6 + wave) { "骑" } + List(4 + wave) { "先锋" }
        }

        enemyTypes.forEachIndexed { index, type ->
            val path = if (index % 2 == 0) 1 else 2
            val maxHp = when (type) {
                "卒" -> 35 + wave * 10
                "骑" -> 60 + wave * 15
                "先锋" -> 100 + wave * 25
                "曹" -> 500 + wave * 100
                else -> 40
            }
            val speed = when (type) {
                "卒" -> 0.003f
                "骑" -> 0.006f
                "先锋" -> 0.002f
                "曹" -> 0.001f
                else -> 0.003f
            }
            val reward = when (type) {
                "卒" -> 2
                "骑" -> 4
                "先锋" -> 8
                "曹" -> 50
                else -> 2
            }

            val enemy = Enemy(
                char = type,
                pathId = path,
                currentHp = maxHp,
                maxHp = maxHp,
                speed = speed,
                reward = reward,
                isBoss = type == "曹"
            )
            spawnQueue.add(enemy)
        }
        nextSpawnTime = System.currentTimeMillis()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_waveInProgress.value) {
                val now = System.currentTimeMillis()
                tickGame(now)
                delay(50) // 20 frames per second
            }
        }
    }

    private fun tickGame(currentTime: Long) {
        val currentEnemies = _enemies.value.toMutableList()
        val currentProjectiles = _projectiles.value.toMutableList()

        // 1. Spawning
        if (spawnQueue.isNotEmpty() && currentTime >= nextSpawnTime) {
            val nextEnemy = spawnQueue.removeFirst()
            currentEnemies.add(nextEnemy)
            _enemies.value = currentEnemies
            nextSpawnTime = currentTime + 1500 // spawn every 1.5s
        }

        // 2. Move Enemies
        val remainingEnemies = mutableListOf<Enemy>()
        currentEnemies.forEach { enemy ->
            enemy.pathProgress += enemy.speed
            if (enemy.pathProgress >= 1f) {
                // Reached the base!
                if (enemy.pathId == 1) {
                    _baseHp2.value = kotlin.math.max(0, _baseHp2.value - (if (enemy.isBoss) 3 else 1))
                } else {
                    _baseHp1.value = kotlin.math.max(0, _baseHp1.value - (if (enemy.isBoss) 3 else 1))
                }
                if (_baseHp1.value <= 0 || _baseHp2.value <= 0) {
                    _waveInProgress.value = false
                    _screenState.value = GameScreenState.GAME_OVER
                    _hasWon.value = false
                    gameLoopJob?.cancel()
                }
            } else {
                // Update position coordinates
                val pathPoints = if (enemy.pathId == 1) path1Points else path2Points
                val (x, y) = getCoordinateOnPath(pathPoints, enemy.pathProgress)
                enemy.x = x
                enemy.y = y
                remainingEnemies.add(enemy)
            }
        }
        _enemies.value = remainingEnemies

        // 3. Towers Attack
        _towers.value = _towers.value.map { tower ->
            val spelledGen = getSpelledGeneral(tower, _towers.value)
            val basicUnits = setOf("兵", "马", "弓", "刀", "枪", "盾", "骑")
            val isGeneralComponent = !basicUnits.contains(tower.char)

            if (isGeneralComponent && spelledGen == null) {
                // Inactive general component, does not attack
                tower
            } else {
                // Active! Calculate temporary stats for the general if spelled
                val (finalDmg, finalRng, finalInterval) = if (isGeneralComponent && spelledGen != null) {
                    SynthesisEngine.getCharacterAttributes(spelledGen, tower.star)
                } else {
                    Triple(tower.damage, tower.range, tower.attackIntervalMs)
                }

                if (currentTime >= tower.lastAttackTime + finalInterval) {
                    // Find enemy in range
                    val dummyTower = tower.copy(range = finalRng)
                    val target = findEnemyInRange(dummyTower, remainingEnemies)
                    if (target != null) {
                        // Fire projectile
                        val checkChar = if (isGeneralComponent && spelledGen != null) {
                            if (spelledGen.startsWith("伪·")) spelledGen.substring(2) else spelledGen
                        } else {
                            tower.char
                        }
                        val projType = when (checkChar) {
                            "弓", "黄忠" -> "arrow"
                            "诸葛亮", "周瑜", "司马懿" -> "lightning"
                            else -> "slash"
                        }
                        val projectile = Projectile(
                            currentX = tower.gridX.toFloat() + 0.5f,
                            currentY = tower.gridY.toFloat() + 0.5f,
                            targetEnemyId = target.id,
                            damage = finalDmg,
                            speed = 0.15f,
                            type = projType
                        )
                        currentProjectiles.add(projectile)
                        tower.copy(lastAttackTime = currentTime)
                    } else {
                        tower
                    }
                } else {
                    tower
                }
            }
        }
        _projectiles.value = currentProjectiles

        // 4. Move Projectiles and check hit
        val remainingProjectiles = mutableListOf<Projectile>()
        currentProjectiles.forEach { proj ->
            val target = remainingEnemies.find { it.id == proj.targetEnemyId }
            if (target != null) {
                val dx = target.x + 0.5f - proj.currentX
                val dy = target.y + 0.5f - proj.currentY
                val dist = sqrt(dx*dx + dy*dy)
                if (dist < 0.3f) {
                    // Hit!
                    target.currentHp -= proj.damage
                    if (target.currentHp <= 0) {
                        _peaches.value += target.reward
                        remainingEnemies.remove(target)
                    }
                } else {
                    // Interpolate projectile movement
                    proj.currentX += (dx / dist) * proj.speed
                    proj.currentY += (dy / dist) * proj.speed
                    remainingProjectiles.add(proj)
                }
            }
        }
        _enemies.value = remainingEnemies
        _projectiles.value = remainingProjectiles

        // 5. Wave Completion
        if (spawnQueue.isEmpty() && remainingEnemies.isEmpty()) {
            _waveInProgress.value = false
            gameLoopJob?.cancel()
            _projectiles.value = emptyList()

            // Wave win bonus
            _peaches.value += 15 + _waveNumber.value * 2

            if (_waveNumber.value >= 10) {
                _screenState.value = GameScreenState.GAME_OVER
                _hasWon.value = true
            } else {
                _waveNumber.value += 1
            }
        }
    }

    private fun findEnemyInRange(tower: Tower, enemies: List<Enemy>): Enemy? {
        val tx = tower.gridX.toFloat() + 0.5f
        val ty = tower.gridY.toFloat() + 0.5f
        var bestTarget: Enemy? = null
        var maxProgress = -1f

        enemies.forEach { enemy ->
            val dx = enemy.x + 0.5f - tx
            val dy = enemy.y + 0.5f - ty
            val dist = sqrt(dx*dx + dy*dy)
            if (dist <= tower.range) {
                // Focus on the enemy closest to reaching the base (max progress)
                if (enemy.pathProgress > maxProgress) {
                    maxProgress = enemy.pathProgress
                    bestTarget = enemy
                }
            }
        }
        return bestTarget
    }

    fun getSpelledGeneral(tower: Tower, allTowers: List<Tower>): String? {
        val basicUnits = setOf("兵", "马", "弓", "刀", "枪", "盾", "骑")
        if (basicUnits.contains(tower.char)) return null

        for (other in allTowers) {
            if (other.id == tower.id) continue

            // Horizontal spelling: tower is left, other is right
            if (other.gridX == tower.gridX + 1 && other.gridY == tower.gridY) {
                val spelling = SynthesisEngine.checkSpelling(tower.char, other.char)
                if (spelling != null) return spelling
            }
            // Vertical spelling: tower is top, other is below
            if (other.gridX == tower.gridX && other.gridY == tower.gridY + 1) {
                val spelling = SynthesisEngine.checkSpelling(tower.char, other.char)
                if (spelling != null) return spelling
            }
            // Horizontal spelling: other is left, tower is right
            if (tower.gridX == other.gridX + 1 && tower.gridY == other.gridY) {
                val spelling = SynthesisEngine.checkSpelling(other.char, tower.char)
                if (spelling != null) return spelling
            }
            // Vertical spelling: other is top, tower is below
            if (tower.gridX == other.gridX && tower.gridY == other.gridY + 1) {
                val spelling = SynthesisEngine.checkSpelling(other.char, tower.char)
                if (spelling != null) return spelling
            }
        }
        return null
    }

    private fun triggerRockets() {
        val cost = 20
        if (_peaches.value >= cost) {
            _peaches.value -= cost
            // Deal damage to all active enemies
            val current = _enemies.value.toMutableList()
            val remaining = mutableListOf<Enemy>()
            current.forEach { enemy ->
                enemy.currentHp -= 100
                if (enemy.currentHp > 0) {
                    remaining.add(enemy)
                } else {
                    _peaches.value += enemy.reward
                }
            }
            _enemies.value = remaining
        }
        _activeSkill.value = null
    }

    fun endBattleAndGoToSelection() {
        _screenState.value = GameScreenState.LINEUP_SELECTION
    }

    private fun getCoordinateOnPath(points: List<Pair<Float, Float>>, progress: Float): Pair<Float, Float> {
        if (points.isEmpty()) return 0f to 0f
        if (points.size == 1) return points[0]
        if (progress <= 0f) return points.first()
        if (progress >= 1f) return points.last()

        val lengths = mutableListOf<Float>()
        var totalLength = 0f
        for (i in 0 until points.size - 1) {
            val dx = points[i+1].first - points[i].first
            val dy = points[i+1].second - points[i].second
            val length = sqrt(dx*dx + dy*dy)
            lengths.add(length)
            totalLength += length
        }

        val targetDist = progress * totalLength
        var currentDist = 0f
        for (i in 0 until lengths.size) {
            val segmentLength = lengths[i]
            if (currentDist + segmentLength >= targetDist) {
                val segmentProgress = (targetDist - currentDist) / segmentLength
                val pStart = points[i]
                val pEnd = points[i+1]
                val x = pStart.first + segmentProgress * (pEnd.first - pStart.first)
                val y = pStart.second + segmentProgress * (pEnd.second - pStart.second)
                return x to y
            }
            currentDist += segmentLength
        }
        return points.last()
    }
}
