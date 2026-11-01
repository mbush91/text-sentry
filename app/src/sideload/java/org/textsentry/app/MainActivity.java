package org.textsentry.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Simple home/dashboard for the experimental SMS-monitoring flavor. */
public final class MainActivity extends Activity {
    private Settings settings;
    private KeyVault vault;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new Settings(this);
        vault = new KeyVault(this);
    }

    @Override protected void onResume() {
        super.onResume();
        showHome();
    }

    private void showHome() {
        LinearLayout root = UiKit.screen(this, "Less noise. More signal.", false);
        LinearLayout status = UiKit.card(this, root, 0xFFE4F3D8);
        UiKit.heading(this, status, "Your inbox, in your hands");
        UiKit.description(this, status, "TypeSafe AI · Jev latest");
        UiKit.chip(this, status, vault.hasKey() ? "● API key ready" : "○ Setup needed");
        String state = !settings.monitoring() ? "Monitoring paused"
                : settings.autoSend() ? "Automatic replies enabled" : "Preview mode active";
        TextView headline = UiKit.text(this, state, 22, UiKit.INK, true);
        headline.setPadding(0, UiKit.dp(this, 22), 0, 0);
        status.addView(headline);
        UiKit.description(this, status, settings.autoSend()
                ? "Eligible messages may receive STOP automatically. Carrier rates may apply."
                : "With preview mode, suggestions are logged without sending a message.");
        if (!settings.monitoring()) {
            UiKit.button(this, status, "Start preview monitoring", true, this::startPreview);
        } else {
            UiKit.button(this, status, "Pause monitoring", true, () -> {
                settings.pause();
                showHome();
            });
            if (!settings.autoSend()) {
                UiKit.button(this, status, "Enable auto-replies", false, this::confirmAutoSend);
            } else {
                UiKit.button(this, status, "Return to preview only", false, () -> {
                    settings.setAutomation(true, false);
                    showHome();
                });
            }
        }

        LinearLayout rule = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, rule, "Your rules");
        UiKit.description(this, rule, "Selected: " +
                MessageCategories.summary(settings.categoryMask()));
        UiKit.description(this, rule, "Additional restrictions: " +
                (settings.prompt().isEmpty() ? "None" : settings.prompt()));
        UiKit.chip(this, rule, "Approval threshold  " + settings.confidence() + "%");
        UiKit.button(this, rule, "Open settings  →", false, () ->
                startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout permissions = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, permissions, "SMS permissions");
        boolean canReceive = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean canSend = checkSelfPermission(Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        UiKit.description(this, permissions,
                "Receive incoming SMS: " + (canReceive ? "Granted" : "Not granted"));
        if (!canReceive) UiKit.button(this, permissions, "Allow SMS monitoring", false,
                () -> requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 102));
        UiKit.description(this, permissions,
                "Send SMS: " + (canSend ? "Granted" : "Not granted"));
        if (!canSend) UiKit.button(this, permissions, "Allow SMS sending", false,
                () -> requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 103));
        UiKit.description(this, permissions,
                "Sending remains OFF until you separately enable auto-replies. " +
                "Original SMS deletion requires a complete default SMS app; " +
                "TextSentry does not request that role or delete inbox messages.");

        LinearLayout preview = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, preview, "Test a message");
        UiKit.description(this, preview, "Try a sample safely. Testing never sends a real SMS.");
        EditText sample = UiKit.input(this, preview, "Message text",
                "Sale today! Reply STOP to unsubscribe.", true, false);
        TextView result = UiKit.description(this, preview, "No test run yet.");
        UiKit.button(this, preview, "Analyze sample", true, () -> {
            String content = sample.getText().toString();
            if (!MessagePolicy.eligible("+15555550123", content)) {
                result.setText("Blocked by local safety rules.");
                return;
            }
            if (!vault.hasKey()) {
                result.setText("Add your TypeSafe API key in Settings first.");
                return;
            }
            final String ruleText;
            try {
                ruleText = settings.effectivePrompt();
            } catch (IllegalArgumentException invalid) {
                result.setText("Settings rules are too long.");
                return;
            }
            if (ruleText.isEmpty()) {
                result.setText("Select at least one category in Settings.");
                return;
            }
            final int threshold = settings.confidence();
            result.setText("Evaluating · this test will not send an SMS…");
            new Thread(() -> {
                String answer;
                try {
                    SystemOneClient.Evaluation evaluation = new SystemOneClient().evaluate(
                            vault.read(), SystemOneClient.MODEL, ruleText,
                            content, "STOP", threshold / 100.0);
                    answer = (evaluation.decision.shouldReply ? "Suggested reply: STOP" :
                            "No reply suggested.") + " Jev match: " +
                            Math.round(evaluation.confidence * 100) + "%.";
                } catch (Exception ex) {
                    answer = "Unable to evaluate. Check your API key or connection.";
                }
                String text = answer;
                runOnUiThread(() -> result.setText(text));
            }).start();
        });

        LinearLayout recent = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, recent, "Message history");
        UiKit.description(this, recent,
                "Read the original messages TextSentry evaluated, the model match probability, " +
                "whether STOP was actually sent, and whether TextSentry deleted the SMS.");
        UiKit.button(this, recent, "View message history  →", true, () ->
                startActivity(new Intent(this, HistoryActivity.class)));
        UiKit.description(this, recent,
                "Sensitive history stays encrypted on this device; no inbox deletion is performed.");
    }

    private void startPreview() {
        if (!vault.hasKey()) {
            Toast.makeText(this, "Add a TypeSafe key in Settings first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.RECEIVE_SMS }, 100);
            return;
        }
        settings.setAutomation(true, false);
        showHome();
    }

    private void confirmAutoSend() {
        new AlertDialog.Builder(this)
                .setTitle("Enable automatic replies?")
                .setMessage("TextSentry may send STOP to eligible SMS senders when Jev's approval probability meets your threshold. A wrong decision may confirm your number is active; carrier charges may apply. You can pause at any time.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    if (checkSelfPermission(Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{ Manifest.permission.SEND_SMS }, 101);
                    } else {
                        settings.setAutomation(true, true);
                        showHome();
                    }
                })
                .setNegativeButton("Not now", null).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                      int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == 100 && granted && vault.hasKey()) {
            settings.setAutomation(true, false);
        } else if (requestCode == 101 && granted && settings.monitoring()) {
            settings.setAutomation(true, true);
        } else if ((requestCode == 102 || requestCode == 103) && !granted) {
            Toast.makeText(this, "Permission denied. No changes made.", Toast.LENGTH_LONG).show();
        } else if ((requestCode == 100 || requestCode == 101) && !granted) {
            Toast.makeText(this, "Permission not granted. No change made.", Toast.LENGTH_LONG).show();
        }
        showHome();
    }
}
