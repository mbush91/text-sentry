package org.textsentry.app;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReplyLedgerTest {
    private Context context;
    private ReplyLedger ledger;
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Before public void init() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteSharedPreferences("textsentry_reply_ledger");
        ledger = new ReplyLedger(context);
    }

    @Test public void deduplicatesNonAdjacentMessagesForOneDay() {
        long now = 100000000L;
        assertTrue(ledger.reserveMessage("+15551234567", "message A", now));
        assertTrue(ledger.reserveMessage("+15551234567", "message B", now + 1));
        assertFalse(ledger.reserveMessage("+15551234567", "message A", now + 2));
        assertTrue(ledger.reserveMessage("+15551234567", "message A", now + DAY + 1));
    }

    @Test public void throttleAppliesToSenderNotMessage() {
        long now = 100000000L;
        assertTrue(ledger.reserveSend("+15551234567", now));
        assertFalse(ledger.reserveSend("+15551234567", now + DAY / 2));
        assertTrue(ledger.reserveSend("+15557654321", now + 1));
        assertTrue(ledger.reserveSend("+15551234567", now + DAY + 1));
    }

    @Test public void reservationsPersistAcrossInstances() {
        assertTrue(ledger.reserveMessage("+15551234567", "something", 100000000L));
        assertFalse(new ReplyLedger(context).reserveMessage("+15551234567", "something", 100000001L));
    }

    @Test public void noPlaintextSenderOrBodyInLedger() {
        ledger.reserveMessage("+15551234567", "Sensitive body", 100000000L);
        for (String key : context.getSharedPreferences("textsentry_reply_ledger", Context.MODE_PRIVATE)
                .getAll().keySet()) {
            assertFalse(key.contains("Sensitive"));
            assertFalse(key.contains("15551234567"));
        }
    }
}
