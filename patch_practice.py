import re

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'r') as f:
    content = f.read()

# Add collectAsState
content = re.sub(
    r'val repCount by viewModel\.repCount\.collectAsState\(\)',
    'val repCount by viewModel.repCount.collectAsState()\n    val pullUpMetrics by viewModel.pullUpMetrics.collectAsState()',
    content
)

# Update CameraWithPoseOverlay calls inside the viewport box
target = """                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraWithPoseOverlay(
                            exerciseType = exerciseType,
                            onFrameAnalysis = { score, mistake, fb, rep, skeleton ->
                                viewModel.processCameraFrameAnalysis(score, mistake, fb, rep, skeleton)
                            }
                        )
                        // Live In-Camera Overlay for Reps and Feedback
                        Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {"""

replacement = """                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraWithPoseOverlay(
                            exerciseType = exerciseType,
                            onFrameAnalysis = { score, mistake, fb, rep, skeleton ->
                                viewModel.processCameraFrameAnalysis(score, mistake, fb, rep, skeleton)
                            }
                        )
                        if (exerciseType == "Pull-up") {
                            PullUpDashboardHUD(pullUpMetrics)
                        } else {
                            // Live In-Camera Overlay for Reps and Feedback
                            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {"""

content = content.replace(target, replacement)

target2 = """                                )
                            }
                        }
                    }
                } else {"""

replacement2 = """                                )
                            }
                        }
                        }
                    }
                } else {"""

content = content.replace(target2, replacement2)

dashboard_hud = """
@Composable
fun PullUpDashboardHUD(metrics: com.example.cv.PullUpMetrics) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${metrics.repCount}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(" REPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            }
            
            Box(modifier = Modifier
                .background(if (metrics.phase == "HOLD") DuoOrange else if (metrics.phase == "PULL") DuoGreen else DuoInk, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(metrics.phase, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("${metrics.chinAtBarCount}/${metrics.repCount} CHIN AT THE BAR", color = Color.White, fontSize = 12.sp)
            Text("${metrics.speedLossPct} % SPEED VS REP 1", color = Color.White, fontSize = 12.sp)
            Text("${metrics.peakPowerW} W PEAK POWER", color = Color.White, fontSize = 12.sp)
            Text(String.format("+%.2f °C LATS · MODELLED", metrics.latsTempRise), color = Color.White, fontSize = 12.sp)
            Text("${metrics.latsFatiguedPct} % · ${metrics.bicepsFatiguedPct} % LATS · BICEPS FATIGUED, MODEL", color = Color.White, fontSize = 12.sp)
            Text(String.format("≈ %.1f kcal / %.1f kJ OF HEAT", metrics.totalKcal, metrics.totalHeatKj), color = Color.White, fontSize = 12.sp)
        }
        
        // BOTTOM HUD
        if (metrics.lastRep != null) {
            val rep = metrics.lastRep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("REP ${rep.repNum}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (rep.fullLockout) Box(modifier = Modifier.background(DuoGreen, RoundedCornerShape(4.dp)).padding(4.dp)) { Text("FULL LOCK-OUT", color = Color.White, fontSize = 10.sp) }
                            if (rep.swayCm < 10) Box(modifier = Modifier.background(DuoGreen, RoundedCornerShape(4.dp)).padding(4.dp)) { Text("NO SWING", color = Color.White, fontSize = 10.sp) }
                        }
                        Text(String.format("up %.1f s hold %.1f s down %.1f s", rep.durationConcentric, rep.durationHold, rep.durationEccentric), color = Color.White, fontSize = 12.sp)
                        Text(String.format("peak %.2f m/s %d W ≈ %.1f kcal", rep.peakVelocity, rep.peakPower.toInt(), rep.energyKcal), color = Color.White, fontSize = 12.sp)
                        Text(String.format("speed loss %d %% sway %.0f cm", rep.speedLossPct, rep.swayCm), color = Color.White, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(if (rep.chinVerdict.contains("ABOVE")) DuoGreen else if (rep.chinVerdict.contains("AT")) DuoOrange else DuoRed, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(rep.chinVerdict, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
"""

content = content + dashboard_hud

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'w') as f:
    f.write(content)
