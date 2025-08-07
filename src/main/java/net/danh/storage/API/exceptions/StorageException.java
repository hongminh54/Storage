package net.danh.storage.API.exceptions;

/**
 * Base exception class for Storage API operations
 * All Storage-related exceptions extend this class
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageException extends Exception {

    /**
     * Create new StorageException with message
     *
     * @param message Error message
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Create new StorageException with message and cause
     *
     * @param message Error message
     * @param cause   Underlying cause
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create new StorageException with cause
     *
     * @param cause Underlying cause
     */
    public StorageException(Throwable cause) {
        super(cause);
    }
}
