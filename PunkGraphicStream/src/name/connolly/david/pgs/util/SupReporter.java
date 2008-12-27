/*
 * SupReporter.java
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

public class SupReporter {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		InputStream is;
		PrintWriter pw;
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ArrayList<Integer> packets = new ArrayList<Integer>();
		int bytesRead = 0;

		if (args.length == 0) {
			System.out.println("Usage: TsReporter filename.ts");

			return;
		}

		try {
			is = new BufferedInputStream(new FileInputStream(args[0]));
			pw = new PrintWriter(args[0] + "-pts.html");

			pw.println("<html>");
			pw.println("<head>");
			pw.println("<title>");
			pw.println("TsReporter: " + args[0]);
			pw.println("</title>");
			pw.println("</head>");
			pw.println("<body style='width: 600px'>");

			int b;

			do {
				b = is.read();
				baos.write(b);
				bytesRead++;
				switch (b) {

				case 0x50:
					b = is.read();
					baos.write(b);
					bytesRead++;

					if (b == 0x47) {
						packets.add(bytesRead - 2);
					}

					break;
				case -1:
					packets.add(bytesRead); // final
					break;
				default:
					break;
				}
			} while (b != -1);

			// Collections.reverse(ptsBreaks);
			int count = 0;

			int start = packets.get(0);
			int end = packets.get(1);

			for (int nextEnd = 2; nextEnd < packets.size(); nextEnd++) {
				BigDecimal firstTimecode, secondTimecode;
				final byte[] buf = baos.toByteArray();
				final byte[] unsignedLongTimecode = new byte[5];
				String text;

				unsignedLongTimecode[0] = 0; // clear sign bit

				pw.println("<div style=\"font-family: monospace\">");
				pw.println("No: " + count + " ");

				text = Integer.toString(buf[start + 10] & 0xFF, 16)
						.toUpperCase();

				if (text.length() == 1) {
					text = "0" + text;
				}

				pw.print("Type: " + text + " ");

				for (int i = 1, j = start + 2; j < start + 6; i++, j++) {
					unsignedLongTimecode[i] = buf[j];
				}

				firstTimecode = new BigDecimal(new BigInteger(
						unsignedLongTimecode));
				firstTimecode = firstTimecode.divide(BigDecimal.valueOf(90), 3,
						RoundingMode.DOWN);
				pw.print("Timecode: " + firstTimecode + "ms - ");

				for (int i = 1, j = start + 6; j < start + 10; i++, j++) {
					unsignedLongTimecode[i] = buf[j];
				}

				secondTimecode = new BigDecimal(new BigInteger(
						unsignedLongTimecode));
				secondTimecode = secondTimecode.divide(BigDecimal.valueOf(90),
						3, RoundingMode.DOWN);
				pw.print(secondTimecode + "ms ");

				if (secondTimecode.compareTo(BigDecimal.valueOf(0.0001)) > 0) {
					pw.print("Duration?: "
							+ firstTimecode.subtract(secondTimecode) + "ms ");
				}

				pw.print("Size: " + (end - start + 10) + "b");
				pw.println("</div>");
				count++;
				start = end;
				end = packets.get(nextEnd);
			}
			/*
			 * for (byte b : buf.toByteArray()) { String text =
			 * Integer.toString(b & 0xFF, 16).toUpperCase();
			 * 
			 * if (text.length() == 1) { text = "0" + text; }
			 * 
			 * pw.print(text); }
			 */

			pw.println("</body>");

			pw.println("</html>");

			is.close();

			pw.flush();

			pw.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
