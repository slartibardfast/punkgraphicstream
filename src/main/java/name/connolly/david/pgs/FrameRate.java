/*
 * FrameRate.java
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

public enum FrameRate {
    FILM(23.976023976024, 41.708333333333329, 3750),
    FILM_NTSC(24, 41.666666666666664, 3753.75),
    TV_PAL(25, 40, 3600),
    TV_NTSC(29.97002997003, 33.366666666666667, 3003),
    HD_PAL(50, 20, 1800),
    HD_NTSC(59.9400599400599, 16.683333333333334, 1501.5),
    ANIMATED(100, 10, 900);
    private final double milliseconds;
    private final double ticks;
    private final double fps;

    /**
     *
     * @param fps Frames Per Second
     * @param milliseconds Duration in milliseconds
     * @param ticks Duration in sup ticks
     */
    FrameRate(double fps, double milliseconds, double ticks) {
        this.milliseconds = milliseconds;
        this.ticks = ticks;
        this.fps = fps;
    }

    /**
     *
     * @param timecode in millisecond
     * @return Number of Frame
     */
    public long startFrame(double start) {
        return (long) Math.round(start / milliseconds);
    }

    public long endFrame(double end) {
        return (long) Math.round(end / milliseconds);
    }

    public double milliseconds() {
        return milliseconds;
    }

    public double ticks() {
        return ticks;
    }

    public double fps() {
        return fps;
    }
}
