package com.example.worddefense.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worddefense.data.*

// Colors matching the original game's color scheme
val ParchmentColor = Color(0xFFF3EAD3) // 米黄色宣纸底色
val GrassColor = Color(0xFF9FB9A3) // 浅绿蓝色草地
val PathColor = Color(0xFFD6C8A6) // 浅褐色泥土路
val PlotColor = Color(0xFFFBF8F1) // 部署白块
val StoneBorderColor = Color(0xFF7E7259) // 乱石围栏颜色
val TextBrushColor = Color(0xFF1A1A1A) // 黑色毛笔字色
val EnemyRedColor = Color(0xFFC65A5A) // 敌人红色

@Composable
fun MainScreen(
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ParchmentColor
    ) {
        when (screenState) {
            GameScreenState.LINEUP_SELECTION -> {
                LineupSelectionScreen(viewModel)
            }
            GameScreenState.BATTLE -> {
                BattleScreen(viewModel)
            }
            GameScreenState.GAME_OVER -> {
                GameOverScreen(viewModel)
            }
        }
    }
}

@Composable
fun LineupSelectionScreen(viewModel: GameViewModel) {
    val selectedGenerals by viewModel.selectedGenerals.collectAsState()
    val allGenerals = SynthesisEngine.generalsList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "字 御 三 国",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = TextBrushColor,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Text(
            text = "战前整军 — 请挑选 5 位武将出战",
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "已选择: ${selectedGenerals.size} / 5",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = if (selectedGenerals.size == 5) Color(0xFF4E7E5A) else Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allGenerals) { general ->
                val isSelected = selectedGenerals.contains(general)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleGeneralSelection(general) }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF5D7E5A) else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE4ECD5) else PlotColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = general.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = TextBrushColor
                            )
                            Text(
                                text = "(${general.characters.joinToString("+")})",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "技能: ${general.skillName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                        Text(
                            text = general.skillDesc,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.startBattle() },
            enabled = selectedGenerals.size == 5,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B5846),
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(
                text = "进入关卡",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
        }
    }
}

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val peaches by viewModel.peaches.collectAsState()
    val baseHp1 by viewModel.baseHp1.collectAsState()
    val baseHp2 by viewModel.baseHp2.collectAsState()
    val waveNumber by viewModel.waveNumber.collectAsState()
    val waveInProgress by viewModel.waveInProgress.collectAsState()
    val towers by viewModel.towers.collectAsState()
    val enemies by viewModel.enemies.collectAsState()
    val projectiles by viewModel.projectiles.collectAsState()
    val shopQueue by viewModel.shopQueue.collectAsState()
    val selectedHandIndex by viewModel.selectedHandIndex.collectAsState()
    val selectedMapTowerId by viewModel.selectedMapTowerId.collectAsState()
    val activeSkill by viewModel.activeSkill.collectAsState()
    val targetGeneral by viewModel.targetGeneral.collectAsState()
    val selectedGenerals by viewModel.selectedGenerals.collectAsState()
    val peachCount by viewModel.peachCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP BAR (Hearts, Wave, Peaches)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left base HP (Base 1 - A Dou)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFE5D5B3), Color(0xFFC7B38A))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, Color(0xFF8B7355), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "斗",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF3D2E20)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    repeat(baseHp1) {
                        Text("❤️", fontSize = 10.sp)
                    }
                }
            }

            // Wave display scroll
            Box(
                modifier = Modifier
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color(0xFFC7A774), Color(0xFFF9F3E5), Color(0xFFC7A774))
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(1.5.dp, Color(0xFF6E5638), RoundedCornerShape(4.dp))
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "巨 鹿  第 $waveNumber 波",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF2C1E14),
                    textAlign = TextAlign.Center
                )
            }

            // Right peaches counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    repeat(baseHp2) {
                        Text("❤️", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFE5D5B3), Color(0xFFC7B38A))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, Color(0xFF8B7355), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "斗",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF3D2E20)
                    )
                }
            }
        }

        // Extra details: peach item count & target general
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PeachIcon(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "x $peaches",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF8B5E3C)
                )
            }

            // Destiny general Selector dropdown
            var showDestinyMenu by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { showDestinyMenu = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC7B38A)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "天命祈愿: ${targetGeneral ?: "未设定"}",
                        fontSize = 11.sp,
                        color = TextBrushColor,
                        fontFamily = FontFamily.Serif
                    )
                }
                DropdownMenu(
                    expanded = showDestinyMenu,
                    onDismissRequest = { showDestinyMenu = false },
                    modifier = Modifier.background(ParchmentColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("无天命", fontFamily = FontFamily.Serif) },
                        onClick = {
                            viewModel.setDestinyLock(null)
                            showDestinyMenu = false
                        }
                    )
                    selectedGenerals.forEach { gen ->
                        DropdownMenuItem(
                            text = { Text(gen.name, fontFamily = FontFamily.Serif) },
                            onClick = {
                                viewModel.setDestinyLock(gen.name)
                                showDestinyMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 2. MAIN MAP GRID (Responsive aspect ratio layout)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val gridWidth = maxWidth
            val gridHeight = maxHeight

            // Determine dimensions preserving 7:9 ratio
            val cellSize = kotlin.math.min(gridWidth.value / viewModel.colsCount, gridHeight.value / viewModel.rowsCount).dp
            val totalWidth = cellSize * viewModel.colsCount
            val totalHeight = cellSize * viewModel.rowsCount

            Box(
                modifier = Modifier
                    .size(totalWidth, totalHeight)
                    .border(2.dp, TextBrushColor)
            ) {
                // Background & Grid Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellPx = cellSize.toPx()

                    fun drawStoneFence(x1: Float, y1: Float, x2: Float, y2: Float, isHorizontal: Boolean) {
                        val stoneColor = Color(0xFF7E6F56)
                        val stoneOutlineColor = Color(0xFF4E4331)
                        val length = if (isHorizontal) x2 - x1 else y2 - y1
                        val stoneSize = 6.dp.toPx()
                        val numStones = (length / stoneSize).toInt().coerceAtLeast(3)
                        val step = length / numStones
                        for (i in 0 until numStones) {
                            val center = if (isHorizontal) {
                                Offset(x1 + i * step + step / 2f, y1)
                            } else {
                                Offset(x1, y1 + i * step + step / 2f)
                            }
                            drawCircle(
                                color = stoneColor,
                                radius = 3.5f.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = stoneOutlineColor,
                                radius = 3.5f.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                    // Draw Grass, Paths, and Buildable Plots
                    for (c in 0 until viewModel.colsCount) {
                        for (r in 0 until viewModel.rowsCount) {
                            val left = c * cellPx
                            val top = r * cellPx

                            val isPath = viewModel.isPathCell(c, r)
                            val isPlot = viewModel.isBuildablePlot(c, r)

                            val color = when {
                                isPlot -> PlotColor
                                isPath -> PathColor
                                else -> GrassColor
                            }

                            drawRect(
                                color = color,
                                topLeft = Offset(left, top),
                                size = Size(cellPx, cellPx)
                            )

                            if (isPlot) {
                                // Double border for buildable plot
                                drawRect(
                                    color = Color(0xFFC7B28A),
                                    topLeft = Offset(left + 2.dp.toPx(), top + 2.dp.toPx()),
                                    size = Size(cellPx - 4.dp.toPx(), cellPx - 4.dp.toPx()),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            } else if (!isPath) {
                                // Draw watercolor grass details
                                drawLine(
                                    color = Color(0xFF7A937E),
                                    start = Offset(left + cellPx * 0.3f, top + cellPx * 0.4f),
                                    end = Offset(left + cellPx * 0.35f, top + cellPx * 0.25f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFF7A937E),
                                    start = Offset(left + cellPx * 0.35f, top + cellPx * 0.25f),
                                    end = Offset(left + cellPx * 0.45f, top + cellPx * 0.35f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }

                    // Draw Stone borders around path
                    for (c in 0 until viewModel.colsCount) {
                        for (r in 0 until viewModel.rowsCount) {
                            if (viewModel.isPathCell(c, r)) {
                                val left = c * cellPx
                                val top = r * cellPx

                                // Check Left
                                if (c == 0 || !viewModel.isPathCell(c - 1, r)) {
                                    drawStoneFence(left, top, left, top + cellPx, isHorizontal = false)
                                }
                                // Check Right
                                if (c == viewModel.colsCount - 1 || !viewModel.isPathCell(c + 1, r)) {
                                    drawStoneFence(left + cellPx, top, left + cellPx, top + cellPx, isHorizontal = false)
                                }
                                // Check Top
                                if (r == 0 || !viewModel.isPathCell(c, r - 1)) {
                                    drawStoneFence(left, top, left + cellPx, top, isHorizontal = true)
                                }
                                // Check Bottom
                                if (r == viewModel.rowsCount - 1 || !viewModel.isPathCell(c, r + 1)) {
                                    drawStoneFence(left, top + cellPx, left + cellPx, top + cellPx, isHorizontal = true)
                                }
                            }
                        }
                    }
                }

                // Interactive Click Layers
                for (c in 0 until viewModel.colsCount) {
                    for (r in 0 until viewModel.rowsCount) {
                        val isSelected = towers.find { it.gridX == c && it.gridY == r }?.id == selectedMapTowerId
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = cellSize * c,
                                    y = cellSize * r
                                )
                                .size(cellSize)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.Green else Color.Transparent
                                )
                                .clickable { viewModel.selectMapCell(c, r) }
                        )
                    }
                }

                // Render Spawn Arrows and Bases
                // Spawn 1 (0,8)
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = cellSize * 8)
                        .size(cellSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▲",
                        color = Color.Red,
                        fontSize = (cellSize.value * 0.4f).sp
                    )
                }
                // Spawn 2 (6,0)
                Box(
                    modifier = Modifier
                        .offset(x = cellSize * 6, y = 0.dp)
                        .size(cellSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▼",
                        color = Color.Red,
                        fontSize = (cellSize.value * 0.4f).sp
                    )
                }

                // Render Placed Towers
                towers.forEach { tower ->
                    val spelledGen = viewModel.getSpelledGeneral(tower, towers)
                    val basicUnits = setOf("兵", "马", "弓", "刀", "枪", "盾", "骑")
                    val isGeneralComponent = !basicUnits.contains(tower.char)
                    val isActive = !isGeneralComponent || spelledGen != null

                    Box(
                        modifier = Modifier
                            .offset(
                                x = cellSize * tower.gridX + (cellSize * 0.05f),
                                y = cellSize * tower.gridY + (cellSize * 0.05f)
                            )
                            .size(cellSize * 0.9f)
                            .then(
                                if (isActive) {
                                    Modifier
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color(0xFFF7EFE0), Color(0xFFDFCAA5))
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isGeneralComponent) 2.5.dp else 1.5.dp,
                                            color = if (isGeneralComponent) Color(0xFFC7923E) else Color(0xFF6E5638),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                } else {
                                    Modifier
                                        .background(PlotColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .border(
                                            width = 1.5.dp,
                                            color = Color.Gray.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        ) {
                            Text(
                                text = tower.char,
                                fontSize = when (tower.char.length) {
                                    1 -> (cellSize.value * 0.45f).sp
                                    2 -> (cellSize.value * 0.32f).sp
                                    else -> (cellSize.value * 0.25f).sp
                                },
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = if (isActive) TextBrushColor else Color.Gray.copy(alpha = 0.8f)
                            )

                            // Tiny spelled general name at the bottom
                            if (isGeneralComponent && spelledGen != null) {
                                Text(
                                    text = spelledGen,
                                    fontSize = (cellSize.value * 0.18f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5E3C),
                                    fontFamily = FontFamily.Serif
                                )
                            } else if (isGeneralComponent && !isActive) {
                                Text(
                                    text = "待唤醒",
                                    fontSize = (cellSize.value * 0.16f).sp,
                                    color = Color.Red.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        // Star label (lvl marker)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 2.dp, top = 2.dp)
                        ) {
                            Text(
                                text = "★" + tower.star,
                                fontSize = (cellSize.value * 0.18f).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color(0xFFC7923E) else Color.Gray
                            )
                        }

                        // Feather/Wing icon for specific active generals
                        if (isActive && (tower.char.contains("翼") || tower.char == "骑" || spelledGen == "赵云")) {
                            Text(
                                text = "🪶",
                                fontSize = (cellSize.value * 0.2f).sp,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                            )
                        }
                    }
                }

                // Render Enemies
                enemies.forEach { enemy ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = cellSize * enemy.x + (cellSize * 0.1f),
                                y = cellSize * enemy.y + (cellSize * 0.1f)
                            )
                            .size(cellSize * 0.8f)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(Color(0xFFE57373), Color(0xFFB71C1C))
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color(0xFF5A0000), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = enemy.char,
                            fontSize = (cellSize.value * 0.38f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White
                        )

                        // Health bar
                        val hpPercent = enemy.currentHp.toFloat() / enemy.maxHp.toFloat()
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                        ) {
                            drawRect(
                                color = Color(0x66000000),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height)
                            )
                            drawRect(
                                color = Color(0xFF4CAF50),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width * hpPercent, size.height)
                            )
                        }
                    }
                }

                // Render Projectiles
                projectiles.forEach { proj ->
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val cellPx = cellSize.toPx()
                        val px = proj.currentX * cellPx
                        val py = proj.currentY * cellPx

                        when (proj.type) {
                            "arrow" -> {
                                drawCircle(
                                    color = Color.Black,
                                    radius = 3.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                            "lightning" -> {
                                drawCircle(
                                    color = Color(0xFF2FAEC9),
                                    radius = 4.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                            else -> {
                                // Slash (short line)
                                drawLine(
                                    color = TextBrushColor,
                                    start = Offset(px - 4.dp.toPx(), py - 4.dp.toPx()),
                                    end = Offset(px + 4.dp.toPx(), py + 4.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SHOP AREA (Camp queue & Conscript Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "营" Label and Slots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // House Icon (营) - Designed like a wood-carved Chinese roof plate
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF8B7355), Color(0xFF5C4731))
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(2.dp, Color(0xFF3D2E20), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "营",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 5 Hand Queue Slots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    shopQueue.forEachIndexed { index, char ->
                        val isSelected = selectedHandIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFCFAF2), Color(0xFFF0E6D2))
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) Color(0xFFD32F2F) else Color(0xFFB5A482),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectHandSlot(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (char != null) {
                                Text(
                                    text = char,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = TextBrushColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Large Conscript Button - Revamped to wood gradient with gold peach cost
            val conscriptEnabled = shopQueue.contains(null) && peaches >= viewModel.getConscriptionCost()
            Box(
                modifier = Modifier
                    .width(95.dp)
                    .height(46.dp)
                    .then(
                        if (conscriptEnabled) {
                            Modifier
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color(0xFF9E6F4B), Color(0xFF6B4226))
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(2.dp, Color(0xFF422817), RoundedCornerShape(6.dp))
                                .clickable { viewModel.conscript() }
                        } else {
                            Modifier
                                .background(Color.LightGray, RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.Gray, RoundedCornerShape(6.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "征 兵",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = if (conscriptEnabled) Color.White else Color.DarkGray
                    )
                    Text(
                        text = "🍑 ${viewModel.getConscriptionCost()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (conscriptEnabled) Color(0xFFFFD54F) else Color.DarkGray
                    )
                }
            }
        }

        // 4. ACTION TOOLBAR (Shovel, Rockets, Upgrade, Heal Base, Start wave)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shovel (铲)
            ActionButton(
                label = "铲子",
                description = "拆除",
                iconText = "🪓",
                isActive = activeSkill == "shovel",
                onClick = { viewModel.selectActiveSkill("shovel") }
            )

            // Rockets (火箭)
            ActionButton(
                label = "火箭",
                description = "20🍑",
                iconText = "🏹",
                isActive = false,
                onClick = { viewModel.selectActiveSkill("rockets") }
            )

            // Upgrade (升职)
            ActionButton(
                label = "升职",
                description = "升级",
                iconText = "⭐",
                isActive = activeSkill == "upgrade",
                onClick = { viewModel.selectActiveSkill("upgrade") }
            )

            // HP Peach Item (吃桃)
            ActionButton(
                label = "吃桃",
                description = "余 $peachCount",
                iconText = "🍑",
                isActive = false,
                onClick = { viewModel.usePeachItem() }
            )

            // Start Wave Button - Revamped to Jade Green "战鼓起"
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(42.dp)
                    .then(
                        if (!waveInProgress) {
                            Modifier
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(6.dp))
                                .clickable { viewModel.startWave() }
                        } else {
                            Modifier
                                .background(Color.LightGray, RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color.Gray, RoundedCornerShape(6.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  战 鼓 起",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = if (!waveInProgress) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    description: String,
    iconText: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(62.dp)
            .height(42.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(Color(0xFFE53935), Color(0xFFB71C1C)) // active red seal
                    } else {
                        listOf(Color(0xFF8D8D7B), Color(0xFF6B6B59)) // inactive stone seal
                    }
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) Color(0xFFFFD54F) else Color(0xFF4E4E3E),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = iconText, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Serif)
            }
            Text(text = description, fontSize = 7.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val hasWon by viewModel.hasWon.collectAsState()
    val waveNumber by viewModel.waveNumber.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(if (hasWon) Color(0xFFE4ECD5) else Color(0xFFFCDCDC), CircleShape)
                .border(3.dp, if (hasWon) Color(0xFF6B8E23) else Color.Red, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasWon) "胜" else "败",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = if (hasWon) Color(0xFF6B8E23) else Color.Red
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (hasWon) "恭喜主公！成功防守十波敌军！" else "胜败乃兵家常事，主公请重新来过！",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = TextBrushColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "防守终点: 第 $waveNumber 波波次",
            fontSize = 14.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.endBattleAndGoToSelection() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B5846)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Text(
                text = "返回主页",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
        }
    }
}

@Composable
fun PeachIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Draw small pink circle for peach body
        drawCircle(
            color = Color(0xFFFFB7B2),
            radius = size.minDimension * 0.4f,
            center = Offset(size.width * 0.5f, size.height * 0.55f)
        )
        // Draw top tip
        val tipPath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.15f)
            lineTo(size.width * 0.35f, size.height * 0.35f)
            lineTo(size.width * 0.65f, size.height * 0.35f)
            close()
        }
        drawPath(
            path = tipPath,
            color = Color(0xFFFFB7B2)
        )
        // Draw green leaf
        val leafPath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.25f)
            quadraticTo(size.width * 0.75f, size.height * 0.1f, size.width * 0.85f, size.height * 0.3f)
            quadraticTo(size.width * 0.65f, size.height * 0.35f, size.width * 0.5f, size.height * 0.25f)
            close()
        }
        drawPath(
            path = leafPath,
            color = Color(0xFF81C784)
        )
    }
}
