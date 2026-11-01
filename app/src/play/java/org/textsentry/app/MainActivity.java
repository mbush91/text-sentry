package org.textsentry.app;

import android.app.Activity;
import android.os.Bundle;

/** Permission-minimized Play home; any SMS compose action is user-initiated. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        // Refresh key readiness and confidence when returning from Settings.
        PlayUi.show(this, new Settings(this), new KeyVault(this));
    }
}
