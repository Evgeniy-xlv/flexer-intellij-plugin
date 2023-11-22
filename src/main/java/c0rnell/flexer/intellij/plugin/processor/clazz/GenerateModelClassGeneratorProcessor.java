package c0rnell.flexer.intellij.plugin.processor.clazz;

import c0rnell.flexer.intellij.plugin.AnnotationClassNames;
import c0rnell.flexer.intellij.plugin.LombokBundle;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import c0rnell.flexer.intellij.plugin.processor.LombokPsiElementUsage;
import c0rnell.flexer.intellij.plugin.processor.field.AccessorsInfo;
import c0rnell.flexer.intellij.plugin.processor.field.GenerateModelFieldProcessor;
import c0rnell.flexer.intellij.plugin.util.LombokProcessorUtil;
import c0rnell.flexer.intellij.plugin.util.LombokUtils;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import c0rnell.flexer.intellij.plugin.util.PsiMethodUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GenerateModelClassGeneratorProcessor extends AbstractClassProcessor {

    public GenerateModelClassGeneratorProcessor() {
        super(PsiClass.class, AnnotationClassNames.GENERATE_MODEL);
    }

    private GenerateModelFieldProcessor getGetterFieldProcessor() {
        return ApplicationManager.getApplication().getService(GenerateModelFieldProcessor.class);
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        final boolean result = validateAnnotationOnRightType(psiClass, builder) && validateVisibility(psiAnnotation);

        if (PsiAnnotationUtil.getBooleanAnnotationValue(psiAnnotation, "lazy", false)) {
            builder.addWarning(LombokBundle.message("inspection.message.lazy.not.supported.for.getter.on.type"));
        }

        return result;
    }

    private boolean validateAnnotationOnRightType(@NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        boolean result = true;
        if (psiClass.isAnnotationType() || psiClass.isInterface()) {
            builder.addError(LombokBundle.message("inspection.message.getter.only.supported.on.class.enum.or.field.type"));
            result = false;
        }
        return result;
    }

    private boolean validateVisibility(@NotNull PsiAnnotation psiAnnotation) {
        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
        return null != methodVisibility;
    }

    @Override
    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
        if (methodVisibility != null) {
//            target.addAll(createFieldGetters(psiClass, methodVisibility));
            GenerateModelHandler generateModelHandler = ApplicationManager.getApplication().getService(GenerateModelHandler.class);

            PsiClass innerClass = generateModelHandler.createModelInnerClass(psiClass, psiAnnotation);
            target.add(innerClass);
        }
    }

    @NotNull
    public Collection<PsiMethod> createFieldGetters(@NotNull PsiClass psiClass, @NotNull String methodModifier) {
        Collection<PsiMethod> result = new ArrayList<>();
        final Collection<PsiField> getterFields = filterGetterFields(psiClass);
        GenerateModelFieldProcessor fieldProcessor = getGetterFieldProcessor();
        for (PsiField getterField : getterFields) {
            result.add(fieldProcessor.createGetterMethod(getterField, psiClass, methodModifier));
        }
        return result;
    }

    @NotNull
    private Collection<PsiField> filterGetterFields(@NotNull PsiClass psiClass) {
        final Collection<PsiField> getterFields = new ArrayList<>();

        final Collection<PsiMethod> classMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
//        filterToleratedElements(classMethods);

        GenerateModelFieldProcessor fieldProcessor = getGetterFieldProcessor();
        for (PsiField psiField : psiClass.getFields()) {
            boolean createGetter = true;
            PsiModifierList modifierList = psiField.getModifierList();
            if (null != modifierList) {
                //Skip static fields.
                createGetter = !modifierList.hasModifierProperty(PsiModifier.STATIC);
                //Skip fields having Getter annotation already
                createGetter &= PsiAnnotationSearchUtil.isNotAnnotatedWith(psiField, fieldProcessor.getSupportedAnnotationClasses());
                //Skip fields that start with $
                createGetter &= !psiField.getName().startsWith(LombokUtils.LOMBOK_INTERN_FIELD_MARKER);
                //Skip fields if a method with same name and arguments count already exists
                final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);
                final Collection<String> methodNames = LombokUtils.toAllGetterNames(accessorsInfo, psiField.getName(), PsiType.BOOLEAN.equals(psiField.getType()));
                for (String methodName : methodNames) {
                    createGetter &= !PsiMethodUtil.hasSimilarMethod(classMethods, methodName, 0);
                }
            }

            if (createGetter) {
                getterFields.add(psiField);
            }
        }
        return getterFields;
    }

    @Override
    public LombokPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation) {
        final PsiClass containingClass = psiField.getContainingClass();
        if (null != containingClass) {
            if (PsiClassUtil.getNames(filterGetterFields(containingClass)).contains(psiField.getName())) {
                return LombokPsiElementUsage.READ;
            }
        }
        return LombokPsiElementUsage.NONE;
    }
}
