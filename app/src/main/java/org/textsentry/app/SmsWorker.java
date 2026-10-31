package org.textsentry.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Fail closed: model/network/permission failures never trigger a send. */
public final class SmsWorker extends Worker {
    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        Settings settings = new Settings(context);
        if (!settings.monitoring()) return Result.success();
        String sender = getInputData().getString("sender");
        String message = getInputData().getString("sms");
        if (!MessagePolicy.eligible(sender, message)) {
            settings.addHistory("Ignored (local safety rule)");
            return Result.success();
        }
        try {
            String key = new KeyVault(context).read();
            if (key.isEmpty()) {
                settings.addHistory("Skipped (API key missing)");
                return Result.success();
            }
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((sender + "\u0000" + message).getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : digest) hash.append(String.format("%02x", b & 0xff));
            if (!settings.reserve(hash.toString(), System.currentTimeMillis())) return Result.success();

            AiDecision decision = new DecisionClient().decide(settings.endpoint(), settings.model(),
                    key, settings.prompt(), sender, message);
            if (!decision.shouldReply) {
                settings.addHistory("AI: no reply");
            } else if (!settings.autoSend()) {
                settings.addHistory("Preview only: " + decision.reply);
            } else if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED || !settings.monitoring()) {
                settings.addHistory("Blocked: send permission/consent missing");
            } else {
                SmsManager manager = context.getSystemService(SmsManager.class);
                if (manager == null) throw new IllegalStateException("SMS service unavailable");
                manager.sendTextMessage(sender, null, decision.reply, null, null);
                settings.addHistory("Submitted SMS reply: " + decision.reply);
            }
        } catch (Exception e) {
            settings.addHistory("No reply: provider or Android error");
        }
        return Result.success();
    }
}
