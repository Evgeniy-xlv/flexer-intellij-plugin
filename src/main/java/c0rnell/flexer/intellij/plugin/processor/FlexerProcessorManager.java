package c0rnell.flexer.intellij.plugin.processor;

import c0rnell.flexer.intellij.plugin.processor.clazz.GenerateModelClassGeneratorProcessor;
import c0rnell.flexer.intellij.plugin.processor.clazz.GenerateModelMethodGeneratorProcessor;
import c0rnell.flexer.intellij.plugin.processor.field.GenerateModelFieldProcessor;
import c0rnell.flexer.intellij.plugin.processor.modifier.ModifierProcessor;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public final class FlexerProcessorManager {

    @NotNull
    public static Collection<Processor> getFlexerProcessors() {
        return Arrays.asList(
                ApplicationManager.getApplication().getService(GenerateModelClassGeneratorProcessor.class),
                ApplicationManager.getApplication().getService(GenerateModelMethodGeneratorProcessor.class),
                ApplicationManager.getApplication().getService(GenerateModelFieldProcessor.class)
        );
    }

    @NotNull
    public static Collection<ModifierProcessor> getFlexerModifierProcessors() {
        return Arrays.asList();
    }
}
