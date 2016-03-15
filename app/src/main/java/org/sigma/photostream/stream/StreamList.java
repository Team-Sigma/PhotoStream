package org.sigma.photostream.stream;

import android.view.MenuItem;

import org.sigma.photostream.MainActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class to provide an easy place to update the stream menu in the Drawer.
 * Hooks the add and remove functions to add and remove menu items at the same time.
 *
 * @author Tobias Highfill
 */
public class StreamList extends ArrayList<Stream> {

    final MainActivity parent;
    private Map<Stream, MenuItem> menuItemMap = new HashMap<>();

    public StreamList(MainActivity parent){
        this.parent = parent;
        assert this.parent.streamMenu != null;
    }

    private void addStreamMenuItem(final Stream stream){
        MenuItem item = parent.streamMenu.add(stream.name);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                parent.setCurrentStream(stream);
                return false;
            }
        });
        menuItemMap.put(stream, item);
    }

    private void removeStreamMenuItem(Stream stream){
        parent.streamMenu.removeItem(menuItemMap.get(stream).getItemId());
        menuItemMap.remove(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Stream object) {
        addStreamMenuItem(object);
        return super.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, Stream object) {
        addStreamMenuItem(object);
        super.add(index, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends Stream> collection) {
        for(Stream s : collection){
            addStreamMenuItem(s);
        }
        return super.addAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, Collection<? extends Stream> collection) {
        for(Stream s : collection){
            addStreamMenuItem(s);
        }
        return super.addAll(index, collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object object) {
        removeStreamMenuItem((Stream) object);
        return super.remove(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream remove(int index) {
        removeStreamMenuItem(get(index));
        return super.remove(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> collection) {
        for(Object obj : collection){
            removeStreamMenuItem((Stream) obj);
        }
        return super.removeAll(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        for(int i=0; i<toIndex; i++){
            removeStreamMenuItem(get(i));
        }
        super.removeRange(fromIndex, toIndex);
    }
}
