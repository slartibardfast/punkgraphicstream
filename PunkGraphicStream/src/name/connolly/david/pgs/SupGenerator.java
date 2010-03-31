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

import name.connolly.david.pgs.color.ColorTable;
import name.connolly.david.pgs.debug.SupOutputStream;
import name.connolly.david.pgs.util.ProgressSink;

public class SupGenerator {

    final FrameRate fps;
    private int fpsCode;
    private final SupOutputStream out;
    private BigInteger preloadHeader = BigInteger.valueOf(5832);
    private BigInteger preloadBitmap = BigInteger.valueOf(5652);
    private BigInteger preloadMs = BigInteger.valueOf(90);
    private final ProgressSink progress;
    private final Resolution resolution;

    public SupGenerator(final OutputStream out, final FrameRate fps, final Resolution resolution, final ProgressSink progress) {
        this.out = new SupOutputStream(out);

        this.fps = fps;

        this.progress = progress;

        this.resolution = resolution;

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

    public void addEvent(SubtitleEvent event) throws IOException,
            InterruptedException {
        final BigInteger start;
        final BigInteger end;

        BufferedImage image = event.getImage();
        try {
            RleBitmap bitmap = new RleBitmap(image, event.getOffsetX(), event.getOffsetY());

            start = event.getTimecode().getStartTicks();
            end = event.getTimecode().getEndTicks();
            //System.out.println("start:" + start + " end: " + end);

            if (start.compareTo(preloadHeader) >= 0) {
                writeSubpicture(end, start, bitmap);
            } else {
                writeNoPreloadSubpicture(end, start, bitmap);
            }
        } catch (BitmapOversizeException e) {
            progress.fail("Subtitle image too large. Try to reduce effects, font size or number of characters.");
        }

    }

    public void bitmapPacket(final RleBitmap bitmap, final BigInteger from, final BigInteger to) throws IOException {
        BufferedImage image = bitmap.getImage();
        byte[] rleBytes = bitmap.getRle();
        int size;
        int objectId = 0;
        int biggestWrite = 0xFFE4;
        int objectSize = bitmap.objectSize();
        size = bitmap.firstSize();
        
        if (size >= 0xFFEF) {
            size = 0xFFEF;
        }

        timeHeader(from, to);
        out.write(0x15);
        out.write((size >> 8) & 0xFF);
        out.write(size & 0xFF);
        out.write(0x00);
        out.write(0x00); // Object ID
        out.write(0x00); // Version number
        out.write(0x80); // first in sequence
        out.write((objectSize >> 16) & 0xFF);
        out.write((objectSize >> 8) & 0xFF);
        out.write(objectSize & 0xFF);
        out.write((image.getWidth() >> 8) & 0xFF);
        out.write(image.getWidth() & 0xFF);
        out.write((image.getHeight() >> 8) & 0xFF);
        out.write(image.getHeight() & 0xFF);

        // Support for oversized bitmaps, something like this. needs more work!
        if (rleBytes.length <= (biggestWrite)) {
            out.write(rleBytes); // Done :)
        } else {
            // Larger subtitle
            int offset = 0;
            out.write(rleBytes, offset, biggestWrite);
            offset += biggestWrite;

            objectId++;
            biggestWrite = 0xFFEB;
            while ((offset + biggestWrite) <= rleBytes.length) {
                timeHeader(from, to);
                out.write(0x15);
                out.write(0xFF);
                out.write(0xEF);
                out.write(0x00);
                out.write(0x00);
                out.write(0x00); // Version number
                out.write(0x00); // append switch?
                out.write(rleBytes, offset, biggestWrite);
                offset += biggestWrite;
                objectId++;
            }

            biggestWrite = rleBytes.length - offset;
            if (biggestWrite > 0) {
            timeHeader(from, to);
            out.write(0x15);
            out.write(((biggestWrite + 0x4) >> 8) & 0xFF);
            out.write((biggestWrite + 0x4) & 0xFF);
            out.write(0x00);
            out.write(0x00); // Object ID Ref?
            out.write(0x00); // Version number
            out.write(0x40); // last in sequence?
            out.write(rleBytes, offset, biggestWrite);
            }
        }
    }

    private void writeSubpicture(final BigInteger end, BigInteger start, final RleBitmap bitmap) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x = bitmap.getOffsetX();
        int y = bitmap.getOffsetY();
        
        timeHeader(start, start.subtract(preloadHeader));
        subpictureHeader(resolution.getX(), resolution.getY(), x, y, bitmap.getObjectCount());
        ColorTable colorTable = bitmap.getColorTable();
        timeHeader(start.subtract(preloadMs), start.subtract(preloadHeader));
        windowsHeader(width, height, x, y);
        timeHeader(start.subtract(preloadHeader), BigInteger.ZERO);
        colorTable.writeIndex(out);
        bitmapPacket(bitmap, start.subtract(preloadBitmap), start.subtract(preloadHeader));
        timeHeader(start.subtract(preloadBitmap), BigInteger.ZERO);
        trailer();
        timeHeader(end, end.subtract(preloadMs));
        clearSubpictureHeader(resolution.getX(), resolution.getY(), x, y);
        timeHeader(end, BigInteger.ZERO);
        windowsHeader(width, height, x, y);
        timeHeader(end, BigInteger.ZERO);
        trailer();
    }

    // TODO: Test of multiplexed subtitles before 64.8ms, on non-PS3 devices.
    private void writeNoPreloadSubpicture(final BigInteger end, BigInteger start, RleBitmap bitmap) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x = bitmap.getOffsetX();
        int y = bitmap.getOffsetY();

        timeHeader(start, start);
        subpictureHeader(resolution.getX(), resolution.getY(), x, y, bitmap.getObjectCount());
        ColorTable colorTable = bitmap.getColorTable();
        timeHeader(start, start);
        windowsHeader(width, height, x, y);
        timeHeader(start, BigInteger.ZERO);
        colorTable.writeIndex(out);
        bitmapPacket(bitmap, start, BigInteger.ZERO);
        timeHeader(start, BigInteger.ZERO);
        trailer();
        timeHeader(end, end);
        clearSubpictureHeader(resolution.getX(), resolution.getY(), x, y);
        timeHeader(end, BigInteger.ZERO);
        windowsHeader(width, height, x, y);
        timeHeader(end, BigInteger.ZERO);
        trailer();
    }

    private void windowsHeader(final int width, final int height,
            final int widthOffset, final int heightOffset) throws IOException {
        // 17 00 0A 01 00 00 00 03 F7 07 80 00 31
        out.write(0x17);
        out.write(0x00);
        out.write(0x0A);
        out.write(0x01);
        out.write(0x00);
        out.write(widthOffset >> 8 & 0xFF); // TODO: Confirm Works
        out.write(widthOffset & 0xFF); // TODO: Confirm Works
        out.write(heightOffset >> 8 & 0xFF);
        out.write(heightOffset & 0xFF);
        out.write(width >> 8 & 0xFF);
        out.write(width & 0xFF);
        out.write(height >> 8 & 0xFF);
        out.write(height & 0xFF);
    }

    private void clearSubpictureHeader(final int width, final int height,
            final int x, final int y)
            throws IOException {
        out.write(0x16);

        // Size of Header
        out.write(0x00);
        out.write(0x0B);

        // Size of Picture
        out.write(width >> 8 & 0xFF);
        out.write(width & 0xFF);
        out.write(height >> 8 & 0xFF);
        out.write(height & 0xFF);
        out.write(fpsCode); // ??
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
    }

    private void subpictureHeader(final int width, final int height,
            final int x, final int y, final int objectCount) throws IOException {
        out.write(0x16);

        // Size of Header
        out.write(0x00);
        out.write(0x13);

        // Size of Picture
        out.write(width >> 8 & 0xFF);
        out.write(width & 0xFF);
        out.write(height >> 8 & 0xFF);
        out.write(height & 0xFF);
        out.write(fpsCode);
        out.write(objectCount >> 8 & 0xFF); // Number
        out.write(objectCount & 0xFF);
        out.write(0x80); // State
        out.write(0x00); // Pallette Update Flags
        out.write(0x00); // Pallette Id ref
        out.write(0x01); // Number?
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00); // item cropped =  =| 0x80 , item forced |= 0x40
        out.write(x >> 8 & 0xFF); // TODO: Confirm Works
        out.write(x & 0xFF); // TODO: Confirm Works
        out.write(y >> 8 & 0xFF);
        out.write(y & 0xFF);
    }

    private void timeHeader(final BigInteger from, final BigInteger to)
            throws IOException {
        String fromBytes;
        String toBytes;

        // one ninetieth of a millisecond
        fromBytes = from.toString(16);
        toBytes = to.toString(16);

        if (fromBytes.length() > 8 || toBytes.length() > 8) {
            throw new RuntimeException("Timecode too big");
        }

        // FIXME: A non String version; When its not 2am!
        while (fromBytes.length() < 8) {
            fromBytes = "0" + fromBytes;
        }

        while (toBytes.length() < 8) {
            toBytes = "0" + toBytes;
        }

        out.writeSupHeader();

        for (int i = 0; i < 8; i = i + 2) {
            out.write(Integer.parseInt(fromBytes.substring(i, i + 2), 16));
        }

        for (int i = 0; i < 8; i = i + 2) {
            out.write(Integer.parseInt(toBytes.substring(i, i + 2), 16));
        }
    }

    private void trailer() throws IOException {
        out.write(0x80);
        out.write(0);
        out.write(0);
    }
}
