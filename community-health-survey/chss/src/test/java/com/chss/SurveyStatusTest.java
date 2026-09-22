package com.chss;

import com.chss.entity.SurveyStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyStatusTest {

    @Test
    void allowsValidForwardTransitions() {
        assertTrue(SurveyStatus.DRAFT.canTransitionTo(SurveyStatus.SUBMITTED));
        assertTrue(SurveyStatus.SUBMITTED.canTransitionTo(SurveyStatus.VERIFIED));
        assertTrue(SurveyStatus.VERIFIED.canTransitionTo(SurveyStatus.CLOSED));
    }

    @Test
    void rejectsSkippingStatuses() {
        assertFalse(SurveyStatus.DRAFT.canTransitionTo(SurveyStatus.VERIFIED));
        assertFalse(SurveyStatus.DRAFT.canTransitionTo(SurveyStatus.CLOSED));
    }

    @Test
    void rejectsTransitionsFromClosed() {
        assertFalse(SurveyStatus.CLOSED.canTransitionTo(SurveyStatus.DRAFT));
        assertFalse(SurveyStatus.CLOSED.canTransitionTo(SurveyStatus.SUBMITTED));
    }
}
