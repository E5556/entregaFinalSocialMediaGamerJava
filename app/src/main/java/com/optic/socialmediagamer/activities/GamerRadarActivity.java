package com.optic.socialmediagamer.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;
import com.optic.socialmediagamer.utils.RankHelper;

import java.util.ArrayList;
import java.util.List;

public class GamerRadarActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERM_LOCATION = 101;
    private static final double RADIUS_DEG = 0.009; // ~1 km

    Toolbar mToolbar;
    SwitchCompat mSwitchGhost;
    TextView mTextViewStatus;
    RecyclerView mRecyclerViewNearby;

    GoogleMap mMap;
    FusedLocationProviderClient mFusedClient;
    AuthProvider mAuthProvider;
    UsersProvider mUsersProvider;

    double mMyLat = 0, mMyLng = 0;
    boolean mGhostMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamer_radar);

        mToolbar            = findViewById(R.id.toolbarRadar);
        mSwitchGhost        = findViewById(R.id.switchGhostMode);
        mTextViewStatus     = findViewById(R.id.textViewRadarStatus);
        mRecyclerViewNearby = findViewById(R.id.recyclerViewNearby);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());
        mToolbar.setTitle("📡 Gamer Radar");

        mRecyclerViewNearby.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        mAuthProvider  = new AuthProvider();
        mUsersProvider = new UsersProvider();
        mFusedClient   = LocationServices.getFusedLocationProviderClient(this);

        mSwitchGhost.setOnCheckedChangeListener((btn, checked) -> {
            mGhostMode = checked;
            Toast.makeText(this,
                    checked ? "👻 Modo fantasma ON: tu ubicación no se comparte"
                            : "📍 Modo fantasma OFF: compartiendo ubicación",
                    Toast.LENGTH_SHORT).show();
        });

        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        requestLocationPermission();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (mMyLat != 0) {
            centerMap(mMyLat, mMyLng);
            loadNearbyUsers();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERM_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_LOCATION && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            mTextViewStatus.setText("⚠️ Permiso de ubicación denegado");
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mFusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) {
                mTextViewStatus.setText("⚠️ No se pudo obtener ubicación");
                return;
            }
            mMyLat = loc.getLatitude();
            mMyLng = loc.getLongitude();
            mTextViewStatus.setText("📍 Buscando gamers en ~1 km...");

            if (!mGhostMode) {
                mUsersProvider.updateLocation(mAuthProvider.getUid(), mMyLat, mMyLng);
            }

            if (mMap != null) {
                centerMap(mMyLat, mMyLng);
                loadNearbyUsers();
            }
        });
    }

    private void centerMap(double lat, double lng) {
        LatLng pos = new LatLng(lat, lng);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title("Tú estás aquí")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
    }

    private void loadNearbyUsers() {
        mUsersProvider.getUsersNearby(mMyLat, RADIUS_DEG).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> nearby = new ArrayList<>();
                    String myId = mAuthProvider.getUid();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (doc.getId().equals(myId)) continue;
                        Double docLng = doc.getDouble("lng");
                        if (docLng == null) continue;
                        // Filter by longitude too (Firestore only supports 1-field range)
                        if (Math.abs(docLng - mMyLng) > RADIUS_DEG) continue;
                        // Only show users active in last 30 minutes
                        Long updatedAt = doc.getLong("locationUpdatedAt");
                        if (updatedAt != null
                                && (System.currentTimeMillis() - updatedAt) > 30 * 60 * 1000L) continue;
                        nearby.add(doc);
                    }

                    mTextViewStatus.setText("📡 " + nearby.size() + " gamer(s) cerca de ti");
                    showOnMap(nearby);
                    mRecyclerViewNearby.setAdapter(new NearbyAdapter(nearby));
                });
    }

    private void showOnMap(List<DocumentSnapshot> users) {
        if (mMap == null) return;
        for (DocumentSnapshot doc : users) {
            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            if (lat == null || lng == null) continue;
            String username  = doc.getString("username");
            long xp          = doc.getLong("xp") != null ? doc.getLong("xp") : 0L;
            String nowPlaying = doc.getString("nowPlaying");
            String snippet = RankHelper.getRankEmoji(xp) + " " + RankHelper.getRankName(xp)
                    + (nowPlaying != null && !nowPlaying.isEmpty() ? " · 🎮 " + nowPlaying : "");

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .title("@" + (username != null ? username : "gamer"))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        }
    }

    private class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.VH> {
        List<DocumentSnapshot> list;
        NearbyAdapter(List<DocumentSnapshot> l) { this.list = l; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DocumentSnapshot doc = list.get(pos);
            String username   = doc.getString("username");
            long xp           = doc.getLong("xp") != null ? doc.getLong("xp") : 0L;
            String nowPlaying = doc.getString("nowPlaying");

            h.text1.setText(RankHelper.getRankEmoji(xp) + " @" + (username != null ? username : "?"));
            h.text2.setText(nowPlaying != null && !nowPlaying.isEmpty()
                    ? "🎮 " + nowPlaying : RankHelper.getRankName(xp));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(@NonNull View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
