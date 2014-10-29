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
import name.connolly.david.pgs.debug.SupOutputStream;

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

        if (palette.size() >= 0x47) {
            position++;
        }

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

    // TODO: Create a state chart approach that avoids 5047. Land of a million corners!
    public void writeIndex(final SupOutputStream out) throws IOException {
        int count = 0;
        final int size;

        if (palette.size() >= 0x47) {
            size = ((palette.size() + 1) * 5) + 2; // account for the dummy
        } else {
            size = (palette.size() * 5) + 2;
        }
        
        // 15 3C DC 00 00 00 C0 00 3C D5
        out.write(0x14);
        out.write(size >> 8 & 0xFF);
        out.write(size & 0xFF);
        out.write(0x00);
        out.write(0x00);

        for (final ColorEntry entry : palette.values()) {
            final int color = entry.getYCbCr();

            if (count == 0x47) {
                int dummyColor = 0; // don't use color 0x47, to avoid 5047 in file
                out.write(count);
                // Y
                out.write(dummyColor >> 24 & 0xFF);
                // Cb
                out.write(dummyColor >> 16 & 0xFF);
                // Cr
                out.write(dummyColor >> 8 & 0xFF);
                // A
                out.write(dummyColor & 0xFF);

                count++;
            }

            out.write(count);

            // Y
            if (out.isUnsafeWrite(color >> 24 & 0xFF)) {
                out.write(0x48); // fudge from 0x47
            } else {
                out.write(color >> 24 & 0xFF);
            }

            // Cb
            if (out.isUnsafeWrite(color >> 16 & 0xFF)) {
                out.write(0x48); // fudge from 0x47
            } else {
                out.write(color >> 16 & 0xFF);
            }

            // Cr
            if (out.isUnsafeWrite(color >> 8 & 0xFF)) {
                out.write(0x48); // fudge from 0x47
            } else {
                out.write(color >> 8 & 0xFF);
            }

            // A
            if ((color & 0xFF) == 0x50 && count == 0x46) {
               out.write(0x51); // fudge transparancy to avoid 5047 in file
            } else if (out.isUnsafeWrite(color & 0xFF)) {
                out.write(0x48);
            } else {
                out.write(color & 0xFF);
            }
            
            count++;
        }
    }
}
