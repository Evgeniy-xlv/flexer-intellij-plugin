package c0rnell.flexer.intellij.plugin.processor.field;

import c0rnell.flexer.intellij.plugin.AnnotationClassNames;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationSearchUtil;
import c0rnell.flexer.intellij.plugin.util.PsiAnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class AccessorsInfo {

    public static final AccessorsInfo EMPTY = new AccessorsInfo(false, false, false);

    private final boolean fluent;
    private final boolean chain;
    private final String[] prefixes;
    private final boolean doNotUseIsPrefix;

    private AccessorsInfo(boolean fluentValue, boolean chainValue, boolean doNotUseIsPrefix, String... prefixes) {
        this.fluent = fluentValue;
        this.chain = chainValue;
        this.doNotUseIsPrefix = doNotUseIsPrefix;
        this.prefixes = null == prefixes ? new String[0] : prefixes;
    }

    @NotNull
    public static AccessorsInfo build(boolean fluentValue, boolean chainValue, boolean doNotUseIsPrefix, String... prefixes) {
        return new AccessorsInfo(fluentValue, chainValue, doNotUseIsPrefix, prefixes);
    }

    @NotNull
    public static AccessorsInfo build(@NotNull PsiField psiField) {
        return build(psiField, psiField.getContainingClass());
    }

    @NotNull
    public static AccessorsInfo build(@NotNull PsiVariable psiVariable, @Nullable PsiClass containingClass) {
        final PsiAnnotation accessorsFieldAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiVariable, AnnotationClassNames.ACCESSORS);
        if (null != accessorsFieldAnnotation) {
            return buildFromAnnotation(accessorsFieldAnnotation, containingClass);
        } else {
            return build(containingClass);
        }
    }

    @NotNull
    public static AccessorsInfo build(@NotNull PsiField psiField, @NotNull AccessorsInfo classAccessorsInfo) {
        final PsiAnnotation accessorsFieldAnnotation = PsiAnnotationSearchUtil.findAnnotation(psiField, AnnotationClassNames.ACCESSORS);
        if (null != accessorsFieldAnnotation) {
            return buildFromAnnotation(accessorsFieldAnnotation, psiField.getContainingClass());
        } else {
            return classAccessorsInfo;
        }
    }

    @NotNull
    public static AccessorsInfo build(@Nullable PsiClass psiClass) {
        PsiClass containingClass = psiClass;
        while (null != containingClass) {
            final PsiAnnotation accessorsClassAnnotation = PsiAnnotationSearchUtil.findAnnotation(containingClass, AnnotationClassNames.ACCESSORS);
            if (null != accessorsClassAnnotation) {
                return buildFromAnnotation(accessorsClassAnnotation, containingClass);
            }
            containingClass = containingClass.getContainingClass();
        }

        return buildAccessorsInfo(psiClass, null, null, Collections.emptySet());
    }

    @NotNull
    private static AccessorsInfo buildFromAnnotation(@NotNull PsiAnnotation accessorsAnnotation, @Nullable PsiClass psiClass) {
        Boolean chainDeclaredValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(accessorsAnnotation, "chain");
        Boolean fluentDeclaredValue = PsiAnnotationUtil.getDeclaredBooleanAnnotationValue(accessorsAnnotation, "fluent");
        Collection<String> prefixes = PsiAnnotationUtil.getAnnotationValues(accessorsAnnotation, "prefix", String.class);

        return buildAccessorsInfo(psiClass, chainDeclaredValue, fluentDeclaredValue, prefixes);
    }

    @NotNull
    private static AccessorsInfo buildAccessorsInfo(@Nullable PsiClass psiClass, @Nullable Boolean chainDeclaredValue,
                                                    @Nullable Boolean fluentDeclaredValue, @NotNull Collection<String> prefixDeclared) {
        boolean isFluent = false;
        boolean isChained = false;
        boolean doNotUseIsPrefix = false;
        String[] prefixes = {};

        if (null != psiClass) {
//            final ConfigDiscovery configDiscovery = ConfigDiscovery.getInstance();
            if (null == fluentDeclaredValue) {
//                isFluent = configDiscovery.getBooleanFlexerConfigProperty(ConfigKey.ACCESSORS_FLUENT, psiClass);
            } else {
                isFluent = fluentDeclaredValue;
            }

            if (null == chainDeclaredValue) {
//                isChained = configDiscovery.getBooleanFlexerConfigProperty(ConfigKey.ACCESSORS_CHAIN, psiClass);
            } else {
                isChained = chainDeclaredValue;
            }

            if (prefixDeclared.isEmpty()) {
//                prefixes = configDiscovery.getMultipleValueFlexerConfigProperty(ConfigKey.ACCESSORS_PREFIX, psiClass);
            } else {
                prefixes = prefixDeclared.toArray(new String[0]);
            }

//            doNotUseIsPrefix = configDiscovery.getBooleanFlexerConfigProperty(ConfigKey.GETTER_NO_IS_PREFIX, psiClass);

        } else {
            isFluent = null != fluentDeclaredValue && fluentDeclaredValue;
            isChained = null != chainDeclaredValue && chainDeclaredValue;
            prefixes = prefixDeclared.toArray(new String[0]);
            doNotUseIsPrefix = false;
        }

        boolean isChainDeclaredOrImplicit = isChained || (isFluent && null == chainDeclaredValue);
        return new AccessorsInfo(isFluent, isChainDeclaredOrImplicit, doNotUseIsPrefix, prefixes);
    }

    public boolean isFluent() {
        return fluent;
    }

    public AccessorsInfo withFluent(boolean fluentValue) {
        if (fluent == fluentValue) {
            return this;
        }
        return new AccessorsInfo(fluentValue, chain, doNotUseIsPrefix, prefixes);
    }

    public boolean isChain() {
        return chain;
    }

    public boolean isDoNotUseIsPrefix() {
        return doNotUseIsPrefix;
    }

    public String[] getPrefixes() {
        return prefixes;
    }

    public boolean isPrefixUnDefinedOrNotStartsWith(String fieldName) {
        if (prefixes.length == 0) {
            return false;
        }

        for (String prefix : prefixes) {
            if (canPrefixApply(fieldName, prefix)) {
                return false;
            }
        }
        return true;
    }

    public String removePrefix(String fieldName) {
        for (String prefix : prefixes) {
            if (canPrefixApply(fieldName, prefix)) {
                return prefix.isEmpty() ? fieldName : decapitalizeFirstChar(fieldName.substring(prefix.length()));
            }
        }
        return fieldName;
    }

    private boolean canPrefixApply(String fieldName, String prefix) {
        final int prefixLength = prefix.length();
        // we can use digits and upper case letters after a prefix, but not lower case letters
        return prefixLength == 0 ||
               fieldName.startsWith(prefix) && fieldName.length() > prefixLength &&
               (!Character.isLetter(prefix.charAt(prefix.length() - 1)) || !Character.isLowerCase(fieldName.charAt(prefixLength)));
    }

    private String decapitalizeFirstChar(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
