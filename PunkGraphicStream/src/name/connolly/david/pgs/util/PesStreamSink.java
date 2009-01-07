/*
 * PesStreamSink.java
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

package name.connolly.david.pgs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum PesStreamSink {
	INSTANCE;
	// private Map<Integer, Boolean> dirtyStreams;
	private Map<Integer, List<PesPacket>> packets;
	private Map<Integer, ByteArrayOutputStream> streams;

	private PesStreamSink() {
		streams = new LinkedHashMap<Integer, ByteArrayOutputStream>();
		packets = new LinkedHashMap<Integer, List<PesPacket>>();
		// dirtyStreams = new LinkedHashMap<Integer, Boolean>();
	}

	public void addTsPacket(final TsPacket packet) {
		final int pid = packet.getPid();
		final byte[] payload = packet.getPayload();
		ByteArrayOutputStream stream;

		stream = streams.get(pid);

		packets.put(pid, null);

		if (stream == null) {
			stream = new ByteArrayOutputStream();
			streams.put(pid, stream);
		}
        
        try {
            stream.write(payload);
        } catch (IOException ex) {
            Logger.getLogger(PesStreamSink.class.getName()).log(Level.SEVERE, null, ex);
        }
		
	}

	public List<PesPacket> getPackets(final int pid) {
		final List<PesPacket> packetList = new ArrayList<PesPacket>();
		final ByteArrayOutputStream stream = streams.get(pid);
		int pos;
		int b;
		byte[] rawStream;
		final ByteArrayOutputStream packetStream = new ByteArrayOutputStream(
				184);

		if (stream == null)
			return packetList;
		else if (packets.get(pid) != null)
			return packets.get(pid);

		rawStream = stream.toByteArray();

		pos = 0;

		while (pos < rawStream.length) {
			b = rawStream[pos] & 0xFF;

			if (b == 0x00 && pos + 2 < rawStream.length) {
				// Read ahead to test if we have a preamble here
				if (rawStream[pos + 1] == 0x00 && rawStream[pos + 2] == 0x01) {
					// New PES Packet / Cut old
					if (packetStream.toByteArray().length > 5) {
						packetList
								.add(new PesPacket(packetStream.toByteArray()));
					} else {
						System.out.println("Ignoring non pes packet");
					}
					packetStream.reset();
				}
			}

			packetStream.write(b);
			pos++;
		}

		// Collect final packet
		if (packetStream.toByteArray().length != 0) {
			packetList.add(new PesPacket(packetStream.toByteArray()));
		}

		packets.put(pid, packetList);

		return packetList;
	}

	public void printPesPackets(final int pid) {
		final List<PesPacket> packets = getPackets(pid);

		for (final PesPacket packet : packets) {
			final byte[] payload = packet.getPayload();

			for (final byte b : payload) {
				String text = Integer.toString(b & 0xFF, 16).toUpperCase();

				if (text.length() == 1) {
					text = "0" + text;
				}

				System.out.print(text);
			}

			System.out.println();
			System.out.println();
		}
	}
}
