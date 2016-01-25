package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.CompassExporter;
import org.hwyl.sexytopo.control.io.Loader;
import org.hwyl.sexytopo.control.io.Saver;
import org.hwyl.sexytopo.control.io.SVGSketchExporter;
import org.hwyl.sexytopo.control.io.TherionExporter;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.test.TestSurveyCreator;

import java.io.File;
import java.io.IOException;

/**
 * Base class for all activities that use the action bar.
 */
public abstract class SexyTopoActivity extends ActionBarActivity {

    protected SurveyManager dataManager;

    public SexyTopoActivity() {
        super();
        dataManager = SurveyManager.getInstance(this);

    }


    @Override
    protected void onStart() {
        super.onStart();
        setOrientation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);


        MenuItem menuItem = menu.findItem(R.id.action_back_measurements);
        SharedPreferences preferences = getSharedPreferences(SexyTopo.GENERAL_PREFS, Context.MODE_PRIVATE);
        boolean isSelected =
                preferences.getBoolean(SexyTopo.REVERSE_MEASUREMENTS_PREFERENCE, false);
        menuItem.setChecked(isSelected);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_device:
                startActivity(DeviceActivity.class);
                return true;
            case R.id.action_table:
                startActivity(TableActivity.class);
                return true;
            case R.id.action_plan:
                startActivity(PlanActivity.class);
                return true;
            case R.id.action_elevation:
                startActivity(ExtendedElevationActivity.class);
                return true;
            case R.id.action_survey:
                startActivity(SurveyActivity.class);
                return true;
            case R.id.action_calibration:
                startActivity(CalibrationActivity.class);
                return true;
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                return true;
            case R.id.action_help:
                startActivity(GuideActivity.class);
                return true;
            case R.id.action_about:
                openAboutDialog();
                return true;

            case R.id.action_file_new:
                startNewSurvey();
                return true;
            case R.id.action_file_open:
                openSurvey();
                return true;
            case R.id.action_file_delete:
                deleteSurvey();
                return true;
            case R.id.action_file_save:
                saveSurvey();
                return true;
            case R.id.action_file_save_as:
                saveSurveyAsName();
                return true;
            case R.id.action_file_export:
                exportSurvey();
                return true;


            case R.id.action_undo_last_leg:
                undoLastLeg();
                return true;
            case R.id.action_back_measurements:
                setReverseMeasurementsPreference(item);
                return true;
            case R.id.action_generate_test_survey:
                generateTestSurvey();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void openAboutDialog() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        //TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        //int defaultColor = textView.getTextColors().getDefaultColor();
        //textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.drawable.laser_icon_small)
                .setTitle(getText(R.string.app_name) + " v" + SexyTopo.VERSION)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setView(messageView);
        builder.create().show();

    }

    private void exportSurvey() {
        try {
            Survey survey = getSurvey();

            // Therion text file
            String content = TherionExporter.export(survey);
            String filename = Util.getPathForSurveyFile(survey.getName(), "txt");
            Saver.saveFile(filename, content);

            // Compass dat file
            CompassExporter exporter = new CompassExporter();
            String datFilename = Util.getPathForSurveyFile(survey.getName(), "dat");
            String datContents = exporter.export(survey);
            Saver.saveFile(datFilename, datContents);

            // SVG sketch
            SVGSketchExporter svgExporter = new SVGSketchExporter();
            String svgFilename = Util.getPathForSurveyFile(survey.getName(), "svg");
            svgExporter.export(survey, svgFilename);

        } catch(IOException e) {
            Log.d(SexyTopo.TAG, "Error exporting survey: " + e);
            showSimpleToast("Error exporting survey");
        }
    }


    private void startActivity(Class clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void saveSurvey() {
        try {
            Saver.save(getSurvey());
        } catch (Exception e) {
            Log.d(SexyTopo.TAG, "Error saving survey: " + e);
            showSimpleToast("Error saving survey");
        }
    }


    private void saveSurveyAsName() {

        final EditText input = new EditText(this);
        input.setContentDescription("Enter new name");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_save_as_title))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String newName = value.toString();
                        Survey survey = getSurvey();
                        String oldName = survey.getName();
                        try {
                            survey.setName(newName);
                            Saver.save(survey);
                        } catch (Exception e) {
                            survey.setName(oldName);
                            showSimpleToast("Error saving survey with new name");
                            Log.d(SexyTopo.TAG, "Error saving survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    private void startNewSurvey() {

        warnIfNotSaved();

        final EditText input = new EditText(this);
        input.setText("NewSurvey");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_survey_title))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String name = value.toString();
                        Survey survey = new Survey(name);
                        setSurvey(survey);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    private void deleteSurvey() {

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_survey_title))
                .setMessage(getString(R.string.dialog_delete_survey_content))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            String surveyName = getSurvey().getName();
                            Util.deleteSurvey(surveyName);
                        } catch (Exception e) {
                            showSimpleToast("Error deleting survey");
                            Log.d(SexyTopo.TAG, "Error deleting survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    private void openSurvey() {

        warnIfNotSaved();

        File[] surveyDirectories = Util.getSurveyDirectories();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                 this);
        //builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Open Survey");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.select_dialog_item);

        for (File file : surveyDirectories) {
            arrayAdapter.add(file.getName());
        }

        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String surveyName = arrayAdapter.getItem(which);
                        try {
                            Survey survey = Loader.loadSurvey( surveyName);
                            SurveyManager.getInstance(SexyTopoActivity.this).setCurrentSurvey(survey);
                            showSimpleToast(getString(R.string.loaded) + " " + surveyName);
                        } catch (Exception e) {
                            showSimpleToast(getString(R.string.error_prefix) + e.getMessage());
                        }
                    }
                });
        builderSingle.show();
    }


    private void warnIfNotSaved() {
        if (!getSurvey().isSaved()) {
            showSimpleToast(getString(R.string.warning_survey_not_saved));
        }
    }


    private void undoLastLeg() {
        getSurvey().undoLeg();
        SurveyManager.getInstance(this).broadcastSurveyUpdated();

    }

    private void setReverseMeasurementsPreference(MenuItem item) {
        item.setChecked(!item.isChecked());
        SharedPreferences preferences = getSharedPreferences(SexyTopo.GENERAL_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SexyTopo.REVERSE_MEASUREMENTS_PREFERENCE, item.isChecked());
        editor.commit();
    }

    private void generateTestSurvey() {
        new AlertDialog.Builder(this)
                .setTitle("Generate Test Data")
                .setMessage("Replace the existing survey with randomly-generated data?")
                .setCancelable(false)
                .setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Survey currentSurvey = TestSurveyCreator.create(10, 5);
                        SurveyManager.getInstance(SexyTopoActivity.this).setCurrentSurvey(currentSurvey);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

    }


    protected Survey getSurvey() {
        return SurveyManager.getInstance(this).getCurrentSurvey();
    }


    protected void setSurvey(Survey survey) {
        SurveyManager.getInstance(this).setCurrentSurvey(survey);
    }


    public void showSimpleToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected boolean getBooleanPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(name, false);
    }

    protected String getStringPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(name, "");
    }

    private void setOrientation() {
        String orientationPreference = getStringPreference("pref_orientation");

        if (orientationPreference.equals("Force Portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientationPreference.equals("Force Landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

}
