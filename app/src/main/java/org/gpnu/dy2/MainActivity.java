package org.gpnu.dy2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private boolean isFlying = false;

    private Button bt_fly;
    private Button bt_reset_plane;
    private Button bt_reset_route;

    private MapView mMapView = null;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView(savedInstanceState);



    }

    private void initView(Bundle savedInstanceState){
        bt_fly = findViewById(R.id.bt_fly);
        bt_fly.setOnClickListener(this);
        bt_reset_plane = findViewById(R.id.bt_reset_plane);
        bt_reset_plane.setOnClickListener(this);
        bt_reset_route = findViewById(R.id.bt_reset_route);
        bt_reset_route.setOnClickListener(this);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        AMap aMap = mMapView.getMap();

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_fly://开始飞行事件
                if (isFlying){
                    bt_fly.setClickable(false);
                    break;
                }

                isFlying = true;
                break;

            case R.id.bt_reset_plane://重置飞机事件

            case R.id.bt_reset_route://重置航线事件

                break;
        }
    }
}
