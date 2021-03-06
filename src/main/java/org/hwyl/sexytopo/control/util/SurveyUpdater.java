package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by rls on 23/07/14.
 */
public class SurveyUpdater {

    static {
        //MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.sound_file_1);
        //mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }


    private static final double MAX_DISTANCE_DIFF = 0.2;
    private static final double MAX_AZIMUTH_DIFF = 1;
    private static final double MAX_INCLINATION_DIFF = 1;


    public static void update(Survey survey, List<Leg> legs, boolean considerBacksightPromotion) {
        for (Leg leg : legs) {
            update(survey, leg, considerBacksightPromotion);
        }
    }


    public static void update(Survey survey, List<Leg> legs) {
        for (Leg leg : legs) {
            update(survey, leg);
        }
    }

    public static void update(Survey survey, Leg leg) {
        update(survey, leg, false);
    }

    public static void update(Survey survey, Leg leg, boolean considerBacksightPromotion) {
        Station activeStation = survey.getActiveStation();
        activeStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addUndoEntry(activeStation, leg);

        if (considerBacksightPromotion) {
            boolean createdNewStation = createNewStationIfBacksight(survey);
            if (!createdNewStation) {
                createNewStationIfTripleShot(survey);
            }
        } else {
            createNewStationIfTripleShot(survey);
        }
    }


    public static void updateWithNewStation(Survey survey, Leg leg) {
        Station activeStation = survey.getActiveStation();

        if (!leg.hasDestination()) {
            Station newStation =
                    new Station(StationNamer.generateNextStationName(survey, activeStation));
            leg = Leg.upgradeSplayToConnectedLeg(leg, newStation);
        }

        // FIXME; could the below be moved into Survey? And from elsewhere in this file?
        activeStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addUndoEntry(activeStation, leg);
        survey.setActiveStation(leg.getDestination());
    }


    private static boolean createNewStationIfTripleShot(Survey survey) {

        Station activeStation = survey.getActiveStation();
        if (activeStation.getOnwardLegs().size() >= SexyTopo.NUM_OF_REPEATS_FOR_NEW_STATION) {

            List<Leg> legs = getLatestNLegs(activeStation, SexyTopo.NUM_OF_REPEATS_FOR_NEW_STATION);

            if (areLegsAboutTheSame(legs)) {
                Station newStation = new Station(StationNamer.generateNextStationName(survey, activeStation));

                Leg newLeg = averageLegs(legs);
                newLeg = Leg.upgradeSplayToConnectedLeg(newLeg, newStation);

                activeStation.getOnwardLegs().removeAll(legs);
                activeStation.getOnwardLegs().add(newLeg);

                for (Leg leg : legs) {
                    survey.undoLeg();
                }
                survey.addUndoEntry(activeStation, newLeg);

                survey.setActiveStation(newStation);
                return true;
            }
        }
        return false;
    }


    private static boolean createNewStationIfBacksight(Survey survey) {
        // Examine splays of the current active station to determine if the previous two were a
        // foreward and backsight; if so, promote to a new named station
        Station activeStation = survey.getActiveStation();
        if (activeStation.getOnwardLegs().size() >= 2) {

            List<Leg> legs = getLatestNLegs(activeStation, 2);
            Leg fore = legs.get(legs.size() - 2);
            Leg back = legs.get(legs.size() - 1);  // TODO: check for "reverse mode" to see if backsight comes first?

            if (areLegsBacksights(fore, back)) {
                Station newStation = new Station(StationNamer.generateNextStationName(survey, activeStation));

                Leg newLeg = averageBacksights(fore, back);
                newLeg = Leg.upgradeSplayToConnectedLeg(newLeg, newStation);

                activeStation.getOnwardLegs().removeAll(legs);
                activeStation.getOnwardLegs().add(newLeg);

                for (Leg leg : legs) {
                    survey.undoLeg();
                }
                survey.addUndoEntry(activeStation, newLeg);

                survey.setActiveStation(newStation);
                return true;
            }
        }
        return false;
    }


    public static void editLeg(Survey survey, final Leg toEdit, final Leg edited) {
        SurveyTools.traverseLegs(
                survey,
                new SurveyTools.SurveyLegTraversalCallback() {
                    @Override
                    public boolean call(Station origin, Leg leg) {
                        if (leg == toEdit) {
                            origin.getOnwardLegs().remove(toEdit);
                            origin.getOnwardLegs().add(edited);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
        survey.setSaved(false);
    }

    public static void editStation(Survey survey, Station toEdit, Station edited) {

        boolean weAreRenamingAStation = ! edited.getName().equals(toEdit);
        if (weAreRenamingAStation) {
            Station existing = survey.getStationByName(edited.getName());
            if (existing != null) {
                throw new IllegalArgumentException("New station name is not unique");
            }
        }

        if (toEdit == survey.getOrigin()) {
            survey.setOrigin(edited);
        } else {
            Leg referringLeg = survey.getReferringLeg(toEdit);
            Leg editedLeg = new Leg(referringLeg, edited);
            editLeg(survey, referringLeg, editedLeg);
        }

        if (survey.getActiveStation() == toEdit) {
            survey.setActiveStation(edited);
        }

        survey.setSaved(false);
    }

    public static void renameStation(Survey survey, Station station, String name) {
        Station renamed = new Station(station, name);
        editStation(survey, station, renamed);
    }


    public static void deleteLeg(Survey survey, final Leg toDelete) {
        survey.undoLeg(toDelete);
    }


    private static List<Leg> getLatestNLegs(Station station, int n) {
        List<Leg> legs = station.getOnwardLegs();
        List<Leg> lastNLegs = new ArrayList<>(legs.subList(legs.size() - n, legs.size()));
        return lastNLegs;
    }


    private static boolean areLegsAboutTheSame(List<Leg> legs) {
        double minDistance = Double.POSITIVE_INFINITY, maxDistance = Double.NEGATIVE_INFINITY;
        double minAzimuth = Double.POSITIVE_INFINITY, maxAzimuth = Double.NEGATIVE_INFINITY;
        double minInclination = Double.POSITIVE_INFINITY, maxInclination = Double.NEGATIVE_INFINITY;

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            minAzimuth = Math.min(leg.getAzimuth(), minAzimuth);
            maxAzimuth = Math.max(leg.getAzimuth(), maxAzimuth);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        double distanceDiff = maxDistance - minDistance;
        double azimuthDiff = maxAzimuth - minAzimuth;
        double inclinationDiff = maxInclination - minInclination;

        return distanceDiff <= MAX_DISTANCE_DIFF &&
               azimuthDiff <= MAX_AZIMUTH_DIFF &&
               inclinationDiff <= MAX_INCLINATION_DIFF;
    }


    public static boolean areLegsBacksights(Leg fore, Leg back) {
        // Given two legs, determine if they are in agreement as foresight and backsight.
        Leg correctedBack = back.asBacksight();
        return areLegsAboutTheSame(Arrays.asList(fore, correctedBack));
    }

    public static Leg averageLegs(List<Leg> repeats) {
        int count = repeats.size();
        double distance = 0.0, inclination = 0.0;
        double[] azimuths = new double[count];
        for (int i=0; i < count; i++) {
            Leg leg = repeats.get(i);
            distance += leg.getDistance();
            inclination += leg.getInclination();
            azimuths[i] = leg.getAzimuth();
        }
        distance /= count;
        inclination /= count;
        return new Leg(distance, averageAzimuths(azimuths), inclination);
    }


    public static Leg averageBacksights(Leg fore, Leg back) {
        // Given a foresight and backsight which may not exactly agree, produce an averaged foresight
        return averageLegs(Arrays.asList(fore, back.asBacksight()));
    }


    /** Average some azimuth values together, even if they span the 360/0 boundary */
    private static double averageAzimuths(double[] azms) {
        // Azimuth values jump at the 360/0 boundary, so we must be careful to ensure that
        // values {359, 1} average to 0 rather than the incorrect value 180
        double sum = 0.0;
        double min = Leg.MAX_AZIMUTH, max = Leg.MIN_AZIMUTH;
        for (int i=0; i<azms.length; i++) {
            if (azms[i] < min) min = azms[i];
            if (azms[i] > max) max = azms[i];
        }
        boolean splitOverZero = max - min > 180;
        double[] correctedAzms = new double[azms.length];
        for (int i=0; i < azms.length; i++) {
            correctedAzms[i] = (splitOverZero && azms[i] < 180) ? azms[i] + 360: azms[i];
            sum += correctedAzms[i];
        }
        return (sum / correctedAzms.length) % 360;
    }


    public static void reverseLeg(Survey survey, final Station toReverse) {
        Log.d("reversing " + toReverse.getName());
        SurveyTools.traverseLegs(
                survey,
                new SurveyTools.SurveyLegTraversalCallback() {
                    @Override
                    public boolean call(Station origin, Leg leg) {
                        if (leg.hasDestination() && leg.getDestination() == toReverse) {
                            Leg reversed = leg.reverse();
                            origin.getOnwardLegs().remove(leg);
                            origin.addOnwardLeg(reversed);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        survey.setSaved(false);
    }

}
