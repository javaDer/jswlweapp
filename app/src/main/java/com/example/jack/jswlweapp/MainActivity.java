package com.example.jack.jswlweapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.jack.jswlweapp.Utils.DemoUtils;
import com.example.jack.jswlweapp.Utils.HttpUtils;
import com.google.gson.Gson;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.map.geolocation.TencentPoi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements DialogInterface.OnClickListener,
        TencentLocationListener {

    private static final String[] NAMES = new String[]{"GEO", "NAME",
            "ADMIN AREA", "POI"};

    private static final int[] LEVELS = new int[]{
            TencentLocationRequest.REQUEST_LEVEL_GEO,
            TencentLocationRequest.REQUEST_LEVEL_NAME,
            TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA,
            TencentLocationRequest.REQUEST_LEVEL_POI};
    private static final int DEFAULT = 2;

    private int mIndex = DEFAULT;
    private int mLevel = LEVELS[DEFAULT];
    private TencentLocationManager mLocationManager;
    private TextView mLocationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String key = DemoUtils.getKey(this);
        // 检查 key 的结构
        if (TextUtils.isEmpty(key)
                || !Pattern.matches("\\w{5}(-\\w{5}){5}", key)) {
            new AlertDialog.Builder(this).setTitle("错误的key")
                    .setMessage("运行前请在manifest中设置正确的key")
                    .setPositiveButton("确定", this).show();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            String[] permissions = {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(permissions, 0);
            }
        }
        setContentView(R.layout.activity_template);
        mLocationStatus = (TextView) findViewById(R.id.status);


        Button settings = ((Button) findViewById(R.id.settings));
        settings.setText("Level");
        settings.setVisibility(View.VISIBLE);

        mLocationManager = TencentLocationManager.getInstance(this);
        // 设置坐标系为 gcj-02, 缺省坐标为 gcj-02, 所以通常不必进行如下调用
        mLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_GCJ02);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出 activity 前一定要停止定位!
        stopLocation(null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mIndex = which;
        mLevel = LEVELS[which];
        dialog.dismiss();
    }

    // ====== view listener

    // 响应点击"停止"
    public void stopLocation(View view) {
        mLocationManager.removeUpdates(this);

        updateLocationStatus("停止定位");
    }

    // 响应点击"开始"
    public void startLocation(View view) {
        // 创建定位请求
        TencentLocationRequest request = TencentLocationRequest.create()
                .setInterval(5 * 1000) // 设置定位周期
                .setAllowGPS(true)  //当为false时，设置不启动GPS。默认启动
                .setQQ("10001")
                .setRequestLevel(mLevel); // 设置定位level

        // 开始定位
        mLocationManager.requestLocationUpdates(request, this, getMainLooper());

        updateLocationStatus("开始定位: " + request + ", 坐标系="
                + DemoUtils.toString(mLocationManager.getCoordinateType()));
    }

    public void clearStatus(View view) {
        mLocationStatus.setText(null);
    }

    public void settings(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(
                NAMES, mIndex, this);
        builder.show();
    }

    // ====== view listener

    // ====== location callback

    @Override
    public void onLocationChanged(TencentLocation location, int error,
                                  String reason) {
        String msg = null;
        if (error == TencentLocation.ERROR_OK) {
            // 定位成功
            msg = toString(location, mLevel);
        } else {
            // 定位失败
            msg = "定位失败: " + reason;
        }
        updateLocationStatus(msg);
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {
        // ignore
    }

    // ====== location callback

    private void updateLocationStatus(String message) {
        mLocationStatus.append(message);
        mLocationStatus.append("\n---\n");
    }

    // ===== util method
    private static String toString(TencentLocation location, int level) {
        StringBuilder sb = new StringBuilder();

        sb.append("latitude=").append(location.getLatitude()).append(",");
        sb.append("longitude=").append(location.getLongitude()).append(",");
        sb.append("altitude=").append(location.getAltitude()).append(",");
        sb.append("accuracy=").append(location.getAccuracy()).append(",");

        switch (level) {
            case TencentLocationRequest.REQUEST_LEVEL_GEO:
                break;

            case TencentLocationRequest.REQUEST_LEVEL_NAME:
                sb.append("name=").append(location.getName()).append(",");
                sb.append("address=").append(location.getAddress()).append(",");
                break;

            case TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA:
            case TencentLocationRequest.REQUEST_LEVEL_POI:
            case 7:
                sb.append("nation=").append(location.getNation()).append(",");
                sb.append("province=").append(location.getProvince()).append(",");
                sb.append("city=").append(location.getCity()).append(",");
                sb.append("district=").append(location.getDistrict()).append(",");
                sb.append("town=").append(location.getTown()).append(",");
                sb.append("village=").append(location.getVillage()).append(",");
                sb.append("street=").append(location.getStreet()).append(",");
                sb.append("streetNo=").append(location.getStreetNo()).append(",");
                sb.append("街道=").append(location.getAddress()).append("");
                String url = "http://192.168.10.8:3000/v1/api/";
                Gson gson = new Gson();
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("latitude", location.getLatitude());
                map.put("longitude", location.getLongitude());
                map.put("address", location.getAddress());
                String locationstr = gson.toJson(map);
                OkHttpClient okHttpClient = new OkHttpClient();//1.定义一个client
                RequestBody body = new FormBody.Builder()
                        .add("parameters", locationstr).build();
                Request request = new Request.Builder().url(url).post(body).build();//2.定义一个request
                Call call = okHttpClient.newCall(request);//3.使用client去请求
                call.enqueue(new Callback() {//4.回调方法
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String result = response.body().string();//5.获得网络数据
                        System.out.println(result);
                    }
                });
                //                if (level == TencentLocationRequest.REQUEST_LEVEL_POI) {
//                    List<TencentPoi> poiList = location.getPoiList();
//                    int size = poiList.size();
//                    for (int i = 0, limit = 3; i < limit && i < size; i++) {
//                        sb.append("\n");
//                        sb.append("poi[" + i + "]=")
//                                .append(toString(poiList.get(i))).append(",");
//                    }
//                }

                break;

            default:
                break;
        }

        return sb.toString();
    }

    private static String toString(TencentPoi poi) {
        StringBuilder sb = new StringBuilder();
        sb.append("name=").append(poi.getName()).append(",");
        sb.append("address=").append(poi.getAddress()).append(",");
        sb.append("catalog=").append(poi.getCatalog()).append(",");
        sb.append("distance=").append(poi.getDistance()).append(",");
        sb.append("latitude=").append(poi.getLatitude()).append(",");
        sb.append("longitude=").append(poi.getLongitude()).append(",");
        return sb.toString();
    }

}
