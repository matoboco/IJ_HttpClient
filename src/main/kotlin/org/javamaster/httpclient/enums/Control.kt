package org.javamaster.httpclient.enums

enum class Control(val simpleName: String, val qualifiedName: String) {
    // Spring Framework
    Controller("Controller", "org.springframework.stereotype.Controller"),
    RestController("RestController", "org.springframework.web.bind.annotation.RestController"),

    // Micronaut Framework
    MicronautController("Controller", "io.micronaut.http.annotation.Controller")
}