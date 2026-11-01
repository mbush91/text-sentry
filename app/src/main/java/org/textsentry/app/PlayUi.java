package org.textsentry.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Play flavor never reads, deletes or sends carrier SMS; it reviews user-submitted text. */
public final class PlayUi {
    private PlayUi() {}

    public static void show(Activity activity, Settings settings, KeyVault vault) {
        LinearLayout root = UiKit.screen(activity, "Less noise. More signal.", false);

        LinearLayout intro = UiKit.card(activity, root, 0xFFE4F3D8);
        UiKit.heading(activity, intro, "A calmer way to message");
        UiKit.description(activity, intro,
                "Review the texts you choose with TypeSafe AI. Nothing is analyzed until you ask.");
        UiKit.chip(activity, intro, vault.hasKey() ? "● TypeSafe ready · Jev latest"
                : "○ TypeSafe setup needed");
        UiKit.button(activity, intro, "Settings  →", false, () ->
                activity.startActivity(new Intent(activity, SettingsActivity.class)));

        LinearLayout review = UiKit.card(activity, root, 0xFFFFFFFF);
        UiKit.heading(activity, review, "Review a message");
        UiKit.description(activity, review, "Paste or share text, then opt in to this analysis.");
        String incoming = "";
        Intent incomingIntent = activity.getIntent();
        if (Intent.ACTION_SEND.equals(incomingIntent.getAction())
                && "text/plain".equals(incomingIntent.getType())) {
            CharSequence shared = incomingIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) incoming = shared.subSequence(0, Math.min(1600, shared.length())).toString();
        }
        EditText message = UiKit.input(activity, review, "Message", incoming, true, false);
        CheckBox consent = new CheckBox(activity);
        consent.setText("I agree to send this message to TypeSafe AI for this analysis. TypeSafe's privacy terms apply.");
        consent.setChecked(false);
        review.addView(consent);
        TextView outcome = UiKit.description(activity, review,
                "Ready when you are. This app never monitors or sends SMS automatically.");
        EditText response = UiKit.input(activity, review, "Suggested reply (editable)", "", true, false);
        UiKit.button(activity, review, "Analyze with TypeSafe", true, () -> {
            if (!consent.isChecked()) {
                outcome.setText("Please consent to this analysis first.");
                return;
            }
            String input = message.getText().toString().trim();
            if (!MessagePolicy.eligible("+15555550123", input)) {
                outcome.setText("This text is blocked by local safety rules.");
                return;
            }
            if (!vault.hasKey()) {
                outcome.setText("Add your TypeSafe API key in Settings first.");
                return;
            }
            final String policy = settings.prompt();
            final double threshold = settings.confidence() / 100.0;
            consent.setChecked(false);
            outcome.setText("Analyzing with TypeSafe…");
            new Thread(() -> {
                String status, suggestion;
                try {
                    AiDecision answer = new SystemOneClient().decide(vault.read(),
                            SystemOneClient.MODEL, policy, input, "STOP", threshold);
                    status = answer.shouldReply
                            ? "Suggested reply. Review before composing."
                            : "No reply recommended.";
                    suggestion = answer.shouldReply ? "STOP" : "";
                } catch (Exception ignored) {
                    status = "Analysis failed. Check your API key or connection.";
                    suggestion = "";
                }
                String result = status, draft = suggestion;
                activity.runOnUiThread(() -> {
                    outcome.setText(result);
                    response.setText(draft);
                });
            }).start();
        });

        LinearLayout compose = UiKit.card(activity, root, 0xFFFFFFFF);
        UiKit.heading(activity, compose, "Send only if you choose");
        UiKit.description(activity, compose,
                "The Play edition opens your usual SMS app. Sending always remains your choice.");
        EditText number = UiKit.input(activity, compose, "Recipient number", "", false, false);
        UiKit.button(activity, compose, "Open SMS composer  ↗", false, () -> {
            String to = number.getText().toString().trim();
            String body = response.getText().toString().trim();
            if (!to.matches("\\+?[0-9][0-9 ()-]{7,19}") ||
                    !SystemOneClient.validReply(body)) {
                outcome.setText("Enter a valid phone number and reply of up to 120 characters.");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + Uri.encode(to)));
            intent.putExtra("sms_body", body);
            try {
                activity.startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                outcome.setText("No SMS app is available for manual composition.");
            }
        });

        LinearLayout footer = UiKit.card(activity, root, 0xFFFFFFFF);
        UiKit.heading(activity, footer, "You stay in control");
        UiKit.description(activity, footer,
                "Jev latest · " + settings.confidence() +
                "% minimum match probability. Manage your key, instructions, and local history in Settings.");
        UiKit.button(activity, footer, "Open settings", false, () ->
                activity.startActivity(new Intent(activity, SettingsActivity.class)));
    }
}
