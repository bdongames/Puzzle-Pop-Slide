package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import android.graphics.BitmapFactory
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var rewardedAdManager: RewardedAdManager
    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK with COPPA Compliant rules
        MobileAds.initialize(this) {}
        val requestConfiguration = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(1) // TAG_FOR_CHILD_DIRECTED_TREATMENT_YES = 1
            .setTagForUnderAgeOfConsent(1) // TAG_FOR_UNDER_AGE_OF_CONSENT_YES = 1
            .setMaxAdContentRating("G") // MAX_AD_CONTENT_RATING_G = "G"
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        // Initialize components
        viewModel = GameViewModel(applicationContext)
        rewardedAdManager = RewardedAdManager(this)
        rewardedAdManager.loadAd()
        interstitialAdManager = InterstitialAdManager(this)
        interstitialAdManager.loadAd()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(CandyBgStart, CandyBgEnd)
                                )
                            )
                    ) {
                        GameContent(viewModel, rewardedAdManager, interstitialAdManager, this@MainActivity)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Google Mobile Ads - Rewarded Ad Manager
// ----------------------------------------------------
class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    val adUnitId = "ca-app-pub-7248124273941640/7337170743"

    fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity) {
                onRewardEarned()
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    loadAd()
                }
            }
        } ?: run {
            // Offline/Fail safe fallback so kids never get locked out
            onRewardEarned()
            loadAd()
        }
    }
}

// ----------------------------------------------------
// Google Mobile Ads - Interstitial Ad Manager
// ----------------------------------------------------
class InterstitialAdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    val adUnitId = "ca-app-pub-7248124273941640/7628814782"

    fun loadAd() {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity) {
        interstitialAd?.let { ad ->
            ad.show(activity)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadAd()
                }
            }
        } ?: run {
            loadAd()
        }
    }
}

// ----------------------------------------------------
// Google Mobile Ads - Banner Ad View (Compose Wrapper)
// ----------------------------------------------------
@Composable
fun BannerAdWidget() {
    val adUnitId = "ca-app-pub-7248124273941640/8941896454"
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// ----------------------------------------------------
// Play Pop and Win sounds using ToneGenerator
// ----------------------------------------------------
fun playPopSound(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    try {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
    } catch (e: Exception) {
        // Fallback
    }
}

fun playWinSound(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    try {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 250)
    } catch (e: Exception) {
        // Fallback
    }
}

// ----------------------------------------------------
// Screen routing and Orchestration
// ----------------------------------------------------
@Composable
fun GameContent(
    viewModel: GameViewModel,
    rewardedAdManager: RewardedAdManager,
    interstitialAdManager: InterstitialAdManager,
    activity: Activity
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val points by viewModel.points.collectAsState()
    val is3DMode by viewModel.is3DMode.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()

    var showNotEnoughPointsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == GameScreen.COMPLETE) {
            interstitialAdManager.showAd(activity)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar containing Points and Settings buttons
            if (currentScreen != GameScreen.HOME) {
                GameHeader(
                    points = points,
                    onShopClicked = { viewModel.navigateTo(GameScreen.SHOP) },
                    onSettingsClicked = { viewModel.navigateTo(GameScreen.SETTINGS) }
                )
            }

            // Screen switcher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentScreen) {
                    GameScreen.HOME -> HomeScreen(viewModel)
                    GameScreen.MODE_SELECT -> ModeSelectScreen(viewModel)
                    GameScreen.LEVEL_SELECT -> LevelSelectScreen(viewModel)
                    GameScreen.PRESET_GALLERY -> PresetGalleryScreen(
                        viewModel = viewModel,
                        onNotEnoughPoints = { showNotEnoughPointsDialog = true }
                    )
                    GameScreen.GAMEPLAY -> GameplayScreen(viewModel)
                    GameScreen.COMPLETE -> LevelCompleteScreen(viewModel)
                    GameScreen.SHOP -> PointsShopScreen(viewModel, rewardedAdManager, activity)
                    GameScreen.SETTINGS -> SettingsScreen(viewModel)
                }
            }

            // Banner Ad shown on Home and Level Select screens only
            if (currentScreen == GameScreen.HOME || currentScreen == GameScreen.LEVEL_SELECT) {
                BannerAdWidget()
            }
        }

        // Custom Overlay Tutorial Dialogue
        if (showTutorial) {
            TutorialOverlay(onDismiss = { viewModel.dismissTutorial() })
        }

        // Friendly Not Enough Points dialog
        if (showNotEnoughPointsDialog) {
            FriendlyPointsPopup(
                onDismiss = { showNotEnoughPointsDialog = false },
                onWatchAd = {
                    showNotEnoughPointsDialog = false
                    viewModel.navigateTo(GameScreen.SHOP)
                }
            )
        }
    }
}

// ----------------------------------------------------
// Header Component (Points, Stars, Coins)
// ----------------------------------------------------
@Composable
fun GameHeader(
    points: Int,
    onShopClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini Logo/Home button
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, CandyPink, RoundedCornerShape(16.dp))
                .clickable { onShopClicked() },
            contentAlignment = Alignment.Center
        ) {
            DrawStarIcon(size = 24.dp, isGold = true)
        }

        // Points Pill (Coin/Star Counter)
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .background(Color(0xFFFFEB3B), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                .clickable { onShopClicked() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawCoinIcon(size = 20.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$points",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF5D4037) // deep brown-900 look
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(CandyPink, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Settings Button
        IconButton(
            onClick = onSettingsClicked,
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, CandyPink, RoundedCornerShape(16.dp))
        ) {
            DrawSettingsIcon(size = 24.dp)
        }
    }
}

// ----------------------------------------------------
// Screens: HOME SCREEN
// ----------------------------------------------------
@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val points by viewModel.points.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Big bubbly animated app title
        Text(
            text = "Puzzle Pop\nSlide",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 46.sp,
            textAlign = TextAlign.Center,
            color = CandyPink,
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Transparent)
                .drawBehind {
                    // Draw bubbly double outlines
                }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mascot character
        MascotPoppy(modifier = Modifier.size(160.dp), state = MascotState.HAPPY)

        Spacer(modifier = Modifier.height(30.dp))

        // Play Button
        CandyButton(
            text = "PLAY GAME!",
            color = CandyGreen,
            testTag = "play_game_button",
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            onClick = { viewModel.navigateTo(GameScreen.MODE_SELECT) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Settings and Shop buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CandyButton(
                text = "Coins Shop",
                color = CandyYellow,
                testTag = "home_shop_button",
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                onClick = { viewModel.navigateTo(GameScreen.SHOP) }
            )

            CandyButton(
                text = "Settings",
                color = CandySkyBlue,
                testTag = "home_settings_button",
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                onClick = { viewModel.navigateTo(GameScreen.SETTINGS) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ----------------------------------------------------
// Screens: MODE SELECTION
// ----------------------------------------------------
@Composable
fun ModeSelectScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Puzzle Mode!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Number Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectMode(PuzzleMode.NUMBER) }
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .border(4.dp, CandyPurple, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CandyPurple, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DrawNumberModeIcon(size = 48.dp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Numbers Mode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Slide numbers into order! 1, 2, 3...",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Picture Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectMode(PuzzleMode.PICTURE) }
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .border(4.dp, CandySkyBlue, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CandySkyBlue, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DrawPictureModeIcon(size = 48.dp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Pictures Mode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assemble fun pictures or your own photo!",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        CandyButton(
            text = "Back to Home",
            color = CandyPink,
            testTag = "back_home_btn",
            modifier = Modifier
                .width(200.dp)
                .height(52.dp),
            onClick = { viewModel.navigateTo(GameScreen.HOME) }
        )
    }
}

// ----------------------------------------------------
// Screens: LEVEL SELECT
// ----------------------------------------------------
@Composable
fun LevelSelectScreen(viewModel: GameViewModel) {
    val maxUnlocked by viewModel.maxUnlockedLevel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select Grid Level",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "Unlock them one by one!",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Level 1: Easy
        LevelRow(
            levelNum = 1,
            title = "Level 1: Easy",
            desc = "3x3 Grid (8 tiles)",
            color = CandyGreen,
            isUnlocked = true,
            onSelect = { viewModel.selectLevel(1) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Level 2: Medium
        LevelRow(
            levelNum = 2,
            title = "Level 2: Medium",
            desc = "4x4 Grid (15 tiles)",
            color = CandyYellow,
            isUnlocked = maxUnlocked >= 2,
            onSelect = { viewModel.selectLevel(2) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Level 3: Hard
        LevelRow(
            levelNum = 3,
            title = "Level 3: Hard",
            desc = "5x5 Grid (24 tiles)",
            color = CandyOrange,
            isUnlocked = maxUnlocked >= 3,
            onSelect = { viewModel.selectLevel(3) }
        )

        Spacer(modifier = Modifier.height(40.dp))

        CandyButton(
            text = "Back",
            color = CandyPink,
            testTag = "level_select_back",
            modifier = Modifier
                .width(160.dp)
                .height(52.dp),
            onClick = { viewModel.navigateTo(GameScreen.MODE_SELECT) }
        )
    }
}

@Composable
fun LevelRow(
    levelNum: Int,
    title: String,
    desc: String,
    color: Color,
    isUnlocked: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isUnlocked) 6.dp else 2.dp, RoundedCornerShape(20.dp))
            .border(
                3.dp,
                if (isUnlocked) color else Color.LightGray,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = isUnlocked) { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (isUnlocked) color else Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$levelNum",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) TextDark else Color.Gray
                    )
                    Text(
                        text = desc,
                        fontSize = 13.sp,
                        color = if (isUnlocked) Color.Gray else Color.DarkGray
                    )
                }
            }

            // Lock or Go icon
            if (isUnlocked) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// Screens: PRESET IMAGES GALLERY
// ----------------------------------------------------
@Composable
fun PresetGalleryScreen(
    viewModel: GameViewModel,
    onNotEnoughPoints: () -> Unit
) {
    val points by viewModel.points.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val success = viewModel.setCustomImage(uri)
                if (!success) {
                    onNotEnoughPoints()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select a Puzzle Picture!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "Or load your own photo for 25 points!",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2x2 Grid of built-in images + 5th item "Upload Your Own Photo"
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PresetThumbnail(
                    title = "Rainbow Sun",
                    resId = R.drawable.img_puzzle1,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPresetImage(0) }
                )
                PresetThumbnail(
                    title = "Rocket Space",
                    resId = R.drawable.img_puzzle2,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPresetImage(1) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PresetThumbnail(
                    title = "Meadow Pup",
                    resId = R.drawable.img_puzzle3,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPresetImage(2) }
                )
                PresetThumbnail(
                    title = "Rainbow Ride",
                    resId = R.drawable.img_puzzle4,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPresetImage(3) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5th Tile: Custom Upload
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .border(3.dp, CandyPink, RoundedCornerShape(20.dp))
                    .clickable {
                        if (points >= 25) {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            onNotEnoughPoints()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CandyPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        DrawUploadIcon(size = 30.dp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Upload Your Own Photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Costs 25 coins • Pick from gallery",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CandyButton(
            text = "Back",
            color = CandySkyBlue,
            testTag = "preset_gallery_back",
            modifier = Modifier
                .width(140.dp)
                .height(52.dp),
            onClick = { viewModel.navigateTo(GameScreen.LEVEL_SELECT) }
        )
    }
}

@Composable
fun PresetThumbnail(
    title: String,
    resId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(3.dp, CandyYellow, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Image(
                bitmap = BitmapFactory.decodeResource(
                    LocalContext.current.resources,
                    resId
                ).asImageBitmap(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// Screens: GAMEPLAY SCREEN
// ----------------------------------------------------
@Composable
fun GameplayScreen(viewModel: GameViewModel) {
    val tiles by viewModel.tiles.collectAsState()
    val emptyIdx by viewModel.emptyIndex.collectAsState()
    val moves by viewModel.moveCount.collectAsState()
    val timer by viewModel.timeElapsed.collectAsState()
    val is3D by viewModel.is3DMode.collectAsState()
    val mode by viewModel.selectedMode.collectAsState()
    val currentLevel by viewModel.selectedLevel.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    val context = LocalContext.current
    val gridSize = viewModel.getGridSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: Active Timer, Mascot, Moves (Immersive UI theme)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = String.format("%02d:%02d", timer / 60, timer % 60),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Center Mascot Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(8.dp, CircleShape)
                        .background(Color(0xFFB2FF59), CircleShape)
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🦁", fontSize = 28.sp)
                }
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = 6.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, CandyPink.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LV $currentLevel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CandyPink
                    )
                }
            }

            // Moves Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MOVES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$moves",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // PUZZLE BOARD CONTAINER
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(32.dp))
                .background(Color(0xFF81D4FA), RoundedCornerShape(32.dp))
                .border(4.dp, Color.White, RoundedCornerShape(32.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (tiles.isNotEmpty()) {
                val itemSize = 100f / gridSize
                Column(modifier = Modifier.fillMaxSize()) {
                    for (r in 0 until gridSize) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            for (c in 0 until gridSize) {
                                val index = r * gridSize + c
                                val tile = tiles[index]
                                val isEmpty = index == emptyIdx

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                ) {
                                    if (!isEmpty) {
                                        CandyTile(
                                            tile = tile,
                                            is3D = is3D,
                                            mode = mode,
                                            onClick = {
                                                playPopSound(context, soundEnabled)
                                                viewModel.onTileClicked(index)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restart button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, CandyPink, RoundedCornerShape(16.dp))
                    .clickable { viewModel.startNewGame() },
                contentAlignment = Alignment.Center
            ) {
                DrawRetryIcon(size = 28.dp, tint = CandyPink)
            }

            // 3D Tile Mode Toggle (Interactive "Hint" styled central button)
            CandyButton(
                text = if (is3D) "3D CHUNKY" else "FLAT 2D",
                color = if (is3D) Color(0xFF32CD32) else Color(0xFF94A3B8),
                testTag = "toggle_3d_button",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                onClick = { viewModel.toggle3DMode() }
            )

            // Home / Mode Select button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, CandyPink, RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateTo(GameScreen.LEVEL_SELECT) },
                contentAlignment = Alignment.Center
            ) {
                DrawBackIcon(size = 28.dp, tint = CandyPink)
            }
        }
    }
}

@Composable
fun CandyTile(
    tile: PuzzleTile,
    is3D: Boolean,
    mode: PuzzleMode,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press physics animation
    val offsetAnimation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else if (is3D) 6.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("tile_${tile.number}")
    ) {
        // Drop Shadow layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (is3D) 6.dp else 4.dp)
                .background(
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(14.dp)
                )
        )

        // Main Tile body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offsetAnimation - (if (is3D) 6.dp else 4.dp))
                .background(
                    brush = if (mode == PuzzleMode.NUMBER) {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFFF5F7FA)))
                    } else {
                        Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0)))
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 2.dp,
                    color = if (mode == PuzzleMode.NUMBER) Color(0xFFF1F5F9) else CandyYellow,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (mode == PuzzleMode.NUMBER) {
                Text(
                    text = "${tile.number}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3B82F6) // Tailwind blue-500
                )
            } else {
                // Picture Tile
                tile.bitmapSlice?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Slice ${tile.number}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // 3D Bevel look (overlay highlighting top-left, shadow bottom-right)
            if (is3D) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Top-Left Highlight
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.35f),
                                size = Size(size.width, 4.dp.toPx()),
                                cornerRadius = CornerRadius(14.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.35f),
                                size = Size(4.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(14.dp.toPx())
                            )
                            // Bottom-Right Shadow
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.25f),
                                topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                size = Size(size.width, 4.dp.toPx()),
                                cornerRadius = CornerRadius(14.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.25f),
                                topLeft = Offset(size.width - 4.dp.toPx(), 0f),
                                size = Size(4.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(14.dp.toPx())
                            )
                        }
                )
            }
        }
    }
}

// ----------------------------------------------------
// Screens: LEVEL COMPLETE CELEBRATION
// ----------------------------------------------------
enum class MascotState { HAPPY, CHEERING, WINKING }

@Composable
fun LevelCompleteScreen(viewModel: GameViewModel) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val context = LocalContext.current

    // Trigger Win Sound once
    LaunchedEffect(Unit) {
        playWinSound(context, soundEnabled)
    }

    // Stars, Score, Highscore states
    val stars = viewModel.lastStars.value
    val score = viewModel.lastScore.value
    val isNewHigh = viewModel.isNewHighscore.value
    val maxUnlocked by viewModel.maxUnlockedLevel.collectAsState()

    // Confetti Animation loop
    var confettiParticles by remember { mutableStateOf(generateConfetti()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60 FPS
            confettiParticles = confettiParticles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.25f // Gravity
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Render confetti
                confettiParticles.forEach { p ->
                    if (p.isCircle) {
                        drawCircle(color = p.color, radius = p.size, center = Offset(p.x, p.y))
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(p.x, p.y),
                            size = Size(p.size * 1.5f, p.size)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Victory Header Text
            Text(
                text = "SUPER JOB!",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CandyPink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Puzzle Completed!",
                fontSize = 18.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Cheering Mascot
            MascotPoppy(modifier = Modifier.size(140.dp), state = MascotState.CHEERING)

            Spacer(modifier = Modifier.height(20.dp))

            // Star Rating (1 to 3 stars)
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) { index ->
                    val isLit = index < stars
                    val starScale by animateFloatAsState(
                        targetValue = if (isLit) 1.2f else 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    Box(modifier = Modifier.scale(starScale)) {
                        DrawStarIcon(size = 48.dp, isGold = isLit)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score details card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .border(4.dp, CandyYellow, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR SCORE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "$score",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )

                    if (isNewHigh) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CandyOrange, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW HIGHSCORE!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You earned +${stars * 15} Coins!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CandyGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation choices
            CandyButton(
                text = "Next Level",
                color = CandyGreen,
                testTag = "next_level_button",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    val currentLvl = viewModel.selectedLevel.value
                    if (currentLvl < 3) {
                        viewModel.selectLevel(currentLvl + 1)
                    } else {
                        viewModel.navigateTo(GameScreen.LEVEL_SELECT)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CandyButton(
                    text = "Retry",
                    color = CandyYellow,
                    testTag = "retry_puzzle_btn",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = { viewModel.navigateTo(GameScreen.GAMEPLAY) }
                )

                CandyButton(
                    text = "Map",
                    color = CandySkyBlue,
                    testTag = "map_back_btn",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    onClick = { viewModel.navigateTo(GameScreen.LEVEL_SELECT) }
                )
            }
        }
    }
}

// Simple particle model for physics-driven victory celebration
data class Confetti(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val isCircle: Boolean
)

fun generateConfetti(): List<Confetti> {
    val random = kotlin.random.Random(System.currentTimeMillis())
    val list = ArrayList<Confetti>()
    val colors = listOf(CandyPink, CandySkyBlue, CandyYellow, CandyGreen, CandyOrange, CandyPurple)
    // Spray from top and center
    for (i in 0 until 120) {
        list.add(
            Confetti(
                x = 500f + random.nextFloat() * 100f,
                y = 150f + random.nextFloat() * 100f,
                vx = (random.nextFloat() - 0.5f) * 15f,
                vy = -random.nextFloat() * 18f - 4f,
                color = colors[random.nextInt(colors.size)],
                size = 8f + random.nextFloat() * 14f,
                isCircle = random.nextBoolean()
            )
        )
    }
    return list
}

// ----------------------------------------------------
// Screens: REWARDS & SHOP
// ----------------------------------------------------
@Composable
fun PointsShopScreen(
    viewModel: GameViewModel,
    rewardedAdManager: RewardedAdManager,
    activity: Activity
) {
    val points by viewModel.points.collectAsState()
    val lastAdWatch by viewModel.lastAdWatchTime.collectAsState()
    val lastDailyDate by viewModel.lastDaily200AdDate.collectAsState()
    val isDailyAvailable = viewModel.canClaimDailyReward()

    var statusMessage by remember { mutableStateOf("") }
    var cooldownMessage by remember { mutableStateOf("") }

    // Tick/Refresh cooldown
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastAdWatch
            val cooldown = 2 * 60 * 1000
            if (elapsed < cooldown) {
                val remSecs = ((cooldown - elapsed) / 1000).toInt()
                cooldownMessage = String.format("Wait %02d:%02d", remSecs / 60, remSecs % 60)
            } else {
                cooldownMessage = ""
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Coins & Rewards Shop!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Collect coins to upload custom images!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Big coins showcase
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DrawCoinIcon(size = 48.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$points",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )
                        Text(
                            text = "Your Coin Balance",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action 1: Watch rewarded video ad
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .border(3.dp, CandyGreen, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DrawVideoPlayIcon(size = 36.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Watch Video Ad",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Earns +50 Free Coins!",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (cooldownMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color.LightGray, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cooldownMessage,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    } else {
                        CandyButton(
                            text = "WATCH NOW!",
                            color = CandyGreen,
                            testTag = "watch_ad_btn",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                viewModel.watchVideoReward(
                                    onSuccess = {
                                        statusMessage = "Earned +50 points!"
                                    },
                                    onFailure = { cooldown ->
                                        statusMessage = "Ad is cooling down!"
                                    }
                                )
                                rewardedAdManager.showAd(activity) {
                                    // Reward callback automatically handled by viewModel
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action 2: Daily Reward (Get 200 Coins)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .border(3.dp, CandyOrange, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Get 200 Coins",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (isDailyAvailable) "Watch video to get 200 coins (Daily 1/1)" else "Already claimed today!",
                            fontSize = 12.sp,
                            color = if (isDailyAvailable) Color.Gray else CandyPink
                        )
                    }

                    if (!isDailyAvailable) {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(44.dp)
                                .background(Color.LightGray, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLAIMED",
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        CandyButton(
                            text = "CLAIM NOW",
                            color = CandyOrange,
                            testTag = "buy_points_btn",
                            modifier = Modifier
                                .width(130.dp)
                                .height(44.dp),
                            onClick = {
                                if (viewModel.canClaimDailyReward()) {
                                    rewardedAdManager.showAd(activity) {
                                        viewModel.claimDailyReward(
                                            onSuccess = {
                                                statusMessage = "Earned +200 free coins!"
                                            },
                                            onFailure = { err ->
                                                statusMessage = err
                                            }
                                        )
                                    }
                                } else {
                                    statusMessage = "Already claimed today!"
                                }
                            }
                        )
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage,
                    color = CandyPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        CandyButton(
            text = "Back",
            color = CandyPink,
            testTag = "shop_back_btn",
            modifier = Modifier
                .width(140.dp)
                .height(52.dp),
            onClick = { viewModel.navigateTo(GameScreen.HOME) }
        )
    }
}

// ----------------------------------------------------
// Screens: SETTINGS
// ----------------------------------------------------
@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val is3DMode by viewModel.is3DMode.collectAsState()
    val maxUnlocked by viewModel.maxUnlockedLevel.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Game Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Toggle sound
            SettingsRow(
                title = "Sound Beeps",
                desc = "Short playful beep on slides",
                isEnabled = soundEnabled,
                onToggle = { viewModel.toggleSound() },
                icon = { DrawSpeakerIcon(size = 32.dp, isMuted = !soundEnabled) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle music
            SettingsRow(
                title = "Background Music",
                desc = "Calm background melody",
                isEnabled = musicEnabled,
                onToggle = { viewModel.toggleMusic() },
                icon = { DrawMusicIcon(size = 32.dp, isEnabled = musicEnabled) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle 3D Beveled Tiles
            SettingsRow(
                title = "3D Tile Finish",
                desc = "Raised, glossy candy depth",
                isEnabled = is3DMode,
                onToggle = { viewModel.toggle3DMode() },
                icon = { Draw3DModeIcon(size = 32.dp, color = CandyPurple) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Policy Link Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://sites.google.com/view/puzzlepopslide-privacy-policy")
                        )
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(browserIntent)
                    }
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .border(2.dp, CandySkyBlue, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawPrivacyIcon(size = 28.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Open privacy documentation page",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset progress
            CandyButton(
                text = "Reset Game Progress",
                color = CandyOrange,
                testTag = "reset_progress_btn",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    viewModel.resetProgress()
                    playPopSound(context, soundEnabled)
                }
            )
        }

        CandyButton(
            text = "Back",
            color = CandyPink,
            testTag = "settings_back_btn",
            modifier = Modifier
                .width(140.dp)
                .height(52.dp),
            onClick = { viewModel.navigateTo(GameScreen.HOME) }
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    desc: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CandyGreen,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFFF0F0F0)
                )
            )
        }
    }
}

// ----------------------------------------------------
// UI overlays: TUTORIAL ON FIRST LAUNCH
// ----------------------------------------------------
@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(310.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(4.dp, CandyYellow, RoundedCornerShape(28.dp))
                .padding(24.dp)
                .clickable(enabled = false) {}, // prevent click-through
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Little Puzzler!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CandyPink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            MascotPoppy(modifier = Modifier.size(100.dp), state = MascotState.HAPPY)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HOW TO PLAY:\n\n" +
                        "1. Tap any tile next to the empty space to slide it!\n" +
                        "2. Solve numbers in ascending order (1, 2, 3...)\n" +
                        "3. Solve pictures to reassemble the drawing!\n" +
                        "4. Finish faster with fewer moves to earn 3 STARS!",
                fontSize = 14.sp,
                color = TextDark,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            CandyButton(
                text = "LET'S PLAY!",
                color = CandyGreen,
                testTag = "dismiss_tutorial_btn",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onDismiss
            )
        }
    }
}

// ----------------------------------------------------
// UI overlays: NOT ENOUGH POINTS POPUP
// ----------------------------------------------------
@Composable
fun FriendlyPointsPopup(
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(4.dp, CandyOrange, RoundedCornerShape(28.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Oh No! Low Coins!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CandyOrange,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sad mascot look
            MascotPoppy(modifier = Modifier.size(100.dp), state = MascotState.WINKING)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You need 25 coins to upload your own picture! Watch a video or play a standard game to earn more coins!",
                fontSize = 14.sp,
                color = TextDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            CandyButton(
                text = "GET COINS!",
                color = CandyGreen,
                testTag = "popup_get_coins",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onWatchAd
            )

            Spacer(modifier = Modifier.height(10.dp))

            CandyButton(
                text = "Cancel",
                color = Color.LightGray,
                testTag = "popup_cancel",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                onClick = onDismiss
            )
        }
    }
}

// ----------------------------------------------------
// Custom Component: Physical Candy Button
// ----------------------------------------------------
@Composable
fun CandyButton(
    text: String,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetAnimation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow/Base layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 6.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        // Lower thick block layer (3D beveled thickness)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .background(
                    color = when (color) {
                        CandyPink -> Color(0xFFC24072)
                        CandySkyBlue -> Color(0xFF268D94)
                        CandyYellow -> Color(0xFFC7AF1A)
                        CandyGreen -> Color(0xFF38965B)
                        CandyOrange -> Color(0xFFC26E4A)
                        CandyPurple -> Color(0xFF7A4F9F)
                        Color(0xFFFFEB3B) -> Color(0xFFFBC02D)
                        Color(0xFF32CD32) -> Color(0xFF228B22)
                        else -> Color(
                            (color.red * 0.7f).coerceIn(0f, 1f),
                            (color.green * 0.7f).coerceIn(0f, 1f),
                            (color.blue * 0.7f).coerceIn(0f, 1f),
                            color.alpha
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
        )

        // Top glossy face layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offsetAnimation - 6.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.85f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(2.dp, Color.White, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (color == CandyYellow) TextDark else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// Custom Mascot "Poppy" - Animate Bouncing, Smiles
// ----------------------------------------------------
@Composable
fun MascotPoppy(modifier: Modifier = Modifier, state: MascotState) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Wave or bob animation
    val animBobY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val animScaleX by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier
            .graphicsLayer {
                translationY = animBobY
                scaleX = animScaleX
            }
    ) {
        val w = size.width
        val h = size.height

        // Jigsaw core body: Rounded box in Sunshine Yellow
        val bodyRect = androidx.compose.ui.geometry.Rect(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.85f)
        val bodyColor = CandyYellow
        val strokeColor = TextDark
        val strokeWidth = 5.dp.toPx()

        // 1. Draw body
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(bodyRect.left, bodyRect.top),
            size = Size(bodyRect.width, bodyRect.height),
            cornerRadius = CornerRadius(24.dp.toPx()),
        )

        // Jigsaw side knobs (circles to make it look like puzzle pieces)
        drawCircle(
            color = bodyColor,
            radius = w * 0.12f,
            center = Offset(w * 0.5f, h * 0.1f)
        )
        drawCircle(
            color = bodyColor,
            radius = w * 0.12f,
            center = Offset(w * 0.9f, h * 0.5f)
        )

        // Draw outer thick borders
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(bodyRect.left, bodyRect.top),
            size = Size(bodyRect.width, bodyRect.height),
            cornerRadius = CornerRadius(24.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Outline knobs
        drawCircle(
            color = strokeColor,
            radius = w * 0.12f,
            center = Offset(w * 0.5f, h * 0.1f),
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = strokeColor,
            radius = w * 0.12f,
            center = Offset(w * 0.9f, h * 0.5f),
            style = Stroke(width = strokeWidth)
        )

        // Fill knob interiors to hide intersecting borders
        drawCircle(color = bodyColor, radius = w * 0.10f, center = Offset(w * 0.5f, h * 0.1f))
        drawCircle(color = bodyColor, radius = w * 0.10f, center = Offset(w * 0.9f, h * 0.5f))

        // Mascot cute eyes
        val eyeRadius = w * 0.05f
        val leftEyeCenter = Offset(w * 0.38f, h * 0.42f)
        val rightEyeCenter = Offset(w * 0.62f, h * 0.42f)

        if (state == MascotState.WINKING) {
            // Left eye winking (smile arch)
            drawArc(
                color = strokeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeCenter.x - eyeRadius, leftEyeCenter.y - eyeRadius),
                size = Size(eyeRadius * 2, eyeRadius * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Right eye round
            drawCircle(color = strokeColor, radius = eyeRadius, center = rightEyeCenter)
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(rightEyeCenter.x - 2.dp.toPx(), rightEyeCenter.y - 2.dp.toPx()))
        } else {
            // Both eyes round and bubbly
            drawCircle(color = strokeColor, radius = eyeRadius, center = leftEyeCenter)
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(leftEyeCenter.x - 2.dp.toPx(), leftEyeCenter.y - 2.dp.toPx()))

            drawCircle(color = strokeColor, radius = eyeRadius, center = rightEyeCenter)
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(rightEyeCenter.x - 2.dp.toPx(), rightEyeCenter.y - 2.dp.toPx()))
        }

        // Cute rosy cheeks (Blush)
        drawCircle(
            color = CandyPink.copy(alpha = 0.7f),
            radius = w * 0.04f,
            center = Offset(w * 0.28f, h * 0.52f)
        )
        drawCircle(
            color = CandyPink.copy(alpha = 0.7f),
            radius = w * 0.04f,
            center = Offset(w * 0.72f, h * 0.52f)
        )

        // Mascot mouth (happy arc / open grin)
        if (state == MascotState.CHEERING) {
            // Big cheering open mouth
            val mouthPath = Path().apply {
                moveTo(w * 0.43f, h * 0.52f)
                quadraticTo(w * 0.5f, h * 0.72f, w * 0.57f, h * 0.52f)
                close()
            }
            drawPath(path = mouthPath, color = CandyPink)
            drawPath(path = mouthPath, color = strokeColor, style = Stroke(width = 3.dp.toPx()))
        } else {
            // Soft smile arc
            drawArc(
                color = strokeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.43f, h * 0.48f),
                size = Size(w * 0.14f, h * 0.14f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Little cute cartoon feet
        val footY = h * 0.85f
        val footW = w * 0.14f
        val footH = h * 0.08f
        drawRoundRect(color = CandyPurple, topLeft = Offset(w * 0.28f, footY), size = Size(footW, footH), cornerRadius = CornerRadius(8.dp.toPx()))
        drawRoundRect(color = strokeColor, topLeft = Offset(w * 0.28f, footY), size = Size(footW, footH), cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(width = strokeWidth))
        drawRoundRect(color = CandyPurple, topLeft = Offset(w * 0.58f, footY), size = Size(footW, footH), cornerRadius = CornerRadius(8.dp.toPx()))
        drawRoundRect(color = strokeColor, topLeft = Offset(w * 0.58f, footY), size = Size(footW, footH), cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(width = strokeWidth))
    }
}

// ----------------------------------------------------
// Custom Hand-Drawn Candy Vector Icons in Compose Canvas
// ----------------------------------------------------
@Composable
fun DrawStarIcon(size: Dp, isGold: Boolean = true) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cX = w / 2
        val cY = h / 2
        val outerRadius = w * 0.45f
        val innerRadius = w * 0.20f

        val path = Path()
        for (i in 0 until 10) {
            val angle = i * Math.PI / 5 - Math.PI / 2
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = cX + cos(angle).toFloat() * r
            val y = cY + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = if (isGold) CandyYellow else Color.LightGray
        )
        drawPath(
            path = path,
            color = TextDark,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun DrawCoinIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            brush = Brush.verticalGradient(listOf(CandyYellow, Color(0xFFC7AF1A))),
            radius = w * 0.45f
        )
        drawCircle(
            color = TextDark,
            radius = w * 0.45f,
            style = Stroke(width = 2.5f.dp.toPx())
        )
        // Inner detail ring
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = w * 0.32f,
            style = Stroke(width = 1.5f.dp.toPx())
        )
    }
}

@Composable
fun DrawNumberModeIcon(size: Dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Text(
            text = "1 2 3",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.32).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DrawPictureModeIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Border frame
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = Size(w * 0.8f, h * 0.8f),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        // Draw miniature sun inside
        drawCircle(
            color = CandyOrange,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

@Composable
fun DrawUploadIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Draw cloud upload or camera
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.7f, h * 0.55f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawCircle(
            color = CandyPink,
            radius = w * 0.15f,
            center = Offset(w * 0.5f, h * 0.55f)
        )
        // Camera flash bulb
        drawCircle(
            color = CandyYellow,
            radius = w * 0.06f,
            center = Offset(w * 0.72f, h * 0.38f)
        )
    }
}

@Composable
fun DrawVideoPlayIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // TV box
        drawRoundRect(
            color = CandyGreen,
            topLeft = Offset(w * 0.1f, h * 0.15f),
            size = Size(w * 0.8f, h * 0.7f),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.1f, h * 0.15f),
            size = Size(w * 0.8f, h * 0.7f),
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
        // Play triangle
        val path = Path().apply {
            moveTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.42f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = Color.White)
    }
}

@Composable
fun Draw3DModeIcon(size: Dp, color: Color = Color.White) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Draw a simple isometric 3D cube
        val cX = w / 2
        val cY = h / 2
        val r = w * 0.4f

        val path1 = Path().apply {
            moveTo(cX, cY)
            lineTo(cX, cY - r)
            lineTo(cX + r * 0.86f, cY - r * 0.5f)
            lineTo(cX + r * 0.86f, cY + r * 0.5f)
            lineTo(cX, cY)
        }
        val path2 = Path().apply {
            moveTo(cX, cY)
            lineTo(cX, cY + r)
            lineTo(cX - r * 0.86f, cY + r * 0.5f)
            lineTo(cX - r * 0.86f, cY - r * 0.5f)
            lineTo(cX, cY)
        }
        drawPath(path = path1, color = color.copy(alpha = 0.9f))
        drawPath(path = path2, color = color.copy(alpha = 0.6f))
        drawCircle(color = TextDark, radius = 2.dp.toPx(), center = Offset(cX, cY))
    }
}

@Composable
fun DrawRetryIcon(size: Dp, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Retry",
        tint = tint,
        modifier = Modifier.size(size)
    )
}

@Composable
fun DrawBackIcon(size: Dp, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = "Back",
        tint = tint,
        modifier = Modifier.size(size)
    )
}

@Composable
fun DrawSettingsIcon(size: Dp) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = TextDark,
        modifier = Modifier.size(size)
    )
}

@Composable
fun DrawSpeakerIcon(size: Dp, isMuted: Boolean) {
    Icon(
        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
        contentDescription = "Sound",
        tint = CandyPink,
        modifier = Modifier.size(size)
    )
}

@Composable
fun DrawMusicIcon(size: Dp, isEnabled: Boolean) {
    Icon(
        imageVector = if (isEnabled) Icons.Default.PlayArrow else Icons.Default.Close,
        contentDescription = "Music",
        tint = CandySkyBlue,
        modifier = Modifier.size(size)
    )
}

@Composable
fun DrawPrivacyIcon(size: Dp) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Privacy Policy",
        tint = CandySkyBlue,
        modifier = Modifier.size(size)
    )
}
