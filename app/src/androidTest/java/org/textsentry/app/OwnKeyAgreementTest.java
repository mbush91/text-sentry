package org.textsentry.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;

/** A personal key must require explicit agreement before it is stored. */
@RunWith(AndroidJUnit4.class)
public final class OwnKeyAgreementTest {
    @Test public void explainsIndependentProviderResponsibilityAndRequiresAcceptance() {
        String text = OwnKeyAgreement.MESSAGE.toLowerCase(java.util.Locale.US);
        assertTrue(text.contains("typesafe"));
        assertTrue(text.contains("zero-data-retention"));
        assertTrue(text.contains("responsible"));
        assertTrue(text.contains("retention"));
        assertTrue(text.contains("charges"));
    }

    @Test public void cancelNeverInvokesSaveButExplicitAgreementDoes() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        var context = instrumentation.getTargetContext();
        new AuthSessionStore(context).clear();
        new KeyVault(context).clear();
        SetupFlow.reset(context);

        Intent intent = new Intent(context, SetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);
        AtomicBoolean accepted = new AtomicBoolean(false);
        try {
            instrumentation.runOnMainSync(() -> {
                AlertDialog dialog = OwnKeyAgreement.show(activity, () -> accepted.set(true));
                assertTrue(dialog.isShowing());
                assertNotNull(dialog.getButton(DialogInterface.BUTTON_POSITIVE));
                assertNotNull(dialog.getButton(DialogInterface.BUTTON_NEGATIVE));
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
            });
            assertFalse("Cancel must not approve using a personal key", accepted.get());

            instrumentation.runOnMainSync(() -> {
                AlertDialog dialog = OwnKeyAgreement.show(activity, () -> accepted.set(true));
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            });
            assertTrue("Only explicit agreement invokes the caller", accepted.get());
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            SetupFlow.reset(context);
        }
    }
}
