package org.gpnu.dy2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AMap.OnMapClickListener {
    private boolean isFlying = false;

    private Button bt_fly;
    private Button bt_reset_plane;
    private Button bt_reset_route;

    private TextView tv_info;

    private MapView mMapView = null;
    private AMap aMap = null;

    private SmoothMoveMarker planeMarker;

    private Timer timer = new Timer();

    private MyTask myTask;

    private final int second = 10;

    private List<LatLng> points = new ArrayList<>();
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView(savedInstanceState);

    }

    private void initView(Bundle savedInstanceState) {
        bt_fly = findViewById(R.id.bt_fly);
        bt_fly.setOnClickListener(this);
        bt_reset_plane = findViewById(R.id.bt_reset_plane);
        bt_reset_plane.setOnClickListener(this);
        bt_reset_route = findViewById(R.id.bt_reset_route);
        bt_reset_route.setOnClickListener(this);

        tv_info = findViewById(R.id.tv_info);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        aMap = mMapView.getMap();
        aMap.setOnMapClickListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();

    }

    private void flying() {
        if (planeMarker!=null){
            planeMarker.destroy();
        }
        LatLngBounds bounds = new LatLngBounds(points.get(0), points.get(points.size() - 2));
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

        setPlanePlace();

        // 设置滑动的总时间
        planeMarker.setTotalDuration(second);
        // 开始滑动
        planeMarker.startSmoothMove();

        myTask = new MyTask();

        //定时查看当前经纬度及高度
        timer.schedule(myTask, 0, 300);

        planeMarker.setMoveListener(new SmoothMoveMarker.MoveListener(){
            @Override
            public void move(double v) {
//                Log.i("distance",planeMarker.getPosition().toString());
            }
        });

    }

    /**
     * 飞机起点
     */
    private void setPlanePlace() {
        planeMarker = new SmoothMoveMarker(aMap);
        // 设置滑动的图标
        planeMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.plane));
        LatLng drivePoint = points.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
        points.set(pair.first, drivePoint);
        List<LatLng> subList = points.subList(pair.first, points.size());

        // 设置滑动的轨迹左边点
        planeMarker.setPoints(subList);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_fly://开始飞行事件
                aMap.setOnMapClickListener(null);

                showPolyline();

                bt_fly.setEnabled(false);
                bt_fly.setTextColor(getResources().getColor(R.color.buttonInactive));

                bt_reset_route.setEnabled(false);
                bt_reset_route.setTextColor(getResources().getColor(R.color.buttonInactive));
                flying();

                isFlying = true;
                break;

            case R.id.bt_reset_plane://重置飞机事件
                myTask.cancel();
                isFlying = false;

                //停止飞行
//                planeMarker.setVisible(false);
                planeMarker.destroy();

                setPlanePlace();    //回到起点

                //飞机停止 重置航线可用 、飞机可以起飞
                bt_reset_route.setEnabled(true);
                bt_reset_route.setTextColor(getResources().getColor(R.color.buttonActive));
                bt_fly.setEnabled(true);
                bt_fly.setTextColor(getResources().getColor(R.color.buttonActive));

                break;
            case R.id.bt_reset_route://重置航线事件
                //清除地图上的Marker和Line
                points.clear();
                aMap.clear();

                points.clear();
                isFlying = false;
                aMap.setOnMapClickListener(this);
                break;
        }
    }

    /**
     * 标记点击的位置
     * @param latLng
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onMapClick(LatLng latLng) {

        Log.i("distance",latLng.toString());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setBackground(getResources().getDrawable(R.drawable.textview_circle));
        textView.setText(String.valueOf(points.size()));
        markerOptions.icon(BitmapDescriptorFactory.fromView(textView));
        markerOptions.title(String.valueOf(points.size()));
        markerOptions.anchor(0.5f, 0.5f);
        points.add(latLng);

        aMap.addMarker(markerOptions);

        if (points.size() > 0) {
            bt_fly.setEnabled(true);
            bt_fly.setTextColor(getResources().getColor(R.color.buttonActive));
        }
    }

    private Polyline showPolyline() {
        return aMap.addPolyline(new PolylineOptions().
                addAll(points).width(6).color(Color.argb(255, 1, 1, 1)));
    }

    class MyTask extends TimerTask{
        int mill = second*1000;
        @Override
        public void run() {
            LatLng latLng = planeMarker.getPosition();
            Log.i("distance",planeMarker.getPosition().toString());
            tv_info.setText(String.format("经度：%.4f,纬度：%.4f,高度：0", latLng.longitude,latLng.latitude));
            mill = mill - 300;
            if (mill < 0){  //飞行结束
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bt_reset_route.setEnabled(true);
                        bt_reset_route.setTextColor(getResources().getColor(R.color.buttonActive));
                    }
                });
                this.cancel();
            }
        }
    }

}

