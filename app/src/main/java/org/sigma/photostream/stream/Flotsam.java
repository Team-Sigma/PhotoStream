package org.sigma.photostream.stream;

import android.graphics.Bitmap;

/**
 * @author Tobias Highfill
 */
public class Flotsam {
    private final Bitmap img;
    public String name = "";
    public String description = "";

    public Flotsam(Bitmap img){
        this.img = img;
    }

    public Flotsam(Bitmap img, String name){
        this(img);
        this.name = name;
    }

    public Flotsam(Bitmap img, String name, String description){
        this(img, name);
        this.description = description;
    }
}
