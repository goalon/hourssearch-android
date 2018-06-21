package com.example.mateusz.thehourssearch;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String TAG = "MapsActivity";

    private static final String URL = "https://hourssearch.herokuapp.com/update/";

    private GoogleMap mMap;
    private FloatingActionButton searchButton;
    private FloatingActionButton downloadButton;
    private Map<LatLng, JSONObject> mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(view -> search());

        downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(view -> download());

        mInfo = new HashMap<>(10);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        SearchDialogFragment.setmLinkMap(mMap);
        SearchDialogFragment.setmLinkInfo(mInfo);

        mMap.setOnInfoWindowClickListener(marker -> {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Info");
            alertDialog.setMessage(extractInfo(mInfo.get(marker.getPosition())));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    (dialog, which) -> dialog.dismiss());
            alertDialog.show();
        });

        Geocoder geocoder = new Geocoder(this);
        Address address;

        try {
            address = geocoder.getFromLocationName("Warsaw, Poland", 1).get(0);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

        LatLng warsaw = new LatLng(address.getLatitude(), address.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.zoomTo(8));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(warsaw));
        SearchDialogFragment.setmWarsaw(warsaw);
    }

    private void search() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment newFragment = SearchDialogFragment.getInstance(fm);

        if (newFragment.isAdded()) {
            return;
        }

        newFragment.show(fm, SearchDialogFragment.TAG);
    }

    private void download() {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, URL, response -> {
            Toast.makeText(this, response, Toast.LENGTH_LONG).show();
        }, error -> {
            Log.e(TAG, error.getMessage());
        }
        );
        stringRequest.setTag(TAG);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private String extractInfo(JSONObject info) {
        try {
            return "Tytuł: " + info.getString("title") + "\n\n" +
                    "Typ: " + info.getString("level") + "\n\n" +
                    "Rodzaj: " + info.getString("kind") + "\n\n" +
                    "Nazwa: " + info.getString("name") + "\n\n" +
                    "Miejscowość: " + info.getString("locality") + "\n\n" +
                    "Ulica: " + info.getString("street") + "\n\n" +
                    "Nr: " + info.getString("number") + "\n\n" +
                    "Kod pocztowy: " + info.getString("postal_code") + "\n\n" +
                    "Email: " + info.getString("email") + "\n\n" +
                    "Nr telefonu: " + info.getString("phone_number") + "\n\n" +
                    "Przedmiot: " + info.getString("subject") + "\n\n" +
                    "Liczba godzin w tygodniu: " + Integer.valueOf(info.getInt("hours")) + "\n\n" +
                    "Wymiar zatrudnienia: " + info.getString("means") + "\n\n" +
                    "Opis: " + info.getString("description");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
