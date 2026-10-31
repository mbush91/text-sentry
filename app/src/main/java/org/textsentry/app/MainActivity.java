package org.textsentry.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/** Intentionally simple, native BYOK setup and preview UI. */
public final class MainActivity extends Activity {
    private EditText endpoint, model, prompt, key, sample, cannedReply;
    private Switch monitor, autoSend, systemOne;
    private TextView result;
    private Settings settings;
    private KeyVault vault;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(this);
        vault = new KeyVault(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("TextSentry");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        addLabel(root, "Your messages. Your rules. AI-powered replies.");
        addLabel(root, "Messages are sent to your configured AI provider only when monitoring is enabled. Auto-send is disabled by default.");

        systemOne = new Switch(this);
        systemOne.setText("Use TypeSafe System One (Jev) classification");
        systemOne.setChecked(settings.systemOne());
        root.addView(systemOne);
        addLabel(root, "System One decides whether to reply; your fixed response is never model-generated. Requires a TypeSafe BYOK key.");
        endpoint = field(root, "HTTPS chat completions endpoint (chat provider only)", settings.endpoint(), false, false);
        endpoint.setEnabled(!systemOne.isChecked());
        model = field(root, "Model identifier (jev-latest for System One)", settings.model(), false, false);
        key = field(root, "API key (leave empty to keep saved key)", "", false, true);
        if (vault.hasKey()) key.setHint("API key already saved securely");
        prompt = field(root, "Rules / instructions", settings.prompt(), true, false);
        cannedReply = field(root, "Fixed response for System One", settings.cannedReply(), false, false);
        cannedReply.setEnabled(systemOne.isChecked());
        systemOne.setOnCheckedChangeListener((button, selected) -> {
            endpoint.setEnabled(!selected);
            cannedReply.setEnabled(selected);
            if (selected && Settings.DEFAULT_MODEL.equals(model.getText().toString())) {
                model.setText("jev-latest");
            } else if (!selected && "jev-latest".equals(model.getText().toString())) {
                model.setText(Settings.DEFAULT_MODEL);
            }
        });

        monitor = new Switch(this);
        monitor.setText("Monitor incoming SMS");
        monitor.setChecked(settings.monitoring());
        root.addView(monitor);
        autoSend = new Switch(this);
        autoSend.setText("Automatically send AI-approved replies (opt-in)");
        autoSend.setChecked(settings.autoSend());
        root.addView(autoSend);
        addLabel(root, "Preview mode: with Auto-send off, incoming messages are evaluated, but no reply is transmitted.");

        Button save = button(root, "Save settings & request permissions");
        save.setOnClickListener(v -> save());

        sample = field(root, "Try a sample SMS (never sends a real SMS)",
                "Sale today! Reply STOP to unsubscribe.", true, false);
        result = new TextView(this);
        result.setText("Use Test decision to preview the response.");
        result.setPadding(0, dp(10), 0, dp(10));
        Button test = button(root, "Test decision (preview only)");
        test.setOnClickListener(v -> testDecision());
        root.addView(result);
        addLabel(root, "Recent local decisions (no message bodies stored):");
        TextView log = new TextView(this);
        log.setText(settings.history());
        root.addView(log);
        Button reload = button(root, "Refresh recent decisions");
        reload.setOnClickListener(v -> log.setText(settings.history()));
    }

    private void save() {
        String url = endpoint.getText().toString().trim();
        String modelName = model.getText().toString().trim();
        String rules = prompt.getText().toString().trim();
        if ((!systemOne.isChecked() && !MessagePolicy.validEndpoint(url))
                || modelName.isEmpty() || rules.isEmpty()
                || (systemOne.isChecked() && !SystemOneClient.validReply(cannedReply.getText().toString()))) {
            toast("Valid provider settings, rules and a safe fixed reply are required");
            return;
        }
        try {
            String updatedKey = key.getText().toString().trim();
            if (!updatedKey.isEmpty()) vault.save(updatedKey);
            if (monitor.isChecked() && !vault.hasKey()) {
                toast("Save an API key before enabling monitoring");
                monitor.setChecked(false);
                return;
            }
            settings.save(url, modelName, rules, monitor.isChecked(),
                    monitor.isChecked() && autoSend.isChecked(), systemOne.isChecked(),
                    cannedReply.getText().toString());
            key.setText("");
            if (vault.hasKey()) key.setHint("API key already saved securely");
            toast("Saved locally");
            ArrayList<String> missing = new ArrayList<>();
            if (monitor.isChecked() && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECEIVE_SMS);
            if (monitor.isChecked() && autoSend.isChecked()
                    && checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.SEND_SMS);
            if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), 1);
        } catch (Exception ignored) {
            toast("Could not encrypt API key. Monitoring not enabled.");
            monitor.setChecked(false);
            settings.save(url, modelName, rules, false, false, systemOne.isChecked(),
                    cannedReply.getText().toString());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                        int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 1) return;
        boolean receive = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean send = checkSelfPermission(Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        if (!receive || (settings.autoSend() && !send)) {
            settings.save(settings.endpoint(), settings.model(), settings.prompt(), false, false);
            monitor.setChecked(false);
            autoSend.setChecked(false);
            toast("SMS permission denied; monitoring disabled");
        }
    }

    private void testDecision() {
        String rule = prompt.getText().toString().trim();
        String input = sample.getText().toString();
        String url = endpoint.getText().toString().trim();
        String name = model.getText().toString().trim();
        // Snapshot UI state on the main thread before the background request.
        boolean useSystemOne = systemOne.isChecked();
        String configuredReply = cannedReply.getText().toString();
        String suppliedKey = key.getText().toString().trim();
        if (!MessagePolicy.eligible("+15555550123", input)
                || (!useSystemOne && !MessagePolicy.validEndpoint(url))) {
            result.setText("Preview blocked by local safety rules or invalid endpoint.");
            return;
        }
        result.setText("Evaluating preview; no SMS will be sent.");
        new Thread(() -> {
            String text;
            try {
                String secret = suppliedKey.isEmpty() ? vault.read() : suppliedKey;
                AiDecision decision = useSystemOne
                        ? new SystemOneClient().decide(secret, name, rule, input,
                                configuredReply)
                        : new DecisionClient().decide(
                                url, name, secret, rule, "+15555550123", input);
                text = decision.shouldReply ? "Preview reply: " + decision.reply : "Preview: no reply";
            } catch (Exception ignored) {
                text = "Preview failed. Check provider URL, key and model.";
            }
            String show = text;
            runOnUiThread(() -> result.setText(show));
        }).start();
    }

    private EditText field(LinearLayout root, String label, String value, boolean multi, boolean secret) {
        addLabel(root, label);
        EditText edit = new EditText(this);
        edit.setSingleLine(!multi);
        if (multi) { edit.setMinLines(3); edit.setGravity(Gravity.TOP); }
        if (secret) edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setText(value);
        root.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        return edit;
    }

    private void addLabel(LinearLayout root, String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(15);
        text.setPadding(0, dp(14), 0, dp(4));
        root.addView(text);
    }

    private Button button(LinearLayout root, String name) {
        Button button = new Button(this);
        button.setText(name);
        root.addView(button);
        return button;
    }

    private int dp(int amount) {
        return (int) (amount * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
