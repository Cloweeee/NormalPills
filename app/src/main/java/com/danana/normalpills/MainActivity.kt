@file:OptIn(ExperimentalPermissionsApi::class)

package com.danana.normalpills

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import com.danana.normalpills.ui.theme.NormalPillsTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.*
import com.tencent.mmkv.MMKV
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.*
import nl.dionsegijn.konfetti.core.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class CustomObject (
        val date: String,
        val duration: Long
    )

    // Retrieve array with recent pills
    private fun getDates(): JSONArray {
        val sharedPref = this.getPreferences(MODE_PRIVATE)
        val defaultValue = getString(R.string.dates_array_default)
        return JSONArray(sharedPref.getString(getString(R.string.dates_array), defaultValue))
    }

    fun startTimer(notifBool: Boolean, timeToUse: Long, running: MutableState<Boolean>, dateString: MutableState<String>, sweepProgress: MutableState<Float>, lastSelected: MutableState<Long>, showKonfetti: MutableState<Boolean>, lastDate: MutableState<Long>) {

        if(running.value) return
        running.value = true
        // Ticking must happen at least every second
        var interval = timeToUse/7200
        if(interval > 1*1000) interval = 1000

        val countDownTimer = object : CountDownTimer(timeToUse, interval) { // Create a timer
            override fun onTick(millisRemaining: Long) {
                dateString.value = DateUtils.formatElapsedTime(StringBuilder("HH:MM:SS"), millisRemaining / 1000) // Display current time inside of the DateText composable
                sweepProgress.value = millisRemaining.toFloat()/lastSelected.value.toFloat()
            }

            override fun onFinish() {
                running.value = false
                showKonfetti.value = false
            }
        }

        fun scheduleAlarm(
            title: String?,
            message: String?,
            length: Long
        ) {
            val flags = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
                else -> FLAG_UPDATE_CURRENT
            }
            val alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent =
                Intent(applicationContext, NotificationBroadcastReceiver::class.java).let { intent ->
                    intent.putExtra("NOTIFICATION_TITLE", title)
                    intent.putExtra("NOTIFICATION_MESSAGE", message)
                    PendingIntent.getBroadcast(applicationContext, 0, intent, flags)
                }

            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + length, alarmIntent)
        }

        if(notifBool && Preferences.notificationsEnabled()) {
            scheduleAlarm("Your dose is no longer in effect!", "Your dose was taken on ${SimpleDateFormat("HH:mm:ss").format(lastDate.value)}.", timeToUse)
        }
        countDownTimer.start()
    }

    @Composable
    fun HistoryItem(formattedDate: String, time: String, formattedDuration: String) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Row {
                        Text(
                            text = formattedDate + "   ",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = time,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium.merge(fontWeight = MaterialTheme.typography.titleSmall.fontWeight),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formattedDuration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        Divider()
    }

    // Main text display
    // Displays either time since last intake, or current timer
    @Composable
    fun DateText(dateString: MutableState<String>, running: MutableState<Boolean>) {
        val text = remember { mutableStateOf("") }
        val style = remember { mutableStateOf(TextStyle()) }
        if (!running.value) {
            text.value = "Last dose was taken ${dateString.value}"
            style.value = MaterialTheme.typography.titleLarge.plus(TextStyle(fontSize = 15.sp))
        } else {
            text.value = dateString.value
            style.value = MaterialTheme.typography.titleLarge
        }
        Text(
            text = text.value,
            style = style.value,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier =
            Modifier
                .padding(all = 8.dp)
                .width(250.dp)
        )
    }

    // Chips used for selecting duration of pill. Works as if they were radio-buttons, is however using a hacky implementation
    @Composable
    fun TimeChip(timeString: String, timeMs: Long, selectedTime: MutableState<Long>) {
        FilterChip(
            modifier = Modifier.padding(all = 8.dp),
            selected = selectedTime.value == timeMs, // Only display as selected if the current selected time corresponds to the time of the chip
            onClick = {
                selectedTime.value = timeMs // Change selected time to that of the chip

                val sharedPref = this.getPreferences(MODE_PRIVATE)
                with(sharedPref.edit()) {
                    // Remember the user's selection
                    putLong(getString(R.string.duration), selectedTime.value)
                    apply()
                }
            },
            label = { Text(timeString) },
            leadingIcon = if (selectedTime.value == timeMs) { // If selected, display a check-mark
                {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else { // If not selected, display a cross
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            }
        )
    }

    @Composable
    fun HomeScreen(navController: NavController) {

        val running = remember { mutableStateOf(false) } // Boolean used to remember whether a timer is running

        val sharedPref = this.getPreferences(MODE_PRIVATE)

        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        val showKonfetti = remember { mutableStateOf(false) }

        if(showKonfetti.value && Preferences.confettiEnabled()) {
            KonfettiView(parties = listOf(
                Party(
                    emitter = Emitter(1, TimeUnit.SECONDS).perSecond(500),
                    speed = 50f,
                    angle = 0
                )
            ),
            modifier = Modifier.fillMaxSize())
        }

        val dateString = remember { mutableStateOf("") } // String used in the main text box

        val selectedTime: MutableState<Long> = remember { // Load in the user's old selection, if non-existent, use 0
            mutableStateOf(sharedPref.getLong(getString(R.string.duration), 0))
        }

        val dates = getDates()
        val lastDate: MutableState<Long> = remember { mutableStateOf(0) }
        val lastSelected: MutableState<Long> = remember { mutableStateOf(0) }

        if(dates.length() > 0) { // If the user has recorded intakes before, load them in.
            val recentDateObject: JSONObject = dates[dates.length() - 1] as JSONObject

            lastDate.value = sdf.parse(recentDateObject.get("date").toString()).time
            lastSelected.value = recentDateObject.get("selectedTime").toString().toLong()
        }

        val sweepProgress: MutableState<Float> = remember { mutableStateOf(1f) } // Float remembering the current completion of the timer

        if(((Date().time.minus(lastDate.value) ) < lastSelected.value) && !running.value) {
            startTimer(false, lastSelected.value - (Date().time.minus(lastDate.value)), running, dateString, sweepProgress, lastSelected, showKonfetti, lastDate)
        }

        if (!running.value) { // If the timer isn't running, do the following
            if(lastDate.value == 0L) { // 0L is the default value for lastDate, if this is the case, the user has never recorded an intake
                dateString.value = "never"
            } else dateString.value = DateUtils.getRelativeTimeSpanString(lastDate.value).toString() // Otherwise, display time since last intake
        }

        val scaffoldState = rememberBottomSheetScaffoldState()
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 96.dp,
            sheetContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text="History", style=MaterialTheme.typography.headlineMedium)
                }
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {

                    data class Occurance(
                        val date: String,
                        val selectedTime: Long,
                    )

                    val datesList: List<Occurance> = Gson().fromJson(dates.toString() , Array<Occurance>::class.java).toList().reversed()

                    items(datesList.size) {dateIndex ->
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val dateObj = LocalDateTime.parse(datesList[dateIndex].date, dtf)
                        val df = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        val tdf = DateTimeFormatter.ofPattern("HH:mm")
                        // coding this made me wnat to kill myself
                        HistoryItem(formattedDate = df.format(dateObj), time = tdf.format(dateObj), formattedDuration = ((datesList[dateIndex].selectedTime/3600/1000).toString() + " hours"))
                    }
                }
        }) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (row, card, settings) = createRefs()

                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier
                        .constrainAs(ref = settings) {
                            top.linkTo(parent.top)
                        }
                        .padding(all = 12.dp)
                ) {
                    Icon(Icons.Filled.Settings, "Settings")
                }

                Row(
                    modifier = Modifier // Row containing selectable chips
                        .fillMaxWidth()
                        .padding(all = 24.dp)
                        .constrainAs(ref = row) {
                            bottom.linkTo(card.top) // Put the Row of chips right above the main Card
                        }
                        .horizontalScroll(rememberScrollState()),
                ) {
                    // All selectable timer-lengths
                    //TimeChip(timeString = "TEST 20s", timeMs = 20 * 1000, selectedTime)
                    TimeChip(timeString = "1 hour", timeMs = 1 * 1000 * 60 * 60, selectedTime)
                    TimeChip(timeString = "2 hours", timeMs = 2 * 1000 * 60 * 60, selectedTime)
                    TimeChip(timeString = "4 hours", timeMs = 4 * 1000 * 60 * 60, selectedTime)
                    TimeChip(timeString = "8 hours", timeMs = 8 * 1000 * 60 * 60, selectedTime)
                    //TODO: Add a custom length option in the future.
                }
                ElevatedCard( // Create the main card!
                    Modifier
                        .size(width = 300.dp, height = 450.dp)
                        .fillMaxSize()
                        .constrainAs(ref = card) {
                            centerTo(parent)
                        },
                    elevation = elevatedCardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier // Box containing (optional) image, timer display and a line
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxSize()) {
                            val color = MaterialTheme.colorScheme.primary
                            val lineColor = MaterialTheme.colorScheme.secondary
                            val circleColor = MaterialTheme.colorScheme.secondaryContainer

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = circleColor
                                )
                                drawArc(
                                    color = lineColor,
                                    140f,
                                    260f,
                                    false,
                                    style = Stroke(
                                        2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                                if (running.value) {
                                    drawArc(
                                        color = color,
                                        140f,
                                        sweepProgress.value * 260f,
                                        false,
                                        style = Stroke(
                                            12.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }

                            // Only display autism creatures when it is enabled in the settings
                            if(Preferences.creaturesEnabled()) {
                                val image = remember { mutableStateOf(0) }

                                if (running.value) image.value = R.drawable.autism_creature // If the timer is running, display autism creature!
                                else image.value = R.drawable.adhd_creature // If the timer isn't running, display ADHD creature
                                Image(
                                    painterResource(id = image.value),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit,
                                    modifier = (Modifier
                                        .size(150.dp)
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                            )
                                )
                            } else {
                                // If autism creatures are disabled, display checkmarks instead
                                val mainIcon: MutableState<ImageVector> = remember { mutableStateOf(Icons.Rounded.Cancel) }
                                val contentDescription = remember { mutableStateOf("") }

                                if(running.value) {
                                    mainIcon.value = Icons.Rounded.CheckCircle
                                    contentDescription.value = "Checkmark indicating a running timer"
                                } else {
                                    mainIcon.value = Icons.Rounded.Cancel
                                    contentDescription.value = "Cancel icon indicating a non-running timer"
                                }
                                Icon(
                                    imageVector = mainIcon.value,
                                    contentDescription = "Checkmark",
                                    modifier = (Modifier
                                        .size(150.dp)
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                            )
                                )
                            }
                        }


                        Column(verticalArrangement = Arrangement.Center /*modifier = Modifier.height(104.dp)*/) {
                            DateText(dateString = dateString, running) // Display either time since last intake, or current timer
                            //TODO: Add text displaying intake time
                            Button( // Button used to input intakes
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                enabled = (!running.value && selectedTime.value != 0L),
                                onClick = {
                                    lastDate.value = Date().time
                                    startTimer(true, selectedTime.value, running, dateString, sweepProgress, lastSelected, showKonfetti, lastDate)
                                    showKonfetti.value = true;

                                    // Write new date
                                    with(sharedPref.edit()) { // Save current time/date, as well as the time that was selected
                                        val date = JSONObject()
                                        date.put("date", sdf.format(Date()))
                                        date.put("selectedTime", selectedTime.value)
                                        dates.put(date)
                                        putString(
                                            getString(R.string.dates_array),
                                            dates.toString()
                                        )
                                        apply()
                                    }
                                }) {
                                Text(text = "Taking some normal pills") // Button text TODO: Create setting to disable 'fun' stuff
                            }
                            if(!notificationManager.areNotificationsEnabled()) {
                                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Error, "Warning!", modifier = Modifier.align(Alignment.CenterVertically))
                                    Text("Notifications aren't enabled, consider turning them on in the settings menu.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, overflow = TextOverflow.Visible)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Settings(navController: NavController) {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigate("home")
                        }) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    }
                )
                SettingItem(title = "General", description = "Cosmetics inside the app such as confetti", icon = Icons.Filled.Settings) {
                    navController.navigate("generalsettings")
                }
                SettingItem(title = "Fun", description = "Cosmetics inside the app such as confetti", icon = Icons.Filled.Celebration) {
                    navController.navigate("funsettings")
                }
                SettingItem(title = "About", description = "Version, github", icon = Icons.Filled.Info) {
                    navController.navigate("about")
                }
            }

        }

    @Composable
    fun FunSetttings(navController: NavController) {
        Column() {
            TopAppBar(
                title = {
                    Text(
                        text = "Fun",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
            var isCreaturesEnabled by remember { mutableStateOf(Preferences.creaturesEnabled()) }
            var isConfettiEnabled by remember { mutableStateOf(Preferences.confettiEnabled()) }

            PreferenceSwitch(
                title = "Creatures",
                description = "Enable autism/ADHD creatures, otherwise known as tbh and btw",
                isChecked = isCreaturesEnabled,
                onClick = {
                    isCreaturesEnabled =! isCreaturesEnabled
                    Preferences.updateValue(CREATURES, isCreaturesEnabled)
                }
            )
            PreferenceSwitch(
                title = "Confetti",
                description = "Enable confetti when starting the timer",
                isChecked = isConfettiEnabled,
                onClick = {
                    isConfettiEnabled =! isConfettiEnabled
                    Preferences.updateValue(CONFETTI, isConfettiEnabled)
                }
            )
        }
    }

    @Composable
    fun AboutInfo(navController: NavController) {
        Column() {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )

            val handler = LocalUriHandler.current

            SettingItem(title = "Version", description = BuildConfig.VERSION_NAME, icon = Icons.Filled.Update) {}
            SettingItem(title = "GitHub", description = "github.com/DananaBanana/NormalPills", icon = Icons.Filled.Code) { handler.openUri("https://github.com/DananaBanana/NormalPills")}

        }
    }

    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun GeneralSettings(navController: NavController) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = "General",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var areNotificationsEnabled by remember { mutableStateOf(Preferences.notificationsEnabled()) }
            var isNotificationPermissionGranted by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }
            val notificationPermission =
                if (Build.VERSION.SDK_INT >= 33) rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { status ->
                    if (!status) Toast.makeText(this@MainActivity, "Notification permissons denied", Toast.LENGTH_SHORT).show()
                    else isNotificationPermissionGranted = true
                } else null

            PreferenceSwitch(
                title = "Notifications",
                description = if (isNotificationPermissionGranted) "Notifies you whenever your dose runs out." else "Notification permissions denied",
                icon = if (!isNotificationPermissionGranted) Icons.Outlined.NotificationsOff
                else if (!areNotificationsEnabled) Icons.Outlined.Notifications
                else Icons.Outlined.NotificationsActive,
                isChecked = areNotificationsEnabled && isNotificationPermissionGranted,
                onClick = {
                    notificationPermission?.launchPermissionRequest()
                    if (isNotificationPermissionGranted) {
                        areNotificationsEnabled = !areNotificationsEnabled
                        Preferences.updateValue(NOTIFICATIONS, areNotificationsEnabled)
                    }
                }
            )

        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MMKV.initialize(this)

        setContent {
            NormalPillsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberAnimatedNavController()
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable(
                            route = "home",
                            enterTransition =  { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) },
                            exitTransition = { fadeOut(animationSpec = tween(90)) }
                        ) {
                            HomeScreen(navController)
                        }
                        composable(
                            route = "settings",
                            enterTransition =  { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) },
                            exitTransition = { fadeOut(animationSpec = tween(90)) }
                        ) {
                            Settings(navController)
                        }
                        composable(
                            route = "funsettings",
                            enterTransition =  { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) },
                            exitTransition = { fadeOut(animationSpec = tween(90)) }
                        ) {
                            FunSetttings(navController)
                        }
                        composable(
                            route = "about",
                            enterTransition =  { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) },
                            exitTransition = { fadeOut(animationSpec = tween(90)) }
                        ) {
                            AboutInfo(navController)
                        }
                        composable(
                            route = "generalsettings",
                            enterTransition =  { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) },
                            exitTransition = { fadeOut(animationSpec = tween(90)) }
                        ) {
                            GeneralSettings(navController)
                        }
                    }
                }
            }
        }
        }
    }
