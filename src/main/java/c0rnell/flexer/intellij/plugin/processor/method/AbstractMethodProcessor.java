package c0rnell.flexer.intellij.plugin.processor.method;

import c0rnell.flexer.intellij.plugin.problem.FlexerProblem;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemEmptyBuilder;
import c0rnell.flexer.intellij.plugin.problem.ProblemNewBuilder;
import c0rnell.flexer.intellij.plugin.processor.AbstractProcessor;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base flexer processor class for method annotations
 */
public abstract class AbstractMethodProcessor extends AbstractProcessor implements MethodProcessor {

    AbstractMethodProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                            @NotNull String supportedAnnotationClass) {
        super(supportedClass, supportedAnnotationClass);
    }

    AbstractMethodProcessor(@NotNull Class<? extends PsiElement> supportedClass,
                            @NotNull String supportedAnnotationClass,
                            @NotNull String equivalentAnnotationClass) {
        super(supportedClass, supportedAnnotationClass, equivalentAnnotationClass);
    }

    @NotNull
    @Override
    public List<? super PsiElement> process(@NotNull PsiClass psiClass, @Nullable String nameHint) {
        List<? super PsiElement> result = new ArrayList<>();
        for (PsiMethod psiMethod : PsiClassUtil.collectClassMethodsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiMethod, getSupportedAnnotationClasses());
            if (null != psiAnnotation) {
                if (possibleToGenerateElementNamed(nameHint, psiClass, psiAnnotation, psiMethod) &&
                    validate(psiAnnotation, psiMethod, ProblemEmptyBuilder.getInstance())) {
                    processIntern(psiMethod, psiAnnotation, result);
                }
            }
        }
        return result;
    }

    protected boolean possibleToGenerateElementNamed(@Nullable String nameHint, @NotNull PsiClass psiClass,
                                                     @NotNull PsiAnnotation psiAnnotation, @NotNull PsiMethod psiMethod) {
        return true;
    }

    @NotNull
    @Override
    public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
        List<PsiAnnotation> result = new ArrayList<>();
        for (PsiMethod psiMethod : PsiClassUtil.collectClassMethodsIntern(psiClass)) {
            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiMethod, getSupportedAnnotationClasses());
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

        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiAnnotation, PsiMethod.class);
        if (null != psiMethod) {
            ProblemNewBuilder problemNewBuilder = new ProblemNewBuilder();
            validate(psiAnnotation, psiMethod, problemNewBuilder);
            result = problemNewBuilder.getProblems();
        }

        return result;
    }

    protected abstract boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiMethod psiMethod, @NotNull ProblemBuilder builder);

    protected abstract void processIntern(PsiMethod psiMethod, PsiAnnotation psiAnnotation, List<? super PsiElement> target);
}
