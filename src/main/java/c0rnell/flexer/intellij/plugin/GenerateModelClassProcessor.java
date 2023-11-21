//package com.dguner.lombokbuilderhelper;
//
//import com.dguner.lombokbuilderhelper.util.LombokProcessorUtil;
//import com.dguner.lombokbuilderhelper.psi.ProblemBuilder;
//import com.dguner.lombokbuilderhelper.psi.ProblemEmptyBuilder;
//import com.dguner.lombokbuilderhelper.util.PsiAnnotationSearchUtil;
//import com.dguner.lombokbuilderhelper.util.PsiAnnotationUtil;
//import com.dguner.lombokbuilderhelper.util.PsiClassUtil;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiMethod;
//import com.intellij.psi.PsiModifier;
//import com.intellij.psi.PsiModifierList;
//import com.intellij.psi.PsiType;
//import com.intellij.psi.augment.PsiAugmentProvider;
//import com.intellij.psi.impl.source.PsiExtensibleClass;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//public class GenerateModelClassProcessor extends PsiAugmentProvider {
//
//    @Override
//    protected @NotNull <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type, @Nullable String nameHint) {
//
//        final List<Psi> emptyResult = Collections.emptyList();
//        if (type == PsiClass.class || element instanceof PsiExtensibleClass) {
//
//            final PsiClass psiClass = (PsiClass) element;
//
//            if (psiClass.isAnnotationType() || psiClass.isInterface()) {
//                return emptyResult;
//            }
//
//            // skip if not flexer library linked
//
//            return getPsis(psiClass, type, nameHint);
//        }
//
//        return emptyResult;
//    }
//
//    public final @NotNull String @NotNull [] getSupportedAnnotationClasses() {
//        return new String[]{"c0rnell.flexer.dto.GenerateDto"};
//    }
//
//    @NotNull
//    private <Psi extends PsiElement> List<Psi> getPsis(PsiClass psiClass, Class<Psi> type, String nameHint) {
//        final List<Psi> result = new ArrayList<>();
//
//        PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiClass, getSupportedAnnotationClasses());
//        if (null != psiAnnotation
//            && supportAnnotationVariant(psiAnnotation)
//            && possibleToGenerateElementNamed(nameHint, psiClass, psiAnnotation)
//            && validate(psiAnnotation, psiClass, ProblemEmptyBuilder.getInstance())
//        ) {
//            generatePsiElements(psiClass, psiAnnotation, result);
//        }
//
//        return result;
//    }
//
//    protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
//        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
//        if (methodVisibility != null) {
//            target.addAll(createFieldGetters(psiClass, methodVisibility));
//        }
//    }
//
//    public Collection<PsiMethod> createFieldGetters(@NotNull PsiClass psiClass, @NotNull String methodModifier) {
//        Collection<PsiMethod> result = new ArrayList<>();
//        final Collection<PsiField> getterFields = filterGetterFields(psiClass);
//        GenerateModelFieldProcessor fieldProcessor = getGetterFieldProcessor();
//        for (PsiField getterField : getterFields) {
//            result.add(fieldProcessor.createGetterMethod(getterField, psiClass, methodModifier));
//        }
//        return result;
//    }
//
//    private Collection<PsiField> filterGetterFields(@NotNull PsiClass psiClass) {
//        final Collection<PsiField> getterFields = new ArrayList<>();
//
//        final Collection<PsiMethod> classMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
////        filterToleratedElements(classMethods);
//
//        GenerateModelFieldProcessor fieldProcessor = getGetterFieldProcessor();
//        for (PsiField psiField : psiClass.getFields()) {
//            boolean createGetter = true;
//            PsiModifierList modifierList = psiField.getModifierList();
//            if (null != modifierList) {
//                //Skip static fields.
//                createGetter = !modifierList.hasModifierProperty(PsiModifier.STATIC);
//                //Skip fields having Getter annotation already
//                createGetter &= PsiAnnotationSearchUtil.isNotAnnotatedWith(psiField, fieldProcessor.getSupportedAnnotationClasses());
//                //Skip fields that start with $
//                createGetter &= !psiField.getName().startsWith(LombokUtils.LOMBOK_INTERN_FIELD_MARKER);
//                //Skip fields if a method with same name and arguments count already exists
//                final AccessorsInfo accessorsInfo = AccessorsInfo.build(psiField);
//                final Collection<String> methodNames = LombokUtils.toAllGetterNames(accessorsInfo, psiField.getName(), PsiType.BOOLEAN.equals(psiField.getType()));
//                for (String methodName : methodNames) {
//                    createGetter &= !PsiMethodUtil.hasSimilarMethod(classMethods, methodName, 0);
//                }
//            }
//
//            if (createGetter) {
//                getterFields.add(psiField);
//            }
//        }
//        return getterFields;
//    }
//
//    private GenerateModelFieldProcessor getGetterFieldProcessor() {
//        return ApplicationManager.getApplication().getService(GenerateModelFieldProcessor.class);
//    }
//
//    protected boolean supportAnnotationVariant(@NotNull PsiAnnotation psiAnnotation) {
//        return true;
//    }
//
//    protected boolean possibleToGenerateElementNamed(@Nullable String nameHint, @NotNull PsiClass psiClass,
//                                                     @NotNull PsiAnnotation psiAnnotation) {
//        return true;
//    }
//
//    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
//        final boolean result = validateAnnotationOnRightType(psiClass, builder) && validateVisibility(psiAnnotation);
//
//        if (PsiAnnotationUtil.getBooleanAnnotationValue(psiAnnotation, "lazy", false)) {
//            builder.addWarning("inspection.message.lazy.not.supported.for.getter.on.type");
//        }
//
//        return result;
//    }
//
//    private boolean validateAnnotationOnRightType(@NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
//        boolean result = true;
//        if (psiClass.isAnnotationType() || psiClass.isInterface()) {
//            builder.addError("inspection.message.getter.only.supported.on.class.enum.or.field.type");
//            result = false;
//        }
//        return result;
//    }
//
//    private boolean validateVisibility(@NotNull PsiAnnotation psiAnnotation) {
//        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
//        return null != methodVisibility;
//    }
//}
