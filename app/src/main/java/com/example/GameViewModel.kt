package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class GameScreen {
    HOME,
    MODE_SELECT,
    LEVEL_SELECT,
    PRESET_GALLERY,
    GAMEPLAY,
    COMPLETE,
    SHOP,
    SETTINGS
}

enum class PuzzleMode {
    NUMBER,
    PICTURE
}

data class PuzzleTile(
    val id: Int, // The correct/solved position (0 to size*size - 1)
    val number: Int, // Number displayed (1 to size*size - 1)
    val isSolvedPos: Boolean,
    var bitmapSlice: Bitmap? = null // Sliced image piece for picture mode
)

class GameViewModel(private val context: Context) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("PuzzlePopSlidePrefs", Context.MODE_PRIVATE)

    // Live state flows for UI updates
    private val _currentScreen = MutableStateFlow(GameScreen.HOME)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _points = MutableStateFlow(100)
    val points: StateFlow<Int> = _points.asStateFlow()

    private val _is3DMode = MutableStateFlow(false)
    val is3DMode: StateFlow<Boolean> = _is3DMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(true)
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _selectedMode = MutableStateFlow(PuzzleMode.NUMBER)
    val selectedMode: StateFlow<PuzzleMode> = _selectedMode.asStateFlow()

    private val _selectedLevel = MutableStateFlow(1) // 1 = Easy (3x3), 2 = Medium (4x4), 3 = Hard (5x5)
    val selectedLevel: StateFlow<Int> = _selectedLevel.asStateFlow()

    private val _selectedImageIndex = MutableStateFlow(0) // 0 to 3 for preset images, -1 for custom photo
    val selectedImageIndex: StateFlow<Int> = _selectedImageIndex.asStateFlow()

    private val _customImagePath = MutableStateFlow<String?>(null)
    val customImagePath: StateFlow<String?> = _customImagePath.asStateFlow()

    // Unlocked level state (Level 1 starts unlocked, completing Level 1 unlocks Level 2, completing Level 2 unlocks Level 3)
    private val _maxUnlockedLevel = MutableStateFlow(1)
    val maxUnlockedLevel: StateFlow<Int> = _maxUnlockedLevel.asStateFlow()

    // Gameplay States
    private val _tiles = MutableStateFlow<List<PuzzleTile>>(emptyList())
    val tiles: StateFlow<List<PuzzleTile>> = _tiles.asStateFlow()

    private val _emptyIndex = MutableStateFlow(0)
    val emptyIndex: StateFlow<Int> = _emptyIndex.asStateFlow()

    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()

    private val _timeElapsed = MutableStateFlow(0) // in seconds
    val timeElapsed: StateFlow<Int> = _timeElapsed.asStateFlow()

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    private val _isSolved = MutableStateFlow(false)
    val isSolved: StateFlow<Boolean> = _isSolved.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    // Last game completion stats
    val lastScore = mutableStateOf(0)
    val lastStars = mutableStateOf(1)
    val isNewHighscore = mutableStateOf(false)

    // AdMob rewarded cooldown tracker
    private val _lastAdWatchTime = MutableStateFlow(0L)
    val lastAdWatchTime: StateFlow<Long> = _lastAdWatchTime.asStateFlow()

    // Daily once 200 coin reward tracker
    private val _lastDaily200AdDate = MutableStateFlow("")
    val lastDaily200AdDate: StateFlow<String> = _lastDaily200AdDate.asStateFlow()

    // Bitmap cache of slices
    private val _tileBitmaps = MutableStateFlow<List<Bitmap?>>(emptyList())

    // Timer Job
    private var timerJob: Job? = null

    init {
        // Load stored parameters from SharedPreferences
        _points.value = sharedPrefs.getInt("points", 100)
        _is3DMode.value = sharedPrefs.getBoolean("is3DMode", false)
        _soundEnabled.value = sharedPrefs.getBoolean("soundEnabled", true)
        _musicEnabled.value = sharedPrefs.getBoolean("musicEnabled", true)
        _maxUnlockedLevel.value = sharedPrefs.getInt("maxUnlockedLevel", 1)
        _lastAdWatchTime.value = sharedPrefs.getLong("lastAdWatchTime", 0L)
        _lastDaily200AdDate.value = sharedPrefs.getString("lastDaily200AdDate", "") ?: ""
        _customImagePath.value = sharedPrefs.getString("customImagePath", null)

        val firstLaunch = sharedPrefs.getBoolean("firstLaunch", true)
        if (firstLaunch) {
            _showTutorial.value = true
            sharedPrefs.edit().putBoolean("firstLaunch", false).apply()
        }
    }

    // Navigation and Menu handlers
    fun navigateTo(screen: GameScreen) {
        _currentScreen.value = screen
        if (screen == GameScreen.GAMEPLAY) {
            startNewGame()
        } else {
            stopTimer()
        }
    }

    fun toggle3DMode() {
        val newVal = !_is3DMode.value
        _is3DMode.value = newVal
        sharedPrefs.edit().putBoolean("is3DMode", newVal).apply()
    }

    fun toggleSound() {
        val newVal = !_soundEnabled.value
        _soundEnabled.value = newVal
        sharedPrefs.edit().putBoolean("soundEnabled", newVal).apply()
    }

    fun toggleMusic() {
        val newVal = !_musicEnabled.value
        _musicEnabled.value = newVal
        sharedPrefs.edit().putBoolean("musicEnabled", newVal).apply()
    }

    fun dismissTutorial() {
        _showTutorial.value = false
    }

    fun selectMode(mode: PuzzleMode) {
        _selectedMode.value = mode
        navigateTo(GameScreen.LEVEL_SELECT)
    }

    fun selectLevel(level: Int) {
        if (level <= _maxUnlockedLevel.value) {
            _selectedLevel.value = level
            if (_selectedMode.value == PuzzleMode.PICTURE) {
                navigateTo(GameScreen.PRESET_GALLERY)
            } else {
                navigateTo(GameScreen.GAMEPLAY)
            }
        }
    }

    fun selectPresetImage(index: Int) {
        _selectedImageIndex.value = index
        navigateTo(GameScreen.GAMEPLAY)
    }

    // Points handling
    fun addPoints(amount: Int) {
        val newPoints = _points.value + amount
        _points.value = newPoints
        sharedPrefs.edit().putInt("points", newPoints).apply()
    }

    fun deductPoints(amount: Int): Boolean {
        if (_points.value >= amount) {
            val newPoints = _points.value - amount
            _points.value = newPoints
            sharedPrefs.edit().putInt("points", newPoints).apply()
            return true
        }
        return false
    }

    fun watchVideoReward(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val elapsed = now - _lastAdWatchTime.value
        val cooldown = 2 * 60 * 1000 // 2 minutes

        if (elapsed >= cooldown) {
            _lastAdWatchTime.value = now
            sharedPrefs.edit().putLong("lastAdWatchTime", now).apply()
            addPoints(50)
            onSuccess()
        } else {
            val remainingSecs = ((cooldown - elapsed) / 1000).toInt()
            val mins = remainingSecs / 60
            val secs = remainingSecs % 60
            onFailure(String.format("%02d:%02d", mins, secs))
        }
    }

    fun canClaimDailyReward(): Boolean {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return _lastDaily200AdDate.value != currentDate
    }

    fun claimDailyReward(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = sdf.format(Date())
        if (_lastDaily200AdDate.value != currentDate) {
            _lastDaily200AdDate.value = currentDate
            sharedPrefs.edit().putString("lastDaily200AdDate", currentDate).apply()
            addPoints(200)
            onSuccess()
        } else {
            onFailure("Already claimed today!")
        }
    }

    fun resetProgress() {
        _points.value = 100
        _maxUnlockedLevel.value = 1
        _is3DMode.value = false
        _soundEnabled.value = true
        _musicEnabled.value = true
        _customImagePath.value = null

        sharedPrefs.edit().clear().apply()
        sharedPrefs.edit().putBoolean("firstLaunch", false).apply()
    }

    // Custom Image handler
    fun setCustomImage(uri: Uri): Boolean {
        if (_points.value >= 25) {
            try {
                val copiedPath = copyUriToInternalStorage(uri)
                if (copiedPath != null) {
                    deductPoints(25)
                    _customImagePath.value = copiedPath
                    sharedPrefs.edit().putString("customImagePath", copiedPath).apply()
                    _selectedImageIndex.value = -1 // -1 means custom image
                    navigateTo(GameScreen.GAMEPLAY)
                    return true
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error picking image", e)
            }
        }
        return false
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "custom_puzzle_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Game logic mechanics
    fun startNewGame() {
        stopTimer()
        _moveCount.value = 0
        _timeElapsed.value = 0
        _isSolved.value = false
        _isGameActive.value = true

        val size = getGridSize()
        val tileCount = size * size

        // Prepare image slices if Picture Mode
        var bitmaps: List<Bitmap?> = List(tileCount) { null }
        if (_selectedMode.value == PuzzleMode.PICTURE) {
            bitmaps = prepareImageSlices(size)
        }

        // Generate solved tile list
        val tileList = ArrayList<PuzzleTile>()
        for (i in 0 until tileCount) {
            tileList.add(PuzzleTile(
                id = i,
                number = i + 1,
                isSolvedPos = true,
                bitmapSlice = bitmaps.getOrNull(i)
            ))
        }

        // Shuffle by making random solvable moves
        _tiles.value = tileList
        _emptyIndex.value = tileCount - 1 // Start empty tile at bottom-right
        shuffleTiles(size)

        // Reset move count after shuffle and start timer
        _moveCount.value = 0
        startTimer()
    }

    fun getGridSize(): Int {
        return when (_selectedLevel.value) {
            1 -> 3 // Easy (3x3)
            2 -> 4 // Medium (4x4)
            3 -> 5 // Hard (5x5)
            else -> 3
        }
    }

    private fun shuffleTiles(size: Int) {
        val list = _tiles.value.toMutableList()
        var emptyIdx = _emptyIndex.value
        val random = Random(System.currentTimeMillis())

        // Perform 150-200 random adjacent swaps to guarantee solvability
        val swapCount = 150 + random.nextInt(50)
        for (step in 0 until swapCount) {
            val row = emptyIdx / size
            val col = emptyIdx % size

            val validMoves = ArrayList<Int>()
            if (row > 0) validMoves.add(emptyIdx - size) // Move UP
            if (row < size - 1) validMoves.add(emptyIdx + size) // Move DOWN
            if (col > 0) validMoves.add(emptyIdx - 1) // Move LEFT
            if (col < size - 1) validMoves.add(emptyIdx + 1) // Move RIGHT

            if (validMoves.isNotEmpty()) {
                val targetIdx = validMoves[random.nextInt(validMoves.size)]
                // Swap target and empty tile
                val temp = list[emptyIdx]
                list[emptyIdx] = list[targetIdx]
                list[targetIdx] = temp

                emptyIdx = targetIdx
            }
        }

        _tiles.value = list
        _emptyIndex.value = emptyIdx
    }

    fun onTileClicked(clickedIndex: Int) {
        if (_isSolved.value || !_isGameActive.value) return

        val size = getGridSize()
        val emptyIdx = _emptyIndex.value

        val rowClick = clickedIndex / size
        val colClick = clickedIndex % size
        val rowEmpty = emptyIdx / size
        val colEmpty = emptyIdx % size

        // Calculate Manhattan distance. Orthogonal adjacency means distance is exactly 1.
        val distance = abs(rowClick - rowEmpty) + abs(colClick - colEmpty)

        if (distance == 1) {
            val list = _tiles.value.toMutableList()
            
            // Swap tiles
            val temp = list[clickedIndex]
            list[clickedIndex] = list[emptyIdx]
            list[emptyIdx] = temp

            _tiles.value = list
            _emptyIndex.value = clickedIndex
            _moveCount.value = _moveCount.value + 1

            checkIfSolved()
        }
    }

    private fun checkIfSolved() {
        val list = _tiles.value
        val size = getGridSize()
        val totalTiles = size * size

        var solved = true
        for (i in 0 until totalTiles) {
            if (list[i].id != i) {
                solved = false
                break
            }
        }

        if (solved) {
            triggerWin()
        }
    }

    private fun triggerWin() {
        _isSolved.value = true
        _isGameActive.value = false
        stopTimer()

        // Calculate stars and scores
        val time = _timeElapsed.value
        val moves = _moveCount.value
        val size = getGridSize()

        // Scoring Formula
        val baseScore = 1000
        val timeBonus = maxOf(0, 1000 - time * 5)
        val movesBonus = maxOf(0, 1000 - moves * 10)
        val totalScore = baseScore + timeBonus + movesBonus
        lastScore.value = totalScore

        // Star calculations
        val stars = when (size) {
            3 -> { // Easy (3x3)
                if (moves <= 25 && time <= 40) 3
                else if (moves <= 50 && time <= 80) 2
                else 1
            }
            4 -> { // Medium (4x4)
                if (moves <= 65 && time <= 100) 3
                else if (moves <= 130 && time <= 200) 2
                else 1
            }
            5 -> { // Hard (5x5)
                if (moves <= 140 && time <= 240) 3
                else if (moves <= 250 && time <= 450) 2
                else 1
            }
            else -> 1
        }
        lastStars.value = stars

        // Check and save Highscore
        val levelKey = "lvl_${_selectedLevel.value}_mode_${_selectedMode.value.name}_img_${_selectedImageIndex.value}"
        val bestScoreKey = "${levelKey}_bestScore"
        val bestTimeKey = "${levelKey}_bestTime"

        val prevBestScore = sharedPrefs.getInt(bestScoreKey, 0)
        if (totalScore > prevBestScore) {
            isNewHighscore.value = true
            sharedPrefs.edit()
                .putInt(bestScoreKey, totalScore)
                .putInt(bestTimeKey, time)
                .apply()
        } else {
            isNewHighscore.value = false
        }

        // Unlock next level if currently completed maximum unlocked level
        val currentLevel = _selectedLevel.value
        if (currentLevel == _maxUnlockedLevel.value && currentLevel < 3) {
            val nextLevel = currentLevel + 1
            _maxUnlockedLevel.value = nextLevel
            sharedPrefs.edit().putInt("maxUnlockedLevel", nextLevel).apply()
        }

        // Award some bonus points for completing
        val rewardPoints = stars * 15
        addPoints(rewardPoints)

        navigateTo(GameScreen.COMPLETE)
    }

    fun getBestScoreAndTime(): Pair<Int, Int> {
        val levelKey = "lvl_${_selectedLevel.value}_mode_${_selectedMode.value.name}_img_${_selectedImageIndex.value}"
        val score = sharedPrefs.getInt("${levelKey}_bestScore", 0)
        val time = sharedPrefs.getInt("${levelKey}_bestTime", 0)
        return Pair(score, time)
    }

    // Timer management
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_isGameActive.value) {
                delay(1000)
                _timeElapsed.value = _timeElapsed.value + 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // Image Slicing Logic
    private fun prepareImageSlices(gridSize: Int): List<Bitmap?> {
        val slices = ArrayList<Bitmap?>()
        try {
            // 1. Load source bitmap
            val originalBitmap = when {
                _selectedImageIndex.value == -1 && _customImagePath.value != null -> {
                    BitmapFactory.decodeFile(_customImagePath.value)
                }
                _selectedImageIndex.value == 0 -> {
                    getBitmapFromDrawable(R.drawable.img_puzzle1)
                }
                _selectedImageIndex.value == 1 -> {
                    getBitmapFromDrawable(R.drawable.img_puzzle2)
                }
                _selectedImageIndex.value == 2 -> {
                    getBitmapFromDrawable(R.drawable.img_puzzle3)
                }
                _selectedImageIndex.value == 3 -> {
                    getBitmapFromDrawable(R.drawable.img_puzzle4)
                }
                else -> {
                    getBitmapFromDrawable(R.drawable.img_puzzle1)
                }
            }

            if (originalBitmap == null) {
                return List(gridSize * gridSize) { null }
            }

            // 2. Resize and crop original bitmap to a perfect square (e.g., 600x600 for performance and resolution)
            val size = minOf(originalBitmap.width, originalBitmap.height)
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                (originalBitmap.width - size) / 2,
                (originalBitmap.height - size) / 2,
                size,
                size
            )

            // Rescale to standard 600x600
            val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 600, 600, true)

            // 3. Slice bitmap into a square grid of parts
            val sliceSize = 600 / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val slice = Bitmap.createBitmap(scaledBitmap, c * sliceSize, r * sliceSize, sliceSize, sliceSize)
                    slices.add(slice)
                }
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error slicing bitmap", e)
            return List(gridSize * gridSize) { null }
        }
        return slices
    }

    private fun getBitmapFromDrawable(resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
