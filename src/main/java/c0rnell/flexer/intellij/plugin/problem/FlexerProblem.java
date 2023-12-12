package c0rnell.flexer.intellij.plugin.problem;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.InspectionMessage;

import java.util.Objects;

public class FlexerProblem {

    private static final LocalQuickFix[] EMPTY_QUICK_FIX = LocalQuickFix.EMPTY_ARRAY;

    private final ProblemHighlightType highlightType;
    private final LocalQuickFix[] quickFixes;
    @InspectionMessage
    private final String message;

    public FlexerProblem(@InspectionMessage String message) {
        this(message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, EMPTY_QUICK_FIX);
    }

    public FlexerProblem(@InspectionMessage String message, ProblemHighlightType highlightType) {
        this(message, highlightType, EMPTY_QUICK_FIX);
    }

    public FlexerProblem(@InspectionMessage String message, LocalQuickFix... quickFixes) {
        this(message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, quickFixes);
    }

    public FlexerProblem(@InspectionMessage String message, ProblemHighlightType highlightType, LocalQuickFix... quickFixes) {
        this.message = message;
        this.highlightType = highlightType;
        this.quickFixes = quickFixes;
    }

    public ProblemHighlightType getHighlightType() {
        return highlightType;
    }

    public LocalQuickFix[] getQuickFixes() {
        return quickFixes;
    }

    @InspectionMessage
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlexerProblem that = (FlexerProblem) o;

        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message);
    }
}
