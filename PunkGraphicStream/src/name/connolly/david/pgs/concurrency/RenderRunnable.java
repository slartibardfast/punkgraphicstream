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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.Render;
import name.connolly.david.pgs.RenderException;
import name.connolly.david.pgs.Resolution;
import name.connolly.david.pgs.SubtitleEvent;
import name.connolly.david.pgs.Timecode;
import name.connolly.david.pgs.SubtitleEvent.SubtitleType;
import name.connolly.david.pgs.util.ProgressSink;

public class RenderRunnable implements Runnable {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final String inputFilename;
    private final FrameRate fps;
    private final ProgressSink progress;
    private final Render renderer = Render.INSTANCE;
    private int quantizeThreadCount = Runtime.getRuntime().availableProcessors();
    private int renderAheadCount = 4;
    private final BlockingQueue<SubtitleEvent> quantizeQueue;
    private final EncodeQueue encodeQueue;
    private final TreeSet<Timecode> timecodes = new TreeSet<Timecode>();
    private final AtomicBoolean renderPending = new AtomicBoolean(true);
    private final Semaphore quantizePending = new Semaphore(quantizeThreadCount);
    private final int x;
    private final int y;

    public RenderRunnable(String inputFilename, String outputFilename,
            FrameRate fps, Resolution resolution, ProgressSink progress) {
        this.inputFilename = inputFilename;
        this.fps = fps;
        this.progress = progress;

        quantizeQueue = new LinkedBlockingQueue<SubtitleEvent>(renderAheadCount);
        encodeQueue = new EncodeQueue(renderAheadCount);

        for (int cpu = 0; cpu < quantizeThreadCount; cpu++) {
            new Thread(new QuantizeRunnable(quantizeQueue, encodeQueue,
                    renderPending, quantizePending, progress), "Quantizer-" + cpu).start();
        }

        new Thread(new EncodeRunnable(encodeQueue, outputFilename, fps, resolution,
                quantizeThreadCount, quantizePending, progress), "Encoder").start();

        switch (resolution) {
            case NTSC_480:
                x = 720;
                y = 480;
                break;
            case PAL_576:
                x = 720;
                y = 576;
                break;
            case HD_720:
                x = 1280;
                y = 720;
                break;
            case HD_1080:
            default:
                x = 1920;
                y = 1080;
                break;
        }
    }

    public void run() {
        try {
            renderer.init(progress);

            File file = new File(inputFilename).getCanonicalFile();

            renderer.openSubtitle(file.getParent(), file.getAbsolutePath(), x, y);

            processTimecodes();

            renderTimecodes();
        } catch (IOException ex) {
            progress.fail(ex.getMessage());
            Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final RenderException ex) {
            progress.fail(ex.getMessage());
            Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (final InterruptedException ex) {
            progress.fail(ex.getMessage());
            Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE,
                    null, ex);
        } finally {
            renderPending.set(false);

            try {
                renderer.closeSubtitle();
            } catch (RenderException ex) {
                Logger.getLogger(RenderRunnable.class.getName()).log(Level.SEVERE,
                        null, ex);
            }

        }
    }

    public void cancel() {
        cancelled.set(true);
    }

    private TreeSet<Timecode> processTimecodes() throws RenderException {
        final int eventCount = renderer.getEventCount();

        for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
            Timecode timecode = renderer.getEventTimecode(eventIndex);
            final Iterator<Timecode> i = timecodes.iterator();

            while (i.hasNext()) {
                final Timecode other = i.next();
                if (timecode.overlaps(other)) {
                    i.remove();
                    timecode = timecode.merge(other);
                }
            }
            timecodes.add(timecode);
        }

        return timecodes;
    }

    private void renderTimecodes() throws RenderException, InterruptedException {
        int percentage;
        int eventCount = timecodes.size();
        int eventIndex = 0;
        final Iterator<Timecode> i = timecodes.iterator();

        // Timecode loop: build subtitle event for timecode
        while (i.hasNext()) {
            SubtitleEvent event = new SubtitleEvent(i.next(), SubtitleType.FIRST);
            percentage = Math.round((float) eventIndex / eventCount * 100f);
            progress.progress(percentage, "Rendering Event " + eventIndex + " of " + eventCount + " (Estimated)");
            // Render loop: render, check for change, split on change
            while (event != null && !cancelled.get()) {
                Timecode timecode = event.getTimecode();
                final long end = timecode.getEnd();

                SubtitleEvent nextEvent = null;
                final BufferedImage image = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
                long detectTimecode = Math.round(timecode.getStart() + fps.milliseconds());
                boolean changed = false;
                boolean nextUnreachable = detectTimecode >= timecode.getEnd();
                // Prepare for change detect
                renderer.changeDetect(timecode.getStart());
                // Change Detect Loop
                while (!changed && !nextUnreachable) {
                    changed = renderer.changeDetect(detectTimecode) > 0;
                    nextUnreachable = detectTimecode + fps.milliseconds() > timecode.getEnd();

                    if (changed) {
                        event.setTimecode(new Timecode(timecode.getStart(), detectTimecode - 1));
                        event.setType(SubtitleType.SEQUENCE);
                        timecode = new Timecode(detectTimecode, end);
                        nextEvent = new SubtitleEvent(timecode, SubtitleType.SEQUENCE);

                        eventCount++;
                    } else if (!nextUnreachable) {
                        detectTimecode = detectTimecode + (long) fps.milliseconds();
                    }
                }

                event.setImage(image);
                renderer.render(event, image, event.getRenderTimecode());

                if (!renderer.isRunning()) {
                    // End the thread, no more images will be passed further on.
                    return;
                }
                
                quantizeQueue.put(event);
                eventIndex++;
                event = nextEvent;
            }
        }
    }
}
