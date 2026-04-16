package com.careerops.mobile.web

import com.careerops.mobile.data.StructuredJobDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobPageExtractorMetadataTest {

    @Test
    fun parseInjectedHeader_extractsEmployerTitleSalary() {
        val page = """
            ---STRUCTURED_JOB_METADATA---
            Employer: Sarthak Advertising Private Limited
            Title: Senior 3D Visualizer
            Location: UP, IN
            Compensation (schema): INR 40000 - 100000 (unknown)
            ---END_STRUCTURED_JOB_METADATA---

            Apply now for great role.
        """.trimIndent()
        val d = JobPageExtractor.parseInjectedStructuredMetadataHeader(page)!!
        assertEquals("Sarthak Advertising Private Limited", d.company)
        assertEquals("Senior 3D Visualizer", d.role)
        assertEquals("UP, IN", d.location)
        assertTrue(d.salaryRaw.contains("INR"))
        val stripped = JobPageExtractor.stripInjectedStructuredMetadata(page)
        assertFalse(stripped.contains("STRUCTURED_JOB_METADATA"))
        assertTrue(stripped.contains("Apply now"))
    }

    @Test
    fun mergeDrafts_prefersFirstNonBlank() {
        val a = StructuredJobDraft(company = "A Co", role = "", salaryRaw = "10 LPA")
        val b = StructuredJobDraft(company = "B Co", role = "Engineer", salaryRaw = "")
        val m = JobPageExtractor.mergeStructuredJobDrafts(a, b)
        assertEquals("A Co", m.company)
        assertEquals("Engineer", m.role)
        assertEquals("10 LPA", m.salaryRaw)
    }
}
