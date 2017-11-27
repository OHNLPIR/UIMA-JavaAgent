package edu.mayo.bsi.uima.perf.interceptors.sync;

import edu.mayo.bsi.uima.perf.UIMAAgent;
import edu.mayo.bsi.uima.perf.AnnotationIndices;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.uima.cas.CAS;

import java.util.logging.Level;

/**
 * Intercepts calls to {@link org.apache.uima.cas.impl.CASImpl#reset()} and cleans up the associated annotation index as
 * well
 */
public class CASCleanupInterceptor {
    public static void intercept(@SuperCall Runnable call, @This CAS cas) {
        try {
            AnnotationIndices.removeIndex(cas.getJCas());
        } catch (Exception e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "An error occurred during cas reset interception", e);
        }
        call.run();
    }
}
