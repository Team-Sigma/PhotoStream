package org.sigma.photostream.util;

/**
 * Created by mattress on 2/18/2016.
 */
public interface Converter<F,T> {
    /**
     * Converts the input into the output type
     * @param src
     * @return
     */
    T convert(F src);
}
