package com.atlas.btdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.print.PrinterId;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = MainActivity.class.getSimpleName();
    private Button mBtScan = null;
    private Button mBtCreateBond = null;
    private  Button mBtConnect = null;
    private Button mbtPlayPcm = null;
    private Button mBtPlayMusic = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private Handler mHandler = null;
    private  static final int MSG_SCAN = 0;
    private  static final int MSG_PAIR = 1;
    private  static final int MSG_CONNECT = 2;
    private  static final int MSG_START_PLAYPCM = 3;
    private static final int MSG_STOP_PLAYPCM = 4;
    private  static final int DELAYT_TIMES = 500;
    private BtReceiver mBtReceiver = null;
    private BluetoothDevice mTargetDevice = null;
    private BluetoothProfile mBluetoothA2dpProfile = null;
    private MusicPlayer mMusicPlayer = null;
    private static final String PCM_PATH = "";
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMusicPlayer = new MusicPlayer();
//        playPcm(PCM_PATH);


        mHandler = new Handler(){
            @Override
            public void dispatchMessage(Message msg) {
                    Log.d(TAG, "dispatchMessage, msg.what = " + msg.what);
                    switch (msg.what) {
                        case MSG_SCAN:
                            scanDevice();
                            break;
                        case MSG_PAIR:
                            pairDevice();
                            break;
                        case MSG_CONNECT:
                            connectDevice();
                            break;
                        case MSG_START_PLAYPCM:
                            mMusicPlayer.startPlay(getApplicationContext());
                            break;
                        case MSG_STOP_PLAYPCM:
                            mMusicPlayer.stopPlay();
                            break;
                        default:
                            break;
                    }
            }
        };
        initView();

        //初始化广播接收器
        mBtReceiver = new BtReceiver();
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBtReceiver, intent);
        //初始化蓝牙
        initBt();

        //初始化profileProxy
        initProfileProxy();
        //开始扫描
        scanDevice();
    }

    private  void initView() {
        mBtScan = (Button)findViewById(R.id.bt_scan);
        Log.d(TAG, "mBtScan = " + mBtScan);
        mBtCreateBond = (Button)findViewById(R.id.bt_createbond);
        Log.d(TAG, "mBtCreateBond = " + mBtCreateBond);
        mBtConnect  = (Button)findViewById(R.id.bt_connect);
        Log.d(TAG,"mBtConnect = " + mBtConnect);
        mbtPlayPcm = (Button)findViewById(R.id.bt_playpcm);
        Log.d(TAG, "mbtPlayPcm = " + mbtPlayPcm);
        mBtPlayMusic = (Button)findViewById(R.id.bt_playmusic);
        Log.d(TAG, "mBtPlayMusic = " + mBtPlayMusic);

        mBtScan.setOnClickListener(this);
        mBtCreateBond.setOnClickListener(this);
        mBtConnect.setOnClickListener(this);
        mbtPlayPcm.setOnClickListener(this);
        mBtPlayMusic.setOnClickListener(this);


    }
    private void initBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "mBluetothAdapter = " + mBluetoothAdapter);
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "do't support bt,return");
        }
    }

    private int initProfileProxy() {
        mBluetoothAdapter.getProfileProxy(this,mProfileListener, BluetoothProfile.A2DP);
        return 0;
    }
    @Override
    public void onClick(View view) {
        Log.d(TAG, "view id = " + view.getId());
        switch (view.getId()) {
            case R.id.bt_scan:
                Log.d(TAG, "start bt scan");
                mHandler.sendEmptyMessageDelayed(MSG_SCAN, DELAYT_TIMES);
                break;
            case R.id.bt_connect:
                initProfileProxy();
                Log.d(TAG, "start bt connect");
                mHandler.sendEmptyMessageDelayed(MSG_CONNECT, DELAYT_TIMES);
                break;
            case R.id.bt_playpcm:
                if(mMusicPlayer != null && mMusicPlayer.isMusicPlaying()) {
                    Log.d(TAG, "music is playing");
//                    mHandler.sendEmptyMessage(MSG_START_PLAYPCM);
                    mHandler.sendEmptyMessageDelayed(MSG_STOP_PLAYPCM, DELAYT_TIMES);
                } else {
                    Log.d(TAG, "music play has stopped");
//                    mHandler.sendEmptyMessage(MSG_STOP_PLAYPCM);
                    mHandler.sendEmptyMessageDelayed(MSG_START_PLAYPCM, DELAYT_TIMES);
                }
                break;
            default:
                break;
        }

    }

    private boolean opentBt() {
        if(mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bt is aleady on,return");
            return true;
        } else {
            mBluetoothAdapter.enable();
            mHandler.sendEmptyMessageDelayed(MSG_SCAN, DELAYT_TIMES);
            return false;
        }
    }
    private void scanDevice() {
        if(opentBt()) {
            if (mBluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "scanDevice is already run");
                return;
            }
            boolean ret = mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "scanDevice ret = " + ret);
        } else {
            Log.d(TAG, "bt is not open,wait");
        }

    }

    private void stopScan() {
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "stop scan device");
            return;
        }

        Log.d(TAG, "scanDevice is already stop");
    }

    public class BtReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive intent = " + action);
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice btdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final String address = btdevice.getAddress();
                final String deviceName = btdevice.getName();
                Log.d(TAG, "onReceive found device, deivce address = " + address + ",deviceName = " + deviceName);
                if(isTargetDevice(btdevice)) {
                    stopScan();
                    mHandler.sendEmptyMessageDelayed(MSG_PAIR, DELAYT_TIMES);
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice btdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int preBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                int newBondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                Log.d(TAG, "btdeivice = " + btdevice.getName() + "bond state change, preBondState = " + preBondState
                        + ", newBondState = " + newBondState);
                if(preBondState == BluetoothDevice.BOND_BONDING && newBondState == BluetoothDevice.BOND_BONDED) {
                    //判断一下是否是目标设备
                    if(isTargetDevice(btdevice)) {
                        connectDevice();
                    }

                }
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                BluetoothDevice btdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int preConnectionState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
                int newConnectionState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                Log.d(TAG, "btdevice = " + btdevice.getName() + ", preConnectionState = "
                        + preConnectionState + ", newConnectionState" + newConnectionState);
                if(newConnectionState == BluetoothProfile.STATE_CONNECTED && preConnectionState == BluetoothProfile.STATE_CONNECTING) {
                    Log.d(TAG, "target device connect success");
                    if(mMusicPlayer != null && mMusicPlayer.isMusicPlaying()) {
                        Log.d(TAG, "music is playing");
//                        mHandler.sendEmptyMessage(MSG_START_PLAYPCM);
                        mHandler.sendEmptyMessageDelayed(MSG_STOP_PLAYPCM, DELAYT_TIMES);
                    } else {
                        Log.d(TAG, "music play has stopped");
//                        mHandler.sendEmptyMessage(MSG_STOP_PLAYPCM);
                        mHandler.sendEmptyMessageDelayed(MSG_START_PLAYPCM,DELAYT_TIMES);
                    }
                }
            }
        }
    };

    //可以根据多个限制条件来设定目标设备，例如，信号强度，设备类型，设备名称等。
    //此处我们只用了设备名称来判断
    private boolean isTargetDevice(BluetoothDevice device) {
        if(device.getName() != null && device.getName().equals("S7")) {
            Log.d(TAG, "deivce :" + device.getName() + "is target device");
            mTargetDevice = device;
            return true;
        }
        Log.d(TAG, "deivce :" + device.getName() + "is not target device");
        return false;
    }

    private void pairDevice() {
        Log.d(TAG,"start pair device = " + mTargetDevice);
        if(mTargetDevice.getBondState() != BluetoothDevice.BOND_NONE){
            Log.d(TAG, "targetdevice is already bonded,return");
            return;
        }
        mTargetDevice.createBond();
    }

    private void connectDevice() {
        if(mBluetoothA2dpProfile == null) {
            Log.d(TAG, "don't get a2dp profile,can not run connect");
        } else {
            try {
                //通过反射获取BluetoothA2dp中connect方法
                Method connectMethod = BluetoothA2dp.class.getMethod("connect",
                        BluetoothDevice.class);
                Log.d(TAG, "connectMethod = " + connectMethod);
                connectMethod.invoke(mBluetoothA2dpProfile, mTargetDevice);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "onServiceConnected, profile = " + profile);
            if(profile == BluetoothProfile.A2DP) {
                mBluetoothA2dpProfile = (BluetoothA2dp)proxy;
            }
        }

        @Override
        public  void  onServiceDisconnected(int profile) {
            Log.d(TAG, "onServiceDisconnected, profile = " + profile);
        }
    };


}
