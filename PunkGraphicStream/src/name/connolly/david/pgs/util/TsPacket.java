/*
 * TsPacket.java
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
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

import java.util.Arrays;

public class TsPacket {
	public enum Adaption {
		ADAPTATION, ADAPTATIONPAYLOAD, PAYLOAD, RESERVED
	};

	private final byte[] packet;

	public TsPacket(final byte[] buf) {
		packet = Arrays.copyOf(buf, 188);
	}

	public Adaption getAdaption() {
		final int adaption = (packet[3] & 0x30) >> 4;

		switch (adaption) {
		case 1:
			return Adaption.PAYLOAD;
		case 2:
			return Adaption.ADAPTATION;
		case 3:
			return Adaption.ADAPTATIONPAYLOAD;
		default:
			return Adaption.RESERVED;
		}
	}

	public int getContinuity() {
		return packet[3] & 0xF;
	}

	public byte[] getPacket() {
		return Arrays.copyOf(packet, 188);
	}

	public byte[] getPayload() {
		return Arrays.copyOfRange(packet, payloadOffset(), 188);
	}

	public int getPayloadLength() {
		return 188 - payloadOffset();
	}

	public boolean getPayloadStart() {
		return ((packet[1] & 0xE0) >> 6 & 0x3) == 1 ? true : false;
	}

	public int getPid() {
		int pid = packet[2];

		pid += (packet[1] & 0x1F) << 8;

		return pid;
	}

	private int payloadOffset() {
		int offset = 4;

		switch (getAdaption()) {
		case ADAPTATION:
		case ADAPTATIONPAYLOAD:
			offset += 1 + (packet[0x4] & 0xFF);
			break;
		default:
			break;
		}

		return offset;
	}
}
