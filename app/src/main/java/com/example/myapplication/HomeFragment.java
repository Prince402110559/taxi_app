// HomeFragment (production-ready UX pass)
package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private SearchView searchBar, dropSearchView;
    private View progressOverlay; // simple blocking progress overlay

    public ActivityMainBinding binding;
    private static final int REQ_LOCATION = 42;
    private FusedLocationProviderClient fusedClient;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final OkHttpClient http = new OkHttpClient();

    // Simple in-memory cache for ranks by documentId
    private final Map<String, RankInfo> rankCache = new HashMap<>();

    // Cache last options so Back from summary can reopen without refetch
    private List<String> lastStartNames;
    private List<String> lastPriceLabels;
    private List<LatLng> lastStartLLs;
    private LatLng lastEndLL;
    private List<Double> lastPricesRaw;
    private String lastDestinationName;

    // Track current route overlays to avoid stacking
    private com.google.android.gms.maps.model.Polyline currentRouteLine;
    private final List<com.google.android.gms.maps.model.Marker> currentRouteMarkers = new ArrayList<>();

    // Holds minimal rank info
    private static class RankInfo {
        final String id;
        final String name;
        final LatLng latLng;
        RankInfo(String id, String name, LatLng latLng) {
            this.id = id; this.name = name; this.latLng = latLng;
        }
    }

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mapView = root.findViewById(R.id.mapView);
        searchBar = root.findViewById(R.id.searchBar);
        dropSearchView = root.findViewById(R.id.dropSearchView);
        FloatingActionButton myLocationButton = root.findViewById(R.id.myLocation);
        progressOverlay = root.findViewById(R.id.progressRoot); // from progress_overlay.xml included in fragment_home

        searchBar.setQueryHint("Choose Start Location");
        dropSearchView.setQueryHint("Choose Destination");
        searchBar.setIconified(false);
        dropSearchView.setIconified(false);

        myLocationButton.setOnClickListener(v -> zoomToCurrentLocation());

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        root.findViewById(R.id.profileImage).setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.profile));

        searchBar.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) getUserCurrentAddress(address -> searchBar.setQuery(address, false));
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String start) {
                String drop = dropSearchView.getQuery().toString();
                if (!start.isEmpty() && !drop.isEmpty()) {
                    findAndShowRidesToDestination(drop);
                } else {
                    showInfoSheet("Enter both start and destination to continue.", "OK", null, null);
                }
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });

        dropSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String drop) {
                String start = searchBar.getQuery().toString();
                if (!start.isEmpty() && !drop.isEmpty()) {
                    findAndShowRidesToDestination(drop);
                } else {
                    showInfoSheet("Enter both start and destination to continue.", "OK", null, null);
                }
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });

        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        try {
            MapsInitializer.initialize(requireContext());
            googleMap = map;

            applyMapStyle();

            LatLngBounds SOUTH_AFRICA = new LatLngBounds(
                    new LatLng(-35.0, 16.0),
                    new LatLng(-22.0, 33.0)
            );
            googleMap.setLatLngBoundsForCameraTarget(SOUTH_AFRICA);
            googleMap.setMinZoomPreference(4.5f);
            googleMap.setMaxZoomPreference(20.0f);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-29.0, 24.0), 5.0f));

            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setRotateGesturesEnabled(false);
            googleMap.getUiSettings().setTiltGesturesEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);

            if (hasLocationPermission()) {
                enableMyLocation();
                moveCameraToMyLocation();
            } else {
                requestLocationPermission();
            }
        } catch (Exception ignored) {}
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
            moveCameraToMyLocation();
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            showInfoSheet("Location permission denied. You can still search, but nearby sorting will be less accurate.", "OK", null, null);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    private void moveCameraToMyLocation() {
        if (!hasLocationPermission()) return;
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 14f));
            }
        });
    }

    private void enableMyLocation() {
        if (googleMap == null) return;
        try {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException ignored) {}
    }

    private void applyMapStyle() {
        if (googleMap == null) return;
        int night = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int styleRes = (night == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                ? R.raw.map_style_dark
                : R.raw.map_style_light;
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), styleRes));
        } catch (Resources.NotFoundException ignored) {}
    }

    private void getUserCurrentAddress(Consumer<String> callback) {
        if (!hasLocationPermission()) { callback.accept(""); return; }
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                Geocoder geocoder = new Geocoder(requireContext());
                try {
                    List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        callback.accept(addresses.get(0).getAddressLine(0));
                        return;
                    }
                } catch (IOException ignored) {}
            }
            callback.accept("");
        });
    }

    // Step 1: Find destination rank → routes to it
    private void findAndShowRidesToDestination(String endRankName) {
        String normalized = endRankName.trim().toLowerCase();

        if (googleMap != null) googleMap.clear();
        showProgress(true);

        db.collection("rank")
                .whereEqualTo("name_lowercase", normalized)
                .get()
                .addOnSuccessListener(endQuery -> {
                    if (endQuery.isEmpty()) {
                        showProgress(false);
                        showInfoSheet("Destination not found. Try a different spelling.", "OK", null, null);
                        return;
                    }

                    DocumentSnapshot endDoc = endQuery.getDocuments().get(0);
                    RankInfo endRank = toRankInfo(endDoc);
                    if (endRank.latLng == null) {
                        showProgress(false);
                        showInfoSheet("This destination has no map location yet. Pick another destination.", "OK", null, null);
                        return;
                    }
                    rankCache.put(endRank.id, endRank);

                    // Fetch all routes ending here
                    db.collection("route")
                            .whereEqualTo("endRankId", endRank.id)
                            .get()
                            .addOnSuccessListener(routeQuery -> {
                                if (routeQuery.isEmpty()) {
                                    showProgress(false);
                                    showInfoSheet("There are no rides to this destination.", "OK", null, null);
                                    return;
                                }

                                // Collect starts and prices (dedupe preserving order)
                                Map<String, Double> startIdToPrice = new LinkedHashMap<>();
                                for (DocumentSnapshot routeDoc : routeQuery) {
                                    String startId = routeDoc.getString("startRankId");
                                    Double price = routeDoc.getDouble("price");
                                    if (startId != null && price != null && !startIdToPrice.containsKey(startId)) {
                                        startIdToPrice.put(startId, price);
                                    }
                                }
                                if (startIdToPrice.isEmpty()) {
                                    showProgress(false);
                                    showInfoSheet("No valid rides were found.", "OK", null, null);
                                    return;
                                }

                                // Resolve any missing starts from cache; for the rest, batch fetch
                                List<String> needFetch = new ArrayList<>();
                                for (String id : startIdToPrice.keySet()) {
                                    if (!rankCache.containsKey(id)) needFetch.add(id);
                                }

                                Consumer<Void> continueWithDisplay = unused -> {
                                    // Now build lists
                                    List<String> startNames = new ArrayList<>();
                                    List<LatLng> startLatLngs = new ArrayList<>();
                                    List<Double> pricesRaw = new ArrayList<>();

                                    for (Map.Entry<String, Double> e : startIdToPrice.entrySet()) {
                                        RankInfo info = rankCache.get(e.getKey());
                                        if (info != null && info.latLng != null) {
                                            startNames.add(info.name != null ? info.name : "(unknown)");
                                            startLatLngs.add(info.latLng);
                                            pricesRaw.add(e.getValue());
                                        }
                                    }
                                    if (startNames.isEmpty()) {
                                        showProgress(false);
                                        showInfoSheet("We couldn’t load starting points for this destination.", "OK", null, null);
                                        return;
                                    }

                                    // Sort by nearest start to user
                                    fusedClient.getLastLocation().addOnSuccessListener(userLoc -> {
                                        double uLat = (userLoc != null) ? userLoc.getLatitude() : Double.NaN;
                                        double uLng = (userLoc != null) ? userLoc.getLongitude() : Double.NaN;

                                        class RouteOption {
                                            final String startName; final LatLng startLL; final Double price; final float distanceMeters;
                                            RouteOption(String n, LatLng ll, Double p, float d){ startName=n; startLL=ll; price=p; distanceMeters=d; }
                                        }
                                        List<RouteOption> opts = new ArrayList<>();
                                        for (int i = 0; i < startNames.size(); i++) {
                                            float dist = Float.MAX_VALUE;
                                            if (!Double.isNaN(uLat) && !Double.isNaN(uLng)) {
                                                float[] res = new float[1];
                                                android.location.Location.distanceBetween(
                                                        uLat, uLng,
                                                        startLatLngs.get(i).latitude, startLatLngs.get(i).longitude,
                                                        res
                                                );
                                                dist = res[0];
                                            }
                                            opts.add(new RouteOption(startNames.get(i), startLatLngs.get(i), pricesRaw.get(i), dist));
                                        }
                                        Collections.sort(opts, (a, b) -> Float.compare(a.distanceMeters, b.distanceMeters));

                                        List<String> sortedStartNames = new ArrayList<>();
                                        List<LatLng> sortedStartLLs = new ArrayList<>();
                                        List<Double> sortedPricesRaw = new ArrayList<>();
                                        List<String> sortedPriceLabels = new ArrayList<>();
                                        for (RouteOption o : opts) {
                                            sortedStartNames.add(o.startName);
                                            sortedStartLLs.add(o.startLL);
                                            sortedPricesRaw.add(o.price);
                                            sortedPriceLabels.add(String.format(Locale.getDefault(),"Price: R%.2f", o.price));
                                        }

                                        showProgress(false);
                                        lastStartNames = sortedStartNames;
                                        lastPriceLabels = sortedPriceLabels;
                                        lastStartLLs = sortedStartLLs;
                                        lastEndLL = endRank.latLng;
                                        lastPricesRaw = sortedPricesRaw;
                                        lastDestinationName = endRank.name != null ? endRank.name : endRankName;

                                        showBottomSheetWithPrices(
                                                lastStartNames,
                                                lastDestinationName,
                                                lastPriceLabels,
                                                lastStartLLs,
                                                lastEndLL,
                                                lastPricesRaw
                                        );
                                    });
                                };

                                if (needFetch.isEmpty()) {
                                    continueWithDisplay.accept(null);
                                } else {
                                    db.collection("rank")
                                            .whereIn(FieldPath.documentId(), needFetch)
                                            .get()
                                            .addOnSuccessListener(startRanksQuery -> {
                                                for (DocumentSnapshot doc : startRanksQuery) {
                                                    RankInfo ri = toRankInfo(doc);
                                                    if (ri != null) rankCache.put(ri.id, ri);
                                                }
                                                continueWithDisplay.accept(null);
                                            })
                                            .addOnFailureListener(e -> {
                                                showProgress(false);
                                                showInfoSheet("We couldn’t load starting points. Please try again.", "Try again",
                                                        v -> findAndShowRidesToDestination(endRankName),
                                                        "Cancel");
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                showProgress(false);
                                showInfoSheet("We couldn’t load routes. Please try again.", "Try again",
                                        v -> findAndShowRidesToDestination(endRankName),
                                        "Cancel");
                            });
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    showInfoSheet("We couldn’t reach the destination data. Check your connection and try again.", "Try again",
                            v -> findAndShowRidesToDestination(endRankName),
                            "Cancel");
                });
    }

    // Bottom sheet list: pick a start → confirm → draw route
    private void showBottomSheetWithPrices(
            List<String> startNames, String destinationName, List<String> priceLabels,
            List<LatLng> startLatLngs, LatLng endLatLng, List<Double> pricesRaw
    ) {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ride_options, null);
        ListView listView = sheetView.findViewById(R.id.listView);
        ((TextView) sheetView.findViewById(R.id.titleText))
                .setText("Start options for " + destinationName);
        ((TextView) sheetView.findViewById(R.id.subtitleText))
                .setText("Closest shown first");

        // Model for binding
        class Item { String name, price, dest, distance; LatLng sLL; Double rawPrice; }
        List<Item> items = new ArrayList<>();
        int size = Math.min(startNames.size(), Math.min(priceLabels.size(), startLatLngs.size()));
        for (int i = 0; i < size; i++) {
            Item it = new Item();
            it.name = startNames.get(i);
            it.price = priceLabels.get(i).replace("Price: ", ""); // "R25.00"
            it.dest = "to " + destinationName;
            it.sLL = startLatLngs.get(i);
            it.rawPrice = pricesRaw.get(i);
            it.distance = "";
            items.add(it);
        }

        final int[] selected = {-1};

        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(requireContext(), R.layout.ride_option_row, items) {
            @NonNull @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) row = getLayoutInflater().inflate(R.layout.ride_option_row, parent, false);
                Item it = getItem(position);
                if (it != null) {
                    ((TextView) row.findViewById(R.id.txtTitle)).setText(it.name);
                    ((TextView) row.findViewById(R.id.txtPrice)).setText(it.price);
                    ((TextView) row.findViewById(R.id.txtSubtitle)).setText(it.dest);
                    ((TextView) row.findViewById(R.id.txtDistance)).setText(it.distance != null ? it.distance : "");
                }
                MaterialCardView card = (MaterialCardView) row; // root must be MaterialCardView
                card.setCardElevation(position == 0 ? 8f : 4f);
                row.setBackgroundResource(position == selected[0]
                        ? R.drawable.ride_row_bg_selected
                        : R.drawable.ride_row_bg);
                return row;
            }
        };
        listView.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        Button btnSelect = sheetView.findViewById(R.id.btnSelect);
        Button btnClose  = sheetView.findViewById(R.id.btnCloseSheet);
        btnSelect.setEnabled(false);

        listView.setOnItemClickListener((parent, row, position, id) -> {
            selected[0] = position;
            btnSelect.setEnabled(true);
            adapter.notifyDataSetChanged();
        });

        btnSelect.setOnClickListener(v -> {
            if (selected[0] < 0) return;
            Item it = items.get(selected[0]);
            dialog.dismiss();
            showProgress(true);
            // Clear previous map overlays before drawing a new route
            clearCurrentRouteOverlays();
            requestOsrmAndRender(it.sLL, endLatLng, it.name, destinationName, it.rawPrice);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Routing: OSRM with graceful errors and progress handling
    private void requestOsrmAndRender(LatLng start, LatLng end, String startName, String endName, Double price) {
        if (Math.abs(start.latitude) > 90 || Math.abs(end.latitude) > 90 || Math.abs(start.longitude) > 180 || Math.abs(end.longitude) > 180) {
            showProgress(false);
            showInfoSheet("The selected route has invalid coordinates.", "OK", null, null);
            return;
        }

        // Ensure only one route at a time
        clearCurrentRouteOverlays();

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.longitude + "," + start.latitude + ";"
                + end.longitude + "," + end.latitude
                + "?overview=full&geometries=geojson&alternatives=false&steps=false";

        Request request = new Request.Builder().url(url).build();
        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showProgress(false);
                    showInfoSheet("We couldn’t draw the route. Check your connection and try again.", "Retry",
                            v -> requestOsrmAndRender(start, end, startName, endName, price),
                            "Cancel");
                });
            }

            @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                String bodyStr = response.body() != null ? response.body().string() : null;

                if (!response.isSuccessful() || bodyStr == null) {
                    requireActivity().runOnUiThread(() -> {
                        showProgress(false);
                        showInfoSheet("Route service is unavailable right now. Please try again shortly.", "Retry",
                                v -> requestOsrmAndRender(start, end, startName, endName, price),
                                "Cancel");
                    });
                    return;
                }

                if (bodyStr.contains("\"code\":\"NoRoute\"")) {
                    requireActivity().runOnUiThread(() -> {
                        showProgress(false);
                        showInfoSheet("No drivable route found between the selected ranks. Try a nearby starting rank or adjust the destination pin.", "Choose another start",
                                v -> findAndShowRidesToDestination(endName),
                                "Close");
                    });
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    connectOSRMRouteWithEta(bodyStr, start, end, startName, endName, price);
                    showProgress(false);
                });
            }
        });
    }

    private void connectOSRMRouteWithEta(String json, LatLng start, LatLng end, String startName, String endName, Double price) {
        try {
            JSONObject data = new JSONObject(json);
            JSONArray routes = data.getJSONArray("routes");
            if (routes.length() == 0) {
                showInfoSheet("No route found. Try another start.", "OK", null, null);
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            double durationSec = route.getDouble("duration");
            double distanceM   = route.getDouble("distance");

            String etaText  = formatDuration(durationSec);
            String distText = formatDistance(distanceM);

            JSONArray coordinates = route.getJSONObject("geometry").getJSONArray("coordinates");
            List<LatLng> points = new ArrayList<>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray c = coordinates.getJSONArray(i);
                double lng = c.getDouble(0);
                double lat = c.getDouble(1);
                LatLng p = new LatLng(lat, lng);
                points.add(p);
                builder.include(p);
            }
            int routeColor = ContextCompat.getColor(requireContext(), R.color.routePrimary);

            com.google.android.gms.maps.model.Marker startMarker =
                    googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(start).title(startName));
            com.google.android.gms.maps.model.Marker endMarker =
                    googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(end).title(endName));
            if (startMarker != null) currentRouteMarkers.add(startMarker);
            if (endMarker != null) currentRouteMarkers.add(endMarker);

            currentRouteLine = googleMap.addPolyline(
                    new com.google.android.gms.maps.model.PolylineOptions().addAll(points).width(8).color(routeColor)
            );

            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            BottomSheetDialog summaryDialog = new BottomSheetDialog(requireContext());
            View sv = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_route_summary, null);

            TextView titleV = sv.findViewById(R.id.txtTitle);
            TextView chipEta = sv.findViewById(R.id.chipEta);
            TextView chipDist = sv.findViewById(R.id.chipDistance);
            TextView chipPrice = sv.findViewById(R.id.chipPrice);
            TextView txtNote = sv.findViewById(R.id.txtNote);
            Button btnBack = sv.findViewById(R.id.btnBackToOptions);
            Button btnClose = sv.findViewById(R.id.btnClose);

            titleV.setText(startName + " → " + endName);
            chipEta.setText("ETA " + etaText);
            chipDist.setText(distText);
            chipPrice.setText(price != null
                    ? "R" + String.format(Locale.getDefault(),"%.2f", price)
                    : "N/A");
            txtNote.setText("Tap Back to choose a different start, or Close to keep this route on the map.");

            summaryDialog.setContentView(sv);

            btnBack.setOnClickListener(v -> {
                // Optional: remove preview route when going back to the list.
                clearCurrentRouteOverlays();
                summaryDialog.dismiss();
                if (lastStartNames != null) {
                    showBottomSheetWithPrices(
                            lastStartNames, lastDestinationName, lastPriceLabels,
                            lastStartLLs, lastEndLL, lastPricesRaw
                    );
                } else {
                    showInfoSheet("Couldn’t restore options.", "OK", null, null);
                }
            });

            btnClose.setOnClickListener(v -> summaryDialog.dismiss());
            summaryDialog.show();

        } catch (Exception ignored) {
            showInfoSheet("We couldn’t process the route details. Please try again.", "Retry",
                    btn -> requestOsrmAndRender(start, end, startName, endName, price),
                    "Close");
        }
    }

    private String formatDuration(double sec) {
        int minutes = (int)Math.round(sec / 60.0);
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int rem = minutes % 60;
        return hours + " hr " + (rem > 0 ? rem + " min" : "");
    }
    @Nullable
    private RankInfo toRankInfo(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        LatLng ll = extractLatLngFromRank(doc);
        String name = doc.getString("name");
        return new RankInfo(doc.getId(), name, ll);
    }

    @Nullable
    private LatLng extractLatLngFromRank(DocumentSnapshot doc) {
        Object raw = doc.get("location");
        if (raw instanceof com.google.firebase.firestore.GeoPoint) {
            com.google.firebase.firestore.GeoPoint gp = (com.google.firebase.firestore.GeoPoint) raw;
            return new LatLng(gp.getLatitude(), gp.getLongitude());
        } else if (raw instanceof String) {
            try {
                String locStr = (String) raw; // legacy string "26.1057° S, 28.1016° E"
                String[] parts = locStr.split(",");
                double lat = parseDeg(parts[0]);
                double lng = parseDeg(parts[1]);
                return new LatLng(lat, lng);
            } catch (Exception ignored) {}
        }
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
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

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

    private String formatDistance(double meters) {
        if (meters < 1000) return ((int)meters) + " m";
        double km = meters / 1000.0;
        return String.format(Locale.getDefault(),"%.1f km", km);
    }

    // Shows/hides a blocking progress overlay; prevents repeated taps
    private void showProgress(boolean show) {
        if (progressOverlay == null) return;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (getView() != null) getView().setEnabled(!show);
    }

    // Small reusable bottom sheet for friendly messages and optional actions
    private void showInfoSheet(String message, String primaryText, View.OnClickListener primaryAction, String secondaryText) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_info, null);
        TextView txt = v.findViewById(R.id.txtMessage);
        Button btnPrimary = v.findViewById(R.id.btnPrimary);
        Button btnSecondary = v.findViewById(R.id.btnSecondary);

        txt.setText(message);

        if (primaryText != null) {
            btnPrimary.setText(primaryText);
            btnPrimary.setOnClickListener(view -> {
                dialog.dismiss();
                if (primaryAction != null) primaryAction.onClick(view);
            });
        } else {
            btnPrimary.setVisibility(View.GONE);
        }

        if (secondaryText != null) {
            btnSecondary.setText(secondaryText);
            btnSecondary.setOnClickListener(view -> dialog.dismiss());
        } else {
            btnSecondary.setVisibility(View.GONE);
        }

        dialog.setContentView(v);
        dialog.show();
    }

    private void clearCurrentRouteOverlays() {
        if (currentRouteLine != null) {
            currentRouteLine.remove();
            currentRouteLine = null;
        }
        for (com.google.android.gms.maps.model.Marker m : currentRouteMarkers) {
            if (m != null) m.remove();
        }
        currentRouteMarkers.clear();
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override public void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
}
