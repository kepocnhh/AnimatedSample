package test.android.animated

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
private fun v0() {
    AnimatedVisibility(
        visible = true,
    ) {
        // todo
    }
}

@Composable
private fun v1(
    initialValue: Float,
    targetValue: Float,
    duration: Duration,
    easing: Easing,
): Float {
    val animatable = remember {
        Animatable(initialValue, Float.VectorConverter, null, "foobarbaz")
    }
    LaunchedEffect(targetValue) {
        animatable.animateTo(targetValue, tween(durationMillis = duration.inWholeMilliseconds.toInt(), easing = easing))
    }
    return animatable.value
}

@Composable
private fun v2(
    backValue: Float,
    forwardValue: Float,
    duration: Duration,
    easing: Easing,
    isForward: Boolean,
): Float {
    val values = remember { mutableFloatStateOf(backValue) }
    val timeLeftState = remember { mutableLongStateOf(0) }
    LaunchedEffect(isForward) {
        val startedValue: Float
        val targetValue: Float
        if (isForward) {
            startedValue = backValue
            targetValue = forwardValue
        } else {
            startedValue = forwardValue
            targetValue = backValue
        }
        if (values.floatValue != targetValue) {
            val valuesDiff = targetValue - startedValue
            val timeNanos = duration.inWholeNanoseconds
            val timeNow = withFrameNanos { it }
            val timeStart = timeNow - timeLeftState.longValue
            while (true) {
                val timePassed = withFrameNanos { it - timeStart }
                if (timePassed < timeNanos) {
                    timeLeftState.longValue = timeNanos - timePassed
                    val fraction = timePassed.toFloat() / timeNanos
                    values.floatValue = startedValue + if (isForward) {
                        val multiplier = easing.transform(fraction = fraction)
                        valuesDiff * multiplier
                    } else {
                        val multiplier = easing.transform(fraction = 1 - fraction)
                        valuesDiff - valuesDiff * multiplier
                    }
                } else {
                    timeLeftState.longValue = 0
                    values.floatValue = targetValue
                    break
                }
            }
        }
    }
    return values.floatValue
}

@Composable
private fun animatedFloat(
    initialValue: Float,
    targetValue: Float,
    duration: Duration,
    easing: Easing,
): Float {
    val values = remember { mutableFloatStateOf(initialValue) }
    val targetValues = remember { AtomicReference(targetValue) }
    LaunchedEffect(targetValue) {
        targetValues.set(targetValue)
        val timeNanos = duration.inWholeNanoseconds.toFloat()
//        val timeStart = withFrameNanos { it }
        val timeStart = System.nanoTime() // todo
        val startedValue = values.floatValue
        val valuesDiff = targetValue - startedValue
        if (valuesDiff != 0f) {
            withContext(Dispatchers.Default) {
                while (true) {
                    if (targetValue != targetValues.get()) break
//                val timeDiff = withFrameNanos { it - timeStart }.toFloat()
                    val timeNow = System.nanoTime()
                    val timeDiff = timeNow.minus(timeStart).toFloat()
                    if (timeDiff < timeNanos) {
                        val fraction = timeDiff / timeNanos
                        values.floatValue = startedValue + valuesDiff * easing.transform(fraction)
                    } else {
                        values.floatValue = targetValue
                        break
                    }
                }
            }
        }
    }
    return values.floatValue
}

@Composable
internal fun AnimatedState.TestScreen(label: String, color: Color) {
    val targetValues = remember { mutableFloatStateOf(0f) }
    val currentValue = animatedFloat(
        initialValue = 0f,
        targetValue = targetValues.value,
        duration = 2.seconds,
        easing = LinearEasing,
        label = label,
    )
    val text = """
        label: $label
        current: $currentValue
        target: ${targetValues.floatValue}
    """.trimIndent()
    BasicText(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        text = text,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = currentValue)
                .background(color = color),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BasicText(
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clickable {
                    targetValues.value = 0f
                }
                .wrapContentSize(),
            text = "set target 0",
        )
        BasicText(
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clickable {
                    targetValues.value = 1f
                }
                .wrapContentSize(),
            text = "set target 1",
        )
    }
}

@Composable
private fun V2Screen(color: Color) {
    val directions = remember { mutableStateOf(false) }
    val isForward = directions.value
    val currentValue = v2(
        backValue = 0f,
        forwardValue = 1f,
        duration = 2.seconds,
//        easing = LinearEasing,
        easing = FastOutSlowInEasing,
        isForward = isForward,
    )
    val text = """
        current: $currentValue
        isForward: $isForward
    """.trimIndent()
    BasicText(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        text = text,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = currentValue)
                .background(color = color),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BasicText(
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clickable {
                    directions.value = false
                }
                .wrapContentSize(),
            text = "to back",
        )
        BasicText(
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clickable {
                    directions.value = true
                }
                .wrapContentSize(),
            text = "to forward",
        )
    }
}

@Composable
internal fun MainScreen() {
    val state = remember { AnimatedState() }
    val isLoading = state.loading.collectAsState().value
    LaunchedEffect(Unit) {
        state.loading.collect { isLoading ->
            println("loading: $isLoading")
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        ) {
            state.TestScreen(
                label = "red",
                color = Color.Red,
            )
            state.TestScreen(
                label = "green",
                color = Color.Green,
            )
            V2Screen(color = Color.Blue)
            BasicText(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(enabled = !isLoading) {
                        println("click")
                    }
                    .wrapContentSize(),
                text = "click",
            )
        }
    }
}
