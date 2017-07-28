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
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
    private static CountDownTimer cTimerSleepOn = null;
    private static CountDownTimer cTimerSleepOnAction = null;
    private static boolean chargingState = false;
    private static Map networkTypeHash = new HashMap();
    private static int timerSleepOn;
    private static int timeToCompletion;



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
            if ((timerDone) || (CallState)) {return;} //CallState add
            if (getStateWiFiData() && getConnectedWiFiData()){ setPreferredNetworkTypeWiFiOn(); return;}
            if (mConnectivityManager != null){
                if (mConnectivityManager.getMobileDataEnabled()){
                    networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_on"));
                    //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_on"));
                    if (networkType >= 0){
                        setSimPreferredNetworkType(networkType);
                    }
                } else {
                    networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_off"));
                    //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_off"));
                    if (networkType >= 0) {
                        setSimPreferredNetworkType(networkType);
                    }
                }
            }
        }
    }

    private void setSimPreferredNetworkType(int networkType) {
        int SimCard;
        int[] SIMid = mCoreDualSim.getSubId(mContext);

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
        if (SimCard == 0) {SimCard = 1;}
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
                        default:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], networkType);
                            }
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
        //int[] SIMid = mCoreDualSim.getSubscriptionId();
        int[] SIMid = mCoreDualSim.getSubId(mContext);
        WiFiDataTransfer = MiuiCoreSettingsPreference.getKeyParam(mContext,"smartnet_wifi_data_transfer");
        MobileDataTransfer = MiuiCoreSettingsPreference.getKeyParam(mContext,"smartnet_mobile_data_transfer");
        if (CallState) {
            saveCurrentPrefferedNetworkType();
            networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext, "call_mobiledata"));
            //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"call_mobiledata"));
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
                networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_WiFiOn"));
                //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_WiFiOn"));
            }else {
                if (lastStateMobileData != 0) {
                    networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_on"));
                    //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_on"));
                } else {
                    restoreCurrentPrefferedNetworkType();
                    return;
                }
            }
            if (WiFiDataTransfer != 0) {
                if (lastStateWiFiData != 0) {
                    setStateWiFiData(!CallState);
                }
            }

            if (MobileDataTransfer != 0) {
                if (lastStateMobileData != 0) {
                    setStateMobileData(!CallState);
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
        //int[] SIMid = mCoreDualSim.getSubscriptionId();
        int[] SIMid = mCoreDualSim.getSubId(mContext);
        SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
        if (SimCard == 0) {SimCard = 1;}
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
                        default:
                            if (mCoreDualSim.isSIM1Ready()){
                                lastStateNetworkTypeSIM1=mITelephony.getPreferredNetworkType(SIMid[0]);
                            }
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
        //int[] SIMid = mCoreDualSim.getSubscriptionId();
        int[] SIMid = mCoreDualSim.getSubId(mContext);
        SimCard  = MiuiCoreSettingsPreference.getKeyParam(mContext,"mobiledata_SIM_Select");
        if (SimCard == 0) {SimCard = 1;}
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
                        default:
                            if (mCoreDualSim.isSIM1Ready()) {
                                mITelephony.setPreferredNetworkType(SIMid[0], lastStateNetworkTypeSIM1);
                            }
                    }
                } else {
                    mITelephony.setPreferredNetworkType(lastStateNetworkTypeSIM1, 0);
                }
            } catch (RemoteException e) {

            }
        }
        mContext.sendBroadcast(new Intent("my.settings.CHANGE_SMART_MOBILE_NETWORK"));

    }




    public void setPreferredNetworkTypeWiFiOn() {
        int networkType;
        //int[] SIMid = mCoreDualSim.getSubscriptionId();
        int[] SIMid = mCoreDualSim.getSubId(mContext);
        networkType = checkRILConstants(MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_WiFiOn"));
        //networkType = getRILConstants(Settings.System.getString(mContext.getContentResolver(),"mobiledata_WiFiOn"));
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
        mIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        //mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        context.registerReceiver(mIntent,mIntentFilter);
    }

    private int checkRILConstants(int networkType){
        String value;
        int result;
        if (networkTypeHash.size() == 0 ){
            String[] NetworkTypeString ={"NETWORK_MODE_WCDMA_PREF",
                    "NETWORK_MODE_GSM_ONLY",
                    "NETWORK_MODE_WCDMA_ONLY",
                    "NETWORK_MODE_GSM_UMTS",
                    "NETWORK_MODE_CDMA",
                    "NETWORK_MODE_CDMA_NO_EVDO",
                    "NETWORK_MODE_EVDO_NO_CDMA",
                    "NETWORK_MODE_GLOBAL",
                    "NETWORK_MODE_LTE_CDMA_EVDO",
                    "NETWORK_MODE_LTE_GSM_WCDMA",
                    "NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA",
                    "NETWORK_MODE_LTE_ONLY",
                    "NETWORK_MODE_LTE_WCDMA",
                    "NETWORK_MODE_TDSCDMA_ONLY",
                    "NETWORK_MODE_TDSCDMA_WCDMA",
                    "NETWORK_MODE_LTE_TDSCDMA",
                    "NETWORK_MODE_TDSCDMA_GSM",
                    "NETWORK_MODE_LTE_TDSCDMA_GSM",
                    "NETWORK_MODE_TDSCDMA_GSM_WCDMA",
                    "NETWORK_MODE_LTE_TDSCDMA_WCDMA",
                    "NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA",
                    "NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA",
                    "NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA"};
            for (int i= 0;i<=NetworkTypeString.length-1; i ++){
                networkTypeHash.put(i,NetworkTypeString[i]);
            }
        }
        if (networkType <= 0) { return -1;}
        switch (networkType){
            case 18:
                return networkType;
            case 20:
                return networkType;
            case  90:
                networkType = 0;
        }
        value = networkTypeHash.get(networkType).toString();
        try {
            Field field = Class.forName("com.android.internal.telephony.RILConstants").getField(value);
            result = field.getInt(null);
        } catch (NoSuchFieldException e) {
            result = -1;
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            result = -1;
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            result = -1;
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
                //Settings.System.putInt(mContext.getContentResolver(), "mobiledata_activity", 0);
                setCallPreferredNetworkType(true);
            }
            CallState = true;
        }

        if ((phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) && (CallState)) {
            if (SmartNetLastState != 0) {
                //Settings.System.putInt(mContext.getContentResolver(), "mobiledata_activity", SmartNetLastState);
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
                if (l / 1000 <= 10) {
                    Log.d("SmartNet2.0", "setTimer(I)V: Switching seconds remaining: " + l / 1000);
                }
            }

            @Override
            public void onFinish() {
                timerDone = true;
                timerStart = false;
                //check WiFi enabled
                if (!getStateWiFiData() && !getConnectedWiFiData() && timerSleepOn > 0) {
                    setTimerSleepOn(timerSleepOn);
                    cTimerSleepOn.start();
                }
                setPreferredNetworkType(mCoreDualSim.getSubId(mContext),checkRILConstants(1));
                Log.d("SmartNet2.0","setTimer(I)V: Switch network type");
            }
        };
    }

    private  void setTimerSleepOn(int value){ //Timer enabled fast mobile data (10,20,30 minute example)
        cTimerSleepOn = new CountDownTimer(value,1000) {
            @Override
            public void onFinish() {
                //Restore config
                restoreCurrentPrefferedNetworkType();
                cTimerSleepOn.cancel();
                cTimerSleepOn = null;
                setTimerSleepAction();
                cTimerSleepOnAction.start();
                Log.d("SmartNet2.0","setTimerSleepOn(I)V: Enable fast speed mobile data for synchronize");
            }

            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished / 1000 <= 10) {
                    Log.d("SmartNet2.0", "setTimerSleepOn(I)V: Switching seconds remaining: " + millisUntilFinished / 1000);
                }

            }
        };
    }

    private void setTimerSleepAction(){ //Work Time 1 minute
        cTimerSleepOnAction = new CountDownTimer(60000,1000) {
            @Override
            public void onFinish() {
                //Transition to 2G
                setPreferredNetworkType(mCoreDualSim.getSubId(mContext),checkRILConstants(1));
                cTimerSleepOnAction.cancel();
                cTimerSleepOnAction = null;
                setTimerSleepOn(timerSleepOn);
                cTimerSleepOn.start();
                Log.d("SmartNet2.0","setTimerSleepAction()V: Disable fast speed mobile data for synchronize");
            }

            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished / 1000 <= 10) {
                    Log.d("SmartNet2.0", "setTimerSleepAction()V: Switching seconds remaining: " + millisUntilFinished / 1000);
                }
            }
        };

    }

    private void controlTimers(int value){
        switch (value){
            case 0:
                cTimer = null;
                timerDone = false;
                if (cTimerSleepOn != null) {
                    cTimerSleepOn.cancel();
                    cTimerSleepOn = null;
                }
                if (cTimerSleepOnAction != null) {
                    cTimerSleepOnAction.cancel();
                    cTimerSleepOnAction = null;
                }
                break;
            case 1:
                setTimer(timeToCompletion);
                cTimer.start();
                timerStart = true;
                break;
            case -1:
                cTimer.cancel();
                cTimer = null;
                timerStart = false;
                if (cTimerSleepOn != null) {
                    cTimerSleepOn.cancel();
                    cTimerSleepOn = null;
                }
                if (cTimerSleepOnAction != null) {
                    cTimerSleepOnAction.cancel();
                    cTimerSleepOnAction = null;
                }
                break;
        }

    }
    private void TimerIntentAction(boolean state) {
        timeToCompletion = MiuiCoreSettingsPreference.getKeyParam(mContext, "smartnet_timer_value");
        timerSleepOn = MiuiCoreSettingsPreference.getKeyParam(mContext, "smartnet_timer_sleepon_value");
        Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Charging State="+chargingState);
        if (timeToCompletion > 0) {
            if (state) {
                if ((timerDone) && (cTimer != null)) {
                    //Restore NetworkType
                    restoreCurrentPrefferedNetworkType();
                    controlTimers(0);
                    Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Restore Network Type");
                } else {
                    //Cancel Timer
                    if ((timerStart) && (cTimer != null)) {
                        controlTimers(-1);
                        Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Cancel Timer");
                    }
                }
            } else {

                if ((!timerStart) && (!chargingState) && (!CallState)) {
                    //Save Network Type Start Timer
                    saveCurrentPrefferedNetworkType();
                    controlTimers(1);
                    Log.d("SmartNet2.0", "TimerIntentAction(Z)V: Start Timer");

                }
            }

        }
    }

    private void BatteryCharging(Intent mIntent){
        int status = mIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (MiuiCoreSettingsPreference.getKeyParam(mContext,"smartnet_charging_control") != 0) {
            chargingState = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        } else {
            chargingState = false;
        }
    }
/*
    private void WiFiControl(){
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> results = wifiManager.getScanResults();
        for (final ScanResult ap : results) {
            Log.d("SmartNet2.3","SSID: "+ap.SSID+" level: "+ap.level);
        }
        List<WifiConfiguration> configured = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configured) {
            Log.d("SmartNet2.3",config.SSID);
        }

    }*/
    class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            SmartNet mSmartNet = SmartNet.this;
            String mIntent = intent.getAction();
            //Log.d("SmartNet2.0","Intent="+mIntent);
            if("android.intent.action.PHONE_STATE".equals(mIntent)){
                CallIntentAction(intent);
            }

           /* if ("android.net.wifi.SCAN_RESULTS".equals(mIntent)){

            WiFiControl();
            }*/

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
