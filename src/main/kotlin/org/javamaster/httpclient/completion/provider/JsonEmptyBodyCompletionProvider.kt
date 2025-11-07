package org.javamaster.httpclient.completion.provider

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.javamaster.httpclient.utils.HttpUtils
import org.javamaster.httpclient.utils.HttpUtils.resolveUrlControllerTargetPsiClass
import org.javamaster.httpclient.utils.PsiTypeUtils

/**
 * @author yudong
 */
class JsonEmptyBodyCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val psiElement = parameters.position

        val targetPsiClass = resolveUrlControllerTargetPsiClass(psiElement) ?: return

        val prefix = result.prefixMatcher.prefix
        if (prefix.contains("\"")) {
            return
        }

        PsiTypeUtils.collectFields(targetPsiClass)
            .forEach {
                if (it.modifierList?.hasModifierProperty("static") == true) {
                    return@forEach
                }

                val typeText = it.type.presentableText + " " + HttpUtils.getPsiFieldDesc(it)

                val builder = LookupElementBuilder
                    .create(it, "\"" + it.name + "\"")
                    .withTypeText(typeText, true)

                result.addElement(builder)
            }
    }

}
