package de.tudarmstadt.tk.carsensing.bluetooth;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Initial version of John's Bluetooth code (not currently used)
 * 
 *
 */

public class BluetoothJohn  {

	ArrayAdapter<String>		listAdapter;
	ArrayAdapter<String>		rspAdapter;
	Button						connectNew;
	Button						sendReset;
	Button						sendPrep;
	Button						sendBasicCmd;
	ListView					listView;
	ListView					listView1;
	BluetoothAdapter			btAdapter;
	Set<BluetoothDevice>		btDevArray;
	ArrayList<String>			pairedDevices;
	ArrayList<BluetoothDevice>	btDevices;
	IntentFilter				filter;
	Handler						mHandler;
	BroadcastReceiver			receiver;
	BluetoothSocket				btSock;
	protected static final int	SUCCESS_CONNECT = 0;
	protected static final int	MESSAGE_READ = 1;
	//public static final UUID	MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B");
	public static final UUID	MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private static final String	TAG = "BluetoothJohn";
	ConnectedThread				ctListener;
	boolean						bConnected = false;
	boolean						bEcho = false;
	
	BlockingQueue<String>		blkqBtResponse;
	
	/* parse return errors */
	protected static final int	OBD_STS_GOOD = 0;
	protected static final int	NO_CONN = 1;
	protected static final int	OBD_ERR = 2;
	protected static final int	OBD_UNSUPPORTED_MODE = 3;
	protected static final int	ELM327_ERR = 4;
	
	/* send errors */
	protected static final int	SEND_STS_GOOD = 0;
	/* NO_CONN = 1 - same as above */
	
	private Context context;
	private Handler handler;
	
	
	public BluetoothJohn(final Context context, final Handler handler) {
		this.context = context;
		this.handler = handler;
		init();	//set up variables
		if( btAdapter == null )
        {
        
        	Log.v(TAG, "onCreate, device doesn't have bluetooth");
        	
        }
        else
        {
        	//if the device has bluetooth, make sure it is enabled
        	if( btAdapter.isEnabled() == false )
        	{
        		warnUserToTurnOnBluetooth();
        	}
        	Log.v(TAG, "onCreate, device has bluetooth");
        	getPairedDevices();
        	/* make this phone discoverable during discover */
        	/*
        	Intent discoverableIntent = new
        	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        	startActivity(discoverableIntent);
        	*/
        	/* continue */
        	startDiscovery();
        }
		
	}
	
	
	
 
	private void startDiscovery() {
		Log.v(TAG, "in startDiscovery");
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
		Log.v(TAG, "end startDiscovery");
	}

	private void warnUserToTurnOnBluetooth() {
//		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//		startActivityForResult(intent,1);
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
				listAdapter.add( btDev.getName() + " (Paired)" + "\n" + btDev.getAddress() );
				pairedDevices.add( btDev.getName() );
			}
		}
		Log.v(TAG, "end getPairedDevices");
	}


	
    private void init() {
		/* initialize variables to correspond to their
		 * actual GUI counterparts
		 */
    	Log.v(TAG, "in init");
	
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		/* set up array of pair devices so we know which available devices are paried */
		pairedDevices = new ArrayList<String>();
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		btDevices = new ArrayList<BluetoothDevice>();
		/* intialize the blockingqueue object that will be used to sync the threads */
		blkqBtResponse = new LinkedBlockingQueue<String>();
		/* object to receive events from the kernel */
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
					
					String s = "";
					for(int a=0; a<pairedDevices.size();a++ )
					{
						if( device.getName().equals(pairedDevices.get(a)) )
						{
							s = s + " (Paired)";
							break;
						}
					}
					listAdapter.add( device.getName() + s + "\n" + device.getAddress() );
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
						warnUserToTurnOnBluetooth();
					}
					
				}
				else
				{
					Log.v(TAG, "onReceive - UNKNOWN EVENT");
				}
				Log.v(TAG, "end onReceive");
			} /* end onReceive method for broadcastreceiver */
		}; /* end broadcastreceiver */
		/* */
		/* filters to funnel events to the broadcast receiver */
//		registerReceiver(receiver,filter);
//		IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//		registerReceiver(receiver,filter2);
//		IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//		registerReceiver(receiver,filter3);
//		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//		registerReceiver(receiver,filter4);
		/* end filters */
		
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
					rspAdapter.add("Connected");
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
					//rspAdapter.add(sParsedMsg);
					//parseReturnMessage(sParsedMsg);
					Log.v(TAG, "adding message to blocking queue");
					blkqResponseListener.add(sParsedMsg);
					Log.v(TAG, "message added to blocking queue");
					sParsedMsg = "";
				}
				
			}
		}; /* end of new handler class */
		/* */
		Log.v(TAG, "end init");
	}/* end of init method */
    
    
    /* this method returns when the bluetooth/obd dongle
     * has received the ">" character - THIS METHOD SHOULD
     * ONLY BE CALLED WHEN YOU ARE EXPECTING TO GET
     * SOMETHING BACK FROM THE DONGLE - otherwise it could
     * block indefinitely. When a response including the ">"
     * character is received from the dongle, this method
     * will return it.
     */
    public String waitForReady()
    {
    	String 	sResponse = "";
    	
    	Log.v(TAG,"in waitForReady");
    	
    	/* clear the blocking queue at first - maybe?
    	 * We'll assume that if there's stuff in there
    	 * and nobody was waiting for it, it's not
    	 * horribly important - like the initial string that
    	 * the ELM327 module sends when it first boots up, etc.
    	 */
    	blkqBtResponse.clear();
    	Log.v(TAG,"blocking queue cleared");
    	
		Log.v(TAG,"Trying poll");
		/* poll the queue for 10000 ms until something shows up. */
		try{
    		sResponse = blkqBtResponse.poll(10000, TimeUnit.MILLISECONDS );
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
    
    
    /* this method is used to send a command to 
     * the OBD bluetooth dongle, ONCE EVERYTHING
     * IS ALREADY SET UP. If a connection has not
     * been established, or this fails for some
     * reason, indicate that in the return code.
     * NOTE: THE LAST CHARACTER IN YOUR COMMAND MUST
     * BE A LINE FEED, IE, '\r'. Ah heck, i'll just
     * check it for ya, and add it if you messed up.
     */
    public int sendElm327Cmd(String sCmd)
    {
		Log.v(TAG, "in sendElm327Cmd");
		if( bConnected = false )
		{
			Log.v(TAG,"Returning - no device to send cmd to");
			return NO_CONN;
		}
		else
		{
			/* first, make sure the last character is a newline */
			if( sCmd.charAt(sCmd.length() - 1) != '\r' )
			{
				sCmd = sCmd + '\r';
			}
			else
			{
				ctListener.write(sCmd.getBytes());
			}
		}
		
		Log.v(TAG,"Command sent - returning");
		return 0;
    }
    
    
    /* 
     * this method receives input from the ELM327 and 
     * figures out how to turn it into something useful
     * TLDR - meat, meet potatoes
     */
    public int parseReturnMessage(String sMsg)
    {
    	Log.v(TAG, "in parseReturnMessage");
    	/* in this error case, the ELM327 module
    	 * was unable to connect to the car's 
    	 * computer - in all likelyhood the car is
    	 * off, but the ELM327/BT dongle still has power.
    	 * Actually, this could be a useful test to determine
    	 * whether or not the car is on, now that I think
    	 * about it. 
    	 */
    	if( sMsg.contains("UNABLE TO CONNECT") )
    	{
    		rspAdapter.add("CONN ERR");
    		Log.v(TAG, "parseReturnMessage, unable to connect");
    		return NO_CONN;
    	}
    	else if( sMsg.contains("AT") )
    	{
    		/* for now just return in this case - at this point we'll
    		 * assume that all AT commands that get a response "just work", 
    		 * although eventually we may wish to actually confirm this.
    		 */
    		Log.v(TAG, "parseReturnMessage, at command");
    		return OBD_STS_GOOD;
    	}
    	else if( sMsg.contains("4") )
    	{
    		/* we issued an OBD - type command, and apparently got a
    		 * matching response (when you issue an OBD command, the
    		 * response contains 4X, where X is the mode page the command
    		 * belongs to. So, start by chopping off everything before
    		 * the 4, since it's either just an echo or not terribly
    		 * relevant, like the string "SEARCHING..." which sometimes
    		 * shows up in the responses.
    		 */
    		String sRsp = sMsg.substring( sMsg.indexOf("4") );
    		Log.v(TAG,"parseReturnMessage, obd-type response");
    		Log.v(TAG,"sRsp: " + sRsp);
    		/* at this point, the substring is something of the
    		 * general form "4X YY ZZ ZZ..." where Y is the mode
    		 * page that was requested, YY is the PID requested
    		 * from that mode page, and ZZ.... is the data the
    		 * car's computer send back. At this point we will
    		 * filter by mode page and PID because that determines
    		 * the format of the responses. For the time being, 
    		 * each individual PID and mode page filter can look
    		 * for multiline responses on its own - it probably won't
    		 * make a difference for what we're trying to do here.
    		 */
    		if( sRsp.charAt(1) == '1')
    		{
    			/* fall into this case for mode 1 */
    			Log.v(TAG,"parseReturn - mode 1");
    			if( sRsp.charAt(2) == ' ' )
    			{
    				/* third character is a space, like we expect */
    				Log.v(TAG,"PID string: " + sRsp.substring(3, 5));
    				int iPID = Integer.parseInt(sRsp.substring(3, 5),16);
    				Log.v(TAG,"PID = " + iPID);
    				switch(iPID)
    				{
    					/* NOTE - values and math taken shamelessly from the relevant 
    					 * wikipedia article, which can be found at
    					 * http://en.wikipedia.org/wiki/OBD-II_PIDs
    					 * Presumably this article has been edited to reflect the latest
    					 * and greatest SAE standards.
    					 * 
    					 * NOTE 2 - most of the modes and PIDs supported by different
    					 * vehicle manufactures are proprietary and cost a fortune to get
    					 * access to, if you can even get access to them. The modes and
    					 * PIDs supported here should be standard but just know there are
    					 * many, many more.
    					 */
	    				case 0:
	    					/* PID's supported (bitmap) */
	    					/* eventually, we will want to support this, because there may
	    					 * be some more "advanced" data that we want to query, but only
	    					 * certain vehicles will support it. For now, we'll just assume
	    					 * that vehicles manufactured within the last 10 years or so 
	    					 * are smart enough to report silly things like speed and engine
	    					 * RPM, which the computer knows anyways.
	    					 */
	    					break;
	    				case 1:
	    					/* Monitor status */
	    					break;
	    				case 2:
	    					/* Freeze DTC */
	    					break;
	    				case 3:
	    					/* Fuel system status */
	    					break;
	    				case 4:
	    					/* Calculated engine load */
	    					int iCalcLoad = Integer.parseInt( "" + sRsp.charAt(6) + sRsp.charAt(7),16 );
	    					Log.v(TAG, "parseReturn - calcualted engine load: " + iCalcLoad );
	    					float fCalcLoad = (float)iCalcLoad * (float) (100.0/255.0);
	    					Log.v(TAG, "fCalcLoad: " + fCalcLoad);
	    					final String sPrintUI4 = "Load%: " + fCalcLoad;

	    					break;
	    				case 5: /* coolant temperature */
	    					/* this is composed of the byte following the PID */
	    					int iTempC = Integer.parseInt( "" + sRsp.charAt(6) + sRsp.charAt(7),16 );
	    					Log.v(TAG, "parseReturn - coolant temp: " + iTempC );
	    					int iUiTempC = iTempC - 40; /* must offset temp by 40 */
	    					final String sPrintUI5 = "C Temp: " + iUiTempC;

	    					//rspAdapter.add("C Temp: " + iTempC);
	    					break;
	    				case 6:
	    					/* short term fuel % trim on bank 1 */
	    					break;
	    				case 7:
	    					/* long term fuel % trim on bank 1 */
	    					break;
	    				case 8:
	    					/* short term fuel % trim on bank 2 */
	    					break;
	    				case 9:
	    					/* long term fuel % trim on bank 2 */
	    					break;
	    				case 10:
	    					/* Fuel pressure */
	    					/* this value is one byte, which must be multiplied by three for the actual value */
	    					int iFuelPressure = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7),16);
	    					Log.v(TAG,"parseReturn - Fuel Pressure: " + iFuelPressure);
	    					iFuelPressure = iFuelPressure * 3;
	    					rspAdapter.add("Fuel Pressure(kPa): " + iFuelPressure);
	    					break;
	    				case 11:
	    					/* Intake MAP (manifold absolute pressure) */
	    					int iMAP = Integer.parseInt( "" + sRsp.charAt(6) + sRsp.charAt(7),16 );
	    					Log.v(TAG, "parseReturn - MAP: " + iMAP );
	    					rspAdapter.add("MAP(kPa): " + iMAP);
	    					break;
	    				case 12:
	    					/* Engine RPM */
	    					/* this case has 4 data bytes - so responses look like
	    					 * 41 0C XX YY. After converting to an int, the result needs
	    					 * to be divided by 4, since RPM's are given in 1/4 RPM increments.
	    					 */
	    					int iRPM = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7) + sRsp.charAt(9) + sRsp.charAt(10),16);
	    					Log.v(TAG, "parseReturn - RPM: " + iRPM);
	    					float fRPM = (float)iRPM/(float)4.0;
	    					final String sPrintUI12 = "RPM: " + fRPM;

	    					break;
	    				case 13:
	    					/* Speed (km/h) */
	    					/* this value is just whatever is reported - the speed is between 0 and 255 km/h */
	    					/* hmm - wtf happens with a car that has a top speed exceeding 255 km/h? BUICK POWER*/
	    					int iSpeed = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7),16);
	    					Log.v(TAG, "parseReturn - speed: " + iSpeed);
	    					rspAdapter.add("Speed: " + iSpeed + "km/h");
	    					break;
	    				case 14:
	    					/* timing advance relative to cylinder 1 */
	    					int iTimingAdvance = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7),16);
	    					Log.v(TAG, "parseReturn - timing advance: " + iTimingAdvance);
	    					float fTimingAdvance = ((float)iTimingAdvance/(float)2.0) - (float)64.0;
	    					rspAdapter.add("Timing Advance: " + fTimingAdvance);
	    				case 15:
	    					/* IAT's - intake air temperature */
	    					/* represented the same as coolant temp - take the raw value and subtract 40,
	    					 * and there's your temp in Celsius.
	    					 */
	    					int iIATTempC = Integer.parseInt( "" + sRsp.charAt(6) + sRsp.charAt(7),16 );
	    					Log.v(TAG, "parseReturn - coolant temp: " + iIATTempC );
	    					iIATTempC = iIATTempC - 40; /* must offset temp by 40 */
	    					final String sPrintUI15 = "C Temp (IAT): " + iIATTempC;

	    					break;
	    				case 16:
	    					/* MAF (mass air flow) rate, in grams/sec */
	    					/* this is the two data bytes following the PID, similar
	    					 * to engine RPMs. Note that the character at index 8 gets
	    					 * skipped when we convert the return data into an int - that's
	    					 * because it's a space :)
	    					 */
	    					int iMAFRate = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7) + sRsp.charAt(9) + sRsp.charAt(10),16);
	    					Log.v(TAG,"parseReturn, MAF rate: " + iMAFRate);
	    					float fMAFRate = (float)iMAFRate/(float)100.0;
	    					rspAdapter.add("MAF Rate: " + fMAFRate);
	    					break;
	    				case 17:
	    					/* throttle position - percentage */
	    					/* one data byte that gets converted to a percentage */
	    					int iThrottlePercent = Integer.parseInt("" + sRsp.charAt(6) + sRsp.charAt(7),16);
	    					Log.v(TAG,"parseReturn, Throttle: " + iThrottlePercent);
	    					float fThrottlePercent = (float)iThrottlePercent * (float) (100.0/255.0);
	    					rspAdapter.add("Throttle: " + fThrottlePercent + "%");
	    					break;
	    				default:
	    					break;
    				}
    			}
    			else
    			{
    				/* unknown response format */
    				Log.v(TAG, "parseResponse - Unknown response format");
    				return OBD_ERR;
    			}
    			
    		}
    		else
    		{
    			/* for now, only support mode 1 */
    			Log.v(TAG,"parseReturnMessage, invalid mode");
    			rspAdapter.add("INV MODE");
    			return OBD_UNSUPPORTED_MODE;
    		}
    	}
    	else
    	{
    		/* not sure what to do here - we are connected to the car's
    		 * computer, and the message was not an AT command and did
    		 * not appear to have OBD data - what gives?
    		 */
    		Log.v(TAG, "parseReturnMessage, unknown response type");
    		Log.v(TAG, "response: " + sMsg);
    		rspAdapter.add("UNKNOWN RSP" + "\n" + sMsg);
    		return ELM327_ERR;
    	}
    	
    	/* should never, ever get here */
    	return 999999;
    }
   /* 
    @Override
    protected void onPause()
    {
    	Log.v(TAG, "in onPause");
    	super.onPause();
    	btAdapter.cancelDiscovery();
    	unregisterReceiver(receiver);
    	Log.v(TAG, "end onPause");
    }
    
    @Override
    protected void onResume()
    {
    	Log.v(TAG, "in onResume");
    	super.onResume();
		registerReceiver(receiver,filter);
		IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(receiver,filter2);
		IntentFilter filter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver,filter3);
		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(receiver,filter4);
    	Log.v(TAG, "end onResume");
    }
    
    @Override
    protected void onDestroy()
    {
    	Log.v(TAG, "in onDestroy");
    	super.onDestroy();
    	btAdapter.cancelDiscovery();
    	unregisterReceiver(receiver);
    	Log.v(TAG, "end onDestroy");
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data )
    {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode == RESULT_CANCELED )
    	{
    		Toast.makeText(getApplicationContext(), "Please enable bluetooth to continue", Toast.LENGTH_SHORT).show();
    		finish();
    	}
    }*/

    public void buttonPress(View view)
    {
    	Log.v(TAG, "in buttonPress, sending command sequence");
    	SendCommandsThread sendCmd = new SendCommandsThread("01 04" + '\r');
    	sendCmd.start();
    	Log.v(TAG, "end buttonPress" );
    } 
    
    /* a couple of test strings to send that will eventually
     * read the engine coolant temperature if everything works
     * right
     */
    public void resetButtonPress(View view)
    {	
    	Log.v(TAG, "in resetButtonPress, sending command sequence");
    	SendCommandsThread sendCmd = new SendCommandsThread("01 05" + '\r');
    	sendCmd.start();
    	Log.v(TAG, "end resetButtonPress" );
    }
    
    public void prepareButtonPress(View view)
    {
    	Log.v(TAG, "in resetButtonPress, sending command sequence");
    	SendCommandsThread sendCmd = new SendCommandsThread("01 0C" + '\r');
    	sendCmd.start();
    	Log.v(TAG, "end resetButtonPress" );
    }
    
    public void cmdButtonPress(View view)
    {
    	Log.v(TAG, "in resetButtonPress, sending command sequence");
    	SendCommandsThread sendCmd = new SendCommandsThread("01 0F" + '\r');
    	sendCmd.start();
    	Log.v(TAG, "end resetButtonPress" );
    }
    
    /* this method looks through all the paired devices on
     * the phone, finds one with the string "OBD" in the name,
     * and attempts to connect to it. No BT scans, no nothing,
     * just connect to the paired device. 
     */
	private void initConnectionToDongle() {
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
		connect.start();
		
	}
    
	
	
	/* ConnectThread Class */
	
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
		            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
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
	
	private class ConnectedThread extends Thread {
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
	
	private class SendCommandsThread extends Thread {
		String sCmd = "";
	 
	    public SendCommandsThread(String sCommand) {
	    	sCmd = sCommand;
	    }
	 
	    public void run() {
	    	String sResponse;
	    	Log.v(TAG,"sending commands in new thread");
	    	sendElm327Cmd("AT Z" + '\r');
	    	Log.v(TAG,"waiting for ready");
	    	sResponse = waitForReady();
	    	Log.v(TAG, "sResponse: " + sResponse);
	    	//rspAdapter.add(sResponse);
	    	sendElm327Cmd("AT SP 0" + '\r');
	    	sResponse = waitForReady();
	    	Log.v(TAG, "sResponse: " + sResponse);
	    	//rspAdapter.add(sResponse);
	    	sendElm327Cmd(sCmd);
	    	sResponse = waitForReady();
	    	Log.v(TAG, "sResponse: " + sResponse);
	    	parseReturnMessage(sResponse);
	    	//rspAdapter.add(sResponse);
	    	Log.v(TAG, "end run method, cmd sequence sent");
	    }

	}
    
    
}
