package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by driggs on 1/22/16.
 */
public class RealSurvexImporterExporter {


    private static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();


    public static String export(Survey survey) {

        List<Map<TableCol, Object>> data = graphToListTranslator.toListOfColMaps(survey);
        StringBuilder sb = new StringBuilder(1024);

        sb.append(String.format("*begin %s\n", survey.getName()));  // FIXME remove name from *begin and *end?!
        sb.append(String.format("*declination %.3f\n\n", survey.getDeclination()));  // FIXME add units "degrees"

        for (Map map : data) {
            sb.append('\t');
            sb.append(getEntry(TableCol.FROM, map)).append('\t');
            sb.append(getEntry(TableCol.TO, map)).append('\t');
            sb.append(getEntry(TableCol.DISTANCE, map)).append('\t');
            sb.append(getEntry(TableCol.BEARING, map)).append('\t');
            sb.append(getEntry(TableCol.INCLINATION, map)).append('\n');
        }

        sb.append(String.format("*end %s\n", survey.getName()));
        return sb.toString();
    }


    private static String getEntry(TableCol tableCol, Map map) {
        Object value = map.get(tableCol);
        return tableCol.format(value);
    }



    public static Survey parse(String text, String name) {

        Map<String, Station> nameToStation = new HashMap<>();
        String[] lines = text.split("\n");

        Survey survey = null;
        double declination = 0.0;

        Station origin = survey.getOrigin();
        nameToStation.put(origin.getName(), origin);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("*begin")) {
                String fileName = line.split("\\s+", 1)[0].trim();
                if (!fileName.isEmpty()) {
                    name = fileName;
                }

            } else if (line.startsWith("*declination")) {
                declination = Double.parseDouble(line.split("\\s+")[1]);

            } else if (line.startsWith("*end")) {
                String fileName = line.split("\\s+", 1)[0];
                if (!fileName.isEmpty() && !fileName.equals(name)) {
                    throw new IllegalArgumentException("Nested *begin/*end blocks not supported.");
                }

            } else if (line.startsWith("*")) {
                org.hwyl.sexytopo.control.Log.d("Ignoring unsupported Survex command:  " + line.trim());

            } else {

                survey = new Survey(name, declination);
                String[] fields = line.split("\\s+");
                if (fields.length != 5) {
                    throw new IllegalArgumentException("Survex measurements must be in 'data normal' form:  " + line);
                }
                addLegToSurvey(survey, nameToStation, fields);
            }
        }

        return survey;
    }

    private static void addLegToSurvey(Survey survey, Map<String, Station> nameToStation, String[] fields) {

        Station from = retrieveOrCreateStation(nameToStation, fields[0]);
        Station to = retrieveOrCreateStation(nameToStation, fields[1]);
        double distance = Double.parseDouble(fields[2]);
        double bearing = Double.parseDouble(fields[3]);
        double inclination = Double.parseDouble(fields[4]);

        Leg leg = new Leg(distance, bearing, inclination, to);
        from.addOnwardLeg(leg);

        // FIXME: bit of a hack; hopefully the last station processed will be the active one
        // (should probably record the active station in the file somewhere)
        survey.setActiveStation(from);
    }

    private static Station retrieveOrCreateStation(Map<String, Station> nameToStation, String name) {
        if (name.equals(SexyTopo.BLANK_STATION_NAME)) {
            return Survey.NULL_STATION;
        } else if (nameToStation.containsKey(name)) {
            return nameToStation.get(name);
        } else {
            Station station = new Station(name);
            nameToStation.put(name, station);
            return station;
        }
    }

}
