package com.example.android.notificationscheduler;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class NotificationJobService extends JobService {

    private NotificationManager mNotifyManager;

    //Notification channel ID
    private static final String PRIMARY_CHANNEL_ID =
            "primary_notification_channel";
    private static final String NOTIFICATION_ID =
            "notification_jobservice_id";

    //PendingIntent ID
    private static final int PENDING_INTENT_REQUEST_CODE =
            0;

    boolean cancelJob;


    /**
     * This method is executed on the main thread. In this app, we are posting a notification, which
     * can be done on the main thread without blocking the UI
     */
    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        //Toggle the job start to on, to be read by the AsyncTask prior to running onPostExecute code
        cancelJob = false;

        new DelayTask(this).execute(jobParameters);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {

        //Tell the AsyncTask to cancel the work in onPostExecute because the constraints are no longer
        //valid
        cancelJob = true;

        Toast.makeText(this,
                "Job cancelled since device constraints changed while running",
                Toast.LENGTH_LONG).show();

        jobFinished(jobParameters, false);

        //Return true here to reschedule the job if it fails
        return true;
    }

    public void createNotificationChannel() {

        //Get an instance of the NotificationManager for API 21+
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Create the channel inside a build check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = getString(R.string.channel_name);
            String channelDescription = getString(R.string.channel_description);
            int importanceHigh = NotificationManager.IMPORTANCE_HIGH;

            //Create the notification channel
            NotificationChannel notificationChannel = new NotificationChannel(
                    PRIMARY_CHANNEL_ID,
                    channelName,
                    importanceHigh);

            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    private class DelayTask extends AsyncTask<JobParameters, Void, JobParameters> {

        private final NotificationJobService jobService;

        public DelayTask (NotificationJobService service) {
            this.jobService = service;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            SystemClock.sleep(5000);
            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            if (!cancelJob) {
                makeNotification();
            }
            jobService.jobFinished(jobParameters, false);
        }
    }

    private void makeNotification () {
        createNotificationChannel();

        //Create the intent that will launch the app when the notification is clicked
        Intent launchMainActivityIntent = new Intent (this, MainActivity.class);
        launchMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent launchMainPendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchMainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Set string resources
        String contentTitle = getString(R.string.notification_title);
        String contentText = getString(R.string.notification_text);

        //Construct the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this,
                NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_job_running)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(launchMainPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        mNotifyManager.notify(0, builder.build());
    }
}
