package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class MessageCategoriesTest {
    @Test public void defaultsOnlyMarketingIsSelected() {
        assertEquals(MessageCategories.MARKETING, MessageCategories.DEFAULT_MASK);
        String instructions = MessageCategories.compose(MessageCategories.DEFAULT_MASK, "");
        assertTrue(instructions.contains("marketing advertisements"));
        assertFalse(instructions.contains("nonpartisan get-out-the-vote"));
        assertFalse(instructions.contains("candidate or political party campaigns"));
    }

    @Test public void allSelectedCategoriesShareOneBooleanDecision() {
        String instructions = MessageCategories.compose(
                MessageCategories.VOTE | MessageCategories.POLITICAL | MessageCategories.MARKETING,
                "Ignore messages from my dentist.");
        assertTrue(instructions.contains("nonpartisan get-out-the-vote"));
        assertTrue(instructions.contains("candidate or political party campaigns"));
        assertTrue(instructions.contains("marketing advertisements"));
        assertTrue(instructions.contains("ANY ONE"));
        assertTrue(instructions.contains("Ignore messages from my dentist."));
        assertTrue(instructions.contains("explicit STOP"));
        assertTrue(instructions.contains("never override"));
    }

    @Test public void noSelectionsMeansNoProviderQuery() {
        assertEquals("", MessageCategories.compose(0, "Ignore everything"));
        assertEquals("None selected", MessageCategories.summary(0));
    }

    @Test public void selectedCategoryRulesAreNotAddedWhenUnchecked() {
        String onlyVote = MessageCategories.compose(MessageCategories.VOTE, "");
        assertTrue(onlyVote.contains("nonpartisan get-out-the-vote"));
        assertFalse(onlyVote.contains("candidate or political party campaigns"));
        assertFalse(onlyVote.contains("marketing advertisements"));
        assertFalse(onlyVote.contains("Ignore messages from my dentist."));
    }

    @Test public void rejectsUnknownMaskAndOversizedCombinedRules() {
        for (int invalid : new int[]{-1, 8, 15}) {
            try {
                MessageCategories.compose(invalid, "");
                fail("Unexpected mask must be rejected");
            } catch (IllegalArgumentException expected) {}
        }
        try {
            MessageCategories.compose(MessageCategories.VOTE, "x".repeat(3000));
            fail("Combined instructions cannot exceed TypeSafe limits");
        } catch (IllegalArgumentException expected) {}
    }
}
