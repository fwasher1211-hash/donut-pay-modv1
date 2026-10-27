package com.you.donutpay;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to chat and tries to detect messages where a player paid you.
 * DonutSMP's payment message format may be different — if nothing gets
 * detected, open ChatPattern.json (built into one string below when parsing)
 * and adjust the PATTERN below to match what /pay prints in chat.
 *
 * Default example patterns handled:
 *   PlayerName paid you $1,000
 *   You received $1,000 from PlayerName
 *   [TIP] PlayerName tipped you $1,000
 */
public class ChatListener {

    // amount: number with optional commas and optional $ before it
    private static final Pattern PAID_YOU = Pattern.compile(
            "^(?:\\[.*?]\\s*)?([A-Za-z0-9_]+)\\s+(?:paid|tipped|gave)\\s+you\\s+\\$?([\\d,\\.]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RECEIVED_FROM = Pattern.compile(
            "(?:received|got)\\s+\\$?([\\d,\\.]+)\\s+from\\s+([A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE
    );

    public static void register() {
        // fabric's chat receive event: message, only visible in overlay?
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register(
                (message, overlay) -> handleMessage(message.getString())
        );
    }

    private static void handleMessage(String text) {
        if (text == null || text.isEmpty()) return;
        String t = text.trim();

        Matcher m = PAID_YOU.matcher(t);
        if (m.find()) {
            String name = m.group(1);
            double amount = parseAmount(m.group(2));
            if (amount > 0) record(name, amount);
            return;
        }

        m = RECEIVED_FROM.matcher(t);
        if (m.find()) {
            String name = m.group(2);
            double amount = parseAmount(m.group(1));
            if (amount > 0) record(name, amount);
        }
    }

    private static void record(String name, double amount) {
        DonutPayClient.STORE.addPayment(new PaymentStore.Payment(
                UUID.randomUUID(), name, "tip", System.currentTimeMillis(), amount
        ));
    }

    private static double parseAmount(String s) {
        return Double.parseDouble(s.replace(",", "").replace("$", ""));
    }
}
