package com.github.ivashov.ikmlib.networkhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkHelper {
	private Context context;

	private WifiManager.WifiLock wifiLock;
	private PowerManager.WakeLock wakeLock;

	private CountDownLatch latch;

	private NetworkStateReceiver receiver;

	public NetworkHelper(Context context) {
		this.context = context;
	}

	public void start() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "WIFI_TAG");
		wifiLock.acquire();

		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKE_TAG");
		wakeLock.acquire(120 * 1000);
	}

	public boolean awaitNetwork() {
		if (isConnected()) {
			return true;
		}

		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		wifiManager.reconnect();

		if (isConnected()) {
			return true;
		}

		try {
			latch = new CountDownLatch(1);
			context.registerReceiver(receiver = new NetworkStateReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
			latch.await(15000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
		}

		return isConnected();
	}

	public void stop() {
		if (receiver != null)
			context.unregisterReceiver(receiver);
		if (wifiLock != null)
			wifiLock.release();
		if (wakeLock != null)
			wakeLock.release();
	}

	public boolean isConnected() {
		ConnectivityManager cm =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected();
	}

	class NetworkStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isConnected()) {
				latch.countDown();
			}
		}
	}
}
