package org.textsentry.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Fail closed: model/network/permission failures never trigger a send. */
public final class SmsWorker extends Worker {
    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        Settings settings = new Settings(context);
        String queuedGeneration = getInputData().getString("generation");
        if (!settings.monitoring() || !settings.generation().equals(queuedGeneration)) return Result.success();
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
            ReplyLedger ledger = new ReplyLedger(context);
            if (!ledger.reserveMessage(sender, message, System.currentTimeMillis())) {
                settings.addHistory("Ignored (duplicate SMS)");
                return Result.success();
            }

            AiDecision decision = new SystemOneClient().decide(key, SystemOneClient.MODEL,
                    settings.prompt(), message, "STOP", settings.confidence() / 100.0);
            if (!settings.monitoring() || !settings.generation().equals(queuedGeneration)) {
                return Result.success();
            }
            if (!decision.shouldReply) {
                settings.addHistory("AI: no reply");
            } else if (!settings.autoSend()) {
                settings.addHistory("Preview only: " + decision.reply);
            } else if (context.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                settings.addHistory("Blocked: send permission/consent missing");
            } else {
                if (!ledger.reserveSend(sender, System.currentTimeMillis())) {
                    settings.addHistory("Blocked: recipient daily reply limit");
                    return Result.success();
                }
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
