package de.tudarmstadt.tk.carsensing.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import de.tudarmstadt.tk.carsensing.settings.Settings;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Class to handle the Bluetooth Connection to the OBD dongle
 * @author Julien Gedeon
 *
 */

public class BluetoothService {

	private static final String TAG = "BluetoothService";
	private static final UUID BT_UUID = UUID.fromString(Settings.BLUETOOTH_UUID);
   
	private Handler handler;
	private boolean connected;
	private Context context;
	
	private BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;
	private BluetoothSocket btSocket;
	private BluetoothConnection btConnection;
	private String btAddress;

	/**
	 * Constructor without bluetooth device (the searchOBDDongle() function will be called to search for a paired dongle) 
	 * @param context
	 * @param handler
	 */
	public BluetoothService(final Context context, final Handler handler) {
		this.handler = handler;
		this.context = context;
		connected = false;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btAddress = searchOBDDongle();
	}
	
	
	/**
	 * Constructor with bluetooth device
	 * @param context
	 * @param handler
	 * @param device
	 */
	public BluetoothService(final Context context, final Handler handler, final String device) {
		this.handler = handler;
		this.context = context;
		connected = false;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btAddress = device;
	}
	
	
	public boolean isConnected() {
		return connected;
	}
	
	
	 /**
	  * Connect the dongle
	  */
	public void connect()  {
		Log.d(TAG, "BT State-"+btAdapter.getState());
		Log.d(TAG, "Connecting to Bluetooth device " + btAddress);
	    try {
		    btAdapter.cancelDiscovery();
		    if(btAddress!=null){
	    	btDevice = btAdapter.getRemoteDevice(btAddress);
		    }else{
		    	Log.e(TAG,"No valid bluetooth devices were found. btAddress=null");
		    	connected = false;
		    	return;
		    }
			btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID);
			try{
		    btSocket.connect();
			}catch(IOException e){
				Log.e(TAG, "IOException: Could not connect to BT device");
				e.printStackTrace();
			}
		    // Start the connection thread
		    btConnection = new BluetoothConnection(btSocket);
		    btConnection.start();
		    Log.d(TAG, "Connected to Bluetooth device" + btAddress);
		    connected = true;
		} catch (IOException e) {
			Log.d(TAG, "Could not connect to Bluetooth device " + btAddress);
			e.printStackTrace();
			connected = false;
		}
	}
	
	/**
	 * The public interface to send messages to the dongle
	 * @param message
	 */
	public void send(final String message) {
		if (this.isConnected()) {
			byte[] bytesToSend = message.getBytes();
			//Log.d(TAG, "Sending Bluetooth message:" + message);
			btConnection.write(bytesToSend);
		}
	}
	
	
	/**
	 * Disconnect the dongle
	 */
	public void disconnect() {
		if (this.isConnected()) {
			Log.d(TAG, "Disconnecting Dongle");
			try {
				btSocket.close();
				btConnection.cancel();
				btConnection.interrupt();
				btConnection = null;
			} catch (IOException e) {
				Log.d(TAG, "Error closing Bluetooth socket");
				e.printStackTrace();
			} finally {
				connected = false;
			}
		}
	}

	/**
	 * Searches for paired dongles (any device that contains "OBD" in its name)
	 * @return bluetooth address of the dongle
	 */
	private String searchOBDDongle() {
		Log.d(TAG, "Searching for paired OBD Dongles");
		Set<BluetoothDevice> btDevArray;
		btDevArray = btAdapter.getBondedDevices();
		if( btDevArray.size() > 0 )
		{
			//there are paired bluetooth devices
			for(BluetoothDevice btDev:btDevArray )
			{
				/* check the name for the string "OBD" */
				if( btDev.getName().contains("OBD") )
				{
					Log.d(TAG, "Found OBD Dongle with Address:" + btDev.getAddress());
					return btDev.getAddress();
				}
			}
		}
		Log.d(TAG, "No OBD dongle found");
		return null;
	}
	
	/**
	 *  Returns the name of the connected OBD device
	 *  
	 */
	
	public String getDeviceName(){
		return btAddress;
	}
	
	
	/**
	 * The Connection Thread for sending and receiving data
	 *
	 */
	private class BluetoothConnection extends Thread {
		private final BluetoothSocket btSocket;
	    private final InputStream inStream;
	    private final OutputStream outStream;
		private boolean ready;
		
	    public BluetoothConnection(BluetoothSocket socket) {
	            Log.d(TAG, "BluetoothConnection Thread started");
	            btSocket = socket;
	            InputStream tmpIn = null;
	            OutputStream tmpOut = null;

	            try {
	                tmpIn = socket.getInputStream();
	                tmpOut = socket.getOutputStream();
	            } catch (IOException e) {
	                Log.e(TAG, "Could not create Temp sockets");
	            }

	            inStream = tmpIn;            
	            outStream = tmpOut;
	            ready = true;
	        }
	        String msg ="";
	        
	        public void run() { 
	            while (connected) {
	                try { 
	                  byte[] buffer = new byte[1];
	                    int bytes = inStream.read(buffer, 0, buffer.length);
	                    //Log.d(TAG, "Read " + bytes + "of data");
	                    String in = new String(buffer);
            			if (Character.getNumericValue(in.charAt(0)) != -1) {
            				msg = msg + in;
            			}
            			if (in.charAt(0) == 0x3e) { // The famous ">" character
                        	//Log.d(TAG, "Received Bluetooth message:" + msg);
                        	handler.obtainMessage(IntentMessages.BLUETOOTH_RECEIVE, buffer.length, -1, msg).sendToTarget();
                        	msg="";
                        	ready = true;
            			}               	
	                } catch (Exception e) {
	                	Log.d(TAG, "Exception in Bluetooth Thread");
	                	connected=false;
	                	disconnect();
	                	e.printStackTrace();	                	
	                }
	            }           
	        }       
	       
	        public void write(byte[] buffer) {
	        	try {
	                if (ready) {
		                //Log.d(TAG, "Write message to Bluetooth:" + new String(buffer));
		                ready = false;
		                outStream.flush();
		                outStream.write(buffer);
		            	outStream.flush();
		                handler.obtainMessage(IntentMessages.BLUETOOTH_SEND, -1, -1, buffer).sendToTarget();
	                }

	            } catch (Exception e) {
	                Log.e(TAG, "Exception during write", e);
	                e.printStackTrace();
	                disconnect();
	            }
	        }

	        public void cancel() {
	            try {
	                btSocket.close();
		            Log.d(TAG, "BluetoothConnection Thread stopped");
	            } catch (IOException e) {
	                Log.e(TAG, "Close of connect socket failed");
	            }
	        }
	    }

}
