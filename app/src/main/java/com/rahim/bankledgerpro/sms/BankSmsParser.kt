package com.rahim.bankledgerpro.sms

data class ParsedTransaction(
    val amount: Long,
    val type: String,
    val bank: String,
    val balance: Long?,
    val raw: String
)

object BankSmsParser {
    private val banks = linkedMapOf(
        "توسعه تعاون" to listOf("توسعه تعاون", "TOSETAAVON", "ToseTaavon"),
        "ملی" to listOf("بانک ملی", "MELLI", "Melli"),
        "ملت" to listOf("بانک ملت", "MELLAT", "Mellat"),
        "صادرات" to listOf("بانک صادرات", "SADERAT", "Saderat"),
        "تجارت" to listOf("بانک تجارت", "TEJARAT", "Tejarat"),
        "رفاه" to listOf("بانک رفاه", "REFAH", "Refah"),
        "کشاورزی" to listOf("بانک کشاورزی", "KESHAVARZI", "Keshavarzi"),
        "مسکن" to listOf("بانک مسکن", "MASKAN", "Maskan"),
        "پارسیان" to listOf("بانک پارسیان", "PARSIAN", "Parsian"),
        "پاسارگاد" to listOf("بانک پاسارگاد", "PASARGAD", "Pasargad"),
        "سامان" to listOf("بانک سامان", "SAMAN", "Saman"),
        "اقتصاد نوین" to listOf("اقتصاد نوین", "ENBANK", "Novin"),
        "دی" to listOf("بانک دی", "DAYBANK", "Day"),
        "آینده" to listOf("بانک آینده", "AYANDEH", "Ayandeh"),
        "گردشگری" to listOf("بانک گردشگری", "TOURISM", "Gardeshgari"),
        "شهر" to listOf("بانک شهر", "SHAHR", "Shahr"),
        "سپه" to listOf("بانک سپه", "SEPAH", "Sepah"),
        "قوامین" to listOf("قوامین", "GHAVAMIN"),
        "انصار" to listOf("انصار", "ANSAR"),
        "موسسه ملل" to listOf("ملل", "MELAL"),
        "بلوبانک" to listOf("بلوبانک", "BLU"),
        "بانکینو" to listOf("بانکینو", "BANKINO"),
        "مهر ایران" to listOf("مهر ایران", "MEHR IRAN"),
        "رسالت" to listOf("رسالت", "RESALAT")
    )

    fun parse(sender: String?, body: String): ParsedTransaction? {
        val text = normalize(body)
        val bank = detectBank(sender, text) ?: "نامشخص"

        val type = when {
            listOf("واریز", "واریزی", "بستانکار", "دریافت", "بستان").any(text::contains) -> "IN"
            listOf("برداشت", "برداشتی", "خرید", "بدهکار", "پرداخت", "خروج").any(text::contains) -> "OUT"
            else -> return null
        }

        val amounts = Regex("""(?<!\d)(\d{1,3}(?:[,\s]\d{3})+|\d{4,})(?!\d)""")
            .findAll(text)
            .map { it.groupValues[1].replace(",", "").replace(" ", "") }
            .mapNotNull { it.toLongOrNull() }
            .toList()

        if (amounts.isEmpty()) return null

        val balance = findBalance(text, amounts)
        val amount = chooseAmount(amounts, balance)
        return ParsedTransaction(amount, type, bank, balance, body)
    }

    private fun chooseAmount(values: List<Long>, balance: Long?): Long {
        if (balance != null) {
            values.firstOrNull { it != balance }?.let { return it }
        }
        return values.first()
    }

    private fun findBalance(text: String, amounts: List<Long>): Long? {
        val balancePattern = Regex("""(?:موجودی|مانده|مانده حساب|bal|balance)\D{0,20}(\d{1,3}(?:[,\s]\d{3})+|\d{4,})""", RegexOption.IGNORE_CASE)
        val match = balancePattern.find(text)
        return match?.groupValues?.getOrNull(1)?.replace(",", "")?.replace(" ", "")?.toLongOrNull()
            ?: amounts.lastOrNull()?.takeIf { amounts.size > 1 }
    }

    private fun detectBank(sender: String?, text: String): String? {
        val haystack = "${sender.orEmpty()} $text"
        return banks.entries.firstOrNull { (_, keys) ->
            keys.any { haystack.contains(it, ignoreCase = true) }
        }?.key
    }

    fun knownBanks(): List<String> = banks.keys.toList()

    private fun normalize(value: String): String =
        value.replace("٬", ",")
            .replace("،", ",")
            .replace("﷼", "")
            .replace("ریال", "")
            .replace("تومان", "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
