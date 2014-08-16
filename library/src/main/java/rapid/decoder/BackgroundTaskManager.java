package rapid.decoder;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

class BackgroundTaskManager {
    private static Boolean sHasSupportLibraryV4;

    private static WeakHashMap<Object, BackgroundTask> sWeakTasks;
    private static HashMap<Object, BackgroundTask> sStrongTasks;

    @NonNull
    public static BackgroundTask register(@NonNull Object key) {
        BackgroundTask task;
        if (shouldBeWeak(key)) {
            task = new BackgroundTask(new WeakReference<Object>(key));
            if (sWeakTasks == null) {
                sWeakTasks = new WeakHashMap<Object, BackgroundTask>();
            } else {
                cancelRecord(sWeakTasks, key);
            }
            sWeakTasks.put(key, task);
        } else {
            task = new BackgroundTask(key);
            if (sStrongTasks == null) {
                sStrongTasks = new HashMap<Object, BackgroundTask>();
            } else {
                cancelRecord(sStrongTasks, key);
            }
            sStrongTasks.put(key, task);
        }
        return task;
    }

    public static BackgroundTask removeWeak(Object key) {
        if (key == null) return null;
        if (sWeakTasks != null) {
            return sWeakTasks.remove(key);
        }
        return null;
    }

    public static BackgroundTask removeStrong(Object key) {
        if (key == null) return null;
        if (sStrongTasks != null) {
            return sStrongTasks.remove(key);
        }
        return null;
    }

    public static boolean cancelStrong(Object key) {
        if (sStrongTasks == null) return false;
        BackgroundTask task = sStrongTasks.remove(key);
        if (task != null) {
            task.cancel();
            return true;
        } else {
            return false;
        }
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
        return o instanceof View ||
                o instanceof Activity ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && o instanceof Fragment ||
                hasSupportLibraryV4() && o instanceof android.support.v4.app.Fragment;
    }

    public static boolean hasAnyTasks() {
        return sWeakTasks != null && !sWeakTasks.isEmpty() ||
                sStrongTasks != null && !sStrongTasks.isEmpty();
    }
}
