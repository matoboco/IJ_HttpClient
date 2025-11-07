package org.javamaster.httpclient.action.addHttp

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import org.javamaster.httpclient.HttpIcons
import org.javamaster.httpclient.curl.CurlParser
import org.javamaster.httpclient.nls.NlsBundle
import org.javamaster.httpclient.utils.CurlUtils
import org.javamaster.httpclient.utils.NotifyUtil
import java.awt.datatransfer.DataFlavor

/**
 * Import cURL command directly from clipboard without showing dialog
 * @author yudong
 */
@Suppress("ActionPresentationInstantiatedInCtor")
class ImportCurlFromClipboardAction : AddAction(NlsBundle.nls("import.curl.from.clipboard")) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val editor = FileEditorManager.getInstance(project).selectedTextEditor

        if (editor == null) {
            NotifyUtil.notifyWarn(project, NlsBundle.nls("no.editor.open"))
            return
        }

        val contents = CopyPasteManager.getInstance().getContents<String?>(DataFlavor.stringFlavor)

        if (contents.isNullOrEmpty()) {
            NotifyUtil.notifyWarn(project, NlsBundle.nls("clipboard.empty"))
            return
        }

        if (!CurlUtils.isCurlString(contents)) {
            NotifyUtil.notifyWarn(project, NlsBundle.nls("clipboard.not.curl"))
            return
        }

        try {
            val curlRequest = CurlParser(contents).parseToCurlRequest()
            val httpStr = ImportCurlAction.toHttpRequest(curlRequest, contents)

            val document = FileDocumentManager.getInstance().getDocument(editor.virtualFile)!!

            runWriteAction {
                WriteCommandAction.runWriteCommandAction(project) {
                    document.insertString(document.textLength, httpStr)
                    editor.caretModel.moveToOffset(document.textLength)
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }

            NotifyUtil.notifyInfo(project, NlsBundle.nls("curl.imported.successfully"))
        } catch (e: Exception) {
            NotifyUtil.notifyError(project, NlsBundle.nls("curl.import.failed") + ": " + e.message)
        }
    }
}
