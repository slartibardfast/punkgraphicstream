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
package name.connolly.david.pgs;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class EncodeRunnable implements Runnable {
	private final ArrayList<SubtitleEvent> events;
	final String filename;
	final FrameRate fps;
	 
	public EncodeRunnable(final ArrayList<SubtitleEvent> events, final String filename, final FrameRate fps) {
		this.events = events;
		this.filename = filename;
		this.fps = fps;
	}

	public void run() {
		try {
			final OutputStream os = new BufferedOutputStream(
					new FileOutputStream(filename));
			final SupGenerator packet = new SupGenerator(os, fps);
			SubtitleEvent event;
			BufferedImage indexed;
			int count = 0;

			for (int i = 0; i < events.size(); i++) {
				event = events.get(i);

				indexed = event.takeImage();
				
				packet.addBitmap(indexed, 1920, 1080, event.getTimecode(),
						event.getLength());

				System.out.println("Encoded no.\t" + count);
				
				count++;
			}

			os.flush();
			os.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}