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

import java.util.Iterator;
import java.util.LinkedHashSet;
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
                    RleBitmap bitmap = new RleBitmap(image);
                    bitmap.setHeight(image.getHeight());
                    bitmap.setWidth(image.getWidth());
                    bitmap.setX(event.getOffsetX());
                    bitmap.setY(y);
                    bitmaps.add(bitmap);
                    y = +bitmap.getHeight();
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
            writeSubpictureNoPreload(end, start, bitmaps);
        }

    }

    public void bitmapPacket(final RleBitmap bitmap, final BigInteger start, final BigInteger preload) throws IOException {
        BufferedImage image = bitmap.getImage();
        byte[] rleBytes = bitmap.getRle();
        int size;

        if (bitmap.size() > 0xFFFF) {
            size = 0xFFFF;
        } else {
            size = bitmap.size();
        }

        timeHeader(start.subtract(preload), start.subtract(preload));
        
        os.write(0x15);
        os.write(size >> 8 & 0xFF);
        os.write(size & 0xFF);
        os.write(0x00);
        os.write(0x00); // Object ID
        os.write(0x00); // Version number
        os.write(0xC0);
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

            int biggestWrite = 0xFFFF - 11;
            int offset = 0;
            os.write(rleBytes, offset, biggestWrite);
            offset += biggestWrite;
            biggestWrite = 0xFFFF - 4;

            while ((offset + biggestWrite) < rleBytes.length) {
                timeHeader(start.subtract(preload), start.subtract(preload));

                os.write(0x15);
                os.write(0xFF);
                os.write(0xFF);
                os.write(0x00);
                os.write(0x00); // Object ID
                os.write(0x00); // Version number
                os.write(0x00); // append switch
                os.write(rleBytes, offset, biggestWrite);
                offset += biggestWrite;
            }

            biggestWrite = rleBytes.length - offset;
            timeHeader(start.subtract(preload), start.subtract(preload));
            os.write(0x15);
            os.write(((biggestWrite + 4) >> 8) & 0xFF);
            os.write((biggestWrite + 4) & 0xFF);
            os.write(0x00); // Object ID
            os.write(0x00); // Object ID
            os.write(0x00); // Version number
            os.write(0x00); // append switch
            os.write(rleBytes, offset, biggestWrite);
        }
    }

    private void writeSubpicture(final BigInteger end, BigInteger start, final LinkedHashSet<RleBitmap> bitmaps) throws IOException {
        Iterator<RleBitmap> i = bitmaps.iterator();
        timeHeader(start, start.subtract(preloadHeader));
        subpictureHeader(resolution.getX(), resolution.getY(), 0, 0, bitmaps.size());

        RleBitmap bitmap = i.next();
        ColorTable colorTable = bitmap.getColorTable();
        int width, height, x, y;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        x = bitmap.getX();
        y = bitmap.getY();
        timeHeader(start.subtract(preloadMs), start.subtract(preloadHeader));
        bitmapHeader(width, height, x, y);
        timeHeader(start.subtract(preloadHeader), BigInteger.ZERO);
        colorTable.writeIndex(os);
        bitmapPacket(bitmap, start, preloadHeader);
        timeHeader(start.subtract(preloadBitmap), BigInteger.ZERO);
        trailer();
        timeHeader(end, end.subtract(preloadMs));
        clearSubpictureHeader(resolution.getX(), resolution.getY());
        timeHeader(end, BigInteger.ZERO);
        bitmapHeader(resolution.getX(), resolution.getY(), 0, 0);
        timeHeader(end, BigInteger.ZERO);
        trailer();
    }

    // TODO: Test of multiplexed subtitles before 64.8ms
    private void writeSubpictureNoPreload(final BigInteger end, BigInteger start, final LinkedHashSet<RleBitmap> bitmaps) throws IOException {
        timeHeader(start, start);
        subpictureHeader(resolution.getX(), resolution.getY(), 0, 0, bitmaps.size());
        Iterator<RleBitmap> i = bitmaps.iterator();

        RleBitmap bitmap = i.next();
        ColorTable colorTable = bitmap.getColorTable();
        int width, height, x, y;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        x = bitmap.getX();
        y = bitmap.getY();

        timeHeader(start, start);
        bitmapHeader(width, height, x, y);
        timeHeader(start, BigInteger.ZERO);
        colorTable.writeIndex(os);
        //timeHeader(start, start);
        bitmapPacket(bitmap, start, BigInteger.ZERO);


        timeHeader(start, BigInteger.ZERO);
        trailer();
        timeHeader(end, end);
        clearSubpictureHeader(resolution.getX(), resolution.getY());
        timeHeader(end, BigInteger.ZERO);
        bitmapHeader(resolution.getX(), resolution.getY(), 0, 0);
        timeHeader(end, BigInteger.ZERO);
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
        os.write(fpsCode); // ??
        os.write(subpictureCount >> 8 & 0xFF);
        os.write(subpictureCount & 0xFF);
        os.write(0x00);
        os.write(0x00);
        os.write(0x00);
        os.write(0x00);

        subpictureCount++;
    }

    private void subpictureHeader(final int width, final int height,
            final int widthOffset, final int heightOffset, final int numberBitmaps) throws IOException {
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

        os.write(fpsCode);
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
