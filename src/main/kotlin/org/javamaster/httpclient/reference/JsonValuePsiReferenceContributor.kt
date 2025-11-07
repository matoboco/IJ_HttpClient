package org.javamaster.httpclient.reference

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.javamaster.httpclient.reference.provider.JsonValuePsiReferenceProvider

/**
 * All variable declarations in JSON have been extracted and separately injected as PlainText type,
 * so this Contributor is no longer needed
 *
 * @author yudong
 */
class JsonValuePsiReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral::class.java),
            JsonValuePsiReferenceProvider()
        )
    }

}

