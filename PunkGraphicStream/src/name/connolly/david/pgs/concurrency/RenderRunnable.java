/*
 * Render.java
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

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.TreeSet;

import name.connolly.david.pgs.util.ProgressSink;
import name.connolly.david.pgs.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RenderRunnable implements Runnable {

	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final String inputFilename;
	private final String outputFilename;
	private final FrameRate fps;
	private final ProgressSink progress;
	private final Resolution resolution;

	public RenderRunnable(String inputFilename, String outputFilename,
			FrameRate fps, Resolution resolution, ProgressSink progress) {
		this.inputFilename = inputFilename;
		this.outputFilename = outputFilename;
		this.fps = fps;
		this.progress = progress;
		this.resolution = resolution;
	}

	public void run() {
		final Render r = Render.INSTANCE;
		final int quantizeThreadCount = Runtime.getRuntime().availableProcessors();
		int eventIndex = 0;
		int eventCount;
		int percentage;
		final int renderCount = quantizeThreadCount * 2 - 1;
		final BlockingQueue<SubtitleEvent> quantizeQueue;
		final BlockingQueue<SubtitleEvent> encodeQueue;
		final TreeSet<Timecode> timecodes = new TreeSet<Timecode>();
		final AtomicBoolean renderPending = new AtomicBoolean(true);
		final Semaphore quantizePending = new Semaphore(quantizeThreadCount);
		int x;
		int y;

		switch (resolution) {
		case NTSC_480p:
			x = 720;
			y = 480;
			break;
		case PAL_576p:
			x = 720;
			y = 576;
			break;
		case HD_720p:
			x = 1280;
			y = 720;
			break;
		case HD_1080p:
		default: 
			x = 1920;
		y = 1080;
		break;
		}

		try {
			r.openSubtitle(inputFilename, x, y);

			eventCount = r.getEventCount();

			quantizeQueue = new LinkedBlockingQueue<SubtitleEvent>(renderCount);
			encodeQueue = new LinkedBlockingQueue<SubtitleEvent>();

			for (int cpu = 0; cpu < quantizeThreadCount; cpu++) {
				new Thread(new QuantizeRunnable(quantizeQueue, encodeQueue,
						renderPending, quantizePending, progress)).start();
			}

			new Thread(new EncodeRunnable(encodeQueue, outputFilename, fps,
					quantizeThreadCount, quantizePending, progress)).start();

			processTimecodes(eventCount, r, timecodes);

			eventCount = timecodes.size();

			Iterator<Timecode> i = timecodes.iterator();

			// Timecode loop: build subtitle event for timecode
			while (i.hasNext()) {
				Timecode timecode = i.next();
				SubtitleEvent event = new SubtitleEvent(timecode);
				percentage = Math.round((float) eventIndex / eventCount * 100f);
				progress.progress(percentage,
						"Rendering Event No. " + eventIndex + " of " + eventCount);

				// Render loop: render, check for change, split on change
				while (event != null && !cancelled.get()) {
					SubtitleEvent nextEvent = null;
					final BufferedImage image = new BufferedImage(x, y,
							BufferedImage.TYPE_INT_ARGB);
					int change = 0;
					long changeTimecode = event.getTimecode().getStart();



					// Change Detect Loop: Check each frame to see if animation has
					// occurred
					while (changeTimecode < (timecode.getEnd() - 1)
							&& (change == 0 || (changeTimecode
									- event.getTimecode().getStart() < fps
									.frameDurationInMilliseconds()))) {
						changeTimecode++;
						change = r.changeDetect(changeTimecode);
					}

					if (change > 0) {
						long lastTimecode = changeTimecode - 1;

						event.getTimecode().setDuration(lastTimecode - event.getTimecode().getStart());

						nextEvent = new SubtitleEvent(new Timecode(changeTimecode, timecode.getEnd()));
					}

					r.render(image, event.getRenderTimecode());

					event.putImage(image);

					try {
						quantizeQueue.put(event);
					} catch (InterruptedException ex) {
						Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE, null, ex);
					}

					event = nextEvent;
				}

				eventIndex++;
			}

			renderPending.set(false);

			r.closeSubtitle();
		} catch (RenderException ex) {
			progress.fail(ex.getMessage());
			Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void cancel() {
		cancelled.set(true);
	}

	private void processTimecodes(int eventCount, final Render r, final TreeSet<Timecode> timecodes) throws RenderException {
		for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
			Timecode timecode = r.getEventTimecode(eventIndex); // FIXME: Native Code
			Iterator<Timecode> i = timecodes.iterator();

			while (i.hasNext()) {
				Timecode other = i.next();
				if (timecode.overlaps(other)) {
					i.remove();
					timecode = timecode.merge(other);
				}
			}
			timecodes.add(timecode);
		}
	}
}
