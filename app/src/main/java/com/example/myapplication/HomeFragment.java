package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


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

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
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
        FloatingActionButton locationButton = view.findViewById(R.id.myLocation);

        searchBar.setQueryHint("Choose Start Location");
        dropSearchView.setQueryHint("Choose Destination");
        searchBar.setIconified(false);
        dropSearchView.setIconified(false);
        locationButton.setOnClickListener(v -> zoomToCurrentLocation());

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
                    findAndShowRidesToDestination(start, drop);
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



        // we also use this one to handle user input
        //we have  2 incase the user presses submit while on either of the searchviews
        dropSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String drop) {
                String start = searchBar.getQuery().toString();
                if (!start.isEmpty() && !drop.isEmpty()) {
                    findAndShowRidesToDestination(start, drop);
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

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
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
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
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
    //method to move users view o their location
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void moveCameraToMyLocation() {
        if (!hasLocationPermission()) return;
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && googleMap != null) {
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
            }
        });
    }
    //method to enable ad find the users location on he map
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

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
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

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void zoomToCurrentLocation(){
        if (googleMap == null || fusedClient == null){
            Toast.makeText(requireContext(), "Map or location client not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasLocationPermission()){
            requestLocationPermission();
            return;
        }

        try {
            // Enable blue dot
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Get current location
            fusedClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                } else {
                    Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void findAndShowRidesToDestination(String startRankName, String endRankName) {
        String startNormalized = startRankName.trim().toLowerCase();
        String endNormalized = endRankName.trim().toLowerCase();

        //Get start rank
        db.collection("rank")
                .whereEqualTo("name_lowercase", startNormalized)
                .get()
                .addOnSuccessListener(startQuery -> {
                    if (startQuery.isEmpty()) {
                        Toast.makeText(getContext(), "Start rank not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot startDoc = startQuery.getDocuments().get(0);
                    String startRankId = startDoc.getId();
                    String startRankRealName = startDoc.getString("name");
                    Double startLat = startDoc.getDouble("latitude");
                    Double startLng = startDoc.getDouble("longitude");

                    if (startLat == null || startLng == null) {
                        Log.d("DEBUG", "Start rank missing coordinates: " + startDoc.getData());
                        Toast.makeText(getContext(), "Start rank coordinates missing in database", Toast.LENGTH_LONG).show();
                        return;
                    }

                    LatLng startPoint = new LatLng(startLat, startLng);

                    //Get end rank
                    db.collection("rank")
                            .whereEqualTo("name_lowercase", endNormalized)
                            .get()
                            .addOnSuccessListener(endQuery -> {
                                if (endQuery.isEmpty()) {
                                    Toast.makeText(getContext(), "Destination rank not found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                DocumentSnapshot endDoc = endQuery.getDocuments().get(0);
                                String endRankId = endDoc.getId();
                                String endRankRealName = endDoc.getString("name");
                                Double endLat = endDoc.getDouble("latitude");
                                Double endLng = endDoc.getDouble("longitude");

                                if (endLat == null || endLng == null) {
                                    Log.d("DEBUG", "End rank missing coordinates: " + endDoc.getData());
                                    Toast.makeText(getContext(), "Destination coordinates missing in database", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                LatLng endPoint = new LatLng(endLat, endLng);

                                //Find the route by IDs
                                db.collection("route")
                                        .whereEqualTo("startRankId", startRankId)
                                        .whereEqualTo("endRankId", endRankId)
                                        .get()
                                        .addOnSuccessListener(routeQuery -> {
                                            if (routeQuery.isEmpty()) {
                                                Toast.makeText(getContext(), "No route found between " + startRankRealName + " and " + endRankRealName, Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            DocumentSnapshot routeDoc = routeQuery.getDocuments().get(0);
                                            Double price = routeDoc.getDouble("price");

                                            // Draw route on map
                                            createRoute(startPoint, endPoint, startRankRealName, endRankRealName);

                                            // Show price
                                            Toast.makeText(getContext(), "Price: R" + price, Toast.LENGTH_LONG).show();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed loading route: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed loading destination rank: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed loading start rank: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    //am not sure if we still gonna need this class. The prices are already covered in the findAndShowRidesToDestination()
    private void showBottomSheetWithPrices(List<String> start ,String destination, List<String> prices) {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ride_options, null);
        ListView listView = sheetView.findViewById(R.id.listView);

        List<String> displayList = new ArrayList<>();
        for (int i = 0; i < start.size(); i++) {
            displayList.add(start.get(i) + " → " + destination + " \n" + prices.get(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        dialog.setContentView(sheetView);
        dialog.show();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Toast.makeText(getContext(), "You selected: " + displayList.get(position), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }

    //finds the route between two coordinates
    private void createRoute(LatLng start, LatLng end, String startName, String endName) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.longitude + "," + start.latitude + ";"
                + end.longitude + "," + end.latitude
                + "?overview=full&geometries=geojson";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to draw route: " + startName + " → " + endName, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    requireActivity().runOnUiThread(() -> connectOSRMRoute(json, start, end, startName, endName));
                }
            }
        });
    }

    //connects routes between two coordinates and makes the route visible on the map
    private void connectOSRMRoute(String json, LatLng start, LatLng end, String startName, String endName) {
        try {
            JSONObject data = new JSONObject(json);
            JSONArray routes = data.getJSONArray("routes");

            if (routes.length() == 0) {
                Toast.makeText(getContext(), "No route found", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");

            List<LatLng> points = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lng = coord.getDouble(0);
                double lat = coord.getDouble(1);
                points.add(new LatLng(lat, lng));
            }

            // Draw route and markers
            googleMap.addMarker(new MarkerOptions().position(start).title(startName));
            googleMap.addMarker(new MarkerOptions().position(end).title(endName));

            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(8);
            googleMap.addPolyline(polylineOptions);

// 🔹 Focus camera on route
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error parsing route data", Toast.LENGTH_SHORT).show();
        }
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
