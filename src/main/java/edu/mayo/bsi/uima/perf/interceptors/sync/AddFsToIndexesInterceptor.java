package edu.mayo.bsi.uima.perf.interceptors.sync;

import edu.mayo.bsi.uima.perf.AnnotationIndices;
import edu.mayo.bsi.uima.perf.UIMAAgent;
import edu.mayo.bsi.uima.perf.structures.AnnotationIndex;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.logging.Level;

/**
 * Intercepts {@link org.apache.uima.cas.FSIndexRepository#addFS(FeatureStructure)} calls to keep the
 * performance AnnotationIndex up to date
 */
public class AddFsToIndexesInterceptor {

    public static void intercept(@SuperCall Runnable call, @This FSIndexRepository indexRepository, @Argument(0) FeatureStructure fs) {
        try {
            if (fs instanceof Annotation) {
                AnnotationIndex index = AnnotationIndices.getForFSIndexRepository(indexRepository, true);
                index.insert((Annotation) fs);
            }
        } catch (Exception e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "An error occurred during annotation add interception", e);
        }
        call.run();
    }
}
