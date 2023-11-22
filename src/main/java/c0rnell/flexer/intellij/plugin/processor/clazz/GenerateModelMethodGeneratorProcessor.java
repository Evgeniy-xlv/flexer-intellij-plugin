package c0rnell.flexer.intellij.plugin.processor.clazz;

import c0rnell.flexer.intellij.plugin.AnnotationClassNames;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class GenerateModelMethodGeneratorProcessor extends AbstractClassProcessor {

    public GenerateModelMethodGeneratorProcessor() {
        super(PsiMethod.class, AnnotationClassNames.GENERATE_MODEL);
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        return true;
    }

    @Override
    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
        GenerateModelHandler generateModelHandler = ApplicationManager.getApplication().getService(GenerateModelHandler.class);

        PsiClass innerClass = generateModelHandler.getExistInnerModelClass(psiClass, psiAnnotation).orElse(null);
        if (innerClass == null) {
            // have to create full class (with all methods) here, or auto completion doesn't work
            innerClass = generateModelHandler.createModelInnerClass(psiClass, psiAnnotation);
        }

        // generation of a toModel() method in the annotated class
        target.add(generateModelHandler.createToModelMethodIfNecessary(psiClass, innerClass, psiAnnotation));

        // generation of an all args constructor in the annotated class
        Collection<PsiField> fields = generateModelHandler.collectClassFieldsInternWithIgnore(psiClass);
        PsiMethod constructor = generateModelHandler.createAllArgsConstructorIfNecessary(psiClass, psiAnnotation, fields, true);
        if (constructor != null) {
            target.add(constructor);
        }
    }
}
