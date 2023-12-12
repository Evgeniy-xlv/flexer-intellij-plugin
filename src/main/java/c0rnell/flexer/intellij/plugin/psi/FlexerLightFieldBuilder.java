package c0rnell.flexer.intellij.plugin.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.light.LightFieldBuilder;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class FlexerLightFieldBuilder extends LightFieldBuilder implements SyntheticElement {

    private final FlexerLightIdentifier myNameIdentifier;

    public FlexerLightFieldBuilder(@NotNull PsiManager manager, @NotNull String name, @NotNull PsiType type) {
        super(manager, name, type);
        super.setModifierList(new FlexerLightModifierList(manager));
        myNameIdentifier = new FlexerLightIdentifier(manager, name);
    }

    @Override
    public FlexerLightFieldBuilder setModifiers(String... modifiers) {
        final FlexerLightModifierList flexerLightModifierList = (FlexerLightModifierList) getModifierList();
        flexerLightModifierList.clearModifiers();
        Stream.of(modifiers).forEach(flexerLightModifierList::addModifier);
        return this;
    }

    @Override
    public FlexerLightFieldBuilder setModifierList(LightModifierList modifierList) {
        setModifiers(modifierList.getModifiers());
        return this;
    }

    @Nullable
    @Override
    public PsiFile getContainingFile() {
        PsiClass containingClass = getContainingClass();
        return containingClass != null ? containingClass.getContainingFile() : null;
    }

    public FlexerLightFieldBuilder withContainingClass(PsiClass psiClass) {
        setContainingClass(psiClass);
        return this;
    }

    public FlexerLightFieldBuilder withImplicitModifier(@PsiModifier.ModifierConstant @NotNull @NonNls String modifier) {
        ((FlexerLightModifierList) getModifierList()).addImplicitModifierProperty(modifier);
        return this;
    }

    public FlexerLightFieldBuilder withModifier(@PsiModifier.ModifierConstant @NotNull @NonNls String modifier) {
        ((FlexerLightModifierList) getModifierList()).addModifier(modifier);
        return this;
    }

    public FlexerLightFieldBuilder withNavigationElement(PsiElement navigationElement) {
        setNavigationElement(navigationElement);
        return this;
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

    @NotNull
    @Override
    public PsiIdentifier getNameIdentifier() {
        return myNameIdentifier;
    }

    public String toString() {
        return "FlexerLightFieldBuilder: " + getName();
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        // just add new element to the containing class
        final PsiClass containingClass = getContainingClass();
        if (null != containingClass) {
            CheckUtil.checkWritable(containingClass);
            return containingClass.add(newElement);
        }
        return null;
    }

    @Override
    public TextRange getTextRange() {
        TextRange r = super.getTextRange();
        return r == null ? TextRange.EMPTY_RANGE : r;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        // simple do nothing
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        // simple do nothing
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        if (another instanceof FlexerLightFieldBuilder) {
            final FlexerLightFieldBuilder anotherLightField = (FlexerLightFieldBuilder) another;

            boolean stillEquivalent = getName().equals(anotherLightField.getName()) &&
                                      getType().equals(anotherLightField.getType());

            if (stillEquivalent) {
                final PsiClass containingClass = getContainingClass();
                final PsiClass anotherContainingClass = anotherLightField.getContainingClass();

                stillEquivalent = (null == containingClass && null == anotherContainingClass) ||
                                  (null != containingClass && containingClass.isEquivalentTo(anotherContainingClass));
            }

            return stillEquivalent;
        } else {
            return super.isEquivalentTo(another);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlexerLightFieldBuilder that = (FlexerLightFieldBuilder) o;
        return
                Objects.equals(myNameIdentifier, that.myNameIdentifier) &&
                Objects.equals(getModifierList(), that.getModifierList()) &&
                Objects.equals(getContainingClass(), that.getContainingClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myNameIdentifier, getModifierList(), getContainingClass());
    }
}
