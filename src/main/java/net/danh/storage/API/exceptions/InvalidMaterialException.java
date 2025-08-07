package net.danh.storage.API.exceptions;

/**
 * Exception thrown when an invalid material is used in storage operations
 *
 * @author VoChiDanh, hongminh54
 * @version 2.3.2
 */
public class InvalidMaterialException extends StorageException {

    /**
     * Create new InvalidMaterialException with default message
     */
    public InvalidMaterialException() {
        super("Invalid material");
    }

    /**
     * Create new InvalidMaterialException with custom message
     *
     * @param message Error message
     */
    public InvalidMaterialException(String message) {
        super(message);
    }

    /**
     * Create new InvalidMaterialException with message and cause
     *
     * @param message Error message
     * @param cause   Underlying cause
     */
    public InvalidMaterialException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create new InvalidMaterialException for specific material
     *
     * @param material The invalid material
     */
    public InvalidMaterialException(String material, String reason) {
        super("Invalid material '" + material + "': " + reason);
    }
}
