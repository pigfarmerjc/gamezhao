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
                        .background(PlotColor, RoundedCornerShape(4.dp))
                        .border(1.5.dp, TextBrushColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "斗",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = TextBrushColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    repeat(baseHp1) {
                        Text("❤️", fontSize = 10.sp)
                    }
                }
            }

            // Wave display
            Text(
                text = "巨鹿 第 $waveNumber 波",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TextBrushColor
            )

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
                        .background(PlotColor, RoundedCornerShape(4.dp))
                        .border(1.5.dp, TextBrushColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "斗",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = TextBrushColor
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

                    // Draw Grass, Paths, and Buildable Plots
                    for (c in 0 until viewModel.colsCount) {
                        for (r in 0 until viewModel.rowsCount) {
                            val left = c * cellPx
                            val top = r * cellPx

                            val color = when {
                                viewModel.isBuildablePlot(c, r) -> PlotColor
                                viewModel.isPathCell(c, r) -> PathColor
                                else -> GrassColor
                            }

                            drawRect(
                                color = color,
                                topLeft = Offset(left, top),
                                size = Size(cellPx, cellPx)
                            )

                            // Draw rough borders for path cells (stone fences)
                            if (viewModel.isPathCell(c, r)) {
                                drawRect(
                                    color = StoneBorderColor,
                                    topLeft = Offset(left, top),
                                    size = Size(cellPx, cellPx),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            } else {
                                drawRect(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    topLeft = Offset(left, top),
                                    size = Size(cellPx, cellPx),
                                    style = Stroke(width = 0.5f.dp.toPx())
                                )
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
                    Box(
                        modifier = Modifier
                            .offset(
                                x = cellSize * tower.gridX + (cellSize * 0.05f),
                                y = cellSize * tower.gridY + (cellSize * 0.05f)
                            )
                            .size(cellSize * 0.9f)
                            .background(PlotColor, RoundedCornerShape(4.dp))
                            .border(1.5.dp, TextBrushColor, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tower.char,
                            fontSize = when (tower.char.length) {
                                1 -> (cellSize.value * 0.5f).sp
                                2 -> (cellSize.value * 0.35f).sp
                                else -> (cellSize.value * 0.28f).sp
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = TextBrushColor
                        )

                        // Render Wing/Feather icon for specific characters (e.g. 翼)
                        if (tower.char.contains("翼") || tower.char == "骑") {
                            Text(
                                text = "🪶",
                                fontSize = (cellSize.value * 0.25f).sp,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        // Star label
                        Text(
                            text = tower.star.toString(),
                            fontSize = (cellSize.value * 0.22f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC7923E),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 2.dp, top = 2.dp)
                        )
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
                            .background(EnemyRedColor, CircleShape)
                            .border(1.dp, TextBrushColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = enemy.char,
                            fontSize = (cellSize.value * 0.4f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White
                        )

                        // Health bar
                        val hpPercent = enemy.currentHp.toFloat() / enemy.maxHp.toFloat()
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawRect(
                                color = Color.Gray,
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height)
                            )
                            drawRect(
                                color = Color.Green,
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
                // House Icon (营)
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color(0xFF8B7355), RoundedCornerShape(4.dp))
                        .border(1.5.dp, TextBrushColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "营",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

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
                                .background(PlotColor, RoundedCornerShape(4.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFD32F2F) else TextBrushColor,
                                    shape = RoundedCornerShape(4.dp)
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

            // Large Conscript Button
            Button(
                onClick = { viewModel.conscript() },
                enabled = shopQueue.contains(null) && peaches >= viewModel.getConscriptionCost(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8C5333),
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .width(90.dp)
                    .height(45.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "征兵",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White
                    )
                    Text(
                        text = "🍑 ${viewModel.getConscriptionCost()}",
                        fontSize = 10.sp,
                        color = Color.White
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

            // Start Wave Button
            Button(
                onClick = { viewModel.startWave() },
                enabled = !waveInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8E23)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(40.dp)
            ) {
                Text(
                    text = "▶ 开战",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White
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
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFFD32F2F) else Color(0xFF8B8B7A)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .width(60.dp)
            .height(40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = iconText, fontSize = 14.sp)
            Text(text = label, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Serif)
            Text(text = description, fontSize = 7.sp, color = Color.White.copy(alpha = 0.8f))
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
