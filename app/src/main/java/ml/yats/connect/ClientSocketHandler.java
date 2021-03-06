
package ml.yats.connect;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private ChatManager chat;
    public ClientSocketHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress("192.168.49.1",
                    MainActivity.SERVER_PORT), 10000);
            Log.d(TAG, "Launching the client I/O handler");
            chat = new ChatManager(socket, handler, false);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public ChatManager getChat() {
        return chat;
    }

}
