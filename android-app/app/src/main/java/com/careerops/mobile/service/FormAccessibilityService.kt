package com.careerops.mobile.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.careerops.mobile.data.CandidateProfile

class FormAccessibilityService : AccessibilityService() {
    private val mapper = FormFieldMapper(
        CandidateProfile()
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED
        ) {
            return
        }

        val node = event.source ?: return
        tryAutoFill(node)
    }

    override fun onInterrupt() {
        // No-op for MVP.
    }

    private fun tryAutoFill(node: AccessibilityNodeInfo) {
        if (!node.isEditable) return
        val suggested = mapper.match(node) ?: return

        // Skip already-filled fields.
        val current = node.text?.toString()?.trim().orEmpty()
        if (current.isNotEmpty()) return

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                suggested
            )
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}

