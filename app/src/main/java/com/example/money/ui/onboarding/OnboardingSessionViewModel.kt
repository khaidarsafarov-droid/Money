package com.example.money.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Keeps the wizard on screen for this visit after the answers are saved. */
class OnboardingSessionViewModel : ViewModel() {
    var active by mutableStateOf(false)
}
