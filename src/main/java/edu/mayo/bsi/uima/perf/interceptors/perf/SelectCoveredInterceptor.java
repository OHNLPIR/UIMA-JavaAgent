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
 * Intercepts calls to {@link org.apache.uima.fit.util.JCasUtil#selectCovered(JCas, Class, int, int)},
 * {@link org.apache.uima.fit.util.JCasUtil#selectCovered(Class, AnnotationFS)} , and
 * {@link org.apache.uima.fit.util.JCasUtil#selectCovered(JCas, Class, AnnotationFS)}
 * and redirects to {@link edu.mayo.bsi.uima.perf.structures.AnnotationIndex#getCovered(int, int, Class)}
 */
public class SelectCoveredInterceptor {
    // JCasUtil#selectCovered(JCas, Class, int, int)
    public static <T extends Annotation> List<T> intercept(@Argument(0) JCas cas, @Argument(1) Class<T> clazz, @Argument(2) int begin, @Argument(3) int end) {
        return AnnotationIndices.getForCas(cas, true).getCovered(begin, end, clazz);
    }
    // JCasUtil#selectCovered(Class, AnnotationFS)
    public static <T extends Annotation> List<T> intercept(@SuperCall Callable<List<T>> call, @Argument(0) Class<T> clazz, @Argument(1) AnnotationFS coveringAnnotation) {
        try {
            return AnnotationIndices.getForCas(coveringAnnotation.getView().getJCas(), true).getCovered(coveringAnnotation.getBegin(), coveringAnnotation.getEnd(), clazz);
        } catch (CASException e) {
            UIMAAgent.LOGGER.log(Level.SEVERE, "Error getting CAS for JCasUtil#selectCovered(Class, AnnotationFS)", e);
            UIMAAgent.LOGGER.log(Level.WARNING, "Method will be delegated back to default");
            try {
                return call.call();
            } catch (Exception e1) {
                UIMAAgent.LOGGER.log(Level.SEVERE, "Could not forward call to original runnable!", e1);
                throw new RuntimeException("Fatal error, please check logs: ", e1);
            }
        }
    }
    // JCasUtil#selectCovered(JCas, Class, AnnotationFS)
    public static <T extends Annotation> List<T> intercept(@Argument(0) JCas cas, @Argument(1) Class<T> clazz, @Argument(2) AnnotationFS coveringAnnotation) {
        return AnnotationIndices.getForCas(cas, true).getCovered(coveringAnnotation.getBegin(), coveringAnnotation.getEnd(), clazz);
    }
}
