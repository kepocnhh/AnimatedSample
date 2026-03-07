package test.android.animated

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

class AnimatedState {
    private val _animations = MutableStateFlow<Set<String>>(emptySet())
    val loading = object : StateFlow<Boolean> {
        override val value: Boolean get() = _animations.value.isNotEmpty()
        override val replayCache: List<Boolean> = emptyList()

        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            var value: Boolean? = null
            _animations.collect { labels ->
                val isLoading = labels.isNotEmpty()
                if (isLoading != value) {
                    value = isLoading
                    collector.emit(isLoading)
                }
            }
        }
    }

    @Composable
    fun animatedFloat(
        initialValue: Float,
        targetValue: Float,
        duration: Duration,
        easing: Easing,
        label: String,
    ): Float {
        val values = remember { mutableFloatStateOf(initialValue) }
        val targetValues = remember { AtomicReference(targetValue) }
        LaunchedEffect(label, targetValue) {
            targetValues.set(targetValue)
            val timeNanos = duration.inWholeNanoseconds.toFloat()
            val timeStart = withFrameNanos { it }
//            val timeStart = System.nanoTime() // todo
            val startedValue = values.floatValue
            val valuesDiff = targetValue - startedValue
            if (valuesDiff != 0f) {
                _animations.value += label
                withContext(Dispatchers.Default) {
                    while (targetValue == targetValues.get()) {
                        val timeDiff = withFrameNanos { it - timeStart }.toFloat()
//                        val timeNow = System.nanoTime()
//                        val timeDiff = timeNow.minus(timeStart).toFloat()
                        if (timeDiff < timeNanos) {
                            val fraction = timeDiff / timeNanos
                            values.floatValue = startedValue + valuesDiff * easing.transform(fraction)
                        } else {
                            values.floatValue = targetValue
                            _animations.value -= label
                            break
                        }
                    }
                }
            }
        }
        return values.floatValue
    }

    @Composable
    fun animatedFloat(
        duration: Duration,
        easing: Easing,
        isForward: Boolean,
    ): Float {
        val values = remember { mutableFloatStateOf(0f) }
        val timeLeftState = remember { AtomicLong(0L) }
        val durations = remember { AtomicReference(duration) }
        val easingState = remember { AtomicReference(easing) }
        val ids = remember { AtomicReference(UUID.randomUUID().toString()) }
        LaunchedEffect(duration, easing, isForward) {
            val timeLeft: Long
            val currentValue: Float
            if (duration != durations.get() || easing != easingState.get()) {
                durations.set(duration)
                easingState.set(easing)
                timeLeft = 0
                currentValue = 0f
            } else {
                timeLeft = timeLeftState.get()
                currentValue = values.floatValue
            }
            val targetValue = if (isForward) 1f else 0f
            if (currentValue != targetValue) {
                val timeNanos = duration.inWholeNanoseconds
                val timeNow = withFrameNanos { it }
                val timeStart = timeNow - timeLeft
                _animations.value += ids.get()
                while (true) {
                    val timePassed = withFrameNanos { it - timeStart }
                    if (timePassed < timeNanos) {
                        timeLeftState.set(timeNanos - timePassed)
                        val fraction = if (isForward) {
                            timePassed.toFloat().div(timeNanos)
                        } else {
                            timeNanos.minus(timePassed).toFloat().div(timeNanos)
                        }
                        values.floatValue = easing.transform(fraction = fraction)
                    } else {
                        timeLeftState.set(0)
                        values.floatValue = targetValue
                        break
                    }
                }
            }
            _animations.value -= ids.get()
        }
        return values.floatValue
    }
}
