package org.adnan.travner.util;

import org.bson.types.ObjectId;

/**
 * Utility class for safe ObjectId operations
 */
public class ObjectIdUtil {

    /**
     * Safely converts a string to ObjectId with proper validation
     * 
     * @param id the string to convert
     * @return ObjectId if valid
     * @throws IllegalArgumentException if invalid format
     */
    public static ObjectId safeObjectId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            return new ObjectId(id.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ID format: " + id);
        }
    }

    /**
     * Checks if a string is a valid ObjectId format
     * 
     * @param id the string to check
     * @return true if valid ObjectId format
     */
    public static boolean isValidObjectId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            new ObjectId(id.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}