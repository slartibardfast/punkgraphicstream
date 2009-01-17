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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class Timecode implements Comparable<Timecode> {
	private long start;
	private long end;

	public int compareTo(Timecode o) {
		return new Long(start).compareTo(o.start);
	}

	public long getDuration() {
		return end - start;
	}

	public long getStart() {
		return start;
	}

	private Timecode() {

	}

	public Timecode(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public BigInteger getStartTicks(FrameRate fps) {
		long frameNumber = fps.startFrame(start);
		BigDecimal startTicks = BigDecimal.valueOf(frameNumber);
		
		startTicks = startTicks.multiply(BigDecimal.valueOf(fps.ticks()));
		
		return startTicks.round(MathContext.DECIMAL64).toBigInteger();
	}

	public long getEnd() {
		return end;
	}

	public BigInteger getEndTicks(FrameRate fps) {
		long frameNumber = fps.endFrame(end);
		BigDecimal endTicks = BigDecimal.valueOf(frameNumber);
		
		endTicks = endTicks.multiply(BigDecimal.valueOf(fps.ticks()));
		
		endTicks = endTicks.add(BigDecimal.valueOf(fps.ticks() - 1)); //
		
		return endTicks.round(MathContext.DECIMAL64).toBigInteger();
	}

	public Timecode merge(Timecode other) {
		if (other == null)
			throw new IllegalArgumentException(
					"Timecode other must not be null");
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		final Timecode other = (Timecode) obj;

		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return new Long(start).hashCode();
	}

	public boolean overlaps(Timecode other) {
		if (other == null)
			throw new IllegalArgumentException(
					"Timecode other must not be null");

		if (other.start >= start && other.start <= end)
			return true;
		else if (other.end >= start && other.end <= end)
			return true;
		else if (start >= other.start && start <= other.end)
			return true;
		else if (end >= other.start && end <= other.end)
			return true;
		return false;
	}
}
