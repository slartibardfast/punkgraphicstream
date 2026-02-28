package name.connolly.david.pgs;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimecodeTest {

    @Test
    public void fromFlicksPreservesValues() {
        Timecode t = Timecode.fromFlicks(1000, 2000);
        assertEquals(1000, t.getStart());
        assertEquals(2000, t.getEnd());
    }

    @Test
    public void fromMillisecondsConvertsToFlicks() {
        // 1 ms = 705600 flicks
        Timecode t = Timecode.fromMilliseconds(1, 2);
        assertEquals(705600, t.getStart());
        assertEquals(705600 * 2, t.getEnd());
    }

    @Test
    public void getDurationReturnsEndMinusStart() {
        Timecode t = Timecode.fromFlicks(100, 500);
        assertEquals(400, t.getDuration());
    }

    @Test
    public void getStartTicksDividesByTickSize() {
        // 1 tick = 7840 flicks
        Timecode t = Timecode.fromFlicks(78400, 156800);
        assertEquals(10, t.getStartTicks());
    }

    @Test
    public void getEndTicksUsesEnd() {
        Timecode t = Timecode.fromFlicks(78400, 156800);
        assertEquals(20, t.getEndTicks());
    }

    @Test
    public void mergeUsesMinStartMaxEnd() {
        Timecode a = Timecode.fromFlicks(100, 500);
        Timecode b = Timecode.fromFlicks(200, 800);
        Timecode merged = a.merge(b);
        assertEquals(100, merged.getStart());
        assertEquals(800, merged.getEnd());
    }

    @Test
    public void mergeReversedOrder() {
        Timecode a = Timecode.fromFlicks(200, 800);
        Timecode b = Timecode.fromFlicks(100, 500);
        Timecode merged = a.merge(b);
        assertEquals(100, merged.getStart());
        assertEquals(800, merged.getEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mergeNullThrows() {
        Timecode.fromFlicks(0, 100).merge(null);
    }

    @Test
    public void overlapsWhenSecondStartsInFirst() {
        Timecode a = Timecode.fromFlicks(100, 500);
        Timecode b = Timecode.fromFlicks(300, 800);
        assertTrue(a.overlaps(b));
    }

    @Test
    public void overlapsWhenSecondEndsInFirst() {
        Timecode a = Timecode.fromFlicks(300, 800);
        Timecode b = Timecode.fromFlicks(100, 500);
        assertTrue(a.overlaps(b));
    }

    @Test
    public void overlapsWhenContained() {
        Timecode a = Timecode.fromFlicks(100, 800);
        Timecode b = Timecode.fromFlicks(200, 500);
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    public void noOverlapWhenDisjoint() {
        Timecode a = Timecode.fromFlicks(100, 200);
        Timecode b = Timecode.fromFlicks(300, 400);
        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void overlapsNullThrows() {
        Timecode.fromFlicks(0, 100).overlaps(null);
    }

    @Test
    public void compareToByStart() {
        Timecode a = Timecode.fromFlicks(100, 500);
        Timecode b = Timecode.fromFlicks(200, 300);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void equalsBasedOnStart() {
        Timecode a = Timecode.fromFlicks(100, 500);
        Timecode b = Timecode.fromFlicks(100, 800);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualsDifferentStart() {
        Timecode a = Timecode.fromFlicks(100, 500);
        Timecode b = Timecode.fromFlicks(200, 500);
        assertFalse(a.equals(b));
    }
}
