package org.sigma.photostream.stream;

import java.util.List;

/**
 * Created by mattress on 2/14/2016.
 */
public abstract class Stream {

    /**
     * Discard all images and restart
     */
    public abstract void refresh();

    /**
     * Check if this stream has ended
     * @return true if more images can be loaded, false otherwise
     */
    public abstract boolean hasMoreImages();

    /**
     * Count the currently loaded images
     * @return The current number of images
     */
    public abstract int count();

    /**
     * Get the next image
     * @return The next image
     */
    public abstract Flotsam next();

    /**
     * Return all currently loaded images as a list
     * @return A list of all currently loaded images
     */
    public abstract List<Flotsam> toList();
}
