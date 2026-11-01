package org.textsentry.app;

import android.app.Activity;
import android.os.Bundle;

/** Play edition only analyzes text when users request it. */
public final class MainActivity extends Activity {
    private String lastSettingsGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SetupGate.enforce(this)) return;
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!SetupGate.enforce(this)) return;
        // Keep the user's in-progress draft when returning from their SMS app.
        Settings settings = new Settings(this);
        if (lastSettingsGeneration != null &&
                !lastSettingsGeneration.equals(settings.generation())) refresh();
    }

    private void refresh() {
        Settings settings = new Settings(this);
        lastSettingsGeneration = settings.generation();
        PlayUi.show(this, settings, new KeyVault(this));
    }
}
