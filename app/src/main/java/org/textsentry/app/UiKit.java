package org.textsentry.app;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Consistent native surfaces; no WebView, framework or remote UI assets. */
public final class UiKit {
    private UiKit() {}
    // Keep in sync with website :root tokens in src/assets/site.css.
    public static final int NIGHT = Color.rgb(5, 14, 32);
    public static final int INK = Color.rgb(11, 23, 48);
    public static final int MUTED = Color.rgb(93, 111, 136);
    public static final int ACCENT = Color.rgb(38, 215, 238);
    public static final int BG = Color.rgb(243, 247, 255);
    public static final int HIGHLIGHT = Color.rgb(231, 240, 255);

    public static int dp(Activity a, int n) {
        return Math.round(n * a.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable shape(int color, int radius, int border) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        if (border != 0) bg.setStroke(1, border);
        return bg;
    }

    public static LinearLayout vertical(Activity a) {
        LinearLayout line = new LinearLayout(a);
        line.setOrientation(LinearLayout.VERTICAL);
        return line;
    }

    private static void styleSystemBars(Activity a) {
        Window window = a.getWindow();
        // The theme provides these colors even before onCreate draws a single frame.
        // Android 15+ enforces edge-to-edge, so a permanent navy overlay is drawn below.
        if (Build.VERSION.SDK_INT < 35) {
            window.setStatusBarColor(NIGHT);
            window.setNavigationBarColor(BG);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility((decor.getSystemUiVisibility()
                    & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    public static LinearLayout screen(Activity a, String subtitle, boolean back) {
        styleSystemBars(a);
        FrameLayout frame = new FrameLayout(a);
        frame.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout body = vertical(a);
        scroll.addView(body);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        // This fixed scrim remains dark when scrolling content behind the status bar.
        // Window#setStatusBarColor alone does not work on Android 15+ with targetSdk 36.
        View statusScrim = new View(a);
        statusScrim.setBackgroundColor(NIGHT);
        frame.addView(statusScrim, new FrameLayout.LayoutParams(-1, 0, Gravity.TOP));
        a.setContentView(frame);
        if (Build.VERSION.SDK_INT >= 30) {
            // PhoneWindow has no decor view before setContentView; obtaining its
            // controller in styleSystemBars would crash when first opening the app.
            WindowInsetsController controller = a.getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
            frame.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                scroll.setPadding(0, bars.top, 0, bars.bottom);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) statusScrim.getLayoutParams();
                if (params.height != bars.top) {
                    params.height = bars.top;
                    statusScrim.setLayoutParams(params);
                }
                return insets;
            });
            frame.requestApplyInsets();
        }
        LinearLayout header = vertical(a);
        header.setBackgroundColor(NIGHT);
        header.setPadding(dp(a, 22), dp(a, 14), dp(a, 22), dp(a, 18));
        body.addView(header);
        LinearLayout brand = new LinearLayout(a);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand);
        if (back) {
            TextView backButton = text(a, "‹", 32, ACCENT, true);
            backButton.setGravity(Gravity.CENTER_VERTICAL);
            backButton.setContentDescription("Back");
            LinearLayout.LayoutParams backParams =
                    new LinearLayout.LayoutParams(dp(a, 40), dp(a, 46));
            brand.addView(backButton, backParams);
            backButton.setOnClickListener(v -> a.finish());
        }
        android.widget.ImageView icon = new android.widget.ImageView(a);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setContentDescription("TextSentry icon");
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(a, 40), dp(a, 40));
        logoParams.rightMargin = dp(a, 10);
        brand.addView(icon, logoParams);
        TextView wordmark = text(a, "TextSentry", 25, Color.WHITE, true);
        wordmark.setLetterSpacing(-0.055f);
        brand.addView(wordmark);
        TextView dot = text(a, ".", 25, ACCENT, true);
        brand.addView(dot);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = text(a, subtitle, 13, 0xFFD0DFFF, false);
            sub.setPadding(0, dp(a, 10), 0, 0);
            header.addView(sub);
        }
        LinearLayout content = vertical(a);
        content.setPadding(dp(a, 20), dp(a, 26), dp(a, 20), dp(a, 28));
        body.addView(content);
        return content;
    }

    public static TextView text(Activity a, String value, float size, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(dp(a, 2), 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static LinearLayout card(Activity a, LinearLayout parent, int background) {
        LinearLayout item = vertical(a);
        item.setPadding(dp(a, 20), dp(a, 21), dp(a, 20), dp(a, 21));
        item.setBackground(shape(background, dp(a, 19), background == Color.WHITE ? 0xFFDEE7F7 : 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(a, 15);
        parent.addView(item, lp);
        return item;
    }

    public static TextView heading(Activity a, LinearLayout parent, String title) {
        TextView t = text(a, title, 19, INK, true);
        parent.addView(t);
        return t;
    }

    public static TextView description(Activity a, LinearLayout parent, String value) {
        TextView t = text(a, value, 13, MUTED, false);
        t.setPadding(0, dp(a, 7), 0, dp(a, 6));
        parent.addView(t);
        return t;
    }

    public static EditText input(Activity a, LinearLayout parent, String label,
                                 String value, boolean multi, boolean secret) {
        TextView l = text(a, label, 13, INK, true);
        l.setPadding(0, dp(a, 16), 0, dp(a, 7));
        parent.addView(l);
        EditText edit = new EditText(a);
        edit.setTextSize(15);
        edit.setTextColor(INK);
        edit.setSingleLine(!multi);
        edit.setPadding(dp(a, 12), dp(a, 11), dp(a, 12), dp(a, 11));
        edit.setBackground(shape(Color.WHITE, dp(a, 9), 0xFFDEE7F7));
        if (multi) { edit.setMinLines(4); edit.setGravity(Gravity.TOP); }
        if (secret) edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setText(value);
        parent.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        return edit;
    }

    public static Button button(Activity a, LinearLayout parent, String title,
                                boolean primary, Runnable action) {
        Button b = new Button(a);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? NIGHT : INK);
        b.setBackground(shape(primary ? ACCENT : HIGHLIGHT, dp(a, 11), 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 52));
        lp.topMargin = dp(a, 14);
        parent.addView(b, lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    /** Google-approved light treatment: standard four-color G, bordered white surface. */
    public static LinearLayout googleSignInButton(Activity a, LinearLayout parent,
                                                  Runnable action) {
        LinearLayout surface = new LinearLayout(a);
        surface.setOrientation(LinearLayout.HORIZONTAL);
        surface.setGravity(Gravity.CENTER);
        surface.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        surface.setBackground(shape(Color.WHITE, dp(a, 14), 0xFF747775));
        surface.setClickable(true);
        surface.setFocusable(true);
        surface.setContentDescription("Sign in with Google");
        android.widget.ImageView google = new android.widget.ImageView(a);
        google.setImageResource(R.drawable.ic_google_g);
        surface.addView(google, new LinearLayout.LayoutParams(dp(a, 24), dp(a, 24)));
        TextView label = text(a, "Sign in with Google", 15, 0xFF1F1F1F, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.leftMargin = dp(a, 10);
        surface.addView(label, labelParams);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(a, 56));
        params.topMargin = dp(a, 20);
        parent.addView(surface, params);
        surface.setOnClickListener(v -> action.run());
        return surface;
    }

    public static TextView chip(Activity a, LinearLayout parent, String value) {
        TextView t = text(a, value, 12, 0xFF145180, true);
        t.setBackground(shape(0xFFCCF8FF, dp(a, 20), 0));
        t.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(a, 12);
        parent.addView(t, lp);
        return t;
    }
}
