
package ml.yats.connect;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class ChatManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    protected boolean isServer;

    public ChatManager(Socket socket, Handler handler, boolean isServer) {
        this.socket = socket;
        this.handler = handler;
        this.isServer=isServer;
        GroupOwnerSocketHandler.chats.add(this);
    }

    protected InputStream iStream;
    protected OutputStream oStream;
    private static final String TAG = "ChatHandler";

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(MainActivity.MY_HANDLE, this)
                    .sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(MainActivity.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
                    if(isServer){
                        for(ChatManager chat : GroupOwnerSocketHandler.chats) {
                            if(chat!=this)
                            chat.write(buffer);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                GroupOwnerSocketHandler.chats.remove(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

}
