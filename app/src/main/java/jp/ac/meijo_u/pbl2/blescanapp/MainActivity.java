package jp.ac.meijo_u.pbl2.blescanapp;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.altbeacon.beacon.Identifier;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements RangeNotifier, MonitorNotifier {

    private Region mRegion;
    private BeaconManager beaconManager;
    private TextView beaconInfoTextView;
    private TextView gettingOnTextView;
    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String TAG = "BeaconDetection";
    private ActivityResultLauncher<String[]> permissionResult;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconInfoTextView = findViewById(R.id.beaconInfoTextView);
        gettingOnTextView = findViewById(R.id.gettingOnTextView);

        // BeaconManagerの初期化
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));

        // UUIDの指定
        //ビーコンごとにUUIDを変えて複数指定をすることを考えたが，他チームとビーコンを共有しているため省略した．
        mRegion = new Region("iBeacon", Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"), null, null);

        // パーミッションリクエストのセットアップ
        permissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    Boolean bluetoothScanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                    Boolean bluetoothConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);

                    if (fineLocationGranted && coarseLocationGranted &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (bluetoothScanGranted && bluetoothConnectGranted))) {
                        startBeaconMonitoring();
                    } else {
                        Log.e(TAG, "Required permission not granted!");
                    }
                }
        );

        requestPermissionsIfNeeded();
    }

    private void requestPermissionsIfNeeded() {
        // Android 12以上とそれ以下でリクエストするパーミッションを分ける
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionResult.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            });
        } else {
            permissionResult.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void startBeaconMonitoring() {
            beaconManager.addMonitorNotifier(this);
            beaconManager.addRangeNotifier(this);
            beaconManager.startMonitoring(mRegion);
            beaconManager.startRangingBeacons(mRegion);
    }

    @Override
    public void didEnterRegion(Region region) {
        //検知したビーコン (バスに乗車したかどうか) をサーバに送信
        // HttpPostTaskクラスを利用
        String serverUrl = "https://web-client-api.onrender.com/api/blebeecon"; // サーバのエンドポイント
        String dataToSend = "{\"message\": \"get on " + region.getUniqueId() + " \"}"; // 送信するデータ

        new HttpPostTask(MainActivity.this).execute(serverUrl, dataToSend);

        Log.d(TAG, "Enter Region " + (region != null ? region.getUniqueId() : "unknown"));
        runOnUiThread(() -> beaconInfoTextView.setText("Enter Region: " + (region != null ? region.getUniqueId() : "unknown")));
        runOnUiThread(() -> gettingOnTextView.setText("乗車中"));


    }

    @Override
    public void didExitRegion(Region region) {
        // HttpPostTaskクラスを利用
        String serverUrl = "https://web-client-api.onrender.com/api/blebeecon"; // サーバのエンドポイント
        String dataToSend = "{\"message\": \"get off " + region.getUniqueId() + " \"}"; // 送信するデータ

        new HttpPostTask(MainActivity.this).execute(serverUrl, dataToSend);

        Log.d(TAG, "Exit Region " + (region != null ? region.getUniqueId() : "unknown"));
        runOnUiThread(() -> beaconInfoTextView.setText("バスなし"));
        runOnUiThread(() -> gettingOnTextView.setText("乗車してないよ"));
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "beacons.size " + (beacons != null ? beacons.size() : 0));
        if (beacons != null) {
            for (Beacon beacon : beacons) {
                String beaconDetails = "UUID: " + beacon.getId1();
                Log.d(TAG, beaconDetails);
                runOnUiThread(() -> beaconInfoTextView.setText(beaconDetails));
                runOnUiThread(() -> gettingOnTextView.setText("乗車中"));
            }
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG, "Determine State: " + state);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            beaconManager.stopMonitoring(mRegion);
            beaconManager.stopRangingBeacons(mRegion);
    }
}