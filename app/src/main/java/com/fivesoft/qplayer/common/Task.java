package com.fivesoft.qplayer.common;

/**
 * A task is a unit of work that can be started, stopped, restarted, and queried for its position and length.<br>
 * The position and length are in metric specified by the task implementation.
 */

public interface Task {

    int UNKNOWN_VALUE = -1;
    int ERROR_SET_SPEED_UNSUPPORTED = -7901;
    int ERROR_SET_POSITION_UNSUPPORTED = -7902;

    /**
     * Starts the task from the last position.
     * @return 0 if success, otherwise error code.
     */

    int start();

    /**
     * Stops the task. The task can be resumed by calling start().
     * @return 0 if success, otherwise error code.
     */

    int stop();

    /**
     * Starts the task from the beginning. Resets the position to 0.
     * @return 0 if success, otherwise error code.
     */

    int restart();

    /**
     * Checks if the task is running.
     * @return true if the task is running, otherwise false.
     */

    boolean isRunning();

    /**
     * Gets the current position of the task.<br>
     * The position is in metric specified by the task.
     * @return The current position of the task or -1 if the position is unknown.
     */

    long getPosition();

    /**
     * Gets the total length of the task.<br>
     * The length is in metric specified by the task.
     * @return The total length of the task or -1 if the length is unknown.
     */

    long getLength();

    /**
     * Sets task position.
     * @param position The position to set. The position is in metric specified by the task.
     * @return 0 if success, otherwise error code. (if unsupported, returns {@link #ERROR_SET_POSITION_UNSUPPORTED})
     */

    default int setPosition(long position){
        return ERROR_SET_POSITION_UNSUPPORTED;
    }

    /**
     * Sets task perform speed. May be unsupported.
     * @param speed The speed to set.
     *              The speed is a factor of the normal speed.<br>
     *              1.0f is normal speed and 2.0f is double speed.
     * @return 0 if success, otherwise error code. (if unsupported, returns {@link #ERROR_SET_SPEED_UNSUPPORTED})
     */

    default int setSpeed(float speed){
        return ERROR_SET_SPEED_UNSUPPORTED;
    }

}
