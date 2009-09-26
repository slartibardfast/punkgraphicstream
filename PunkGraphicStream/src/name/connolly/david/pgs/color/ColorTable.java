/*
 * ColorTable.java
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
package name.connolly.david.pgs.color;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ColorTable {
    private int colorMissingCount = 0;
    // Key: RGB Color Value: ColorEntry
    private final Map<Integer, ColorEntry> palette = new LinkedHashMap<Integer, ColorEntry>();

    private class ColorEntry {
        private final int position;
        private final int ycbcr;

        public ColorEntry(final int position, final int rgb) {
            this.position = position;
            ycbcr = YCrCbRec709_ColorSpace.fromRGB(rgb);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ColorEntry other = (ColorEntry) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (position != other.position) {
                return false;
            }
            if (ycbcr != other.ycbcr) {
                return false;
            }
            return true;
        }

        private ColorTable getOuterType() {
            return ColorTable.this;
        }

        public int getPosition() {
            return position;
        }

        public int getYCbCr() {
            return ycbcr;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + position;
            result = prime * result + ycbcr;
            return result;
        }
    }

    public ColorTable() {
    }

    public ColorTable(final int[] rgbPalette) {
        for (final int rgbColor : rgbPalette) {
            addColor(rgbColor);
            //palette.put(rgbColor, new ColorEntry(palette.size(), rgbColor));
        }
    }

    public int addColor(final int rgbColor) {
        int position = palette.size();

        palette.put(rgbColor, new ColorEntry(position, rgbColor));

        return position;
    }

    public int getColorPosition(final int rgbColor) {
        final ColorEntry color = palette.get(rgbColor);

        if (color == null) {
            return -1;
        }

        return color.getPosition();
    }

    public int getYCbCrColor(final int rgbColor) {
        final ColorEntry color = palette.get(rgbColor);

        if (color == null) {
            ++colorMissingCount;
            System.err.println("Must never enquire about a color outside of table: " + colorMissingCount);
            throw new RuntimeException("Color not found!");
        }

        return color.getYCbCr();
    }

    public void writeIndex(final OutputStream baos) throws IOException {
        int count = 0;
        final int size;
        size = (palette.size() * 5) + 2;
        
        // 15 3C DC 00 00 00 C0 00 3C D5
        baos.write(0x14);
        baos.write(size >> 8 & 0xFF);
        baos.write(size & 0xFF);
        baos.write(0x00);
        baos.write(0x00);

        for (final ColorEntry entry : palette.values()) {
            final int color = entry.getYCbCr();
            baos.write(count);
            // Y
            baos.write(color >> 24 & 0xFF);
            // Cb
            baos.write(color >> 16 & 0xFF);
            // Cr
            baos.write(color >> 8 & 0xFF);
            // A
            if (count == 0x46 && (color & 0xFF) == 0x50) {
                // the next number would be 0x47 and it would fail, so fudge A
                baos.write(0x51);
            } else {
                baos.write(color & 0xFF);
            }

            count++;
        }
    }
}
