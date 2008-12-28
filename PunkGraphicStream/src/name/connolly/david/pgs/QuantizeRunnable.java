/*
 * QuantizeRunnable.java
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuantizeRunnable implements Runnable {
	private final BlockingQueue<SubtitleEvent> quantizeQueue;
	private final BlockingQueue<SubtitleEvent> encodeQueue;
	private final AtomicBoolean renderPending;
	private final Semaphore quantizePending;
	
	public QuantizeRunnable(final BlockingQueue<SubtitleEvent> quantizeQueue,
			final BlockingQueue<SubtitleEvent> encodeQueue, final AtomicBoolean renderPending, final Semaphore quantizePending) {
		this.quantizeQueue = quantizeQueue;
		this.encodeQueue = encodeQueue;
		this.renderPending = renderPending;
		this.quantizePending = quantizePending;
	}

	public void run() {
		try {
			SubtitleEvent event;
			BufferedImage indexed;
			BufferedImage image;
			
			quantizePending.acquire();
			
			while (renderPending.get() || quantizeQueue.size() > 0 ) {
				event = quantizeQueue.take();
				image = event.takeImage();

				indexed = NeuQuantQuantizer.indexImage(image);
				
				event.putImage(indexed);
				
				System.out.println("Quantized Frame No. " + event.getId());
				encodeQueue.put(event);
			}
			
			quantizePending.release();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}