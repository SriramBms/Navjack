package de.tudarmstadt.tk.carsensing.measurement.position;

import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NetworkListener implements LocationListener {

	private Handler serviceHandler;
	private LocationManager locationManager;

	public NetworkListener (Handler serviceHandler, LocationManager locationManager) {
		this.serviceHandler = serviceHandler;
		this.locationManager = locationManager;
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Message msg = new Message();
		Bundle bundle = new Bundle(2);
		bundle.putInt("messageType", MeasurementService.NETWORK_STATUS);
		bundle.putInt("status", status);
		msg.setData(bundle);
		serviceHandler.sendMessage(msg);
	}

	@Override
	public void onLocationChanged(Location location) {
		Message msg = new Message();
		Bundle bundle = new Bundle(7);
		bundle.putInt("messageType", MeasurementService.NETWORK_POSITION);
		bundle.putString("provider", location.getProvider());
		bundle.putDouble("latitude", location.getLatitude());
		bundle.putDouble("longitude", location.getLongitude());
		bundle.putDouble("altitude", location.getAltitude());
		bundle.putFloat("accuracy", location.getAccuracy());
		msg.setData(bundle);		
		serviceHandler.sendMessage(msg);
	}

	public void sendLastKnownPosition() {
		onLocationChanged(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
	}

}