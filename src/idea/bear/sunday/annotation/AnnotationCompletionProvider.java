package idea.bear.sunday.annotation;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

public class AnnotationCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters,
                                  ProcessingContext processingContext,
                                  @NotNull CompletionResultSet completionResultSet) {

        final PsiElement psiElement = completionParameters.getOriginalPosition();
        if(psiElement == null) {
            return;
        }

        final Project project = psiElement.getProject();
        final PhpIndex phpIndex = PhpIndex.getInstance(project);
        final Icon icon = IconLoader.getIcon("/idea/bear/sunday/icons/bearsunday.png");

        Collection<PhpClass> phpClasses = new THashSet<>();
        Collection<PhpNamespace> namespaces = phpIndex.getNamespacesByName("\\bear\\resource\\annotation");
        namespaces.addAll(phpIndex.getNamespacesByName("\\bear\\repositorymodule\\annotation"));
        for (PhpNamespace namespace: namespaces) {
            phpClasses.addAll(PsiTreeUtil.getChildrenOfTypeAsList(namespace.getStatements(), PhpClass.class));
        }

        for (PhpClass phpClass: phpClasses) {
            if (phpClass.isAbstract()) {
                continue;
            }
            LookupElementBuilder lookupElement = LookupElementBuilder
                .create(phpClass.getName() + "()")
                .withPresentableText(phpClass.getName())
                .withTypeText(phpClass.getPresentableFQN(), true)
                .withPsiElement(phpClass.getContext())
                .withInsertHandler(new InsertHandler<LookupElement>() {
                    @Override
                    public void handleInsert(InsertionContext insertionContext, LookupElement lookupElement) {
                        // caret move into quotation after insert completion
                        insertionContext.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
                        PsiElement psiElement = PsiUtilCore.getElementAtOffset(
                            insertionContext.getFile(), insertionContext.getStartOffset());
                        PhpPsiElement scope = PhpCodeInsightUtil.findScopeForUseOperator(psiElement);
                        String insertPhpUse = "\\" + PsiTreeUtil.getChildrenOfTypeAsList(lookupElement.getPsiElement(), PhpClass.class).get(0).getPresentableFQN();
                        if (scope == null) {
                            return;
                        }
                        if (PhpCodeInsightUtil.alreadyImported(scope, insertPhpUse) == null) {
                            PhpAliasImporter.insertUseStatement(insertPhpUse, scope);
                        }
                    }
                }).withIcon(icon);
            completionResultSet.addElement(lookupElement);
        }
    }
}
