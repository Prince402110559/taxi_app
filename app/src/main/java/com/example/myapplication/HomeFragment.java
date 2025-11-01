package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView; // ✅ Correct import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.res.Configuration;
import com.google.android.gms.maps.model.MapStyleOptions;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private ImageView menuButton, profileImage;
    private SearchView searchBar, dropSearchView;
    public ActivityMainBinding binding;
    private static final int REQ_LOCATION = 42;
    private FusedLocationProviderClient fusedClient;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.mapView);
        menuButton = view.findViewById(R.id.menuButton);
        profileImage = view.findViewById(R.id.profileImage);
        searchBar = view.findViewById(R.id.searchBar);
        dropSearchView = view.findViewById(R.id.dropSearchView);

        searchBar.setQueryHint("Choose Start Location");
        dropSearchView.setQueryHint("Choose Destination");
        searchBar.setIconified(false);
        dropSearchView.setIconified(false);

        /*
        searchBar.setOnClickListener(v -> {
             // Expands search view
            searchBar.requestFocus();
        });
        dropSearchView.setOnClickListener(v -> {
             // Expands search view
            dropSearchView.requestFocus();
        });*/



        // Initialize the map
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }







        // Menu button click
        menuButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Menu clicked", Toast.LENGTH_SHORT).show();
        });

        // Profile button click
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        profileImage.setOnClickListener(v ->{
            Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show();
            bottomNav.setSelectedItemId(R.id.profile);

        });


        searchBar.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                getUserCurrentAddress(address -> searchBar.setQuery(address, false));
            }
        });

        //this is where we handle the users input
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String start) {
                String drop = dropSearchView.getQuery().toString();
                if (!start.isEmpty() && !drop.isEmpty()) {
                    findAndShowRidesToDestination( drop);
                }else {
                    Toast.makeText(getContext(), "Please enter both start and destination", Toast.LENGTH_SHORT).show();

                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });



        // we also use this one to handle user input
        //we have  2 incase the user presses submit while on either of the searchviews
        dropSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String drop) {
                String start = searchBar.getQuery().toString();
                if (!start.isEmpty() && !drop.isEmpty()) {
                    findAndShowRidesToDestination( drop);
                } else {
                    Toast.makeText(getContext(), "Please enter both start and destination", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        try {
            MapsInitializer.initialize(requireContext());
            googleMap = map;


            // Apply light/dark map style
            applyMapStyle();

            // South Africa bounds
            LatLngBounds SOUTH_AFRICA = new LatLngBounds(
                    new LatLng(-35.0, 16.0),
                    new LatLng(-22.0, 33.0)
            );
            googleMap.setLatLngBoundsForCameraTarget(SOUTH_AFRICA);
            googleMap.setMinZoomPreference(4.5f);
            googleMap.setMaxZoomPreference(20.0f);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-29.0, 24.0), 5.0f));

            // UI toggles
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setRotateGesturesEnabled(false);
            googleMap.getUiSettings().setTiltGesturesEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            // Location
            if (hasLocationPermission()) {
                enableMyLocation();
                moveCameraToMyLocation();
            } else {
                requestLocationPermission();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            moveCameraToMyLocation(); // optional
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            moveCameraToMyLocation();
        } else {
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }
    //mthod to move users view o their location
    private void moveCameraToMyLocation() {
        if (!hasLocationPermission()) return;
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && googleMap != null) {
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
            }
        });
    }
    //method tonble ad find the users lcation on he mp
    private void enableMyLocation() {
        if (googleMap == null) return;
        try {
            googleMap.setMyLocationEnabled(true); // shows blue dot and my-location button
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException ignored) {}
    }

    //method to set dark mode map
    private void applyMapStyle() {
        if (googleMap == null) return;
        int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int styleRes = (night == Configuration.UI_MODE_NIGHT_YES)
                ? R.raw.map_style_dark
                : R.raw.map_style_light;
        try {
            boolean ok = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), styleRes));
            if (!ok) {
                // style parsed but not applied (rare)
            }
        } catch (Resources.NotFoundException e) {
            // fall back silently
        }
    }

    private void getUserCurrentAddress(Consumer<String> callback) {
        if (!hasLocationPermission()) {
            callback.accept("");
            return;
        }
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                Geocoder geocoder = new Geocoder(requireContext());
                try {
                    List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        callback.accept(addresses.get(0).getAddressLine(0));
                        return;
                    }
                } catch (IOException ignored) { }
            }
            callback.accept("");
        });
    }



    private void findAndShowRidesToDestination(String endRankName) {
        String normalized = endRankName.trim().toLowerCase();
        Toast.makeText(getContext(), "Searching destination: " + normalized, Toast.LENGTH_SHORT).show();

        db.collection("rank")
                .whereEqualTo("name_lowercase", normalized)
                .get()
                .addOnSuccessListener(endQuery -> {
                    if (endQuery.isEmpty()) {
                        Toast.makeText(getContext(), "Destination rank not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot endDoc = endQuery.getDocuments().get(0);
                    String endRankId = endDoc.getId();
                    String destinationRankName = endDoc.getString("name");
                    LatLng endLatLng = extractLatLngFromRank(endDoc);
                    if (endLatLng == null) {
                        Toast.makeText(getContext(), "Destination coordinates missing", Toast.LENGTH_LONG).show();
                        return;
                    }

                    db.collection("route")
                            .whereEqualTo("endRankId", endRankId)
                            .get()
                            .addOnSuccessListener(routeQuery -> {
                                if (routeQuery.isEmpty()) {
                                    Toast.makeText(getContext(), "No rides available to this destination", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                List<String> startRankIds = new ArrayList<>();
                                List<Double> pricesRaw = new ArrayList<>();
                                for (DocumentSnapshot routeDoc : routeQuery) {
                                    String startId = routeDoc.getString("startRankId");
                                    Double price = routeDoc.getDouble("price");
                                    if (startId != null && price != null) {
                                        startRankIds.add(startId);
                                        pricesRaw.add(price);
                                    }
                                }
                                if (startRankIds.isEmpty()) {
                                    Toast.makeText(getContext(), "No valid start ranks found", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                db.collection("rank")
                                        .whereIn(FieldPath.documentId(), startRankIds)
                                        .get()
                                        .addOnSuccessListener(startRanksQuery -> {
                                            List<String> startNames = new ArrayList<>();
                                            List<String> priceLabels = new ArrayList<>();
                                            List<LatLng> startLatLngs = new ArrayList<>();

                                            // Align outputs with startRankIds/pricesRaw
                                            for (DocumentSnapshot doc : startRanksQuery) {
                                                String startId = doc.getId();
                                                int idx = startRankIds.indexOf(startId);
                                                if (idx < 0) continue;

                                                String rankName = doc.getString("name");
                                                Double price = pricesRaw.get(idx);
                                                LatLng sLL = extractLatLngFromRank(doc);
                                                if (sLL == null) continue;

                                                startNames.add(rankName != null ? rankName : "(unknown)");
                                                priceLabels.add(String.format("Price: R%.2f", price));
                                                startLatLngs.add(sLL);
                                            }

                                            if (startNames.isEmpty()) {
                                                Toast.makeText(getContext(), "No start ranks with coordinates found", Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            showBottomSheetWithPrices(
                                                    startNames,
                                                    destinationRankName != null ? destinationRankName : endRankName,
                                                    priceLabels,
                                                    startLatLngs,
                                                    endLatLng,
                                                    pricesRaw
                                            );
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(getContext(), "Failed loading start ranks: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed loading routes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed loading destination ranks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }



    private void showBottomSheetWithPrices(
            List<String> startNames,
            String destinationName,
            List<String> priceLabels,
            List<LatLng> startLatLngs,
            LatLng endLatLng,
            List<Double> pricesRaw
    ) {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ride_options, null);
        ListView listView = sheetView.findViewById(R.id.listView);

        List<String> displayList = new ArrayList<>();
        int size = Math.min(startNames.size(), Math.min(priceLabels.size(), startLatLngs.size()));
        for (int i = 0; i < size; i++) {
            displayList.add(startNames.get(i) + " → " + destinationName + " \n" + priceLabels.get(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(sheetView);
        dialog.show();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            LatLng startLL = startLatLngs.get(position);
            String startName = startNames.get(position);
            Double price = pricesRaw.get(position);
            requestOsrmAndRender(startLL, endLatLng, startName, destinationName, price);
        });
    }

    @Nullable
    private LatLng extractLatLngFromRank(DocumentSnapshot doc) {
        Object raw = doc.get("location");
        if (raw instanceof com.google.firebase.firestore.GeoPoint) {
            com.google.firebase.firestore.GeoPoint gp = (com.google.firebase.firestore.GeoPoint) raw;
            return new LatLng(gp.getLatitude(), gp.getLongitude());
        } else if (raw instanceof String) {
            String locStr = (String) raw; // like "26.1057° S, 28.1016° E"
            try {
                String[] parts = locStr.split(",");
                double lat = parseDeg(parts[0]);
                double lng = parseDeg(parts[1]);
                return new LatLng(lat, lng);
            } catch (Exception ignored) { }
        }
        // Optional alternates if you migrate:
        com.google.firebase.firestore.GeoPoint gpAlt = doc.getGeoPoint("coords");
        if (gpAlt != null) return new LatLng(gpAlt.getLatitude(), gpAlt.getLongitude());
        Double latNum = doc.getDouble("latitude");
        Double lngNum = doc.getDouble("longitude");
        if (latNum != null && lngNum != null) return new LatLng(latNum, lngNum);
        return null;
    }




    private double parseDeg(String token) {
        token = token.trim();
        boolean south = token.endsWith("S") || token.endsWith("s");
        boolean west  = token.endsWith("W") || token.endsWith("w");
        String num = token.replace("°", "").replaceAll("[NnSsEeWw]", "").trim();
        double v = Double.parseDouble(num);
        if (south || west) v = -v;
        return v;
    }

    private void requestOsrmAndRender(LatLng start, LatLng end, String startName, String endName, Double price) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.longitude + "," + start.latitude + ";"
                + end.longitude + "," + end.latitude
                + "?overview=full&geometries=geojson&alternatives=false&steps=false";

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull java.io.IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to draw route: " + startName + " → " + endName, Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws java.io.IOException {
                Log.d("OSRM_URL", url);
                Log.d("OSRM_HTTP", "code=" + response.code() + " msg=" + response.message());
                String bodyStr = response.body() != null ? response.peekBody(Long.MAX_VALUE).string() : "null";
                Log.d("OSRM_BODY", bodyStr);

                if (!response.isSuccessful() || response.body() == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Routing service error", Toast.LENGTH_SHORT).show());
                    return;
                }
                String json = response.body().string();
                requireActivity().runOnUiThread(() -> connectOSRMRouteWithEta(json, start, end, startName, endName, price));
            }
        });
    }

    private void connectOSRMRouteWithEta(String json, LatLng start, LatLng end, String startName, String endName, Double price) {
        try {
            org.json.JSONObject data = new org.json.JSONObject(json);
            org.json.JSONArray routes = data.getJSONArray("routes");
            if (routes.length() == 0) {
                Toast.makeText(getContext(), "No route found", Toast.LENGTH_SHORT).show();
                return;
            }

            org.json.JSONObject route = routes.getJSONObject(0);
            double durationSec = route.getDouble("duration");
            double distanceM   = route.getDouble("distance");

            String etaText  = formatDuration(durationSec);
            String distText = formatDistance(distanceM);

            org.json.JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");
            java.util.List<LatLng> points = new java.util.ArrayList<>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (int i = 0; i < coordinates.length(); i++) {
                org.json.JSONArray c = coordinates.getJSONArray(i);
                double lng = c.getDouble(0);
                double lat = c.getDouble(1);
                LatLng p = new LatLng(lat, lng);
                points.add(p);
                builder.include(p);
            }

            // Optional: googleMap.clear(); // if you want to clear previous drawings
            googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(start).title(startName));
            googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(end).title(endName));
            googleMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions().addAll(points).width(8).color(Color.BLUE));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            // Simple summary bottom sheet
            BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
            View v = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_route_summary, null);
            android.widget.TextView title = v.findViewById(R.id.txtTitle);
            android.widget.TextView line1 = v.findViewById(R.id.txtLine1);
            android.widget.TextView line2 = v.findViewById(R.id.txtLine2);
            android.widget.TextView line3 = v.findViewById(R.id.txtLine3);
            title.setText(startName + " → " + endName);
            line1.setText("ETA: " + etaText);
            line2.setText("Distance: " + distText);
            line3.setText(price != null ? ("Price: R" + String.format(java.util.Locale.getDefault(),"%.2f", price)) : "Price: N/A");
            dialog.setContentView(v);
            dialog.show();

        } catch (org.json.JSONException e) {
            Toast.makeText(getContext(), "Error parsing route data", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDuration(double sec) {
        int minutes = (int)Math.round(sec / 60.0);
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int rem = minutes % 60;
        return hours + " hr " + (rem > 0 ? rem + " min" : "");
    }

    private String formatDistance(double meters) {
        if (meters < 1000) return ((int)meters) + " m";
        double km = meters / 1000.0;
        return String.format(java.util.Locale.getDefault(),"%.1f km", km);
    }













    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}