package org.textsentry.app;

import android.app.Activity;
import android.app.AlertDialog;

/** Explicit, local-only responsibility disclosure for independently provided API keys. */
final class OwnKeyAgreement {
    static final String TITLE = "Using your own API key";
    static final String MESSAGE =
            "Your messages will be sent directly to TypeSafe under your own account. "
            + "TextSentry's managed zero-data-retention protection does not apply. "
            + "You are responsible for checking your provider's terms, data-retention "
            + "agreement and any usage charges before submitting messages.";

    private OwnKeyAgreement() {}

    /** Only the affirmative button can invoke the action that continues key setup. */
    static AlertDialog show(Activity activity, Runnable onAgree) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(TITLE)
                .setMessage(MESSAGE)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("I understand and agree", (unused, which) -> onAgree.run())
                .create();
        dialog.show();
        return dialog;
    }
}
