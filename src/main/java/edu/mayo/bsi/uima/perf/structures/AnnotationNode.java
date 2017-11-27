package edu.mayo.bsi.uima.perf.structures;


import org.apache.uima.cas.text.AnnotationFS;

import java.util.*;

public final class AnnotationNode implements AnnotationIndex {

    private AnnotationIndex left;
    private AnnotationIndex right;
    private int split;

    AnnotationNode(int start, int end) {
        split = (start + end) / 2;
        if ((split - start) > MIN_LEAF_SIZE) {
            left = new AnnotationNode(start, split);
            right = new AnnotationNode(split + 1, end);
        } else {
            left = new AnnotationLeaf();
            right = new AnnotationLeaf();
        }
    }

    @Override
    public void insert(AnnotationFS ann) {
        if (ann.getBegin() <= split) {
            left.insert(ann);
        }
        if (ann.getEnd() > split) {
            right.insert(ann);
        }
    }

    @Override
    public void remove(AnnotationFS ann) {
        if (ann.getBegin() <= split) {
            left.remove(ann);
        }
        if (ann.getEnd() > split) {
            right.remove(ann);
        }
    }

    @Override
    public <T extends AnnotationFS> List<T> getCovering(int start, int end, Class<T> clazz) {
        LinkedList<T> build = new LinkedList<T>();
        HashSet<T> set = new HashSet<T>();
        if (start <= split) {
            for (T ann : left.getCovering(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        if (end > split) {
            for (T ann : right.getCovering(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        return build;
    }

    @Override
    public <T extends AnnotationFS> List<T> getCovered(int start, int end, Class<T> clazz) {
        LinkedList<T> build = new LinkedList<T>();
        HashSet<T> set = new HashSet<T>();
        if (start <= split) {
            for (T ann : left.getCovered(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        if (end > split) {
            for (T ann : right.getCovered(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        return build;
    }

    @Override
    public <T extends AnnotationFS> List<T> getCollisions(int start, int end, Class<T> clazz) {
        LinkedList<T> build = new LinkedList<T>();
        HashSet<T> set = new HashSet<T>();
        if (start <= split) {
            for (T ann : left.getCollisions(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        if (end > split) {
            for (T ann : right.getCollisions(start, end, clazz)) {
                if (set.add(ann)) {
                    build.add(ann);
                }
            }
        }
        return build;
    }

    @Override
    public void grow(int size) {
        throw new UnsupportedOperationException("Growth of an annotation index should be accomplished through the AnnotationRoot");
    }

    // Accessor method to allow for growth operations
    void setLeft(AnnotationIndex left) {
        this.left = left;
    }

    @Override
    public void clear() {
        this.left.clear();
        this.right.clear();
        this.left = null;
        this.right = null;
    }
}