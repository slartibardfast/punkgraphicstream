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

import java.util.LinkedHashSet;
import java.util.Set;
import name.connolly.david.pgs.color.ColorTable;
import name.connolly.david.pgs.util.ProgressSink;

public class SupGenerator {

    final FrameRate fps;
    private int fpsCode;
    private final OutputStream os;
    private int subpictureCount;
    private BigInteger preloadHeader = BigInteger.valueOf(5832);
    private BigInteger preloadBitmap = BigInteger.valueOf(5652);
    private BigInteger preloadMs = BigInteger.valueOf(90);
    private final ProgressSink progress;
    private final Resolution resolution;

    public SupGenerator(final OutputStream os, final FrameRate fps, final Resolution resolution, final ProgressSink progress) {
        this.os = os;

        subpictureCount = 0;

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
        LinkedHashSet<BufferedImage> images = new LinkedHashSet<BufferedImage>();
        LinkedHashSet<RleBitmap> bitmaps = new LinkedHashSet<RleBitmap>();
        boolean valid = false;

        images.add(event.getImage());

        while (!valid) {
            try {
                int y = event.getOffsetY();

                for (BufferedImage image : images) {
                    RleBitmap bitmap = new RleBitmap(image, event.getOffsetX(), y);
                    bitmaps.add(bitmap);
                    y += bitmap.getHeight();
                }

                valid = true;
            } catch (BitmapOversizeException e) {
                // Split images in two and encode (will fail at one line in height)
                LinkedHashSet<BufferedImage> splitImages = new LinkedHashSet<BufferedImage>();

                for (BufferedImage image : images) {
                    progress.renderMessage("[pgs] Spliting image from : " + image.getWidth() + " " + image.getHeight() + "\n");
                    progress.renderMessage("[pgs] \t\tto: 0 0 " + image.getWidth() + " " + image.getHeight() / 2 + "\n");
                    progress.renderMessage("[pgs] \t\tand: 0 " + image.getHeight() / 2 + " " + image.getWidth() + " " + (image.getHeight() - image.getHeight() / 2) + "\n");
                    splitImages.add(image.getSubimage(0, 0, image.getWidth(), image.getHeight() / 2));
                    splitImages.add(image.getSubimage(0, image.getHeight() / 2, image.getWidth(), (image.getHeight() - image.getHeight() / 2)));
                }

                images = splitImages;
                bitmaps.clear();
            }
        }
        start = event.getTimecode().getStartTicks();
        end = event.getTimecode().getEndTicks();


        if (start.compareTo(preloadHeader) >= 0) {
            writeSubpicture(end, start, bitmaps);
        } else {
            writeNoPreloadSubpicture(end, start, bitmaps);
        }
    }

    public void bitmapPacket(int objectId, final RleBitmap bitmap,
            final BigInteger from, final BigInteger to) throws IOException {
        BufferedImage image = bitmap.getImage();
        byte[] rleBytes = bitmap.getRle();
        int size;

        if (bitmap.firstSize() > 0xFFFF) {
            size = 0xFFFF;
        } else {
            size = bitmap.firstSize();
        }

        timeHeader(from, to);

        os.write(0x15);
        os.write(size >> 8 & 0xFF);
        os.write(size & 0xFF);
        os.write(objectId >> 8 & 0xFF);
        os.write(objectId & 0xFF);
        os.write(0x00); // Version number
        os.write(0x80); // first in sequence
        size = bitmap.objectSize();
        os.write(size >> 16 & 0xFF);
        os.write(size >> 8 & 0xFF);
        os.write(size & 0xFF);
        os.write(image.getWidth() >> 8 & 0xFF);
        os.write(image.getWidth() & 0xFF);
        os.write(image.getHeight() >> 8 & 0xFF);
        os.write(image.getHeight() & 0xFF);

        if (rleBytes.length <= (0xFFFF - 0xB)) {
            os.write(rleBytes); // Done :)
            } else {
            // Larger subtitle

            int biggestWrite = 0xFFFF - 0xB;
            int offset = 0;
            os.write(rleBytes, offset, biggestWrite);
            offset += biggestWrite;
            biggestWrite = 0xFFFF - 0x4;

            while ((offset + biggestWrite) < rleBytes.length) {
                timeHeader(from, to);

                os.write(0x15);
                os.write(0xFF);
                os.write(0xFF);
                os.write(objectId >> 8 & 0xFF);
                os.write(objectId & 0xFF);
                os.write(0x00); // Version number
                os.write(0x00); // append switch
                os.write(rleBytes, offset, biggestWrite);
                offset += biggestWrite;
            }

            biggestWrite = rleBytes.length - offset;
            timeHeader(from, to);
            os.write(0x15);
            os.write(((biggestWrite + 4) >> 8) & 0xFF);
            os.write((biggestWrite + 4) & 0xFF);
            os.write(objectId >> 8 & 0xFF);
            os.write(objectId & 0xFF);
            os.write(0x00); // Version number
            os.write(0x40); // last in sequence
            os.write(rleBytes, offset, biggestWrite);
        }
    }

    private void writeSubpicture(final BigInteger end, BigInteger start, final Set<RleBitmap> bitmaps) throws IOException {
        //int windowId = 0;
        //int paletteId = 0;

        int id = 0;
        for (RleBitmap bitmap : bitmaps) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int x = bitmap.getOffsetX();
            int y = bitmap.getOffsetY();

            timeHeader(start, start.subtract(preloadHeader));
            subpictureHeader(id, id, id, resolution.getX(), resolution.getY(), x, y);
            ColorTable colorTable = bitmap.getColorTable();
            timeHeader(start.subtract(preloadMs), start.subtract(preloadHeader));
            windowsHeader(id, width, height, x, y);
            timeHeader(start.subtract(preloadHeader), BigInteger.ZERO);
            colorTable.writeIndex(0, os);
            bitmapPacket(id, bitmap, start.subtract(preloadBitmap), start.subtract(preloadHeader));
            timeHeader(start.subtract(preloadBitmap), BigInteger.ZERO);
            trailer();
            timeHeader(end, end.subtract(preloadMs));
            clearSubpictureHeader(id, resolution.getX(), resolution.getY());
            timeHeader(end, BigInteger.ZERO);
            windowsHeader(id, width, height, x, y);
            timeHeader(end, BigInteger.ZERO);
            trailer();
            id++;
        }
    }

    // TODO: Test of multiplexed subtitles before 64.8ms, on non-PS3 devices.
    private void writeNoPreloadSubpicture(final BigInteger end, BigInteger start, final Set<RleBitmap> bitmaps) throws IOException {
        int id = 0;

        for (RleBitmap bitmap : bitmaps) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int x = bitmap.getOffsetX();
            int y = bitmap.getOffsetY();

            timeHeader(start, start);
            subpictureHeader(id, id, id, resolution.getX(), resolution.getY(), 0, 0);
            ColorTable colorTable = bitmap.getColorTable();
            timeHeader(start, start);
            windowsHeader(id, resolution.getX(), resolution.getY(), 0, 0);
            timeHeader(start, BigInteger.ZERO);
            colorTable.writeIndex(0, os);
            bitmapPacket(id, bitmap, start, BigInteger.ZERO);
            timeHeader(start, BigInteger.ZERO);
            trailer();
            timeHeader(end, end);
            clearSubpictureHeader(0, width, height);
            timeHeader(end, BigInteger.ZERO);
            windowsHeader(id, resolution.getX(), resolution.getY(), 0, 0);
            timeHeader(end, BigInteger.ZERO);
            trailer();
            id++;
        }
    }

    private void windowsHeader(int windowId, final int width, final int height,
            final int widthOffset, final int heightOffset) throws IOException {
        // 17 00 0A 01 00 00 00 03 F7 07 80 00 31
        os.write(0x17);
        os.write(0x00);
        os.write(0x0A);
        os.write(0x01);
        os.write(windowId & 0xFF); //????
        os.write(widthOffset >> 8 & 0xFF); // TODO: Confirm Works
        os.write(widthOffset & 0xFF); // TODO: Confirm Works
        os.write(heightOffset >> 8 & 0xFF);
        os.write(heightOffset & 0xFF);
        os.write(width >> 8 & 0xFF);
        os.write(width & 0xFF);
        os.write(height >> 8 & 0xFF);
        os.write(height & 0xFF);
    }

    private void clearSubpictureHeader(int paletteId, final int width, final int height)
            throws IOException {
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
        os.write(paletteId & 0xFF); // Pallette Id ref
        os.write(0x00); // No object

        subpictureCount++;
    }

    private void subpictureHeader(int windowId, int objectId, int paletteId,
            final int width, final int height, final int x, final int y)
            throws IOException {
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
        os.write(subpictureCount >> 8 & 0xFF); // Number
        os.write(subpictureCount & 0xFF);
        os.write(0x80); // State
        os.write(0x00); // Pallette Update Flags
        os.write(paletteId & 0xFF); // Pallette Id ref
        os.write(0x01); // Object present, is it a count?
        os.write(objectId >> 8 & 0xFF); // Object ID
        os.write(objectId & 0xFF); // Object ID
        os.write(windowId & 0xFF); // Window ID
        os.write(0x00); // item cropped =  =| 0x80 , item forced |= 0x40
        os.write(x >> 8 & 0xFF); // TODO: Confirm Works
        os.write(x & 0xFF); // TODO: Confirm Works
        os.write(y >> 8 & 0xFF);
        os.write(y & 0xFF);

        subpictureCount++;
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
