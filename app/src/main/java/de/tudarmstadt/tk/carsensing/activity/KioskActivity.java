package de.tudarmstadt.tk.carsensing.activity;

import java.util.concurrent.Executor;

import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService.ServiceBinder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * The kiosk activity that is displayed when the boot is completed.
 * @author Julien Gedeon
 *
 */

public class KioskActivity extends Activity {

	private static final String TAG = "KioskActivity";
	private static final int REQUEST_CODE = 1;
	public static final int RESULT_CODE_PHONE_IDLE = 2;
	private static final int REQUEST_CODE_BT = 3;
	public static final int RESULT_ACTIVITY_CLOSED = 4;
	private ImageButton btnLiveData;
	private ImageButton btnNavigation;
	private ImageButton btnSettings;
	private ImageButton btnBt;
	ProgressDialog pd=null;
	private String btAddress = "";
	//private boolean waitingForService = false;
	private boolean mServiceConnected = false;
	Executor ldExecutor;
	MeasurementService mMeasurementService;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "OnCreate");
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Log.d(TAG, "Starting the Measurement Service");
		//final Intent serviceIntent = new Intent(this, MeasurementService.class);
		//Bundle bundle = new Bundle();
		//bundle.putFloat(IntentMessages.INTERVAL, 25.0f);
		//serviceIntent.putExtras(bundle);
		/*
		new AsyncTask<Integer, Long, Boolean>(){

			@Override
			protected Boolean doInBackground(Integer... params) {
				final Intent serviceIntent = new Intent(KioskActivity.this, MeasurementService.class);
				Bundle bundle = new Bundle();
				bundle.putFloat(IntentMessages.INTERVAL, 25.0f);
				serviceIntent.putExtras(bundle);
				KioskActivity.this.startService(serviceIntent);
				return true;
			}

		}.execute();
		 */
		final Intent serviceIntent = new Intent(KioskActivity.this, MeasurementService.class);
		//Bundle bundle = new Bundle();
		//bundle.putFloat(IntentMessages.INTERVAL, 25.0f);
		//serviceIntent.putExtras(bundle);
		startService(serviceIntent);
		Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
			setContentView(R.layout.activity_kiosk_portrait);
		}else{
			setContentView(R.layout.activity_kiosk_landscape);
		}
		activateUI();
		//connectToOBDDevice();
	}

	private ServiceConnection mConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ServiceBinder binder = (ServiceBinder)service;
			mMeasurementService = binder.getService();
			mServiceConnected = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mServiceConnected = false;

		}

	};

	

	void activateUI(){
		btnLiveData = (ImageButton) findViewById(R.id.btnLiveData);
		btnNavigation = (ImageButton) findViewById (R.id.btnNavigation);
		btnSettings = (ImageButton) findViewById(R.id.btnSettings);
		btnBt = (ImageButton) findViewById(R.id.btnBT);
		btnLiveData.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "Clicked Live Data Button");
				/*connectToOBDDevice();
				Intent intent = new Intent(KioskActivity.this, LiveDataActivity.class);
				startActivityForResult(intent,REQUEST_CODE);*/

				if(btAddress == ""){
					Intent intent = new Intent(KioskActivity.this, BluetoothActivity.class);
					startActivityForResult(intent, REQUEST_CODE_BT);
				}else{
					/*pd = new ProgressDialog(KioskActivity.this, ProgressDialog.STYLE_SPINNER);
					pd.setIndeterminate(true);
					pd.setMessage("Configuring device. Please wait");
					pd.setCancelable(false);
					pd.setCanceledOnTouchOutside(false);
					pd.show();
					mMeasurementService.setOBDStatus(true,btAddress);
					
					Intent intent = new Intent(KioskActivity.this, LiveDataActivity.class);

					startActivityForResult(intent,REQUEST_CODE);*/
					final LinearLayout progresstxt = (LinearLayout)findViewById(R.id.progresstextcontainer);
					
					
					new AsyncTask<Integer, String, String> (){

					     

					     

					     protected void onPreExecute(){

					    	 /*pd = new ProgressDialog(KioskActivity.this, ProgressDialog.STYLE_SPINNER);
								pd.setIndeterminate(true);
								pd.setMessage("Configuring device. Please wait");
								pd.setCancelable(false);
								pd.setCanceledOnTouchOutside(false);
								pd.show();*/
								progresstxt.setVisibility(View.VISIBLE);
								progresstxt.bringToFront();
								
					     }

					     protected void onPostExecute(String result) {
					        /* pd.dismiss();*/
					         progresstxt.setVisibility(View.GONE);
					     }

						@Override
						protected String doInBackground(Integer... params) {
							KioskActivity.this.runOnUiThread(new Runnable(){

								@Override
								public void run() {
									mMeasurementService.setOBDStatus(true,btAddress);
									
								}
								
							});
							
							Intent intent = new Intent(KioskActivity.this, LiveDataActivity.class);
							startActivityForResult(intent,REQUEST_CODE);
							return null;
						}
					 }.execute();
					
				}
				
				
				


			}
		});

		btnNavigation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "Clicked Navigation Button");
				try{
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=")); 
					startActivity(intent);
				}catch(ActivityNotFoundException e){
					Log.e(TAG, "Maps Not Installed");
					Toast.makeText(KioskActivity.this, R.string.messagemapsnotinstalled, Toast.LENGTH_SHORT).show();
				}

			}
		});

		btnSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Settings.ACTION_SETTINGS));

			}

		});

		btnBt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(KioskActivity.this, BluetoothActivity.class);
				startActivityForResult(intent, REQUEST_CODE_BT);

			}

		});

	}

	private void connectToOBDDevice(){

		final Intent obdServiceIntent = new Intent(KioskActivity.this, MeasurementService.class);
		stopService(new Intent(KioskActivity.this, MeasurementService.class));
		// set up a bundle that is passed to the service intent
		Bundle bundle = new Bundle();


		if(btAddress == ""){
			Toast.makeText(this, R.string.messageconnectobddevice, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, BluetoothActivity.class);
			startActivityForResult(intent, REQUEST_CODE_BT);
		}else{




			// obd dongle was selected
			bundle.putBoolean(IntentMessages.COLLECT_OBD, true);
			bundle.putFloat(IntentMessages.INTERVAL, 25.0f);
			bundle.putString(IntentMessages.BLUETOOTH_ADDRESS, btAddress);
			/*
        		bundle.putBoolean(IntentMessages.COLLECT_SPEED, true);

        		bundle.putBoolean(IntentMessages.COLLECT_RPM, true);

        		bundle.putBoolean(IntentMessages.COLLECT_ENGINE_LOAD, true);

        		bundle.putBoolean(IntentMessages.COLLECT_MAF, true);

        		bundle.putBoolean(IntentMessages.COLLECT_COOLANT_TEMP, true);

        		bundle.putBoolean(IntentMessages.COLLECT_INTAKE_TEMP, true);
			 */



		}
		obdServiceIntent.putExtras(bundle);

		//TODO
		/*
		new AsyncTask<Integer, Long, Boolean>(){

			@Override
			protected Boolean doInBackground(Integer... params) {
				KioskActivity.this.startService(obdServiceIntent);
				return true;
			}

		}.execute();
		 */
		KioskActivity.this.startService(obdServiceIntent);


	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_kiosk, menu);
		return true;
	}

	@Override
	public void onBackPressed(){
		return;
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		switch (item.getItemId())
		{
		case R.id.mnuDev:
			//Log.d(TAG, "Menu MainActivity");
			intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == REQUEST_CODE){
			if(resultCode == RESULT_CODE_PHONE_IDLE || resultCode == RESULT_ACTIVITY_CLOSED){
				//stopService(new Intent(this,MeasurementService.class));
				//finish();
				if(pd != null){
					pd.dismiss();
					pd = null;
				}
				mMeasurementService.setOBDStatus(false, null);
			}

		}

		if(requestCode == REQUEST_CODE_BT){
			if(resultCode == RESULT_OK){      
				btAddress = data.getStringExtra(IntentMessages.BLUETOOTH_ADDRESS);
				Log.d(TAG, "Got dongle address back from bluetooth activity:" + btAddress);
				/*
				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putInt("messageType",MeasurementService.SENSOR_ENABLE_OBD);
				bundle.putString("btAddress", btAddress);
				msg.setData(bundle);
				 */

				//connectToOBDDevice();
			}
			if (resultCode == RESULT_CANCELED) {    
				// bt selection cancelled
				Toast.makeText(this, R.string.messageconnectobddevice, Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);


		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.activity_kiosk_landscape);


		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			setContentView(R.layout.activity_kiosk_portrait);

		}
		activateUI();
	}


	@Override
	protected void onStart(){
		super.onStart();
		Intent intent = new Intent(this, MeasurementService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop(){
		super.onStop();
		if(mServiceConnected){
			unbindService(mConnection);
			mServiceConnected = false;
		}
	}
	@Override
	public void onResume(){
		//connectToOBDDevice();
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MeasurementService.actionServiceStarted);
		

	}

}
