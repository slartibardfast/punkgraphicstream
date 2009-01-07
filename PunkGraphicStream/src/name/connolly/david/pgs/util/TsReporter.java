/*
 * TsReporter.java
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TsReporter {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		InputStream is;
		PrintWriter pw;
		final PesStreamSink sink = PesStreamSink.INSTANCE;
		final byte[] buf = new byte[188];
		int len;
		int count = 0;

		if (args.length == 0) {
			System.out.println("Usage: TsReporter filename.ts");

			return;
		}

		try {
			is = new BufferedInputStream(new FileInputStream(args[0]));
			pw = new PrintWriter("report.html");

			pw.println("<html>");
			pw.println("<head>");
			pw.println("<title>");
			pw.println("TsReporter: " + args[0]);
			pw.println("</title>");
			pw.println("</head>");
			pw.println("<body>");

			do {
				TsPacket packet;

				len = is.read(buf, 0, 188);

				packet = new TsPacket(buf);

				sink.addTsPacket(packet);
				/*
				 * if (packet.getPayloadStart() && packet.hasPesPacket()) { if
				 * (os != null) { os.flush(); os.close(); os = null; }
				 * 
				 * os = new BufferedOutputStream(new FileOutputStream("stream-"+
				 * count + "-pid-" + packet.getPid() + ".supraw"));
				 * 
				 * os.write(new
				 * PesStreamSink(packet.getPayload()).getPayload()); } else { if
				 * (os != null && packet.hasPesPacket()) { os.write(new
				 * PesStreamSink(packet.getPayload()).getPayload()); } }
				 */

				// System.out.println("Packet No: " + count + " Packet Pid: " +
				// packet.getPid() + " Adaption: " + packet.getAdaption() +
				// " Continuity: " + packet.getContinuity() + " Payload Start: "
				// + packet.getPayloadStart() + " Payload Length: " +
				// packet.getPayloadLength());
				count++;

				pw.println("<div style=\"font-family: monospace\">");

				pw.println("Packet No: " + count + " Packet Pid: "
						+ packet.getPid() + " Adaption: "
						+ packet.getAdaption() + " Continuity: "
						+ packet.getContinuity() + " Payload Start: "
						+ packet.getPayloadStart() + " Payload Length: "
						+ packet.getPayloadLength());

				for (final byte b : buf) {
					String text = Integer.toString(b & 0xFF, 16).toUpperCase();

					if (text.length() == 1) {
						text = "0" + text;
					}

					pw.print(text);
				}

				pw.println();

				pw.println("</div>");
			} while (len != -1);

			pw.println("</body>");

			pw.println("</html>");

			is.close();

			pw.flush();

			pw.close();

			sink.printPesPackets(4608);
		} catch (final IOException ex) {
			Logger.getLogger(SupReporter.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
