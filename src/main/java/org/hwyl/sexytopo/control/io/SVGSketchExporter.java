package org.hwyl.sexytopo.control.io;

import android.graphics.Color;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * Created by driggs on 1/17/16.
 */
public class SVGSketchExporter {

    private boolean compressOutput = false;

    public void export(Survey survey, String outPath) throws IOException {
        Sketch sketch = survey.getPlanSketch();
        SVGWriter svg = new SVGWriter();

        Space<Coord2D> planSpace = Projection2D.PLAN.project(survey);

        svg.startLayer("Lineplot");
        svg.setColor(Color.RED);
        svg.addPath(planSpace.getLegMap().values());
        svg.endLayer();  // Lineplot


        svg.startLayer("Sketch");
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            svg.setColor(pathDetail.getColour());
            svg.addPolyline(pathDetail.getPath());
        }
        svg.endLayer();  // Sketch

        svg.startLayer("Stations");
        svg.setColor(Color.BLACK);
        for (Map.Entry<Station, Coord2D> entry : planSpace.getStationMap().entrySet()) {
            Coord2D coord = entry.getValue();
            Station station = entry.getKey();

            svg.addText(station.getName(), coord);
        }
        svg.endLayer();  // Stations


        File outFile = new File(outPath);
        svg.write(outFile, this.compressOutput);
    }

}
