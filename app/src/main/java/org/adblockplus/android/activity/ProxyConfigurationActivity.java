package org.adblockplus.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.adblockplus.android.R;
import org.adblockplus.android.Utils;
import org.adblockplus.android.service.ProxyService;

public class ProxyConfigurationActivity extends AppCompatActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxyconfiguration);
        final int port = getIntent().getIntExtra("port", 0);

        final StringBuilder info = new StringBuilder();
        final int textId = ProxyService.GLOBAL_PROXY_USER_CONFIGURABLE ? R.raw.proxysettings : R.raw.proxysettings_old;
        Utils.appendRawTextFile(this, info, textId);
        final String msg = String.format(info.toString(), port);

        final TextView tv = (TextView) findViewById(R.id.message_text);
        tv.setText(Html.fromHtml(msg));
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        final Button buttonToHide = (Button) findViewById(ProxyService.GLOBAL_PROXY_USER_CONFIGURABLE ? R.id.gotit : R.id.opensettings);
        buttonToHide.setVisibility(View.GONE);
    }

    public void onGotit(final View view) {
        finish();
    }

    public void onSettings(final View view) {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        finish();
    }
}
