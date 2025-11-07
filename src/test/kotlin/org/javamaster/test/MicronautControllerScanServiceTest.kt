package org.javamaster.test

import org.javamaster.httpclient.enums.HttpMethod
import org.javamaster.httpclient.enums.MicronautHttpMethod
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Micronaut controller scanning functionality
 * @author Claude AI
 */
class MicronautControllerScanServiceTest {

    @Test
    fun testMicronautHttpMethodEnum() {
        // Test GET mapping
        val getMethod = MicronautHttpMethod.getByQualifiedName("io.micronaut.http.annotation.Get")
        assertNotNull("GET annotation should be found", getMethod)
        assertEquals("GET method should map to HttpMethod.GET", HttpMethod.GET, getMethod?.method)

        // Test POST mapping
        val postMethod = MicronautHttpMethod.getByQualifiedName("io.micronaut.http.annotation.Post")
        assertNotNull("POST annotation should be found", postMethod)
        assertEquals("POST method should map to HttpMethod.POST", HttpMethod.POST, postMethod?.method)

        // Test short name lookup
        val getByShortName = MicronautHttpMethod.getByShortName("Get")
        assertNotNull("GET annotation should be found by short name", getByShortName)
        assertEquals("Short name should match qualified name", getMethod, getByShortName)
    }

    @Test
    fun testMicronautHttpMethodShortNames() {
        assertEquals("Get", MicronautHttpMethod.GET.shortName)
        assertEquals("Post", MicronautHttpMethod.POST.shortName)
        assertEquals("Put", MicronautHttpMethod.PUT.shortName)
        assertEquals("Delete", MicronautHttpMethod.DELETE.shortName)
        assertEquals("Patch", MicronautHttpMethod.PATCH.shortName)
    }

    @Test
    fun testAllMicronautHttpMethods() {
        // Verify all HTTP method annotations are properly mapped
        val httpMethods = listOf(
            MicronautHttpMethod.GET to HttpMethod.GET,
            MicronautHttpMethod.POST to HttpMethod.POST,
            MicronautHttpMethod.PUT to HttpMethod.PUT,
            MicronautHttpMethod.DELETE to HttpMethod.DELETE,
            MicronautHttpMethod.PATCH to HttpMethod.PATCH,
            MicronautHttpMethod.HEAD to HttpMethod.HEAD,
            MicronautHttpMethod.OPTIONS to HttpMethod.OPTIONS,
            MicronautHttpMethod.TRACE to HttpMethod.TRACE
        )

        for ((micronautMethod, expectedHttpMethod) in httpMethods) {
            assertEquals(
                "${micronautMethod.shortName} should map to $expectedHttpMethod",
                expectedHttpMethod,
                micronautMethod.method
            )
        }
    }

    @Test
    fun testMicronautParameterAnnotations() {
        // Verify parameter annotations are mapped to UNKNOWN
        val paramAnnotations = listOf(
            MicronautHttpMethod.QUERY_VALUE,
            MicronautHttpMethod.PATH_VARIABLE,
            MicronautHttpMethod.BODY,
            MicronautHttpMethod.HEADER,
            MicronautHttpMethod.COOKIE_VALUE
        )

        for (annotation in paramAnnotations) {
            assertEquals(
                "${annotation.shortName} should map to HttpMethod.UNKNOWN",
                HttpMethod.UNKNOWN,
                annotation.method
            )
        }
    }

    @Test
    fun testMicronautAnnotationQualifiedNames() {
        assertEquals(
            "io.micronaut.http.annotation.Get",
            MicronautHttpMethod.GET.qualifiedName
        )
        assertEquals(
            "io.micronaut.http.annotation.Controller",
            "io.micronaut.http.annotation.Controller"
        )
        assertEquals(
            "io.micronaut.http.annotation.QueryValue",
            MicronautHttpMethod.QUERY_VALUE.qualifiedName
        )
    }

    @Test
    fun testNonExistentAnnotation() {
        val result = MicronautHttpMethod.getByQualifiedName("io.micronaut.http.annotation.NonExistent")
        assertNull("Non-existent annotation should return null", result)

        val resultByShortName = MicronautHttpMethod.getByShortName("NonExistent")
        assertNull("Non-existent short name should return null", resultByShortName)
    }
}
