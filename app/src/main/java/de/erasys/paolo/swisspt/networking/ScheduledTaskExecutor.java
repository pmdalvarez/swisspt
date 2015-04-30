package de.erasys.paolo.swisspt.networking;

import android.os.Handler;

import java.util.ArrayList;

/**
 * Created by paolo on 30.04.15.
 */
public class ScheduledTaskExecutor {

    private static ScheduledTaskExecutor sInstance;

    private ArrayList<Reloader> mReloaders;

    private ScheduledTaskExecutor() {
        mReloaders = new ArrayList<>();
    }

    public synchronized static ScheduledTaskExecutor getInstance() {
        if (sInstance == null) {
            sInstance = new ScheduledTaskExecutor();
        }
        return sInstance;
    }

    public void addTask(BasicLoader loader, int reloadTime) {
        Reloader reloader = new Reloader(loader, reloadTime);
        HandlerInstanceProvider.getInstance().post(reloader);
        mReloaders.add(reloader);
    }

    public void removeTask(BasicLoader loader) {
        Reloader reloader = getReloader(loader);
        if (reloader != null) {
            HandlerInstanceProvider.getInstance().removeCallbacks(reloader);
        }
    }

    private Reloader getReloader(BasicLoader loader) {
        for (Reloader reloader : mReloaders) {
            if (reloader.getLoader() == loader) {
                return reloader;
            }
        }
        return null;
    }

}
