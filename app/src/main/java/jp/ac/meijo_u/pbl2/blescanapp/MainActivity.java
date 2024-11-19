package jp.ac.meijo_u.pbl2.blescanapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import org.altbeacon.beacon.Identifier;

import org.altbeacon.beacon.*;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements RangeNotifier, MonitorNotifier {

    private Region mRegion;
    private BeaconManager beaconManager;
    private TextView beaconInfoTextView;
    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String TAG = "BeaconDetection";
    private ActivityResultLauncher<String[]> permissionResult;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconInfoTextView = findViewById(R.id.beaconInfoTextView);

        // BeaconManagerの初期化
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));

        // UUIDの指定
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
        Log.d(TAG, "Enter Region " + (region != null ? region.getUniqueId() : "unknown"));
        runOnUiThread(() -> beaconInfoTextView.setText("Enter Region: " + (region != null ? region.getUniqueId() : "unknown")));
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "Exit Region " + (region != null ? region.getUniqueId() : "unknown"));
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "beacons.size " + (beacons != null ? beacons.size() : 0));
        if (beacons != null) {
            for (Beacon beacon : beacons) {
                String beaconDetails = "UUID: " + beacon.getId1() +
                        ", Major: " + beacon.getId2() ;
                Log.d(TAG, beaconDetails);
                runOnUiThread(() -> beaconInfoTextView.setText(beaconDetails));
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