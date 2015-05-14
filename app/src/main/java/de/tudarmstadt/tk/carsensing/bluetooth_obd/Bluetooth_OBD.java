package de.tudarmstadt.tk.carsensing.bluetooth_obd;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * New version of John's bluetooth code (not currently used)
 *
 */
public class Bluetooth_OBD {
	
	Set<BluetoothDevice>		btDevArray;
	ArrayList<BluetoothDevice>	btDevices;
	String						TAG = "BT_OBD";
	BluetoothAdapter 			btAdapter;
	LinkedBlockingQueue<String> blkqBtResponse;
	BroadcastReceiver			receiver;
	BluetoothSocket				btSock;
	public static final UUID	MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	Context						curContext;
	Handler						mHandler;
	ConnectedThread				ctListener;
	boolean						bConnected = false;
	boolean						bEcho = false;
	protected static final int	SUCCESS_CONNECT = 0;
	protected static final int	MESSAGE_READ = 1;
	
	/* constructor */
	public Bluetooth_OBD(Context PassedContext,boolean bBlock)
	{
		initVars(PassedContext);
		getPairedDevices();
		//initConnectionToDongle(bBlock);
	}
	
	/* another constructor */
	public Bluetooth_OBD(String btDevName,Context PassedContext,boolean bBlock)
	{
		/* this constructor will automatically open a connection
		 * to the device specified by btDevName, if it is paired
		 * to the 
		 */
		initVars(PassedContext);
		getPairedDevices();
		//initConnectionToDongle(bBlock);
	}
	
	/* send a string to the device */
	public void SendCommand(String sCmd)
	{
		ctListener.write(sCmd.getBytes());
	}
	
	/* call this method if you're not sure what the state
	 * of the dongle is, and want to wait for a response from
	 * it based on new stuff that you are sending
	 */
	public void ClearResponseBuffer()
	{
    	blkqBtResponse.clear();
	}
	
    /* this method returns when the bluetooth/obd dongle
     * has received the ">" character - THIS METHOD SHOULD
     * ONLY BE CALLED WHEN YOU ARE EXPECTING TO GET
     * SOMETHING BACK FROM THE DONGLE - otherwise it could
     * block for iTimeoutMs. When a response including the ">"
     * character is received from the dongle, this method
     * will return it. The parameter iTimeoutMs is the timeout
     * value in milliseconds.
     */
    public String WaitForReady(int iTimeoutMs)
    {
    	String 	sResponse = "";
    	
    	Log.v(TAG,"in waitForReady");
		Log.v(TAG,"Trying poll");
		/* poll the queue for iTimeoutMs ms until something shows up. */
		try{
    		sResponse = blkqBtResponse.poll(iTimeoutMs, TimeUnit.MILLISECONDS );
    	}
    	catch(InterruptedException E){
    		/* do nothing for now */
    		Log.v(TAG,"interrupted exception");
    	}
    	
		if( sResponse == "" + '\0' )
		{
			/* the polling failed */
			return "";
		}
		else
		{
			return sResponse;
		}
    }
	
    /* this function opens a socket to the bluetooth OBD dongle. 
     * The only requirement is that the dongle is paired - that's it. 
     * If it doesn't exist, then an exception will occur. No discovery.
     */
	public void InitConnectionToDongle(boolean bBlock) {
		BluetoothDevice	btFoundDev=null;
		
		btDevArray = btAdapter.getBondedDevices();
		if( btDevArray.size() > 0 )
		{
			//there are paired bluetooth devices
			for(BluetoothDevice btDev:btDevArray )
			{
				/* check the name for the string "OBD" */
				if( btDev.getName().contains("OBD") )
				{
					/* if this one has obd in the string, that's
					 * the one we want.
					 */
					btFoundDev = btDev;
					break;
				}
			}
		}
		else
		{
			/* if there are no paired devices, we're done I guess */
			return;
		}
		
		/* if we're here, we have a bluetooth device - so hop to it!! */
		/* cancel discovery */
		if(btAdapter.isDiscovering() )
		{
			btAdapter.cancelDiscovery();
		}
		/* start a connection */
		ConnectThread connect = new ConnectThread(btFoundDev,5);
		if( bBlock )
		{
			connect.run();
		}
		else
		{
			connect.start();
		}
	}
    
	/* call this sucker right at the beginning of constructor */
	private void initVars(Context PassedContext)
	{
		curContext = PassedContext;
		btAdapter = BluetoothAdapter.getDefaultAdapter(); 
		btDevices = new ArrayList<BluetoothDevice>();
		blkqBtResponse = new LinkedBlockingQueue<String>();
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.v(TAG, "in onReceive");
				String action = intent.getAction();
				
				if( BluetoothDevice.ACTION_FOUND.equals(action) )
				{
					Log.v(TAG, "onReceive - DEVICE FOUND");
					//a bluetooth device was found
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					btDevices.add(device);
				}
				else if( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action) )
				{
					//the adapter started discovery
					Log.v(TAG, "onReceive - DISCOVERY STARTED");
				}
				else if( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) )
				{
					Log.v(TAG, "onReceive - DISCOVERY FINISHED");
				}
				else if( BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) )
				{
					//the adapter's state changed - make sure the adapter hasn't been turned
					//off or something
					Log.v(TAG, "onReceive - STATE CHANGED");
					if( btAdapter.getState() == BluetoothAdapter.STATE_OFF )
					{
						//do something if bluetooth has been turned off
					}
					
				}
				else
				{
					Log.v(TAG, "onReceive - UNKNOWN EVENT");
				}
				Log.v(TAG, "end onReceive");
			} /* end onReceive method for broadcastreceiver */
		}; /* end broadcastreceiver */
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		curContext.registerReceiver(receiver,filter);
		IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		curContext.registerReceiver(receiver,filter2);
		IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		curContext.registerReceiver(receiver,filter3);
		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		curContext.registerReceiver(receiver,filter4);
		/* object to handle events in the bluetooth threads */
		mHandler = new Handler()
		{
			String sParsedMsg = "";
			/* this is kinda hacky, and to make this more extensible you'd
			 * want a way to set this thing via a passable param, but for
			 * now, et viola.
			 */
			BlockingQueue<String> blkqResponseListener = blkqBtResponse;
			
			@Override
			public void handleMessage(Message msg)
			{
				Log.v(TAG, "in handleMessage");
				super.handleMessage(msg);
				switch(msg.what){
				case SUCCESS_CONNECT:
					Log.v(TAG, "handleMessage - SUCCESS_CONNECT");
					btSock = (BluetoothSocket)msg.obj;
					ctListener = new ConnectedThread(btSock);
					ctListener.start();
					String sCon = "AT Z" + '\r';
					ctListener.write(sCon.getBytes());
					break;
				case MESSAGE_READ:
					Log.v(TAG, "handleMessage - MESSAGE_READ");
					byte[] readBuf = (byte[])msg.obj;
					String sMsg = (new String(readBuf)).substring(0,msg.arg1);
					Log.v(TAG, "message: " + sMsg);
					btEcho(sMsg);
					parseElm327Message(sMsg);
					break;
				default:
					Log.v(TAG, "handleMessage - DEFAULT");
					break;
				}
				Log.v(TAG, "end handleMessage");
			} /* end of handleMessage method */
			
			private void btEcho(String sMsg) {
				/* if echo is set to true, echo the received chars
				 * back - useful for debugging and not much else
				 */
				if( bEcho )
				{
					ctListener.write( sMsg.getBytes() );
				}
			}

			void parseElm327Message(String sNewMsg)
			{
				sParsedMsg = sParsedMsg + sNewMsg;
				if( sParsedMsg.contains(">") )
				{
					Log.v(TAG, "whole message: " + sParsedMsg);
					Log.v(TAG, "adding message to blocking queue");
					blkqResponseListener.add(sParsedMsg);
					Log.v(TAG, "message added to blocking queue");
					sParsedMsg = "";
				}
				
			}
		}; /* end of new handler class */
	}
	
	private void getPairedDevices() {
		Log.v(TAG, "in getPairedDevices");
		btDevArray = btAdapter.getBondedDevices();
		if( btDevArray.size() > 0 )
		{
			//there are paired bluetooth devices
			for(BluetoothDevice btDev:btDevArray )
			{
				//iterate through all pair devices and add them to our list
				btDevices.add(btDev);
			}
		}
		Log.v(TAG, "end getPairedDevices");
	}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device, int iMaxConAttempts) {
	    	Log.v(TAG, "in ConnectThread constructor");
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	        int iNumConAttempts = 0;
	        
	        //listAdapter.add("trying rfcomm");
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        // Try a couple times if it fails, and the caller wants.
	        while( (iNumConAttempts < iMaxConAttempts) && (tmp==null) )
	        {
		        try 
		        {
		        	Log.v(TAG, "ConnectThread constructor - attempting rfcomm socket");
		            // MY_UUID is the app's UUID string, also used by the server code
		            tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
		            if( tmp == null )
		            {
		            	Log.v(TAG, "ConnectThread constructor - tmp is null");
		            }
		        } 
		        catch (IOException e) 
		        { 
		        	Log.v(TAG, "ConnectThread constructor - rfcomm socket failed");
		        	Log.v(TAG, "Message: " + e.getMessage());
		        }
		        iNumConAttempts++;
	        }
	        
	        mmSocket = tmp;
	        //listAdapter.add("rfcomm success");
	        Log.v(TAG, "ConnectThread contructor - rfcomm success");
	        Log.v(TAG, "end ConnectThread constructor");
	    } /* end constructor */
	 
	    public void run() {
	    	Log.v(TAG, "in ConnectThread run");
	    	//listAdapter.add("trying connect");
	        // Cancel discovery if it's occuring so we don't slow the connection
	    	if( btAdapter.isDiscovering() )
	    	{
	    		Log.v(TAG, "ConnectThread run - canceling discovery");
	    		btAdapter.cancelDiscovery();
	    	}
	    	
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	        	Log.v(TAG, "ConnectThread run - attempting to connect socket");
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	        	Log.v(TAG, "ConnectThread run - unable to connect socket");
	        	Log.v(TAG, "Message: " + connectException.getMessage());
	            try {
	            	Log.v(TAG, "ConnectThread run - trying to close socket");
	                mmSocket.close();
	            } catch (IOException closeException) { 
	            	Log.v(TAG, "ConnectThread run - unable to close socket");
	            }
	            return;
	        }
	 
	        //listAdapter.add("connect success");
	        Log.v(TAG, "ConnectThread run - invoking handler");	
	        // Do work to manage the connection (in a separate thread)
	        mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
	        Log.v(TAG, "end ConnectThread run");
	    } /* end run method */

		/** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}	
	
	public class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
		        try {
		            tmpIn = socket.getInputStream();
		            tmpOut = socket.getOutputStream();
		        } catch (IOException e) { }
	        
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        
	        Log.v(TAG, "in ConnectedThread run");
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	            	Log.v(TAG, "ConnectedThread run - listening for stuff");
	            	buffer = new byte[1024];
	                bytes = mmInStream.read(buffer);
	                Log.v(TAG, "ConnectedThread run - read something");
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	        Log.v(TAG, "end ConnectedThread run");
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	        	Log.v(TAG, "ConnectedThread write - writing bytes");
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	    	Log.v(TAG, "ConnectedThread cancel");
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	

}