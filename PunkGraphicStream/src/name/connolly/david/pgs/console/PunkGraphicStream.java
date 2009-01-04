/*
 * PunkGraphicStream.java
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

package name.connolly.david.pgs.console;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import name.connolly.david.pgs.EncodeRunnable;
import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.QuantizeRunnable;
import name.connolly.david.pgs.Render;
import name.connolly.david.pgs.SubtitleEvent;

public class PunkGraphicStream {
	public static void main(final String[] args) {
		final Render r = new Render();
		final String inputFilename;
		final int quantizeThreadCount = Runtime.getRuntime()
				.availableProcessors();
		int eventIndex = 0;
		int frameIndex = 0;
		final int renderCount = quantizeThreadCount * 2 - 1;
		final BlockingQueue<SubtitleEvent> quantizeQueue;
		final BlockingQueue<SubtitleEvent> encodeQueue;
		final AtomicBoolean renderPending = new AtomicBoolean(true);
		final Semaphore quantizePending = new Semaphore(quantizeThreadCount);

		System.setProperty("java.awt.headless", "true");

		String outputFilename = "default.sup";
		FrameRate fps = FrameRate.FILM;

		if (args.length != 2) {
			printUsageAndQuit();
		}

		inputFilename = args[0];

		try {
			fps = FrameRate.valueOf(args[1].toUpperCase());
		} catch (final IllegalArgumentException e) {
			printUsageAndQuit();
		}

		if (inputFilename.length() > 5) {
			outputFilename = inputFilename.substring(0,
					inputFilename.length() - 4)
					+ ".sup";
		} else {
			printUsageAndQuit();
		}

		r.openSubtitle(inputFilename);

		r.getEventCount();

		quantizeQueue = new LinkedBlockingQueue<SubtitleEvent>(renderCount);
		encodeQueue = new LinkedBlockingQueue<SubtitleEvent>();

		for (int cpu = 0; cpu < quantizeThreadCount; cpu++) {
			new Thread(new QuantizeRunnable(quantizeQueue, encodeQueue,
					renderPending, quantizePending)).start();
		}

		new Thread(new EncodeRunnable(encodeQueue, outputFilename, fps,
				quantizeThreadCount, quantizePending)).start();

		SubtitleEvent event = r.getEvent(eventIndex);
		long frameRenderUntilTimecode = 0;

		System.out.println("Rendering Event No.\t" + eventIndex);
		while (event != null) {
			final BufferedImage currentFrame;
			SubtitleEvent nextEvent = null;

			final int detectChange;

			currentFrame = new BufferedImage(1920, 1080,
					BufferedImage.TYPE_INT_ARGB);

			r.render(currentFrame, event.getRenderTimecode());

			if (frameRenderUntilTimecode == 0) {
				detectChange = r.changeDetect(event.getTimecode()
						+ (event.getDuration() - 1));

				if (detectChange > 0) {
					frameRenderUntilTimecode = event.getDuration()
							+ event.getRenderTimecode();

					event.setFrameRate(fps);
					frameIndex++;

					//System.out.print("Rendering Frame No.\t" + frameIndex);
					System.out.println(" (Change in next frame detected...)");
					//System.out.println();

					nextEvent = new SubtitleEvent(event, fps, frameIndex);
				}
			} else if (frameRenderUntilTimecode > event.getRenderTimecode()
					+ fps.frameDurationInMilliseconds()) {
				frameIndex++;
				//System.out.println("Rendering Frame No.\t" + frameIndex);
				nextEvent = new SubtitleEvent(event, fps, frameIndex);
			}

			if (nextEvent == null) {
				frameRenderUntilTimecode = 0;
				frameIndex = 0;
				eventIndex++;
				System.out.println("Rendering Event No.\t" + eventIndex);
				nextEvent = r.getEvent(eventIndex);
			}

			event.putImage(currentFrame);

			try {
				quantizeQueue.put(event);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			event = nextEvent;
		}

		renderPending.set(false);

		r.closeSubtitle();
	}

	private static void printUsageAndQuit() {
		System.out.println("Usage: ");
		System.out.println("java -jar PunkGraphicStream.jar filename.ass fps");
		System.out.println();
		System.out
				.println("fps = [film, film_ntsc, pal, ntsc, hd_pal, hd_ntsc]");

		printLicence();

		System.exit(0);
	}

	private static void printLicence() {
		System.out.println("PunkGraphicStream 0.2");
		System.out
				.println("Copyright 2008 David Connolly. All rights reserved.");
		System.out.println();
		System.out
				.println("This is free software; see sources for copying conditions and credits of dependencies.");
		System.out
				.println("There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE");
	}
}