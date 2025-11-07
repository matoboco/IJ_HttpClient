package org.javamaster.httpclient.scan.support

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex
import com.intellij.psi.impl.search.JavaSourceFilterScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.javamaster.httpclient.enums.Control
import org.javamaster.httpclient.enums.HttpMethod
import org.javamaster.httpclient.enums.MicronautHttpMethod
import org.javamaster.httpclient.enums.MicronautHttpMethod.Companion.getByQualifiedName
import org.javamaster.httpclient.enums.MicronautHttpMethod.Companion.getByShortName
import org.javamaster.httpclient.utils.AnnoUtils
import org.javamaster.httpclient.utils.AnnoUtils.collectMethodAnnotations
import java.util.function.Consumer

/**
 * Service for scanning Micronaut controllers and extracting HTTP endpoints
 * @author Claude AI (based on Spring implementation by yudong)
 */
@Service(Service.Level.PROJECT)
class MicronautControllerScanService {

    fun findRequests(project: Project, searchScope: GlobalSearchScope): List<Request> {
        val requests = mutableListOf<Request>()

        fetchRequests(project, searchScope) {
            requests.add(it)
        }

        return requests
    }

    fun fetchRequests(project: Project, scope: GlobalSearchScope, consumer: Consumer<Request>) {
        val annotationIndex = JavaAnnotationIndex.getInstance()

        val annotations = StubIndex.getElements(
            annotationIndex.key,
            Control.MicronautController.simpleName,
            project,
            JavaSourceFilterScope(scope),
            PsiAnnotation::class.java
        )

        iterateControllers(annotations, consumer)
    }

    private fun iterateControllers(controllerAnnoList: Collection<PsiAnnotation>, consumer: Consumer<Request>) {
        controllerAnnoList.forEach { controllerAnno ->
            val psiModifierList = controllerAnno.parent as? PsiModifierList ?: return@forEach
            val controllerClass = psiModifierList.parent as? PsiClass ?: return@forEach

            // Micronaut @Controller has value or uri parameter for base path
            val basePath = extractControllerPath(controllerAnno)

            val childrenRequests: MutableList<Request> = mutableListOf()
            var parentRequest: Request? = null

            if (basePath.isNotEmpty() && basePath != "/") {
                parentRequest = Request(HttpMethod.REQUEST, basePath, null, null)
            }

            val requests = controllerClass.allMethods
                .map { getRequests(it) }
                .flatten()

            childrenRequests.addAll(requests)

            if (parentRequest == null) {
                childrenRequests.forEach { consumer.accept(it) }
            } else {
                childrenRequests.forEach {
                    val request = it.copyWithParent(parentRequest)
                    consumer.accept(request)
                }
            }
        }
    }

    private fun extractControllerPath(annotation: PsiAnnotation): String {
        val attributes = annotation.attributes
        for (attribute in attributes) {
            val name = attribute.attributeName

            // Micronaut uses 'value' or 'uri' for base path
            if (name == "value" || name == "uri") {
                when (val value = AnnoUtils.getAttributeValue(attribute.attributeValue)) {
                    is String -> return formatPath(value)
                }
            }
        }
        return "/"
    }

    private fun getRequests(method: PsiMethod): List<Request> {
        val methodAnnotations = collectMethodAnnotations(method)

        return methodAnnotations
            .map { getRequests(it, method) }
            .flatten()
    }

    private fun getRequests(annotation: PsiAnnotation, psiMethod: PsiMethod): List<Request> {
        var httpMethod = getByQualifiedName(annotation.qualifiedName)

        if (httpMethod == null) {
            httpMethod = getByShortName(annotation.nameReferenceElement?.text)
        }

        // Skip if not a Micronaut HTTP annotation or if it's a parameter annotation
        if (httpMethod == null || httpMethod.method == HttpMethod.UNKNOWN) {
            return emptyList()
        }

        val paths: MutableList<String> = mutableListOf()
        var hasPath = false

        val attributes = annotation.attributes
        for (attribute in attributes) {
            val name = attribute.attributeName

            // Micronaut uses 'value' or 'uri' or 'uris' for path
            if (name == "value" || name == "uri" || name == "uris") {
                hasPath = true
                when (val value = AnnoUtils.getAttributeValue(attribute.attributeValue)) {
                    is String -> paths.add(formatPath(value))
                    is List<*> -> value.forEach { paths.add(formatPath(it)) }
                }
            }
        }

        // If no path is specified, use "/"
        if (!hasPath) {
            paths.add("/")
        }

        return paths.map { path ->
            Request(httpMethod.method, path, psiMethod, null)
        }
    }

    private fun formatPath(path: Any?): String {
        val slash = "/"
        if (path == null) {
            return slash
        }

        val currPath = path as? String ?: path.toString()

        if (currPath.isEmpty()) {
            return slash
        }

        if (currPath.startsWith(slash)) {
            return currPath
        }

        return slash + currPath
    }

    companion object {
        fun getService(project: Project): MicronautControllerScanService {
            return project.getService(MicronautControllerScanService::class.java)
        }
    }
}
