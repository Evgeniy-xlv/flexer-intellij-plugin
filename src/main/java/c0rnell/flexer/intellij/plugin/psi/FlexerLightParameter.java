package c0rnell.flexer.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.impl.light.LightParameter;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class FlexerLightParameter extends LightParameter implements SyntheticElement {

    private final FlexerLightIdentifier myNameIdentifier;

    public FlexerLightParameter(@NotNull String name, @NotNull PsiType type, @NotNull PsiElement declarationScope) {
        this(name, type, declarationScope, JavaLanguage.INSTANCE);
    }

    public FlexerLightParameter(@NotNull String name,
                                @NotNull PsiType type,
                                @NotNull PsiElement declarationScope,
                                @NotNull Language language) {
        super(name, type, declarationScope, language);
        super.setModifierList(new FlexerLightModifierList(declarationScope.getManager(), language));
        myNameIdentifier = new FlexerLightIdentifier(declarationScope.getManager(), name);
    }

    @NotNull
    @Override
    public String getName() {
        return myNameIdentifier.getText();
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        myNameIdentifier.setText(name);
        return this;
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        return myNameIdentifier;
    }

    @Override
    public TextRange getTextRange() {
        TextRange r = super.getTextRange();
        return r == null ? TextRange.EMPTY_RANGE : r;
    }

    @Override
    public FlexerLightParameter setModifiers(String... modifiers) {
        final FlexerLightModifierList flexerLightModifierList = (FlexerLightModifierList) getModifierList();
        flexerLightModifierList.clearModifiers();
        Stream.of(modifiers).forEach(flexerLightModifierList::addModifier);
        return this;
    }

    @Override
    public FlexerLightParameter setModifierList(LightModifierList modifierList) {
        setModifiers(modifierList.getModifiers());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlexerLightParameter that = (FlexerLightParameter) o;

        final PsiType thisType = getType();
        final PsiType thatType = that.getType();
        if (thisType.isValid() != thatType.isValid()) {
            return false;
        }

        return thisType.getCanonicalText().equals(thatType.getCanonicalText());
    }

    @Override
    public int hashCode() {
        return getType().hashCode();
    }
}
