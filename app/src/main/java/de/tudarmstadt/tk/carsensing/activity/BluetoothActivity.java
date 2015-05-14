package de.tudarmstadt.tk.carsensing.activity;

import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import java.util.Set;

/**
 * Activity to select the Bluetooth OBD dongle. 
 * Doesn't discover new devices; only shows already paired devices, i.e the dongle has to be paired via the phone's system settings first! 
 * @author Julien Gedeon
 *
 */

public class BluetoothActivity extends Activity {

	private static final String TAG = "BluetoothActivity";

	private BluetoothAdapter btAdapter;
	private Set<BluetoothDevice> btDevices;
	private ArrayAdapter<String> devicesAdapter;
	private ListView lstDevices;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "OnCreate");
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_bluetooth);
		
		devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

		lstDevices = (ListView) findViewById(R.id.lstDevices);
		lstDevices.setAdapter(devicesAdapter);
		lstDevices.setOnItemClickListener(lstItemClickListener);

		// get all paired bluetooth devices
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btDevices = btAdapter.getBondedDevices();


		if(btDevices.size()==0){
			//TODO: Need to find a better way to alert user about not finding anything in the paired devices list than adding
			// to the adapter. Toast is not effective if the user missed the notification
			//devicesAdapter.add("Please pair an OBD device using 'Settings' menu to access this feature");
			//devicesAdapter.getView(0, null, null).setClickable(false);


			Log.d(TAG, "No paired devices found \n");
			Toast.makeText(this, R.string.messagenodevicesfound, Toast.LENGTH_LONG).show();
			//setResult(Activity.RESULT_CANCELED);
			//finish();
		}else{

			for (BluetoothDevice device : btDevices) {
				devicesAdapter.add(device.getName() + "\n" + device.getAddress());
			}
		}
	}

	private OnItemClickListener lstItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			// send back the address of the selected bluetooth device to the activity
			Intent intent = new Intent();
			intent.putExtra(IntentMessages.BLUETOOTH_ADDRESS, address);
			Log.d(TAG, "Selected Bluetooth device:" + address);
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};



}
