package jp.techacademy.yumie.minakami.testble_baseonalt;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.Bundle;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

// AltBeacon関連のライブラリをimport
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

import static android.R.attr.targetSdkVersion;


public class MainActivity extends AppCompatActivity  implements BeaconConsumer{

    // iBeaconフォーマットを指定（AltBeaconだけでなく、全てのiBeaconを対象とする）
    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // リージョン基本名称
    public final static String BASE_REGION_NAME = "my_region";

    // Beaconマネージャー
    private BeaconManager beaconManager;
    // Bluetoothアダプター
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Bluetooth利用状況確認フラグ
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            final PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            int targetSdkVersion = info.applicationInfo.targetSdkVersion;
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if(targetSdkVersion >= Build.VERSION_CODES.M ) {
                // 許可されていない場合
                if( this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                    // ユーザーに許可を求める
                    requestPermissions( new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION );
                }
            } else {
                if(PermissionChecker.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                }
            }
        }

//        // Android6以降のパーミッション確認
//        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
//        {
//            // 許可されていない場合
//            if( this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
//            {
//                // ユーザーに許可を求める
//                requestPermissions( new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION );
//            }
//        }

        // ビーコン・マネージャーの初期化
        beaconManager = BeaconManager.getInstanceForApplication( this );
        beaconManager.getBeaconParsers().add( new BeaconParser().setBeaconLayout( IBEACON_FORMAT ) );
        beaconManager.setForegroundBetweenScanPeriod( 1000 );
        beaconManager.setBackgroundBetweenScanPeriod( 1000 );

        // アダプターを取得
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Bluetoothアダプターが存在しない場合
        if( mBluetoothAdapter == null )
        {
            // エラーを表示
            Toast.makeText( this, "Bluetooth is not available", Toast.LENGTH_LONG ).show();
            finish();
            return;
        }
        // Bluetoothアダプターが無効な場合
        if( !mBluetoothAdapter.isEnabled() )
        {
            // ユーザーに許可を求める
            Intent enableIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( enableIntent, REQUEST_ENABLE_BT );
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // サービスの開始
        Log.d( "javalog", "サービスの開始");
        beaconManager.bind(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        stopBeacon();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        stopBeacon();
    }

    /* サービス停止メソッド */
    private void stopBeacon()
    {
        // サービスの停止
        Log.d( "javalog", "サービスの停止");

        // 通知イベントを一旦削除
        beaconManager.removeAllRangeNotifiers();
        beaconManager.removeAllMonitorNotifiers();

        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect()
    {
        // 全てのビーコンを検出するためのリージョンを作成
        Region mRegion = new Region( BASE_REGION_NAME, null, null, null );

        // 検索結果通知イベントの作成
        beaconManager.addMonitorNotifier
                (
                        new MonitorNotifier()
                        {
                            // 領域侵入通知
                            @Override
                            public void didEnterRegion( Region region )
                            {
                                try
                                {
                                    // レンジングの開始
                                    Log.d( "javalog", "領域侵入 : " + region.getUniqueId() );
                                    beaconManager.startRangingBeaconsInRegion( region );
                                } catch( RemoteException e ) {
                                    // 例外が発生した場合
                                    e.printStackTrace();
                                }
                            }

                            // 領域退出
                            @Override
                            public void didExitRegion( Region region )
                            {
                                try
                                {
                                    // レンジングの停止
                                    Log.d( "javalog", "領域退出 : " + region.getUniqueId());
                                    beaconManager.stopRangingBeaconsInRegion( region );
                                } catch( RemoteException e ) {
                                    // 例外が発生した場合
                                    e.printStackTrace();
                                }
                            }

                            // 領域に対する状態が変化
                            // この処理は確認用であり、通常は必要とされない。
                            @Override
                            public void didDetermineStateForRegion( int i, Region region )
                            {
                                Log.d( "javalog", "領域に対する状態が変化 : " + region.getUniqueId() );
                            }
                        }
                );

        // ビーコン詳細情報の通知を受け取るためのイベント定義
        // レンジングが開始され、初めて有効となる。
        beaconManager.addRangeNotifier
                (
                        new RangeNotifier()
                        {
                            @Override
                            public void didRangeBeaconsInRegion( Collection beacons, Region region )
                            {
                                // 検出したビーコンの情報を全部みる
                                //double lastDistance = Double.MAX_VALUE;
                                //Beacon nearBeacon = null;

                                for( Object beacon2 : beacons )
                                {
                                    Beacon beacon = (Beacon)beacon2;

                                    Log.d( "javalog", "Region:" + region.getUniqueId() + ", UUID:" + beacon.getId1() + ", major:" + beacon.getId2() + ", minor:" + beacon.getId3() + ", Distance:" + beacon.getDistance() + ",RSSI" + beacon.getRssi() + ", TxPower" + beacon.getTxPower() );
                                }
                            }
                        }
                );

        // デバイス検索を開始
        try
        {
            beaconManager.startMonitoringBeaconsInRegion( mRegion );
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }
}
