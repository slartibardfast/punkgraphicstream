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

import name.connolly.david.pgs.util.ProgressSink;
import name.connolly.david.pgs.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EncodeRunnable implements Runnable {
	private final BlockingQueue<SubtitleEvent> encodeQueue;
	private final String filename;
	private final FrameRate fps;
	private final int quantizeThreadCount;
	private final Semaphore quantizePending;
    private final ProgressSink progress;

	public EncodeRunnable(final BlockingQueue<SubtitleEvent> encodeQueue,
			final String filename, final FrameRate fps,
			final int quantizeThreadCount, final Semaphore quantizePending,
            final ProgressSink progress) {
		this.encodeQueue = encodeQueue;
		this.filename = filename;
		this.fps = fps;
		this.quantizeThreadCount = quantizeThreadCount;
		this.quantizePending = quantizePending;
        this.progress = progress;
	}

	public void run() {
        FileOutputStream os = null;
        
        try {
            os = new FileOutputStream(filename);
            final SupGenerator packet = new SupGenerator(os, fps);
            SubtitleEvent event;
            BufferedImage indexed;
            long frameIndex = 0;
            
            // Continue while at least one quantizeThread is runing or queue is
            // not empty
            while (quantizePending.tryAcquire(quantizeThreadCount) == false | encodeQueue.size() > 0) {
                event = encodeQueue.take();
                if (event.getId() != frameIndex) {
                    //System.err
                    //		.println("Encode Request out of sequence [recieved:"
                    //				+ event.getId() + " wanted:" + frameIndex + "]");
                    if (encodeQueue.size() == 0) {
                        Thread.sleep(400);
                    }
                    encodeQueue.put(event);
                    continue;
                }
                indexed = event.takeImage();
                packet.addBitmap(indexed, 1920, 1080, event);
                //System.out.println("Encoded no.\t" + event.getId());
                frameIndex++;
            }
            
           os.flush();
        } catch (IOException ex) {
            Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                os.close();
                progress.done();
            } catch (IOException ex) {
                Logger.getLogger(EncodeRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
		
	}
}