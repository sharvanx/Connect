package ml.yats.connect;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;
    private WiFiChatFragment chatFragment;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiDirectBroadcastReceiver receiver = null;
    private boolean isWifiP2pEnabled = false;
    private ArrayList<WiFiP2pService> deviceList = new ArrayList<WiFiP2pService>();
    private WifiP2pDnsSdServiceRequest serviceRequest;
    protected TextView mGroupInfoText;
    protected boolean isGroupFormed = false;
    static final int SELECT_MODE = 0;
    private String mode;
    private Handler handler = new Handler(this);

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_connect";
    public static final String SERVICE_REG_TYPE = "_tcp";

    public static final String TAG = "Connect main";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int START_CHAT = 0x400 + 3;

    static final int SERVER_PORT = 4545;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void setIsGroupFormed(boolean isGroupFormed) {
        this.isGroupFormed = isGroupFormed;
    }

    public Handler getHandler(){ return handler; }
    public void setHandler(Handler handler) {
        this.handler = handler;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mGroupInfoText = (TextView) findViewById(R.id.groupInfoText);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        initialize();
    }

    public void onResume() {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void initialize() {
        mode = getIntent().getStringExtra("mode");
        if (!isGroupFormed) {
            if (mode.equals("router")) {
                manager.createGroup(channel, receiver);
                Log.d("main", "creating group");
            } else {
                Log.d("main", "client mode");
                discoverService();
            }
        }
    }

    public void startRegistration(Map<String,String> s) {
        Map<String, String> record = new HashMap<String, String>(s);
        Log.d("startreg main", s.toString());
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });
    }
    public void appendStatus(String status) {
        String current = mGroupInfoText.getText().toString();
        mGroupInfoText.setText(current + "\n" + status);
    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            // update the UI and add the item the discovered
                            // device.
//                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
//                                    .findFragmentByTag("services");
//                            if (fragment != null) {
//                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
//                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                deviceList.add(service);
//                                adapter.notifyDataSetChanged();
                                Log.d("main", "onBonjourServiceAvailable "
                                        + instanceName);
                            }
                        }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d("client: ",record.toString());
                        WifiConfiguration conf = new WifiConfiguration();
                        conf.SSID = "\"" + record.get("SSID") + "\"";
                        conf.preSharedKey = "\""+ record.get("Password") +"\"";
                        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                        wifiManager.addNetwork(conf);
                        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                        for( WifiConfiguration i : list ) {
                            if(i.SSID != null && i.SSID.equals("\"" + record.get("SSID") + "\"")) {
                                wifiManager.disconnect();
                                wifiManager.enableNetwork(i.networkId, true);
                                wifiManager.reconnect();
                                break;
                            }
                        }
                        for(WiFiP2pService s: deviceList){
                            if(s.device.deviceName.equals(device.deviceName)){
                                s.record.putAll(record);
                                s.fullDomainName = fullDomainName;
                            }
                        }
                        Log.d("netconf",record.toString());
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });

        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");

            }
        });
    }

    /*@Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
    }
*/
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case START_CHAT:
                Log.d("chat","starting chat");
                chatFragment = new WiFiChatFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.container_root, chatFragment).commit();
                mGroupInfoText.setVisibility(View.GONE);
                break;
            case MESSAGE_READ:
                Log.d("chat","message read");
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                (chatFragment).pushMessage("Buddy: " + readMessage);
                //appendStatus("Buddy: " + readMessage);
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                (chatFragment).setChatManager((ChatManager) obj);

        }
        return true;
    }
}