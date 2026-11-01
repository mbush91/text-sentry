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

        LinearLayout intro = UiKit.card(activity, root, UiKit.HIGHLIGHT);
        UiKit.heading(activity, intro, "A calmer way to message");
        UiKit.description(activity, intro,
                "Review only the texts you choose. Nothing is analyzed until you ask.");
        UiKit.chip(activity, intro, AnalysisAccess.status(activity));
        UiKit.button(activity, intro, "View history  →", false, () ->
                activity.startActivity(new Intent(activity, HistoryActivity.class)));
        UiKit.button(activity, intro, "Google account / Sign up  →", false, () ->
                activity.startActivity(new Intent(activity, AccountActivity.class)));
        UiKit.button(activity, intro, "Settings  →", false, () ->
                activity.startActivity(new Intent(activity, SettingsActivity.class)));

        LinearLayout review = UiKit.card(activity, root, 0xFFFFFFFF);
        UiKit.heading(activity, review, "Review a message");
        UiKit.description(activity, review,
                "Selected: " + MessageCategories.summary(settings.categoryMask()) +
                ". Paste or share a text, then opt in to analysis.");
        String incoming = "";
        Intent incomingIntent = activity.getIntent();
        if (Intent.ACTION_SEND.equals(incomingIntent.getAction())
                && "text/plain".equals(incomingIntent.getType())) {
            CharSequence shared = incomingIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) incoming = shared.subSequence(0, Math.min(1600, shared.length())).toString();
        }
        EditText message = UiKit.input(activity, review, "Message", incoming, true, false);
        CheckBox consent = new CheckBox(activity);
        consent.setText("I agree to send this text directly to TypeSafe Jev " +
                "(through OpenRouter when signed in). Provider privacy terms apply.");
        consent.setChecked(false);
        review.addView(consent);
        TextView outcome = UiKit.description(activity, review,
                "Ready when you are. This app never monitors or sends SMS automatically.");
        EditText response = UiKit.input(activity, review, "Suggested reply (editable)", "", true, false);
        UiKit.button(activity, review, "Analyze message", true, () -> {
            if (!consent.isChecked()) {
                outcome.setText("Please consent to this analysis first.");
                return;
            }
            String input = message.getText().toString().trim();
            if (!MessagePolicy.eligible("+15555550123", input)) {
                outcome.setText("This text is blocked by local safety rules.");
                return;
            }
            if (!AnalysisAccess.hasProvider(activity)) {
                outcome.setText("Sign in or bring your own key to analyze.");
                return;
            }
            final String policy;
            try {
                policy = settings.effectivePrompt();
            } catch (IllegalArgumentException invalid) {
                outcome.setText("Combined rules are too long. Update Settings.");
                return;
            }
            if (policy.isEmpty()) {
                outcome.setText("Select a message category in Settings first.");
                return;
            }
            final double threshold = settings.confidence() / 100.0;
            consent.setChecked(false);
            outcome.setText("Analyzing with Jev…");
            new Thread(() -> {
                String status, suggestion;
                try {
                    SystemOneClient.Evaluation evaluation = AnalysisAccess.evaluate(activity,
                            policy, input, threshold,
                            DeveloperMode.traceSink(activity, "manual"));
                    AiDecision answer = evaluation.decision;
                    new HistoryStore(activity).add("Manual review", input, evaluation.confidence,
                            answer.shouldReply ? "STOP" : "",
                            answer.shouldReply ? "manual_suggestion" : "no_reply", "manual");
                    status = answer.shouldReply
                            ? "Suggested reply (" + Math.round(evaluation.confidence * 100)
                                + "%). Review before composing."
                            : "No reply recommended (" + Math.round(evaluation.confidence * 100) + "%).";
                    suggestion = answer.shouldReply ? "STOP" : "";
                } catch (Exception ignored) {
                    status = "Analysis failed. Check your account/key or connection.";
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
                "% minimum match probability. Manage categories, instructions, and local history in Settings.");
        UiKit.button(activity, footer, "Open settings", false, () ->
                activity.startActivity(new Intent(activity, SettingsActivity.class)));
    }
}
