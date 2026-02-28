package name.connolly.david.pgs;

import org.junit.Test;
import static org.junit.Assert.*;

public class FrameRateTest {

    @Test
    public void flickValuesArePositive() {
        for (FrameRate fr : FrameRate.values()) {
            assertTrue(fr.name() + " flicks should be positive", fr.flicks() > 0);
        }
    }

    @Test
    public void higherFpsHasSmallerFlickInterval() {
        // HD_NTSC (~60fps) should have smaller flick interval than FILM (~24fps)
        assertTrue(FrameRate.HD_NTSC.flicks() < FrameRate.FILM.flicks());
        assertTrue(FrameRate.HD_PAL.flicks() < FrameRate.TV_PAL.flicks());
    }

    @Test
    public void clampRoundsToFrameBoundaries() {
        FrameRate fr = FrameRate.FILM;
        long oneFrame = fr.flicks();

        // Timecode starting mid-frame should clamp down to frame start
        Timecode t = Timecode.fromFlicks(oneFrame + 100, oneFrame * 3 - 100);
        Timecode clamped = fr.clamp(t);

        assertEquals(oneFrame, clamped.getStart());
        assertEquals(oneFrame * 3, clamped.getEnd());
    }

    @Test
    public void clampExactFrameBoundaryUnchanged() {
        FrameRate fr = FrameRate.TV_PAL;
        long oneFrame = fr.flicks();

        Timecode t = Timecode.fromFlicks(oneFrame * 2, oneFrame * 5);
        Timecode clamped = fr.clamp(t);

        assertEquals(oneFrame * 2, clamped.getStart());
        assertEquals(oneFrame * 5, clamped.getEnd());
    }

    @Test
    public void clampSingleFrameEdgeCase() {
        FrameRate fr = FrameRate.FILM;
        long oneFrame = fr.flicks();

        // Start and end in the same frame → should extend to at least one frame
        Timecode t = Timecode.fromFlicks(oneFrame + 1, oneFrame + 2);
        Timecode clamped = fr.clamp(t);

        assertEquals(oneFrame, clamped.getStart());
        assertEquals(oneFrame * 2, clamped.getEnd());
    }

    @Test
    public void flicksToMillisecondsConversion() {
        FrameRate fr = FrameRate.FILM;
        // 705600 flicks = 1 ms
        assertEquals(1, fr.flicksToMilliseconds(705600));
        assertEquals(1000, fr.flicksToMilliseconds(705600000));
    }

    @Test
    public void allFrameRatesHaveReasonableFps() {
        for (FrameRate fr : FrameRate.values()) {
            assertTrue(fr.name() + " fps should be > 20", fr.fps() > 20);
            assertTrue(fr.name() + " fps should be < 70", fr.fps() < 70);
        }
    }
}
