package com.digitalseba.admin.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Fmt {
    private val dhaka: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    private fun nf(decimals: Int) = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }

    fun money(v: Double): String = "৳" + nf(2).format(v)

    /** Compact money for tight tiles: ৳12,340 or ৳1.2L */
    fun moneyCompact(v: Double): String = when {
        v >= 10_000_000 -> "৳" + nf(2).format(v / 10_000_000) + " Cr"
        v >= 100_000 -> "৳" + nf(2).format(v / 100_000) + " L"
        else -> "৳" + nf(0).format(v)
    }

    fun int(v: Double): String = nf(0).format(v)
    fun int(v: Int): String = nf(0).format(v.toLong())

    fun ago(ts: Long?): String {
        if (ts == null || ts <= 0) return "unknown"
        val diff = System.currentTimeMillis() / 1000 - ts
        return when {
            diff < 60 -> "just now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86400 -> "${diff / 3600}h ago"
            else -> "${diff / 86400}d ago"
        }
    }

    private fun fmt(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = dhaka }

    fun date(ts: Long?, fallback: String = "Never"): String =
        if (ts == null || ts <= 0) fallback else fmt("dd MMM, hh:mm a").format(Date(ts * 1000))

    fun dateLong(ts: Long?, fallback: String = "Never"): String =
        if (ts == null || ts <= 0) fallback else fmt("dd MMM yyyy, hh:mm a").format(Date(ts * 1000))

    fun clock(): String = fmt("dd MMM, hh:mm:ss a").format(Date())

    fun hour(): Int = fmt("H").format(Date()).toIntOrNull() ?: 12

    fun dayLabel(ymd: String): String = try {
        val p = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = dhaka }
        fmt("dd MMM").format(p.parse(ymd)!!)
    } catch (e: Exception) {
        ymd
    }

    fun weekday(ymd: String): String = try {
        val p = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = dhaka }
        fmt("EEE").format(p.parse(ymd)!!)
    } catch (e: Exception) {
        ymd.takeLast(2)
    }

    fun greeting(): String = when (hour()) {
        in 5..11 -> "শুভ সকাল"
        in 12..16 -> "শুভ দুপুর"
        in 17..19 -> "শুভ সন্ধ্যা"
        else -> "শুভ রাত্রি"
    }
}

object Intents {
    fun open(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "অ্যাপ খোলা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(context: Context, phone: String) {
        if (phone.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun whatsapp(context: Context, phone: String, message: String = "") {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("0")) digits = "88$digits"
        open(context, "https://wa.me/$digits" + if (message.isNotBlank()) "?text=" + Uri.encode(message) else "")
    }

    fun copy(context: Context, text: String, label: String = "Copied") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun share(context: Context, text: String) {
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
