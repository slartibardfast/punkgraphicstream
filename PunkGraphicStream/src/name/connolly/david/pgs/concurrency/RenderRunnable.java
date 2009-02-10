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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.Render;
import name.connolly.david.pgs.RenderException;
import name.connolly.david.pgs.Resolution;
import name.connolly.david.pgs.SubtitleEvent;
import name.connolly.david.pgs.Timecode;
import name.connolly.david.pgs.SubtitleEvent.SubtitleType;
import name.connolly.david.pgs.util.ProgressSink;

public class RenderRunnable implements Runnable {
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final String inputFilename;
	private final FrameRate fps;
	private final ProgressSink progress;
	private final Render renderer = Render.INSTANCE;
	private int quantizeThreadCount = Runtime.getRuntime().availableProcessors();
	private int renderAheadCount = 4;
	private final BlockingQueue<SubtitleEvent> quantizeQueue;
	private final BlockingQueue<SubtitleEvent> encodeQueue;
	private final TreeSet<Timecode> timecodes = new TreeSet<Timecode>();
	private final AtomicBoolean renderPending = new AtomicBoolean(true);
	private final Semaphore quantizePending = new Semaphore(quantizeThreadCount);
	private final int x;
	private final int y;

	public RenderRunnable(String inputFilename, String outputFilename,
			FrameRate fps, Resolution resolution, ProgressSink progress) {
		this.inputFilename = inputFilename;
		this.fps = fps;
		this.progress = progress;

        // Be cautious until can test on quad core system with 256mb heap
        if (quantizeThreadCount > 2) {
            quantizeThreadCount = 2;
        }
        
		quantizeQueue = new LinkedBlockingQueue<SubtitleEvent>(renderAheadCount);
		encodeQueue = new LinkedBlockingQueue<SubtitleEvent>();

		for (int cpu = 0; cpu < quantizeThreadCount; cpu++) {
			new Thread(new QuantizeRunnable(quantizeQueue, encodeQueue,
					renderPending, quantizePending, progress), "Quantizer-"
					+ cpu).start();
		}

		new Thread(new EncodeRunnable(encodeQueue, outputFilename, fps,
				quantizeThreadCount, quantizePending, progress), "Encoder")
		.start();

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
	}

	public void run() {
		try {
            renderer.init();
            
			renderer.openSubtitle(inputFilename, x, y);

			processTimecodes();

			renderTimecodes();

			renderPending.set(false);

			renderer.closeSubtitle();
		} catch (final RenderException ex) {
			progress.fail(ex.getMessage());
			Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (final InterruptedException ex) {
			progress.fail(ex.getMessage());
			Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	public void cancel() {
		cancelled.set(true);
	}

	private TreeSet<Timecode> processTimecodes() throws RenderException {
		final int eventCount = renderer.getEventCount();

		for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
			Timecode timecode = renderer.getEventTimecode(eventIndex);
			final Iterator<Timecode> i = timecodes.iterator();

			while (i.hasNext()) {
				final Timecode other = i.next();
				if (timecode.overlaps(other)) {
					i.remove();
					timecode = timecode.merge(other);
				}
			}
			timecodes.add(timecode);
		}

		return timecodes;
	}

	private void renderTimecodes() throws RenderException, InterruptedException {
		int percentage;
		int eventCount = timecodes.size();
		int eventIndex = 0;
		final Iterator<Timecode> i = timecodes.iterator();

		// Timecode loop: build subtitle event for timecode
		while (i.hasNext()) {
			SubtitleEvent event = new SubtitleEvent(i.next(), SubtitleType.FIRST);
			percentage = Math.round((float) eventIndex / eventCount * 100f);
			progress.progress(percentage, "Rendering Event " + eventIndex
					+ " of " + eventCount + " (Estimated)");
			// Render loop: render, check for change, split on change
			while (event != null && !cancelled.get()) {
				Timecode timecode = event.getTimecode();
				final long end = timecode.getEnd();
				
				SubtitleEvent nextEvent = null;
				final BufferedImage image = new BufferedImage(x, y,
						BufferedImage.TYPE_INT_ARGB);
				long changeAtMillisecond = timecode.getStart() + fps.milliseconds();
				boolean changed = false;
				boolean ended = changeAtMillisecond >= timecode.getEnd() - 1;
				// Prepare for change detect
				renderer.changeDetect(timecode.getStart());
				// Change Detect Loop
				while (!ended && !changed) {
					ended = changeAtMillisecond >= end - 1;
					changed = renderer.changeDetect(changeAtMillisecond) > 0;

					if (changed) {
						event.setTimecode(new Timecode(timecode.getStart(), changeAtMillisecond - 1));
						timecode = new Timecode(changeAtMillisecond, end);
						nextEvent = new SubtitleEvent(timecode, SubtitleType.SEQUENCE);
						
						eventCount++;
					}

					if (changeAtMillisecond + fps.milliseconds() > timecode.getEnd() - 1) {
						changeAtMillisecond = timecode.getEnd() - 1;
					} else {
						changeAtMillisecond += fps.milliseconds();
					}
				}

				renderer.render(image, event.getRenderTimecode());
				event.setImage(image);
				quantizeQueue.put(event);
				eventIndex++;
				event = nextEvent;
			}
		}
	}
}
