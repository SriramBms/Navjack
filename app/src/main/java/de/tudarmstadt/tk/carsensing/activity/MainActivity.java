package de.tudarmstadt.tk.carsensing.activity;

import de.tudarmstadt.tk.carsensing.R;
import java.util.Date;

import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;

import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * The app's main activity (mainly for testing/debugging purposes)
 * @author Julien Gedeon
 * 
 */

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	// UI Elements
	private ToggleButton startStopButton;
	private CheckBox chkLight;
	private CheckBox chkAccel ;
	private CheckBox chkHeading;
	private CheckBox chkMagnetic;
	private CheckBox chkSpeed;
	private CheckBox chkRPM;
	private CheckBox chkEngineLoad;
	private CheckBox chkMAF;
	private CheckBox chkCoolTemp;
	private CheckBox chkIntakeTemp;
	private CheckBox chkLocation;
	private CheckBox chkWifi;
	private CheckBox chkCellular;
	private CheckBox chkBatteryTemp;
	private CheckBox chkBatteryLevel;
	private CheckBox chkLoudness;
	private EditText txtDescription;
	private EditText txtInterval;
	private TextView txtBluetoothStatus;
	
	// OBD dongle bluetooth address
	private String btAddress = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OnCreate");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Set up UI elements
        txtDescription = (EditText) findViewById(R.id.txtDescription);
        txtDescription.clearFocus(); // prevent keyboard from showing up directly at startup
        txtDescription.setText(DateFormat.format("yyyy-MM-dd   hh:mm:ss", new Date().getTime()));    
        txtInterval = (EditText) findViewById(R.id.txtInterval);
        startStopButton = (ToggleButton) findViewById(R.id.btnStartStop);
        if (serviceRunning()) startStopButton.setChecked(true);
       
        txtBluetoothStatus = (TextView) findViewById(R.id.txtBluetoothStatus);
        txtBluetoothStatus.setTextColor(Color.RED);
        chkSpeed = (CheckBox) findViewById(R.id.chkSpeed);
        chkRPM = (CheckBox) findViewById(R.id.chkRPM);
        chkEngineLoad = (CheckBox) findViewById(R.id.chkEngineLoad);
        chkMAF = (CheckBox) findViewById(R.id.chkMAF);
        chkCoolTemp = (CheckBox) findViewById(R.id.chkCoolTemp);
        chkIntakeTemp = (CheckBox) findViewById(R.id.chkIntakeTemp);
        
        chkLocation = (CheckBox) findViewById(R.id.chkLocation);
        chkWifi = (CheckBox) findViewById(R.id.chkWifi);
        chkCellular = (CheckBox) findViewById(R.id.chkCellular);
        
        chkLight = (CheckBox) findViewById(R.id.chkLight);
        chkAccel = (CheckBox) findViewById(R.id.chkAccel);
        chkHeading = (CheckBox) findViewById(R.id.chkHeading);
        chkMagnetic = (CheckBox) findViewById(R.id.chkMagnetic);
        chkBatteryTemp = (CheckBox) findViewById(R.id.chkBatteryTemp);
        chkBatteryLevel = (CheckBox) findViewById(R.id.chkBatteryLevel);
        chkLoudness = (CheckBox) findViewById(R.id.chkLoudness);

        startStopButton.setOnClickListener(new OnClickListener() { 
            @Override
            public void onClick(View v) {
            	if (startStopButton.isChecked()) {
            		// Start the measurement service
            		Log.d(TAG, "btnStartStop -> Start Service");
            		Intent serviceIntent = new Intent(MainActivity.this, MeasurementService.class);
            		// set up a bundle that is passed to the service intent
            		Bundle bundle = new Bundle();
            		bundle.putString(IntentMessages.DESCRIPTION, txtDescription.getText().toString().trim());
            		Log.d(TAG, "Measurement Service Intent Bundle:Description=" + txtDescription.getText().toString().trim());            
            		bundle.putFloat(IntentMessages.INTERVAL, Float.valueOf(txtInterval.getText().toString()));
            		Log.d(TAG, "Measurement Service Intent Bundle:Interval=" + txtInterval.getText().toString());  
            		bundle.putBoolean(IntentMessages.COLLECT_LIGHT, chkLight.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkLight=" + chkLight.isChecked());
            		bundle.putBoolean(IntentMessages.COLLECT_ACCEL, chkAccel.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkAccel=" + chkAccel.isChecked());            		
            		bundle.putBoolean(IntentMessages.COLLECT_HEADING, chkHeading.isChecked());
            		Log.d(TAG,  "Measurement Service Intent Bundle:chkHeading=" + chkHeading.isChecked());
            		bundle.putBoolean(IntentMessages.COLLECT_MAGNETIC, chkMagnetic.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkMagnetic=" + chkMagnetic.isChecked());   
            		bundle.putBoolean(IntentMessages.COLLECT_BATTERY_LEVEL, chkBatteryLevel.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkBatteryLevel=" + chkBatteryLevel.isChecked());  
            		bundle.putBoolean(IntentMessages.COLLECT_BATTERY_TEMP, chkBatteryTemp.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkBatteryTemp=" + chkBatteryTemp.isChecked());  
            		bundle.putBoolean(IntentMessages.COLLECT_LOCATION, chkLocation.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkLocation=" + chkLocation.isChecked());             		
            		bundle.putBoolean(IntentMessages.COLLECT_WIFI, chkWifi.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkWifi=" + chkWifi.isChecked());              		
            		bundle.putBoolean(IntentMessages.COLLECT_CELLULAR, chkCellular.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkCellular=" + chkCellular.isChecked()); 
            		bundle.putBoolean(IntentMessages.COLLECT_LOUDNESS, chkLoudness.isChecked());
            		Log.d(TAG, "Measurement Service Intent Bundle:chkLoudness=" + chkLoudness.isChecked());    
            		
            		if (btAddress != "") {
            			// obd dongle was selected
            			bundle.putBoolean(IntentMessages.COLLECT_OBD, true);
                		Log.d(TAG, "Measurement Service Intent Bundle:collectOBD=true");      
            			bundle.putString(IntentMessages.BLUETOOTH_ADDRESS, btAddress);
                		Log.d(TAG, "Measurement Service Intent Bundle:btAddress=" + btAddress);      
                		bundle.putBoolean(IntentMessages.COLLECT_SPEED, chkSpeed.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkSpeed=" + chkSpeed.isChecked());  
                		bundle.putBoolean(IntentMessages.COLLECT_RPM, chkRPM.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkRPM=" + chkRPM.isChecked());    
                		bundle.putBoolean(IntentMessages.COLLECT_ENGINE_LOAD, chkEngineLoad.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkEngineLoad=" + chkEngineLoad.isChecked());    
                		bundle.putBoolean(IntentMessages.COLLECT_MAF, chkMAF.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkMAF=" + chkMAF.isChecked());    
                		bundle.putBoolean(IntentMessages.COLLECT_COOLANT_TEMP, chkCoolTemp.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkCoolTemp=" + chkCoolTemp.isChecked());    
                		bundle.putBoolean(IntentMessages.COLLECT_INTAKE_TEMP, chkIntakeTemp.isChecked());
                		Log.d(TAG, "Measurement Service Intent Bundle:chkIntakeTemp=" + chkIntakeTemp.isChecked());               		
            		}      		
            		
            		serviceIntent.putExtras(bundle);
            		startService(serviceIntent);
            	} else {
            		// Stop the measurement service
            		Log.d(TAG, "btnStartStop -> Stop Service");
            		stopService(new Intent(MainActivity.this, MeasurementService.class));
            	}
            }
        });
 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
         Intent intent;
        switch (item.getItemId())
        {
        	case R.id.mnuLiveData:
        		Log.d(TAG, "Menu: mnuLiveData");
    			intent = new Intent(this, LiveDataActivity.class);
    			startActivity(intent);
    			return true;
        	case R.id.mnuSelectBluetooth:
        		Log.d(TAG, "Menu: mnuConnectBluetooth");       		
        		intent = new Intent(this, BluetoothActivity.class);
        		startActivityForResult(intent, 1);
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	  if (requestCode == 1) {
    	     if(resultCode == RESULT_OK){      
    	         btAddress = data.getStringExtra(IntentMessages.BLUETOOTH_ADDRESS);
    	         Log.d(TAG, "Got dongle address back from bluetooth activity:" + btAddress);
    	         txtBluetoothStatus.setText("(device selected)");
    	         txtBluetoothStatus.setTextColor(Color.GREEN);
    	     }
    	     if (resultCode == RESULT_CANCELED) {    
    	    	 // bt selection cancelled
    	     }
    	  }
    }
    
    
    @Override
    public void onDestroy() {
	    Log.d(TAG, "OnDestroy");
    	super.onDestroy();
    }
    
    
    private boolean serviceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MeasurementService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
}
