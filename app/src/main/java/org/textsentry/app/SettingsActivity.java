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
        settings = new Settings(this);
        vault = new KeyVault(this);
        confidence = settings.confidence();

        root = UiKit.screen(this, "Your rules. Your decisions.", true);
        LinearLayout account = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, account, "TextSentry account");
        UiKit.description(this, account, AnalysisAccess.status(this));
        UiKit.description(this, account,
                "Google sign-in uses a $1/month managed Jev key. " +
                "Otherwise you can use your own TypeSafe key below.");
        UiKit.button(this, account, "Sign in / Manage account", false, () ->
                startActivity(new Intent(this, AccountActivity.class)));

        LinearLayout provider = UiKit.card(this, root, 0xFFFFFFFF);
        TextView heading = UiKit.heading(this, provider, "AI connection");
        // Easter egg: tap the AI connection heading seven times.
        heading.setOnClickListener(v -> {
            if (settings.developerMode()) return;
            if (++developerTaps >= 7) {
                developerTaps = 0;
                new AlertDialog.Builder(this)
                        .setTitle("Enable developer mode?")
                        .setMessage("Developer mode keeps up to five complete Jev request and " +
                                "response bodies locally, including sensitive SMS text and rules. " +
                                "The log is encrypted; API keys are not included.")
                        .setPositiveButton("Enable", (dialog, which) -> {
                            settings.setDeveloperMode(true);
                            refreshDeveloperCard();
                            toast("Developer mode enabled");
                        }).setNegativeButton("Cancel", null).show();
            }
        });
        UiKit.description(this, provider,
                "Optional: bring your own TypeSafe key without an account. " +
                "Saved only on this device. Google sign-in takes priority if connected.");
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
        UiKit.heading(this, rules, "Message categories");
        UiKit.description(this, rules,
                "Choose which types of unsolicited texts Jev should consider for a STOP reply. " +
                "A message only needs to match one selected category.");
        voteCategory = addCategory(rules, "Go vote reminders",
                "Unsolicited nonpartisan reminders to vote.", MessageCategories.VOTE);
        politicalCategory = addCategory(rules, "Political campaign messages",
                "Candidate, party, fundraising or campaign texts.", MessageCategories.POLITICAL);
        marketingCategory = addCategory(rules, "Marketing ads",
                "Retail promotions, sales, coupons and advertising.", MessageCategories.MARKETING);
        UiKit.description(this, rules,
                "A message does not need to mention STOP or provide an unsubscribe option. " +
                "Official voting information, security codes and personal texts remain excluded.");

        UiKit.description(this, rules,
                "Additional prompting, when configured, is combined after the selected " +
                "categories in the same Jev request.");
        generatedInstructions = UiKit.description(this, rules, "");
        generatedInstructions.setMaxLines(12);
        voteCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        politicalCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        marketingCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        updatePreview();
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
        UiKit.button(this, delete, "View message history", false, () ->
                startActivity(new Intent(this, HistoryActivity.class)));
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

    private CheckBox addCategory(LinearLayout parent, String title, String subtitle, int bit) {
        CheckBox box = new CheckBox(this);
        box.setText(title);
        box.setTextSize(15);
        box.setTextColor(UiKit.INK);
        box.setPadding(0, UiKit.dp(this, 10), 0, 0);
        box.setChecked((settings.categoryMask() & bit) != 0);
        parent.addView(box);
        UiKit.description(this, parent, subtitle);
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
                ? "● API key saved securely. Leave the field empty to keep it."
                : "○ No BYOK key saved. Google sign-in works without one.");
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
        if (root != null) {
            refreshDeveloperCard();
            updatePreview();
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
        UiKit.description(this, developerCard,
                "Advanced prompting and encrypted Jev request/response traces.");
        UiKit.button(this, developerCard, "Open developer tools  →", true, () ->
                startActivity(new Intent(this, DeveloperActivity.class)));
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
