package ml.yats.connect;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SelectorActivity extends AppCompatActivity {

    private Button mRouter;
    private Button mClient;
    private EditText mPhoneText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        mRouter = (Button) findViewById(R.id.button_router);
        mClient = (Button) findViewById(R.id.button_client);
        mPhoneText = (EditText) findViewById(R.id.phoneText);

        mRouter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent("ml.yats.connect.mainactivity");
                i.putExtra("mode","router");
                i.putExtra("number",mPhoneText.getText().toString());
                startActivity(i);

            }
        });

        mClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent("ml.yats.connect.mainactivity");
                i.putExtra("mode","client");
                i.putExtra("number",mPhoneText.getText().toString());
                startActivity(i);
            }
        });
    }
}
