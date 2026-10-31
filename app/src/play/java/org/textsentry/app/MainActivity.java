package org.textsentry.app;

import android.app.Activity;
import android.os.Bundle;

/** Play edition delegates to manually initiated review UI. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PlayUi.show(this, new Settings(this), new KeyVault(this));
    }
}
