package com.careerops.mobile.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SalaryNormalizerTest {

    @Test
    fun explicitLpa() {
        assertEquals(18.0, SalaryNormalizer.approximateAnnualLpa("Compensation: 18 LPA"), 0.01)
    }

    @Test
    fun lpaRangeUsesUpperBound() {
        assertEquals(15.0, SalaryNormalizer.approximateAnnualLpa("12-15 LPA package"), 0.01)
    }

    @Test
    fun lakhsPerMonth() {
        assertEquals(18.0, SalaryNormalizer.approximateAnnualLpa("1.5 lakh per month"), 0.01)
    }

    @Test
    fun inrPerMonth() {
        val s = SalaryNormalizer.approximateAnnualLpa("INR 1,80,000 / month")
        assertEquals(21.6, s ?: 0.0, 0.05)
    }

    @Test
    fun usdPerMonthRoughInrLpa() {
        val s = SalaryNormalizer.approximateAnnualLpa("\$5000 / month")
        assertEquals(50.4, s ?: 0.0, 0.2)
    }

    @Test
    fun blankReturnsNull() {
        assertNull(SalaryNormalizer.approximateAnnualLpa(""))
    }
}
