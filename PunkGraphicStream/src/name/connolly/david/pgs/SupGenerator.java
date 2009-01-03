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

public class SupGenerator {
	private int fps;
	private final OutputStream os;
	private int subpictureCount;

	public SupGenerator(final OutputStream os, final FrameRate fps) {
		this.os = os;

		subpictureCount = 0;

		switch (fps) {
		case FILM_NTSC:
			this.fps = 0x10;
			break;
		case FILM:
			this.fps = 0x20;
			break;
		case TV_PAL:
			this.fps = 0x30;
			break;
		case TV_NTSC:
			this.fps = 0x40;
			break;
		case HD_PAL:
			this.fps = 0x50;
			break;
		case HD_NTSC:
			this.fps = 0x60;
			break;
		default:
			this.fps = 0x20;
			break;
		}
	}

	public void addBitmap(final BufferedImage image, final int width,
			final int height, SubtitleEvent event) throws IOException {
		final BigInteger to = event.getEndTimecode();
		BigInteger from = event.getStartTimecode();
		final RleBitmap bitmap = new RleBitmap(image);
		final ColorTable colorTable = bitmap.encode();

		if (from.compareTo(BigInteger.valueOf(65)) == -1) {
			from = from.add(BigInteger.valueOf(65)); // TODO: Investigate muxing
														// subtitles before 65ms
		}

		timeHeader(from, from.subtract(BigInteger.valueOf(65)));
		subpictureHeader(width, height, 0, 0);
		timeHeader(from.subtract(BigInteger.ONE), from.subtract(BigInteger
				.valueOf(65)));
		bitmapHeader(width, height, 0, 0);
		timeHeader(from.subtract(BigInteger.valueOf(65)), BigInteger.ZERO);
		colorTable.writeIndex(os);
		timeHeader(from.subtract(BigInteger.valueOf(63)), from
				.subtract(BigInteger.valueOf(65)));
		bitmap.writeBitmap(os);
		timeHeader(from.subtract(BigInteger.valueOf(63)), BigInteger.ZERO);
		trailer();
		timeHeader(to, to.subtract(BigInteger.ONE));
		clearSubpictureHeader(width, height);
		timeHeader(to, BigInteger.ZERO);
		bitmapHeader(width, height, 0, 0);
		timeHeader(to, BigInteger.ZERO);
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
		os.write(fps); // ??
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

		os.write(fps);
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
