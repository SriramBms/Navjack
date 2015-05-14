package de.tudarmstadt.tk.carsensing.powermanager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import de.tudarmstadt.tk.carsensing.activity.LiveDataActivity;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Activity to display live data from the OBD dongle
 * @author Sriram Shantharam
 *
 */

public class PhoneActivityService extends IntentService {
	private final static String NAME="PhoneActivityService";
	private Handler handler=new Handler();
	
	private Context mContext;
	
	public static final String ACTION_STOP_UPDATES = "android.action.stop_updates";
	public static final String ACTION_START_UPDATES = "android.action.start_updates";
	private static final int ACTIVITY_CONFIDENCE_THRESHOLD = 70;
	private static final long TIME_UPDATE_INTERVAL = 60*1000; //In milliseconds - mins* seconds per minute * milliseconds per second
	private static final String TAG = "PhoneActivityService";
	
	public PhoneActivityService() {
		super(NAME);
		Log.v(NAME,NAME+":Service started");
		
	}

	@Override
	public void onCreate(){
		super.onCreate();
		Log.v(TAG,"OnCreate");
		mContext=this;


	}


	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(NAME,NAME+"Intent received");
		if(ActivityRecognitionResult.hasResult(intent)){
			ActivityRecognitionResult activityresult = ActivityRecognitionResult.extractResult(intent);
			DetectedActivity activity = activityresult.getMostProbableActivity();
			final int activityconfidence = activity.getConfidence();
			final String activityname = getNameFromType(activity.getType());
			Log.v(NAME,activityname+":"+activityconfidence);
			/*// Logging for debugging. Not really required in final build
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(mContext, activityname+":"+activityconfidence, Toast.LENGTH_SHORT).show();
				}
			});
			*/
			

			if(activity.getType()==DetectedActivity.STILL){
				
					Log.v(TAG,"Phone stationary");
					Intent phonestationary = new Intent();
					phonestationary.setAction(LiveDataActivity.ACTION_PHONE_STATIONARY);
					
					sendBroadcast(phonestationary);
					
			}else{
				Intent phoneinmotion = new Intent();
				phoneinmotion.setAction(LiveDataActivity.ACTION_PHONE_IN_MOTION);
				sendBroadcast(phoneinmotion);
				Log.v(TAG,"Phone not stationary");
			
			}

		}else{
			Log.v(NAME,intent.toString());
		}

	}
	
	

	private String getNameFromType(int activityType) {
		switch(activityType) {
		case DetectedActivity.IN_VEHICLE:
			return "In_vehicle";
		case DetectedActivity.ON_BICYCLE:
			return "On_bicycle";
		case DetectedActivity.ON_FOOT:
			return "On_foot";
		case DetectedActivity.STILL:
			return "Still";
		case DetectedActivity.UNKNOWN:
			return "Unknown";
		case DetectedActivity.TILTING:
			return "Tilting";
		case DetectedActivity.WALKING:
			return "Walking";
		case DetectedActivity.RUNNING:
			return "Running";

		}
		return "Not in list";
	}

}
