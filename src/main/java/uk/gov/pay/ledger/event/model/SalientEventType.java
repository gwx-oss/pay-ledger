package uk.gov.pay.ledger.event.model;

import java.util.Arrays;
import java.util.Optional;

public enum SalientEventType {
    PAYMENT_CREATED,
    PAYMENT_STARTED,
    PAYMENT_EXPIRED,
    AUTHORISATION_REJECTED,
    STATUS_CORRECTED_TO_AUTHORISATION_REJECTED_TO_MATCH_GATEWAY_STATUS,
    AUTHORISATION_SUCCEEDED,
    AUTHORISATION_CANCELLED,
    GATEWAY_ERROR_DURING_AUTHORISATION,
    GATEWAY_TIMEOUT_DURING_AUTHORISATION,
    UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION,
    STATUS_CORRECTED_TO_AUTHORISATION_ERROR_TO_MATCH_GATEWAY_STATUS,
    GATEWAY_REQUIRES_3DS_AUTHORISATION,
    CAPTURE_CONFIRMED,
    CAPTURE_SUBMITTED,
    CAPTURE_ERRORED,
    CAPTURE_ABANDONED_AFTER_TOO_MANY_RETRIES,
    USER_APPROVED_FOR_CAPTURE,
    USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL,
    SERVICE_APPROVED_FOR_CAPTURE,
    STATUS_CORRECTED_TO_CAPTURED_TO_MATCH_GATEWAY_STATUS,
    CAPTURE_CONFIRMED_BY_GATEWAY_NOTIFICATION,
    CANCEL_BY_EXPIRATION_SUBMITTED,
    CANCEL_BY_EXPIRATION_FAILED,
    CANCELLED_BY_EXPIRATION,
    CANCEL_BY_EXTERNAL_SERVICE_SUBMITTED,
    CANCEL_BY_EXTERNAL_SERVICE_FAILED,
    CANCELLED_BY_EXTERNAL_SERVICE,
    CANCEL_BY_USER_SUBMITTED,
    CANCEL_BY_USER_FAILED,
    CANCELLED_BY_USER,

    REFUND_CREATED_BY_USER,
    REFUND_CREATED_BY_SERVICE,
    REFUND_SUBMITTED,
    REFUND_SUCCEEDED,
    REFUND_ERROR;

    public static Optional<SalientEventType> from(String eventName) {
        return Arrays.stream(SalientEventType.values())
                .filter(v -> v.name().equals(eventName))
                .findFirst();
    }
}
