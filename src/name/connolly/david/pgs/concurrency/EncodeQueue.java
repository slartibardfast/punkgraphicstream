/*
 * EncodeQueue.java
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import name.connolly.david.pgs.SubtitleEvent;

public class EncodeQueue {
    private final BlockingQueue<SubtitleEvent> encodeQueue;
    private long lastEvent;

    public EncodeQueue(int size) {
        encodeQueue = new LinkedBlockingQueue<SubtitleEvent>(size);
        lastEvent = -1;
    }

    public SubtitleEvent take() throws InterruptedException {
        SubtitleEvent event = encodeQueue.take();

        return event;
    }

    public void put(SubtitleEvent event) throws InterruptedException {
        encodeQueue.put(event);
        lastEvent = event.getId();
        this.notifyAll();
    }

    public SubtitleEvent poll(long arg0, TimeUnit arg1) throws InterruptedException {
        return encodeQueue.poll(arg0, arg1);
    }

    public long nextEvent() {
        return lastEvent + 1;

    }

    public boolean hasPending() {
        return encodeQueue.size() != 0;
    }
}
