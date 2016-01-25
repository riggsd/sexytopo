package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.control.util.StationRotator;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a cross-section of a passage at a specified angle on a top-down plan sketch.
 */
public class CrossSection {

    private final Survey survey;
    private final Station station;
    private final double angle;

    private Space3DTransformer transformer;

    public CrossSection(Survey survey, Station station, double angle) {
        this.survey = survey;
        this.station = station;
        this.angle = angle;
        this.transformer  = new Space3DTransformer(survey);
    }

    public Space<Coord2D> getProjection() {

        Space<Coord2D> projection = new Space<>();
        projection.addStation(station, Coord2D.ORIGIN);

        for (Leg leg : station.getUnconnectedOnwardLegs()) {
            // first of all normalise to match the angle of the cross section
            Leg rotated = leg.rotate(-angle);
            Coord3D coord3D = transformer.transform(Coord3D.ORIGIN, rotated);
            Coord2D coord2D = new Coord2D(coord3D.getX(), coord3D.getZ());
            Line<Coord2D> line = new Line<>(Coord2D.ORIGIN, coord2D);
            projection.addLeg(rotated, line);
        }

        return projection;
    }

    public Station getStation() {
        return station;
    }

    public double getAngle() {
        return angle;
    }
}
