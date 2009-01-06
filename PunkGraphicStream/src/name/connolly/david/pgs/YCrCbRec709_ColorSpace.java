/*
 * YCrCbRec709_ColorSpace.java
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

public class YCrCbRec709_ColorSpace {
	public static int fromRGB(final int rgbvalue) {
		int YCbCrA = 0;
		final int a = rgbvalue >> 24 & 0xFF;
		final double er = (rgbvalue >> 16 & 0xFF) / 255d;
		final double eg = (rgbvalue >> 8 & 0xFF) / 255d;
		final double eb = (rgbvalue & 0xFF) / 255d;

		// System.out.println("a: "+ a + " r: " + er + " g: " + eg + " b: " +
		// eb);

		/*
		 * E′Y = 0.7152 E′G + 0.0722 E′B + 0.2126 E′R E′PB = – 0.3854 E′G +
		 * 0.5000 E′B – 0.1146 E′R E′PR = – 0.4542 E′G – 0.0458 E′B + 0.5000 E′R
		 */

		final double ey = 0.7152 * eg + 0.0722 * eb + 0.2126 * er;
		final double epr = -(0.3854 * eg) + 0.5 * eb - 0.1146 * er;
		final double epb = -(0.4542 * eg) - 0.0458 * eb + 0.5 * er;

		// System.out.println("E′Y: "+ ey + " E′Pb: " + epb+ " E′Pr: " + epr +
		// " A: " + a);
		/*
		 * Y = max[0, min[ 255, Round( ( 219 E′Y ) ) + 16 ] ] Cb = max[0, min[
		 * 255, Round( ( 224 E′PB ) ) + 128] ] Cr = max[0, min[ 255, Round( (
		 * 224 E′PR ) ) + 128] ]
		 */
		final int y = (int) Math.max(0, Math
				.min(255, Math.round(219 * ey) + 16));
		final int cr = (int) Math.max(0, Math.min(255,
				Math.round(224 * epr) + 128));
		final int cb = (int) Math.max(0, Math.min(255,
				Math.round(224 * epb) + 128));

		// System.out.println("Y: "+ y + " Cb: " + cb+ "Cr: " + cr + " A: " +
		// a);

		YCbCrA |= a & 0xFF;
		YCbCrA |= y << 24;
		YCbCrA |= cb << 16;
		YCbCrA |= cr << 8;

		return YCbCrA;
	}

	public static int toRGB(final int colorvalue) {
		return 0;
	}
}
