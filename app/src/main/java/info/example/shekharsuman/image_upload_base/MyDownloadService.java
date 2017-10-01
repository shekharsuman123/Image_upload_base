/**
 *
 */

package info.example.shekharsuman.image_upload_base;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;

public class MyDownloadService extends Service {

    private static final String TAG = "Storage#DownloadService";

    /** Actions **/
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String ACTION_COMPLETED = "action_completed";
    public static final String ACTION_ERROR = "action_error";

    /** Extras **/
    public static final String EXTRA_DOWNLOAD_PATH = "extra_download_path";
    public static final String EXTRA_URL_PATH = "extra_url_path";
    public static final String EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded";

    private StorageReference mStorage;
    private int mNumTasks = 0;
    FirebaseDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Storage
        mStorage = FirebaseStorage.getInstance().getReference();
        database = FirebaseDatabase.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            // Get the path to download from the intent
            final String downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH);
            final String brodUrl = intent.getStringExtra(EXTRA_URL_PATH);
            // Mark task started
            Log.d(TAG, ACTION_DOWNLOAD + ":" + downloadPath);
            taskStarted();
            DatabaseReference myRef = database.getReference("activity");
            myRef.child("photo").setValue(brodUrl);


            // Download and get total bytes
            mStorage.child(downloadPath).getStream()
                    .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
                        @Override
                        @SuppressWarnings("VisibleForTests") public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG, "download:SUCCESS");

                            // Send success broadcast with number of bytes downloaded
                            Intent broadcast = new Intent(ACTION_COMPLETED);
                            broadcast.putExtra(EXTRA_DOWNLOAD_PATH, downloadPath);
                            broadcast.putExtra(EXTRA_BYTES_DOWNLOADED, taskSnapshot.getTotalByteCount());
                            LocalBroadcastManager.getInstance(getApplicationContext())
                                    .sendBroadcast(broadcast);
                            //display in long period of time
                            Toast.makeText(getApplicationContext(), "msg msg", Toast.LENGTH_LONG).show();
                            // Mark task completed
                            taskCompleted();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.w(TAG, "download:FAILURE", exception);

                            // Send failure broadcast
                            Intent broadcast = new Intent(ACTION_ERROR);
                            broadcast.putExtra(EXTRA_DOWNLOAD_PATH, downloadPath);
                            LocalBroadcastManager.getInstance(getApplicationContext())
                                    .sendBroadcast(broadcast);

                            // Mark task completed
                            taskCompleted();
                        }
                    });
        }

        return START_REDELIVER_INTENT;
    }

    private void taskStarted() {
        changeNumberOfTasks(1);
    }

    private void taskCompleted() {
        changeNumberOfTasks(-1);
    }

    private synchronized void changeNumberOfTasks(int delta) {
        Log.d(TAG, "changeNumberOfTasks:" + mNumTasks + ":" + delta);
        mNumTasks += delta;

        // If there are no tasks left, stop the service
        if (mNumTasks <= 0) {
            Log.d(TAG, "stopping");
            stopSelf();
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COMPLETED);
        filter.addAction(ACTION_ERROR);

        return filter;
    }
}
