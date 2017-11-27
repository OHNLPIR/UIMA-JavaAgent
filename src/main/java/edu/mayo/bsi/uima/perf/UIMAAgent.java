package edu.mayo.bsi.uima.perf;

import edu.mayo.bsi.uima.perf.interceptors.perf.SelectCoveredInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.perf.SelectCoveringInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.sync.AddFsToIndexesInterceptor;
import edu.mayo.bsi.uima.perf.interceptors.sync.CASCleanupInterceptor;
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
                // Redefine CASImpl
                .type(ElementMatchers.named("org.apache.uima.cas.impl.CASImpl"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type,
                                                            ClassLoader ignored, JavaModule ignored2) {
                        return builder.method(ElementMatchers.named("addFsToIndexes"))
                                .intercept(MethodDelegation.to(AddFsToIndexesInterceptor.class))
                                .method(ElementMatchers.named("removeFsFromIndexes"))
                                .intercept(MethodDelegation.to(RemoveFSFromIndexesInterceptor.class))
                                .method(ElementMatchers.named("reset"))
                                .intercept(MethodDelegation.to(CASCleanupInterceptor.class));
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
