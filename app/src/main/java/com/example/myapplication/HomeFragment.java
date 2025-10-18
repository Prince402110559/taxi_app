package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapsInitializer;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private ImageView menuButton, profileImage;
    private SearchView searchBar;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.mapView);
        menuButton = view.findViewById(R.id.menuButton);
        profileImage = view.findViewById(R.id.profileImage);
        searchBar = view.findViewById(R.id.searchBar);

        // Initialize the map view
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        // Menu button click event
        menuButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "Menu clicked", Toast.LENGTH_SHORT).show()
        );

        // Profile image click event
        profileImage.setOnClickListener(v ->
                Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show()
        );

        // Search bar text listener
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(getContext(), "Searching for: " + query, Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle live search suggestions here if needed
                return false;
            }
        });

        return view;
    }

    // Called when map is ready
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        MapsInitializer.initialize(requireContext());
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Optionally set a default location or marker here
        // LatLng location = new LatLng(-26.2041, 28.0473); // Johannesburg example
        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
    }

    // Lifecycle methods for MapView
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