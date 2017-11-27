package edu.mayo.bsi.uima.perf.structures;


import org.apache.uima.cas.text.AnnotationFS;

import java.util.*;

public final class AnnotationLeaf implements AnnotationIndex {

    private ArrayList<AnnotationFS> annColl;
    private boolean dirty;

    AnnotationLeaf() {
        annColl = new ArrayList<AnnotationFS>();
        dirty = false;
    }

    @Override
    public void insert(AnnotationFS ann) {
        annColl.add(ann);
        dirty = true;
    }

    @Override
    public void remove(AnnotationFS ann) {
        annColl.remove(ann);
    }

    @Override
    public <T extends AnnotationFS> List<T> getCovering(int start, int end, Class<T> clazz) {
        if (dirty) {
            sort();
        }
        LinkedList<T> ret = new LinkedList<T>();
        for (AnnotationFS ann : annColl) {
            if (ann.getBegin() >= start && ann.getEnd() <= end) {
                if (clazz.isInstance(ann)) {
                    //noinspection unchecked
                    ret.add((T) ann);
                }
            }
        }
        return ret;
    }

    @Override
    public <T extends AnnotationFS> List<T> getCovered(int start, int end, Class<T> clazz) {
        if (dirty) {
            sort();
        }
        LinkedList<T> ret = new LinkedList<T>();
        for (AnnotationFS ann : annColl) {
            if (ann.getBegin() <= start && ann.getEnd() >= end) {
                if (clazz.isInstance(ann)) {
                    //noinspection unchecked
                    ret.add((T) ann);
                }
            }
        }
        return ret;
    }

    @Override
    public <T extends AnnotationFS> List<T> getCollisions(int start, int end, Class<T> clazz) {
        if (dirty) {
            sort();
        }
        LinkedList<T> ret = new LinkedList<T>();
        for (AnnotationFS ann : annColl) {
            if ((ann.getBegin() <= start && ann.getEnd() > start) || (ann.getBegin() >= start
                    && ann.getBegin() <= end)) {
                if (clazz.isInstance(ann)) {
                    //noinspection unchecked
                    ret.add((T) ann);
                }
            }
        }
        return ret;
    }

    private void sort() {
        Collections.sort(annColl, new Comparator<AnnotationFS>() {
            @Override
            public int compare(AnnotationFS o1, AnnotationFS o2) {
                int startPos = o1.getBegin() - o2.getBegin();
                if (startPos != 0) {
                    return startPos > 0 ? 1 : -1;
                }
                int endPos = o1.getEnd() - o2.getEnd();
                if (endPos != 0) {
                    return endPos > 0 ? 1 : -1;
                }
                return 0;
            }
        });
        dirty = false;
    }

    @Override
    public void grow(int size) {
        throw new UnsupportedOperationException("Growth of an AnnotationFS index should be accomplished through the AnnotationRoot");
    }

    @Override
    public void clear() {
        this.annColl = null;
    }
}
