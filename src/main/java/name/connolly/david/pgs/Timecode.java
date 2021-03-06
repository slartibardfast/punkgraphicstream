/*
 * Timecode.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This file is part of PunkGraphicStream.
 *
 * PunkGraphicStream is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package name.connolly.david.pgs;

public class Timecode implements Comparable<Timecode> {
    private long start;
    private long end;

    @Override
    public int compareTo(Timecode o) {
        return Long.valueOf(start).compareTo(o.start);
    }

    @Override
    public String toString() {
        return "Timecode start: " + start + " end " + end;
    }

    public static Timecode fromFlicks(long start, long end) {
        Timecode t = new Timecode();
        t.start = start;
        t.end = end;
        return t;
    }

    public static Timecode fromMilliseconds(long start, long end) {
        Timecode t = new Timecode(start, end);
        return t;
    }


    public long getDuration() {
        return end - start;
    }

    public long getStart() {
        return start;
    }

    public long getStartTicks() {
        return start / 7840;
    }

    private Timecode() {
    }

    public Timecode(long startMs, long endMs) {
        /* libpgs-jni works in ms only */
        this.start = startMs * 705600;
        this.end = endMs * 705600;
    }

    public long getEnd() {
        return end;
    }

    public long getEndTicks() {
        return start / 7840;
    }
    
    public Timecode merge(Timecode other) {
        if (other == null) {
            throw new IllegalArgumentException(
                    "Timecode other must not be null");
        }
        final Timecode merged = new Timecode();

        // First occuring Start
        if (start <= other.start) {
            merged.start = start;
        } else {
            merged.start = other.start;
        }

        // Last occuring End
        if (end >= other.end) {
            merged.end = end;
        } else {
            merged.end = other.end;
        }

        return merged;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final Timecode other = (Timecode) obj;

        if (start != other.start) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(start).hashCode();
    }

    public boolean overlaps(Timecode other) {
        if (other == null) {
            throw new IllegalArgumentException(
                    "Timecode other must not be null");
        }

        if (other.start >= start && other.start <= end) {
            return true;
        } else if (other.end >= start && other.end <= end) {
            return true;
        } else if (start >= other.start && start <= other.end) {
            return true;
        } else if (end >= other.start && end <= other.end) {
            return true;
        }
        return false;
    }
}
