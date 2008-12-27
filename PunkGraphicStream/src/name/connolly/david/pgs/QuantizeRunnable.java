/*
 * QuantizeRunnable.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
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
import java.util.ArrayList;

public class QuantizeRunnable implements Runnable {
	private final ArrayList<SubtitleEvent> events;
	private final PendingRenderLock lock;
	private final int thread;
	private final int threadCount;

	public QuantizeRunnable(final ArrayList<SubtitleEvent> events,
			final PendingRenderLock pending, final int thread,
			final int threadCount) {
		this.events = events;
		this.thread = thread;
		this.threadCount = threadCount;
		lock = pending;
	}

	public void run() {
		try {
			SubtitleEvent event;
			BufferedImage indexed;
			BufferedImage image;

			for (int i = thread; i < events.size(); i = i + threadCount) {
				event = events.get(i);
				image = event.takeImage();

				indexed = NeuQuantQuantizer.indexImage(image);
				event.putImage(indexed);

				synchronized (lock) {
					lock.remove();
					lock.notify();
				}

				System.out.println("Quantized no.\t" + i);
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}