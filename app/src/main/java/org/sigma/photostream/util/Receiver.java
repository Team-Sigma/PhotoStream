package org.sigma.photostream.util;

/**
 * Created by mattress on 3/5/2016.
 */
public interface Receiver<E> {
    void receive(E e);
}
