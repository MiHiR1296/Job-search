package com.careerops.mobile.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobPostingJsonLdParserTest {

    @Test
    fun parses_single_job_posting() {
        val json = """
            {
              "@context": "https://schema.org",
              "@type": "JobPosting",
              "title": "Senior Android Engineer",
              "hiringOrganization": { "@type": "Organization", "name": "Acme Corp" },
              "jobLocation": { "@type": "Place", "address": { "addressLocality": "Bengaluru", "addressCountry": "IN" } },
              "baseSalary": {
                "@type": "MonetaryAmount",
                "currency": "INR",
                "value": { "@type": "QuantitativeValue", "minValue": 30, "maxValue": 45, "unitText": "YEAR" }
              },
              "description": "<p>Build Kotlin apps.</p>"
            }
        """.trimIndent()
        val d = JobPostingJsonLdParser.parseFromConcatenatedBlocks(json)
        assertEquals("Acme Corp", d?.company)
        assertTrue(d?.role?.contains("Android", ignoreCase = true) == true)
        assertTrue(d?.salaryRaw?.contains("INR", ignoreCase = true) == true)
    }
}
