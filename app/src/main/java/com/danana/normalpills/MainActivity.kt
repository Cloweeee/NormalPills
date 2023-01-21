package com.danana.normalpills

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.danana.normalpills.ui.theme.NormalPillsTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    // Retrieve array with recent pills
    private fun getDates(): JSONArray {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val defaultValue = getString(R.string.dates_array_default)
        return JSONArray(sharedPref.getString(getString(R.string.dates_array), defaultValue))
    }

    // Main text display
    // Displays either time since last intake, or current timer
    @Composable
    fun DateText(dateString: MutableState<String>, running: MutableState<Boolean>) {
        val text = remember { mutableStateOf("") }
        val style = remember { mutableStateOf(TextStyle()) }
        if (!running.value) {
            text.value = "Last rittie was ${dateString.value}"
            style.value = MaterialTheme.typography.titleLarge
        } else {
            text.value = dateString.value
            style.value = MaterialTheme.typography.titleLarge
        }
        Text(
            text = text.value,
            style = style.value,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
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
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

                with(sharedPref.edit()) {
                    putLong(
                        getString(R.string.duration), selectedTime.value // Remember the user's selection
                    )
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NormalPillsTheme {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

                val dateString = remember { mutableStateOf("") } // String used in the main text box

                val selectedTime: MutableState<Long> = remember { // Load in the user's old selection, if non-existent, use 0
                    mutableStateOf(
                        sharedPref.getLong(
                            getString(R.string.duration), 0
                        )
                    )
                }

                val dates = getDates()
                val lastDate: MutableState<Long> = remember { mutableStateOf(0) }
                val lastSelected: MutableState<Long> = remember { mutableStateOf(0) }

                if(dates.length() > 0) { // If the user has recorded intakes before, load them in.
                    val recentDateObject: JSONObject = dates[dates.length() - 1] as JSONObject

                    lastDate.value = sdf.parse(recentDateObject.get("date").toString()).time
                    lastSelected.value = recentDateObject.get("selectedTime").toString().toLong()
                } else {
                    lastDate.value = 0
                    lastSelected.value = selectedTime.value
                }

                val timeToUse: MutableState<Long> = remember { mutableStateOf(selectedTime.value) } // Duration of the timer
                val timeSinceLast = Date().time.minus(lastDate.value) // Long remembering how long ago the last intake was

                val timerProgress: MutableState<Float> = remember { mutableStateOf(0f) } // Float remembering the current completion of the timer
                val running = remember { mutableStateOf(false) } // Boolean used to remember whether a timer is running

                val countDownTimer = object : CountDownTimer(timeToUse.value, 1000) { // Create a timer
                    override fun onTick(millisRemaining: Long) {
                        running.value = true
                        dateString.value = DateUtils.formatElapsedTime(StringBuilder("HH:MM:SS"), millisRemaining / 1000) // Display current time inside of the DateText composable
                        timerProgress.value = (millisRemaining.toFloat()/lastSelected.value.toFloat())
                    }

                    override fun onFinish() {
                        running.value = false
                    }
                }

                if(timeSinceLast < lastSelected.value) { // This checks whether a timer *should* still be running, if so, the app was probably closed and should resume.
                    timeToUse.value = lastSelected.value.minus(timeSinceLast)
                    countDownTimer.start()
                }

                if (!running.value) { // If the timer isn't running, do the following
                    if(lastDate.value == 0L) { // 0L is the default value for lastDate, if this is the case, the user has never recorded an intake
                        dateString.value = "never"
                    } else dateString.value = DateUtils.getRelativeTimeSpanString(lastDate.value).toString() // Otherwise, display time since last intake
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConstraintLayout {
                        val (row, card) = createRefs()

                        Row(modifier = Modifier // Row containing selectable chips
                                    .fillMaxWidth()
                                    .padding(all = 24.dp)
                                    .constrainAs(ref = row) {
                                        bottom.linkTo(card.top) // Put the Row of chips right above the main Card
                                    }
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                // All selectable timer-lengths
                                TimeChip(timeString = "1 hour", timeMs = 1 * 1000 * 60 * 60, selectedTime)
                                TimeChip(timeString = "2 hours", timeMs = 2 * 1000 * 60 * 60, selectedTime)
                                TimeChip(timeString = "4 hours", timeMs = 4 * 1000 * 60 * 60, selectedTime)
                                TimeChip(timeString = "8 hours", timeMs = 8 * 1000 * 60 * 60, selectedTime)
                                //TODO: Add a custom length option in the future.
                            }
                            ElevatedCard( // Create the main card!
                                modifier = Modifier
                                    .size(width = 300.dp, height = 400.dp)
                                    .fillMaxSize()
                                    .constrainAs(ref = card) {
                                        centerTo(parent)
                                    }
                            ) {
                                CardDefaults.cardElevation(8.dp)
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
                                            if (running.value) {
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
                                                drawArc(
                                                    color = color,
                                                    140f,
                                                    timerProgress.value * 260f,
                                                    false,
                                                    style = Stroke(
                                                        12.dp.toPx(),
                                                        cap = StrokeCap.Round
                                                    )
                                                )
                                            }
                                        }
                                        val image = remember { mutableStateOf(0) }

                                        if (running.value) image.value = R.drawable.autism_creature // If the timer is running, display autism creature!
                                        else image.value = R.drawable.adhd_creature // If the timer isn't running, display ADHD creature
                                        //TODO: Create a setting to disable 'creatures', and other 'fun' stuff that is currently implemented
                                        Image(
                                            painterResource(id = image.value),
                                            contentDescription = "",
                                            contentScale = ContentScale.Fit,
                                            modifier = (Modifier
                                                .size(150.dp))
                                                .align(Alignment.Center)
                                                .fillMaxSize()
                                        )
                                    }


                                    Column(verticalArrangement = Arrangement.Center) {
                                        DateText(dateString = dateString, running) // Display either time since last intake, or current timer

                                        Button( // Button used to input intakes
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                            enabled = (!running.value && selectedTime.value != 0L),
                                            onClick = {
                                                countDownTimer.start()
                                                lastSelected.value = selectedTime.value
                                                lastDate.value = Date().time
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }