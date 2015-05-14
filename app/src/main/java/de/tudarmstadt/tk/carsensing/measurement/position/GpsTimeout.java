package de.tudarmstadt.tk.carsensing.measurement.position;

import java.util.TimerTask;

import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class GpsTimeout extends TimerTask {

	private Handler handler = null;

	public GpsTimeout(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		Bundle data = new Bundle(1);
		data.putInt("messageType", MeasurementService.GPS_TIMEOUT);
		Message sendMsg = new Message();
		sendMsg.setData(data);
		handler.sendMessage(sendMsg);
	}

}
