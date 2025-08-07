package net.danh.storage.API.exceptions;

/**
 * Exception thrown when storage operation fails due to storage being full
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class StorageFullException extends StorageException {

    /**
     * Create new StorageFullException with default message
     */
    public StorageFullException() {
        super("Storage is full");
    }

    /**
     * Create new StorageFullException with custom message
     *
     * @param message Error message
     */
    public StorageFullException(String message) {
        super(message);
    }

    /**
     * Create new StorageFullException with message and cause
     *
     * @param message Error message
     * @param cause   Underlying cause
     */
    public StorageFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
