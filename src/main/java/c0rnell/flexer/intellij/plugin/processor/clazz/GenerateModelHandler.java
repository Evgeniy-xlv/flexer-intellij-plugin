package c0rnell.flexer.intellij.plugin.processor.clazz;

import c0rnell.flexer.intellij.plugin.AnnotationClassNames;
import c0rnell.flexer.intellij.plugin.psi.FlexerLightClassBuilder;
import c0rnell.flexer.intellij.plugin.psi.FlexerLightFieldBuilder;
import c0rnell.flexer.intellij.plugin.psi.FlexerLightMethodBuilder;
import c0rnell.flexer.intellij.plugin.psi.FlexerLightParameter;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationUtil;
import c0rnell.flexer.intellij.plugin.util.PsiClassUtil;
import c0rnell.flexer.intellij.plugin.util.PsiMethodUtil;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateModelHandler {

    private static final String ANNOTATION_MODEL_CLASS_NAME = "modelClassName";
    private static final String ANNOTATION_TO_MODEL_METHOD_NAME = "toModelMethodName";
    private static final String MODEL_CLASS_NAME = "Model";
    private static final String TO_MODEL_METHOD_NAME = "toModel";

    @NotNull
    public PsiClass createModelInnerClass(@NotNull PsiClass psiClass,
                                          @NotNull PsiAnnotation psiAnnotation) {
        String innerClassName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_MODEL_CLASS_NAME, MODEL_CLASS_NAME);

        FlexerLightClassBuilder innerClass = new FlexerLightClassBuilder(psiClass, innerClassName, psiClass.getQualifiedName() + "." + innerClassName)
                .withContainingClass(psiClass)
                .withNavigationElement(psiAnnotation)
                .withParameterTypes(psiClass.getTypeParameterList())
                .withModifier(PsiModifier.PUBLIC)
                .withModifier(PsiModifier.STATIC);
        innerClass.withFieldSupplier(() -> {
            Collection<PsiField> ownerFields = collectClassFieldsInternWithIgnore(psiClass);
            Collection<PsiField> fields = new ArrayList<>();
            for (PsiField ownerField : ownerFields) {
                fields.add(
                        new FlexerLightFieldBuilder(innerClass.getManager(), ownerField.getName(), ownerField.getType())
                                .withContainingClass(innerClass)
                                .withNavigationElement(psiAnnotation)
                                .withModifier(PsiModifier.PRIVATE)
                                .withModifier(PsiModifier.FINAL)
                );
            }
            return fields;
        });
        innerClass.withMethodSupplier(() -> {
            Collection<PsiMethod> psiMethods = new ArrayList<>();
//            FlexerLightMethodBuilder constructorBuilder = new FlexerLightMethodBuilder(innerClass.getManager(), innerClassName)
//                    .withConstructor(true)
//                    .withContainingClass(innerClass)
//                    .withNavigationElement(psiAnnotation)
//                    .withModifier(PsiModifier.PACKAGE_LOCAL);
//
//            StringBuilder blockText = new StringBuilder();
//            Collection<PsiField> ownerFields = collectClassFieldsInternWithIgnore(psiClass);
//            for (PsiField ownerField : ownerFields) {
//                constructorBuilder.withParameter(
//                        new FlexerLightParameter(ownerField.getName(), ownerField.getType(), constructorBuilder)
//                );
//
//                blockText.append(String.format("this.%s = %s;\n", ownerField.getName(), ownerField.getName()));
//            }
//
//            constructorBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText.toString(), constructorBuilder));
            Collection<PsiField> ownerFields = collectClassFieldsInternWithIgnore(psiClass);
            PsiMethod constructorBuilder = createAllArgsConstructorIfNecessary(innerClass, psiAnnotation, ownerFields, true);
            if (constructorBuilder != null) {
                psiMethods.add(constructorBuilder);
            }

            StringBuilder toStringBlockText = new StringBuilder("return \"{");

            Iterator<PsiField> innerClassFields = collectClassFieldsInternWithIgnore(psiClass).iterator();
            while (innerClassFields.hasNext()) {
                PsiField innerClassField = innerClassFields.next();
                String methodName = String.format("get%s", Strings.capitalize(innerClassField.getName()));
                FlexerLightMethodBuilder methodBuilder = new FlexerLightMethodBuilder(innerClass.getManager(), methodName)
                        .withContainingClass(innerClass)
                        .withNavigationElement(psiAnnotation)
                        .withMethodReturnType(innerClassField.getType())
                        .withModifier(PsiModifier.PUBLIC);

                String blockText = String.format("return this.%s;", innerClassField.getName());
                methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText, methodBuilder));

                psiMethods.add(methodBuilder);

                toStringBlockText.append('\"').append(innerClassField.getName()).append("\": ")
                        .append("this.").append(innerClassField.getName());
                if (innerClassFields.hasNext()) {
                    toStringBlockText.append(", ");
                }
            }
            toStringBlockText.append("}\";");

            FlexerLightMethodBuilder toStringBuilder = new FlexerLightMethodBuilder(innerClass.getManager(), "toString")
                    .withContainingClass(innerClass)
                    .withNavigationElement(psiAnnotation)
                    .withMethodReturnType(PsiType.getJavaLangString(innerClass.getManager(), GlobalSearchScope.allScope(psiClass.getProject())))
                    .withModifier(PsiModifier.PUBLIC);
            toStringBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(toStringBlockText.toString(), toStringBuilder));
            psiMethods.add(toStringBuilder);

            return psiMethods;
        });
        return innerClass;
    }

    @Nullable
    public PsiMethod createToModelMethodIfNecessary(@NotNull PsiClass psiClass,
                                                    @NotNull PsiClass psiModelClass,
                                                    @NotNull PsiAnnotation psiAnnotation) {
        String methodName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_TO_MODEL_METHOD_NAME, TO_MODEL_METHOD_NAME);

        if (hasMethod(psiClass, methodName)) {
            return null;
        }

        PsiType psiTypeWithGenerics = PsiClassUtil.getTypeWithGenerics(psiModelClass);

        FlexerLightMethodBuilder methodBuilder = new FlexerLightMethodBuilder(psiClass.getManager(), methodName)
                .withMethodReturnType(psiTypeWithGenerics)
                .withContainingClass(psiClass)
                .withNavigationElement(psiAnnotation)
                .withModifier(PsiModifier.PUBLIC);
//        String blockText = String.format("return new %s();", psiTypeWithGenerics.getPresentableText());

        StringBuilder blockText = new StringBuilder("return new %s(");
        Iterator<PsiField> ownerFields = collectClassFieldsInternWithIgnore(psiClass).iterator();
        while (ownerFields.hasNext()) {
            PsiField next = ownerFields.next();
            blockText.append(String.format("this.%s", next.getName()));
            if (ownerFields.hasNext()) {
                blockText.append(", ");
            } else {
                blockText.append(");");
            }
        }

        methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText.toString(), methodBuilder));

        return methodBuilder;
    }

    @Nullable
    public PsiMethod createAllArgsConstructorIfNecessary(@NotNull PsiClass targetPsiClass,
                                                         @NotNull PsiAnnotation psiAnnotation,
                                                         @NotNull Collection<PsiField> fields,
                                                         boolean ignoreDefaultConstructor) {
        Collection<PsiMethod> constructors = PsiClassUtil.collectClassConstructorIntern(targetPsiClass);
        for (PsiMethod constructor : constructors) {
            if (!constructor.getParameterList().isEmpty() || !ignoreDefaultConstructor) {
                return null;
            }
        }

        String methodName = targetPsiClass.getName();
        FlexerLightMethodBuilder constructorBuilder = new FlexerLightMethodBuilder(targetPsiClass.getManager(), methodName)
                .withConstructor(true)
                .withContainingClass(targetPsiClass)
                .withNavigationElement(psiAnnotation)
                .withModifier(PsiModifier.PACKAGE_LOCAL);

        StringBuilder blockText = new StringBuilder();
        for (PsiField ownerField : fields) {
            constructorBuilder.withParameter(
                    new FlexerLightParameter(ownerField.getName(), ownerField.getType(), constructorBuilder)
            );

            blockText.append(String.format("this.%s = %s;\n", ownerField.getName(), ownerField.getName()));
        }

        constructorBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(blockText.toString(), constructorBuilder));
        return constructorBuilder;
    }

    public Collection<PsiField> collectClassFieldsInternWithIgnore(@NotNull PsiClass psiClass) {
        return PsiClassUtil.collectClassFieldsIntern(psiClass).stream()
                .filter(psiField -> !PsiAnnotationSearchUtil.isAnnotatedWith(psiField, AnnotationClassNames.GENERATE_MODEL_IGNORE))
                .collect(Collectors.toList());
    }

    public Optional<PsiClass> getExistInnerModelClass(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        String innerClassName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, ANNOTATION_MODEL_CLASS_NAME, MODEL_CLASS_NAME);
        return PsiClassUtil.getInnerClassInternByName(psiClass, innerClassName);
    }

    boolean hasMethod(@NotNull PsiClass psiClass, @NotNull String methodName) {
        Collection<PsiMethod> existingMethods = PsiClassUtil.collectClassStaticMethodsIntern(psiClass);
        return existingMethods.stream()
                .map(PsiMethod::getName)
                .anyMatch(methodName::equals);
    }
}
