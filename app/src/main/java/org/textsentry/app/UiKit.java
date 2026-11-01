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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Consistent native surfaces; no WebView, framework or remote UI assets. */
public final class UiKit {
    private UiKit() {}
    public static final int NIGHT = Color.rgb(15, 29, 31);
    public static final int INK = Color.rgb(27, 47, 47);
    public static final int MUTED = Color.rgb(101, 121, 114);
    public static final int GREEN = Color.rgb(200, 248, 113);
    public static final int BG = Color.rgb(246, 248, 244);

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

    public static LinearLayout screen(Activity a, String subtitle, boolean back) {
        a.getWindow().setStatusBarColor(NIGHT);
        a.getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout body = vertical(a);
        scroll.addView(body);
        a.setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                scroll.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
            scroll.requestApplyInsets();
        }
        LinearLayout header = vertical(a);
        header.setBackgroundColor(NIGHT);
        header.setPadding(dp(a, 24), dp(a, 28), dp(a, 24), dp(a, 31));
        body.addView(header);
        if (back) {
            Button backButton = new Button(a);
            backButton.setText("←  Back");
            backButton.setAllCaps(false);
            backButton.setTextColor(GREEN);
            backButton.setBackgroundColor(Color.TRANSPARENT);
            header.addView(backButton);
            backButton.setOnClickListener(v -> a.finish());
        }
        TextView title = text(a, "TextSentry", 31, Color.WHITE, true);
        title.setLetterSpacing(-0.06f);
        header.addView(title);
        TextView sub = text(a, subtitle, 14, 0xFFB9C9C2, false);
        sub.setPadding(0, dp(a, 8), 0, 0);
        header.addView(sub);
        LinearLayout content = vertical(a);
        content.setPadding(dp(a, 18), dp(a, 22), dp(a, 18), dp(a, 30));
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
        item.setBackground(shape(background, dp(a, 19), background == Color.WHITE ? 0xFFE3E9E2 : 0));
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
        edit.setBackground(shape(0xFFF7F9F5, dp(a, 9), 0xFFDCE7DD));
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
        b.setBackground(shape(primary ? GREEN : 0xFFEAF0E9, dp(a, 11), 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 52));
        lp.topMargin = dp(a, 14);
        parent.addView(b, lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    public static TextView chip(Activity a, LinearLayout parent, String value) {
        TextView t = text(a, value, 12, 0xFF2F674A, true);
        t.setBackground(shape(0xFFE2F5D9, dp(a, 20), 0));
        t.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(a, 12);
        parent.addView(t, lp);
        return t;
    }
}
