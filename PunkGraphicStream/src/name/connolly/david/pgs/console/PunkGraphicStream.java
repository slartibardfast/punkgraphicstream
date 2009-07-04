/*
 * PunkGraphicStream.java
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
package name.connolly.david.pgs.console;

import name.connolly.david.pgs.FrameRate;
import name.connolly.david.pgs.Resolution;
import name.connolly.david.pgs.concurrency.RenderRunnable;
import name.connolly.david.pgs.util.ProgressSink;

public class PunkGraphicStream {

    public static void main(final String[] args) {
        final String input;
        String output = "default.ass";
        FrameRate frameRate = FrameRate.FILM;
        Resolution resolution = Resolution.HD_1080;
        
        System.setProperty("java.awt.headless", "true");

        if (args.length != 2) {
            printUsageAndQuit();
        }

        input = args[0];

        try {
            frameRate = FrameRate.valueOf(args[1].toUpperCase());
        } catch (final IllegalArgumentException e) {
            printUsageAndQuit();
        }

        try {
            resolution = Resolution.valueOf(args[2].toUpperCase());
        } catch (final IllegalArgumentException e) {
            printUsageAndQuit();
        }

        if (input.length() > 5) {
            output = input.substring(0, input.length() - 4) + ".sup";
        } else {
            printUsageAndQuit();
        }

        new Thread(new RenderRunnable(input, output, frameRate,
                resolution, new ProgressSink() {

            public void progress(int percentage, String message) {
                System.out.println(message);
            }

            public void done() {
                System.out.println("Encode of " + input + " Done");
            }

            public void fail(String message) {
                System.out.println("Encode of " + input + " Failed - " + message);
            }

            public void renderMessage(String message) {
                System.out.println(message);
            }
        })).start();
    }

    private static void printUsageAndQuit() {
        System.out.println("Usage: ");
        System.out.println("java -jar PunkGraphicStream.jar filename.ass fps resolution");
        System.out.println();
        System.out.println("fps = [film, film_ntsc, pal, ntsc, hd_pal, hd_ntsc]");
        System.out.println("resolution = [ntsc_480, pal_576, hd_720, hd_1080]");

        printLicence();

        System.exit(0);
    }

    private static void printLicence() {
        System.out.println("PunkGraphicStream 0.6");
        System.out.println("Copyright 2009 David Connolly. All rights reserved.");
        System.out.println();
        System.out.println("This is free software; see sources for copying conditions and credits of dependencies.");
        System.out.println("There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE");
        System.out.println();
        System.out.println("Portions of this software contain:");
        System.out.println("NeuQuant Neural-Net Quantization Algorithm");
        System.out.println("Copyright (c) 1994 Anthony Dekker");
        System.out.println("NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994.");
        System.out.println("See \"Kohonen neural networks for optimal colour quantization\" in \"Network: Computation in Neural Systems\" Vol. 5 (1994) pp 351-367. for a discussion of the algorithm.");
        System.out.println();
        System.out.println("See also: http://www.acm.org/~dekker/NEUQUANT.HTML");
    }
}
