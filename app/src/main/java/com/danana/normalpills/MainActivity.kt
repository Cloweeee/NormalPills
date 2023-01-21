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
    private fun getDates(): JSONArray {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val defaultValue = getString(R.string.dates_array_default)
        return JSONArray(sharedPref.getString(getString(R.string.dates_array), defaultValue))
    }

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

    @Composable
    fun TimeChip(timeString: String, timeMs: Long, selectedTime: MutableState<Long>) {
        FilterChip(
            modifier = Modifier.padding(all = 8.dp),
            selected = selectedTime.value == timeMs,
            onClick = {
                selectedTime.value = timeMs
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

                with(sharedPref.edit()) {
                    putLong(
                        getString(R.string.duration), selectedTime.value
                    )
                    apply()
                }
            },
            label = { Text(timeString) },
            leadingIcon = if (selectedTime.value == timeMs) {
                {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Localized Description",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else {
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConstraintLayout {
                        val (row, card) = createRefs()

                        val dateString = remember { mutableStateOf("") }
                        val selectedTime: MutableState<Long> = remember {
                            mutableStateOf(
                                sharedPref.getLong(
                                    getString(R.string.duration),
                                    0
                                )
                            )
                        }

                        val dates = getDates()
                        val lastDate: MutableState<Long> = remember { mutableStateOf(0) }
                        val lastSelected: MutableState<Long> = remember { mutableStateOf(0) }

                        if(dates.length() > 0) {
                            val recentDateObject: JSONObject = dates[dates.length() - 1] as JSONObject

                            lastDate.value = sdf.parse(recentDateObject.get("date").toString()).time
                            lastSelected.value = recentDateObject.get("selectedTime").toString().toLong()
                        } else {
                            lastDate.value = 0
                            lastSelected.value = selectedTime.value
                        }

                        val timeToUse: MutableState<Long> = remember { mutableStateOf(selectedTime.value) }
                        val timeSinceLast = Date().time.minus(lastDate.value)

                        val percentage: MutableState<Float> = remember { mutableStateOf(0f) }
                        val running = remember { mutableStateOf(false) }

                        val countDownTimer = object : CountDownTimer(timeToUse.value, 1000) {
                            override fun onTick(millisRemaining: Long) {
                                running.value = true
                                dateString.value = DateUtils.formatElapsedTime(
                                    StringBuilder("HH:MM:SS"), millisRemaining / 1000
                                )
                                percentage.value = (millisRemaining.toFloat()/lastSelected.value.toFloat())
                            }

                            override fun onFinish() {
                                running.value = false
                            }
                        }

                        if(timeSinceLast < lastSelected.value) {
                            timeToUse.value = lastSelected.value.minus(timeSinceLast)
                            countDownTimer.start()
                        }

                            if (!running.value) {
                                if(lastDate.value == 0L) {
                                    dateString.value = "never"
                                } else dateString.value = DateUtils.getRelativeTimeSpanString(lastDate.value).toString()
                            }


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(all = 24.dp)
                                    .constrainAs(ref = row) {
                                        bottom.linkTo(card.top)
                                    }
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                TimeChip(
                                    timeString = "1 hour",
                                    timeMs = 1 * 1000 * 60 * 60,
                                    selectedTime
                                )
                                TimeChip(
                                    timeString = "2 hours",
                                    timeMs = 2 * 1000 * 60 * 60,
                                    selectedTime
                                )
                                TimeChip(
                                    timeString = "4 hours",
                                    timeMs = 4 * 1000 * 60 * 60,
                                    selectedTime
                                )
                                TimeChip(
                                    timeString = "8 hours",
                                    timeMs = 8 * 1000 * 60 * 60,
                                    selectedTime
                                )
                            }
                            ElevatedCard(
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
                                    Box(modifier = Modifier
                                        .size(200.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxSize()) {
                                        val color = MaterialTheme.colorScheme.primary
                                        val lineColor = MaterialTheme.colorScheme.secondary
                                        val circleColor =
                                            MaterialTheme.colorScheme.secondaryContainer

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
                                                    percentage.value * 260f,
                                                    false,
                                                    style = Stroke(
                                                        12.dp.toPx(),
                                                        cap = StrokeCap.Round
                                                    )
                                                )
                                            }
                                        }
                                        val image = remember { mutableStateOf(R.drawable.adhd_creature) }
                                        if (running.value) {
                                            image.value = R.drawable.autism_creature
                                        } else {
                                            image.value = R.drawable.adhd_creature
                                        }
                                        Image(
                                            painterResource(id = image.value),
                                            contentDescription = "Autism!",
                                            contentScale = ContentScale.Fit,
                                            modifier = (Modifier
                                                .size(150.dp))
                                                .align(Alignment.Center)
                                                .fillMaxSize()
                                        )
                                    }


                                    Column(verticalArrangement = Arrangement.Center) {
                                        DateText(dateString = dateString, running)

                                        Button(
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                            enabled = (!running.value && selectedTime.value != 0L),
                                            onClick = {
                                                countDownTimer.start()
                                                lastSelected.value = selectedTime.value
                                                lastDate.value = Date().time
                                                // Write new date
                                                with(sharedPref.edit()) {
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
                                            Text(text = "Taking some normal pills")
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