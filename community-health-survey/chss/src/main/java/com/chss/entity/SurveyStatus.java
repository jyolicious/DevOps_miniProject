package com.chss.entity;

/**
 * Status workflow frozen in Week 1: DRAFT -> SUBMITTED -> VERIFIED -> CLOSED.
 * Only forward, single-step transitions are permitted (US-3).
 */
public enum SurveyStatus {
    DRAFT,
    SUBMITTED,
    VERIFIED,
    CLOSED;

    public boolean canTransitionTo(SurveyStatus target) {
        return switch (this) {
            case DRAFT -> target == SUBMITTED;
            case SUBMITTED -> target == VERIFIED;
            case VERIFIED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
