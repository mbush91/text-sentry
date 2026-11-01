package org.textsentry.app;

import android.content.Context;

/** Developer-only on-device trace capture; no analytics or backend upload. */
public final class DeveloperMode {
    private DeveloperMode() {}

    public static SystemOneClient.TraceSink traceSink(Context context, String source) {
        Context app = context.getApplicationContext();
        if (!new Settings(app).developerMode()) return null;
        return (request, response) -> {
            // A developer may disable tracing while an evaluation is in flight.
            if (new Settings(app).developerMode()) {
                new HistoryStore(app).addTrace(source, request, response);
            }
        };
    }
}
