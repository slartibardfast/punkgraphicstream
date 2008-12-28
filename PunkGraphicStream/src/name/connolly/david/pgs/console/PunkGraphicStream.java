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
import java.util.ArrayList;

import name.connolly.david.pgs.EncodeRunnable;
import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.PendingRenderLock;
import name.connolly.david.pgs.QuantizeRunnable;
import name.connolly.david.pgs.Render;
import name.connolly.david.pgs.SubtitleEvent;

public class PunkGraphicStream {
	public static void main(final String[] args) {
		final Render r = new Render();
		final ArrayList<SubtitleEvent> events;
		final PendingRenderLock lock = new PendingRenderLock();
		final String inputFilename;
		final int cpuCount = Runtime.getRuntime().availableProcessors();
		
		System.setProperty("java.awt.headless", "true"); 
		
		String outputFilename = "default.sup";
		FrameRate fps = FrameRate.FILM;
		

		if (args.length != 2) {
			printUsageAndQuit();
		}

		inputFilename = args[0];

		try {
			fps = FrameRate.valueOf(args[1].toUpperCase());
		} catch (IllegalArgumentException e) {
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

		events = r.generateEvents();
		
		for (int i = 0; i < cpuCount; i++) {
			new Thread(new QuantizeRunnable(events, lock, i, cpuCount)).start();
		}
		
		new Thread(new EncodeRunnable(events, outputFilename, fps)).start();

		for (int i = 0; i < events.size(); i++) {
			BufferedImage image, image2;
			final SubtitleEvent event = events.get(i);
			int detectChange;
			
			synchronized (lock) {
				while (lock.count() > (cpuCount * 2) - 1) {
					try {
						lock.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}

			}

			System.out.println("Rendering no:\t" + i);

			image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
			image2 = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);

			//r.render(image, event.getTimecode() + event.getLength() / 2);
			r.render(image, event.getTimecode());
			
			detectChange = r.render(image2, event.getTimecode() + fps.frameDurationInMilliseconds());
			
			if (detectChange > 0) {		
				System.out.println("Change in next frame detected...");
			}
			
			event.putImage(image);

			synchronized (lock) {
				lock.add();
			}
		}

		r.closeSubtitle();
	}
	
	private static void printUsageAndQuit() {
		System.out.println("PunkGraphicStream filename.ass fps");
		System.out
				.println("fps: film, film_ntsc, pal, ntsc, hd_pal, hd_ntsc");
		System.exit(0);
	}
}