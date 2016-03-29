
package ml.yats.connect;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * A structure to hold service information.
 */
public class WiFiP2pService {
    WifiP2pDevice device;
    String instanceName = null;
    String serviceRegistrationType = null;
    String fullDomainName = null;
    Map<String, String> record = new HashMap<String,String>();
}
