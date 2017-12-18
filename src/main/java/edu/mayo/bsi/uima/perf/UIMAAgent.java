package edu.mayo.bsi.uima.perf;

import edu.mayo.bsi.uima.perf.interceptors.perf.SelectCoveredInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.perf.SelectCoveringInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.sync.AddFsToIndexesInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.sync.FSIndexRepositoryCleanupInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.sync.RemoveFSFromIndexesInterceptor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class UIMAAgent {
    public static Logger LOGGER = Logger.getLogger("UIMA-Agent");

    public static void premain(String arg, Instrumentation inst) {
        new AgentBuilder.Default()
                // Redefine FSIndexRepository
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("org.apache.uima.cas.FSIndexRepository")))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                                            ClassLoader ignored, JavaModule ignored2) {
                        return builder.method(ElementMatchers.hasParameters(ElementMatchers.whereAny(ElementMatchers.named("org.apache.uima.cas.FeatureStructure"))).and(ElementMatchers.named("addFS")))
                                .intercept(MethodDelegation.to(AddFsToIndexesInterceptor.class))
                                .method(ElementMatchers.hasParameters(ElementMatchers.whereAny(ElementMatchers.named("org.apache.uima.cas.FeatureStructure"))).and(ElementMatchers.named("removeFS")))
                                .intercept(MethodDelegation.to(RemoveFSFromIndexesInterceptor.class));
                    }
                })
                .type(ElementMatchers.named("org.apache.uima.cas.impl.FSIndexRepositoryImpl"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
                        return builder.method(ElementMatchers.named("flush"))
                                .intercept(MethodDelegation.to(FSIndexRepositoryCleanupInterceptor.class));
                    }
                })
                // Redefine JCasUtil
                .type(ElementMatchers.named("org.apache.uima.fit.util.JCasUtil"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                                            ClassLoader ignored, JavaModule ignored2) {
                        return builder.method(ElementMatchers.named("selectCovered"))
                                .intercept(MethodDelegation.to(SelectCoveredInterceptor.class))
                                .method(ElementMatchers.named("selectCovering"))
                                .intercept(MethodDelegation.to(SelectCoveringInterceptor.class));
                    }
                })
                .installOn(inst);
    }

}
