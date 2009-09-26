/*
 * Quantizer.java
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
package name.connolly.david.pgs.color;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Quantizer {

    public static BufferedImage indexImage(BufferedImage image) {
        return indexImage(image, 256);
    }

    public static BufferedImage indexImage(BufferedImage image, int numColors) {
        NeuQuant nq;

        try {
            final int x = image.getWidth();
            final int y = image.getHeight();
            nq = new NeuQuant(image, x, y, numColors);
            nq.init();
            int argb;

            for (int yIndex = 0; yIndex < y; yIndex++) {
                for (int xIndex = 0; xIndex < x; xIndex++) {
                    argb = image.getRGB(xIndex, yIndex);
                    image.setRGB(xIndex, yIndex, nq.convert(argb));
                }
            }
        } catch (final IOException ex) {
            Logger.getLogger(Quantizer.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return image;
    }
}
