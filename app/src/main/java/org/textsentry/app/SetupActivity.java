package org.textsentry.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Required, distraction-free entry screen for both distribution flavors. */
public final class SetupActivity extends Activity {
    private boolean openingHome;
    private boolean showingOwnKey;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (SetupGate.hasAccess(this)) {
            goHome();
        } else {
            render();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (SetupGate.hasAccess(this)) goHome();
    }

    private void goHome() {
        if (openingHome || isFinishing()) return;
        openingHome = true;
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (shared != null && !shared.isEmpty()) {
            home.setAction(Intent.ACTION_SEND).setType("text/plain");
            home.putExtra(Intent.EXTRA_TEXT, shared);
        }
        startActivity(home);
        finish();
    }

    private void render() {
        LinearLayout root = UiKit.screen(this, "Less noise. More signal.", false);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription("TextSentry icon");
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(UiKit.dp(this, 80), UiKit.dp(this, 80));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.topMargin = UiKit.dp(this, 18);
        logoParams.bottomMargin = UiKit.dp(this, 20);
        card.addView(logo, logoParams);

        TextView title = UiKit.heading(this, card, "Take back your inbox");
        title.setGravity(Gravity.CENTER);
        TextView subtitle = UiKit.description(this, card,
                "Filter unwanted political and marketing texts.");
        subtitle.setGravity(Gravity.CENTER);

        UiKit.button(this, card, "Continue with Google", true, () -> {
            Intent auth = new Intent(this, AccountActivity.class);
            auth.putExtra("welcome", true);
            startActivity(auth);
        });

        TextView ownKey = UiKit.description(this, card,
                "Skip sign-up · Bring your own key");
        ownKey.setTextSize(12);
        ownKey.setGravity(Gravity.CENTER);
        ownKey.setPaintFlags(ownKey.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        ownKey.setPadding(UiKit.dp(this, 10), UiKit.dp(this, 14),
                UiKit.dp(this, 10), UiKit.dp(this, 14));
        ownKey.setContentDescription("Skip sign-up and bring your own TypeSafe API key");
        ownKey.setOnClickListener(v -> {
            showingOwnKey = !showingOwnKey;
            render();
        });

        if (showingOwnKey) {
            EditText key = UiKit.input(this, card, "TypeSafe API key", "", false, true);
            key.setHint("Paste your API key");
            TextView error = UiKit.description(this, card, "");
            UiKit.button(this, card, "Use my key", true, () -> {
                String entered = key.getText().toString().trim();
                if (entered.isEmpty()) {
                    error.setText("Enter your API key.");
                    return;
                }
                try {
                    new KeyVault(this).save(entered);
                    if (!SetupGate.hasAccess(this)) throw new IllegalStateException();
                    goHome();
                } catch (Exception e) {
                    error.setText("Couldn't save the key. Try again.");
                }
            });
        }

        TextView note = UiKit.description(this, root, "No SMS is sent to TextSentry servers.");
        note.setGravity(Gravity.CENTER);
    }
}
