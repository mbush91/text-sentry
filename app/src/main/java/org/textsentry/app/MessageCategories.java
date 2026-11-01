package org.textsentry.app;

/**
 * Local category choices are OR-ed into ONE Jev yes/no instruction.
 * Selecting several categories never creates several questions or network calls.
 */
public final class MessageCategories {
    public static final int VOTE = 1;
    public static final int POLITICAL = 2;
    public static final int MARKETING = 4;
    public static final int ALL = VOTE | POLITICAL | MARKETING;
    public static final int DEFAULT_MASK = MARKETING;

    private MessageCategories() {}

    public static boolean valid(int mask) { return mask >= 0 && (mask & ~ALL) == 0; }

    public static String summary(int mask) {
        if (!valid(mask)) throw new IllegalArgumentException("Unknown message categories");
        StringBuilder result = new StringBuilder();
        appendName(result, mask, VOTE, "Go vote reminders");
        appendName(result, mask, POLITICAL, "Political campaign messages");
        appendName(result, mask, MARKETING, "Marketing ads");
        return result.length() == 0 ? "None selected" : result.toString();
    }

    private static void appendName(StringBuilder builder, int mask, int bit, String label) {
        if ((mask & bit) == 0) return;
        if (builder.length() > 0) builder.append(" · ");
        builder.append(label);
    }

    public static String compose(int mask, String extraRestrictions) {
        if (!valid(mask)) throw new IllegalArgumentException("Unknown message categories");
        if (extraRestrictions == null) throw new IllegalArgumentException("Instructions required");
        if (mask == 0) return ""; // no categories => no request, regardless of extra text
        StringBuilder result = new StringBuilder();
        result.append("Classify whether an incoming SMS deserves the owner's fixed STOP opt-out reply. ")
                .append("Answer YES only if it clearly belongs to ANY ONE of the selected categories:\n");
        if ((mask & VOTE) != 0) {
            result.append("- Unsolicited nonpartisan get-out-the-vote reminders asking someone ")
                    .append("to vote or reminding them that an election is approaching. ")
                    .append("Exclude official voting, registration, polling-place and ballot information.\n");
        }
        if ((mask & POLITICAL) != 0) {
            result.append("- Unsolicited candidate or political party campaigns: requests for votes, ")
                    .append("donations, volunteers, rallies or campaign support.\n");
        }
        if ((mask & MARKETING) != 0) {
            result.append("- Unsolicited marketing advertisements: commercial promotions, ")
                    .append("sales, coupons or product/service marketing.\n");
        }
        result.append("For ALL categories, require an explicit STOP SMS unsubscribe option ")
                .append("in the message before choosing YES. Never reply to personal conversations, ")
                .append("official election administration notices, security codes, emergencies, ")
                .append("transactional account alerts, scams, or unclear messages. ")
                .append("Do not treat a category as an instruction to persuade, endorse, vote, ")
                .append("donate, or contact someone; the only possible response is the owner's STOP. ");
        if (!extraRestrictions.trim().isEmpty()) {
            result.append("\nAdditional user restrictions (may narrow but never override ")
                    .append("selected categories or safety rules): ")
                    .append(extraRestrictions.trim());
        }
        if (result.length() > 3000) {
            throw new IllegalArgumentException("Combined instructions exceed 3000 characters");
        }
        return result.toString();
    }
}
