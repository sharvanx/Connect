package ml.yats.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Saravanan on 04-Feb-16.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver
        implements WifiP2pManager.ActionListener,WifiP2pManager.GroupInfoListener,WifiP2pManager.ConnectionInfoListener {

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) {
                return;
            }
            Log.d("receiver", "P2P Connection Changed");
            manager.requestGroupInfo(channel, this);
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                manager.requestConnectionInfo(channel, this);
            } else {
                // It's a disconnect
                Log.d("receiver", "p2p not connected");
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //Log.d("receiver", "Network State changed");
            if (activity.wifiManager == null) {
                Log.d("receiver", "wifimanager null");
                return;
            }

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                int ip = activity.wifiManager.getDhcpInfo().gateway;
                String ipAddress = Formatter.formatIpAddress(ip);
                Log.d("receiver","connected to " + ipAddress);
                if(ipAddress.equals("192.168.49.1")){
                    Log.d("receiver", "connected");
                    activity.setIsGroupFormed(true);
                    if (!activity.isSocketCreated) {
                        Thread handler;
                        activity.getHandler().obtainMessage(MainActivity.START_CHAT).sendToTarget();
                        Log.d(TAG, "Connected as peer");
                        try {
                            handler = new ClientSocketHandler(
                                    activity.getHandler());
                            handler.start();
                            activity.isSocketCreated = true;
                        } catch (Exception e) {
                            activity.isSocketCreated = false;
                            Log.d(TAG,
                                    "Failed to create a client thread - " + e.getMessage());
                        }
                    }
                }
            } else {
                // It's a disconnect
                activity.setIsGroupFormed(false);
                Log.d("receiver", "wifi not connected");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            WifiP2pDevice device = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d("receiver", "Device status -" + device.status);

        }
    }

    @Override
    public void onSuccess() {
        Log.d("main", "Creating Local Group ");
    }

    @Override
    public void onFailure(int reason) {
        Log.d("main", "Local Group failed, error code " + reason);
    }


    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null) {
            activity.setIsGroupFormed(true);
            Map<String, String> s = new HashMap<>();
            s.put("SSID", group.getNetworkName());
            s.put("Password", group.getPassphrase());
            WifiP2pDevice owner = group.getOwner();
            s.put("Address", owner.deviceAddress);
            //s.put("Name", owner.deviceName);
            //s.put("Status", Integer.toString(owner.status));
            final String groupDetails = s.toString();
            if (group.isGroupOwner()) {
                activity.startRegistration(s);
            }
            Log.d("groupinfo", s.toString());
            activity.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    activity.mGroupInfoText.setText(groupDetails);
                }
            });


        } else {
            activity.setIsGroupFormed(false);
            activity.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    activity.appendStatus("null");
                }
            });
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        if (p2pInfo != null) {
            Log.d("conninfo", p2pInfo.toString());
            if (p2pInfo.isGroupOwner) {
                if (!activity.isSocketCreated) {
                    Log.d(TAG, "Connected as group owner");
                    activity.getHandler().obtainMessage(MainActivity.START_CHAT).sendToTarget();
                    Thread handler;
                    try {
                        handler = new GroupOwnerSocketHandler(
                                activity.getHandler());
                        handler.start();
                        activity.isSocketCreated = true;
                    } catch (IOException e) {
                        activity.isSocketCreated = false;
                        Log.d(TAG,
                                "Failed to create a server thread - " + e.getMessage());
                    }
                }
            }
        }
    }
}
