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

public class SubtitleEvent {
	private static int eventCount = 0;
	private long duration;
	private long id;
	private BufferedImage image;
	private final Object imageLock = new Object();
	private BufferedImage indexed;
	private final Object indexedLock = new Object();
	private long timecode;
	/**
	 * SubtitleEvent must be carefully initialised in order of occurrence!
	 */
	public SubtitleEvent(final long timecode, final long duration) {
		super();
		this.duration = duration;
		this.timecode = timecode;
		
		id = eventCount;
		
		eventCount++;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubtitleEvent other = (SubtitleEvent) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public long getId() {
		return id;
	}

	public long getDuration() {
		return duration;
	}

	public long getTimecode() {
		return timecode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
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
		this.duration = duration;
	}

	public void setTimecode(final long start) {
		timecode = start;
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
		return "SubtitleEvent start: " + timecode + " duration: "
				+ duration;
	}
}
