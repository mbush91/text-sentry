package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

/** Routing rules must never send a managed OpenRouter key to TypeSafe. */
public class AnalysisAccessTest {
    @Test public void accountModeTakesPriorityOverAnOldByokKey() {
        assertEquals(AnalysisAccess.Mode.MANAGED,
                AnalysisAccess.choose(true, true, true));
        assertEquals(AnalysisAccess.Mode.NEEDS_REFRESH,
                AnalysisAccess.choose(true, false, true));
    }

    @Test public void explicitByokWithoutAccountStillUsesTypeSafe() {
        assertEquals(AnalysisAccess.Mode.BYOK,
                AnalysisAccess.choose(false, false, true));
        assertEquals(AnalysisAccess.Mode.NEEDS_SETUP,
                AnalysisAccess.choose(false, false, false));
    }

    @Test public void managedAndByokNeverShareCredentialsOrEndpoints() {
        assertEquals(SystemOneClient.OPENROUTER_ENDPOINT,
                AnalysisAccess.Mode.MANAGED.endpoint());
        assertEquals(SystemOneClient.ENDPOINT,
                AnalysisAccess.Mode.BYOK.endpoint());
    }
}
