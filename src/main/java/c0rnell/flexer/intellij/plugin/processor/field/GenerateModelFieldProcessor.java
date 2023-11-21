package c0rnell.flexer.intellij.plugin.processor.field;

import c0rnell.flexer.intellij.plugin.AnnotationClassNames;
import c0rnell.flexer.intellij.plugin.LombokBundle;
import c0rnell.flexer.intellij.plugin.problem.ProblemBuilder;
import c0rnell.flexer.intellij.plugin.processor.LombokPsiElementUsage;
import c0rnell.flexer.intellij.plugin.psi.LombokLightMethodBuilder;
import c0rnell.flexer.intellij.plugin.quickfix.PsiQuickFixFactory;
import c0rnell.flexer.intellij.plugin.util.LombokProcessorUtil;
import c0rnell.flexer.intellij.plugin.util.LombokUtils;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import c0rnell.flexer.intellij.plugin.util.PsiMethodUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class GenerateModelFieldProcessor extends AbstractFieldProcessor {

    public GenerateModelFieldProcessor() {
        super(PsiMethod.class, AnnotationClassNames.GENERATE_MODEL);
    }

    @Override
    protected void generatePsiElements(@NotNull PsiField psiField,
                                       @NotNull PsiAnnotation psiAnnotation,
                                       @NotNull List<? super PsiElement> target) {
        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
        final PsiClass psiClass = psiField.getContainingClass();
        if (null != methodVisibility && null != psiClass) {
            target.add(createGetterMethod(psiField, psiClass, methodVisibility));
        }
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField, @NotNull ProblemBuilder builder) {
        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
        boolean result = null != methodVisibility;

        final boolean lazy = isLazyGetter(psiAnnotation);
        if (null == methodVisibility && lazy) {
            builder.addWarning(LombokBundle.message("inspection.message.lazy.does.not.work.with.access.level.none"));
        }

        if (result && lazy) {
            if (!psiField.hasModifierProperty(PsiModifier.FINAL) || !psiField.hasModifierProperty(PsiModifier.PRIVATE)) {
                builder.addError(LombokBundle.message("inspection.message.lazy.requires.field.to.be.private.final"),
                        PsiQuickFixFactory.createModifierListFix(psiField, PsiModifier.PRIVATE, true, false),
                        PsiQuickFixFactory.createModifierListFix(psiField, PsiModifier.FINAL, true, false));
                result = false;
            }
            if (!psiField.hasInitializer()) {
                builder.addError(LombokBundle.message("inspection.message.lazy.requires.field.initialization"));
                result = false;
            }
        }

        validateOnXAnnotations(psiAnnotation, psiField, builder, "onMethod");

        if (result) {
            result = validateExistingMethods(psiField, builder);
        }

        if (result) {
            result = validateAccessorPrefix(psiField, builder);
        }

        return result;
    }

    private boolean isLazyGetter(@NotNull PsiAnnotation psiAnnotation) {
        return PsiAnnotationUtil.getBooleanAnnotationValue(psiAnnotation, "lazy", false);
    }

    private boolean validateExistingMethods(@NotNull PsiField psiField, @NotNull ProblemBuilder builder) {
        boolean result = true;
        final PsiClass psiClass = psiField.getContainingClass();
        if (null != psiClass) {
            final boolean isBoolean = PsiType.BOOLEAN.equals(psiField.getType());
            final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);
            final Collection<String> methodNames = LombokUtils.toAllGetterNames(accessorsInfo, psiField.getName(), isBoolean);
            final Collection<PsiMethod> classMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
//            filterToleratedElements(classMethods);

            for (String methodName : methodNames) {
                if (PsiMethodUtil.hasSimilarMethod(classMethods, methodName, 0)) {
                    final String setterMethodName = LombokUtils.getGetterName(psiField);

                    builder.addWarning(LombokBundle.message("inspection.message.not.generated.s.method.with.similar.name.s.already.exists"),
                            setterMethodName, methodName);
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean validateAccessorPrefix(@NotNull PsiField psiField, @NotNull ProblemBuilder builder) {
        boolean result = true;
        if (AccessorsInfo.build(psiField).isPrefixUnDefinedOrNotStartsWith(psiField.getName())) {
            builder.addWarning(LombokBundle.message("inspection.message.not.generating.getter.for.this.field"));
            result = false;
        }
        return result;
    }

    @NotNull
    public PsiMethod createGetterMethod(@NotNull PsiField psiField, @NotNull PsiClass psiClass, @NotNull String methodModifier) {
        final String methodName = LombokUtils.getGetterName(psiField);

        LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(psiField.getManager(), methodName)
                .withMethodReturnType(psiField.getType())
                .withContainingClass(psiClass)
                .withNavigationElement(psiField);
        if (StringUtil.isNotEmpty(methodModifier)) {
            methodBuilder.withModifier(methodModifier);
        }
        boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
        if (isStatic) {
            methodBuilder.withModifier(PsiModifier.STATIC);
        }

        final String blockText = String.format("return %s.%s;", isStatic ? psiClass.getName() : "this", psiField.getName());
        methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText, methodBuilder));

        final PsiModifierList modifierList = methodBuilder.getModifierList();

        copyCopyableAnnotations(psiField, modifierList, LombokUtils.BASE_COPYABLE_ANNOTATIONS);
        PsiAnnotation fieldGetterAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, AnnotationClassNames.GENERATE_MODEL);
        copyOnXAnnotations(fieldGetterAnnotation, modifierList, "onMethod");
        if (psiField.isDeprecated()) {
            modifierList.addAnnotation(CommonClassNames.JAVA_LANG_DEPRECATED);
        }

        return methodBuilder;
    }

    @Override
    public LombokPsiElementUsage checkFieldUsage(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation) {
        return LombokPsiElementUsage.READ;
    }
}
