package de.erasys.paolo.swisspt.networking;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by paolo on 30.04.15.
 */
public class CommonQueueExecutor {

    private final ExecutorService mService;

    private static CommonQueueExecutor sInstance;

    private CommonQueueExecutor() {
        mService = Executors.newFixedThreadPool(10);
    }

    public synchronized static CommonQueueExecutor getInstance() {
        if (sInstance == null || sInstance.isShutdown())  {
            sInstance = new CommonQueueExecutor();
        }
        return sInstance;
    }

    public void shutdown() {
        mService.shutdown();
    }

    public boolean isShutdown() {
        return mService.isShutdown();
    }

    public void addRequest(Runnable task) {
        mService.execute(task);
    }

}
