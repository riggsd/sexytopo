package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.control.util.Space2DUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rls on 23/09/14.
 */
public class Sketch {

    private Set<PathDetail> pathDetails = new HashSet<>();
    private Set<TextDetail> textDetails = new HashSet<>();
    private Set<CrossSectionDetail> crossSectionDetails = new HashSet<>();
    private Set[] allSketchDetailSets = new Set[]{pathDetails, textDetails, crossSectionDetails};


    private List<SketchDetail> sketchHistory = new ArrayList<>();
    private List<SketchDetail> undoneHistory = new ArrayList<>();

    private PathDetail activePath;
    private Colour activeColour = Colour.BLACK;

    private boolean isSaved = true;

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void setPathDetails(Set<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
    }

    public void setTextDetails(Set<TextDetail> textDetails) {
        this.textDetails = textDetails;
    }


    public Set<PathDetail> getPathDetails() {
        return pathDetails;
    }


    public PathDetail getActivePath() {
        return activePath;
    }


    public PathDetail startNewPath(Coord2D start) {
        activePath = new PathDetail(start, activeColour);
        pathDetails.add(activePath);
        addSketchDetail(activePath);
        return activePath;
    }

    private void addSketchDetail(SketchDetail sketchDetail) {
        setSaved(false);
        sketchHistory.add(sketchDetail);
        undoneHistory.clear();
    }

    public void finishPath() {
        activePath = null;
    }

    public void addTextDetail(Coord2D location, String text) {
        TextDetail textDetail = new TextDetail(location, text, activeColour);
        textDetails.add(textDetail);
        addSketchDetail(textDetail);
    }

    public Set<TextDetail> getTextDetails() {
        return textDetails;
    }

    public void setActiveColour(Colour colour) {
        this.activeColour = colour;
    }


    public void undo() {
        if (!sketchHistory.isEmpty()) {
            SketchDetail toUndo = sketchHistory.remove(sketchHistory.size() - 1);

            if (toUndo instanceof PathDetail) {
                pathDetails.remove(toUndo);
            } else if (toUndo instanceof TextDetail) {
                textDetails.remove(toUndo);
            } else if (toUndo instanceof CrossSectionDetail) {
                crossSectionDetails.remove(toUndo);
            }

            undoneHistory.add(toUndo);
        }
    }


    public void redo() {

        if (!undoneHistory.isEmpty()) {
            SketchDetail toRedo = undoneHistory.remove(undoneHistory.size() - 1);

            if (toRedo instanceof PathDetail) {
                pathDetails.add((PathDetail) toRedo);
            } else if (toRedo instanceof TextDetail) {
                textDetails.add((TextDetail) toRedo);
            } else if (toRedo instanceof CrossSectionDetail) {
                crossSectionDetails.add((CrossSectionDetail) toRedo);
            }

            sketchHistory.add(toRedo);
        }
    }

    public void deleteDetail(SketchDetail sketchDetail) {
        if (sketchDetail instanceof PathDetail) {
            pathDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof CrossSectionDetail) {
            crossSectionDetails.remove(sketchDetail);
        }
    }


    public Coord2D findEligibleSnapPointWithin(Coord2D point, double delta) {

        Coord2D closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PathDetail path : pathDetails) {

            if (activePath == path) {
                continue;
            }

            Coord2D start = path.getPath().get(0);
            Coord2D end = path.getPath().get(path.getPath().size() - 1);
            for (Coord2D coord2D : new Coord2D[]{start, end}) {
                double distance = Space2DUtils.getDistance(point, coord2D);
                if (distance < delta && distance < minDistance) {
                    closest = coord2D;
                    minDistance = distance;
                }
            }
        }
        return closest;
    }


    private Set<SketchDetail> allSketchDetails() {
        Set<SketchDetail> all = new HashSet<>();
        for (Set set : allSketchDetailSets) {
            all.addAll(set);
        }
        return all;
    }

    public SketchDetail findNearestDetailWithin(Coord2D point, double delta) {

        SketchDetail closest = null;
        double minDistance = Double.MAX_VALUE;

        for (SketchDetail detail : allSketchDetails()) {
            double distance = detail.getDistanceFrom(point);
            if (distance < delta && distance < minDistance) {
                closest = detail;
                minDistance = distance;
            }
        }

        return closest;
    }


    public void addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey) {
        CrossSectionDetail sectionDetail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        crossSectionDetails.add(sectionDetail);
        addSketchDetail(sectionDetail);
    }

    public Set<CrossSectionDetail> getCrossSectionDetails() {
        return crossSectionDetails;
    }

    public void setCrossSectionDetails(Set<CrossSectionDetail> crossSectionDetails) {
        this.crossSectionDetails = crossSectionDetails;
    }
}
