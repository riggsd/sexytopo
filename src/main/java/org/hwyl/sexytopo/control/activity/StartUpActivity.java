package org.hwyl.sexytopo.control.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.Loader;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.util.MagneticDeclination;
import org.hwyl.sexytopo.model.survey.Survey;

public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);




        /*
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                Toast.makeText(getApplicationContext(), "Paired: " + device.getName(), Toast.LENGTH_SHORT).show();
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if (name == null) {
                    name = device.getName();
                } else {
                    //throw new IllegalStateException("More than one device paired");
                }
            }
        }

        return name;*/


        Survey survey = isThereAnActiveSurvey()? loadActiveSurvey() : createNewActiveSurvey();
        SurveyManager.getInstance(this).setCurrentSurvey(survey);

        Log.setContext(this);

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }

    private boolean isThereAnActiveSurvey() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        return preferences.contains(SexyTopo.ACTIVE_SURVEY_NAME);
    }


    public Survey loadActiveSurvey() {

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        String activeSurveyName = preferences.getString(SexyTopo.ACTIVE_SURVEY_NAME, null);

        Toast.makeText(getApplicationContext(),
                getString(R.string.loading_survey) + " " + activeSurveyName,
                Toast.LENGTH_SHORT).show();

        Survey survey;
        try {
            survey = Loader.loadSurvey(activeSurveyName);
        } catch (Exception e) {
            survey = createNewActiveSurvey();
            Toast.makeText(getApplicationContext(),
                    getString(R.string.loading_survey_error),
                    Toast.LENGTH_SHORT).show();
        }

        return survey;
    }


    private Survey createNewActiveSurvey() {

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

        String defaultNameBase = getString(R.string.default_survey_name);
        String defaultName = Util.getNextDefaultSurveyName(defaultNameBase);
        double declination = MagneticDeclination.getDeclination(this);

        Survey survey = new Survey(defaultName, declination);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SexyTopo.ACTIVE_SURVEY_NAME, defaultName);
        editor.commit();

        return survey;
    }


}
