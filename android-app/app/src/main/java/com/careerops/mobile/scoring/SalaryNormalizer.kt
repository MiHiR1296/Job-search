package com.careerops.mobile.scoring

/**
 * Best-effort annual CTC in Indian LPA from mixed salary strings (LPA, lakhs/month, INR/month, USD/month).
 * Used only for heuristic scoring vs profile CTC fields, not legal/comp commitments.
 */
object SalaryNormalizer {
    /** Rough USD→INR for monthly USD bands on global JDs (tunable). */
    private const val USD_INR = 84.0

    fun approximateAnnualLpa(
        salaryText: String,
        payPeriodHint: String = "unknown",
        extraContext: String = ""
    ): Double? {
        val blob = (salaryText + "\n" + extraContext).lowercase()
        if (blob.isBlank()) return null

        explicitLpaRange(blob)?.let { return it }
        explicitLpaSingle(blob)?.let { return it }
        lakhsPerMonth(blob)?.let { return it }
        informalLPerMo(blob)?.let { return it }
        inrPerMonth(blob)?.let { return it }
        usdPerMonth(blob)?.let { return it }

        if (payPeriodHint.equals("yearly", ignoreCase = true) ||
            payPeriodHint.equals("annual", ignoreCase = true)
        ) {
            // Schema sometimes gives baseSalary without "LPA" wording
            yearlyInrFromDigits(blob)?.let { return it }
        }
        return null
    }

    private fun explicitLpaRange(blob: String): Double? {
        val m = Regex("""(?i)([0-9]{1,2}(?:\.[0-9]+)?)\s*(?:-|to|–)\s*([0-9]{1,2}(?:\.[0-9]+)?)\s*lpa""").find(blob)
            ?: return null
        val a = m.groupValues[1].toDoubleOrNull() ?: return null
        val b = m.groupValues[2].toDoubleOrNull() ?: return a
        return maxOf(a, b)
    }

    private fun explicitLpaSingle(blob: String): Double? =
        Regex("""(?i)\b([0-9]{1,2}(?:\.[0-9]+)?)\s*lpa\b""").find(blob)?.groupValues?.get(1)?.toDoubleOrNull()

    /** Indian English: "1.8 lakh per month" → LPA = lakhs/month × 12. */
    private fun lakhsPerMonth(blob: String): Double? {
        val m = Regex("""(?i)([0-9]+(?:\.[0-9]+)?)\s*(?:lac|lakh|lakhs)\s*(?:per\s*)?(?:month|mo\b)""").find(blob)
            ?: return null
        val lakhsPerMo = m.groupValues[1].toDoubleOrNull() ?: return null
        return lakhsPerMo * 12.0
    }

    private fun informalLPerMo(blob: String): Double? {
        val m = Regex("""(?i)([0-9]+(?:\.[0-9]+)?)\s*l\s*/\s*mo\b""").find(blob) ?: return null
        val lakhsPerMo = m.groupValues[1].toDoubleOrNull() ?: return null
        return lakhsPerMo * 12.0
    }

    private fun inrPerMonth(blob: String): Double? {
        val m = Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9][0-9,]*(?:\.[0-9]+)?)\s*(?:/|\s*per\s*)?\s*(month|mo\b)""").find(blob)
            ?: return null
        val monthlyInr = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (monthlyInr < 1_000) return null
        return monthlyInr * 12.0 / 100_000.0
    }

    private fun usdPerMonth(blob: String): Double? {
        val m = Regex("""(?i)\$\s*([0-9][0-9,]*(?:\.[0-9]+)?)\s*(?:/|\s*per\s*)?\s*(month|mo\b)""").find(blob)
            ?: Regex("""(?i)\b([0-9][0-9,]*(?:\.[0-9]+)?)\s*usd\s*(?:/|\s*per\s*)?\s*(month|mo\b)""").find(blob)
            ?: return null
        val usdMo = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (usdMo <= 0) return null
        val annualInr = usdMo * 12.0 * USD_INR
        return annualInr / 100_000.0
    }

    /** Very loose: large INR-looking annual number in text (lakhs or full INR). */
    private fun yearlyInrFromDigits(blob: String): Double? {
        val m = Regex("""(?i)(?:₹|rs\.?|inr)\s*([0-9][0-9,]*(?:\.[0-9]+)?)\s*(?:/|\s*per\s*)?\s*(?:year|yr|annum|p\.a\.)""").find(blob)
            ?: return null
        val n = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return when {
            n >= 100_000 -> n / 100_000.0
            n >= 10 -> n
            else -> null
        }
    }
}
