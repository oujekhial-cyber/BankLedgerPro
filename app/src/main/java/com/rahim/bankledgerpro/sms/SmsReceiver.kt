package com.rahim.bankledgerpro.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val body = sms.messageBody ?: return@forEach
            val sender = sms.originatingAddress
            val parsed = BankSmsParser.parse(sender, body) ?: return@forEach

            val pending = context.getSharedPreferences("pending_sms", Context.MODE_PRIVATE)
            pending.edit()
                .putString("body", body)
                .putString("sender", sender)
                .putLong("amount", parsed.amount)
                .putString("type", parsed.type)
                .putString("bank", parsed.bank)
                .putLong("balance", parsed.balance ?: -1L)
                .apply()

            showNotification(context, parsed)
        }
    }

    private fun showNotification(context: Context, tx: ParsedTransaction) {
        val channelId = "transactions"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "تراکنش‌های بانکی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان پیامک‌های واریز و برداشت"
                }
            )
        }

        val launch = Intent(context, Class.forName("com.rahim.bankledgerpro.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_pending", true)
        }
        val pi = android.app.PendingIntent.getActivity(
            context, 5001, launch,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = if (tx.type == "IN") "واریز جدید" else "برداشت جدید"
        val text = "${format(tx.amount)} ${tx.bank} — برای چه کاری بود؟"
        NotificationManagerCompat.from(context).notify(
            5001,
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }

    private fun format(v: Long): String = "%,d".format(v)
}
