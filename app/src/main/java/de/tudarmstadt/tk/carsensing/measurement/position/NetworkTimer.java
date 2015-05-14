package de.tudarmstadt.tk.carsensing.measurement.position;

import java.util.TimerTask;

import de.tudarmstadt.tk.carsensing.measurement.service.MeasurementService;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NetworkTimer extends TimerTask {

	private Handler handler;

	public NetworkTimer(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		Bundle data = new Bundle(1);
		data.putInt("messageType", MeasurementService.NETWORK_UPDATE_POSITION);
		Message sendMsg = new Message();
		sendMsg.setData(data);
		handler.sendMessage(sendMsg);
	}

}
