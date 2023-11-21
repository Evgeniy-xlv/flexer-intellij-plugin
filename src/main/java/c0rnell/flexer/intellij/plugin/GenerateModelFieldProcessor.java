//package com.dguner.lombokbuilderhelper;
//
//import com.dguner.lombokbuilderhelper.psi.LombokLightMethodBuilder;
//import com.dguner.lombokbuilderhelper.util.LombokProcessorUtil;
//import com.dguner.lombokbuilderhelper.util.LombokUtils;
//import com.dguner.lombokbuilderhelper.psi.ProblemEmptyBuilder;
//import com.dguner.lombokbuilderhelper.util.PsiAnnotationSearchUtil;
//import com.dguner.lombokbuilderhelper.util.PsiClassUtil;
//import com.dguner.lombokbuilderhelper.util.PsiMethodUtil;
//import com.intellij.codeInsight.AnnotationUtil;
//import com.intellij.codeInsight.intention.AddAnnotationPsiFix;
//import com.intellij.openapi.util.text.StringUtil;
//import com.intellij.psi.CommonClassNames;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiMethod;
//import com.intellij.psi.PsiModifier;
//import com.intellij.psi.PsiModifierList;
//import com.intellij.psi.PsiNameValuePair;
//import com.intellij.psi.augment.PsiAugmentProvider;
//import com.intellij.psi.impl.source.PsiExtensibleClass;
//import com.intellij.util.containers.ContainerUtil;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class GenerateModelFieldProcessor extends PsiAugmentProvider {
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
//        for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
//            PsiAnnotation psiAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, getSupportedAnnotationClasses());
//            if (null != psiAnnotation) {
//                if (possibleToGenerateElementNamed(nameHint, psiClass, psiAnnotation, psiField)
//                    && validate(psiAnnotation, psiField, ProblemEmptyBuilder.getInstance())) {
//
//                    generatePsiElementsForField(psiField, psiAnnotation, result);
//                }
//            }
//        }
//
//        return result;
//    }
//
//    private <Psi extends PsiElement> void generatePsiElementsForField(PsiField psiField, PsiAnnotation psiAnnotation, List<Psi> result) {
//        final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
//        final PsiClass psiClass = psiField.getContainingClass();
//        if (null != methodVisibility && null != psiClass) {
//            result.add((Psi) createGetterMethod(psiField, psiClass, methodVisibility));
//        }
//    }
//
//    private boolean validate(PsiAnnotation psiAnnotation, PsiField psiField, ProblemEmptyBuilder instance) {
//        return true;
//    }
//
//    private boolean possibleToGenerateElementNamed(String nameHint, PsiClass psiClass, PsiAnnotation psiAnnotation, PsiField psiField) {
//        return true;
//    }
//
//    public PsiMethod createGetterMethod(@NotNull PsiField psiField, @NotNull PsiClass psiClass, @NotNull String methodModifier) {
//        final String methodName = LombokUtils.getGetterName(psiField);
//
//        LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(psiField.getManager(), methodName)
//                .withMethodReturnType(psiField.getType())
//                .withContainingClass(psiClass)
//                .withNavigationElement(psiField);
//        if (StringUtil.isNotEmpty(methodModifier)) {
//            methodBuilder.withModifier(methodModifier);
//        }
//        boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
//        if (isStatic) {
//            methodBuilder.withModifier(PsiModifier.STATIC);
//        }
//
//        final String blockText = String.format("return %s.%s;", isStatic ? psiClass.getName() : "this", psiField.getName());
//        methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText, methodBuilder));
//
//        final PsiModifierList modifierList = methodBuilder.getModifierList();
//
//        copyCopyableAnnotations(psiField, modifierList, LombokUtils.BASE_COPYABLE_ANNOTATIONS);
//        PsiAnnotation fieldGetterAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, "c0rnell.flexer.dto.GenerateDto");
//        copyOnXAnnotations(fieldGetterAnnotation, modifierList, "onMethod");
//        if (psiField.isDeprecated()) {
//            modifierList.addAnnotation(CommonClassNames.JAVA_LANG_DEPRECATED);
//        }
//
//        return methodBuilder;
//    }
//
//    protected static void copyOnXAnnotations(@Nullable PsiAnnotation processedAnnotation,
//                                             @NotNull PsiModifierList modifierList,
//                                             @NotNull String onXParameterName) {
//        if (processedAnnotation == null) {
//            return;
//        }
//
//        Iterable<String> annotationsToAdd = LombokProcessorUtil.getOnX(processedAnnotation, onXParameterName);
//        annotationsToAdd.forEach(modifierList::addAnnotation);
//    }
//
//
//    protected static List<String> copyableAnnotations(@NotNull PsiField psiField, final List<String> copyableAnnotations) {
//        final List<String> combinedListOfCopyableAnnotations = new ArrayList<>(copyableAnnotations);
//
////        final PsiClass containingClass = psiField.getContainingClass();
//        // append only for BASE_COPYABLE
////        if (copyableAnnotations == LombokUtils.BASE_COPYABLE_ANNOTATIONS && null != containingClass) {
////            String[] configuredCopyableAnnotations =
////                    ConfigDiscovery.getInstance().getMultipleValueLombokConfigProperty(ConfigKey.COPYABLE_ANNOTATIONS, containingClass);
////            combinedListOfCopyableAnnotations.addAll(Arrays.asList(configuredCopyableAnnotations));
////        }
//
//        final List<String> existingAnnotations = ContainerUtil.map(psiField.getAnnotations(), PsiAnnotation::getQualifiedName);
//        existingAnnotations.retainAll(combinedListOfCopyableAnnotations);
//
//        return existingAnnotations;
//    }
//
//    protected static void copyCopyableAnnotations(@NotNull PsiField fromPsiElement,
//                                                  @NotNull PsiModifierList toModifierList,
//                                                  List<String> copyableAnnotations) {
//        List<String> existingAnnotations = copyableAnnotations(fromPsiElement, copyableAnnotations);
//
//        for (String annotation : existingAnnotations) {
//            PsiAnnotation srcAnnotation = AnnotationUtil.findAnnotation(fromPsiElement, annotation);
//            PsiNameValuePair[] valuePairs =
//                    srcAnnotation != null ? srcAnnotation.getParameterList().getAttributes() : PsiNameValuePair.EMPTY_ARRAY;
//            AddAnnotationPsiFix.addPhysicalAnnotationIfAbsent(annotation, valuePairs, toModifierList);
//        }
//    }
//}
