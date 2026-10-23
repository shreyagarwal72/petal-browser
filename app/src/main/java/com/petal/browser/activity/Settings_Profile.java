package com.petal.browser.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petal.browser.account.PetalAccountSyncBridge;

public class Settings_Profile extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(PetalAccountSyncBridge.createAccountSyncView(
            this,
            new kotlin.jvm.functions.Function0<kotlin.Unit>() {
                @Override
                public kotlin.Unit invoke() {
                    finish();
                    return kotlin.Unit.INSTANCE;
                }
            },
            new kotlin.jvm.functions.Function1<Object, kotlin.Unit>() {
                @Override
                public kotlin.Unit invoke(Object shortcutObj) {
                    if (shortcutObj instanceof com.petal.browser.account.PetalShortcut) {
                        com.petal.browser.account.PetalShortcut shortcut = (com.petal.browser.account.PetalShortcut) shortcutObj;
                        if (shortcut.getUrl() != null && !shortcut.getUrl().trim().isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.getUrl().trim()));
                            intent.setClass(Settings_Profile.this, BrowserActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        }
                    }
                    return kotlin.Unit.INSTANCE;
                }
            }
        ));
    }
}