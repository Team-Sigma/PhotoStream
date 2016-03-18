package org.sigma.photostream.util;

/**
 * Convenience for getting something (that could change) on demand
 * @author Tobias Highfill
 */
public interface Giver<E> {
    E give();
}
