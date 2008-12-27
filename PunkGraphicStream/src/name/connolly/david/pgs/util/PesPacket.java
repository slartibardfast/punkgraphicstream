/*
 * PesPacket.java
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

import java.util.Arrays;

public class PesPacket {
	private final byte[] packet;
	private int payloadOffset;

	public PesPacket(final byte[] packet) {
		this.packet = packet;

		payloadOffset = 9;

		if (getPid() == 0xBD || getPid() >= 0xE0 && getPid() <= 0xEF
				|| getPid() >= 0xC0 && getPid() <= 0xDF) {

			payloadOffset += packet[0x8] & 0xFF;
		}
	}

	public byte[] getPacket() {
		return packet;
	}

	public byte[] getPayload() {
		return Arrays.copyOfRange(packet, payloadOffset, packet.length);
	}

	public int getPid() {
		return packet[3] & 0xFF;
	}
}
