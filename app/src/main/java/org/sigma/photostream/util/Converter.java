package org.sigma.photostream.util;

/**
 * A neat little utility interface to convert things
 *
 * @author Tobias Highfill
 */
public interface Converter<F,T> {
    /**
     * Converts the input into the output type
     * @param src The original object
     * @return The output object
     */
    T convert(F src);
}
