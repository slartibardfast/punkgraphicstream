/*
 * FrameRate.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
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

import java.math.BigDecimal;

public enum FrameRate {
    FILM(29429400, 23.976023976024, 41.708333333333329, 3750),
    FILM_NTSC(29400000, 24, 41.666666666666664, 3753.75),
    TV_PAL(28224000, 25, 40, 3600),
    TV_NTSC(23543520, 29.97002997003, 33.366666666666667, 3003),
    HD_PAL(14112000, 50, 20, 1800),
    HD_NTSC(11771760, 59.9400599400599, 16.683333333333334, 1501.5);

    private final long flicks;
    private final BigDecimal milliseconds;
    private final BigDecimal ticks;
    private final double fps;
    
    
    // Flick LCM 705600000 is thanks to Facebook
    //
    // 1 second in milliseconds = 1000
    // 1 second in ticks = 90000
    // 1 second in flicks = 705600000
    // 1 milliseconds in ticks = 90
    // 1 milliseconds in flicks = 705600
    // 1 tick in flicks = 7840
    /**
     *
     * @param fps Frames Per Second
     * @param milliseconds Duration in milliseconds
     * @param ticks Duration in sup ticks
     */
    FrameRate(long flicks, double fps, double milliseconds, double ticks) {
        this.flicks = flicks; 
        
        // less usefully true:
        this.milliseconds = BigDecimal.valueOf(milliseconds);
        this.ticks = BigDecimal.valueOf(ticks);
        this.fps = fps;
    }

    public double milliseconds() {
        return milliseconds.doubleValue();
    }

    public double ticks() {
        return ticks.doubleValue();
    }

    public long flicks() {
        return this.flicks;
    }
    
    // returns in flicks
    public Timecode clamp(Timecode timecode) {
        final long startFlick = timecode.getStart();
        final long endFlick = timecode.getEnd();

        long firstFlick = (startFlick / this.flicks) * this.flicks;
        long lastFlick = endFlick % this.flicks == 0 ? 
            (endFlick / this.flicks) * this.flicks : 
            ((endFlick / this.flicks) * this.flicks) + this.flicks;

        // unlikely but possible - show for single frame
        if (firstFlick == lastFlick) {
            lastFlick = firstFlick + this.flicks;
        }

        return Timecode.fromFlicks(firstFlick, lastFlick);
    }

    public long flicksToMilliseconds(long flicks){
        long milliseconds = flicks / 705600;
        return milliseconds;
    }

    public double fps() {
        return fps;
    }
}
