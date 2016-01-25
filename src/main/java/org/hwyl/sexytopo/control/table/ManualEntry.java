package org.hwyl.sexytopo.control.table;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.TableActivity;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/**
 * Handles manual data entry in the TableActivity. Revolves around creating and reacting to dialogs.
 */
public class ManualEntry {

    private ManualEntry() {}


    public static void addStation(final TableActivity tableActivity, final Survey survey) {



        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
                tableActivity,
                new EditCallback() {
                    @Override
                    public void submit(Leg leg, Dialog dialog) {
                        SurveyUpdater updater = new SurveyUpdater(survey, tableActivity);
                        updater.updateWithNewStation(leg);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle(R.string.manual_add_station_title);
    }



    public static void addSplay(final TableActivity tableActivity, final Survey survey) {
        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
                tableActivity,
                new EditCallback() {
                    @Override
                    public void submit(Leg leg, Dialog dialog) {
                        SurveyUpdater updater = new SurveyUpdater(survey, tableActivity);
                        updater.update(leg);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle(R.string.manual_add_splay_title);
    }


    public static void editLeg(final TableActivity tableActivity,
                               final Survey survey,
                               final Leg toEdit) {

        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
                tableActivity,
                new EditCallback() {
                    public void submit(Leg edited, Dialog dialog) {
                        if (toEdit.hasDestination()) {
                            edited = new Leg(
                                    edited.getDistance(),
                                    edited.getBearing(),
                                    edited.getInclination(),
                                    toEdit.getDestination());
                        }
                        SurveyUpdater updater = new SurveyUpdater(survey, tableActivity);
                        updater.editLeg(toEdit, edited);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle("Edit Leg");

        ((TextView) (dialog.findViewById(R.id.editDistance)))
                .setText("" + toEdit.getDistance());
        ((TextView) (dialog.findViewById(R.id.editAzimuth)))
                .setText("" + toEdit.getBearing());
        ((TextView) (dialog.findViewById(R.id.editInclination)))
                .setText("" + toEdit.getInclination());
    }


    public static void addStationWithLruds(final TableActivity tableActivity, final Survey survey) {

        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog_with_lruds,
                tableActivity,
                new EditCallback() {
                    @Override
                    public void submit(Leg leg, Dialog dialog) {
                        Station station = survey.getActiveStation();
                        SurveyUpdater updater = new SurveyUpdater(survey, tableActivity);
                        updater.updateWithNewStation(leg);
                        Station newActiveStation = survey.getActiveStation();
                        survey.setActiveStation(station);
                        createLrudIfPresent(survey, station, dialog, R.id.editDistanceLeft, LRUD.LEFT);
                        createLrudIfPresent(survey, station, dialog, R.id.editDistanceRight, LRUD.RIGHT);
                        createLrudIfPresent(survey, station, dialog, R.id.editDistanceUp, LRUD.UP);
                        createLrudIfPresent(survey, station, dialog, R.id.editDistanceDown, LRUD.DOWN);
                        survey.setActiveStation(newActiveStation);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle(R.string.manual_add_station_title);
    }


    private static void createLrudIfPresent(Survey survey, Station station,
                                            Dialog dialog, int fieldId,
                                            LRUD direction) {
        Double value = getFieldValue(dialog, fieldId);
        if (value != null) {
            Leg leg = direction.createSplay(survey, station, value);
            SurveyUpdater updater = new SurveyUpdater(survey, dialog.getContext());
            updater.update(leg);
        }

    }


    private static Double getFieldValue(Dialog dialog, int id) {
        try {
            TextView field = (TextView) (dialog.findViewById(id));
            return Double.parseDouble(field.getText().toString());
        } catch (Exception e) {
            return null;
        }
    }


    private static AlertDialog createDialog(int layoutId, final TableActivity tableActivity,
                            final EditCallback editCallback) {


        AlertDialog.Builder builder = new AlertDialog.Builder(tableActivity);

        LayoutInflater inflater = tableActivity.getLayoutInflater();
        final View dialogView = inflater.inflate(layoutId, null);
        builder
            .setView(dialogView)
            .setTitle(R.string.manual_add_station_title)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int buttonId) {
                    // do nothing
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int buttonId) {
                    // do nothing
                }
            });


        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int buttonId) {

                Double distance = getFieldValue(dialog, R.id.editDistance);
                Double azimuth = getFieldValue(dialog, R.id.editAzimuth);
                Double inclination = getFieldValue(dialog, R.id.editInclination);

                if (distance == null || !Leg.isDistanceLegal(distance)) {
                    TextView editDistance = ((TextView) dialogView.findViewById(R.id.editDistance));
                    editDistance.setError("Bad distance");
                    tableActivity.showSimpleToast("Bad distance");
                } else if (azimuth == null || !Leg.isAzimuthLegal(azimuth)) {
                    TextView editAzimuth = ((TextView)(dialogView.findViewById(R.id.editAzimuth)));
                    tableActivity.showSimpleToast("Bad azimuth");
                    editAzimuth.setError("Bad azimuth");
                } else if (inclination == null || !Leg.isInclinationLegal(inclination)) {
                    TextView editInclination = ((TextView)(dialogView.findViewById(R.id.editInclination)));
                    editInclination.setError("Bad inclination");
                    tableActivity.showSimpleToast("Bad inclination " + inclination);
                } else {
                    dialogInterface.dismiss();
                    Leg leg = new Leg(distance, azimuth, inclination);
                    editCallback.submit(leg, dialog);
                }
            }

        });

        return dialog;

    }

    public static void renameStation(final TableActivity activity,
                                     final Survey survey, final Station toRename) {

        final EditText renameField = new EditText(activity);
        renameField.setText(toRename.getName());


        renameField.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO - nothing
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO - nothing
            }

            public void afterTextChanged(Editable s) {

                String currentName = toRename.getName();
                String currentText = renameField.getText().toString();

                // only check for non-null or max length
                if (currentText.isEmpty()) {
                    renameField.setError("Cannot be blank");
                } else if (!currentText.equals(currentName) && (survey.getStationByName(currentText) != null)) {
                    renameField.setError("Station name must be unique");
                } else {
                    renameField.setError(null);
                }
            }
        });



        new AlertDialog.Builder(activity)
                .setTitle("Edit name")
                .setView(renameField)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        String newName = renameField.getText().toString();
                        try {
                            SurveyUpdater updater = new SurveyUpdater(survey, activity);
                            updater.renameStation(toRename, newName);
                            activity.syncTableWithSurvey();
                        } catch (Exception e) {
                            activity.showSimpleToast("Rename failed");
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        // do nothing
                    }
                })
                .show();
    }

    public interface EditCallback {
        void submit(Leg leg, Dialog dialog);
    }

}
