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
import java.io.ByteArrayOutputStream;

import name.connolly.david.pgs.color.ColorTable;

public class RleBitmap {
    private final int MAX_PAYLOAD_FIRST = 0xFFFF - 0xB;
    private final int MAX_PAYLOAD = 0xFFFF - 0x4;
    private ColorTable table;
    private final BufferedImage image;
    private ByteArrayOutputStream rle;
    private int offsetX;
    private int offsetY;
    
    public byte[] getRle() {
        return rle.toByteArray();
    }

    public BufferedImage getImage() {
        return image;
    }
    
    public int getHeight() {
        return image.getHeight();
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getObjectCount() {
        int size = rle.size();
        int count = 0;
        
        if (size > MAX_PAYLOAD_FIRST) {
            size -= MAX_PAYLOAD_FIRST;
            count++;
        }

        while (size > MAX_PAYLOAD) {
            size -= MAX_PAYLOAD;
            count++;
        }

        if (size > 0) {
            count++;
        }

        return count;
    }

    public RleBitmap(final BufferedImage image, int offsetX, int offsetY) throws BitmapOversizeException {
        this.image = image;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        
        encode();
    }

    private void encode() throws BitmapOversizeException {
        rle = new ByteArrayOutputStream();
        table = new ColorTable();
        final int width = image.getWidth();
        final int height = image.getHeight();
        int xIndex = 0;
        int yIndex = 0;
        int count = 0;
        int position = 0;

        Integer pixel = null; // ARGB 32-Bit -> Need null :)

        while (yIndex < height) {
            xIndex = 0;

            while (xIndex < width) {
                if (new Integer(image.getRGB(xIndex, yIndex)).equals(pixel)) {
                    count++;
                } else if (pixel == null) {
                    pixel = image.getRGB(xIndex, yIndex);
                    position = table.getColorPosition(pixel);

                    if (position == -1) {
                        position = table.addColor(pixel);
                    }

                    count = 1;
                } else {
                    // write out old pixel
                    writeRleCommand(rle, count, position, yIndex);

                    // new pixel
                    pixel = image.getRGB(xIndex, yIndex);
                    position = table.getColorPosition(pixel);

                    if (position == -1) {
                        position = table.addColor(pixel);
                    }
                    count = 1;
                }

                xIndex++;
            }

            writeRleCommand(rle, count, position, yIndex);

            pixel = null;
            rle.write(0x00);
            rle.write(0x00);

            yIndex++;
        }

        if (firstSize() > (0xFFFF - 0xB)) {
            throw new BitmapOversizeException();
        }
    }

    public ColorTable getColorTable() {
        return table;
    }

    public int firstSize() {
        return rle.size() + 0xB;
    }

    public int objectSize() {
        return rle.size() + 0x4;
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

        // System.out.println("yIndex: " + yIndex + " color: " + color +
        // " extended: "
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
