package c0rnell.flexer.intellij.plugin.provider;

import c0rnell.flexer.intellij.plugin.processor.FlexerProcessorManager;
import c0rnell.flexer.intellij.plugin.processor.Processor;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlexerProcessorProvider implements Disposable {

    public static FlexerProcessorProvider getInstance(@NotNull Project project) {
        final FlexerProcessorProvider service = ServiceManager.getService(project, FlexerProcessorProvider.class);
        service.checkInitialized();
        return service;
    }

    private final Map<Class, Collection<Processor>> flexerTypeProcessors;

    private final Map<String, Collection<Processor>> flexerProcessors;
    private final Collection<String> registeredAnnotationNames;
    private boolean alreadyInitialized;

    public FlexerProcessorProvider() {
        flexerProcessors = new ConcurrentHashMap<>();
        flexerTypeProcessors = new ConcurrentHashMap<>();
        registeredAnnotationNames = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void dispose() {
    }

    private void checkInitialized() {
        if (!alreadyInitialized) {
            initProcessors();
            alreadyInitialized = true;
        }
    }

    public void initProcessors() {
        flexerProcessors.clear();
        flexerTypeProcessors.clear();
        registeredAnnotationNames.clear();

        for (Processor processor : FlexerProcessorManager.getFlexerProcessors()) {
            String[] annotationClasses = processor.getSupportedAnnotationClasses();
            for (@NotNull String annotationClass : annotationClasses) {
                putProcessor(flexerProcessors, annotationClass, processor);
                putProcessor(flexerProcessors, StringUtil.getShortName(annotationClass), processor);
            }

            putProcessor(flexerTypeProcessors, processor.getSupportedClass(), processor);
        }

        registeredAnnotationNames.addAll(flexerProcessors.keySet());
    }

    @NotNull
    Collection<Processor> getFlexerProcessors(@NotNull Class supportedClass) {
        return flexerTypeProcessors.computeIfAbsent(supportedClass, k -> ConcurrentHashMap.newKeySet());
    }

    @NotNull
    public Collection<Processor> getProcessors(@NotNull PsiAnnotation psiAnnotation) {
        final String qualifiedName = psiAnnotation.getQualifiedName();
        final Collection<Processor> result = qualifiedName == null ? null : flexerProcessors.get(qualifiedName);
        return result == null ? Collections.emptySet() : result;
    }

    @NotNull
    Collection<FlexerProcessorData> getApplicableProcessors(@NotNull PsiMember psiMember) {
        Collection<FlexerProcessorData> result = Collections.emptyList();
        if (verifyFlexerAnnotationPresent(psiMember)) {
            result = new ArrayList<>();

            addApplicableProcessors(psiMember, result);
            final PsiClass psiClass = psiMember.getContainingClass();
            if (null != psiClass) {
                addApplicableProcessors(psiClass, result);
            }
        }
        return result;
    }

    private <K, V> void putProcessor(final Map<K, Collection<V>> map, final K key, final V value) {
        Collection<V> valueList = map.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        valueList.add(value);
    }

    private boolean verifyFlexerAnnotationPresent(@NotNull PsiClass psiClass) {
        if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiClass, registeredAnnotationNames)) {
            return true;
        }
        Collection<PsiField> psiFields = PsiClassUtil.collectClassFieldsIntern(psiClass);
        for (PsiField psiField : psiFields) {
            if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiField, registeredAnnotationNames)) {
                return true;
            }
        }
        Collection<PsiMethod> psiMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
        for (PsiMethod psiMethod : psiMethods) {
            if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiMethod, registeredAnnotationNames)) {
                return true;
            }
        }
        final PsiElement psiClassParent = psiClass.getParent();
        if (psiClassParent instanceof PsiClass) {
            return verifyFlexerAnnotationPresent((PsiClass) psiClassParent);
        }

        return false;
    }

    private boolean verifyFlexerAnnotationPresent(@NotNull PsiMember psiMember) {
        if (PsiAnnotationSearchUtil.checkAnnotationsSimpleNameExistsIn(psiMember, registeredAnnotationNames)) {
            return true;
        }

        final PsiClass psiClass = psiMember.getContainingClass();
        return null != psiClass && verifyFlexerAnnotationPresent(psiClass);
    }

    private void addApplicableProcessors(@NotNull PsiMember psiMember, @NotNull Collection<FlexerProcessorData> target) {
        final PsiModifierList psiModifierList = psiMember.getModifierList();
        if (null != psiModifierList) {
            for (PsiAnnotation psiAnnotation : psiModifierList.getAnnotations()) {
                for (Processor processor : getProcessors(psiAnnotation)) {
                    target.add(new FlexerProcessorData(processor, psiAnnotation));
                }
            }
        }
    }
}
