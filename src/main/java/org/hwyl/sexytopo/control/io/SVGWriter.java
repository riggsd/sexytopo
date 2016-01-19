package org.hwyl.sexytopo.control.io;


import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.sketch.Colour;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Framework for writing SVG vector graphics files.
 *
 * Though its API is mostly generic, it is designed specifically for creating layered sketches
 * for import into Inkscape or Adobe Illustrator. And, while it is mostly independent of
 * SexyTopo, convenience methods that recognize its graph model geometries are provided which
 * map the top-left graphics origin to our cartesian bottom-left origin.
 *
 * It is assumed that our cartesian space (real world) units are meters, while scaled map
 * units are centimeters.
 *
 * The API is stateful in the sense that colors, stroke widths, and layer stack must be manually
 * set, and are maintained until changed.
 *
 * Created by driggs on 1/17/16.
 */
public class SVGWriter {

    private StringBuilder sb = new StringBuilder(1024);

    private final int scale;
    private double minX, maxX, minY, maxY;

    private double strokeWidth = 0.025;  // map scale (cm)
    private double fontSize = 0.075;  // map scale (cm)
    private String color = "black";

    private String indent = "";


    /**
     * Convert an Android, SexyTopo, or generic four-byte color int to SVG compatible hex value.
     */
    public static String hexColor(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }


    /**
     * Create a stateful SVG Writer instance.
     * @param scale Ratio of cave scale to map scale.
     */
    public SVGWriter(int scale) {
        this.scale = scale;
    }

    /**
     * Create a stateful SVG Writer instance at default scale of 10m = 1cm (1000:1)
     */
    public SVGWriter() {
        this(1000);
    }


    /**
     * Append a line of text while building the SVG document. Text will be indented per the
     * current layer hierarchy. <tt>String.format()</tt> formatting is supported.
     * @param line Single line of SVG XML text
     * @param args Optional <tt>String.format()</tt> values
     */
    private void svgAppend(String line, Object... args) {
        if (args.length > 0) {
            line = String.format(line, args);
        }
        this.sb.append(this.indent).append(line).append('\n');
    }


    /** Map a survey-space X coordinate into SVG space */
    private double x(double x) {
        if (x < this.minX) this.minX = x;
        if (x > this.maxX) this.maxX = x;
        return x;
    }

    /** Map a survey-space X coordinate into SVG space */
    private double x(Coord2D c) {
        return this.x(c.getX());
    }

    /** Map a survey-space Y coordinate into SVG space */
    private double y(double y) {
        if (y < this.minY) this.minY = y;
        if (y > this.maxY) this.maxY = y;
        return y;
    }

    /** Map a survey-space Y coordinate into SVG space */
    private double y(Coord2D c) {
        return this.y(c.getY());
    }

    /** Map a survey-space Y coordinate into SVG space (compensating for top-left 0,0 origin) */
    private double yFlip(Coord2D c) {
        return this.y(-c.getY());
    }

    /**
     * Set stroke width for subsequent lines, polylines, and paths.
     * @param width Stroke width in map units (cm)
     */
    public void setStrokeWidth(int width) {
        this.strokeWidth = width;
    }

    /**
     * Set stroke color for subsequent geometries and text.
     * @param color
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Set stroke color for subsequent geometries and text.
     * @param color
     */
    public void setColor(int color) {
        this.color = hexColor(color);
    }

    /**
     * Set stroke color for subsequent geometries and text.
     * @param color
     */
    public void setColor(Colour colour) {
        this.color = hexColor(colour.intValue);
    }


    /**
     * Start a new SVG layer. Layers may be nested arbitrarily deeply, but must be
     * closed with a call to <tt>endLayer()</tt>.
     * @param id Unique ID (and visible name) for the layer
     * @return New layer stack depth
     */
    public int startLayer(String id) {
        this.svgAppend("<g id='%s' inkscape:groupmode='layer' ai:layer='yes'>", id);
        this.indent = this.indent + '\t';
        return this.indent.length();
    }

    /**
     * Close an SVG layer. You must call <tt>endLayer()</tt> exactly once for every
     * corresponding creation with <tt>startLayer()</tt>.
     * @return
     */
    public int endLayer() {
        if (this.indent.length() < 1) {
            throw new IllegalStateException("SVG layers unbalanced!");
        }
        this.indent = this.indent.substring(0, this.indent.length()-1);
        this.svgAppend("</g>");
        return this.indent.length();
    }

    /**
     * Add text to the SVG document.
     * @param text
     * @param x
     * @param y
     */
    public void addText(String text, double x, double y) {
        this.svgAppend("<text x='%s' y='%s' font-size='%scm' fill='%s'>%s</text>",
                this.x(x), this.y(y), this.fontSize, this.color, text);
    }

    public void addText(String text, Coord2D coord) {
        this.addText(text, this.x(coord), this.yFlip(coord));
    }

    /**
     * Add a single SVG line segment.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public void addLine(double x1, double y1, double x2, double y2) {
        this.svgAppend(
                "<line x1='%s' y1='%s' x2='%s' y2='%s' stroke='%s' stroke-width='%s' />",
                this.x(x1), this.y(y1), this.x(x2), this.y(y2), this.color, this.strokeWidth);
    }

    public void addLine(Coord2D c1, Coord2D c2) {
        this.addLine(this.x(c1), this.yFlip(c1), this.x(c2), this.yFlip(c2));
    }

    /**
     * Add a single SVG polyline segment.
     * @param coords
     */
    public void addPolyline(List<Coord2D> coords) {
        StringBuilder sb = new StringBuilder(512);
        for (Coord2D c : coords) {
            sb.append(this.x(c)).append(',').append(this.y(c)).append(' ');
        }
        this.svgAppend(
                "<polyline stroke='%s' stroke-linecap='round' fill='none' points='%s' />",
                this.color, sb.toString().trim());
    }

    /**
     * Add a complex SVG path, which is a sort of arbitrarily complex joined line segment.
     * @param lines
     */
    public void addPath(Collection<Line<Coord2D>> lines) {
        StringBuilder sb = new StringBuilder(512);
        for (Line<Coord2D> line : lines) {
            Coord2D start = line.getStart();
            Coord2D end = line.getEnd();
            sb.append(String.format("M%s,%s L%s,%s ", this.x(start), this.yFlip(start), this.x(end), this.yFlip(end)));
        }
        this.svgAppend("<path stroke='%s' fill='none' stroke-linejoin='miter' d='%s' />", this.color, sb.toString().trim());
    }


    /**
     * Render the SVG document to String.
     * @return Complete XML document including SGML DTD declaration
     */
    public String toString() {
        double width = this.maxX - this.minX;  // meters
        double height = this.maxY - this.minY;  // meters
        double mapWidth = width / this.scale * 100;  // cm
        double mapHeight = height / this.scale * 100;  //cm

        StringBuilder svg = new StringBuilder(1024);
        svg.append("<?xml version='1.0'?>\n");
        svg.append("<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n");
        svg.append("<svg ")
                .append("xmlns='http://www.w3.org/2000/svg' ")
                .append("xmlns:xlink='http://www.w3.org/1999/xlink' ")
                .append("xmlns:inkscape='http://www.inkscape.org/namespaces/inkscape' ")
                .append("xmlns:ai='http://ns.adobe.com/AdobeIllustrator/10.0/' ")
                .append(String.format("width='%scm' height='%scm' ", mapWidth, mapHeight))
                .append(String.format("viewBox='%s,%s %s,%s'", this.minX, this.minY, width, height))
                .append(">\n");

        // debug scalebar:  should be 1cm map width, 0.1cm map height at 1000:1
        svg.append(String.format(
                "<rect x='10' y='%s' width='10' height='1' /><text x='10' y='%s' font-size='%scm' >Scale: %d:1</text>\n",
                height - 10, height - 11, this.fontSize * 0.8, this.scale));

        svg.append(this.sb);
        svg.append("</svg>");
        svg.append("\n");
        return svg.toString();
    }

    /**
     * Write the rendered SVG XML document to file.
     * @param file Output file
     * @param zip Whether output should be gzipped (eg. `.svgz' file) or uncompressed (default)
     * @throws IOException
     */
    public void write(File file, boolean zip) throws IOException {
        BufferedWriter writer = null;
        try {
            OutputStream os = new FileOutputStream(file);
            if (zip) {
                os = new GZIPOutputStream(os);
            }
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            writer = new BufferedWriter(osw);
            writer.write(this.toString());
            writer.flush();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
