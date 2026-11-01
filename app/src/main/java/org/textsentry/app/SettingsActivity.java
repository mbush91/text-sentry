package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CheckBox;
import android.text.Editable;
import android.text.TextWatcher;
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
    private CheckBox voteCategory, politicalCategory, marketingCategory;
    private TextView generatedInstructions;
    private TextView confidenceLabel;
    private TextView keyState;
    private int confidence;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new Settings(this);
        vault = new KeyVault(this);
        confidence = settings.confidence();

        LinearLayout root = UiKit.screen(this, "Your rules. Your decisions.", true);
        LinearLayout account = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, account, "TextSentry account");
        UiKit.description(this, account,
                "Create an account or sign in securely with Google. This does not change your " +
                "current TypeSafe key or enable automatic replies.");
        UiKit.button(this, account, "Sign in / Manage account", false, () ->
                startActivity(new Intent(this, AccountActivity.class)));

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

        UiKit.heading(this, rules, "Additional instructions");
        prompt = UiKit.input(this, rules, "Optional extra restrictions",
                settings.prompt(), true, false);
        prompt.setHint("e.g., Ignore messages from local charities");
        UiKit.description(this, rules,
                "Your selected categories and these extra restrictions are combined " +
                "into one TypeSafe request per eligible message.");
        generatedInstructions = UiKit.description(this, rules, "");
        generatedInstructions.setMaxLines(12);
        voteCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        politicalCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        marketingCategory.setOnCheckedChangeListener((b, checked) -> updatePreview());
        prompt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence t, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence t, int start, int before, int count) {
                updatePreview();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
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
            String combined = MessageCategories.compose(selectedMask(), prompt.getText().toString());
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
                : "○ No API key saved. Add one to enable analysis.");
    }

    private void save() {
        String policy = prompt.getText().toString().trim();
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

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
