/*
 * Render.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RleBitmap {
	private final BufferedImage image;
	ByteArrayOutputStream rle;

	public RleBitmap(final BufferedImage image) {
		this.image = image;
		rle = new ByteArrayOutputStream();
	}

	public ColorTable encode() {
		final ColorTable table = new ColorTable();
		int x = 0;
		int y = 0;
		int count = 0;
		int position = 0;

		Integer pixel = null; // ARGB 32-Bit -> Need null :)

		while (y < 1080) {
			x = 0;

			while (x < 1920) {
				if (new Integer(image.getRGB(x, y)).equals(pixel)) {
					count++;
				} else if (pixel == null) {
					pixel = image.getRGB(x, y);
					position = table.getColorPosition(pixel);

					if (position == -1) {
						position = table.addColor(pixel);
					}

					count = 1;
				} else {
					// write out old pixel
					writeRleCommand(rle, count, position, y);

					// new pixel
					pixel = image.getRGB(x, y);
					position = table.getColorPosition(pixel);

					if (position == -1) {
						position = table.addColor(pixel);
					}
					count = 1;
				}

				x++;
			}

			writeRleCommand(rle, count, position, y);

			pixel = null;
			rle.write(0x00);
			rle.write(0x00);

			y++;
		}

		return table;
	}

	public void writeBitmap(final OutputStream baos) throws IOException {
		int size = rle.size() + 11;

		baos.write(0x15);
		baos.write(size >> 8 & 0xFF);
		baos.write((size & 0xFF));
		baos.write(0x00);
		baos.write(0x00);
		baos.write(0x00);
		baos.write(0xC0);
		baos.write(0x00);
		size -= 7;
		baos.write(size >> 8 & 0xFF);
		baos.write((size & 0xFF));
		baos.write(image.getWidth() >> 8 & 0xFF);
		baos.write(image.getWidth() & 0xFF);
		baos.write(image.getHeight() >> 8 & 0xFF);
		baos.write(image.getHeight() & 0xFF);

		try {
			baos.write(rle.toByteArray());
		} catch (final IOException e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	/**
	 * Format discussed on http://forum.doom9.org/showthread.php?t=124105
	 */
	private void writeRleCommand(final ByteArrayOutputStream rle,
			final int count, final int position, final int y) {
		final boolean color = position != 0; // if 0 color == 0 or end of line
												// signal.
		// else nonzero color
		final boolean extended = count > 63;
		// ByteArrayOutputStream rle = new ByteArrayOutputStream();

		// System.out.println("y: " + y + " color: " + color + " extended: "
		// + extended + " pixel count: " + count + " index Position: "
		// + position);

		if (count < 3 && position != 0) {
			for (int i = 0; i < count; i++) {
				rle.write(position);
			}
		} else {
			rle.write(0);

			if (extended) {
				if (color) {
					int b = 0xC0;
					b |= count >> 8 & 0x3F;
					rle.write(b);
					b = count & 0xFF;
					rle.write(b);
				} else {
					int b = 0x40;
					b |= count >> 8 & 0x3F;
					rle.write(b);
					b = count & 0xFF;
					rle.write(b);
				}
			} else {
				if (color) {
					int b = 0x80;
					b |= count & 0x3F;
					rle.write(b);
				} else {
					int b = 0;
					b |= count & 0x3F;
					rle.write(b);
				}
			}

			if (color) {
				rle.write(position);
			}
		}
	}
}
