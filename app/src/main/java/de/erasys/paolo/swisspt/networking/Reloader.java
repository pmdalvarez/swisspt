package de.erasys.paolo.swisspt.networking;

/**
 * Created by paolo on 30.04.15.
 */
public class Reloader implements Runnable {

    BasicLoader mLoader;
    int mIntervalSecs;

    public Reloader(BasicLoader loader, int intervalSecs) {
        mLoader = loader;
        mIntervalSecs = intervalSecs;
    }

    public void run() {
        CommonQueueExecutor.getInstance().addRequest(mLoader);
        HandlerInstanceProvider.getInstance().postDelayed(this, mIntervalSecs * 1000);
    }

    public BasicLoader getLoader() {
        return mLoader;
    }

}
