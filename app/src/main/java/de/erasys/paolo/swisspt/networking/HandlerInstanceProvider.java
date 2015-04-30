package de.erasys.paolo.swisspt.networking;


import android.os.Handler;

/**
 * Created by paolo on 30.04.15.
 */
public class HandlerInstanceProvider {

    private static Handler mHandler = null;

    public static Handler getInstance() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        return mHandler;
    }

}
