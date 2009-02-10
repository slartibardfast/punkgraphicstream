/*
 * EncodeRunnable.java
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
package name.connolly.david.pgs.concurrency;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.SubtitleEvent;
import name.connolly.david.pgs.SupGenerator;
import name.connolly.david.pgs.util.ProgressSink;

public class EncodeRunnable implements Runnable {
	private final BlockingQueue<SubtitleEvent> encodeQueue;
	private final String filename;
	private final FrameRate fps;
	private final int quantizeThreadCount;
	private final Semaphore quantizePending;
	private final ProgressSink progress;

	public EncodeRunnable(final BlockingQueue<SubtitleEvent> encodeQueue,
			final String filename, final FrameRate fps,
			final int quantizeThreadCount, final Semaphore quantizePending,
			final ProgressSink progress) {
		this.encodeQueue = encodeQueue;
		this.filename = filename;
		this.fps = fps;
		this.quantizeThreadCount = quantizeThreadCount;
		this.quantizePending = quantizePending;
		this.progress = progress;
	}

	public void run() {
		OutputStream os = null;
        

        try {
            SubtitleEvent event;
			final SupGenerator packet;
			long encodeIndex = 0;
			boolean quantizeThreadsActive = quantizePending
					.tryAcquire(quantizeThreadCount) == false;
			boolean encodePending = encodeQueue.size() > 0;
            os = new BufferedOutputStream(new FileOutputStream(filename));
            packet = new SupGenerator(os, fps);
            
			// Continue while at least one quantizeThread is 
			// running or queue is not empty.
			while (quantizeThreadsActive || encodePending) {
				event = encodeQueue.take();

				// If out of sequence, pause & add to the end of the queue
				if (event.getId() != encodeIndex) {
					if (encodeQueue.size() == 0) {
						Thread.sleep(100);
					}

					encodeQueue.put(event);
					continue;
				}

				packet.addBitmap(event);

				encodePending = encodeQueue.size() > 0;

				quantizeThreadsActive = quantizePending
						.tryAcquire(quantizeThreadCount) == false;

				if (!quantizeThreadsActive) {
					quantizePending.release(quantizeThreadCount); // For Next Run																 
				}

				encodeIndex++;
			}

			os.flush();
		} catch (final IOException ex) {
			progress.fail(ex.getMessage());
			Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final InterruptedException ex) {
			progress.fail(ex.getMessage());
			Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE,
					null, ex);
		} finally {
			try {
				os.close();
			} catch (final IOException ex) {
				progress.fail(ex.getMessage());
				Logger.getLogger(EncodeRunnable.class.getName()).log(
						Level.SEVERE, null, ex);
			} finally {
                SubtitleEvent.lastEvent();

                progress.done();
            }
		}
	}
}