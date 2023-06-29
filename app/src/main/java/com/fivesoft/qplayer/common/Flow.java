package com.fivesoft.qplayer.common;

/**
 * Base interface for classes which can be opened and closed.
 */

public interface Flow {

    /**
     * Error code indicating that the operation was successful.
     */
    int SUCCESS = 0;

    /**
     * Error code indicating that something cannot be done because the flow is closed.
     */
    int ERROR_FLOW_CLOSED = -100001;

    /**
     * Error code indicating that flow cannot be open because it is already opened.
     */
    int ERROR_ALREADY_OPENED = -100002;

    /**
     * Error code indicating that flow has been interrupted.
     */

    int ERROR_INTERRUPTED = -200003;

    /**
     * Error code indicating that an I/O exception occurred.
     */

    int ERROR_IO_EXCEPTION = -200004;

    /**
     * Error code indicating timeout.
     */

    int ERROR_TIMEOUT = -200005;

    /**
     * Open the flow. (Initialize necessary resources)
     * @return 0 if successful, otherwise error code
     */

    int open();

    /**
     * Close the flow. (Release resources)
     * After this method is called, the flow should not be used anymore.
     * @return 0 if successful, otherwise error code
     */

    int close();

}
