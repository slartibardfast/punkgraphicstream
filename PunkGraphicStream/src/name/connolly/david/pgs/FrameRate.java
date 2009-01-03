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
	FILM, FILM_NTSC, TV_PAL, TV_NTSC, HD_PAL, HD_NTSC;

	public long frameDurationInMilliseconds() {
		double frame;
		final double secondInMilliseconds = 1000;

		switch (this) {
		case FILM:
			frame = 24d;
			return Math.round(secondInMilliseconds / frame);
		case FILM_NTSC:
			frame = 24000d / 1001d;
			return Math.round(secondInMilliseconds / frame);
		case TV_PAL:
			frame = 25;
			return Math.round(secondInMilliseconds / frame);
		case TV_NTSC:
			frame = 30000d / 1001d;
			return Math.round(secondInMilliseconds / frame);
		case HD_PAL:
			frame = 50;
			return Math.round(secondInMilliseconds / frame);
		case HD_NTSC:
			frame = 60000d / 1001d;
			return Math.round(secondInMilliseconds / frame);
		default:
			return 0;
		}
	}

	/**
	 * SupTicks are at a higher resolution than milliseconds
	 */
	public long frameDurationInSupTicks() {
		double frame;
		final double secondInSupTicks = 1000 * 90;

		switch (this) {
		case FILM:
			frame = 24d;
			return Math.round(secondInSupTicks / frame);
		case FILM_NTSC:
			frame = 24000d / 1001d;
			return Math.round(secondInSupTicks / frame);
		case TV_PAL:
			frame = 25;
			return Math.round(secondInSupTicks / frame);
		case TV_NTSC:
			frame = 30000d / 1001d;
			return Math.round(secondInSupTicks / frame);
		case HD_PAL:
			frame = 50;
			return Math.round(secondInSupTicks / frame);
		case HD_NTSC:
			frame = 60000d / 1001d;
			return Math.round(secondInSupTicks / frame);
		default:
			return 0;
		}
	}

	/**
	 * @returns Number of Frames between timecode & duration.
	 */
	public int getFrameCount(long timecode, long duration) {
		return 0;
	}
}