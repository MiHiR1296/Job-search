package com.careerops.mobile.ui

/**
 * High-level app phase for onboarding, normal use, and deep-link/share job flows.
 * Derived in [MainViewModel] whenever UI state is committed.
 */
enum class AppFlowPhase {
    /** Onboarding wizard (resume + constraints) not finished. */
    FirstRun,

    /** Profile ready; user drives tabs manually. */
    Ready,

    /** A job URL arrived via share/deep link and auto-flow may be active. */
    JobFromShare
}
