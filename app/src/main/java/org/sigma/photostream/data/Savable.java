package org.sigma.photostream.data;

import android.content.ContentValues;

/**
 * For objects that can be saved into SQLite
 * @author Tobias Highfill
 */
public interface Savable extends Identifiable {

    /**
     * Get a ContentValues object containing the data to be saved
     * @return The ContentValues object containing the data to be saved
     */
    ContentValues toContentValues();

    /**
     * Get the name of the table to store it in
     * @return The name of the table
     */
    String getTable();

    /**
     * Get a nullable column to set in case we want to insert an empty row
     * @return The name of a nullable column
     */
    String nullColumn();

    /**
     * Gets a human readable name for this object
     * @return A name for this object
     */
    String getName();
}
