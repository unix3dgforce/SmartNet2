package com.android.systemui.smartnet;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.List;

public class SmartNetCoreDualSim {

    private static SmartNetCoreDualSim mSmartNetCoreDualSim;
    private String imeiSIM1;
    private String imeiSIM2;
    private boolean isSIM1Ready;
    private boolean isSIM2Ready;
    private int[] mSubscriptionId;

    public String getImeiSIM1() {
        return imeiSIM1;
    }

    public String getImeiSIM2() {
        return imeiSIM2;
    }

    public boolean isSIM1Ready() {
        return isSIM1Ready;
    }

    public boolean isSIM2Ready() {
        return isSIM2Ready;
    }
    public boolean isDualSIM() {
        return imeiSIM2 != null;
    }
    public int[] getSubscriptionId(){
        return mSubscriptionId;
    }

    private SmartNetCoreDualSim(){

    }

    public static SmartNetCoreDualSim getInstance(Context context){
        mSmartNetCoreDualSim = new SmartNetCoreDualSim();

        TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        //mSmartNetCoreDualSim.mSubscriptionId = getSubId(context);
        //mSmartNetCoreDualSim.imeiSIM1 = telephonyManager.getDeviceId();
        mSmartNetCoreDualSim.imeiSIM1 = null;
        mSmartNetCoreDualSim.imeiSIM2 = null;

        try{
            mSmartNetCoreDualSim.imeiSIM1 = getDeviceIdBySlot(context, "getDeviceId", 0);
            mSmartNetCoreDualSim.imeiSIM2 = getDeviceIdBySlot(context, "getDeviceId", 1);

        } catch (GeminiMethodNotFoundException e) {
            //e.printStackTrace();

            try {
                mSmartNetCoreDualSim.imeiSIM1 = getDeviceIdBySlot(context, "getDeviceIdGemini", 0);
                mSmartNetCoreDualSim.imeiSIM2 = getDeviceIdBySlot(context, "getDeviceIdGemini", 1);
            } catch (GeminiMethodNotFoundException e1) {
                //Call here for next manufacturer's predicted method name if you wish
                //e1.printStackTrace();
            }
        }

        //mSmartNetCoreDualSim.isSIM1Ready = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        mSmartNetCoreDualSim.isSIM1Ready = false;
        mSmartNetCoreDualSim.isSIM2Ready = false;

        try {
            mSmartNetCoreDualSim.isSIM1Ready = getSIMStateBySlot(context, "getSimState", 0);
            mSmartNetCoreDualSim.isSIM2Ready = getSIMStateBySlot(context, "getSimState", 1);

        } catch (GeminiMethodNotFoundException e) {
            try {

                mSmartNetCoreDualSim.isSIM1Ready = getSIMStateBySlot(context, "getSimStateGemini", 0);
                mSmartNetCoreDualSim.isSIM2Ready = getSIMStateBySlot(context, "getSimStateGemini", 1);
            e.printStackTrace();

            } catch (GeminiMethodNotFoundException e1) {
                //Call here for next manufacturer's predicted method name if you wish
                e1.printStackTrace();
            }
        }

        return mSmartNetCoreDualSim;
    }

    public int[] getSubId(Context mContext){
        int[] SubId = new int[2];
        int i =0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){
            List<SubscriptionInfo> mSubscriptionInfo = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
            if (mSubscriptionInfo != null){
                for (SubscriptionInfo subInfo : mSubscriptionInfo){
                    if (isSIM1Ready && i == 0) {
                        SubId[i] = subInfo.getSubscriptionId();
                        i++;
                        continue;
                    }
                    if (isSIM2Ready && i == 1){
                        SubId[i] = subInfo.getSubscriptionId();
                        i++;
                        continue;
                    }

                    if (isSIM2Ready) {
                        SubId[i+1] = subInfo.getSubscriptionId();
                        continue;
                    }
                    i++;
                }
            }
        } else {
            SubId[i]=i;
            i++;
        }
        return SubId;
    }

    private static String getDeviceIdBySlot(Context context, String predictedMethodName, int slotID) throws GeminiMethodNotFoundException {
        String imei = null;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try{
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);
            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimID.invoke(telephony, obParameter);
            if(ob_phone != null){
                imei = ob_phone.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeminiMethodNotFoundException(predictedMethodName);
        }
        return imei;
    }

    private static  boolean getSIMStateBySlot(Context context, String predictedMethodName, int slotID) throws GeminiMethodNotFoundException {
        boolean isReady = false;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try{
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimStateGemini = telephonyClass.getMethod(predictedMethodName, parameter);
            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimStateGemini.invoke(telephony, obParameter);
            if(ob_phone != null){
                int simState = Integer.parseInt(ob_phone.toString());
                if(simState == TelephonyManager.SIM_STATE_READY){
                    isReady = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GeminiMethodNotFoundException(predictedMethodName);
        }

        return isReady;
    }


    private static class GeminiMethodNotFoundException extends Exception {

        private static final long serialVersionUID = -996812356902545308L;

        public GeminiMethodNotFoundException(String info) {
            super(info);
        }
    }
}
