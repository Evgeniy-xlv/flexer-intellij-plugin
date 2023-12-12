package c0rnell.flexer.intellij.plugin.processor.field;

import c0rnell.flexer.intellij.plugin.FlexerBundle;
import c0rnell.flexer.intellij.plugin.problem.FlexerProblem;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemEmptyBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemNewBuilder;
import c0rnell.flexer.intellij.plugin.processor.AbstractProcessor;
import c0rnell.flexer.intellij.plugin.util.ProcessorUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import c0rnell.flexer.intellij.plugin.util.Utils;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Base flexer processor class for field annotations
 */
public abstract class AbstractFieldProcessor extends AbstractProcessor implements FieldProcessor {

    AbstractFieldProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                           @NotNull String supportedAnnotationClass) {
        super(supportedClass, supportedAnnotationClass);
    }

    AbstractFieldProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                           @NotNull String supportedAnnotationClass,
                           @NotNull String equivalentAnnotationClass) {
        super(supportedClass, supportedAnnotationClass, equivalentAnnotationClass);
    }

    @NotNull
    @Override
    public List<? super PsiElement> process(@NotNull PsiClass psiClass, @Nullable String nameHint) {
        List<? super PsiElement> result = new ArrayList<>();
        for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, getSupportedAnnotationClasses());
            if (null != psiAnnotation) {
                if (possibleToGenerateElementNamed(nameHint, psiClass, psiAnnotation, psiField)
                    && validate(psiAnnotation, psiField, ProblemEmptyBuilder.getInstance())) {

                    generatePsiElements(psiField, psiAnnotation, result);
                }
            }
        }
        return result;
    }

    protected boolean possibleToGenerateElementNamed(@Nullable String nameHint, @NotNull PsiClass psiClass,
                                                     @NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField) {
        return true;
    }

    @NotNull
    @Override
    public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
        List<PsiAnnotation> result = new ArrayList<>();
        for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, getSupportedAnnotationClasses());
            if (null != psiAnnotation) {
                result.add(psiAnnotation);
            }
        }
        return result;
    }

    @NotNull
    @Override
    public Collection<FlexerProblem> verifyAnnotation(@NotNull PsiAnnotation psiAnnotation) {
        Collection<FlexerProblem> result = Collections.emptyList();

        PsiField psiField = PsiTreeUtil.getParentOfType(psiAnnotation, PsiField.class);
        if (null != psiField) {
            ProblemNewBuilder problemNewBuilder = new ProblemNewBuilder();
            validate(psiAnnotation, psiField, problemNewBuilder);
            result = problemNewBuilder.getProblems();
        }

        return result;
    }

    protected abstract boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField, @NotNull ProblemBuilder builder);

    protected void validateOnXAnnotations(@NotNull PsiAnnotation psiAnnotation,
                                          @NotNull PsiField psiField,
                                          @NotNull ProblemBuilder builder,
                                          @NotNull String parameterName) {
        final List<String> copyableAnnotations = copyableAnnotations(psiField, Utils.BASE_COPYABLE_ANNOTATIONS);

        if (!copyableAnnotations.isEmpty()) {
            final Iterable<String> onXAnnotations = ProcessorUtil.getOnX(psiAnnotation, parameterName);

            for (String copyableAnnotation : copyableAnnotations) {
                for (String onXAnnotation : onXAnnotations) {
                    if (onXAnnotation.startsWith(copyableAnnotation)) {
                        builder.addError(FlexerBundle.message("inspection.message.annotation.copy.duplicate", copyableAnnotation));
                    }
                }
            }
        }

        if (psiField.isDeprecated()) {
            final Iterable<String> onMethodAnnotations = ProcessorUtil.getOnX(psiAnnotation, "onMethod");
            if (StreamSupport.stream(onMethodAnnotations.spliterator(), false).anyMatch(CommonClassNames.JAVA_LANG_DEPRECATED::equals)) {
                builder.addError(FlexerBundle.message("inspection.message.annotation.copy.duplicate", CommonClassNames.JAVA_LANG_DEPRECATED));
            }
        }
    }

    protected abstract void generatePsiElements(@NotNull PsiField psiField,
                                                @NotNull PsiAnnotation psiAnnotation,
                                                @NotNull List<? super PsiElement> target);

}
