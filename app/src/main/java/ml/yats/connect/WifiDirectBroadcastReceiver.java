package ml.yats.connect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.IOException;
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
                                       MainActivity activity){
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }
        Log.d("receiver","P2P Connection Changed");
            manager.requestGroupInfo(channel, this);
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                manager.requestConnectionInfo(channel, this);
            } else {
                // It's a disconnect
                Log.d("receiver", "not connected");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {

            WifiP2pDevice device = (WifiP2pDevice) intent
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
        Log.d("main","Local Group failed, error code " + reason);
    }


    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if(group!=null) {
            activity.setIsGroupFormed(true);
            Map<String, String> s = new HashMap<String, String>();
            s.put("SSID", group.getNetworkName());
            s.put("Password", group.getPassphrase());
            WifiP2pDevice owner = group.getOwner();
            s.put("Address", owner.deviceAddress);
            //s.put("Name", owner.deviceName);
            //s.put("Status", Integer.toString(owner.status));
            final String groupDetails = s.toString();
            if(group.isGroupOwner()) {
                activity.startRegistration(s);
            }
            Log.d("groupinfo", s.toString());
            activity.getHandler().post(new Runnable() {
                @Override
                public void run() {
                activity.mGroupInfoText.setText(groupDetails);
                }
            });


        }
        else {
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
        String s="";
        if(p2pInfo!=null) {
            s = p2pInfo.toString();
        }
        Log.d("conninfo",s);
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        activity.getHandler().obtainMessage(MainActivity.START_CHAT);
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
                        activity.getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    activity.getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }
        //activity.getHandler().obtainMessage(MainActivity.START_CHAT);
    }
}
