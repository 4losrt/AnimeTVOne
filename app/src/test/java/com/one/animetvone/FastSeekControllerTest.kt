package com.one.animetvone

import com.one.animetvone.ui.player.FastSeekController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the remote fast-forward/rewind accumulator.
 *
 * The two guarantees worth locking down are: a burst of presses compounds from
 * the position the burst started at (ExoPlayer's position lags behind `seekTo`,
 * so anchoring on it would swallow most of a held D-pad), and a held D-pad's
 * ultra-fast key auto-repeat cannot blow through the whole episode instantly.
 */
class FastSeekControllerTest {

    private val duration = 1_440_000L // 24 minutes
    private val anchor = 300_000L // 5 minutes

    @Test
    fun singlePressMovesByTheBaseStep() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        val (target, delta) = controller.press(1, anchor, duration, nowMs = 0L)!!

        assertEquals(anchor + 10_000L, target)
        assertEquals(10_000L, delta)
    }

    @Test
    fun keyAutoRepeatFasterThanTheFloorIsIgnored() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        controller.press(1, anchor, duration, nowMs = 0L)
        // A held key repeats every ~50ms; those extra presses must not each add
        // another step.
        assertNull(controller.press(1, anchor, duration, nowMs = 50L))
        assertNull(controller.press(1, anchor, duration, nowMs = 120L))
        // Past the floor a fresh step is accepted again.
        assertEquals(30_000L, controller.press(1, anchor, duration, nowMs = 500L)!!.second)
    }

    @Test
    fun repeatedTapsCompoundFromTheBurstAnchor() {
        val controller = FastSeekController(baseStepMs = 10_000L)
        // ExoPlayer reports the *same* stale position for every press of the
        // burst; a naive implementation anchored on it would barely move.
        val stalePosition = anchor

        val first = controller.press(1, stalePosition, duration, nowMs = 0L)!!.first
        val second = controller.press(1, stalePosition, duration, nowMs = 500L)!!.first
        val third = controller.press(1, stalePosition, duration, nowMs = 1_000L)!!.first

        assertEquals(anchor + 10_000L, first)
        assertEquals(anchor + 30_000L, second)
        assertEquals(anchor + 60_000L, third)
    }

    @Test
    fun holdingTheKeyRampsTheStepUp() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        // A held key repeats steadily (~300ms apart here). The step is driven by
        // how long the burst has lasted, so it grows instead of staying flat.
        fun cumulativeAt(nowMs: Long) =
            controller.press(1, anchor, duration, nowMs = nowMs)!!.first - anchor

        assertEquals(10_000L, cumulativeAt(0L)) // 0-400ms    -> 1x
        assertEquals(20_000L, cumulativeAt(300L)) // +1x
        assertEquals(40_000L, cumulativeAt(600L)) // 400-900ms -> +2x
        assertEquals(70_000L, cumulativeAt(900L)) // 900ms-1.6s -> +3x
        assertEquals(100_000L, cumulativeAt(1_200L)) // +3x
        assertEquals(150_000L, cumulativeAt(1_600L)) // 1.6-2.6s  -> +5x
    }

    @Test
    fun aPauseLongerThanTheSessionGapStartsANewBurst() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        controller.press(1, anchor, duration, nowMs = 0L)
        // A pause later a new burst anchors on the *current* position.
        val (target, delta) = controller.press(1, anchor + 40_000L, duration, nowMs = 3_000L)!!

        assertEquals(anchor + 50_000L, target)
        assertEquals(10_000L, delta)
    }

    @Test
    fun changingDirectionRestartsTheAcceleration() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        controller.press(1, anchor, duration, nowMs = 0L)
        controller.press(1, anchor, duration, nowMs = 500L) // would ramp up

        val (target, delta) = controller.press(-1, anchor, duration, nowMs = 800L)!!

        assertEquals(anchor - 10_000L, target)
        assertEquals(-10_000L, delta)
    }

    @Test
    fun seeksAreClampedToTheMediaBounds() {
        // Fast-forward past the end of the media.
        val nearEnd = FastSeekController(baseStepMs = 10_000L)
        nearEnd.press(1, duration - 5_000L, duration, nowMs = 0L)
        val (endTarget, endDelta) = nearEnd.press(1, duration - 5_000L, duration, nowMs = 500L)!!

        assertEquals(duration, endTarget)
        assertEquals(5_000L, endDelta)

        // Rewind past the start of the media.
        val nearStart = FastSeekController(baseStepMs = 10_000L)
        nearStart.press(-1, 2_000L, duration, nowMs = 0L)
        val (startTarget, startDelta) = nearStart.press(-1, 2_000L, duration, nowMs = 500L)!!

        assertEquals(0L, startTarget)
        assertEquals(-2_000L, startDelta)
    }

    @Test
    fun resetForgetsTheBurst() {
        val controller = FastSeekController(baseStepMs = 10_000L)

        controller.press(1, anchor, duration, nowMs = 0L)
        controller.reset()

        val (target, _) = controller.press(1, anchor, duration, nowMs = 50L)!!
        assertEquals(anchor + 10_000L, target)
    }
}
