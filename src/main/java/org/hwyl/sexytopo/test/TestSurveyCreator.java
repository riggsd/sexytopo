package org.hwyl.sexytopo.test;

import android.content.Context;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;
import java.util.Random;

/**
 * Created by rls on 29/07/14.
 */
public class TestSurveyCreator {

    private static Random random = new Random();


    public static Survey create(int numStations, int numBranches, Context context) {

        Survey survey = new Survey("NewSurvey");

        createBranch(survey, numStations, context);

        for (int i = 0; i < numBranches; i++) {
            List<Station> stations = survey.getAllStations();
            Station active = getRandom(stations);
            survey.setActiveStation(active);
            createBranch(survey, 3, context);
        }

        return survey;

    }



    public static void createBranch(Survey survey, int numStations, Context context) {



        for (int i = 0; i < numStations; i++) {


            double distance = 5 + random.nextInt(10);
            double bearing = 40 + random.nextInt(100);
            double inclination = -20 + random.nextInt(40);

            SurveyUpdater updater = new SurveyUpdater(survey, context);
            Leg leg0 = new Leg(distance, bearing, inclination);
            updater.update(leg0);
            Leg leg1 = new Leg(distance, bearing, inclination);
            updater.update(leg1);
            Leg leg2 = new Leg(distance, bearing, inclination);
            updater.update(leg2);
        }

    }

    public static <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
