package edu.mayo.bsi.uima.perf.interceptors.perf;

import edu.mayo.bsi.uima.perf.UIMAAgent;
import edu.mayo.bsi.uima.perf.AnnotationIndices;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Intercepts calls to {@link org.apache.uima.fit.util.JCasUtil#selectCovering(JCas, Class, int, int)},
 * {@link org.apache.uima.fit.util.JCasUtil#selectCovering(Class, AnnotationFS)} , and
 * {@link org.apache.uima.fit.util.JCasUtil#selectCovering(JCas, Class, AnnotationFS)}
 * and redirects to {@link edu.mayo.bsi.uima.perf.structures.AnnotationIndex#getCovering(int, int, Class)}
 */
public class SelectCoveringInterceptor {
    // JCasUtil#selectCovering(JCas, Class, int, int)
    public static <T extends Annotation> List<T> intercept(@Argument(0) JCas cas, @Argument(1) Class<T> clazz, @Argument(2) int begin, @Argument(3) int end) {
        return AnnotationIndices.getForCas(cas, true).getCovering(begin, end, clazz);
    }
    // JCasUtil#selectCovering(Class, AnnotationFS)
    public static <T extends Annotation> List<T> intercept(@SuperCall Callable<List<T>> call, @Argument(0) Class<T> clazz, @Argument(1) AnnotationFS coveredAnnotation) {
        try {
            return AnnotationIndices.getForCas(coveredAnnotation.getView().getJCas(), true).getCovering(coveredAnnotation.getBegin(), coveredAnnotation.getEnd(), clazz);
        } catch (CASException e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "Error getting CAS for JCasUtil#selectCovering(Class, AnnotationFS)", e);
            UIMAAgent.LOGGER.log(Level.WARNING, "Method will be delegated back to default");
            try {
                return call.call();
            } catch (Exception e1) {
                UIMAAgent.LOGGER.log(Level.SEVERE, "Could not forward call to original runnable!", e1);
                throw new RuntimeException("Fatal error, please check logs: ", e1);
            }
        }
    }
    // JCasUtil#selectCovering(JCas, Class, AnnotationFS)
    public static <T extends Annotation> List<T> intercept(@Argument(0) JCas cas, @Argument(1) Class<T> clazz, @Argument(2) AnnotationFS coveredAnnotation) {
        return AnnotationIndices.getForCas(cas, true).getCovering(coveredAnnotation.getBegin(), coveredAnnotation.getEnd(), clazz);
    }
}
