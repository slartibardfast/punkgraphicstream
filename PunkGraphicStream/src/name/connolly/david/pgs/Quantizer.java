/*
 * Quantizer.java
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

public class Quantizer {
	public static BufferedImage indexImage(BufferedImage image) {
		NeuQuant nq;
		try {
			nq = new NeuQuant(image, 1920, 1080);
			nq.init();
			int argb;

			for (int y = 0; y < 1080; y++) {
				for (int x = 0; x < 1920; x++) {
					argb = image.getRGB(x, y);
					image.setRGB(x, y, nq.convert(argb));
				}
			}
		} catch (final IOException e) {
			throw new RuntimeException("Quantizer failed" + e.getMessage());
		}

		return image;
	}
}
