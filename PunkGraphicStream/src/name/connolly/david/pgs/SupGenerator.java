/*
 * SupGenerator.java
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
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.connolly.david.pgs.SubtitleEvent.SubtitleType;
import name.connolly.david.pgs.color.ColorTable;

public class SupGenerator {
	final FrameRate fps;
	private int fpsCode;
	private final OutputStream os;
	private int subpictureCount;
	private BigInteger preloadHeader = BigInteger.valueOf(5832);
	private BigInteger preloadBitmap = BigInteger.valueOf(5652);
	private BigInteger preloadMs = BigInteger.valueOf(90);
	private BigInteger lastEndTicks;

	public SupGenerator(final OutputStream os, final FrameRate fps) {
		this.os = os;

		subpictureCount = 0;

		this.fps = fps;

		switch (fps) {
		case FILM_NTSC:
			this.fpsCode = 0x10;
			break;
		case FILM:
			this.fpsCode = 0x20;
			break;
		case TV_PAL:
			this.fpsCode = 0x30;
			break;
		case TV_NTSC:
			this.fpsCode = 0x40;
			break;
		case HD_PAL:
			this.fpsCode = 0x60;
			break;
		case HD_NTSC:
			this.fpsCode = 0x70;
			break;
		default:
			this.fpsCode = 0x20;
			break;
		}
	}

	public void addBitmap(SubtitleEvent event) throws IOException,
			InterruptedException {
		final BufferedImage image = event.getImage();
                final int x = event.getOffsetX();
                final int y = event.getOffsetY();
                
		final int width = image.getWidth();
		final int height = image.getHeight();
		final BigInteger start;
        final BigInteger end;
		final RleBitmap bitmap = new RleBitmap(image);
		final ColorTable colorTable = bitmap.encode();

        start = event.getTimecode().getStartTicks();
        end = event.getTimecode().getEndTicks();
                
		if (start.compareTo(preloadHeader) >= 0) {
			writeBitmap(width, height, x, y, end, start, bitmap, colorTable);
		} else {
			writeBitmapNoPreload(width, height, x, y, end, start, bitmap, colorTable);
		}
	}

	private void writeBitmap(final int width, final int height, final int x, final int y,
			final BigInteger end, BigInteger start, final RleBitmap bitmap,
			final ColorTable colorTable) throws IOException {
		timeHeader(start, start.subtract(preloadHeader));
		subpictureHeader(width, height, x, y);
		timeHeader(start.subtract(preloadMs), start.subtract(preloadHeader));
		bitmapHeader(width, height, x, y);
		timeHeader(start.subtract(preloadHeader), BigInteger.ZERO);
		colorTable.writeIndex(os);
		timeHeader(start.subtract(preloadBitmap), start.subtract(preloadHeader));
		bitmap.writeBitmap(os);
		timeHeader(start.subtract(preloadBitmap), BigInteger.ZERO);
		trailer();
		timeHeader(end, end.subtract(preloadMs));
		clearSubpictureHeader(width, height);
		timeHeader(end, BigInteger.ZERO);
		bitmapHeader(width, height, x, y);
		timeHeader(end, BigInteger.ZERO);
		trailer();
	}

	// TODO: Test of multiplexed subtitles before 64.8ms
	private void writeBitmapNoPreload( final int width, final int height, final int x, final int y,
			final BigInteger end, BigInteger start, final RleBitmap bitmap,
			final ColorTable colorTable) throws IOException {
		timeHeader(start, start);
		subpictureHeader(width, height, x, y);
		timeHeader(start, start);
		bitmapHeader(width, height, x, y);
		timeHeader(start, BigInteger.ZERO);
		colorTable.writeIndex(os);
		timeHeader(start, start);
		bitmap.writeBitmap(os);
		timeHeader(start, BigInteger.ZERO);
		trailer();
		timeHeader(end, end);
		clearSubpictureHeader(width, height);
		timeHeader(end, BigInteger.ZERO);
		bitmapHeader(width, height, x, y);
		timeHeader(end, BigInteger.ZERO);
		trailer();
	}

	private void bitmapHeader(final int width, final int height,
			final int widthOffset, final int heightOffset) throws IOException {
		// 17 00 0A 01 00 00 00 03 F7 07 80 00 31
		os.write(0x17);
		os.write(0x00);
		os.write(0x0A);
		os.write(0x01);
		os.write(0x00);
		os.write(widthOffset >> 8 & 0xFF); // TODO: Confirm Works
		os.write(widthOffset & 0xFF); // TODO: Confirm Works
		os.write(heightOffset >> 8 & 0xFF);
		os.write(heightOffset & 0xFF);
		os.write(width >> 8 & 0xFF);
		os.write(width & 0xFF);
		os.write(height >> 8 & 0xFF);
		os.write(height & 0xFF);
	}

	private void clearSubpictureHeader(final int width, final int height)
			throws IOException {
		// width:1920, height:1080 sequenceNumber:1
		// 16 00 0B 07 80 04 38 10 00 01 00 00 00 00
		os.write(0x16);

		// Size of Header
		os.write(0x00);
		os.write(0x0B);

		// Size of Picture
		os.write(width >> 8 & 0xFF);
		os.write(width & 0xFF);
		os.write(height >> 8 & 0xFF);
		os.write(height & 0xFF);
		os.write(fpsCode); // ??
		os.write(subpictureCount >> 8 & 0xFF);
		os.write(subpictureCount & 0xFF);
		os.write(0x00);
		os.write(0x00);
		os.write(0x00);
		os.write(0x00);

		subpictureCount++;
	}

	private void subpictureHeader(final int width, final int height,
			final int widthOffset, final int heightOffset) throws IOException {
		// width:1920, height:1080 sequenceNumber:0
		// 16 00 13 07 80 04 38 10 00 00 80 00 00 01 00 00 00 00 00 00 00 00
		os.write(0x16);

		// Size of Header
		os.write(0x00);
		os.write(0x13);

		// Size of Picture
		os.write(width >> 8 & 0xFF);
		os.write(width & 0xFF);
		os.write(height >> 8 & 0xFF);
		os.write(height & 0xFF);

		os.write(fpsCode);
		os.write(subpictureCount >> 8 & 0xFF);
		os.write(subpictureCount & 0xFF);
		os.write(0x80);
		os.write(0x00);
		os.write(0x00);
		os.write(0x01); // Don't Clear Sub-Picture
		os.write(0x00);
		os.write(0x00);
		os.write(0x00);
		os.write(0x00);
		os.write(widthOffset >> 8 & 0xFF); // TODO: Confirm Works
		os.write(widthOffset & 0xFF); // TODO: Confirm Works
		os.write(heightOffset >> 8 & 0xFF);
		os.write(heightOffset & 0xFF);

		subpictureCount++;
	}

	private void timeHeader(final BigInteger from, final BigInteger to)
			throws IOException {
		String fromBytes;
		String toBytes;

		// one ninetieth of a millisecond
		fromBytes = from.toString(16);
		toBytes = to.toString(16);

		if (fromBytes.length() > 8 || toBytes.length() > 8)
			throw new RuntimeException("Timecode too big");

		// FIXME: A non String version; When its not 2am!
		while (fromBytes.length() < 8) {
			fromBytes = "0" + fromBytes;
		}

		while (toBytes.length() < 8) {
			toBytes = "0" + toBytes;
		}

		os.write(0x50);
		os.write(0x47);

		for (int i = 0; i < 8; i = i + 2) {
			os.write(Integer.parseInt(fromBytes.substring(i, i + 2), 16));
		}

		for (int i = 0; i < 8; i = i + 2) {
			os.write(Integer.parseInt(toBytes.substring(i, i + 2), 16));
		}
	}

	private void trailer() throws IOException {
		os.write(0x80);
		os.write(0);
		os.write(0);
	}
}
