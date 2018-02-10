/*
 * SubtitleEvent.java
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
package name.connolly.david.pgs;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SubtitleEvent implements Comparable<SubtitleEvent> {

    private static int eventCount = 0;
    
    private Timecode timecode;
    private final long id;
    private SubtitleType type;
    private BufferedImage image;

    public List<BufferedImage> subimages = new ArrayList<BufferedImage>();
    public List<Integer> subimages_x = new ArrayList<Integer>();
    public List<Integer> subimages_y = new ArrayList<Integer>();
    private List<Integer> subimages_width = new ArrayList<Integer>();
    private List<Integer> subimages_height = new ArrayList<Integer>();

    private int offsetX;
    private int offsetY;
    public boolean rendered;

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public SubtitleEvent(final Timecode timecode, final SubtitleType type) {
        this.timecode = timecode;
        this.type = type;
        id = eventCount;
        offsetX = 0;
        offsetY = 0;
        eventCount++;
    }

    public int compareTo(SubtitleEvent o) {
        return timecode.compareTo(o.timecode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SubtitleEvent other = (SubtitleEvent) obj;
        if (timecode == null) {
            if (other.timecode != null) {
                return false;
            }
        } else if (!timecode.equals(other.timecode)) {
            return false;
        }
        return true;
    }

    public long getId() {
        return id;
    }

    public long getRenderTimecode() {
        return timecode.getStart();
    }

    public Timecode getTimecode() {
        return timecode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (timecode == null ? 0 : timecode.hashCode());
        return result;
    }

    public void setType(SubtitleType type) {
        this.type = type;
    }

    public void setImage(final BufferedImage image) {
        if (this.image != null) {
            throw new RuntimeException("Image already initialized");
        }

        this.image = image;
    }

    public void setTimecode(Timecode timecode) {
        this.timecode = timecode;
    }

    public BufferedImage getImage() throws InterruptedException {
        return image;
    }

    @Override
    public String toString() {
        return "SubtitleEvent id: " + id + " " + timecode.toString();
    }

    public SubtitleType getType() {
        return type;
    }

    public enum SubtitleType {

        FIRST, // One one SubtitleEvent for Native Event or First in Sequence
        SEQUENCE;   // Substitute start of subtitle for end of last subtitle + 1
    }

    public static void lastEvent() {
        eventCount = 0;
    }

    /*
     * Pending figuring out the format!
     */
    public void addClip(int minX, int minY, int maxX, int maxY) {
        int width = maxX - minX;
        int height = maxY - minY;
        int minimumSize = 503;

        if (width * height < minimumSize) {
            // rare case that would break quantizer
            // TODO: tweak the size of the clipping upwards, instead of skipping
            return;
        }

        offsetX = minX;
        offsetY = minY;

        this.subimages_x.add(minX);
        this.subimages_y.add(minY);
        this.subimages_width.add(width);
        this.subimages_height.add(height);
    }

    public void walkClips() {
        int count = this.subimages_x.size();
        int a_origin_x = 0;
        int a_origin_y = 0;
        boolean a = false;
        int a_x = -1;
        int a_y = -1;
        int a_width = -1;
        int a_height = -1;
        int b_origin_x = image.getWidth();
        int b_origin_y = image.getHeight();
        boolean b = false;
        int b_x = -1;
        int b_y = -1;
        int b_width = -1;
        int b_height = -1;

        // compute distance from from 0,0 TRIANGLE A
        // compute distance max_x, max_y TRIANGLE B
        // add to closest either RECTANGLE A or RECTANGE B
        // (A & B) or A or B? 
        for (int c = 0; c < count; c++) {
            int c_x = this.subimages_x.get(c);
            int c_y = this.subimages_y.get(c);
            int c_width = this.subimages_width.get(c);
            int c_height = this.subimages_height.get(c);
            int a_area; 
            int b_area;

            if (!a || (a && c_x < a_x)) {
                // from origin
                a_area = (c_x - a_origin_x) * (c_y - a_origin_y);
            } else {
                // from a
                a_area = (c_x - a_x) * (c_y - a_y);
            }

            if (!b || (b && c_x > b_x)) {
                b_area = ((b_origin_x - (c_x + c_width)) * 
                          (b_origin_y - (c_y + c_height)));
            } else {
                b_area = (b_x - c_x) * (b_y - c_y);
            }

            if (a_area < b_area) {
                if (!a) {
                    a = true;
                    a_x = c_x;
                    a_y = c_y;
                    a_width = c_width;
                    a_height = c_height;
                } else {
                    if (c_x < a_x) {
                        a_x = c_x;
                    }
                    if (c_y < a_y) {
                        a_y = c_y;
                    }
                    if (c_width > a_width){
                        a_width = c_width;
                    }
                    if (c_height > a_height) {
                        a_height = c_height;
                    }
                }
            } else { 
                if (!b) {
                    b = true;
                    b_x = c_x;
                    b_y = c_y;
                    b_width = c_width;
                    b_height = c_height;
                } else {
                    if (c_x < b_x) {
                        b_x = c_x;
                    }
                    if (c_y < b_y) {
                        b_y = c_y;
                    }
                    if (c_width > b_width){
                        b_width = c_width;
                    }
                    if (c_height > b_height) {
                        b_height = c_height;
                    }
                }
            }
        }

        if (a & b) {
            System.out.println("Two Presentation Segments:\n" +
                "\ta_x: " + a_x +
                "\ta_y: " + a_y +
                "\ta_width: " + a_width +
                "\ta_height: " + a_height +
                "\ta_area: " + (a_width) * (a_height) +
                "\n\tb_x: " + b_x +
                "\tb_y: " + b_y +
                "\tb_width: " + b_width +
                "\tb_height: " + b_height +
                "\tb_area: " + (b_width) * (b_height)
            );
        } else if (a) {
            System.out.println("One Presentation Segment (top-left):\n" +
                "\ta_x: " + a_x +
                "\ta_y: " + a_y +
                "\ta_width: " + a_width +
                "\ta_height: " + a_height +
                "\ta_area: " + (a_width) * (a_height)
            );
        } else if (b) {
            System.out.println("One Presentation Segment (bottom-right):\n" +
                "\tb_x: " + b_x +
                "\tb_y: " + b_y +
                "\tb_width: " + b_width +
                "\tb_height: " + b_height +
                "\tb_area: " + (b_width) * (b_height)
            );
        }

        subimages_x.clear();
        subimages_y.clear();
        if (a) {
            subimages.add(image.getSubimage(a_x, a_y, a_width, a_height));
            subimages_x.add(a_x);
            subimages_y.add(a_y);
        }

        if (b) {
            subimages.add(image.getSubimage(b_x, b_y, b_width, b_height));
            subimages_x.add(b_x);
            subimages_y.add(b_y);
        }
    }

}
