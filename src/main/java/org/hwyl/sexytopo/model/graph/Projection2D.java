package org.hwyl.sexytopo.model.graph;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.control.util.Space3DTransformerForElevation;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Map;

/**
 * Created by rls on 26/07/14.
 */
public enum Projection2D {

    PLAN {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.getX(), coord3D.getY());
        }
    },
    ELEVATION_NS {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.getY(), coord3D.getZ());
        }
    },
    ELEVATION_EW {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.getX(), coord3D.getZ());
        }
    },
    EXTENDED_ELEVATION {
        public Coord2D project(Coord3D coord3D) {
            return ELEVATION_NS.project(coord3D);
        }
    };


    private Space<Coord3D> transform(Survey survey) {
        if (this == EXTENDED_ELEVATION) {
            Space3DTransformerForElevation transformer = new Space3DTransformerForElevation(survey);
            return transformer.transformTo3D();
        } else {
            Space3DTransformer transformer = new Space3DTransformer(survey);
            return transformer.transformTo3D();
        }
    }

    public abstract Coord2D project(Coord3D coord3D);

    public Space<Coord2D> project(Survey survey) {

        Space<Coord3D> space3D = transform(survey);

        Space<Coord2D> space2D = new Space<>();

        for (Map.Entry<Station, Coord3D> entry : space3D.getStationMap().entrySet()) {
            Station station = entry.getKey();
            Coord3D stationCoord3D = entry.getValue();
            Coord2D stationCoord2D = project(stationCoord3D);
            space2D.addStation(station, stationCoord2D);
        }

        for (Map.Entry<Leg, Line<Coord3D>> entry : space3D.getLegMap().entrySet()) {
            Leg leg = entry.getKey();
            Line<Coord3D> line3D = entry.getValue();
            Coord2D start = project(line3D.getStart());
            Coord2D end = project(line3D.getEnd());
            Line<Coord2D> line2D = new Line<>(start, end);
            space2D.addLeg(leg, line2D);
        }

        return space2D;
    }

}
