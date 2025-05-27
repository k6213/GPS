package com.example.gps;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private NaverMap naverMap;
    private Marker currentMarker, startMarker, endMarker;
    private PathOverlay pathOverlay;
    private FusedLocationSource locationSource;

    private LatLng selectedLatLng, startLatLng, endLatLng;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout buttonsLayout;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String TMAP_API_KEY = "vV0xCHFyMO18u8IJoh45laAG1WQ4wqyS3nfxn8fw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.right_drawer);
        buttonsLayout = findViewById(R.id.buttonsLayout);
        buttonsLayout.setVisibility(View.GONE);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        Button btnStartMove = findViewById(R.id.btnStartMove);
        Button btnSelectStart = findViewById(R.id.btnSelectStart);
        Button btnSelectEnd = findViewById(R.id.btnSelectEnd);

        buttonsLayout.setVisibility(View.VISIBLE);

        btnStartMove.setOnClickListener(v -> {
            if (startLatLng == null || endLatLng == null) {
                Toast.makeText(this, "출발지와 도착지를 모두 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            requestTMapWalkingRoute(startLatLng, endLatLng);
        });

        btnSelectStart.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                startLatLng = selectedLatLng;
                if (startMarker != null) startMarker.setMap(null);
                startMarker = new Marker();
                startMarker.setPosition(startLatLng);
                startMarker.setCaptionText("출발지");
                startMarker.setMap(naverMap);
            }
        });

        btnSelectEnd.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                endLatLng = selectedLatLng;
                if (endMarker != null) endMarker.setMap(null);
                endMarker = new Marker();
                endMarker.setPosition(endLatLng);
                endMarker.setCaptionText("도착지");
                endMarker.setMap(naverMap);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                Toast.makeText(this, "메뉴: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        map.setLocationSource(locationSource);
        map.setLocationTrackingMode(LocationTrackingMode.Follow);

        map.setOnMapClickListener((point, coord) -> {
            selectedLatLng = coord;
            Toast.makeText(this, "선택 위치: " + coord.latitude + ", " + coord.longitude, Toast.LENGTH_SHORT).show();
        });
    }

    private void requestTMapWalkingRoute(LatLng start, LatLng end) {
        new Thread(() -> {
            try {
                String urlStr = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json"
                        + "&startX=" + URLEncoder.encode(String.valueOf(start.longitude), "UTF-8")
                        + "&startY=" + URLEncoder.encode(String.valueOf(start.latitude), "UTF-8")
                        + "&endX=" + URLEncoder.encode(String.valueOf(end.longitude), "UTF-8")
                        + "&endY=" + URLEncoder.encode(String.valueOf(end.latitude), "UTF-8")
                        + "&startName=출발지&endName=도착지"
                        + "&appKey=" + TMAP_API_KEY;

                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray features = new JSONObject(sb.toString()).getJSONArray("features");
                List<LatLng> path = new ArrayList<>();

                for (int i = 0; i < features.length(); i++) {
                    JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                    if ("LineString".equals(geometry.getString("type"))) {
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        for (int j = 0; j < coords.length(); j++) {
                            JSONArray point = coords.getJSONArray(j);
                            path.add(new LatLng(point.getDouble(1), point.getDouble(0)));
                        }
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> drawPathAndAnimate(path));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, "TMap 경로 요청 실패", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void drawPathAndAnimate(List<LatLng> path) {
        if (path.isEmpty()) {
            Toast.makeText(this, "경로가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pathOverlay == null) pathOverlay = new PathOverlay();
        pathOverlay.setMap(null);
        pathOverlay.setCoords(path);
        pathOverlay.setColor(0xFF007AFF);
        pathOverlay.setWidth(12);
        pathOverlay.setMap(naverMap);

        if (currentMarker != null) currentMarker.setMap(null);
        currentMarker = new Marker();
        currentMarker.setPosition(path.get(0));
        currentMarker.setMap(naverMap);
        naverMap.moveCamera(CameraUpdate.scrollTo(path.get(0)));

        Handler handler = new Handler(Looper.getMainLooper());
        final int[] index = {1};
        Runnable animateNext = new Runnable() {
            @Override
            public void run() {
                if (index[0] < path.size()) {
                    LatLng from = currentMarker.getPosition();
                    LatLng to = path.get(index[0]++);
                    animateMarker(currentMarker, from, to, 500);
                    handler.postDelayed(this, 300);
                } else {
                    Toast.makeText(MapsActivity.this, "도보 이동 완료", Toast.LENGTH_SHORT).show();
                }
            }
        };
        handler.post(animateNext);
    }

    private void animateMarker(Marker marker, LatLng from, LatLng to, long durationMillis) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(durationMillis);
        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            double lat = from.latitude + (to.latitude - from.latitude) * t;
            double lng = from.longitude + (to.longitude - from.longitude) * t;
            marker.setPosition(new LatLng(lat, lng));
            naverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(lat, lng)));
        });
        animator.start();
    }

    @Override protected void onStart()   { super.onStart(); mapView.onStart(); }
    @Override protected void onResume()  { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()   { super.onPause(); mapView.onPause(); }
    @Override protected void onStop()    { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory()  { super.onLowMemory(); mapView.onLowMemory(); }
}






