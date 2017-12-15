package edu.mayo.bsi.uima.perf.interceptors.sync;

import edu.mayo.bsi.uima.perf.UIMAAgent;
import edu.mayo.bsi.uima.perf.AnnotationIndices;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;

import java.util.logging.Level;

/**
 * Intercepts calls to {@link org.apache.uima.cas.impl.FSIndexRepositoryImpl#flush()} and cleans up the associated annotation index as
 * well
 */
public class FSIndexRepositoryCleanupInterceptor {
    public static void intercept(@SuperCall Runnable call, @This FSIndexRepositoryImpl indexRepository) {
        try {
            AnnotationIndices.removeIndex(indexRepository);
        } catch (Exception e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "An error occurred during index flush interception", e);
        }
        call.run();
    }
}
