package c0rnell.flexer.intellij.plugin.processor.clazz;

import c0rnell.flexer.intellij.plugin.problem.LombokProblem;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemEmptyBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemNewBuilder;
import c0rnell.flexer.intellij.plugin.processor.AbstractProcessor;
import c0rnell.flexer.intellij.plugin.psi.LombokLightClassBuilder;
import c0rnell.flexer.intellij.plugin.util.LombokUtils;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base lombok processor class for class annotations
 *
 * @author Plushnikov Michail
 */
public abstract class AbstractClassProcessor extends AbstractProcessor implements ClassProcessor {

    protected AbstractClassProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                                     @NotNull String supportedAnnotationClass) {
        super(supportedClass, supportedAnnotationClass);
    }

    protected AbstractClassProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                                     @NotNull String supportedAnnotationClass,
                                     @NotNull String equivalentAnnotationClass) {
        super(supportedClass, supportedAnnotationClass, equivalentAnnotationClass);
    }

    @NotNull
    @Override
    public List<? super PsiElement> process(@NotNull PsiClass psiClass, @Nullable String nameHint) {
        List<? super PsiElement> result = Collections.emptyList();
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, getSupportedAnnotationClasses());
        if (null != psiAnnotation
            && supportAnnotationVariant(psiAnnotation)
            && possibleToGenerateElementNamed(nameHint, psiClass, psiAnnotation)
            && validate(psiAnnotation, psiClass, ProblemEmptyBuilder.getInstance())
        ) {
            result = new ArrayList<>();
            generatePsiElements(psiClass, psiAnnotation, result);
        }
        return result;
    }

    protected boolean possibleToGenerateElementNamed(@Nullable String nameHint,
                                                     @NotNull PsiClass psiClass,
                                                     @NotNull PsiAnnotation psiAnnotation) {
        return true;
    }

    @NotNull
    @Override
    public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
        Collection<PsiAnnotation> result = new ArrayList<>();
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, getSupportedAnnotationClasses());
        if (null != psiAnnotation) {
            result.add(psiAnnotation);
        }
        return result;
    }

    protected void addClassAnnotation(Collection<PsiAnnotation> result, @NotNull PsiClass psiClass, String... annotationFQNs) {
        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, annotationFQNs);
        if (null != psiAnnotation) {
            result.add(psiAnnotation);
        }
    }

    protected void addFieldsAnnotation(Collection<PsiAnnotation> result, @NotNull PsiClass psiClass, String... annotationFQNs) {
        for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, annotationFQNs);
            if (null != psiAnnotation) {
                result.add(psiAnnotation);
            }
        }
    }

    @NotNull
    @Override
    public Collection<LombokProblem> verifyAnnotation(@NotNull PsiAnnotation psiAnnotation) {
        Collection<LombokProblem> result = Collections.emptyList();
        // check first for fields, methods and filter it out, because PsiClass is parent of all annotations and will match other parents too
        PsiElement psiElement = PsiTreeUtil.getParentOfType(psiAnnotation, PsiField.class, PsiMethod.class, PsiClass.class);
        if (psiElement instanceof PsiClass) {
            ProblemNewBuilder problemNewBuilder = new ProblemNewBuilder();
            validate(psiAnnotation, (PsiClass) psiElement, problemNewBuilder);
            result = problemNewBuilder.getProblems();
        }

        return result;
    }

    protected Optional<PsiClass> getSupportedParentClass(@NotNull PsiClass psiClass) {
        final PsiElement parentElement = psiClass.getParent();
        if (parentElement instanceof PsiClass && !(parentElement instanceof LombokLightClassBuilder)) {
            return Optional.of((PsiClass) parentElement);
        }
        return Optional.empty();
    }

    @Nullable
    protected PsiAnnotation getSupportedAnnotation(@NotNull PsiClass psiParentClass) {
        return PsiAnnotationSearchUtil.findAnnotation(psiParentClass, getSupportedAnnotationClasses());
    }

    protected abstract boolean validate(@NotNull PsiAnnotation psiAnnotation,
                                        @NotNull PsiClass psiClass,
                                        @NotNull ProblemBuilder builder);

    protected abstract void generatePsiElements(@NotNull PsiClass psiClass,
                                                @NotNull PsiAnnotation psiAnnotation,
                                                @NotNull List<? super PsiElement> target);

    void validateOfParam(PsiClass psiClass, ProblemBuilder builder, PsiAnnotation psiAnnotation, Collection<String> ofProperty) {
        for (String fieldName : ofProperty) {
            if (!StringUtil.isEmptyOrSpaces(fieldName)) {
                PsiField fieldByName = psiClass.findFieldByName(fieldName, false);
                if (null == fieldByName) {
                    final String newPropertyValue = calcNewPropertyValue(ofProperty, fieldName);
//                    builder.addWarning(LombokBundle.message("inspection.message.field.s.does.not.exist.field", fieldName),
//                            PsiQuickFixFactory.createChangeAnnotationParameterFix(psiAnnotation, "of", newPropertyValue));
                    builder.addWarning("inspection.message.field.s.does.not.exist.field");
                }
            }
        }
    }

    void validateExcludeParam(PsiClass psiClass, ProblemBuilder builder, PsiAnnotation psiAnnotation, Collection<String> excludeProperty) {
        for (String fieldName : excludeProperty) {
            if (!StringUtil.isEmptyOrSpaces(fieldName)) {
                PsiField fieldByName = psiClass.findFieldByName(fieldName, false);
                if (null == fieldByName) {
                    final String newPropertyValue = calcNewPropertyValue(excludeProperty, fieldName);
//                    builder.addWarning(LombokBundle.message("inspection.message.field.s.does.not.exist.exclude", fieldName),
//                            PsiQuickFixFactory.createChangeAnnotationParameterFix(psiAnnotation, "exclude", newPropertyValue));
                    builder.addWarning("inspection.message.field.s.does.not.exist.exclude");
                } else {
                    if (fieldName.startsWith(LombokUtils.LOMBOK_INTERN_FIELD_MARKER) || fieldByName.hasModifierProperty(PsiModifier.STATIC)) {
                        final String newPropertyValue = calcNewPropertyValue(excludeProperty, fieldName);
//                        builder.addWarning(LombokBundle.message("inspection.message.field.s.would.have.been.excluded.anyway", fieldName),
//                                PsiQuickFixFactory.createChangeAnnotationParameterFix(psiAnnotation, "exclude", newPropertyValue));
                        builder.addWarning("inspection.message.field.s.would.have.been.excluded.anyway");
                    }
                }
            }
        }
    }

    private String calcNewPropertyValue(Collection<String> allProperties, String fieldName) {
        String result = null;
        if (!allProperties.isEmpty() && (allProperties.size() > 1 || !allProperties.contains(fieldName))) {
            result = allProperties.stream().filter(((Predicate<String>) fieldName::equals).negate())
                    .collect(Collectors.joining("\",\"", "{\"", "\"}"));
        }
        return result;
    }

    boolean shouldGenerateExtraNoArgsConstructor(@NotNull PsiClass psiClass) {
        boolean result = !PsiClassUtil.hasSuperClass(psiClass);
//        if (result) {
//            result = configDiscovery.getBooleanLombokConfigProperty(ConfigKey.NO_ARGS_CONSTRUCTOR_EXTRA_PRIVATE, psiClass);
//        }
//        if (result) {
//            result = PsiAnnotationSearchUtil.isNotAnnotatedWith(psiClass, LombokClassNames.NO_ARGS_CONSTRUCTOR, LombokClassNames.ALL_ARGS_CONSTRUCTOR,
//                    LombokClassNames.REQUIRED_ARGS_CONSTRUCTOR);
//        }
        return result;
    }

    boolean readCallSuperAnnotationOrConfigProperty(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass/*, @NotNull ConfigKey configKey*/) {
        final boolean result;
        final Boolean declaredAnnotationValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(psiAnnotation, "callSuper");
        if (null == declaredAnnotationValue) {
//            final String configProperty = configDiscovery.getStringLombokConfigProperty(configKey, psiClass);
            result = PsiClassUtil.hasSuperClass(psiClass)/* && "CALL".equalsIgnoreCase(configProperty)*/;
        } else {
            result = declaredAnnotationValue;
        }
        return result;
    }
}
