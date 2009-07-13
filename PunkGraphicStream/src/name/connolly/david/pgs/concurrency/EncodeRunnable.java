/*
 * EncodeRunnable.java
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
package name.connolly.david.pgs.concurrency;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.Render;
import name.connolly.david.pgs.Resolution;
import name.connolly.david.pgs.SubtitleEvent;
import name.connolly.david.pgs.SupGenerator;
import name.connolly.david.pgs.util.ProgressSink;

public class EncodeRunnable implements Runnable {

    private final EncodeQueue encodeQueue;
    private final String filename;
    private final FrameRate fps;
    private final Resolution resolution;
    private final int quantizeThreadCount;
    private final Semaphore quantizePending;
    private final ProgressSink progress;

    public EncodeRunnable(final EncodeQueue encodeQueue,
            final String filename, final FrameRate fps, final Resolution resolution,
            final int quantizeThreadCount, final Semaphore quantizePending,
            final ProgressSink progress) {
        this.encodeQueue = encodeQueue;
        this.filename = filename;
        this.fps = fps;
        this.resolution = resolution;
        this.quantizeThreadCount = quantizeThreadCount;
        this.quantizePending = quantizePending;
        this.progress = progress;
    }

    @Override
    public void run() {
        OutputStream os = null;

        try {
            SubtitleEvent event;
            final SupGenerator packet;
            long encodeIndex = 0;
            boolean quantizeThreadsActive = quantizePending.tryAcquire(quantizeThreadCount) == false;

            os = new BufferedOutputStream(new FileOutputStream(filename));
            packet = new SupGenerator(os, fps, resolution, progress);

            // Continue while at least one quantizeThread is
            // running or queue is not empty.
            while (quantizeThreadsActive || encodeQueue.hasPending()) {
                event = encodeQueue.poll(200, TimeUnit.MILLISECONDS);
                quantizeThreadsActive = quantizePending.tryAcquire(quantizeThreadCount) == false;

                if (!quantizeThreadsActive) {
                    quantizePending.release(quantizeThreadCount); // For Next Run
                }

                if (event != null) {
                    packet.addEvent(event);

                    encodeIndex++;
                }
            }

            os.flush();
        } catch (final IOException ex) {
            progress.fail(ex.getMessage());
            Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (final InterruptedException ex) {
            progress.fail(ex.getMessage());
            Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE,
                    null, ex);
        } finally {
            try {
                os.close();

            } catch (final IOException ex) {
                progress.fail(ex.getMessage());
                Logger.getLogger(EncodeRunnable.class.getName()).log(
                        Level.SEVERE, null, ex);
            } finally {
                os = null;
            }
        }
        
        SubtitleEvent.lastEvent();

        if (Render.isRunning()) {
            progress.done();
        }
    }
}
