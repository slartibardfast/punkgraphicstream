/*
 * QuantizeRunnable.java
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import name.connolly.david.pgs.SubtitleEvent;
import name.connolly.david.pgs.color.Quantizer;
import name.connolly.david.pgs.util.ProgressSink;

public class QuantizeRunnable implements Runnable {

    private final BlockingQueue<SubtitleEvent> quantizeQueue;
    private final EncodeQueue encodeQueue;
    private final AtomicBoolean renderPending;
    private final Semaphore quantizePending;
    private final ProgressSink progress;
    private final boolean DEBUG = true;
    
    public QuantizeRunnable(final BlockingQueue<SubtitleEvent> quantizeQueue,
            final EncodeQueue encodeQueue,
            final AtomicBoolean renderPending, final Semaphore quantizePending,
            final ProgressSink progress) {
        this.quantizeQueue = quantizeQueue;
        this.encodeQueue = encodeQueue;
        this.renderPending = renderPending;
        this.quantizePending = quantizePending;
        this.progress = progress;
    }

    public void run() {
        try {
            quantizePending.acquire();

            while (renderPending.get() || quantizeQueue.size() > 0) {
                final SubtitleEvent event;
                final BufferedImage image;

                event = quantizeQueue.poll(200, TimeUnit.MILLISECONDS);

                if (event == null) {
                    continue;
                }

                image = event.getImage();

                Quantizer.indexImage(image);

                if (DEBUG) {
                    try {
                        ImageIO.write(image, "png", new File("Rendered-" + eventIndex + ".png"));
                    } catch (IOException ex) {
                        Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                synchronized (encodeQueue) {
                    while (event.getId() != encodeQueue.nextEvent()) {
                        encodeQueue.wait();
                    }

                    encodeQueue.put(event);
                }
            }

            quantizePending.release();
        } catch (final InterruptedException ex) {
            progress.fail("Quantizer Thread Interupted");
            Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }
}
