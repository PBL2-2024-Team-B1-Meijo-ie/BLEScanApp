package jp.ac.meijo_u.pbl2.blescanapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconConsumer;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }

    // BeaconConsumer インターフェースのメソッドを実装する
    @Override
    public void onBeaconServiceConnect() {
        // ビーコン検出のための処理をここに記述します
        Log.d("Beacon", "Beacon service connected");
    }

    @Override
    public void unbindService(ServiceConnection connection) {
        super.unbindService(connection);
    }
}

