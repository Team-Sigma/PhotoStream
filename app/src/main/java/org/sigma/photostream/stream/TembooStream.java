package org.sigma.photostream.stream;

import android.content.Context;

import com.temboo.core.Choreography;
import com.temboo.core.TembooException;
import com.temboo.core.TembooSession;

/**
 * @author Tobias Highfill
 */
public abstract class TembooStream extends BufferedStream{
    private static TembooSession session = null;

    public TembooStream(long id, Context context) {
        super(id, context);
    }

    protected static TembooSession getSession(){
        if(session == null) {
            try {
                session = new TembooSession(
                        "trh52", "PhotoStream", "xjm5kGvEsjvgODkSdL10aHyWru5UopWS"
                );
            } catch (TembooException e) {
                e.printStackTrace();
            }
        }
        return session;
    }


}
