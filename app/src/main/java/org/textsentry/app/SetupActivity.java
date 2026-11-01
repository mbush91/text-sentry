package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Four short steps: account, battery, SMS permissions, opt-in start. */
public final class SetupActivity extends Activity {
    private static final int INCOMING_SMS_REQUEST = 301;
    private static final int OUTGOING_SMS_REQUEST = 302;
    private boolean openingHome;
    private boolean showingOwnKey;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) showingOwnKey = savedState.getBoolean("show_own_key", false);
        display();
    }

    @Override protected void onResume() {
        super.onResume();
        display();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("show_own_key", showingOwnKey);
        super.onSaveInstanceState(out);
    }

    private void display() {
        if (isFinishing() || openingHome) return;
        if (SetupGate.canEnterApp(this)) {
            goHome();
            return;
        }
        if (!SetupGate.hasAccess(this)) {
            renderLogin();
        } else if (!SetupPermissions.supportsMonitoring()) {
            renderManualStart();
        } else {
            switch (SetupFlow.stage(this)) {
                case SetupFlow.PERMISSIONS:
                    renderPermissions();
                    break;
                case SetupFlow.ACTIVATE:
                    renderActivation();
                    break;
                default:
                    renderBattery();
            }
        }
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

    private LinearLayout stage(String label, String title, String subtitle, int number) {
        LinearLayout root = UiKit.screen(this, "", false);
        LinearLayout progress = UiKit.vertical(this);
        progress.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(progress);

        LinearLayout segments = new LinearLayout(this);
        segments.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 1; i <= (SetupPermissions.supportsMonitoring() ? 4 : 2); i++) {
            android.view.View bar = new android.view.View(this);
            bar.setBackground(UiKit.shape(i <= number ? UiKit.ACCENT : 0xFFD6E0F0,
                    UiKit.dp(this, 8), 0));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, UiKit.dp(this, 5), 1);
            p.setMargins(UiKit.dp(this, 3), 0, UiKit.dp(this, 3), 0);
            segments.addView(bar, p);
        }
        progress.addView(segments, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 5)));

        TextView step = UiKit.text(this, label.toUpperCase(java.util.Locale.US),
                12, UiKit.MUTED, true);
        step.setPadding(0, UiKit.dp(this, 24), 0, UiKit.dp(this, 5));
        root.addView(step);
        TextView heading = UiKit.text(this, title, 29, UiKit.INK, true);
        heading.setLetterSpacing(-0.035f);
        root.addView(heading);
        TextView description = UiKit.text(this, subtitle, 15, UiKit.MUTED, false);
        description.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 18));
        root.addView(description);
        return root;
    }

    private void renderLogin() {
        LinearLayout root = stage("Step 1 of 4 · Account",
                "Welcome to TextSentry", "Stop unwanted political and marketing texts.", 1);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription("TextSentry logo");
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(UiKit.dp(this, 84), UiKit.dp(this, 84));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.topMargin = UiKit.dp(this, 8);
        logoParams.bottomMargin = UiKit.dp(this, 16);
        root.addView(logo, logoParams);

        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.googleSignInButton(this, card, () -> {
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra("welcome", true);
            intent.putExtra("auto_login", true);
            startActivity(intent);
        });

        TextView skip = UiKit.description(this, card, "Or use your own TypeSafe API key");
        skip.setGravity(Gravity.CENTER);
        skip.setTextSize(13);
        skip.setTextColor(UiKit.INK);
        skip.setPaintFlags(skip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        skip.setPadding(0, UiKit.dp(this, 16), 0, UiKit.dp(this, 16));
        skip.setClickable(true);
        skip.setFocusable(true);
        skip.setOnClickListener(v -> {
            showingOwnKey = !showingOwnKey;
            renderLogin();
        });

        if (showingOwnKey) {
            EditText key = UiKit.input(this, card, "TypeSafe API key", "", false, true);
            key.setHint("Paste your key");
            TextView error = UiKit.description(this, card, "");
            UiKit.button(this, card, "Continue with my key", true, () -> {
                String value = key.getText().toString().trim();
                if (value.isEmpty()) {
                    error.setText("Enter your key to continue.");
                    return;
                }
                try {
                    new KeyVault(this).save(value);
                    display();
                } catch (Exception e) {
                    error.setText("Could not save your key.");
                }
            });
        }
        TextView note = UiKit.description(this, root, "Your messages never pass through TextSentry servers.");
        note.setGravity(Gravity.CENTER);
    }

    private void renderBattery() {
        LinearLayout root = stage("Step 2 of 4 · Background access",
                "Keep TextSentry running",
                "In Android settings, choose Battery → Unrestricted.", 2);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.chip(this, card, SetupPermissions.unrestricted(this)
                ? "✓ Battery optimization exemption detected" : "○ Check battery settings");
        UiKit.button(this, card, "Open app battery settings", true, () ->
                SetupPermissions.openBatterySettings(this));
        UiKit.description(this, card, "Return here when finished.");
        UiKit.button(this, root, "Continue", false, () -> {
            SetupFlow.advance(this, SetupFlow.PERMISSIONS);
            display();
        });
        TextView note = UiKit.description(this, root,
                "Some phones use different battery menus. This is recommended, not required.");
        note.setTextSize(12);
    }

    private void renderPermissions() {
        LinearLayout root = stage("Step 3 of 4 · Permissions",
                "Allow SMS access",
                "Needed to check incoming texts. Replies are optional.", 3);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.chip(this, card, SetupPermissions.hasIncoming(this)
                ? "✓ Incoming SMS allowed" : "○ Incoming SMS not allowed");
        if (!SetupPermissions.hasIncoming(this)) {
            UiKit.button(this, card, "Allow incoming SMS", true,
                    () -> SetupPermissions.requestIncoming(this, INCOMING_SMS_REQUEST));
        }
        UiKit.chip(this, card, SetupPermissions.hasOutgoing(this)
                ? "✓ Optional replies allowed" : "○ Optional replies off");
        if (!SetupPermissions.hasOutgoing(this)) {
            UiKit.button(this, card, "Allow SMS replies (optional)", false,
                    () -> SetupPermissions.requestOutgoing(this, OUTGOING_SMS_REQUEST));
        }
        UiKit.button(this, card, "Android permission settings", false, () ->
                SetupPermissions.openAppSettings(this));

        UiKit.button(this, root, "Continue", true, () -> {
            SetupFlow.advance(this, SetupFlow.ACTIVATE);
            display();
        });

        TextView help = UiKit.description(this, root,
                "Can't grant SMS access? See permission help.");
        help.setTextSize(12);
        help.setTextColor(UiKit.INK);
        help.setPaintFlags(help.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        help.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("SMS permission help")
                .setMessage(SetupPermissions.help())
                .setPositiveButton("Open app settings", (d, w) ->
                        SetupPermissions.openAppSettings(this))
                .setNegativeButton("Close", null).show());
    }

    private void renderActivation() {
        LinearLayout root = stage("Step 4 of 4 · Ready",
                "You're ready",
                "Start in preview mode. You can enable automatic replies later.", 4);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.chip(this, card, SetupPermissions.hasIncoming(this)
                ? "✓ Incoming SMS ready" : "○ Incoming SMS permission needed");

        if (SetupPermissions.hasIncoming(this)) {
            UiKit.button(this, card, "Start monitoring", true, () -> {
                if (!SetupGate.hasAccess(this) || !SetupPermissions.hasIncoming(this)) {
                    display();
                    return;
                }
                new Settings(this).setAutomation(true, false);
                SetupFlow.complete(this);
                goHome();
            });
        } else {
            UiKit.button(this, card, "Allow incoming SMS", true,
                    () -> SetupPermissions.requestIncoming(this, INCOMING_SMS_REQUEST));
            UiKit.button(this, card, "Open permission settings", false, () ->
                    SetupPermissions.openAppSettings(this));
            UiKit.button(this, card, "Use app without monitoring", false, () -> {
                new Settings(this).pause();
                SetupFlow.complete(this);
                goHome();
            });
        }
        UiKit.description(this, root, "Preview logs suggestions; it never sends SMS.");
    }

    private void renderManualStart() {
        LinearLayout root = stage("Step 2 of 2 · Ready", "You're ready",
                "Review messages you paste or share. Each analysis needs your approval.", 2);
        LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.button(this, card, "Start reviewing", true, () -> {
            SetupFlow.complete(this);
            goHome();
        });
        UiKit.description(this, card, "This edition doesn't monitor or send SMS.");
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions,
                                                     int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == INCOMING_SMS_REQUEST || code == OUTGOING_SMS_REQUEST) display();
    }
}
