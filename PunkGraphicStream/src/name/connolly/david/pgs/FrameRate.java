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
	FILM(42, 3750), FILM_NTSC(42, 3754), TV_PAL(40, 3600), TV_NTSC(33, 3003), HD_PAL(
			20, 1800), HD_NTSC(17, 1502);

	private final int milliseconds;
	private final int ticks;

	FrameRate(int milliseconds, int ticks) {
		this.milliseconds = milliseconds;
		this.ticks = ticks;
	}

	/**
	 * @returns Number of Frames between timecode & duration.
	 */
	public int getFrameCount(long timecode, long duration) {
		return 0;
	}

	public long milliseconds() {
		return milliseconds;
	}

	public long ticks() {
		return ticks;
	}
}