package com.example.mateusz.thehourssearch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

public class SearchPickerDialogFragment extends DialogFragment {
    public static String TAG = "SearchPickerDialogFragment";

    private NumberPicker mSearchPicker;
    private EditText mSearchText;

    int minValue;
    int maxValue;
    int defValue;
    boolean hours;

    public void set(EditText mSearchText, int minValue, int maxValue, int defValue, boolean hours) {
        this.mSearchText = mSearchText;

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defValue = defValue;
        this.hours = hours;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.search_picker_dialog_fragment, null);
        String searchText = mSearchText.getText().toString();

        mSearchPicker = rootView.findViewById(R.id.search_picker);
        mSearchPicker.setMinValue(minValue);
        mSearchPicker.setMaxValue(maxValue);
        mSearchPicker.setValue(searchText.isEmpty() ? defValue : Integer.parseInt(searchText));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

        builder.setView(rootView.getRootView())
                .setTitle((hours ? R.string.hours_title : R.string.limit_title))
                .setPositiveButton(R.string.set, (dialog, id) ->
                        mSearchText.setText(Integer.toString(mSearchPicker.getValue())))
                .setNegativeButton(R.string.cancel, (dialog, id) ->
                        SearchPickerDialogFragment.this.getDialog().cancel());

        return builder.create();
    }
}
