package org.sigma.photostream.util;

/**
 * Static utility class for useful functions
 * @author Tobias Highfill
 */
public class Util {
    private Util(){}

    /**
     * Concatenate a list of {@link String}s with another string in between.
     * @param joinWith The {@link String} to join the others with
     * @param strings The strings to be joined
     * @return The concatenation of all the strings with the joiner between them
     */
    public static String join(String joinWith, String... strings){
        return join(joinWith, new StringBuilder(), strings);
    }

    /**
     * Concatenate a list of {@link String}s with another string in between.
     * @param joinWith The {@link String} to join the others with
     * @param dest The {@link StringBuilder} to use
     * @param strings The strings to be joined
     * @return The concatenation of all the strings with the joiner between them
     */
    public static String join(String joinWith, StringBuilder dest, String... strings){
        for(int i=0; i<strings.length; i++){
            if(i != 0)
                dest.append(joinWith);
            dest.append(strings[i]);
        }
        return dest.toString();
    }
}
