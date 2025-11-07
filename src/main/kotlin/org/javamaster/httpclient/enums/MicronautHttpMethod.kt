package org.javamaster.httpclient.enums

/**
 * Micronaut HTTP method annotations
 * @author Claude AI (based on Spring implementation by yudong)
 */
enum class MicronautHttpMethod(val qualifiedName: String, val method: HttpMethod) {
    GET("io.micronaut.http.annotation.Get", HttpMethod.GET),
    POST("io.micronaut.http.annotation.Post", HttpMethod.POST),
    PUT("io.micronaut.http.annotation.Put", HttpMethod.PUT),
    DELETE("io.micronaut.http.annotation.Delete", HttpMethod.DELETE),
    PATCH("io.micronaut.http.annotation.Patch", HttpMethod.PATCH),
    HEAD("io.micronaut.http.annotation.Head", HttpMethod.HEAD),
    OPTIONS("io.micronaut.http.annotation.Options", HttpMethod.OPTIONS),
    TRACE("io.micronaut.http.annotation.Trace", HttpMethod.TRACE),

    // Parameter annotations
    QUERY_VALUE("io.micronaut.http.annotation.QueryValue", HttpMethod.UNKNOWN),
    PATH_VARIABLE("io.micronaut.http.annotation.PathVariable", HttpMethod.UNKNOWN),
    BODY("io.micronaut.http.annotation.Body", HttpMethod.UNKNOWN),
    HEADER("io.micronaut.http.annotation.Header", HttpMethod.UNKNOWN),
    COOKIE_VALUE("io.micronaut.http.annotation.CookieValue", HttpMethod.UNKNOWN);

    val shortName by lazy { qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1) }

    companion object {
        private val map by lazy {
            val map = mutableMapOf<String, MicronautHttpMethod>()
            for (it in entries) {
                map[it.qualifiedName] = it
            }
            map
        }

        private val shortMap by lazy {
            val map = mutableMapOf<String, MicronautHttpMethod>()
            for (it in entries) {
                map[it.shortName] = it
            }
            map
        }

        fun getByQualifiedName(qualifiedName: String?): MicronautHttpMethod? {
            return map[qualifiedName]
        }

        fun getByShortName(name: String?): MicronautHttpMethod? {
            return shortMap[name]
        }
    }
}
