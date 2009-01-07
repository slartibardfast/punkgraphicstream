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
package name.connolly.david.pgs.concurrency;

import name.connolly.david.pgs.util.ProgressSink;
import name.connolly.david.pgs.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RenderRunnable implements Runnable {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final String inputFilename;
    private final String outputFilename;
    private final FrameRate fps;
    private final boolean animated;
    private final ProgressSink progress;

    public RenderRunnable(String inputFilename, String outputFilename,
            FrameRate fps, boolean animated, ProgressSink progress) {
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
        this.fps = fps;
        this.animated = animated;
        this.progress = progress;
    }

    public void run() {
        final Render r = Render.INSTANCE;
        final int quantizeThreadCount = Runtime.getRuntime().availableProcessors();
        int eventIndex = 0;
        int frameIndex = 0;
        int eventCount;
        int percentage;
        final int renderCount = quantizeThreadCount * 2 - 1;
        final BlockingQueue<SubtitleEvent> quantizeQueue;
        final BlockingQueue<SubtitleEvent> encodeQueue;
        final AtomicBoolean renderPending = new AtomicBoolean(true);
        final Semaphore quantizePending = new Semaphore(quantizeThreadCount);

        r.openSubtitle(inputFilename);

        eventCount = r.getEventCount();

        quantizeQueue = new LinkedBlockingQueue<SubtitleEvent>(renderCount);
        encodeQueue = new LinkedBlockingQueue<SubtitleEvent>();

        for (int cpu = 0; cpu < quantizeThreadCount; cpu++) {
            new Thread(new QuantizeRunnable(quantizeQueue, encodeQueue,
                    renderPending, quantizePending, progress)).start();
        }

        new Thread(new EncodeRunnable(encodeQueue, outputFilename, fps,
                quantizeThreadCount, quantizePending, progress)).start();

        SubtitleEvent event = r.getEvent(eventIndex);
        long frameRenderUntilTimecode = 0;
        percentage = Math.round((float) eventIndex / eventCount * 100f);
        progress.progress(percentage,
                "Rendering Event No. " + eventIndex + " of " + eventCount);

        while (event != null && !cancelled.get()) {
            final BufferedImage currentFrame;
            SubtitleEvent nextEvent = null;
            int detectChange;

            currentFrame = new BufferedImage(1920, 1080,
                    BufferedImage.TYPE_INT_ARGB);

            if (animated) {
                r.render(currentFrame, event.getRenderTimecode());

                if (frameRenderUntilTimecode == 0) {
                    long detectChangeTimecode = event.getTimecode();
                    long detectChangeEndTimecode = event.getTimecode() + (event.getDuration() - 1);
                    detectChange = 0;

                    // Check each frame to see if animation has occured
                    while (detectChangeTimecode < detectChangeEndTimecode && detectChange == 0) {
                        detectChange = r.changeDetect(detectChangeTimecode);
                        detectChangeTimecode += fps.frameDurationInMilliseconds();
                    }

                    if (detectChange > 0) {
                        frameRenderUntilTimecode = event.getDuration() + event.getRenderTimecode();

                        event.setFrameRate(fps);
                        frameIndex++;
                        percentage = Math.round((float) eventIndex / eventCount * 100f);
                        progress.progress(percentage,
                                "Rendering Animated Event No. " + eventIndex);

                        nextEvent = new SubtitleEvent(event, fps, frameIndex);
                    }
                } else if (frameRenderUntilTimecode > event.getRenderTimecode() + fps.frameDurationInMilliseconds()) {
                    frameIndex++;
                    //System.out.println("Rendering Frame No.\t" + frameIndex);
                    nextEvent = new SubtitleEvent(event, fps, frameIndex);
                }
            } else {
                r.render(currentFrame, event.getAverageTimecode());
            }

            if (nextEvent == null) {
                frameRenderUntilTimecode = 0;
                frameIndex = 0;
                eventIndex++;
                percentage = Math.round((float) eventIndex / eventCount * 100f);
                progress.progress(percentage,
                        "Rendering Event No. " + eventIndex + " of " + eventCount);

                nextEvent = r.getEvent(eventIndex);
            }

            event.putImage(currentFrame);
            
            try {
                quantizeQueue.put(event);
            } catch (InterruptedException ex) {
                Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
            

            event = nextEvent;
        }

        renderPending.set(false);

        r.closeSubtitle();
    }

    public void cancel() {
        cancelled.set(true);
    }
}
