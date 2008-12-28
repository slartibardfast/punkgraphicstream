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

package name.connolly.david.pgs;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Render {
	static {
		System.loadLibrary("ass");
	}
	
	public native void openSubtitle(String filename);

	public native void closeSubtitle();
	
	public native int getEventCount();

	public native SubtitleEvent getEventDuration(int event);
	
	public native long nextEvent(long timecode, int movement);

	public native int render(BufferedImage image, long timecode);
	
	public ArrayList<SubtitleEvent> generateEvents() {
		final int eventCount = getEventCount();
		final ArrayList<SubtitleEvent> events = new ArrayList<SubtitleEvent>();

		for (int i = 0; i < eventCount; i++) {
			final SubtitleEvent event = getEventDuration(i);
			events.add(event);
			
			/* TODO: Move from ArrayList to LinkedHashSet. 
			 * Stop generating all events in advance and 
			 * handle animation similar to below as required e.g. change detect.
			long frameLength = 1000 / (24000/1001);
			long numberOfFrames = (long) Math.ceil((double) event.getLength() / (double)frameLength); 
			
			for (long j = 0; j < numberOfFrames; j++) {
				events.add(new SubtitleEvent(event.getTimecode() + (i * frameLength), frameLength));
			}
			*/
		}
		

		return events;
	}

	public void printPalette(final int[] palette) {
		System.out.println("Number of palette entries: " + palette.length);

		for (final int pixel : palette) {
			YCrCbRec709_ColorSpace.fromRGB(pixel);
		}
	}
}