package com.example.mateusz.thehourssearch;

import android.app.Dialog;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SearchDialogFragment extends DialogFragment {
    public final static String TAG = "SearchDialogFragment";

    private final static String URL = "https://hourssearch.herokuapp.com/query/";

    private Spinner mLevelSpinner;
    private Spinner mKindSpinner;
    private Spinner mSubjectSpinner;
    private EditText mHoursText;
    private Spinner mMeansSpinner;
    private EditText mLimitText;
    private Button mQueryButton;

    private static GoogleMap mLinkMap;
    private static Map<LatLng, JSONObject> mLinkInfo;
    private static LatLng mWarsaw;
    private final static float[] colors = {
            BitmapDescriptorFactory.HUE_AZURE,
            BitmapDescriptorFactory.HUE_BLUE,
            BitmapDescriptorFactory.HUE_CYAN,
            BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_MAGENTA,
            BitmapDescriptorFactory.HUE_ORANGE,
            BitmapDescriptorFactory.HUE_RED,
            BitmapDescriptorFactory.HUE_ROSE,
            BitmapDescriptorFactory.HUE_VIOLET,
            BitmapDescriptorFactory.HUE_YELLOW
    };

    public static SearchDialogFragment getInstance(FragmentManager fm) {
        SearchDialogFragment fragment =
                (SearchDialogFragment) fm.findFragmentByTag(SearchDialogFragment.TAG);

        if (fragment == null) {
            fragment = new SearchDialogFragment();

            fragment.setRetainInstance(true);
            Log.d(TAG, "New instance was created.");
        }

        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        dialog.getWindow()
                .setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().getAttributes().dimAmount = 0.6f;
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setCanceledOnTouchOutside(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        View rootView = inflater.inflate(R.layout.search_dialog_fragment, container, false);

        mLevelSpinner = rootView.findViewById(R.id.level_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
                (getContext(), R.array.levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLevelSpinner.setAdapter(adapter);

        mKindSpinner = rootView.findViewById(R.id.kind_spinner);
        adapter = ArrayAdapter.createFromResource
                (getContext(), R.array.kinds, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKindSpinner.setAdapter(adapter);

        mSubjectSpinner = rootView.findViewById(R.id.subject_spinner);
        adapter = ArrayAdapter.createFromResource
                (getContext(), R.array.subjects, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSubjectSpinner.setAdapter(adapter);

        mMeansSpinner = rootView.findViewById(R.id.means_spinner);
        adapter = ArrayAdapter.createFromResource
                (getContext(), R.array.means, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMeansSpinner.setAdapter(adapter);

        mHoursText = rootView.findViewById(R.id.hours_picker);
        mHoursText.setOnClickListener(view -> {
            SearchPickerDialogFragment newFragment = new SearchPickerDialogFragment();
            newFragment.set(mHoursText, 1, 40, 10, true);
            newFragment.show(getFragmentManager(), SearchPickerDialogFragment.TAG);
        });
        mHoursText.setInputType(InputType.TYPE_NULL);

        mLimitText = rootView.findViewById(R.id.limit_picker);
        mLimitText.setOnClickListener(view -> {
            SearchPickerDialogFragment newFragment = new SearchPickerDialogFragment();
            newFragment.set(mLimitText, 1, 10, 5, false);
            newFragment.show(getFragmentManager(), SearchPickerDialogFragment.TAG);
        });
        mLimitText.setInputType(InputType.TYPE_NULL);

        mQueryButton = rootView.findViewById(R.id.query_button);
        mQueryButton.setOnClickListener(view -> query());

        return rootView;
    }

    private void query() {
        JSONObject jsonObject = boxesExtract();

        Log.d(TAG, jsonObject.toString());

        JsonMixedRequest jsonMixedRequest = new JsonMixedRequest(
                Request.Method.POST, URL + Integer.valueOf(mLimitText.getText().toString()),
                jsonObject, response -> {
                    try {
                        dismiss();
                        JSONArray jsonArray = new JSONArray(response.toString());
                        Log.d(TAG, jsonArray.toString());
                        setMarkers(jsonArray);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
        }, error -> {
                    Log.e(TAG, error.getMessage());
        });
        jsonMixedRequest.setTag(TAG);
        jsonMixedRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonMixedRequest);
    }

    private JSONObject boxesExtract() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("level", mLevelSpinner.getSelectedItem().toString());
            jsonObject.put("kind", mKindSpinner.getSelectedItem().toString());

            Collection<String> subjects = new ArrayList<>(1);
            String subject = mSubjectSpinner.getSelectedItem().toString();
            if (!subject.isEmpty()) {
                subjects.add(subject);
            }

            jsonObject.put("subjects", new JSONArray(subjects));
            jsonObject.put("hours", Integer.valueOf(mHoursText.getText().toString()));
            jsonObject.put("means", mMeansSpinner.getSelectedItem().toString());
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        return jsonObject;
    }

    public static void setmLinkMap(GoogleMap mMap) {
        mLinkMap = mMap;
    }

    public static void setmLinkInfo(Map<LatLng, JSONObject> mInfo) {
        mLinkInfo = mInfo;
    }

    public static void setmWarsaw(LatLng warsaw) {
        mWarsaw = warsaw;
    }

    private void setMarkers(JSONArray places) {
        mLinkMap.moveCamera(CameraUpdateFactory.zoomTo(8));
        mLinkMap.moveCamera(CameraUpdateFactory.newLatLng(mWarsaw));
        mLinkMap.clear();
        mLinkInfo.clear();

        Geocoder geocoder = new Geocoder(getContext());
        MarkerOptions markerOptions = new MarkerOptions();

        for (int i = 0; i < places.length(); ++i) {
            try {
                JSONObject place = places.getJSONObject(i);
                Address address = geocoder.getFromLocationName(getLocation(place), 1).get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(colors[i]));
                markerOptions.title(place.getString("name"));
                mLinkMap.addMarker(markerOptions);
                mLinkInfo.put(latLng, place);
            } catch (JSONException | IOException e) {
                Log.e(TAG, e.getMessage());
                return;
            }
        }
    }

    private String getLocation(JSONObject place) throws JSONException {
        return place.getString("street") + ' ' + place.getString("number") + ", " +
                place.getString("postal_code") + ' ' + place.getString("locality");
    }
}
