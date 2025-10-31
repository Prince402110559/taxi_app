package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView; // ✅ Correct import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.location.Priority;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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



    private void findAndShowRidesToDestination(String endRankName) {
                // Normalize user input for case-insensitive matching

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
                    String endRankId = endQuery.getDocuments().get(0).getId();
                    String destinationRankName = endQuery.getDocuments().get(0).getString("name");


                    db.collection("route")
                            .whereEqualTo("endRankId", endRankId)
                            .get()
                            .addOnSuccessListener(routeQuery -> {

                                if (routeQuery.isEmpty()) {
                                    Toast.makeText(getContext(), "No rides available to this destination", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                List<String> startRankIds = new ArrayList<>();
                                List<Double> prices = new ArrayList<>();

                                for (DocumentSnapshot routeDoc : routeQuery) {
                                    String startId = routeDoc.getString("startRankId");
                                    Double price = routeDoc.getDouble("price");

                                    if (startId != null && price != null) {
                                        startRankIds.add(startId);

                                        prices.add(price);
                                    }
                                }

                                if (startRankIds.isEmpty()) {
                                    Toast.makeText(getContext(), "No valid start ranks found", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                List<String> startridesDisplayList = new ArrayList<>();
                                List<String> ridePricesList = new ArrayList<>();

                                db.collection("rank")
                                        .whereIn(FieldPath.documentId(), startRankIds)
                                        .get()
                                        .addOnSuccessListener(startRanksQuery -> {
                                            for (DocumentSnapshot doc : startRanksQuery) {
                                                String rankName = doc.getString("name");
                                                int idx = startRankIds.indexOf(doc.getId());
                                                double price = prices.get(idx);



                                                startridesDisplayList.add(rankName);
                                                ridePricesList.add(String.format("Price: R%.2f", price));
                                            }

                                            showBottomSheetWithPrices(startridesDisplayList,endRankName, ridePricesList);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Failed loading start ranks: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed loading routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed loading destination ranks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


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
            // TODO: 2025/10/30 we need to add google maps stuff to show the route on the map 
        });
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