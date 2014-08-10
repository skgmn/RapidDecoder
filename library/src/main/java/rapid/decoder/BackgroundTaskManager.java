package rapid.decoder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

class BackgroundTaskManager {
    private static WeakHashMap<Object, BackgroundBitmapLoadTask> sWeakJobs;
    private static HashMap<Object, BackgroundBitmapLoadTask> sStrongJobs;

    @NonNull
    public BackgroundBitmapLoadTask register(@NonNull Object key, boolean isStrong) {
        BackgroundBitmapLoadTask task = new BackgroundBitmapLoadTask();
        if (isStrong) {
            if (sStrongJobs == null) {
                sStrongJobs = new HashMap<Object, BackgroundBitmapLoadTask>();
            } else {
                cancelRecord(sStrongJobs, key);
            }
            sStrongJobs.put(key, task);
        } else {
            if (sWeakJobs == null) {
                sWeakJobs = new WeakHashMap<Object, BackgroundBitmapLoadTask>();
            } else {
                cancelRecord(sWeakJobs, key);
            }
            sWeakJobs.put(key, task);
        }
        return task;
    }

    @Nullable
    public BackgroundBitmapLoadTask remove(@NonNull Object key) {
        if (sWeakJobs != null) {
            BackgroundBitmapLoadTask record = sWeakJobs.remove(key);
            if (record != null) {
                return record;
            }
        }
        if (sStrongJobs != null) {
            return sStrongJobs.remove(key);
        }
        return null;
    }

    private static void cancelRecord(Map<Object, BackgroundBitmapLoadTask> map, Object key) {
        BackgroundBitmapLoadTask task = map.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    public void execute(BackgroundBitmapLoadTask task) {
        Object key = task.key();
        if (key == null) return;

        register(key, task.isKeyStrong());
        task.start();
    }
}
