package com.android.systemui.smartnet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.MiuiCoreSettingsPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.RILConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SmartNet {
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private SmartNetCoreDualSim mCoreDualSim;
    BroadcastReceiver mIntent;
    private TelephonyManager mTelephonyManager;
    private static boolean CallState = false;
    private static int SmartNetLastState = 0;
    private static int lastStateMobileData = 0;
    private static int lastStateWiFiData = 0;
    private static int lastStateNetworkTypeSIM1 = 0;
    private static int lastStateNetworkTypeSIM2 = 0;
    private static boolean timerDone = false;
    private static boolean timerStart = false;
    private static CountDownTimer cTimer = null;
    private static boolean chargingState = false;



    public SmartNet(Context context){
        initialization(context);
        mIntent = new Receiver();
        registerSmartNetReceiver(context);
    }

    public SmartNet(Context context,boolean key){
        initialization(context);
        if (key) {
            mCoreDualSim = SmartNetCoreDualSim.getInstance(mContext);
        }

    }

    private void initialization(Context context){
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ConnectivityManager defaultConnectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (defaultConnectivityManager != null){
            mConnectivityManager = defaultConnectivityManager;
        }
    }

    public void handleMobileData() {
        int networkType;
        mCoreDualSim = SmartNetCoreDualSim.getInstance(mContext);
        mTelephonyManager = new TelephonyManager(mContext);
        if (MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_activity")!=0){

            if (getStateWiFiData() && getConnectedWiFiData()){ setPreferredNetworkTypeWiFiOn(); return;}
            if (mConnectivityManager != null){
                if (mConnectivityManager.getMobileDataEnabled()){
                    //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_on");
                    networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_on"));
                    if (networkType >= 0){
                        setSimPreferredNetworkType(networkType);
                    }
                } else {
                    //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_off");
                    networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_off"));
                    if (networkType >= 0) {
                        setSimPreferredNetworkType(networkType);
                    }
                }
            }
        }
    }

    private void setSimPreferredNetworkType(int networkType) {
        int SimCard;
        int[] SIMid = mCoreDualSim.getSubscriptionId();

        if (mCoreDualSim.isDualSIM()){
            SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
            Log.d("SmartNet2.0","setSimPreferredNetworkType(I)V: SIMid[0]="+SIMid[0]+" SIMid[1]="+SIMid[1]+ " isSIM2Ready()="+mCoreDualSim.isSIM2Ready() + " Network Type=" + networkType + " SIMCard="+SimCard);
            switch (SimCard){
                case 1:
                    setPreferredNetworkType(SIMid[0],networkType);
                    break;
                case 2:
                    setPreferredNetworkType(SIMid[1],networkType);
                    break;
                case 3:
                    setPreferredNetworkType(SIMid[0],networkType);
                    if (mCoreDualSim.isSIM2Ready()) {
                        setPreferredNetworkType(SIMid[1], networkType);
                    }
                    break;
                default:
                    setPreferredNetworkType(SIMid[0],networkType);
            }
        } else {
            setPreferredNetworkType(SIMid[0],networkType);
        }
    }

    private void setPreferredNetworkType(int SIMid, int networkType){
      ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (mITelephony != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d("SmartNet2.0", "setPreferredNetworkType(II)V: [SubId]: " + SIMid + " Network Type: " + networkType);
                    mITelephony.setPreferredNetworkType(SIMid,networkType);
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                   // SubscriptionManager mSubscriptionManager = new SubscriptionManager(mContext);
                   // mSubscriptionManager.setDefaultDataSubId(SubscriptionManager.getSubId(SIMid)[0]);
                    Log.d("SmartNet2.0", "API 22 setPreferredNetworkType(II)V: [SubId]: " + SIMid + " Network Type: " + networkType);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //    SubscriptionManager mSubscriptionManager = new SubscriptionManager(mContext);
                //    mSubscriptionManager.setDefaultDataSubId(SubscriptionManager.getSubId(SIMid)[0]);
                    Log.d("SmartNet2.0", "API 21 setPreferredNetworkType(II)V: [SubId]: " + SIMid + " Network Type: " + networkType);
                }
                mITelephony.setPreferredNetworkType(networkType,0);
            } catch (RemoteException e) {

            }
        }
    }

    private void setPreferredNetworkType(int[] SIMid, int networkType){
        int SimCard;
        SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
        ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (mITelephony != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    switch (SimCard){
                        case 1:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], networkType);
                            }
                            break;
                        case 2:
                            if (mCoreDualSim.isSIM2Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[1], networkType);
                            }
                            break;
                        case 3:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], networkType);
                            }
                            if (mCoreDualSim.isSIM2Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[1], networkType);
                            }
                            break;
                    }
                } else {
                    mITelephony.setPreferredNetworkType(networkType, 0);
                }
            } catch (RemoteException e) {

            }
        }
    }

    public void setCallPreferredNetworkType(boolean CallState){
        int networkType;
        int WiFiDataTransfer;
        int MobileDataTransfer;
        int[] SIMid = mCoreDualSim.getSubscriptionId();
        WiFiDataTransfer = MiuiCoreSettingsPreference.getKeyParam(mContext,"smartnet_wifi_data_transfer");
        MobileDataTransfer = MiuiCoreSettingsPreference.getKeyParam(mContext,"smartnet_mobile_data_transfer");
        if (CallState) {
            saveCurrentPrefferedNetworkType();
            //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext, "call_mobiledata");
            networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"call_mobiledata"));
                //Settings.System.putInt(mContext.getContentResolver(), "smartnet_mobiledata_laststate", (getStateMobileData() ? 1 : 0));
                //Settings.System.putInt(mContext.getContentResolver(), "smartnet_wifidata_laststate", (getStateWiFiData() ? 1 : 0));
                lastStateMobileData = getStateMobileData() ? 1 : 0;
                lastStateWiFiData = getStateWiFiData() ? 1 : 0;
                if (WiFiDataTransfer != 0) {
                    setStateWiFiData(!CallState);
                }
                if (MobileDataTransfer != 0) {
                    setStateMobileData(!CallState);
                }
                Log.d("SmartNet2.0", "setCallPreferredNetworkType(Z)V: Network Type: " + networkType + " lastStateMobileData=" + lastStateMobileData + " lastStateWiFiData=" + lastStateWiFiData + " WiFiDataTransfer=" + WiFiDataTransfer + " MobileDataTransfer=" + MobileDataTransfer+ " lastStateNetworkTypeSIM1=" +lastStateNetworkTypeSIM1+ " lastStateNetworkTypeSIM2=" +lastStateNetworkTypeSIM2);
        }else{
            //int lastStateMobileData = MiuiCoreSettingsPreference.getKeyParam(mContext, "smartnet_mobiledata_laststate");
            //int lastStateWiFiData = MiuiCoreSettingsPreference.getKeyParam(mContext, "smartnet_wifidata_laststate");
            if (lastStateWiFiData != 0) {
                //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_WiFiOn");
                networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_WiFiOn"));
            }else {
                if (lastStateMobileData != 0) {
                    //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_on");
                    networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_on"));
                } else {
                    restoreCurrentPrefferedNetworkType();
                    return;
                }
            }
            if (MobileDataTransfer != 0) {
                if (lastStateMobileData != 0) {
                    setStateMobileData(!CallState);
                }
            }
            if (WiFiDataTransfer != 0) {
                if (lastStateWiFiData != 0) {
                    setStateWiFiData(!CallState);
                }
            }
            Log.d("SmartNet2.0", "setCallPreferredNetworkType(Z)V: Network Type: " + networkType +" lastStateMobileData="+lastStateMobileData+" lastStateWiFiData="+lastStateWiFiData+" WiFiDataTransfer="+WiFiDataTransfer+" MobileDataTransfer="+MobileDataTransfer+ " lastStateNetworkTypeSIM1=" +lastStateNetworkTypeSIM1+ " lastStateNetworkTypeSIM2=" +lastStateNetworkTypeSIM2);
        }
        if (networkType >= 0) {
            setPreferredNetworkType(SIMid,networkType);
        }
    }

    private void saveCurrentPrefferedNetworkType(){
        int SimCard;
        int[] SIMid = mCoreDualSim.getSubscriptionId();
        SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
        ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (mITelephony != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    switch (SimCard){
                        case 1:
                            if (mCoreDualSim.isSIM1Ready()){
                                lastStateNetworkTypeSIM1=mITelephony.getPreferredNetworkType(SIMid[0]);
                            }
                            break;
                        case 2:
                            if (mCoreDualSim.isSIM2Ready()){
                                lastStateNetworkTypeSIM2=mITelephony.getPreferredNetworkType(SIMid[1]);
                            }
                            break;
                        case 3:
                            if (mCoreDualSim.isSIM1Ready()){
                                lastStateNetworkTypeSIM1=mITelephony.getPreferredNetworkType(SIMid[0]);
                            }
                            if (mCoreDualSim.isSIM2Ready()){
                                lastStateNetworkTypeSIM2=mITelephony.getPreferredNetworkType(SIMid[1]);
                            }
                            break;
                    }
                } else {
                    lastStateNetworkTypeSIM1=mITelephony.getPreferredNetworkType(0);
                }

            } catch (RemoteException e) {

            }
        }
    }

    private void restoreCurrentPrefferedNetworkType(){
        int SimCard;
        int[] SIMid = mCoreDualSim.getSubscriptionId();
        SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
        ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (mITelephony != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    switch (SimCard){
                        case 1:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], lastStateNetworkTypeSIM1);
                            }
                            break;
                        case 2:
                            if (mCoreDualSim.isSIM2Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[1], lastStateNetworkTypeSIM2);
                            }
                            break;
                        case 3:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], lastStateNetworkTypeSIM1);
                            }
                            if (mCoreDualSim.isSIM2Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[1], lastStateNetworkTypeSIM2);
                            }
                            break;
                    }
                } else {
                    mITelephony.setPreferredNetworkType(lastStateNetworkTypeSIM1, 0);
                }
            } catch (RemoteException e) {

            }
        }
    }




    public void setPreferredNetworkTypeWiFiOn() {
        int networkType;
        int[] SIMid = mCoreDualSim.getSubscriptionId();
        //networkType = MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_WiFiOn");
        networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_WiFiOn"));
        switch (networkType) {
            case -1:
                return;
            case 0:
                networkType = 1;
                break;
        }
        setPreferredNetworkType(SIMid, networkType);
    }


    private boolean getStateMobileData(){
        boolean MobileDataState = false;
        try{
            TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method getMobileDataEnabledMethod = mTelephonyManager.getClass().getDeclaredMethod("getDataEnabled");
            if (getMobileDataEnabledMethod != null){
                MobileDataState =(Boolean) getMobileDataEnabledMethod.invoke(mTelephonyManager);
            }
        }catch (Exception ex){

        }
        return  MobileDataState;
    }

    private void setStateMobileData(boolean state){
        try{
            TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method setMobileDataEnabledMethod = mTelephonyManager.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (setMobileDataEnabledMethod != null){
                setMobileDataEnabledMethod.invoke(mTelephonyManager,state);
            }
        }catch (Exception ex){
        }
    }

    private boolean getStateWiFiData(){
        WifiManager wifiManager;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        } else {
            wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        return wifiManager.isWifiEnabled();
    }

    private void setStateWiFiData(boolean value){
        WifiManager wifiManager;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }else{
            wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        wifiManager.setWifiEnabled(value);
    }

    private boolean getConnectedWiFiData(){
        ConnectivityManager connectivityManager =(ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    public void registerSmartNetReceiver(Context context) {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.intent.action.ANY_DATA_STATE");
        mIntentFilter.addAction("my.settings.CHANGE_SMART_MOBILE_NETWORK");
        mIntentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        mIntentFilter.addAction("android.intent.action.PHONE_STATE");
        mIntentFilter.addAction("android.intent.action.SCREEN_ON");
        mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(mIntent,mIntentFilter);
    }

    private int getRILConstants(String nameNetworkType){
        String value;
        int result;
        value = "NETWORK_MODE_".concat(nameNetworkType);
        try {
            Field field = Class.forName("com.android.internal.telephony.RILConstants").getField(value);
            result = field.getInt(null);
        } catch (NoSuchFieldException e) {
            result = 0;
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            result = 0;
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            result = 0;
            e.printStackTrace();
        }
        return result;
    }

    public void CallIntentAction(Intent intent){
        String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (((phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) ||
                (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))) &&  (!CallState)) {
            SmartNetLastState = MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_activity");
            if (SmartNetLastState != 0)  {
                Settings.System.putInt(mContext.getContentResolver(), "mobiledata_activity", 0);
                setCallPreferredNetworkType(true);
            }
            CallState = true;
        }

        if ((phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) && (CallState)) {
            if (SmartNetLastState != 0) {
                Settings.System.putInt(mContext.getContentResolver(), "mobiledata_activity", SmartNetLastState);
                setCallPreferredNetworkType(false);
            }
            CallState = false;
        }
        Log.d("SmartNet2.0","CallState="+CallState + " SmartNetLastState="+SmartNetLastState);
    }

    private void setTimer(int value){
        cTimer = new CountDownTimer(value,1000){

            @Override
            public void onTick(long l) {
                Log.d("SmartNet2.0","setTimer(I)V: Switching seconds remaining: " + l / 1000);
            }

            @Override
            public void onFinish() {
                timerDone = true;
                timerStart = false;
                Log.d("SmartNet2.0","setTimer(I)V: Switch network type");
            }
        };
    }


    private void TimerIntentAction(boolean state) {
        int timeToCompletion = MiuiCoreSettingsPreference.getKeyParam(mContext, "smartnet_timer_value");
        if (timeToCompletion > 0) {
            if (state) {
                if ((timerDone) && (cTimer != null)) {
                    //Restore NetworkType
                    //restoreCurrentPrefferedNetworkType();
                    cTimer = null;
                    timerDone = false;
                    Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Restore Network Type");
                } else {
                    //Cancel Timer
                    if ((timerStart) && (cTimer != null)) {
                        cTimer.cancel();
                        cTimer = null;
                        timerStart = false;
                        Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Kill Timer");
                    }
                }
            } else {
                if ((!timerStart) && (!chargingState)) {
                    //Save Network Type Start Timer
                    //saveCurrentPrefferedNetworkType();

                    setTimer(timeToCompletion);
                    cTimer.start();
                    timerStart = true;
                    Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Start Timer");

                }
            }

        }
    }

    private void BatteryCharging(Intent mIntent){
        int status = mIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        chargingState = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }


    class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            SmartNet mSmartNet = SmartNet.this;
            String mIntent = intent.getAction();
            //Log.d("SmartNet2.0","Intent="+mIntent);
            if("android.intent.action.PHONE_STATE".equals(mIntent)){
                CallIntentAction(intent);
            }
            if (("android.intent.action.ANY_DATA_STATE".equals(mIntent)) ||
                    ("my.settings.CHANGE_SMART_MOBILE_NETWORK".equals(mIntent)) ||
                    ("android.intent.action.BOOT_COMPLETED".equals(mIntent))) {
                mSmartNet.handleMobileData();
            }
            if ("android.intent.action.SCREEN_ON".equals(mIntent)) {
                TimerIntentAction(true);
            }
            if ("android.intent.action.SCREEN_OFF".equals(mIntent)) {
                TimerIntentAction(false);
            }

            if ("android.intent.action.BATTERY_CHANGED".equals(mIntent)){
                BatteryCharging(intent);
            }

        }
    }

}
