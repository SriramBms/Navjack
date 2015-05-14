package de.tudarmstadt.tk.carsensing.measurement.position;

import java.util.LinkedList;
import java.util.Timer;

import de.tudarmstadt.tk.carsensing.measurement.position.Position;
import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;
import de.tudarmstadt.tk.carsensing.settings.Settings;

import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

public class PositionManager implements Callback {

	private Handler handler;
	private LinkedList<Position> gpsPositions;
	private LocationManager locationManager;
	private GpsListener gpsListener;
	private NetworkListener networkListener;

	public boolean gpsActive = false;
	private Timer gpsTimeout = null;
	private Timer networkTimer = null;

	private boolean useLastKnownNetworkPosition = false;

	private boolean highVersion = false;

	public PositionManager(Handler handler, LocationManager locationManager) {
		this.handler = handler;
		this.locationManager = locationManager;

		gpsListener = new GpsListener(new Handler(this));
		networkListener = new NetworkListener(new Handler(this), locationManager);

		gpsPositions = new LinkedList<Position>();

		activateGpsListener(true);
		gpsActive(false);

		if (Build.VERSION.SDK_INT > 8)
			highVersion = true;
		else
			highVersion = false;
	}


	/**
	 * Gibt verwendeten Speicher frei.
	 */
	public void kill() {
		activateGpsListener(false);
		activateNetworkListener(false);
		handler = null;
		gpsPositions = null;
		locationManager = null;
		gpsListener = null;
		networkListener = null;
		if (gpsTimeout != null)
			gpsTimeout.cancel();
		gpsTimeout = null;
	}

	/**
	 * Bearbeitet die einkommenden MEssages und ruft entsprechende Funktionen auf
	 */
	@Override
	public boolean handleMessage(Message msg) {

		Bundle data = msg.getData();
		if (data.containsKey("messageType")) {
			if (data.getInt("messageType") == MeasurementService.GPS_POSITION) {
				receiveGpsPosition(msg);
			} else if (data.getInt("messageType") == MeasurementService.NETWORK_POSITION) {
				receiveNetworkPosition(msg);
			} else if (data.getInt("messageType") == MeasurementService.GPS_STATUS) {
				setGpsStatus(msg);
			} else if (data.getInt("messageType") == MeasurementService.NETWORK_STATUS) {
				setNetworkStatus(msg);
			} else if (data.getInt("messageType") == MeasurementService.GPS_TIMEOUT) {
				gpsTimeout();
			} else if (data.getInt("messageType") == MeasurementService.NETWORK_UPDATE_POSITION) {
				networkUpdatePosition();
			}
		}
		return false;
	}

	/**
	 * Wenn der Network-Listener schon eine gewisse Zeit keine Position mehr übertragen hat, wird diese manuell erneut ermittelt.
	 */
	private void networkUpdatePosition() {
		if (useLastKnownNetworkPosition && networkListener != null)
			networkListener.sendLastKnownPosition();
	}

	/**
	 * Wird aufgerufen, sobald der GPS-Listener eine Position sendet.
	 * Die Position wird zwischengespeichert, um die beste Positionsangabe zu ermitteln.
	 * @param msg
	 */
	private void receiveGpsPosition(Message msg) {
		gpsActive(true);

		Bundle data = msg.getData();
		if (!highVersion) {
			Position position = new Position();
			position.provider = data.getString("provider");
			position.accuracy = data.getFloat("accuracy");
			position.latitude = data.getDouble("latitude");
			position.longitude = data.getDouble("longitude");
			position.altitude = data.getDouble("altitude");
			//		position.time = data.getLong("time");
			gpsPositions.add(position);
		} else {
			Message tempMsg = new Message();
			data.putInt("messageType", MeasurementService.POSITION);
			tempMsg.setData(msg.getData());
			handler.sendMessage(tempMsg);
			startGpsTimeout();
		}
	}

	/**
	 * Wird aufgerufen, sobald der Network-Listener eine Position sendet.
	 * Die Position wird sofort an den Service übermittelt!
	 * @param msg
	 */
	private void receiveNetworkPosition(Message msg) {
		if (handler != null) {
			Position position = new Position(msg);
			handler.sendMessage(position.getMessage());
			setNetworkTimer();
			useLastKnownNetworkPosition = true;
		}
	}

	/**
	 * Wird aufgerufen, sobald der GPS-Listener seinen Status ändert
	 * @param msg
	 */
	private void setGpsStatus(Message msg) {
		Bundle data = msg.getData();

		if (data.getInt("status") == LocationProvider.AVAILABLE) {
			gpsActive(true);
		} else if (data.getInt("status") == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			// GpsTimeout nur starten, wenn zuvor GPS-Daten empfangen wurden. Da es sein könnte , dass der GPS Provider mehrmals hintereinander "Temp. Unavailable" schickt.
			if (gpsActive)
				startGpsTimeout();

			Position bestPosition = getBestGpsPosition();
			if (bestPosition != null)
				handler.sendMessage(bestPosition.getMessage());

			gpsPositions = new LinkedList<Position>();
		}
	}

	/**
	 * Wird aufgerufen, sobald der Network-Listener seinen Status ändert
	 * @param msg
	 */
	private void setNetworkStatus(Message msg) {
		Bundle data = msg.getData();

		if (msg.getData().getInt("status") == LocationProvider.OUT_OF_SERVICE) {
			useLastKnownNetworkPosition = false;
		}
		else if (data.getInt("status") == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			useLastKnownNetworkPosition = false;
		}
	}

	/**
	 * Wird aufgerufen, sobald GPS seit einiger Zeit keine Daten mehr gesendet hat.
	 */
	private void gpsTimeout() {
		gpsActive(false);
	}

	/**
	 * Ermittelt aus den gespeicherten GPS-Positionen die Beste
	 * @return
	 */
	private Position getBestGpsPosition() {
		if(gpsPositions == null)
			return null;
		if (gpsPositions.size() == 0)
			return null;

		Position out = new Position();
		out.provider = "gps";

		Float accuracy = Float.valueOf(Float.MAX_VALUE);
		LinkedList<Double> latitude = null;
		LinkedList<Double> longitude = null;
		LinkedList<Double> altitude = null;

		// Speichere die Positionsangaben mit der genauesten Accurancy
		for (Position actPos : gpsPositions) {
			if (actPos.accuracy < accuracy) {
				latitude = new LinkedList<Double>();
				longitude = new LinkedList<Double>();
				altitude = new LinkedList<Double>();
				latitude.add(actPos.latitude);
				longitude.add(actPos.longitude);
				altitude.add(actPos.altitude);
				accuracy = actPos.accuracy;
			}
			else if (actPos.accuracy == accuracy) {
				latitude.add(actPos.latitude);
				longitude.add(actPos.longitude);
				altitude.add(actPos.altitude);
			}
		}

		// Bleiben maximal zwei Positionsangaben übrig, so wird die letzte übermittelt verwendet.
		if (latitude.size() <= 2 && longitude.size() <= 2) {
			out.accuracy = accuracy;
			out.latitude = latitude.getLast();
			out.longitude = longitude.getLast();
			out.altitude = altitude.getLast();
			return out;
			// Bleiben mehr Positionsangaben übrig, so wird der Median der einzelnen Werte ermittelt und verwendet.
		} else {

			// Umwandeln der LinkedLists in Arrays
			Double[] latitudeArray = (Double[]) latitude.toArray();
			Double[] longitudeArray = (Double[]) longitude.toArray();
			Double[] altitudeArray = (Double[]) altitude.toArray();

			// Bilde den Median der einzelnen Werte und speichere
			out.accuracy = accuracy;
			out.latitude = median(latitudeArray);
			out.longitude = median(longitudeArray);
			out.altitude = median(altitudeArray);
			return out;
		}

	}

	/**
	 * Liefert den Median eines Arrays
	 * @param input
	 * @return
	 */
	private Double median(Double[] input) {
		java.util.Arrays.sort(input);
		int middle = input.length/2;
		if (input.length%2 == 1) {
			return input[middle];
		} else {
			return (input[middle-1] + input[middle]) / 2;
		}
	}

	/**
	 * Startet den Timer, nach dessen Ablauf der Network-Listener registriert wird (da GPS keine Daten mehr liefert).
	 */
	private void startGpsTimeout() {
		if (gpsTimeout != null)
			gpsTimeout.cancel();
		gpsTimeout = new Timer();
		gpsTimeout.schedule(new GpsTimeout(new Handler(this)), Settings.GPS_TIMEOUT * 1000);
	}

	/**
	 * Registriert bzw. entfernt den GPS-Listener
	 * @param active
	 */
	private void activateGpsListener(boolean active) {
		if (active)
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Settings.GPS_UPDATE_TIME * 1000, Settings.GPS_UPDATE_METER, gpsListener);
		else
			locationManager.removeUpdates(gpsListener);
	}

	/**
	 * Registriert bzw. entfernt den Network-Listener
	 * @param active
	 */
	private void activateNetworkListener(boolean active) {
		if (active) {
			if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Settings.NETWORK_UPDATE_TIME * 1000, Settings.NETWORK_UPDATE_METER, networkListener);
				notifyGuiNetworkAvailable(true);
			}
			else {
				locationManager.removeUpdates(networkListener);
				if (networkTimer != null)
					networkTimer.cancel();
				networkTimer = null;
				notifyGuiNetworkAvailable(false);
			}
		}
	}

	/**
	 * Benachrichtigt die GUI, ob die Positionsbestimmung über GPS aktiv ist.
	 * @param active
	 */
	private void notifyGuiGpsAvailable(boolean active) {
		Bundle sendData = new Bundle(2);
		sendData.putInt("messageType", MeasurementService.GPS_GUI_STATUS);
		sendData.putBoolean("active", active);
		Message sendMsg = new Message();
		sendMsg.setData(sendData);
		handler.sendMessage(sendMsg);
	}

	/**
	 * Benachrichtigt die GUI, ob die Positionsbestimmung über das Netzwerk aktiv ist.
	 * @param active
	 */
	private void notifyGuiNetworkAvailable(boolean active) {
		Bundle sendData = new Bundle(2);
		sendData.putInt("messageType", MeasurementService.NETWORK_GUI_STATUS);
		sendData.putBoolean("active", active);
		Message sendMsg = new Message();
		sendMsg.setData(sendData);
		handler.sendMessage(sendMsg);
	}

	/**
	 * Wenn GPS aktiv gesetzt wird, wird die GUI davon unterrichtet und der Network-Listener deaktiviert.
	 * @param active
	 */
	private void gpsActive(boolean active) {
		if (active && !gpsActive) {
			gpsActive = true;
			activateNetworkListener(false);
			notifyGuiGpsAvailable(true);
		} else if (!active) {
			gpsActive = false;
			activateNetworkListener(true);
			notifyGuiGpsAvailable(false);
		}
	}

	/**
	 * Setzt den NetworkTimer, nach dessen Ablauf die letzet Netzwerk-Position verwendet werden soll
	 */
	private void setNetworkTimer() {
		if (networkTimer != null)
			networkTimer.cancel();
		networkTimer = new Timer();
		networkTimer.schedule(new NetworkTimer(new Handler(this)), Settings.NETWORK_UPDATE_TIME * 1000/2, Settings.NETWORK_UPDATE_TIME * 1000);
	}

}
