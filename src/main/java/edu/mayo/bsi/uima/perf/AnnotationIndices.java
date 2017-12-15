package edu.mayo.bsi.uima.perf;

import edu.mayo.bsi.uima.perf.structures.AnnotationIndex;
import edu.mayo.bsi.uima.perf.structures.AnnotationRoot;
import org.apache.uima.cas.*;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Maintains a mapping of current {@link AnnotationIndex} for tracked CASes.<br>
 * <p>
 * <b>Note:</b> Storage and retrieval of indexes are thread safe at a UUID granularity, but individual
 * {@link AnnotationIndex} are not thread-safe: only one thread should be manipulating an AnnotationIndex
 * of a given UID at any given time. In other words, assuming each CAS is associated with a truly unique UID,
 * while multiple CASes can be processed at once, only one thread should be the index of a given CAS at any time
 * </p>
 */
public class AnnotationIndices {
    /**
     * A map of current Annotation indexes being maintained
     */
    private static final Map<UUID, AnnotationIndex> CURR_INDICES = new NonBlockingHashMap<UUID, AnnotationIndex>();

    /**
     * The private field containing the associated CAS implementation within {@link FSIndexRepositoryImpl}
     */
    private static Field FS_INDEX_CAS_FIELD;

    static {
        try {
            FS_INDEX_CAS_FIELD = FSIndexRepositoryImpl.class.getDeclaredField("cas");
            FS_INDEX_CAS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Thread-safe: retrieves an Annotation Index by its UID
     *
     * @param uuid The UUID obtained from metadata stored in the CAS
     * @return The {@link AnnotationIndex} associated with this UID, if it exists, null otherwise
     */
    public static AnnotationIndex forUID(final UUID uuid) {
        return CURR_INDICES.get(uuid);
    }

    /**
     * Thread-safe: creates, stores, and/or retrieves an Annotation Index by its UID
     *
     * @param uuid The UUID obtained from metadata stored in the CAS
     * @return The {@link AnnotationIndex} associated with this UID if it exists, otherwise returns a new instance
     * associated with the parameter UID
     */
    public static AnnotationIndex createIndex(final UUID uuid) {
        if (CURR_INDICES.get(uuid) != null) {
            return CURR_INDICES.get(uuid);
        } else {
            AnnotationIndex ret = new AnnotationRoot();
            CURR_INDICES.put(uuid, ret);
            return ret;
        }
    }

    /**
     * Thread-safe: removes and empties a tracked Annotation Index by its UID, if present
     *
     * @param uuid The UID associated with the Annotation Index to remove
     */
    public static void removeIndex(final UUID uuid) {
        AnnotationIndex removed = CURR_INDICES.remove(uuid);
        if (removed != null) {
            removed.clear();
        }
    }

    /**
     * Not thread-safe: convenience method to get an AnnotationIndex associated with a CAS object
     *
     * @param cas       The CAS to retrieve an AnnotationIndex for
     * @param createNew Whether to create a new annotation index if one does not already exist
     * @return The AnnotationIndex associated with this cas, if it exists, otherwise
     * instantiates new index and returns if createNew is true, or returns null
     */
    @Contract("_, true -> !null")
    public static AnnotationIndex getForCas(final JCas cas, boolean createNew) {
        // We actually can't use selectSingle here because it.....throws an exception (lol) if not exactly one
        // and the cost of throwing/catching exception is significant...
        // so instead we duplicate code from CasUtil#selectSingle.
        Type t = JCasUtil.getType(cas, PerformanceMetadata.class);
        FSIterator<FeatureStructure> iterator = cas.getIndexRepository().getAllIndexedFS(t);

        PerformanceMetadata meta;
        if (iterator.hasNext()) {
            meta = (PerformanceMetadata) iterator.next();
        } else {
            meta = null;
        }
        if (iterator.hasNext()) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "Multiple performance metadata associated with cas, this may lead to undefined behaviour and should never happen!");
        }

        if (meta == null) {
            if (!createNew) {
                return null;
            }
            UUID uid = UUID.randomUUID();
            meta = new PerformanceMetadata(cas);
            meta.setUidLeastSig(uid.getLeastSignificantBits());
            meta.setUidMostSig(uid.getMostSignificantBits());
            meta.addToIndexes();
            // Populate because low-level cas deserializers may have not been instrumented
            AnnotationIndex ret = createIndex(uid);
            for (Annotation ann : JCasUtil.select(cas, Annotation.class)) {
                ret.insert(ann);
            }
            return ret;
        } else {
            long leastSig = meta.getUidLeastSig();
            long mostSig = meta.getUidMostSig();
            UUID retrievalUID = new UUID(mostSig, leastSig);
            return forUID(retrievalUID);
        }
    }

    /**
     * Not thread-safe: convenience method to get an AnnotationIndex associated with an FSIndexRepository
     *
     * @param indexRepository       The CAS to retrieve an AnnotationIndex for
     * @param createNew Whether to create a new annotation index if one does not already exist
     * @return The AnnotationIndex associated with this cas, if it exists, otherwise
     * instantiates new index and returns if createNew is true, or returns null
     */
    @Contract("_, true -> !null")
    public static AnnotationIndex getForFSIndexRepository(final FSIndexRepository indexRepository, boolean createNew) {
        if (!(indexRepository instanceof FSIndexRepositoryImpl)) {
            throw new IllegalStateException("Non FSIndexRepositoryImpl implementation being used as an index repository!");
        }
        CASImpl cas;
        try {
            cas = (CASImpl) FS_INDEX_CAS_FIELD.get(indexRepository);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        // We actually can't use selectSingle here because it.....throws an exception (lol) if not exactly one
        // and the cost of throwing/catching exception is significant...
        // so instead we duplicate code from CasUtil#selectSingle.
        Type t = CasUtil.getType(cas, PerformanceMetadata.class);
        FSIterator<FeatureStructure> iterator = indexRepository.getAllIndexedFS(t);

        PerformanceMetadata meta;
        if (iterator.hasNext()) {
            meta = (PerformanceMetadata) iterator.next();
        } else {
            meta = null;
        }
        if (iterator.hasNext()) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "Multiple performance metadata associated with cas, this may lead to undefined behaviour and should never happen!");
        }

        if (meta == null) {
            if (!createNew) {
                return null;
            }
            UUID uid = UUID.randomUUID();
            JCas jCas;
            try {
                jCas = cas.getJCas();
            } catch (CASException e) {
                throw new IllegalStateException("Could not get associated JCAS from associated CAS of index");
            }
            meta = new PerformanceMetadata(jCas);
            meta.setUidLeastSig(uid.getLeastSignificantBits());
            meta.setUidMostSig(uid.getMostSignificantBits());
            meta.addToIndexes();
            // Populate because low-level cas deserializers may have not been instrumented
            AnnotationIndex ret = createIndex(uid);
            for (Annotation ann : JCasUtil.select(jCas, Annotation.class)) {
                ret.insert(ann);
            }
            return ret;
        } else {
            long leastSig = meta.getUidLeastSig();
            long mostSig = meta.getUidMostSig();
            UUID retrievalUID = new UUID(mostSig, leastSig);
            return forUID(retrievalUID);
        }
    }

    /**
     * Not thread-safe: removes and empties a tracked Annotation Index asosciated with this cas, if it exists
     *
     * @param cas The cas associated with the index to remove
     */
    public static void removeIndex(JCas cas) {
        PerformanceMetadata meta = JCasUtil.selectSingle(cas, PerformanceMetadata.class);
        removeIndex(meta);
    }

    /**
     * Not thread-safe: removes and empties a tracked Annotation Index associated with this cas, if it exists
     *
     * @param indexRepository The FS Index Repository associated with the index to remove
     */
    public static void removeIndex(FSIndexRepository indexRepository) {
        if (indexRepository == null) {
            return;
        }
        if (!(indexRepository instanceof FSIndexRepositoryImpl)) {
            throw new IllegalStateException("Non FSIndexRepositoryImpl implementation being used as an index repository!");
        }
        CASImpl cas;
        try {
            cas = (CASImpl) FS_INDEX_CAS_FIELD.get(indexRepository);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        Type t = CasUtil.getType(cas, PerformanceMetadata.class);
        FSIterator<FeatureStructure> iterator = indexRepository.getAllIndexedFS(t);

        PerformanceMetadata meta;
        if (iterator.hasNext()) {
            meta = (PerformanceMetadata) iterator.next();
        } else {
            meta = null;
        }
        if (iterator.hasNext()) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "Multiple performance metadata associated with cas, this may lead to undefined behaviour and should never happen!");
        }
        removeIndex(meta);
    }

    /**
     * Removes an index by its associated metadata
     * @param meta The meta associated with the index
     */
    private static void removeIndex(PerformanceMetadata meta) {
        if (meta != null) {
            long leastSig = meta.getUidLeastSig();
            long mostSig = meta.getUidMostSig();
            UUID retrievalUID = new UUID(mostSig, leastSig);
            removeIndex(retrievalUID);
        }
    }
}
