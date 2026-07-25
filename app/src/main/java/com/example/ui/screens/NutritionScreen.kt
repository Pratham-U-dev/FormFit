package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.api.FoodAnalysisResult
import com.example.data.NutritionLog
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCard
import com.example.ui.components.DuoProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun duoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DuoInk,
    unfocusedTextColor = DuoInk,
    focusedContainerColor = DuoSurface1,
    unfocusedContainerColor = DuoSurface1,
    focusedBorderColor = DuoGreen,
    unfocusedBorderColor = DuoBorder,
    focusedLabelColor = DuoGreen,
    unfocusedLabelColor = DuoInkMuted,
    focusedPlaceholderColor = DuoInkMuted,
    unfocusedPlaceholderColor = DuoInkMuted,
    cursorColor = DuoGreen
)

private val duoTextFieldStyle = TextStyle(
    color = DuoInk,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedDate by viewModel.selectedNutritionDate.collectAsState()
    val logs by viewModel.nutritionLogsForSelectedDate.collectAsState()
    val totalCalories by viewModel.totalCaloriesForSelectedDate.collectAsState()
    val loggedDates by viewModel.loggedNutritionDates.collectAsState()
    val calorieGoal by viewModel.currentDailyCaloriesGoal.collectAsState()

    var showSnapOptionsModal by remember { mutableStateOf(false) }
    var showManualAddModal by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }

    // State for AI Analysis workflow
    var isAnalyzingFood by remember { mutableStateOf(false) }
    var pendingAiResult by remember { mutableStateOf<FoodAnalysisResult?>(null) }
    var pendingCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }

    // Date navigation list (past 14 days + today)
    val dateList = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val list = mutableListOf<String>()
        for (i in 0..13) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    // Helper function to process a bitmap safely
    fun processBitmapForAnalysis(bitmap: Bitmap) {
        pendingCapturedBitmap = bitmap
        try {
            val file = File(context.cacheDir, "food_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.close()
            pendingPhotoPath = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            pendingPhotoPath = null
        }

        isAnalyzingFood = true
        coroutineScope.launch {
            try {
                val result = viewModel.analyzeFoodImage(bitmap)
                pendingAiResult = result
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "AI Analysis failed. Please try again or add manually.", Toast.LENGTH_SHORT).show()
            } finally {
                isAnalyzingFood = false
            }
        }
    }

    // Photo gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    processBitmapForAnalysis(bitmap)
                } else {
                    Toast.makeText(context, "Could not load selected image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchGallerySafely() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gallery unavailable on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher with safety
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processBitmapForAnalysis(bitmap)
        } else {
            Toast.makeText(context, "No photo captured. You can choose from gallery or try sample food.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraSafely() {
        try {
            cameraLauncher.launch(null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Camera intent failed. Launching Gallery instead.", Toast.LENGTH_LONG).show()
            launchGallerySafely()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraSafely()
        } else {
            Toast.makeText(context, "Camera permission denied. Opening Gallery instead.", Toast.LENGTH_SHORT).show()
            launchGallerySafely()
        }
    }

    fun handleCameraOptionClicked() {
        showSnapOptionsModal = false
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraSafely()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DuoBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // TOP TITLE & CALENDAR SELECTOR HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NUTRITION TRACKER",
                            color = DuoInkMuted,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formatDateTitle(selectedDate, viewModel.todayDateString),
                            color = DuoInk,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    // Calendar Picker Button
                    IconButton(
                        onClick = {
                            showCalendarModal = true
                            com.example.audio.DuoSoundPlayer.playClick()
                        },
                        modifier = Modifier
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .border(2.dp, DuoBorder, shape = RoundedCornerShape(12.dp))
                            .testTag("open_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = DuoGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // HORIZONTAL DATE STRIP
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(dateList) { dateStr ->
                        val isSelected = dateStr == selectedDate
                        val hasLogs = loggedDates.contains(dateStr)
                        val isToday = dateStr == viewModel.todayDateString

                        val displayDay = getShortDayLabel(dateStr, isToday)
                        val displayNum = getDayNumLabel(dateStr)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) DuoGreen else Color.White
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) DuoGreenDark else DuoBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    viewModel.setSelectedNutritionDate(dateStr)
                                    com.example.audio.DuoSoundPlayer.playClick()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = displayDay,
                                    color = if (isSelected) Color.White else DuoInkMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = displayNum,
                                    color = if (isSelected) Color.White else DuoInk,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                if (hasLogs) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                if (isSelected) Color.White else DuoOrange,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CALORIE SUMMARY CARD
                DuoCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🥗", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Calorie Intake",
                                    color = DuoInk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Text(
                                text = "$totalCalories / $calorieGoal kcal",
                                color = DuoGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressFraction = (totalCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
                        DuoProgressBar(
                            progress = progressFraction,
                            modifier = Modifier.fillMaxWidth().height(14.dp),
                            fillColor = if (totalCalories > calorieGoal) DuoRed else DuoGreen
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val remaining = (calorieGoal - totalCalories).coerceAtLeast(0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalCalories <= calorieGoal) "$remaining kcal remaining" else "Over goal by ${totalCalories - calorieGoal} kcal!",
                                color = if (totalCalories <= calorieGoal) DuoInkMuted else DuoRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Macro pills summary
                            val totalProtein = logs.sumOf { it.proteinGrams }
                            val totalCarbs = logs.sumOf { it.carbsGrams }
                            val totalFat = logs.sumOf { it.fatGrams }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MacroBadge(label = "P", value = "${totalProtein}g", color = DuoBlue)
                                MacroBadge(label = "C", value = "${totalCarbs}g", color = DuoOrange)
                                MacroBadge(label = "F", value = "${totalFat}g", color = DuoPurple)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION BUTTONS (SNAP AI PHOTO & MANUAL ENTRY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // AI Snap Button
                    DuoButton(
                        onClick = {
                            com.example.audio.DuoSoundPlayer.playClick()
                            showSnapOptionsModal = true
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        testTag = "snap_food_photo_button"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Snap Food",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SNAP PLATE (AI)",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    // Manual Entry Button
                    DuoButton(
                        onClick = {
                            com.example.audio.DuoSoundPlayer.playClick()
                            showManualAddModal = true
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        backgroundColor = DuoBlue,
                        shadowColor = DuoBlueDark,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        testTag = "manual_entry_button"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Manual Entry",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADD MANUAL",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MEALS SECTION TITLE
                Text(
                    text = "LOGGED MEALS (${logs.size})",
                    color = DuoInkMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // LOGGED MEALS LIST
            if (logs.isEmpty()) {
                item {
                    DuoCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🍽️", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No meals logged for this day",
                                color = DuoInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Snap a photo of your plate or add items manually to track calories!",
                                color = DuoInkMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(logs, key = { it.id }) { mealLog ->
                    NutritionMealCard(
                        meal = mealLog,
                        onDelete = { viewModel.deleteNutritionLog(mealLog.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // --- MODALS & DIALOGS ---

    // 0. Snap Options Picker Modal (Camera / Gallery / Sample)
    if (showSnapOptionsModal) {
        Dialog(onDismissRequest = { showSnapOptionsModal = false }) {
            DuoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📸", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SNAP PLATE VIA AI",
                                color = DuoInk,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { showSnapOptionsModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DuoInkMuted)
                        }
                    }

                    Text(
                        text = "Choose how you would like to analyze your meal:",
                        color = DuoInkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Option 1: Take Camera Photo
                    DuoButton(
                        onClick = {
                            com.example.audio.DuoSoundPlayer.playClick()
                            handleCameraOptionClicked()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TAKE PHOTO (CAMERA)", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Choose from Gallery
                    DuoButton(
                        onClick = {
                            com.example.audio.DuoSoundPlayer.playClick()
                            showSnapOptionsModal = false
                            launchGallerySafely()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        backgroundColor = DuoBlue,
                        shadowColor = DuoBlueDark
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CHOOSE FROM GALLERY", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Sample Meal Photo (AI Test)
                    DuoButton(
                        onClick = {
                            com.example.audio.DuoSoundPlayer.playClick()
                            showSnapOptionsModal = false
                            val sampleBitmap = generateSampleFoodBitmap()
                            processBitmapForAnalysis(sampleBitmap)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        backgroundColor = DuoOrange,
                        shadowColor = Color(0xFFE08200)
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRY SAMPLE MEAL (AI TEST)", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // 1. AI Analyzing Loading / Confirmation Modal
    if (isAnalyzingFood) {
        Dialog(onDismissRequest = { }) {
            DuoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Duo AI Analyzing Plate...",
                        color = DuoInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Detecting food items, calories & macros with Gemini Vision",
                        color = DuoInkMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else if (pendingAiResult != null) {
        // AI Analysis Result Confirmation Dialog
        val result = pendingAiResult!!
        var editedMealName by remember(result) { mutableStateOf(result.mealName) }
        var editedCalories by remember(result) { mutableStateOf(result.calories.toString()) }
        var editedProtein by remember(result) { mutableStateOf(result.proteinGrams.toString()) }
        var editedCarbs by remember(result) { mutableStateOf(result.carbsGrams.toString()) }
        var editedFat by remember(result) { mutableStateOf(result.fatGrams.toString()) }

        Dialog(onDismissRequest = { pendingAiResult = null }) {
            DuoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ AI FOOD DETECTED",
                            color = DuoGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { pendingAiResult = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DuoInkMuted)
                        }
                    }

                    // Captured Photo Preview
                    pendingCapturedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Food Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Editable Fields
                    OutlinedTextField(
                        value = editedMealName,
                        onValueChange = { editedMealName = it },
                        label = { Text("Meal Name", color = DuoInkMuted) },
                        textStyle = duoTextFieldStyle,
                        colors = duoTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editedCalories,
                            onValueChange = { editedCalories = it },
                            label = { Text("Calories", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editedProtein,
                            onValueChange = { editedProtein = it },
                            label = { Text("Protein (g)", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editedCarbs,
                            onValueChange = { editedCarbs = it },
                            label = { Text("Carbs (g)", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editedFat,
                            onValueChange = { editedFat = it },
                            label = { Text("Fat (g)", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DuoButton(
                        onClick = {
                            val cals = editedCalories.toIntOrNull() ?: result.calories
                            val prot = editedProtein.toIntOrNull() ?: result.proteinGrams
                            val carb = editedCarbs.toIntOrNull() ?: result.carbsGrams
                            val fat = editedFat.toIntOrNull() ?: result.fatGrams

                            viewModel.addNutritionLog(
                                mealName = editedMealName.ifBlank { result.mealName },
                                calories = cals,
                                proteinGrams = prot,
                                carbsGrams = carb,
                                fatGrams = fat,
                                photoPath = pendingPhotoPath
                            )

                            pendingAiResult = null
                            pendingCapturedBitmap = null
                            pendingPhotoPath = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark
                    ) {
                        Text("LOG MEAL TO TODAY", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // 2. Manual Calorie Entry Modal
    if (showManualAddModal) {
        var mealNameInput by remember { mutableStateOf("") }
        var caloriesInput by remember { mutableStateOf("") }
        var proteinInput by remember { mutableStateOf("") }
        var carbsInput by remember { mutableStateOf("") }
        var fatInput by remember { mutableStateOf("") }

        // Smart Calculator Tab states
        var isCalculatorTab by remember { mutableStateOf(false) }
        var selectedPresetFood by remember { mutableStateOf("Rice (1 bowl)") }
        var portionAmount by remember { mutableStateOf("1") }

        Dialog(onDismissRequest = { showManualAddModal = false }) {
            DuoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADD NUTRITION LOG",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showManualAddModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DuoInkMuted)
                        }
                    }

                    // Mode Toggle (Direct / Calculator)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DuoBackground, shape = RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isCalculatorTab) Color.White else Color.Transparent)
                                .clickable { isCalculatorTab = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Direct Calories", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DuoInk)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCalculatorTab) Color.White else Color.Transparent)
                                .clickable { isCalculatorTab = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Item Calculator", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DuoInk)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isCalculatorTab) {
                        // Direct Input
                        OutlinedTextField(
                            value = mealNameInput,
                            onValueChange = { mealNameInput = it },
                            label = { Text("Meal / Food Name", color = DuoInkMuted) },
                            placeholder = { Text("e.g. Oatmeal & Banana", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = caloriesInput,
                            onValueChange = { caloriesInput = it },
                            label = { Text("Total Calories (kcal)", color = DuoInkMuted) },
                            textStyle = duoTextFieldStyle,
                            colors = duoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = proteinInput,
                                onValueChange = { proteinInput = it },
                                label = { Text("Prot (g)", color = DuoInkMuted) },
                                textStyle = duoTextFieldStyle,
                                colors = duoTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = carbsInput,
                                onValueChange = { carbsInput = it },
                                label = { Text("Carb (g)", color = DuoInkMuted) },
                                textStyle = duoTextFieldStyle,
                                colors = duoTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = fatInput,
                                onValueChange = { fatInput = it },
                                label = { Text("Fat (g)", color = DuoInkMuted) },
                                textStyle = duoTextFieldStyle,
                                colors = duoTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    } else {
                        // Preset Calculator
                        Text("Select Quick Food Item:", fontSize = 12.sp, color = DuoInkMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        val presetList = listOf(
                            PresetFood("Steak & Eggs", 480, 40, 5, 32),
                            PresetFood("Chicken Breast (150g)", 240, 46, 0, 5),
                            PresetFood("Cooked Rice (1 bowl)", 205, 4, 45, 0),
                            PresetFood("Protein Shake", 220, 30, 8, 3),
                            PresetFood("Avocado Toast", 320, 10, 30, 18),
                            PresetFood("Greek Yogurt & Honey", 180, 15, 20, 2)
                        )

                        LazyColumn(modifier = Modifier.height(140.dp)) {
                            items(presetList) { preset ->
                                val isSelected = preset.name == selectedPresetFood
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DuoGreenLight else DuoSurface1)
                                        .clickable {
                                            selectedPresetFood = preset.name
                                            mealNameInput = preset.name
                                            caloriesInput = preset.calories.toString()
                                            proteinInput = preset.protein.toString()
                                            carbsInput = preset.carbs.toString()
                                            fatInput = preset.fat.toString()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(preset.name, color = DuoInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${preset.calories} kcal", color = DuoGreen, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DuoButton(
                        onClick = {
                            val name = mealNameInput.ifBlank { if (isCalculatorTab) selectedPresetFood else "Logged Meal" }
                            val cals = caloriesInput.toIntOrNull() ?: 300
                            val prot = proteinInput.toIntOrNull() ?: 0
                            val carb = carbsInput.toIntOrNull() ?: 0
                            val fat = fatInput.toIntOrNull() ?: 0

                            viewModel.addNutritionLog(
                                mealName = name,
                                calories = cals,
                                proteinGrams = prot,
                                carbsGrams = carb,
                                fatGrams = fat
                            )

                            showManualAddModal = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark
                    ) {
                        Text("SAVE MEAL LOG", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // 3. Month Calendar Picker Modal
    if (showCalendarModal) {
        Dialog(onDismissRequest = { showCalendarModal = false }) {
            DuoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT DATE",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showCalendarModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DuoInkMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar List for past 30 days
                    val fullDateList = remember {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        val list = mutableListOf<String>()
                        for (i in 0..29) {
                            list.add(sdf.format(cal.time))
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                        }
                        list
                    }

                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(fullDateList) { dateStr ->
                            val isSelected = dateStr == selectedDate
                            val hasLogs = loggedDates.contains(dateStr)
                            val displayTitle = formatDateTitle(dateStr, viewModel.todayDateString)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DuoGreen else DuoSurface1)
                                    .clickable {
                                        viewModel.setSelectedNutritionDate(dateStr)
                                        showCalendarModal = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayTitle,
                                        color = if (isSelected) Color.White else DuoInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (hasLogs) {
                                        Text(
                                            text = "Logged 🥗",
                                            color = if (isSelected) Color.White else DuoOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS & HELPERS ---

@Composable
fun NutritionMealCard(
    meal: NutritionLog,
    onDelete: () -> Unit
) {
    DuoCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail or default icon
            if (!meal.photoPath.isNullOrBlank() && File(meal.photoPath).exists()) {
                val bitmap = remember(meal.photoPath) {
                    BitmapFactory.decodeFile(meal.photoPath)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = meal.mealName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(DuoSurface1, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍲", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.mealName,
                    color = DuoInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "+${meal.calories} kcal",
                        color = DuoGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Text("•", color = DuoInkMuted, fontSize = 12.sp)
                    Text(
                        text = "P: ${meal.proteinGrams}g | C: ${meal.carbsGrams}g | F: ${meal.fatGrams}g",
                        color = DuoInkMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = DuoInkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MacroBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label: $value",
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp
        )
    }
}

data class PresetFood(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

private fun formatDateTitle(dateStr: String, todayStr: String): String {
    return if (dateStr == todayStr) {
        "Today"
    } else {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfOutput = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val date = sdfInput.parse(dateStr)
            if (date != null) sdfOutput.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}

private fun getShortDayLabel(dateStr: String, isToday: Boolean): String {
    if (isToday) return "TODAY"
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("EEE", Locale.getDefault())
        val date = sdfInput.parse(dateStr)
        if (date != null) sdfOutput.format(date).uppercase(Locale.getDefault()) else ""
    } catch (e: Exception) {
        ""
    }
}

private fun getDayNumLabel(dateStr: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("d", Locale.getDefault())
        val date = sdfInput.parse(dateStr)
        if (date != null) sdfOutput.format(date) else ""
    } catch (e: Exception) {
        ""
    }
}

private fun generateSampleFoodBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    paint.color = android.graphics.Color.rgb(240, 240, 240)
    canvas.drawRect(0f, 0f, 400f, 400f, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(200f, 200f, 180f, paint)

    paint.color = android.graphics.Color.rgb(180, 100, 40)
    canvas.drawRoundRect(100f, 120f, 280f, 190f, 20f, 20f, paint)

    paint.color = android.graphics.Color.rgb(70, 160, 70)
    canvas.drawCircle(140f, 260f, 50f, paint)

    paint.color = android.graphics.Color.rgb(230, 210, 150)
    canvas.drawCircle(260f, 260f, 55f, paint)

    return bitmap
}

