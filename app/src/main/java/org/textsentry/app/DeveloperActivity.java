package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Hidden, local-only developer tools. Neither exported nor visible until unlocked. */
public final class DeveloperActivity extends Activity {
    private Settings settings;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SetupGate.enforce(this)) return;
        settings = new Settings(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!SetupGate.enforce(this)) return;
    }

    private void render() {
        if (!settings.developerMode()) {
            finish();
            return;
        }
        LinearLayout root = UiKit.screen(this, "Developer tools", true);
        LinearLayout promptCard = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, promptCard, "Additional model prompting");
        UiKit.description(this, promptCard,
                "Append any optional instructions to your selected filter categories " +
                "in one Jev request. These may narrow results but cannot enable unchecked " +
                "categories or bypass local safety rules.");
        EditText extra = UiKit.input(this, promptCard, "Additional prompt",
                settings.prompt(), true, false);
        extra.setHint("e.g., Ignore texts about local school fundraisers");
        UiKit.description(this, promptCard,
                "Maximum 2,000 characters, subject to the 3,000-character combined limit. " +
                "Saving invalidates work queued with older instructions.");
        UiKit.button(this, promptCard, "Save additional prompt", true, () -> {
            try {
                settings.saveCategoryPolicy(extra.getText().toString(),
                        settings.confidence(), settings.categoryMask());
                Toast.makeText(this, "Developer prompt saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Prompt too long or could not save", Toast.LENGTH_LONG).show();
            }
        });
        UiKit.button(this, promptCard, "Clear additional prompt", false, () -> {
            try {
                settings.saveCategoryPolicy("", settings.confidence(), settings.categoryMask());
                extra.setText("");
                Toast.makeText(this, "Additional prompt cleared", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not clear prompt", Toast.LENGTH_LONG).show();
            }
        });

        LinearLayout diagnostic = UiKit.card(this, root, UiKit.HIGHLIGHT);
        UiKit.heading(this, diagnostic, "Raw Jev input / output");
        UiKit.description(this, diagnostic,
                "The five most recent evaluations appear below. Full JSON request bodies " +
                "(including message text and instructions) and raw TypeSafe responses " +
                "are encrypted on this device. API keys and HTTP authorization headers " +
                "are never recorded. An empty response indicates a transport failure.");
        UiKit.button(this, diagnostic, "Refresh traces", false, this::render);
        UiKit.button(this, diagnostic, "Clear model traces", false, () ->
                new AlertDialog.Builder(this)
                        .setTitle("Clear raw model traces?")
                        .setMessage("Delete the locally encrypted model input and output log?")
                        .setPositiveButton("Clear", (d, which) -> {
                            try {
                                new HistoryStore(this).clearTraces();
                                render();
                            } catch (Exception ex) {
                                Toast.makeText(this, "Could not clear traces",
                                        Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton("Cancel", null).show());

        List<HistoryStore.TraceEntry> entries;
        try {
            entries = new HistoryStore(this).traces();
        } catch (Exception error) {
            UiKit.description(this, diagnostic,
                    "Could not decrypt traces. Existing records were not deleted.");
            return;
        }
        if (entries.isEmpty()) {
            UiKit.description(this, diagnostic,
                    "No model traces yet. Analyze a new SMS or run a manual sample.");
        }
        for (HistoryStore.TraceEntry item : entries) {
            LinearLayout card = UiKit.card(this, root, 0xFFFFFFFF);
            UiKit.heading(this, card, "Jev · " + item.source);
            UiKit.description(this, card, DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(item.timestamp)));
            UiKit.description(this, card, "Full request JSON");
            TextView request = UiKit.text(this, item.request, 12, UiKit.INK, false);
            request.setTextIsSelectable(true);
            card.addView(request);
            UiKit.description(this, card, "Raw response JSON");
            TextView response = UiKit.text(this,
                    item.response.isEmpty() ? "(No provider response)" : item.response,
                    12, UiKit.INK, false);
            response.setTextIsSelectable(true);
            card.addView(response);
        }

        LinearLayout control = UiKit.card(this, root, 0xFFFFFFFF);
        UiKit.heading(this, control, "Developer mode control");
        UiKit.description(this, control,
                "Disabling stops future traces and hides these tools. Saved prompting " +
                "stays active. Existing traces remain encrypted until cleared here, " +
                "with message history, or by deleting app data.");
        UiKit.button(this, control, "Disable developer mode", false, () -> {
            settings.setDeveloperMode(false);
            finish();
        });
    }
}
