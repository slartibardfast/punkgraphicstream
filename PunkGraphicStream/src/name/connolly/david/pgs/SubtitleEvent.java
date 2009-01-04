/*
 * SubtitleEvent.java
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

import java.awt.image.BufferedImage;
import java.math.BigInteger;

public class SubtitleEvent implements Comparable<SubtitleEvent> {
	private static int eventCount = 0;

	private long eventDuration;
	private long eventTimecode;
	private final int frameCount;
	private FrameRate frameRate;
	private final long id;

	private BufferedImage image;
	private final Object imageLock = new Object();
	private BufferedImage indexed;
	private final Object indexedLock = new Object();

	/**
	 * SubtitleEvent must be carefully initialised in order of occurrence!
	 */
	public SubtitleEvent(final long timecode, final long duration) {
		eventDuration = duration;
		eventTimecode = timecode;

		id = eventCount;
		frameRate = null;
		frameCount = 0;

		eventCount++;
	}

	/**
	 * SubtitleEvent must be carefully initialised in order of occurrence! This
	 * constructor is for Animation mode
	 */
	public SubtitleEvent(final SubtitleEvent lastEvent,
			final FrameRate frameRate, final int frameCount) {
		eventDuration = lastEvent.eventDuration;
		eventTimecode = lastEvent.eventTimecode;
		this.frameRate = frameRate;

		id = eventCount;
		this.frameCount = frameCount;

		eventCount++;
	}

	public int compareTo(SubtitleEvent o) {
		return new Long(eventTimecode).compareTo(o.eventTimecode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SubtitleEvent other = (SubtitleEvent) obj;
		if (eventTimecode != other.eventTimecode)
			return false;
		return true;
	}

	public long getDuration() {
		return eventDuration;
	}

	public BigInteger getEndTimecode() {
		BigInteger endTimecode = BigInteger.valueOf(getRenderTimecode());
		endTimecode = endTimecode.multiply(BigInteger.valueOf(90));

		if (frameRate != null) {
			endTimecode = endTimecode.add(BigInteger.valueOf(frameRate
					.frameDurationInSupTicks()));
		} else {
			BigInteger duration = BigInteger.valueOf(getDuration());
			duration = duration.multiply(BigInteger.valueOf(90));
			endTimecode = endTimecode.add(duration);
		}

		return endTimecode;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public FrameRate getFrameRate() {
		return frameRate;
	}

	public long getId() {
		return id;
	}

	public long getRenderTimecode() {
		if (frameRate != null)
			return eventTimecode + frameRate.frameDurationInMilliseconds()
					* frameCount;

		return eventTimecode;
	}

	public BigInteger getStartTimecode() {
		BigInteger timecode = BigInteger.valueOf(eventTimecode);
		timecode = timecode.multiply(BigInteger.valueOf(90));

		if (frameRate != null)
			return timecode.add(BigInteger.valueOf(frameRate
					.frameDurationInSupTicks()
					* frameCount));

		return timecode;
	}

	public long getTimecode() {
		return eventTimecode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (eventTimecode ^ eventTimecode >>> 32);
		return result;
	}

	public boolean isFrame() {
		return frameRate != null;
	}

	public void putImage(final BufferedImage image) {
		if (this.image != null)
			throw new RuntimeException("Image already initialized");

		this.image = image;

		synchronized (imageLock) {
			imageLock.notify();
		}
	}

	public void putIndexed(final BufferedImage indexed) {
		if (this.indexed != null)
			throw new RuntimeException("Indexed already initialized");

		this.indexed = indexed;

		synchronized (indexedLock) {
			indexedLock.notify();
		}
	}

	public void setDuration(final long duration) {
		eventDuration = duration;
	}

	public void setFrameRate(FrameRate frameRate) {
		this.frameRate = frameRate;
	}

	public void setTimecode(final long start) {
		eventTimecode = start;
	}

	public BufferedImage takeImage() throws InterruptedException {
		if (image == null) {
			synchronized (imageLock) {
				imageLock.wait();
			}
		}

		final BufferedImage image = this.image;

		this.image = null;

		return image;
	}

	public BufferedImage takeIndexed() throws InterruptedException {
		if (indexed == null) {
			synchronized (indexedLock) {
				indexedLock.wait();
			}
		}
		BufferedImage indexed;

		indexed = this.indexed;

		this.indexed = null;

		return indexed;
	}

	@Override
	public String toString() {
		return "SubtitleEvent start: " + eventTimecode + " duration: "
				+ eventDuration;
	}
}
