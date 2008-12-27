/*
 * PunkGraphicStream.java
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

package name.connolly.david.pgs.test;

import static org.junit.Assert.fail;
import name.connolly.david.pgs.YCrCbRec709_ColorSpace;

import org.junit.Test;

public class YCrCbRec709_ColorSpaceTest {

	@Test
	public void testFromRGB() {
		YCrCbRec709_ColorSpace.fromRGB(0);
		YCrCbRec709_ColorSpace.fromRGB(0x00FFFFFF);
		YCrCbRec709_ColorSpace.fromRGB(0x00FF0000);
		YCrCbRec709_ColorSpace.fromRGB(0x0000FF00);
		YCrCbRec709_ColorSpace.fromRGB(0x000000FF);
	}

	@Test
	public void testToRGB() {
		fail("Not yet implemented");
	}

}
