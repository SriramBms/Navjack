package de.tudarmstadt.tk.carsensing.activity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.LocationServices;

import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.activity.view.GaugeView;
import de.tudarmstadt.tk.carsensing.activity.view.TextGauge;
import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.obd.OBD;
import de.tudarmstadt.tk.carsensing.obd.OBD_Status;
import de.tudarmstadt.tk.carsensing.powermanager.PhoneActivityService;

import de.tudarmstadt.tk.carsensing.util.UnitConversion;
import android.location.LocationManager;
import android.media.audiofx.BassBoost.Settings;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

/**
 * Activity to display live data from the OBD dongle
 * @author Julien Gedeon, Sriram Shantharam
 *
 */

public class LiveDataActivity extends FragmentActivity implements ConnectionCallbacks,OnConnectionFailedListener{

	private static final String TAG = "LiveDataActivity";
	public static final String UNIT_KPH = "kph";
	public static final String UNIT_MPH = "mph";
	public static final String UNIT_CELSIUS = "°C";
	public static final String UNIT_FAHRENHEIT = "°F";

	private GaugeView rpmView;
	private TextGauge mTextGauge;

	private float currentRPM;
	private float currentSpeed;
	private float currentMAF;
	private float currentIntakeTemp;
	private float currentCoolTemp;
	private String obdStatus;

	// Units are in imperial units for American cars. Change to metric to other countries
	public static String tempUnit = UNIT_FAHRENHEIT;
	public static String speedUnit = UNIT_MPH;


	//Information smoothing constants
	private static final int THRESHOLD_RPM = 0;
	private static final float THRESHOLD_SPEED = 0;
	private static final float THRESHOLD_MAF = 0;
	private static final float THRESHOLD_TEMP = 0;

	private static final int MEASUREMENT_RPM = 1;
	private static final int MEASUREMENT_SPEED = 2;
	private static final int MEASUREMENT_MAF = 3;
	private static final int MEASUREMENT_INTEMP = 4;
	private static final int MEASUREMENT_COOLTEMP = 5;


	private float mLastRPM=0.0f;
	private float mLastSpeed=0.0f;
	private float mLastMAF=0.0f;
	private float mLastInTemp=0.0f;
	private float mLastCoolTemp=0.0f;

	// Google play services declarations
	private boolean mSmartShutdown = true;
	private ConnectionResult mConnResult;
	private static final int PAM_RESOLVE_ERROR = 1;
	private boolean mResolvingError=false;
	private GoogleApiClient mGClient;
	private static final String DIALOG_ERROR = "dialog_error";
	private static final String STATE_RESOLVING_ERROR = "ResolveErrorState";
	public static final String ACTION_PHONE_STATIONARY = "de.tudarmstadt.tk.carsensing.stationary";
	public static final String ACTION_PHONE_IN_MOTION = "de.tudarmstadt.tk.carsensing.inmotion";
	private static final int REQUEST_GOOGLE_SERVICES_CONNECT=0;
	private static final int REQUEST_GOOGLE_SERVICES_DISCONNECT=-1;
	private static final int REQUEST_ACTIVITY_REC_CONNECT=1;
	private static final int REQUEST_ACTIVITY_REC_DISCONNECT=-2;
	private static final int REQUEST_SERVICES_SUSPENDED=-3;
	private final long TIMER_INTERVAL = 300*1000;
	private final long DETECTION_INTERVAL=10*1000; //In milliseconds
	private PendingIntent mActivityRecognitionPendingIntent;
	private AtomicInteger mRequestType;
	private boolean mPhoneStationary=false;
	private Timer mTimer=null;
	private TimerTask mTask;
	private Context mContext;



	private BroadcastReceiver phoneStateReceiver = new BroadcastReceiver(){
		private PhoneStationaryDialog pdialog=null;
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "Activity Broadcast received");
			String action = intent.getAction();
			if(action.equals(ACTION_PHONE_STATIONARY)){
				Log.v(TAG,"PhoneStationary");
				if(!mPhoneStationary && mTimer == null){ //if not previously set/stationary
					mTimer = new Timer();
					mTask = new TimerTask(){

						@Override
						public void run() {
							if(mPhoneStationary){
								//Exit the app or show the dialog
								//Option 1:
								LiveDataActivity.this.setResult(KioskActivity.RESULT_CODE_PHONE_IDLE);
								finish();

								//Option 2:

								/*pdialog = new PhoneStationaryDialog();
								pdialog.show(getFragmentManager(), "phone_stationary");*/


							}

						}

					};
					mTimer.schedule(mTask, TIMER_INTERVAL);

					mPhoneStationary=true;
				}

			}else if(action.equals(ACTION_PHONE_IN_MOTION)){
				Log.v(TAG,"Phone in motion");
				mPhoneStationary=false;
				if(pdialog!=null){
					pdialog.dismiss();
					pdialog=null;
				}
				if(mTimer!=null){
					mTask.cancel();
					mTimer.cancel();

					mTimer = null;
				}


			}else{
				Log.v(TAG,"Invalid intent");
			}

		}

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuItem powersaveoptions = menu.add("Powersave");

		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.v(TAG,Build.MANUFACTURER+":"+Build.MODEL+":"+Build.ID);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_live_data);
		mContext = this;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		rpmView = (GaugeView) findViewById(R.id.rpmGauge);
		rpmView.setValue(0);

		mTextGauge = (TextGauge)findViewById(R.id.TextGauge);
		mTextGauge.setStatus(TextGauge.NO_CONNECTION);
		resetUI();
		if(mSmartShutdown){
			IntentFilter filter = new IntentFilter();
			filter.addAction(ACTION_PHONE_IN_MOTION);
			filter.addAction(ACTION_PHONE_STATIONARY);
			registerReceiver(phoneStateReceiver,filter);

		}

		initGoogleClient();
		mResolvingError = savedInstanceState != null
				&& savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

		mRequestType=new AtomicInteger(0);
		Intent intent = new Intent(this, PhoneActivityService.class);
		mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

	}


	private void initGoogleClient() {
		if(mSmartShutdown){
			int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if(result==ConnectionResult.SUCCESS){

				mGClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API)
				.addApi(ActivityRecognition.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();

			}else{
				Log.w(TAG,"Error connecting to Google play services");
				showErrorDialog(result);


			}
		}


	}


	public void resetUI() {
		Log.d(TAG, "Reset UI");
		rpmView.setValue(0);
		mTextGauge.setSpeed("- "+speedUnit);
		mTextGauge.setMAF("- g/s");
		mTextGauge.setInTemp("- "+tempUnit);
		mTextGauge.setCoolTemp("- "+tempUnit);

	}



	private BroadcastReceiver obdReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received OBD data broadcast");
			Bundle bundle = intent.getExtras();

			String type = bundle.getString(OBD_Status.DATA_TYPE);
			Log.v(TAG, "OBD type:"+type);
			if (type.equals(Measurements.OBD_RPM)) {

				//TODO: TEST SETUP-REVERT THIS TO READ FLOAT IN FINAL BUILD! Only Monkey scripts send data formatted as String
				currentRPM = bundle.getFloat(Measurements.OBD_RPM);

				//String sRPM = bundle.getString(Measurements.OBD_RPM);
				//currentRPM = Float.parseFloat(sRPM);

				if(significantChange(currentRPM,MEASUREMENT_RPM))
					rpmView.setValue(currentRPM);

			}
			if (type.equals(Measurements.OBD_SPEED)) {
				//TODO: REVERT THIS TO READ FLOAT!
				//currentSpeed = Float.parseFloat(bundle.getString(Measurements.OBD_SPEED));
				currentSpeed = bundle.getFloat(Measurements.OBD_SPEED);
				if(significantChange(currentSpeed,MEASUREMENT_SPEED)){
					if (speedUnit.equals(UNIT_MPH))  currentSpeed = UnitConversion.kphToMph(currentSpeed);

					//txtSpeed.setText(currentSpeed + " " + speedUnit);
					mTextGauge.setSpeed(stringFormatter(currentSpeed+"") + " " + speedUnit);
				}

			}
			if (type.equals(Measurements.OBD_MAF)) {
				//currentMAF = Float.parseFloat(bundle.getString(Measurements.OBD_MAF));
				//TODO: REVERT TO FLOAT
				currentMAF = bundle.getFloat(Measurements.OBD_MAF);
				// txtMAF.setText(currentMAF + " g/s");
				if(significantChange(currentMAF,MEASUREMENT_MAF)){
					mTextGauge.setMAF(stringFormatter(currentMAF+"") + " g/s");
				}
			}
			if (type.equals(Measurements.OBD_INTAKE_TEMP)) {
				//TODO: Revert to float
				//currentIntakeTemp = Float.parseFloat(bundle.getString(Measurements.OBD_INTAKE_TEMP));
				currentIntakeTemp = bundle.getFloat(Measurements.OBD_INTAKE_TEMP);
				//currentIntakeTemp = bundle.getFloat(Measurements.OBD_INTAKE_TEMP);
				if(significantChange(currentIntakeTemp,MEASUREMENT_INTEMP)){
					if (tempUnit.equals(UNIT_FAHRENHEIT))  currentIntakeTemp = UnitConversion.CelsiusToFahreheit(currentIntakeTemp);
					//  txtIntakeTemp.setText(currentIntakeTemp + tempUnit);
					mTextGauge.setInTemp(stringFormatter(currentIntakeTemp+"") + tempUnit);
				}
			}
			if (type.equals(Measurements.OBD_COOLANT_TEMP)) {
				//TODO: Revert to float
				//currentCoolTemp = Float.parseFloat(bundle.getString(Measurements.OBD_COOLANT_TEMP));
				currentCoolTemp = bundle.getFloat(Measurements.OBD_COOLANT_TEMP);
				if(significantChange(currentCoolTemp,MEASUREMENT_COOLTEMP)){
					if (tempUnit.equals(UNIT_FAHRENHEIT)) currentCoolTemp = UnitConversion.CelsiusToFahreheit(currentCoolTemp);
					// txtCoolTemp.setText(currentCoolTemp + tempUnit);
					mTextGauge.setCoolTemp(stringFormatter(currentCoolTemp+"") + tempUnit);
				}
			}
			if (type.equals(OBD_Status.OBD_STATUS)) {
				String newStatus = bundle.getString(OBD_Status.OBD_STATUS);
				if (!newStatus.equals(obdStatus)) {
					obdStatus = newStatus;
					if (obdStatus.equals(OBD_Status.NO_CONNECTION)) {
						//	txtStatus.setBackgroundColor(getResources().getColor(R.color.red));
						mTextGauge.setStatus(TextGauge.NO_CONNECTION);
						resetUI();
					} else
						if (obdStatus.equals(OBD_Status.CONNECTED)) {
							//	txtStatus.setBackgroundColor(getResources().getColor(R.color.yellow));
							mTextGauge.setStatus(TextGauge.CONNECTED);
							resetUI();
						} else
							if (obdStatus.equals(OBD_Status.DATA)) {
								mTextGauge.setStatus(TextGauge.DATA);
								//	txtStatus.setBackgroundColor(getResources().getColor(R.color.green));
							}
				}
			}     	            
		}
	};

	String stringFormatter(String formatstring){
		DecimalFormat df = new DecimalFormat("#.#");
		return df.format(Double.parseDouble(formatstring));

	}

	private boolean significantChange(float value, int type){
		switch (type){
		case MEASUREMENT_RPM:
			if(Math.abs(value-mLastRPM)>THRESHOLD_RPM){
				mLastRPM = value;
				return true;
			}
			break;
		case MEASUREMENT_SPEED:
			if(Math.abs(value-mLastSpeed)>THRESHOLD_SPEED){
				mLastSpeed = value;
				return true;
			}
			break;
		case MEASUREMENT_MAF:
			if(Math.abs(value-mLastMAF)>THRESHOLD_MAF){
				mLastMAF = value;
				return true;
			}
			break;
		case MEASUREMENT_INTEMP:
			if(Math.abs(value-mLastInTemp)>THRESHOLD_TEMP){
				mLastInTemp = value;
				return true;
			}
			break;
		case MEASUREMENT_COOLTEMP:
			if(Math.abs(value-mLastCoolTemp)>THRESHOLD_TEMP){
				mLastCoolTemp = value;
				return true;
			}
			break;

		default:
			return false;
		}
		return false;
	}

	@Override
	protected void onResume() {
		IntentFilter obdFilter = new IntentFilter();
		obdFilter.addAction(OBD.BROADCAST_OBD);
		registerReceiver(obdReceiver, obdFilter);
		super.onResume();
	}


	@Override
	protected void onPause() {
		try{unregisterReceiver(obdReceiver);}catch(Exception e){e.printStackTrace();}
		try{unregisterReceiver(phoneStateReceiver);}catch(Exception e){e.printStackTrace();}
		super.onPause();
	}

	@Override
	protected void onStart(){
		super.onStart();
		/*if(mGClient==null){
			initGoogleClient();
		}*/
		checkForService();
		if(mSmartShutdown){


			if(!mResolvingError && mGClient!=null){
				mRequestType.set(REQUEST_GOOGLE_SERVICES_CONNECT);
				mGClient.connect();
			}
		}
	}

	private void checkForService(){
		LocationManager lm = null;
		Builder dialog;

		boolean gps_enabled=false,network_enabled=false;
		if(lm==null)
			lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		try{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}catch(Exception e){e.printStackTrace();}
		try{
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}catch(Exception e){e.printStackTrace();}

		if(!gps_enabled && !network_enabled){
			dialog = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK);
			dialog.setMessage(R.string.messageLocationServices);
			dialog.setPositiveButton(R.string.openSettings, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {

					Intent myIntent = new Intent( android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

					startActivity(myIntent);

				}
			});
			dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					// TODO Auto-generated method stub

				}
			});
			dialog.show();

		}
	}

	@Override
	public void onStop(){
		if(mGClient!=null){
			mRequestType.set(REQUEST_GOOGLE_SERVICES_DISCONNECT);
			ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGClient, mActivityRecognitionPendingIntent);
			mGClient.disconnect();
		}
		stopService(new Intent(this, PhoneActivityService.class));
		if(mTask!=null){
			mTask.cancel();
		}

		if(mTimer!=null){
			mTimer.cancel();
		}
		Log.v(TAG,"Services stopped");
		super.onStop();
	}

	@Override
	protected void onDestroy(){

		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mSmartShutdown){
			outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
		}
	}

	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), "errordialog");
	}

	public void onDialogDismissed() {
		mResolvingError = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(requestCode)
		{
		case PAM_RESOLVE_ERROR:
			mResolvingError = false;
			switch(resultCode)
			{
			case RESULT_OK:
				if(mGClient==null){
					initGoogleClient();
				}
				if(!mGClient.isConnecting()||!mGClient.isConnected()){
					mGClient.connect();
				}

			}
		}
	}


	@Override
	public void onConnectionFailed(ConnectionResult cr) {
		Log.v(TAG,"Connection Failed");
		mConnResult = cr;
		if(mResolvingError){
			return;
		}else if(mConnResult.hasResolution()){
			mResolvingError=true;
			try {
				mConnResult.startResolutionForResult(this, PAM_RESOLVE_ERROR);
			} catch (SendIntentException e) {
				Log.e(TAG, "Error while resolving Error");
				e.printStackTrace();
				mGClient.connect();
			}
		}else{
			showErrorDialog(mConnResult.getErrorCode());
			mResolvingError=true;
		}

	}


	@Override
	public void onConnected(Bundle connectionHint) {
		Log.v(TAG,"Google services connected");

		//Toast.makeText(this, "GoogleServices Connected",Toast.LENGTH_SHORT).show();
		startService(new Intent(this,PhoneActivityService.class));
		startUpdates();


	}





	private void startUpdates(){

		if(mGClient.isConnected()){

			mRequestType.set(REQUEST_ACTIVITY_REC_CONNECT);
			ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGClient, DETECTION_INTERVAL, mActivityRecognitionPendingIntent);

		}else{
			if(!mResolvingError && mGClient!=null){
				mGClient.connect();

			}
		}
	}


	@Override
	public void onConnectionSuspended(int cause) {
		Log.v(TAG,"Google services suspended, cause:"+cause);
		stopService(new Intent(this, PhoneActivityService.class));

	}

	public static class ErrorDialogFragment extends DialogFragment {
		public ErrorDialogFragment() { }

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode,
					this.getActivity(), PAM_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((LiveDataActivity)getActivity()).onDialogDismissed();
		}
	}

	public class PhoneStationaryDialog extends DialogFragment{


		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			Builder dialog = new AlertDialog.Builder(mContext,AlertDialog.THEME_HOLO_DARK);
			dialog.setMessage(R.string.messagePhoneStationary);
			dialog.setPositiveButton(R.string.buttonok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					LiveDataActivity.this.setResult(KioskActivity.RESULT_CODE_PHONE_IDLE);
					finish();

				}
			});
			dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			});
			return dialog.show();
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			//DO NOTHING. Android activity lifecycle takes care of handling this case
			//Toast.makeText(LiveDataActivity.this, "Dialog Dismissed", Toast.LENGTH_SHORT).show();
			//((LiveDataActivity)getActivity()).stopAllServices();
		}
	}

	protected void stopAllServices(){
		stopService(new Intent(this,PhoneActivityService.class));
	}
	
	@Override
	public void onBackPressed(){
		setResult(KioskActivity.RESULT_ACTIVITY_CLOSED);
		finish();
	}

}
