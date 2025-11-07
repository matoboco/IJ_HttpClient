package org.javamaster.httpclient.completion.provider

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.javamaster.httpclient.completion.support.HttpHeadersDictionary.headerValuesMap
import org.javamaster.httpclient.psi.HttpHeader
import org.javamaster.httpclient.psi.HttpHeaderField
import org.javamaster.httpclient.utils.HttpUtils

/**
 * @author yudong
 */
class HttpHeaderFieldValuesProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val headerField = PsiTreeUtil.getParentOfType(
            CompletionUtil.getOriginalOrSelf(parameters.position),
            HttpHeaderField::class.java
        )
        val headerName = headerField?.headerFieldName?.text
        if (StringUtil.isEmpty(headerName)) {
            return
        }

        val headerValues = headerValuesMap[headerName]
        if (headerValues != null) {
            headerValues.forEach {
                result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(it), 200.0))
            }
        }
    }
}