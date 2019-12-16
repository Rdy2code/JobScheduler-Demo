package com.example.android.notificationscheduler;

import androidx.appcompat.app.AppCompatActivity;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //Class member variables
    private JobScheduler mScheduler;
    private Switch mDeviceIdleSwitch;
    private Switch mDeviceChargingSwitch;
    private SeekBar mSeekBar;

    //Integer ID Variables
    private static final int JOB_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize member variables
        mDeviceIdleSwitch = findViewById(R.id.idleSwitch);
        mDeviceChargingSwitch = findViewById(R.id.chargingSwitch);
        mSeekBar = findViewById(R.id.seekBar);
        final TextView seekBarProgress = findViewById(R.id.seekBarProgress);

        //Use a seekbar to set a hard deadline by which the job must be run
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i > 0) {
                    seekBarProgress.setText(
                            getString(R.string.seekbar_set, i, " s"));
                } else {
                    seekBarProgress.setText(getString(R.string.seekbar_not_set));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void cancelJobs(View view) {
        if (mScheduler != null) {
            mScheduler.cancelAll();
            Toast.makeText(this, getString(R.string.toast_jobs_cancelled),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void scheduleJob(View view) {

        mScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        //Get RadioGroup data from UI settings
        RadioGroup netWorkOptions = findViewById(R.id.networkOptions);
        int selectedNetworkID = netWorkOptions.getCheckedRadioButtonId();

        // Get the id of the selected radiobutton and set the JobInfo field to the appropriate value
        // Note "NONE" is the default setting, but the JobScheduler does not count this as a constraint.
        // JobScheduler requires at least one constraint.
        // Include a conditional to check for at least one constraint.
        int selectedNetworkOption = JobInfo.NETWORK_TYPE_NONE;

        switch (selectedNetworkID) {
            case R.id.noNetwork:
                selectedNetworkOption = JobInfo.NETWORK_TYPE_NONE;
                break;
            case R.id.anyNetwork:
                selectedNetworkOption = JobInfo.NETWORK_TYPE_ANY;
                break;
            case R.id.wifiNetwork:
                selectedNetworkOption = JobInfo.NETWORK_TYPE_UNMETERED;
                break;
        }

        //Get SeekBar user-defined setting data from UI
        int seekBarInteger = mSeekBar.getProgress();
        //If the user has set the seekbar, set a boolean to true to indicate this is a constraint
        boolean seekBarSet = seekBarInteger > 0;

        //Build the JobInfo object which contains the data to be passed to the JobScheduler and
        //which identifies the JobService class that will run the job.
        ComponentName serviceName = new ComponentName(getPackageName(),
                NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceName);

        //Register the user-selected constraints with the JobInfo object
        builder.setRequiredNetworkType(selectedNetworkOption);
        //Wait until the screen is off and cpu asleep
        builder.setRequiresDeviceIdle(mDeviceIdleSwitch.isChecked());
        //Wait until the device is charging
        builder.setRequiresCharging(mDeviceChargingSwitch.isChecked());

        if (seekBarSet) {
            //The seekbar setting is in milliseconds, so convert to seconds before passing to builder
            builder.setOverrideDeadline(seekBarInteger * 1000);
        }

        //Set boolean to indicate whether a constraint has been set
        boolean constraintSet =
                (selectedNetworkOption != JobInfo.NETWORK_TYPE_NONE)
                        || mDeviceIdleSwitch.isChecked()
                        || mDeviceChargingSwitch.isChecked()
                        || seekBarSet;

        if (constraintSet) {
            //Package the JobInfo object and schedule the job, because the user has chosen a constraint
            JobInfo jobInfo = builder.build();
            mScheduler.schedule(jobInfo);
            Toast.makeText(this,
                    getString(R.string.toast_job_scheduled), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    getString(R.string.toast_set_constraint_message),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
