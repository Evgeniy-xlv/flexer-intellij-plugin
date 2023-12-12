package c0rnell.flexer.intellij.plugin.provider;

import c0rnell.flexer.intellij.plugin.processor.Processor;
import com.intellij.psi.PsiAnnotation;

public class FlexerProcessorData {

    private final Processor processor;
    private final PsiAnnotation psiAnnotation;

    FlexerProcessorData(Processor processor, PsiAnnotation psiAnnotation) {
        this.processor = processor;
        this.psiAnnotation = psiAnnotation;
    }

    public Processor getProcessor() {
        return processor;
    }

    public PsiAnnotation getPsiAnnotation() {
        return psiAnnotation;
    }
}
