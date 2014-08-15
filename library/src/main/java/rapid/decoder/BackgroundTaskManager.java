package rapid.decoder;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

class BackgroundTaskManager {
    private static Boolean sHasSupportLibraryV4;

    private static WeakHashMap<Object, BackgroundTask> sWeakJobs;
    private static HashMap<Object, BackgroundTask> sStrongJobs;

    private static Handler sHandler;

    @NonNull
    public static BackgroundTask register(@NonNull Object key) {
        BackgroundTask task;
        if (shouldBeWeak(key)) {
            task = new BackgroundTask(new WeakReference<Object>(key));
            if (sWeakJobs == null) {
                sWeakJobs = new WeakHashMap<Object, BackgroundTask>();
            } else {
                cancelRecord(sWeakJobs, key);
            }
            sWeakJobs.put(key, task);
        } else {
            task = new BackgroundTask(key);
            if (sStrongJobs == null) {
                sStrongJobs = new HashMap<Object, BackgroundTask>();
            } else {
                cancelRecord(sStrongJobs, key);
            }
            sStrongJobs.put(key, task);
        }
        return task;
    }

    public static BackgroundTask removeWeak(Object key) {
        if (key == null) return null;
        if (sWeakJobs != null) {
            return sWeakJobs.remove(key);
        }
        return null;
    }

    public static BackgroundTask removeStrong(Object key) {
        if (key == null) return null;
        if (sStrongJobs != null) {
            return sStrongJobs.remove(key);
        }
        return null;
    }

    private static void cancelRecord(Map<Object, BackgroundTask> map, Object key) {
        BackgroundTask task = map.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    private static boolean hasSupportLibraryV4() {
        if (sHasSupportLibraryV4 == null) {
            try {
                Class.forName("android.support.v4.app.Fragment");
                sHasSupportLibraryV4 = true;
            } catch (ClassNotFoundException e) {
                sHasSupportLibraryV4 = false;
            }
        }
        return sHasSupportLibraryV4;
    }

    static boolean shouldBeWeak(Object o) {
        return o instanceof Activity ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && o instanceof Fragment ||
                hasSupportLibraryV4() && o instanceof android.support.v4.app.Fragment;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void cleanUpTasks() {
        if (sWeakJobs == null) return;
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sWeakJobs == null) return;
                for (BackgroundTask task : sWeakJobs.values()) {
                    task.cancel();
                }
            }
        });
    }
}
