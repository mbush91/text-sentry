package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Play distribution intentionally has no SMS permissions, receiver, background
 * monitoring or SMS sends. Users supply/share text, opt in to each analysis,
 * and explicitly open their installed SMS app to compose a reply.
 */
public final class PlayUi {
    private PlayUi() {}

    public static void show(Activity activity, Settings settings, KeyVault vault) {
        new Screen(activity, settings, vault).render();
    }

    private static final class Screen {
        private final Activity activity;
        private final Settings settings;
        private final KeyVault vault;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private EditText endpoint, model, key, prompt, input, phone, reply, canned;
        private Switch jev;
        private CheckBox consent;
        private TextView status;

        Screen(Activity activity, Settings settings, KeyVault vault) {
            this.activity = activity;
            this.settings = settings;
            this.vault = vault;
        }

        void render() {
            ScrollView scroll = new ScrollView(activity);
            LinearLayout page = new LinearLayout(activity);
            page.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(18);
            page.setPadding(padding, padding, padding, padding);
            scroll.addView(page);
            activity.setContentView(scroll);

            TextView heading = new TextView(activity);
            heading.setText("TextSentry · AI message assistant");
            heading.setTextSize(25);
            heading.setGravity(Gravity.CENTER_HORIZONTAL);
            page.addView(heading);

            label(page, "Play edition: analyze text you choose to paste or share. This edition does NOT monitor incoming SMS or send SMS automatically. It does not request SMS permissions.");
            jev = new Switch(activity);
            jev.setText("TypeSafe System One (Jev) classification");
            jev.setChecked(settings.systemOne());
            page.addView(jev);
            endpoint = field(page, "HTTPS chat-completions endpoint (chat mode only)",
                    settings.endpoint(), false, false);
            model = field(page, "Model (Jev: jev-latest)", settings.model(), false, false);
            key = field(page, "Provider API key (blank keeps the saved key)", "", false, true);
            if (vault.hasKey()) key.setHint("API key saved securely on this device");
            prompt = field(page, "Your instructions", settings.prompt(), true, false);
            canned = field(page, "Fixed reply for Jev", settings.cannedReply(), false, false);
            endpoint.setEnabled(!jev.isChecked());
            canned.setEnabled(jev.isChecked());
            jev.setOnCheckedChangeListener((button, checked) -> {
                endpoint.setEnabled(!checked);
                canned.setEnabled(checked);
                if (checked && Settings.DEFAULT_MODEL.equals(model.getText().toString())) {
                    model.setText("jev-latest");
                } else if (!checked && "jev-latest".equals(model.getText().toString())) {
                    model.setText(Settings.DEFAULT_MODEL);
                }
            });

            button(page, "Save BYOK provider settings", () -> {
                try {
                    save();
                    message("Provider settings saved. No SMS access enabled.");
                } catch (Exception e) {
                    message("Unable to save. Check URL, model, rules and API key.");
                }
            });

            String incoming = "";
            Intent intent = activity.getIntent();
            if (Intent.ACTION_SEND.equals(intent.getAction())
                    && "text/plain".equals(intent.getType())) {
                CharSequence received = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (received != null) incoming = received.toString().substring(0,
                        Math.min(1600, received.length()));
            }
            input = field(page, "Paste or share text to evaluate", incoming, true, false);
            consent = new CheckBox(activity);
            consent.setText("I consent to send this text to my selected AI provider over HTTPS for this analysis. The provider's privacy and retention terms apply.");
            consent.setChecked(false);
            page.addView(consent);

            status = new TextView(activity);
            status.setText("Nothing has been sent to an AI provider.");
            page.addView(status);
            reply = field(page, "Suggested reply (editable before sending)", "", true, false);
            phone = field(page, "Recipient number (only for manual SMS compose)", "", false, false);

            button(page, "Analyze text with my AI provider", this::analyze);
            button(page, "Open SMS app to compose (never auto-sends)", this::compose);
            button(page, "Privacy policy", () -> activity.startActivity(new Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                            "https://github.com/mbush91/text-sentry/blob/main/PRIVACY.md"))));
            button(page, "Delete all local TextSentry data", this::erase);
            label(page, "TextSentry has no account, analytics service or server. Your provider key is encrypted on-device. Only text you explicitly submit is transmitted to your AI provider. Manual SMS composition is performed by your existing default messaging app.");
        }

        private void analyze() {
            if (!consent.isChecked()) {
                message("Check the AI transfer consent box before analysis.");
                return;
            }
            String sms = input.getText().toString().trim();
            String provider = endpoint.getText().toString().trim();
            String modelName = model.getText().toString().trim();
            String rule = prompt.getText().toString().trim();
            String ownReply = canned.getText().toString();
            String typedKey = key.getText().toString().trim();
            boolean systemOne = jev.isChecked();
            // Safety checks performed locally, never send OTPs or unsuitable text.
            if (!MessagePolicy.eligible("+15555550123", sms)
                    || (!systemOne && !MessagePolicy.validEndpoint(provider))) {
                message("Text blocked by local safety checks or invalid endpoint.");
                return;
            }
            if (modelName.isEmpty() || rule.isEmpty()) {
                message("Set a model and instructions first.");
                return;
            }
            status.setText("Evaluating text with the provider you selected...");
            executor.execute(() -> {
                String result;
                String suggested = "";
                try {
                    String secret = typedKey.isEmpty() ? vault.read() : typedKey;
                    AiDecision decision = systemOne
                            ? new SystemOneClient().decide(secret, modelName, rule, sms, ownReply)
                            : new DecisionClient().decide(provider, modelName, secret, rule,
                                    "+15555550123", sms);
                    if (decision.shouldReply) {
                        suggested = decision.reply;
                        result = "AI suggests replying. Review and edit before composing.";
                    } else {
                        result = "AI recommends no response.";
                    }
                } catch (Exception ignored) {
                    result = "AI evaluation failed. Verify your provider configuration.";
                }
                final String output = result, draft = suggested;
                activity.runOnUiThread(() -> {
                    status.setText(output);
                    reply.setText(draft);
                    consent.setChecked(false); // New message always requires fresh consent.
                });
            });
        }

        private void compose() {
            String number = phone.getText().toString().trim();
            String body = reply.getText().toString().trim();
            if (!number.matches("\\+?[0-9][0-9 ()-]{7,19}") ||
                    body.isEmpty() || body.length() > 120 || body.contains("\n")) {
                message("Enter a valid recipient and a reply of 1–120 characters.");
                return;
            }
            Intent send = new Intent(Intent.ACTION_SENDTO);
            send.setData(Uri.parse("smsto:" + Uri.encode(number)));
            send.putExtra("sms_body", body);
            try {
                activity.startActivity(send);
            } catch (android.content.ActivityNotFoundException e) {
                message("No installed messaging app can compose SMS.");
            }
        }

        private void save() throws Exception {
            String url = endpoint.getText().toString().trim();
            String name = model.getText().toString().trim();
            String rules = prompt.getText().toString().trim();
            String fixed = canned.getText().toString();
            if ((!jev.isChecked() && !MessagePolicy.validEndpoint(url)) || name.isEmpty()
                    || rules.isEmpty() || (jev.isChecked() && !SystemOneClient.validReply(fixed))) {
                throw new IllegalArgumentException("Invalid AI settings");
            }
            boolean changed = jev.isChecked() != settings.systemOne()
                    || (!jev.isChecked() && !url.equals(settings.endpoint()));
            if (changed) vault.clear();
            String supplied = key.getText().toString().trim();
            if (!supplied.isEmpty()) vault.save(supplied);
            settings.save(url, name, rules, false, false, jev.isChecked(), fixed);
            key.setText("");
        }

        private void erase() {
            new AlertDialog.Builder(activity).setTitle("Erase TextSentry data?")
                    .setMessage("Deletes local provider settings, encrypted key and decision history. It cannot delete messages already submitted to your AI provider.")
                    .setPositiveButton("Erase", (dialog, which) -> {
                        DataControl.deleteLocalData(activity);
                        activity.recreate();
                    })
                    .setNegativeButton("Cancel", null).show();
        }

        private EditText field(LinearLayout root, String title, String content,
                               boolean multiline, boolean password) {
            label(root, title);
            EditText inputField = new EditText(activity);
            inputField.setSingleLine(!multiline);
            if (multiline) {
                inputField.setMinLines(3);
                inputField.setGravity(Gravity.TOP);
            }
            if (password) {
                inputField.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            inputField.setText(content);
            root.addView(inputField);
            return inputField;
        }

        private void label(LinearLayout parent, String value) {
            TextView field = new TextView(activity);
            field.setText(value);
            field.setPadding(0, dp(12), 0, dp(5));
            parent.addView(field);
        }

        private void button(LinearLayout parent, String title, Runnable action) {
            Button button = new Button(activity);
            button.setText(title);
            button.setOnClickListener(v -> action.run());
            parent.addView(button);
        }

        private int dp(int n) {
            return (int) (n * activity.getResources().getDisplayMetrics().density);
        }

        private void message(String value) {
            new AlertDialog.Builder(activity).setMessage(value).setPositiveButton("OK", null)
                    .show();
        }
    }
}
