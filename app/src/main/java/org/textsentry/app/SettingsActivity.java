package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** One intentionally minimal preferences screen shared by both Android flavors. */
public final class SettingsActivity extends Activity {
    private Settings settings;
    private KeyVault vault;
    private EditText apiKey;
    private EditText prompt;
    private TextView confidenceLabel;
    private TextView keyState;
    private int confidence;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new Settings(this);
        vault = new KeyVault(this);
        confidence = settings.confidence();

        LinearLayout root = UiKit.screen(this, "Your rules. Your decisions.", true);
        LinearLayout provider = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, provider, "AI connection");
        UiKit.description(this, provider,
                "TypeSafe AI · System One. Your key is encrypted on this device.");
        UiKit.chip(this, provider, "✓ Jev · latest model");
        apiKey = UiKit.input(this, provider, "API key", "", false, true);
        apiKey.setHint("Paste your TypeSafe API key");
        keyState = UiKit.description(this, provider, "");
        refreshKey();
        UiKit.button(this, provider, "Remove stored API key", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove API key?")
                        .setMessage("This also pauses monitoring and automatic replies.")
                        .setPositiveButton("Remove", (d, w) -> {
                            settings.pause();
                            vault.clear();
                            apiKey.setText("");
                            refreshKey();
                        }).setNegativeButton("Cancel", null).show());

        LinearLayout rules = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, rules, "Decision rules");
        UiKit.description(this, rules,
                "Tell Jev which messages deserve action. Uncertain messages are ignored.");
        prompt = UiKit.input(this, rules, "Instructions", settings.prompt(), true, false);
        confidenceLabel = UiKit.description(this, rules,
                "Minimum approval probability: " + confidence + "%");
        SeekBar slider = new SeekBar(this);
        slider.setMax(50);
        slider.setProgress(confidence - 50);
        slider.setContentDescription("Minimum approval probability");
        rules.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                confidence = 50 + progress;
                confidenceLabel.setText("Minimum approval probability: " + confidence + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        UiKit.description(this, rules,
                "Higher means fewer messages qualify. Default: 95%. Jev returns a yes probability, not a guarantee.");

        LinearLayout delete = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, delete, "Messages & storage");
        UiKit.description(this, delete,
                "Clear TextSentry's local decision history at any time. This does not delete SMS from your messaging inbox.");
        UiKit.button(this, delete, "Clear local decision history", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Clear activity history?")
                        .setMessage("Local decisions will be removed; your SMS inbox is unchanged.")
                        .setPositiveButton("Clear history", (d, w) -> {
                            settings.clearHistory();
                            toast("Local history cleared");
                        }).setNegativeButton("Cancel", null).show());
        UiKit.description(this, delete,
                "Automatic deletion of original SMS requires TextSentry to become a complete default SMS app. That is not supported in this version.");

        LinearLayout privacy = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, privacy, "Privacy");
        UiKit.description(this, privacy,
                "Messages you submit are evaluated by TypeSafe AI under its terms.");
        UiKit.button(this, privacy, "View privacy policy", false, () -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://textsentry.org/privacy.html"))));
        UiKit.button(this, privacy, "Delete all TextSentry app data", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Erase TextSentry data?")
                        .setMessage("Deletes settings, saved API key and local decisions. Does not delete carrier SMS or data already sent to TypeSafe.")
                        .setPositiveButton("Erase", (d, w) -> {
                            DataControl.deleteLocalData(this);
                            finish();
                        }).setNegativeButton("Cancel", null).show());
        UiKit.button(this, root, "Save settings", true, this::save);
    }

    private void refreshKey() {
        keyState.setText(vault.hasKey()
                ? "● API key saved securely. Leave the field empty to keep it."
                : "○ No API key saved. Add one to enable analysis.");
    }

    private void save() {
        String policy = prompt.getText().toString().trim();
        String entered = apiKey.getText().toString().trim();
        try {
            // If saving the key fails, never enable automation or silently lose settings.
            if (!entered.isEmpty()) vault.save(entered);
            settings.savePolicy(policy, confidence, "STOP");
            apiKey.setText("");
            toast("Settings saved");
            finish();
        } catch (Exception ex) {
            toast("Couldn't save. Check the key and instructions.");
        }
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
