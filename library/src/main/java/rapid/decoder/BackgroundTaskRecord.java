package rapid.decoder;

import android.os.AsyncTask;
import android.os.Build;

class BackgroundTaskRecord {
    public boolean isStale;

    private LoadIntoViewTask mTask;

    public void execute(LoadIntoViewTask task) {
        mTask = task;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, task);
        } else {
            task.execute();
        }
    }

    public LoadIntoViewTask getTask() {
        return mTask;
    }
}
