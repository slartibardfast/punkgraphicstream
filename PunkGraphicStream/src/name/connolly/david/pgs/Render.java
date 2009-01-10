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

public enum Render {
    INSTANCE;
    
	static {
		System.loadLibrary("ass");
	}

	public native void openSubtitle(String filename, int x, int y) throws RenderException;

	public native void closeSubtitle() throws RenderException;
	
	public native int changeDetect(long timecode) throws RenderException;

	public native int getEventCount() throws RenderException;

    public native Timecode getEventTimecode(int eventIndex) throws RenderException;

	public native void render(BufferedImage image, long timecode) throws RenderException;
}