package com.example.gps;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.*;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private NaverMap naverMap;

    private Marker currentMarker;
    private Marker startMarker, endMarker;
    private PathOverlay pathOverlay;
    private FusedLocationSource locationSource;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private LatLng selectedLatLng = null;
    private LatLng startLatLng = null;
    private LatLng endLatLng = null;

    private static final String TMAP_API_KEY = "vV0xCHFyMO18u8IJoh45laAG1WQ4wqyS3nfxn8fw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        Button btnStartMove = findViewById(R.id.btnStartMove);
        Button btnToggleJoystick = findViewById(R.id.btnToggleJoystick);
        Button btnSelectStart = findViewById(R.id.btnSelectStart);
        Button btnSelectEnd = findViewById(R.id.btnSelectEnd);
        JoystickView joystick = findViewById(R.id.joystick);

        btnStartMove.setOnClickListener(v -> {
            if (naverMap == null || startLatLng == null || endLatLng == null) {
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

        btnToggleJoystick.setOnClickListener(v -> {
            if (joystick.getVisibility() == View.VISIBLE) {
                joystick.setVisibility(View.GONE);
                btnToggleJoystick.setText("조이스틱 켜기");
            } else {
                joystick.setVisibility(View.VISIBLE);
                btnToggleJoystick.setText("조이스틱 끄기");
            }
        });

        joystick.setJoystickListener((xPercent, yPercent) -> {
            if (currentMarker != null) {
                double lat = currentMarker.getPosition().latitude - yPercent * 0.00005;
                double lng = currentMarker.getPosition().longitude + xPercent * 0.00005;
                LatLng newPos = new LatLng(lat, lng);
                currentMarker.setPosition(newPos);
                naverMap.moveCamera(CameraUpdate.scrollTo(newPos));
            }
        });
    }

    private void requestTMapWalkingRoute(LatLng start, LatLng end) {
        new Thread(() -> {
            try {
                String startX = String.valueOf(start.longitude);
                String startY = String.valueOf(start.latitude);
                String endX = String.valueOf(end.longitude);
                String endY = String.valueOf(end.latitude);

                String urlStr = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1" +
                        "&format=json" +
                        "&startX=" + URLEncoder.encode(startX, "UTF-8") +
                        "&startY=" + URLEncoder.encode(startY, "UTF-8") +
                        "&endX=" + URLEncoder.encode(endX, "UTF-8") +
                        "&endY=" + URLEncoder.encode(endY, "UTF-8") +
                        "&startName=" + URLEncoder.encode("출발지", "UTF-8") +
                        "&endName=" + URLEncoder.encode("도착지", "UTF-8") +
                        "&appKey=" + TMAP_API_KEY;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.getJSONArray("features");

                List<LatLng> path = new ArrayList<>();

                for (int i = 0; i < features.length(); i++) {
                    JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                    String type = geometry.getString("type");

                    if (type.equals("LineString")) {
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        for (int j = 0; j < coords.length(); j++) {
                            JSONArray point = coords.getJSONArray(j);
                            double lon = point.getDouble(0);
                            double lat = point.getDouble(1);
                            path.add(new LatLng(lat, lon));
                        }
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> drawPathAndAnimate(path));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(
                        () -> Toast.makeText(this, "TMap 경로 요청 실패", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(MainActivity.this, "도보 이동 완료", Toast.LENGTH_SHORT).show();
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
            LatLng newPos = new LatLng(lat, lng);
            marker.setPosition(newPos);
            naverMap.moveCamera(CameraUpdate.scrollTo(newPos));
        });
        animator.start();
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

    @Override protected void onStart()   { super.onStart(); mapView.onStart(); }
    @Override protected void onResume()  { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()   { super.onPause(); mapView.onPause(); }
    @Override protected void onStop()    { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory()  { super.onLowMemory(); mapView.onLowMemory(); }
}

