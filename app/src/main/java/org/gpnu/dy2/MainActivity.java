package org.gpnu.dy2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AMap.OnMapClickListener {
    private static final String TAG = "MainActivity";

    private boolean flag = false;    //标记当前是否在设置航线
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView(savedInstanceState);

        Log.i(TAG,sHA1(this));

    }

    public static String sHA1(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
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

        bt_fly.setEnabled(false);
        bt_fly.setTextColor(getResources().getColor(R.color.buttonInactive));
        bt_reset_plane.setEnabled(false);
        bt_reset_plane.setTextColor(getResources().getColor(R.color.buttonInactive));

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

    private void startFly() {
        if (points.size()==1){
            return;
        }

        // 设置飞行的总时间
        planeMarker.setTotalDuration(second);
        // 开始飞行
        planeMarker.startSmoothMove();

        //定时 每0.3秒查看当前经纬度及高度
        myTask = new MyTask();
        timer.schedule(myTask, 0, 300);

    }

    /**
     * 初始化起飞起点
     */
    private void initPlane() {
        planeMarker = new SmoothMoveMarker(aMap);
        planeMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.plane));
        LatLng drivePoint = points.get(0);
        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
        points.set(pair.first, drivePoint);
        List<LatLng> subList = points.subList(pair.first, points.size());
        planeMarker.setPoints(subList);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_fly://开始飞行事件
                if (points.isEmpty()) {
                    break;
                }

                isFlying = true;

                bt_fly.setEnabled(false);
                bt_fly.setTextColor(getResources().getColor(R.color.buttonInactive));
                bt_reset_plane.setEnabled(true);
                bt_reset_plane.setTextColor(getResources().getColor(R.color.buttonActive));
                bt_reset_route.setEnabled(false);
                bt_reset_route.setTextColor(getResources().getColor(R.color.buttonInactive));
                startFly();

                break;

            case R.id.bt_reset_plane://重置飞机事件
                if (isFlying) {
                    myTask.cancel();

                    //重置飞机
                    planeMarker.destroy();
                    initPlane();

                    tv_info.setText(String.format("经度：%.4f,纬度：%.4f,高度：0米", points.get(0).longitude, points.get(0).latitude));

                    //飞机停止 重置航线可用 、飞机可以起飞
                    bt_reset_route.setEnabled(true);
                    bt_reset_route.setTextColor(getResources().getColor(R.color.buttonActive));
                    bt_fly.setEnabled(true);
                    bt_fly.setTextColor(getResources().getColor(R.color.buttonActive));
                }
                break;
            case R.id.bt_reset_route://重置航线事件

                if (flag) {  //表示确认航线

                    if (points.isEmpty()){
                        Toast.makeText(this,"请选择航线！",Toast.LENGTH_SHORT).show();
                        break;
                    }

                    bt_reset_route.setText("重置航线");

                    aMap.setOnMapClickListener(null);

                    initPlane();
                    showPolyline();

                    bt_fly.setEnabled(true);
                    bt_fly.setTextColor(getResources().getColor(R.color.buttonActive));
                    flag = false;
                } else {
                    //清除地图上的Marker和Line
                    points.clear();
                    aMap.clear();

                    bt_reset_route.setText("确认");

                    bt_fly.setEnabled(false);
                    bt_fly.setTextColor(getResources().getColor(R.color.buttonInactive));
                    bt_reset_plane.setEnabled(false);
                    bt_reset_plane.setTextColor(getResources().getColor(R.color.buttonInactive));

                    aMap.setOnMapClickListener(this);
                    flag = true;
                }

                break;
        }
    }

    /**
     * 标记点击的位置
     *
     * @param latLng
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onMapClick(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setBackground(getResources().getDrawable(R.drawable.textview_circle));
        textView.setText(String.valueOf(points.size()));
        markerOptions.icon(BitmapDescriptorFactory.fromView(textView));
        markerOptions.title(String.valueOf(points.size()));
        markerOptions.anchor(0.5f, 0.5f);//icon居中
        aMap.addMarker(markerOptions);

        points.add(latLng);

    }

    private Polyline showPolyline() {
        return aMap.addPolyline(new PolylineOptions().
                addAll(points).width(6).color(Color.argb(255, 1, 1, 1)));
    }

    class MyTask extends TimerTask {
        int mill = second * 1000;

        @Override
        public void run() {
            LatLng latLng = planeMarker.getPosition();
            Log.i("distance", planeMarker.getPosition().toString());
            tv_info.setText(String.format("经度：%.4f,纬度：%.4f,高度：%.2f米", latLng.longitude, latLng.latitude, Math.random() * 1000));
            mill = mill - 300;
            if (mill < 0) {  //飞行结束
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

