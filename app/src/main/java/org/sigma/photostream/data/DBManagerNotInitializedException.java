package org.sigma.photostream.data;

/**
 * Simple exception for when the DatabaseManager hasn't yet been instantiated
 *
 * @author Tobias Highfill
 */
public class DBManagerNotInitializedException extends RuntimeException {
    public DBManagerNotInitializedException(){
        super("DatabaseManager not initialized yet!!");
    }
}
