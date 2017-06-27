package com.android.systemui.smartnet;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.MiuiCoreSettingsPreference;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyIntents;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class SmartNetActivity extends AppCompatActivity {
    TextView tv;
    Button btn_set,mob_on,mob_off;
    RadioButton sim1;
    RadioButton sim2;
    RadioButton combo;
    EditText mobiledata_on;
    EditText mobiledata_off;
    SubscriptionManager mSubscriptionManager;
    TelephonyManager mTelephonyManager;
    //private SmartNetCoreDualSim mCoreDualSim;
    private Context mContext;

    boolean state = false;
    //public void returnNetworkType(){
    //    SmartNet.setEndCallPreferredNetworkType(getApplicationContext());
    //}

    /*public void setPreferredNetworkTypeWiFiOn() {
        int networkType;
        int[] SIMid = mCoreDualSim.getSubscriptionId();
        networkType = MiuiCoreSettingsPreference.getKeyParam(mContext, "mobiledata_WiFiOn");
        switch (networkType) {
            case -1:
                return;
            case 0:
                networkType = 1;
                break;
        }
        setPreferredNetworkType(SIMid, networkType);
    }

    private void setPreferredNetworkType(int[] SIMid, int networkType){

    }

    private void setStateWiFi(boolean value){
        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(!value);
    }

    private boolean getStateMobileData(){
        int DataSub;
        try{
            TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method getMobileDataEnabledMethod = mTelephonyManager.getClass().getDeclaredMethod("getDataEnabled",int.class);
            if (getMobileDataEnabledMethod != null){
                boolean mobileDataEnabled = (Boolean) getMobileDataEnabledMethod.invoke(mTelephonyManager,1);
                return mobileDataEnabled;
            }

        } catch (Exception ex) {

        }
        return false;
    }

    private void setStateWiFiData(boolean value){
        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(value);
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

    private boolean MobileCheck(){
        boolean result = false;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiCheck = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiCheck.isConnected()) {
            result = true;
        }
        return result;
    }

*/
    private  void setCallPreferredNetworkType (Context context, boolean state){
        try {
            Context mMiuiSystemUI = context.createPackageContext("com.android.systemui", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            Class<?> mSmartNetClass = Class.forName("com.android.systemui.smartnet.SmartNet", true, mMiuiSystemUI.getClassLoader());
            Constructor[] allconstructor = mSmartNetClass.getConstructors();
            Class[] parametersOfConstructor1 = allconstructor[1].getParameterTypes();
            Constructor constructor1 = mSmartNetClass.getConstructor(parametersOfConstructor1);
            constructor1.setAccessible(true);
            Object clazz = constructor1.newInstance(context, true);
            Method method = clazz.getClass().getDeclaredMethod("setCallPreferredNetworkType", new Class[]{boolean.class});
            Toast.makeText(context,"Init setCallPreferredNetworkType",Toast.LENGTH_SHORT).show();
            if (state) {
                int callLaststate = MiuiCoreSettingsPreference.getKeyParam(context,"mobiledata_activity");
                Log.d("SmartNet2.0","InCallActivity CallLastState="+callLaststate);
                Settings.System.putInt(context.getContentResolver(),"mobiledata_activity_laststate",callLaststate);
                Settings.System.putInt(context.getContentResolver(), "mobiledata_activity", 0);
                method.invoke(clazz, true);
                Toast.makeText(context,"setCallPreferredNetworkType - True",Toast.LENGTH_SHORT).show();
            } else {
                int laststate = MiuiCoreSettingsPreference.getKeyParam(context,"mobiledata_activity_laststate");
                Log.d("SmartNet2.0","InCallActivity CallLastState="+laststate);
                Settings.System.putInt(context.getContentResolver(), "mobiledata_activity", 1);
                method.invoke(clazz, false);
                Toast.makeText(context,"setCallPreferredNetworkType - False",Toast.LENGTH_SHORT).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //private boolean getStateWiFiData(){
    //    WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
    //    return wifiManager.isWifiEnabled();
    //}

    private boolean getStateMobileData(){
        boolean MobileDataState = false;
        try{
            TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method setMobileDataEnabledMethod = mTelephonyManager.getClass().getDeclaredMethod("getDataEnabled");
            if (setMobileDataEnabledMethod != null){
                MobileDataState =(Boolean) setMobileDataEnabledMethod.invoke(mTelephonyManager);
            }
        }catch (Exception ex){

        }
        return  MobileDataState;
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

    private boolean getConnectedWiFiData(){
        ConnectivityManager connectivityManager =(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    private int getCurentNetworkType(){
        ConnectivityManager connectivityManager =(ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkType = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkType.getType();
    }


        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smartnet2);

        //SmartNet.setStartCallPreferredNetworkType(getApplicationContext());
        //SmartNet.setEndCallPreferredNetworkType(getApplicationContext());
        //returnNetworkType();

            mContext=getApplicationContext();
            //getStateWiFiData();
            //getConnectedWiFiData();
            ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));

            String active_network = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().getSubtypeName();
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            tm.getNetworkType();
            getCurentNetworkType();

            //SmartNet mSmartNet = new SmartNet(mContext);
            //mTelephonyManager = new TelephonyManager(getApplicationContext());
            //getStateMobileData();
            //getStateWiFiData();
            //new SmartNet(mContext);
            //setCallPreferredNetworkType(mContext,true);
            //setCallPreferredNetworkType(mContext,true);
            //setCallPreferredNetworkType(mContext,false);
            //setStateWiFi(true);
            //mContext.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
            //setStateMobileData(false);
            //MobileCheck();
            //mCoreDualSim = SmartNetCoreDualSim.getInstance(mContext);
            //setPreferredNetworkTypeWiFiOn();
        //SmartNetCoreDualSim mCoreDualSim = SmartNetCoreDualSim.getInstance(this);
        //mSubscriptionManager = new SubscriptionManager(getApplicationContext());

        //int currentSubId = mSubscriptionManager.getDefaultDataPhoneId();
        //int subID = SubscriptionManager.getSubId(0)[0];
        //final SubscriptionManager m1SubscriptionManager = new SubscriptionManager(getApplicationContext());
        //SubscriptionManager.setDefaultDataSubId(SubscriptionManager.getSubId(1)[0]);
        //SubscriptionManager.setDefaultDataSubId(1);


        //Intent mIntent = new Intent().setAction("android.intent.action.SIM_STATE_CHANGED");
        //sendBroadcast(mIntent);


        //mSubscriptionManager.setDefaultDataSubId(subID);
        //SubscriptionInfo ttt = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        //int zzz = ttt.getSubscriptionId();
        //SmartNet mSmartNet = new SmartNet(this);
        //String imeiSIM1 = mCoreDualSim.getImeiSIM1();
        //String imeiSIM2 = mCoreDualSim.getImeiSIM2();

        //boolean isSIM1Ready = mCoreDualSim.isSIM1Ready();
        //boolean isSIM2Ready = mCoreDualSim.isSIM2Ready();
        //Settings.Secure.putInt(getContentResolver(),"mobiledata_activity",1);
        //Settings.Secure.putInt(getContentResolver(),"call_mobiledata",12);

        //boolean isDualSIM = mCoreDualSim.isDualSIM();
        tv = (TextView) findViewById(R.id.tv);
        btn_set = (Button) findViewById(R.id.btn_set);
        mobiledata_on = (EditText) findViewById(R.id.mon);
        mobiledata_off = (EditText) findViewById(R.id.moff);
        mob_on = (Button) findViewById(R.id.on_mob_btn);
        mob_off= (Button) findViewById(R.id.off_mob_btn);
        View.OnClickListener radioButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton rb = (RadioButton)v;
                switch (rb.getId()){
                    case R.id.s1:
                        Settings.Secure.putInt(getContentResolver(),"mobiledata_SIM_Select",1);
                        Toast.makeText(getApplicationContext(),"Sim1", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.s2:
                        Settings.Secure.putInt(getContentResolver(),"mobiledata_SIM_Select",2);
                        Toast.makeText(getApplicationContext(),"Sim2", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.co:
                        Settings.Secure.putInt(getContentResolver(),"mobiledata_SIM_Select",3);
                        Toast.makeText(getApplicationContext(),"Combo", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };


        sim1 = (RadioButton) findViewById(R.id.s1);
        sim1.setOnClickListener(radioButtonClickListener);
        sim2 = (RadioButton) findViewById(R.id.s2);
        sim2.setOnClickListener(radioButtonClickListener);
        combo = (RadioButton) findViewById(R.id.co);
        combo.setOnClickListener(radioButtonClickListener);



        View.OnClickListener oclbtn_set = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Settings.Secure.putInt(getContentResolver(),"mobiledata_on",Integer.valueOf(mobiledata_on.getText().toString()));
                Settings.Secure.putInt(getContentResolver(),"mobiledata_off",Integer.valueOf(mobiledata_off.getText().toString()));
                Toast.makeText(getApplicationContext(),"Set Settings", Toast.LENGTH_SHORT).show();
            }
        };

        btn_set.setOnClickListener(oclbtn_set);



        View.OnClickListener onbtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!state){
                //setStateMobileData(true);
                 state = true;
                } else {
                   //setStateMobileData(false);
                    state = false;
                }


            }
        };

        mob_on.setOnClickListener(onbtn);

        View.OnClickListener offbtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!state){
                    //setStateWiFiData(true);
                    state = true;
                } else {
                    //setStateWiFiData(false);
                    state = false;
                }
            }
        };

        mob_off.setOnClickListener(offbtn);

        //List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        //if (subInfoList != null) {
         //   for (SubscriptionInfo subInfo : subInfoList){
                //возврат SubscriptionId

           //     Toast.makeText(getApplicationContext(),"SubscriptionId : "+subInfo.getSubscriptionId() + "Number : "+ subInfo.getNumber(),Toast.LENGTH_SHORT).show();
            //}
        //}

        /*tv.setText(" IME1 : " + imeiSIM1 + "\n" +
                " IME2 : " + imeiSIM2 + "\n" +
                " IS DUAL SIM : " + isDualSIM + "\n" +
                " IS SIM1 READY : " + isSIM1Ready + "\n" +
                " IS SIM2 READY : " + isSIM2Ready + "\n");//+
               // " Sub ID : " + currentSubId+ "\n"+
                //" GetSub ID 1: " + subID+ "\n");*/

    }


}
