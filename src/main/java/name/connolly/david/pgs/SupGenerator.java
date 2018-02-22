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

import name.connolly.david.pgs.color.ColorTable;
import name.connolly.david.pgs.debug.SupOutputStream;
import name.connolly.david.pgs.util.ProgressSink;

public class SupGenerator {

    final FrameRate fps;
    private int fpsCode;
    private final SupOutputStream out;
    // all in ticks
    private long preloadHeader = 5832;
    private long preloadBitmap = 5652;
    private long preloadMs = 90;
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
        try {
            writeEvent(event);
        } catch (BitmapOversizeException e) {
            progress.fail("Subtitle image too large. Try to reduce effects, font size or number of characters.");
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            progress.fail("Subtitle event #" + event.getId() + " failed.");
            e.printStackTrace();
        }

    }

    public void bitmapPacket(int objectId, final RleBitmap bitmap, final long from, final long to) throws IOException {
        BufferedImage image = bitmap.getImage();
        byte[] rleBytes = bitmap.getRle();
        int size;
        int biggestWrite = 0xFFE4;
        int objectSize = bitmap.objectSize();
        size = bitmap.firstSize();
        
        if (size >= 0xFFEF) {
            size = 0xFFEF;
        }

        timeHeader(from, to);
        out.write(0x15); // Object Segment 
        out.write((size >> 8) & 0xFF);
        out.write(size & 0xFF);
        out.write((objectId >> 8) & 0xFF);
        out.write(objectId & 0xFF); 
        out.write(0x00); // version
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
            biggestWrite = 0xFFEB;
            while ((offset + biggestWrite) <= rleBytes.length) {
                timeHeader(from, to);
                out.write(0x15); // Object Segment 
                out.write(0xFF); // Max Size
                out.write(0xEF); // Max Size
                out.write((objectId >> 8) & 0xFF);
                out.write(objectId & 0xFF); 
                out.write(0x00); // version
                out.write(0x00); // append switch?
                out.write(rleBytes, offset, biggestWrite);
                offset += biggestWrite;
            }

            biggestWrite = rleBytes.length - offset;
            if (biggestWrite > 0) {
            timeHeader(from, to);
            out.write(0x15); // Object Segment 
            out.write(((biggestWrite + 0x4) >> 8) & 0xFF);
            out.write((biggestWrite + 0x4) & 0xFF);
            out.write((objectId >> 8) & 0xFF);
            out.write(objectId & 0xFF); 
            out.write(0x00); // version
            out.write(0x40); // last in sequence?
            out.write(rleBytes, offset, biggestWrite);
            }
        }
    }

    private void writeEvent(final SubtitleEvent event) throws Exception {
        Timecode timecode = event.getTimecode();
        long start = timecode.getStartTicks();
        long end = timecode.getEndTicks();
        BufferedImage image = event.getImage();
        RleBitmap bitmap = new RleBitmap(image, 0, 0);
        ColorTable colorTable = bitmap.getColorTable();

        timeHeader(start, start - preloadHeader > 0 ? start - preloadHeader: start);
        subpictureHeader(event);
        timeHeader(start - preloadMs, start - preloadHeader);
        windowsHeader(this.resolution.getWidth(), this.resolution.getHeight(), 0, 0);
        timeHeader(start - preloadHeader, 0);
        colorTable.writeIndex(out);
        // TODO: Non - shared palette
        if (event.subimages.size() >= 1) {
            RleBitmap bitmap0 = new RleBitmap(event.subimages.get(0),event.subimages_x.get(0), event.subimages_y.get(0));
            bitmapPacket(0, bitmap0, start - preloadBitmap, start -preloadHeader);
        }
        if (event.subimages.size() >= 2) {
            RleBitmap bitmap1 = new RleBitmap(event.subimages.get(1),event.subimages_x.get(1), event.subimages_y.get(1));
            bitmapPacket(1, bitmap1, start - preloadBitmap, start - preloadHeader);
        }
        timeHeader(start - preloadBitmap, 0);
        trailer();
        timeHeader(end, end - preloadMs);
        subpictureHeader(null);
        timeHeader(end, 0);
        windowsHeader(this.resolution.getWidth(), this.resolution.getHeight(), 0, 0);
        timeHeader(end, 0);
        trailer();
    }

    private void windowsHeader(final int width, final int height,
            final int widthOffset, final int heightOffset) throws IOException {
        // 17 00 0A 01 00 00 00 03 F7 07 80 00 31
        out.write(0x17);
        out.write(0x00); // (size)
        out.write(0x0A); // (size)
        out.write(0x01); // (count of obj???)
        out.write(0x00); // (window id???)
        out.write(widthOffset >> 8 & 0xFF); // TODO: Confirm Works
        out.write(widthOffset & 0xFF); // TODO: Confirm Works
        out.write(heightOffset >> 8 & 0xFF);
        out.write(heightOffset & 0xFF);
        out.write(width >> 8 & 0xFF);
        out.write(width & 0xFF);
        out.write(height >> 8 & 0xFF);
        out.write(height & 0xFF);
    }

    private void subpictureHeader(SubtitleEvent event) throws Exception {
        out.write(0x16);

        // Size of Header
        out.write(0x00);
        if (event == null) {
            out.write(0x0B);
        } else if (event.subimages.size() == 1) {
            out.write(0x13);
        } else if (event.subimages.size() == 2) {
            out.write(0x1B);
        } else {
            throw new Exception("error wrong size");
        }

        // Size of Picture
        out.write(resolution.getWidth() >> 8 & 0xFF);
        out.write(resolution.getWidth() & 0xFF);
        out.write(resolution.getHeight() >> 8 & 0xFF);
        out.write(resolution.getHeight() & 0xFF);
        out.write(fpsCode);
        if (event == null) {
            out.write(0x00); // presentation id
            out.write(0x01); // presentation id
            out.write(0x00); // State
        } else {
            out.write(0x00); // presentation id
            out.write(0x00); // presentation id
            out.write(0x80); // State
        }
        out.write(0x00); // Pallette Update Flags
        out.write(0x00); // Pallette Id ref
        
        if (event == null) {
            out.write(0x00);
            return;
        } else {
            out.write(event.subimages.size()); // object count
        }
        
        if (event.subimages.size() >= 1) {
            out.write(0x00); // object id
            out.write(0x00); // object id
            out.write(0x00); // 1st object (ref to window id)
            out.write(0x00); // 1st object composition flag item cropped =  =| 0x80 , item forced |= 0x40
            out.write(event.subimages_x.get(0) >> 8 & 0xFF);
            out.write(event.subimages_x.get(0) & 0xFF);
            out.write(event.subimages_y.get(0) >> 8 & 0xFF);
            out.write(event.subimages_y.get(0) & 0xFF);
        }
        if (event.subimages.size() >= 2) {
            out.write(0x00); // object id
            out.write(0x01); // object id
            out.write(0x00); // 1st object (ref to window id)
            out.write(0x00); // 1st object composition flag item cropped =  =| 0x80 , item forced |= 0x40
            out.write(event.subimages_x.get(1) >> 8 & 0xFF);
            out.write(event.subimages_x.get(1) & 0xFF);
            out.write(event.subimages_y.get(1) >> 8 & 0xFF);
            out.write(event.subimages_y.get(1) & 0xFF);
        }
    }

    private void timeHeader(final long from, final long to)
            throws IOException {
        String fromBytes;
        String toBytes;

        // one ninetieth of a millisecond
        fromBytes = Long.toHexString(from);
        toBytes = Long.toHexString(to);

        if (fromBytes.length() > 8 || toBytes.length() > 8) {
            throw new RuntimeException("Timecode is too big");
        }

        // TODO: A non String version; When its not 2am!
        while (fromBytes.length() < 8) {
            fromBytes = "0" + fromBytes;
        }

        while (toBytes.length() < 8) {
            toBytes = "0" + toBytes;
        }

        out.writeSupHeader(fromBytes, toBytes);
    }

    private void trailer() throws IOException {
        out.write(0x80);
        out.write(0);
        out.write(0);
    }
}
