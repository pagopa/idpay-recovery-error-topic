package it.gov.pagopa.idpay.error_recovery.utils;

public final class Constants {
    private Constants(){}

    //region header names
    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_ROOT_CAUSE_CLASS = "rootCauseClass";
    public static final String ERROR_MSG_HEADER_ROOT_CAUSE_MESSAGE = "rootCauseMessage";
    public static final String ERROR_MSG_HEADER_CAUSE_CLASS = "causeClass";
    public static final String ERROR_MSG_HEADER_CAUSE_MESSAGE = "causeMessage";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_RETRY = "retry";
    //endregion
}
