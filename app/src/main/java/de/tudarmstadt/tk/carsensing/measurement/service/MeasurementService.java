package de.tudarmstadt.tk.carsensing.measurement.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.carlogic.CarStatusReader;
import de.tudarmstadt.tk.carsensing.carlogic.DummyCarStatusReader;
import de.tudarmstadt.tk.carsensing.configuration.MeasurementConfiguration;
import de.tudarmstadt.tk.carsensing.constants.IntentMessages;
import de.tudarmstadt.tk.carsensing.constants.Measurements;
import de.tudarmstadt.tk.carsensing.data.JSONPrinter;
import de.tudarmstadt.tk.carsensing.data.OutputData;
import de.tudarmstadt.tk.carsensing.measurement.network.CellularInfo;
import de.tudarmstadt.tk.carsensing.measurement.network.WifiScanner;
import de.tudarmstadt.tk.carsensing.measurement.position.*;
import de.tudarmstadt.tk.carsensing.measurement.sensors.*;
import de.tudarmstadt.tk.carsensing.obd.OBD;
import de.tudarmstadt.tk.carsensing.settings.Settings;

/**
 * 
 * @author Julien Gedeon
 *
 */

public class MeasurementService extends android.app.Service implements Callback {



	public final static String TAG = "MeasurementService";

	public final static int GPS_STATUS = 1;
	public final static int NETWORK_STATUS = 2;
	public final static int PHONE_STATUS = 3;
	public final static int GPS_POSITION = 4;
	public final static int NETWORK_POSITION = 5;
	public final static int GPS_TIMEOUT = 6;
	public final static int POSITION = 7;

	//Control messages
	public final static int SENSOR_ENABLE_OBD = 50;
	//Sensors
	public final static int SENSOR_LIGHT = 31;
	public final static int SENSOR_PRESSURE = 32;
	public final static int SENSOR_ACCELEROMETER = 33;
	public final static int SENSOR_BATTERYLEVEL = 34;
	public final static int SENSOR_BATTERYTEMP = 35;
	public final static int SENSOR_COMPASS = 36;
	public final static int SENSOR_LOUDNESS = 37;
	public final static int SENSOR_MAGNETIC = 38;



	public final static int SERVICE_STARTED = 11;
	public final static String actionServiceStarted = "de.tudarmstadt.tk.carsensing.servicerunning";

	public final static int DB_GPS_POSITION = 12;
	public final static int DB_NETWORK_POSITION = 13;

	public final static int GPS_GUI_STATUS = 14;
	public final static int NETWORK_GUI_STATUS = 15;

	public final static int NETWORK_UPDATE_POSITION = 16;

	public final static int START_LOCATION_SERVICE = 17;
	public final static int SHUTDOWN_LOCATION_SERVICE = 18;

	private static final String TAG_SENSOR_ACCELEROMETER = "Accelerometer";

	private static final String TAG_SENSOR_LIGHT = "LightSensor";
	private static final String TAG_SENSOR_PRESSURE = "PressureSensor";
	private static final String TAG_SENSOR_COMPASS = "Compass";
	private static final String TAG_SENSOR_MAGNETIC = "MagneticSensor";


	private boolean active = false;
	private NotificationManager notificationManager;
	private LocationManager locationManager;
	private PositionManager positionManager;
	// Sensor timer parameters (in seconds)
	

	Handler sensorHandler;

	private String deviceID;

	private Thread dataCollector;
	private Thread obdRequests=null;
	

	private boolean saveLocation;
	private Position currentPosition;
	private float measurementInterval = 20;
	private int obdInterval = 100;
	private String description ;

	private LightSensor lightSensor;
	private Accelerometer accelerometer;
	private Compass compass;
	private MagneticSensor magneticSensor;
	private PressureSensor pressureSensor;

	private WifiScanner wifiScanner;
	private CellularInfo cellularInfo;
	private BatteryLevel batteryLevel;
	private BatteryTemperature batteryTemp;
	private LoudnessSensor loudnessSensor;

	private OBD obd = null;
	private boolean collectSpeed = false;
	private boolean collectRPM = false;
	private boolean collectEngineLoad = false;
	private boolean collectMAF = false;
	private boolean collectCoolantTemp = false;
	private boolean collectIntakeTemp = false;

	private List<JSONPrinter> measurementList = new ArrayList<JSONPrinter>();
	private CarStatusReader carStatus;
	private boolean carIsMoving = false;
	private boolean initLocationServiceShutdown = false;
	private boolean shutdownLocationService = false;
	private boolean saveLastLocation = false;
	private final IBinder mBinder = new ServiceBinder();
	public class ServiceBinder extends Binder {
		public MeasurementService getService() {
			return MeasurementService.this;
		}
	}


	@Override	
	public void onCreate() {
		this.locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		deviceID = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		sensorHandler = new Handler(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand: Intent received");
		Bundle bundle=null;
		//Bundle bundle = new Bundle(); // TODO: temporary hack
		//bundle.putFloat(IntentMessages.INTERVAL, 5.0f);
		if (intent != null && intent.getExtras() != null) {
			bundle = intent.getExtras();

			if(bundle.containsKey(IntentMessages.INTERVAL)){
				measurementInterval = bundle.getFloat(IntentMessages.INTERVAL);
			}


		} else {
			//bundle = MeasurementConfiguration.retrieveConfigurationFromURL(Settings.CONFIGURATION_URL);
		}

		description = DateFormat.format("yyyy-MM-dd   hh:mm:ss", new Date().getTime()).toString().trim();


		cellularInfo = new CellularInfo(this);
		measurementList.add(cellularInfo);

		wifiScanner = new WifiScanner(this);
		measurementList.add(wifiScanner);

		batteryLevel = new BatteryLevel(this);
		measurementList.add(batteryLevel);

		batteryTemp = new BatteryTemperature(this);
		measurementList.add(batteryTemp);


		lightSensor = new LightSensor(this,new Handler(this));
		measurementList.add(lightSensor);

		accelerometer = new Accelerometer(this, new Handler(this));
		measurementList.add(accelerometer);

		compass = new Compass(this, new Handler(this));
		measurementList.add(compass);

		magneticSensor = new MagneticSensor(this, new Handler(this));
		measurementList.add(magneticSensor);

		pressureSensor = new PressureSensor(this, new Handler(this));
		measurementList.add(pressureSensor);


		//add pressure sensor

		saveLocation = true;

		if(bundle != null && !isActive()){

			if (bundle.getBoolean(IntentMessages.COLLECT_OBD)) {
				Log.d(TAG,"OBD enabled in measurement service");
				if (bundle.getString(IntentMessages.BLUETOOTH_ADDRESS) != null) {

					obd = new OBD(bundle.getString(IntentMessages.BLUETOOTH_ADDRESS), this);
					Log.v(TAG, "BT device selected"+bundle.getString(IntentMessages.BLUETOOTH_ADDRESS));
				} else {
					Log.v(TAG, "BT Device not selected");
					obd = new OBD(this);

				}
				if(obd.getDeviceName()==null){
					Toast.makeText(this, "Could not connect to valid OBD device. Please try again", Toast.LENGTH_SHORT).show();
				}



				/*measurementList.add(obd);

				carStatus = obd;
				collectSpeed = true;
				collectRPM = true;
				collectEngineLoad = true;
				collectMAF = true;
				collectCoolantTemp = true;
				collectIntakeTemp = true;*/



				//loudnessSensor = new LoudnessSensor(this);
				//loudnessSensor.startSensor();
				//measurementList.add(loudnessSensor);

			}

		}
		startService();
		/*Intent iServiceRunning = new Intent(actionServiceStarted);
		sendBroadcast(iServiceRunning);*/
		return super.onStartCommand(intent, flags, startId);
	}	


	@Override
	public void onDestroy() {
		Log.d(TAG, "Measurement service stopped");
		stopService();
		stopForeground(true);
		super.onDestroy();
	}



	private class OBDRequests implements Runnable {
		int i = 0;
		@Override
		public void run() {
			while (active) {
				try {
					if(Thread.interrupted() || obd == null){
						Log.d(TAG, "Thread stopped/ obd is null");
						return;
					}
					Log.d(TAG,"OBD Requests Loop");
					Thread.sleep((long)(obdInterval));

					if (obd != null && collectRPM) obd.requestRPM();
					Thread.sleep((long)(obdInterval));
					if (obd != null && collectSpeed) obd.requestSpeed();
					Thread.sleep((long)(obdInterval));
					if (obd != null && collectEngineLoad) obd.requestCLV();
					Thread.sleep((long)(obdInterval));
					if (obd != null && collectMAF) obd.requestMAF();

					if (i==20) {
						Thread.sleep((long)(obdInterval));
						if (obd != null && collectCoolantTemp) obd.requestCoolantTemp();
						Thread.sleep((long)(obdInterval));
						if (obd != null && collectIntakeTemp) obd.requestIntakeTemp();
						i=0;
					}

					i++;
				} catch (InterruptedException e) {
					Log.d(TAG, "Interrupted Exception while sleeping");
					e.printStackTrace();
					return;
				}
			}
		}
	}




	private class CollectData implements Runnable {
		private Handler handler;
		public CollectData(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			while(active) {
				try {
					if(obd == null){
						//Log.d(TAG, "OBD is null");
					}else{
						obd.checkDongleConnection();
						boolean newCarStatus = carStatus.CarIsMoving();
						//boolean newCarStatus = true; //manuall set to true for debugging in lab TODO: REVERT
						Log.d("CarStatus", "CarStatus: " + String.valueOf(newCarStatus));
						if (newCarStatus) {
							if (!carIsMoving) { // Car went from off to on
								//Log.d("CarStatus", "Car went from off to on");
								Message msg = new Message();
								Bundle bundle = new Bundle(1);
								bundle.putInt("messageType", MeasurementService.START_LOCATION_SERVICE);
								msg.setData(bundle);
								handler.sendMessage(msg);
							} else {
								// Car is on, check if Location service is running
								if (positionManager == null) {
									//Log.d("CarStatus", "Restarting Location service");
									Message msg = new Message();
									Bundle bundle = new Bundle(1);
									bundle.putInt("messageType", MeasurementService.START_LOCATION_SERVICE);
									msg.setData(bundle);
									handler.sendMessage(msg);
								}
							}
						} else {
							if (carIsMoving) {  // Car went from on to off
								Log.d("CarStatus", "Car went from on to off");
								initLocationServiceShutdown = true;
							}
						}
						carIsMoving = newCarStatus;
					}
				} catch (Exception e) {
					Log.d(TAG, "Error in Car logic");
					e.printStackTrace();
				}

				if (wifiScanner != null) wifiScanner.scanWifi(false);

				try {
					Thread.sleep((long)(measurementInterval * 1000));
				} catch (InterruptedException e) {
					Log.d(TAG, "Interrupted Exception while sleeping");
					e.printStackTrace();
				}

				long time = System.currentTimeMillis();

				for (JSONPrinter dataToPrint : measurementList) {
					if(dataToPrint == null){
						measurementList.remove(dataToPrint);
						continue;
					}
					OutputData.output(dataToPrint.outputJSON(deviceID, time, description).toString());
				}

				Log.d("CarStatus", "saveLastLocation:" + saveLastLocation);
				Log.d("CarStatus", "saveLocation:" + saveLocation);
				Log.d("CarStatus", "currentPosition:" + currentPosition);
				Log.d("CarStatus", "positionManager:" + positionManager);			

				if ((saveLastLocation && saveLocation && currentPosition !=null) || (saveLocation && currentPosition !=null && positionManager != null)) {
					Log.d("CarStatus" , "Save position");
					OutputData.output(currentPosition.outputJSON(deviceID, time, description).toString());
					saveLastLocation = false;
				}

				if (initLocationServiceShutdown) {
					Log.d("CarStatus", "shutdown location service");
					Message msg = new Message();
					Bundle bundle = new Bundle(1);
					bundle.putInt("messageType", MeasurementService.SHUTDOWN_LOCATION_SERVICE);
					msg.setData(bundle);
					handler.sendMessage(msg);
					initLocationServiceShutdown = false;
				}
			}
		}
	}


	@Override
	public boolean handleMessage(Message msg) {
		//Mainly for debugging. Don't log services that don't add value. All data is 'JSON'ed' into the logs anyway 
		if (active) {
			Bundle data = msg.getData();
			Log.v(TAG, "msg.getData="+data.toString());
			switch (data.getInt("messageType")) {
			case POSITION:
				Log.d("CarStatus", "New Position");
				this.currentPosition = new Position(msg);
				currentPosition.time = System.currentTimeMillis();
				if (shutdownLocationService) {
					synchronized (this) {
						Log.d("CarStatus", "Killing location service");
						shutdownLocationService = false;
						saveLastLocation = true;
						stopLocationService();
					}
				}
				break;
			case START_LOCATION_SERVICE:	
				Log.d("CarStatus", "Start LocationService Handler");
				startLocationService();
				break;
			case SHUTDOWN_LOCATION_SERVICE:
				Log.d("CarStatus", "Shutdown Location Service Handler");
				shutdownLocationService = true;
				break; 
				/*
				case SENSOR_LIGHT:
					Log.d(TAG_SENSOR_LIGHT,"Value="+data.getFloat("value"));
					break;
				case SENSOR_PRESSURE:
					Log.d(TAG_SENSOR_PRESSURE,"Value="+data.getFloat("value"));
					break;
				case SENSOR_ACCELEROMETER:
					Log.d(TAG_SENSOR_ACCELEROMETER, "Value="+data.getFloat("value"));
					break;
				case SENSOR_COMPASS:
					Log.d(TAG_SENSOR_COMPASS, "Value="+data.getFloat("value"));
					break;
				case SENSOR_MAGNETIC:
					Log.d(TAG_SENSOR_MAGNETIC, "Value="+data.getDouble("value"));
				 */
			default:
				//Log.d(TAG,"Unknown sensor data");

			}
		}
		return false;
	}


	public boolean isActive() {
		synchronized (this) {
			return active;
		}
	}


	public void startLocationService() {
		positionManager = new PositionManager(new Handler(this), locationManager);
	}


	public void stopLocationService() {
		if (positionManager != null) {
			synchronized (positionManager) {
				positionManager.kill();
				positionManager = null;
			}
		}
	}


	public void startService() {
		if (!active) {
			synchronized(this) {
				active = true;
				startLocationService();
				carIsMoving = true;
				Log.d(TAG, "Start Data Collector Thread");

				dataCollector = new Thread(new CollectData(new Handler(this)));
				dataCollector.start();

				Log.d(TAG, "Start OBD Requests Thread");
				obdRequests = new Thread(new OBDRequests());
				obdRequests.start();


				//sensorDataLogger = new Thread(new SensorDataLogger());
				//sensorDataLogger.start();

				// Show notification
				Notification notification = new Notification.Builder(this)
				.setContentTitle("CarSensing")
				.setContentText("Measurements are running").setSmallIcon(R.drawable.ic_stat_car).build();
				notification.flags = Notification.FLAG_ONGOING_EVENT;
				notificationManager.notify(0, notification);

			}
		}
	}


	public void stopService() {
		synchronized(this) {
			active = false;
			stopLocationService();
			notificationManager.cancelAll();
			if (loudnessSensor != null) {
				loudnessSensor.stopSensor();
				loudnessSensor = null;
			}
			dataCollector = null;
			lightSensor = null;
			accelerometer = null;
			magneticSensor = null;
			compass = null;
			wifiScanner = null;
			cellularInfo = null;
			batteryLevel = null;
			batteryTemp=null;
			if (obd != null) {
				obd.disconnectDongle();
				obd = null;
			}
			if (loudnessSensor != null) {
				loudnessSensor.stopSensor();
				loudnessSensor = null;
			}
		}
	}

	public void setOBDStatus(boolean state, String btAddress){
		Log.v(TAG, "OBD status: "+String.valueOf(state));
		//Toast.makeText(getApplicationContext(), "Service called from Activity", Toast.LENGTH_SHORT).show();
		if(state){
			if (btAddress != null) {

				obd = new OBD(btAddress, this);

				Log.v(TAG, "BT device selected"+btAddress);
			} else {
				Log.v(TAG, "BT Device not selected");
				obd = new OBD(this);

			}
			if(obd.getDeviceName()==null){
				Toast.makeText(this, "Could not connect to valid OBD device. Please try again", Toast.LENGTH_SHORT).show();
			}

			measurementList.add(obd);

			carStatus = obd;
			collectSpeed = true;
			collectRPM = true;
			collectEngineLoad = true;
			collectMAF = true;
			collectCoolantTemp = true;
			collectIntakeTemp = true;
			if(obdRequests==null){
				obdRequests = new Thread(new OBDRequests());

				obdRequests.start();
			}
		}else{
			if (obd != null) {
				obdRequests.interrupt();
				obdRequests = null;
				obd.disconnectDongle();
				obd = null;
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		return mBinder;
	}




}
