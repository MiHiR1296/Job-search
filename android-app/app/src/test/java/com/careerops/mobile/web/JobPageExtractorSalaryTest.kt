package com.careerops.mobile.web

import org.junit.Assert.assertTrue
import org.junit.Test

class JobPageExtractorSalaryTest {

    @Test
    fun detects_inr_range() {
        val t = "CTC 18-22 LPA plus benefits"
        val s = JobPageExtractor.detectSalaryExpanded(t)
        assertTrue(s.isNotBlank())
    }

    @Test
    fun detects_usd_monthly() {
        val t = "Estimated pay: $5,200/mo based on similar roles"
        val s = JobPageExtractor.detectSalaryExpanded(t)
        assertTrue(s.contains("$", ignoreCase = true) && s.contains("mo", ignoreCase = true))
    }

    @Test
    fun detects_per_month_phrase() {
        val t = "Salary 180000 per month INR"
        val s = JobPageExtractor.detectSalaryExpanded(t)
        assertTrue(s.isNotBlank())
    }
}
