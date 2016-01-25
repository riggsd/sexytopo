package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    public static final String PREF_RECOGNIZE_BACKSIGHTS_KEY = "pref_recognize_backsights";

    static {
        //MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.sound_file_1);
        //mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }


    private static final double MAX_DISTANCE_DIFF = 0.2;
    private static final double MAX_BEARING_DIFF = 1;
    private static final double MAX_INCLINATION_DIFF = 1;

    private final Survey survey;
    private final Context context;
    private final SharedPreferences preferences;


    public SurveyUpdater(Survey survey, Context context) {
        this.survey = survey;
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void update(List<Leg> legs) {
        for (Leg leg : legs) {
            update(leg);
        }
    }


    public void update(Leg leg) {
        Station activeStation = survey.getActiveStation();
        activeStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addUndoEntry(activeStation, leg);

        if (preferences.getBoolean(PREF_RECOGNIZE_BACKSIGHTS_KEY, false)) {
            createNewStationIfBacksight();
        }
        createNewStationIfTripleShot();
    }


    public void updateWithNewStation(Leg leg) {
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


    /**
     * Examine splays of the current active station to determine if we should promote a
     * "triple-shot" of close-enough splays to a new named station.
     * @return <tt>true</tt> if new station was created
     */
    private boolean createNewStationIfTripleShot() {

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


    /**
     * Examine splays of the current active station to determine if the previous two were a
     * foreward and backsight; if so, promote to a new named station.
     * @return <tt>true</tt> if new station was created
     */
    private boolean createNewStationIfBacksight() {

        Station activeStation = survey.getActiveStation();
        if (activeStation.getOnwardLegs().size() >= 2) {

            List<Leg> legs = getLatestNLegs(activeStation, 2);
            Leg fore = legs.get(legs.size()-2);
            Leg back = legs.get(legs.size()-1);  // TODO: check for "reverse mode" to see if backsight comes first?

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


    public void editLeg(final Leg toEdit, final Leg edited) {
        SurveyTools.traverse(
                survey,
                new SurveyTools.SurveyTraversalCallback() {
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

    public void editStation(Station toEdit, Station edited) {

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
            editLeg(referringLeg, editedLeg);
        }

        if (survey.getActiveStation() == toEdit) {
            survey.setActiveStation(edited);
        }

        survey.setSaved(false);
    }

    public void renameStation(Station station, String name) {
        Station renamed = new Station(station, name);
        editStation(station, renamed);
    }


    public void deleteLeg(final Leg toDelete) {
        survey.undoLeg(toDelete);
    }


    private static List<Leg> getLatestNLegs(Station station, int n) {
        List<Leg> legs = station.getOnwardLegs();
        List<Leg> lastNLegs = new ArrayList<>(legs.subList(legs.size() - n, legs.size()));
        return lastNLegs;
    }


    private static boolean areLegsAboutTheSame(List<Leg> legs) {
        double minDistance = Double.POSITIVE_INFINITY, maxDistance = Double.NEGATIVE_INFINITY;
        double minBearing = Double.POSITIVE_INFINITY, maxBearing = Double.NEGATIVE_INFINITY;
        double minInclination = Double.POSITIVE_INFINITY, maxInclination = Double.NEGATIVE_INFINITY;

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            minBearing = Math.min(leg.getBearing(), minBearing);
            maxBearing = Math.max(leg.getBearing(), maxBearing);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        double distanceDiff = maxDistance - minDistance;
        double bearingDiff = maxBearing - minBearing;
        double inclinationDiff = maxInclination - minInclination;

        return distanceDiff <= MAX_DISTANCE_DIFF &&
               bearingDiff <= MAX_BEARING_DIFF &&
               inclinationDiff <= MAX_INCLINATION_DIFF;
    }


    /**
     * Given two legs, determine if they are in agreement as foresight and backsight.
     * @param fore Presumed shot FROM->TO
     * @param back Presumed shot TO->FROM
     * @return
     */
    public static boolean areLegsBacksights(Leg fore, Leg back) {
        Leg correctedBack = back.asBacksight();
        return areLegsAboutTheSame(Arrays.asList(fore, correctedBack));
    }


    /**
     * Given some legs that we expect are "about the same", average their values into one leg.
     * @param repeats
     * @return
     */
    public static Leg averageLegs(List<Leg> repeats) {
        int count = repeats.size();
        double dist = 0.0, inc = 0.0;
        double[] azms = new double[count];
        for (int i=0; i < count; i++) {
            Leg leg = repeats.get(i);
            dist += leg.getDistance();
            inc += leg.getInclination();
            azms[i] = leg.getBearing();
        }
        dist /= count;
        inc /= count;
        return new Leg(dist, averageAzms(azms), inc);
    }


    /** Given a foresight and backsight which may not exactly agree, produce an averaged foresight */
    public static Leg averageBacksights(Leg fore, Leg back) {
        return averageLegs(Arrays.asList(fore, back.asBacksight()));
    }


    /** Average some azimuth values together, even if they span the 360/0 boundary */
    private static double averageAzms(double[] azms) {
        // Azimuth values jump at the 360/0 boundary, so we must be careful to ensure that
        // values {359, 1} average to 0 rather than the incorrect value 180
        double sum = 0.0;
        double min = Leg.MAX_BEARING, max = Leg.MIN_BEARING;
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


    public void reverseLeg(final Station toReverse) {
        Log.d("reversing " + toReverse.getName());
        SurveyTools.traverse(
            survey,
            new SurveyTools.SurveyTraversalCallback() {
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
