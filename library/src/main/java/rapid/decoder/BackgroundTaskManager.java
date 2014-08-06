package rapid.decoder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

class BackgroundTaskManager {
    private static WeakHashMap<Object, BackgroundTaskRecord> sWeakJobs;
    private static HashMap<Object, BackgroundTaskRecord> sStrongJobs;

    @NonNull
    public BackgroundTaskRecord register(@NonNull Object key, boolean isStrong) {
        BackgroundTaskRecord record = new BackgroundTaskRecord();
        if (isStrong) {
            if (sStrongJobs == null) {
                sStrongJobs = new HashMap<Object, BackgroundTaskRecord>();
            } else {
                cancelRecord(sStrongJobs, key);
            }
            sStrongJobs.put(key, record);
        } else {
            if (sWeakJobs == null) {
                sWeakJobs = new WeakHashMap<Object, BackgroundTaskRecord>();
            } else {
                cancelRecord(sWeakJobs, key);
            }
            sWeakJobs.put(key, record);
        }
        return record;
    }

    @Nullable
    public BackgroundTaskRecord remove(@NonNull Object key) {
        if (sWeakJobs != null) {
            BackgroundTaskRecord record = sWeakJobs.remove(key);
            if (record != null) {
                return record;
            }
        }
        if (sStrongJobs != null) {
            return sStrongJobs.remove(key);
        }
        return null;
    }

    private static void cancelRecord(Map<Object, BackgroundTaskRecord> map, Object key) {
        BackgroundTaskRecord record = map.remove(key);
        if (record != null) {
            record.isStale = true;
            BitmapLoadTask task = record.getTask();
            if (task != null) {
                task.cancel();
            }
        }
    }

    public void execute(BitmapLoadTask task) {
        Object key = task.key();
        if (key == null) return;

        BackgroundTaskRecord record = register(key, task.isKeyStrong());
        record.execute(task);
    }
}
