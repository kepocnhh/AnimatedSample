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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

class AnimatedState {
    private val _animations = MutableStateFlow<Set<String>>(emptySet())
    val loading = object : StateFlow<Boolean> {
        override val value: Boolean get() = _animations.value.isNotEmpty()
        override val replayCache: List<Boolean> = emptyList()

        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            val value = AtomicBoolean(_animations.value.isNotEmpty())
            _animations.collect { labels ->
                val isLoading = labels.isNotEmpty()
                println("labels: ${labels.sorted()}")
                if (value.compareAndSet(!isLoading, isLoading)) {
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
}
