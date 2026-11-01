package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CheckBox;
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
    private CheckBox voteCategory, politicalCategory, marketingCategory;
    private TextView generatedInstructions;
    private TextView confidenceLabel;
    private TextView keyState;
    private int confidence;
    private int developerTaps;
    private LinearLayout root, developerCard;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SetupGate.enforce(this)) return;
        settings = new Settings(this);
        vault = new KeyVault(this);
        confidence = settings.confidence();

        root = UiKit.screen(this, "Settings", true);
        LinearLayout provider = UiKit.card(this, root, 0xFFFFFFFF);
        TextView heading = UiKit.heading(this, provider, "AI connection");
        // Easter egg: seven taps still unlock encrypted developer diagnostics.
        heading.setOnClickListener(v -> {
            if (settings.developerMode()) return;
            if (++developerTaps >= 7) {
                developerTaps = 0;
                new AlertDialog.Builder(this)
                        .setTitle("Enable developer mode?")
                        .setMessage("Stores up to five encrypted model requests and responses on this device.")
                        .setPositiveButton("Enable", (dialog, which) -> {
                            settings.setDeveloperMode(true);
                            refreshDeveloperCard();
                        }).setNegativeButton("Cancel", null).show();
            }
        });
        UiKit.chip(this, provider, AnalysisAccess.status(this));
        UiKit.button(this, provider, "Manage account", false, () ->
                startActivity(new Intent(this, AccountActivity.class)));
        apiKey = UiKit.input(this, provider, "Your TypeSafe key (optional)", "", false, true);
        apiKey.setHint("API key");
        keyState = UiKit.description(this, provider, "");
        refreshKey();
        UiKit.button(this, provider, "Remove BYOK key", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Remove your key?")
                        .setMessage("Monitoring will pause if you aren't signed in.")
                        .setPositiveButton("Remove", (d, w) -> {
                            if (!new AuthSessionStore(this).hasSession()) settings.pause();
                            vault.clear();
                            apiKey.setText("");
                            refreshKey();
                            if (!SetupGate.hasAccess(this)) SetupGate.enforce(this);
                        }).setNegativeButton("Cancel", null).show());

        LinearLayout rules = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, rules, "Filter categories");
        voteCategory = addCategory(rules, "Voting reminders", "", MessageCategories.VOTE);
        politicalCategory = addCategory(rules, "Political campaigns", "", MessageCategories.POLITICAL);
        marketingCategory = addCategory(rules, "Marketing", "", MessageCategories.MARKETING);
        generatedInstructions = UiKit.description(this, rules, "");
        generatedInstructions.setVisibility(android.view.View.GONE);
        confidenceLabel = UiKit.description(this, rules, "Minimum match: " + confidence + "%");
        SeekBar slider = new SeekBar(this);
        slider.setMax(50);
        slider.setProgress(confidence - 50);
        slider.setContentDescription("Minimum match confidence");
        rules.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                confidence = 50 + progress;
                confidenceLabel.setText("Minimum match: " + confidence + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        UiKit.button(this, rules, "Save settings", true, this::save);

        LinearLayout data = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, data, "Data & privacy");
        UiKit.button(this, data, "History", false, () ->
                startActivity(new Intent(this, HistoryActivity.class)));
        UiKit.button(this, data, "Clear history", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Clear local history?")
                        .setMessage("Only TextSentry history is deleted, not your SMS inbox.")
                        .setPositiveButton("Clear", (d, w) -> {
                            settings.clearHistory();
                            toast("History cleared");
                        }).setNegativeButton("Cancel", null).show());
        UiKit.button(this, data, "Privacy policy", false, () -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://textsentry.org/privacy.html"))));
        UiKit.button(this, data, "Delete app data", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Erase TextSentry data?")
                        .setMessage("Removes settings, keys and local history. Does not delete SMS.")
                        .setPositiveButton("Erase", (d, w) -> {
                            DataControl.deleteLocalData(this);
                            finish();
                        }).setNegativeButton("Cancel", null).show());
    }

    private CheckBox addCategory(LinearLayout parent, String title, String subtitle, int bit) {
        CheckBox box = new CheckBox(this);
        box.setText(title);
        box.setTextSize(15);
        box.setTextColor(UiKit.INK);
        box.setPadding(0, UiKit.dp(this, 10), 0, 0);
        box.setChecked((settings.categoryMask() & bit) != 0);
        parent.addView(box);
        return box;
    }

    private int selectedMask() {
        int selected = 0;
        if (voteCategory.isChecked()) selected |= MessageCategories.VOTE;
        if (politicalCategory.isChecked()) selected |= MessageCategories.POLITICAL;
        if (marketingCategory.isChecked()) selected |= MessageCategories.MARKETING;
        return selected;
    }

    private void updatePreview() {
        try {
            String combined = MessageCategories.compose(selectedMask(), settings.prompt());
            generatedInstructions.setText(combined.isEmpty()
                    ? "No categories selected — messages will not be evaluated."
                    : "Combined AI instructions (one request):\n" + combined);
        } catch (IllegalArgumentException invalid) {
            generatedInstructions.setText("Rules are too long. Shorten the extra instructions.");
        }
    }

    private void refreshKey() {
        keyState.setText(vault.hasKey()
                ? "✓ Own key saved"
                : "Optional for signed-in users");
    }

    private void save() {
        String policy = settings.prompt(); // Keep the developer prompt on normal saves.
        String entered = apiKey.getText().toString().trim();
        try {
            // If saving the key fails, never enable automation or silently lose settings.
            if (!entered.isEmpty()) vault.save(entered);
            settings.saveCategoryPolicy(policy, confidence, selectedMask());
            apiKey.setText("");
            toast("Settings saved");
            finish();
        } catch (Exception ex) {
            toast("Couldn't save. Check the key and instructions.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (!SetupGate.enforce(this)) return;
        if (root != null) {
            refreshDeveloperCard();
        }
    }

    private void refreshDeveloperCard() {
        if (!settings.developerMode()) {
            if (developerCard != null) {
                root.removeView(developerCard);
                developerCard = null;
            }
            return;
        }
        if (developerCard != null) return;
        developerCard = UiKit.card(this, root, UiKit.HIGHLIGHT);
        UiKit.heading(this, developerCard, "Developer mode");

        UiKit.button(this, developerCard, "Open developer tools  →", true, () ->
                startActivity(new Intent(this, DeveloperActivity.class)));
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
