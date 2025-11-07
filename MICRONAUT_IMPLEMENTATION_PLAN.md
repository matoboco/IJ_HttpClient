# Implementačný plán: Micronaut Controller podpora

## 📋 Cieľ
Rozšíriť HttpClient plugin o podporu Micronaut controllerov s rovnakou funkcionalitou ako má Spring:
- ✅ Navigácia z URL na Micronaut controller metódu (Ctrl+Click)
- ✅ Hover dokumentácia pre Micronaut endpointy
- ✅ JSON completion pre Micronaut controller parametre
- ✅ Vyhľadávanie Micronaut API v SearchEverywhere
- ✅ Inlay hints pre Micronaut endpointy

---

## 🔍 Analýza existujúcej Spring implementácie

### Architektúra Spring podpory

#### 1️⃣ **Enums a konštanty** (package: `enums`)
```kotlin
// Control.kt - definuje controller anotácie
enum class Control(val simpleName: String, val qualifiedName: String) {
    Controller("Controller", "org.springframework.stereotype.Controller"),
    RestController("RestController", "org.springframework.web.bind.annotation.RestController")
}

// SpringHttpMethod.kt - definuje HTTP mapping anotácie
enum class SpringHttpMethod(val qualifiedName: String, val method: HttpMethod) {
    REQUEST_MAPPING("org.springframework.web.bind.annotation.RequestMapping", HttpMethod.REQUEST),
    GET_MAPPING("org.springframework.web.bind.annotation.GetMapping", HttpMethod.GET),
    POST_MAPPING("org.springframework.web.bind.annotation.PostMapping", HttpMethod.POST),
    // atď...
}
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/enums/Control.kt`
- `src/main/kotlin/org/javamaster/httpclient/enums/SpringHttpMethod.kt`

#### 2️⃣ **Skenovanie controllerov** (package: `scan.support`)
```kotlin
// SpringControllerScanService.kt - skenuje Spring controllery
@Service(Service.Level.PROJECT)
class SpringControllerScanService {
    fun findRequests(project: Project, searchScope: GlobalSearchScope): List<Request>
    fun fetchRequests(project: Project, scope: GlobalSearchScope, consumer: Consumer<Request>)
}
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/scan/support/SpringControllerScanService.kt`

**Čo robí**:
1. Používa `JavaAnnotationIndex` na nájdenie všetkých `@Controller` a `@RestController` anotácií
2. Pre každú controller class:
   - Extrahuje `@RequestMapping` z class úrovne (parent path)
   - Iteruje cez všetky metódy
   - Extrahuje HTTP mapping anotácie (GetMapping, PostMapping, atď.)
   - Parsuje `method` a `path/value` atribúty
   - Kombinuje parent path + child path
3. Vracia list `Request` objektov

#### 3️⃣ **Request model** (package: `scan.support`)
```kotlin
// Request.kt - reprezentuje jeden endpoint
class Request(
    tmpMethod: HttpMethod,
    tmpPath: String,
    val psiElement: PsiMethod?,
    parent: Request?
)
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/scan/support/Request.kt`

#### 4️⃣ **Cache manager** (package: `scan`)
```kotlin
// ScanRequest.kt - cachuje výsledky skenovania
object ScanRequest {
    fun findApiMethod(module: Module, searchTxt: String, method: String): PsiMethod?
    fun getCacheRequestMap(module: Module, project: Project): Map<String, List<Request>>
}
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/scan/ScanRequest.kt`

**Čo robí**:
- Cachuje `Request` objekty pomocou `CachedValuesManager`
- Invaliduje cache pri zmene controller súborov (`ControllerPsiModificationTracker`)
- Vyhľadáva metódy podľa path + HTTP metódy

#### 5️⃣ **Navigácia** (package: `reference`)
```kotlin
// HttpUrlControllerMethodPsiReference.kt - navigácia z URL na metódu
class HttpUrlControllerMethodPsiReference(...) : PsiReferenceBase<HttpRequestTarget> {
    override fun resolve(): PsiElement? {
        return ScanRequest.findApiMethod(module, searchTxt, httpMethod.text)
    }
}
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/reference/support/HttpUrlControllerMethodPsiReference.kt`
- `src/main/kotlin/org/javamaster/httpclient/reference/HttpUrlControllerMethodPsiReferenceContributor.kt`
- `src/main/kotlin/org/javamaster/httpclient/reference/provider/HttpUrlControllerMethodPsiReferenceProvider.kt`

#### 6️⃣ **Dokumentácia (Hover)** (package: `doc`)
```kotlin
// HttpUrlControllerMethodDocumentationProvider.kt - hover nad URL
class HttpUrlControllerMethodDocumentationProvider : DocumentationProvider {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String?
}
```

**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/doc/HttpUrlControllerMethodDocumentationProvider.kt`

#### 7️⃣ **JSON completion** (package: `reference`, `completion`)
**Umiestnenie**:
- `src/main/kotlin/org/javamaster/httpclient/reference/JsonKeyControllerMethodFieldPsiReferenceContributor.kt`
- `src/main/kotlin/org/javamaster/httpclient/completion/provider/JsonKeyCompletionProvider.kt`

---

## 🎯 Návrh Micronaut implementácie

### Micronaut anotácie

Micronaut používa iné package pre anotácie:

| Spring | Micronaut |
|--------|-----------|
| `org.springframework.stereotype.Controller` | `io.micronaut.http.annotation.Controller` |
| `org.springframework.web.bind.annotation.RestController` | *(nie je, používa sa len @Controller)* |
| `org.springframework.web.bind.annotation.GetMapping` | `io.micronaut.http.annotation.Get` |
| `org.springframework.web.bind.annotation.PostMapping` | `io.micronaut.http.annotation.Post` |
| `org.springframework.web.bind.annotation.PutMapping` | `io.micronaut.http.annotation.Put` |
| `org.springframework.web.bind.annotation.DeleteMapping` | `io.micronaut.http.annotation.Delete` |
| `org.springframework.web.bind.annotation.PatchMapping` | `io.micronaut.http.annotation.Patch` |
| `org.springframework.web.bind.annotation.RequestMapping` | *(nie je, používa sa @UriMapping alebo konkrétne @Get/@Post)* |
| `org.springframework.web.bind.annotation.RequestParam` | `io.micronaut.http.annotation.QueryValue` |
| `org.springframework.web.bind.annotation.PathVariable` | `io.micronaut.http.annotation.PathVariable` |
| `org.springframework.web.bind.annotation.RequestBody` | `io.micronaut.http.annotation.Body` |
| `org.springframework.web.bind.annotation.RequestHeader` | `io.micronaut.http.annotation.Header` |

### Príklad Micronaut controlleru

```java
import io.micronaut.http.annotation.*;

@Controller("/api/users")
public class UserController {

    @Get("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Post
    public User createUser(@Body User user) {
        return userService.save(user);
    }

    @Put("/{id}")
    public User updateUser(@PathVariable Long id, @Body User user) {
        return userService.update(id, user);
    }

    @Delete("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

---

## 📝 Implementačné kroky

### Fáza 1: Základná infraštruktúra ✅

#### **Krok 1.1: Vytvoriť Micronaut enums**

**Súbor**: `src/main/kotlin/org/javamaster/httpclient/enums/MicronautHttpMethod.kt`

```kotlin
package org.javamaster.httpclient.enums

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
```

#### **Krok 1.2: Rozšíriť Control enum o Micronaut**

**Súbor**: `src/main/kotlin/org/javamaster/httpclient/enums/Control.kt`

```kotlin
enum class Control(val simpleName: String, val qualifiedName: String) {
    // Spring
    Controller("Controller", "org.springframework.stereotype.Controller"),
    RestController("RestController", "org.springframework.web.bind.annotation.RestController"),

    // Micronaut
    MicronautController("Controller", "io.micronaut.http.annotation.Controller")
}
```

---

### Fáza 2: Skenovanie Micronaut controllerov ✅

#### **Krok 2.1: Vytvoriť MicronautControllerScanService**

**Súbor**: `src/main/kotlin/org/javamaster/httpclient/scan/support/MicronautControllerScanService.kt`

```kotlin
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
import java.util.function.Consumer

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
            val psiModifierList = controllerAnno.parent as PsiModifierList
            val controllerClass = psiModifierList.parent as PsiClass? ?: return@forEach

            // Micronaut @Controller má value parameter pre base path
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

            if (name == "value" || name == "uri") {
                when (val value = AnnoUtils.getAttributeValue(attribute.attributeValue)) {
                    is String -> return formatPath(value)
                }
            }
        }
        return "/"
    }

    private fun getRequests(method: PsiMethod): List<Request> {
        val methodAnnotations = AnnoUtils.collectMethodAnnotations(method)

        return methodAnnotations
            .map { getRequests(it, method) }
            .flatten()
    }

    private fun getRequests(annotation: PsiAnnotation, psiMethod: PsiMethod): List<Request> {
        var httpMethod = getByQualifiedName(annotation.qualifiedName)

        if (httpMethod == null) {
            httpMethod = getByShortName(annotation.nameReferenceElement?.text)
        }

        if (httpMethod == null || httpMethod.method == HttpMethod.UNKNOWN) {
            return emptyList()
        }

        val paths: MutableList<String> = mutableListOf()
        var hasPath = false

        val attributes = annotation.attributes
        for (attribute in attributes) {
            val name = attribute.attributeName

            // Micronaut používa 'value' alebo 'uri' pre path
            if (name == "value" || name == "uri") {
                hasPath = true
                when (val value = AnnoUtils.getAttributeValue(attribute.attributeValue)) {
                    is String -> paths.add(formatPath(value))
                    is List<*> -> value.forEach { paths.add(formatPath(it)) }
                }
            }
        }

        // Ak nie je špecifikovaný path, použije sa "/"
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
```

#### **Krok 2.2: Integrovať do ScanRequest**

**Súbor**: `src/main/kotlin/org/javamaster/httpclient/scan/ScanRequest.kt`

Upraviť metódy na volanie oboch služieb (Spring + Micronaut):

```kotlin
fun fetchRequests(project: Project, searchScope: GlobalSearchScope, consumer: Consumer<Request>) {
    // Spring controllery
    val springService = SpringControllerScanService.getService(project)
    springService.fetchRequests(project, searchScope, consumer)

    // Micronaut controllery
    val micronautService = MicronautControllerScanService.getService(project)
    micronautService.fetchRequests(project, searchScope, consumer)
}

fun getCacheRequestMap(module: Module, project: Project): Map<String, List<Request>> {
    val key = keyMap.computeIfAbsent(module.name) {
        Key.create("httpClient.requestMap.$it")
    }

    return CachedValuesManager.getManager(project)
        .getCachedValue(module, key, {
            val requests = mutableListOf<Request>()

            // Spring
            val springService = SpringControllerScanService.getService(project)
            requests.addAll(springService.findRequests(project, module.moduleWithLibrariesScope))

            // Micronaut
            val micronautService = MicronautControllerScanService.getService(project)
            requests.addAll(micronautService.findRequests(project, module.moduleWithLibrariesScope))

            val requestMap = requests.groupBy { it.toString() }

            CachedValueProvider.Result.create(requestMap, ControllerPsiModificationTracker)
        }, false)
}
```

---

### Fáza 3: Navigácia a dokumentácia ✅

Existing implementation (`HttpUrlControllerMethodPsiReference`, `HttpUrlControllerMethodDocumentationProvider`)
by mali fungovať automaticky, keďže používajú `ScanRequest.findApiMethod()`, ktorý teraz vracia aj Micronaut endpointy.

**Testovanie**:
1. Vytvoriť Micronaut controller
2. Otvoriť `.http` súbor
3. Napísať URL z controlleru
4. Skúsiť Ctrl+Click - malo by skočiť na metódu
5. Skúsiť hover - mala by sa zobraziť dokumentácia

---

### Fáza 4: JSON completion ✅

Existing implementation (`JsonKeyControllerMethodFieldPsiReference`) by mala fungovať automaticky,
pretože používa `ScanRequest` na nájdenie controller metód.

**Potrebné overenie**:
- Či Micronaut používa rovnaké anotácie pre parametre (`@Body`)
- Či je potrebné pridať support pre `@QueryValue` atď.

---

### Fáza 5: Registrácia v plugin.xml ✅

**Súbor**: `src/main/resources/META-INF/plugin.xml`

Pridať registráciu služby:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Existujúce Spring service -->
    <projectService
        serviceImplementation="org.javamaster.httpclient.scan.support.SpringControllerScanService"/>

    <!-- Nový Micronaut service -->
    <projectService
        serviceImplementation="org.javamaster.httpclient.scan.support.MicronautControllerScanService"/>
</extensions>
```

---

### Fáza 6: Testovanie ✅

#### **Unit testy**

**Súbor**: `src/test/kotlin/org/javamaster/httpclient/scan/MicronautControllerScanServiceTest.kt`

```kotlin
package org.javamaster.httpclient.scan

import org.junit.Test
import org.javamaster.httpclient.enums.HttpMethod

class MicronautControllerScanServiceTest {

    @Test
    fun testMicronautControllerScanning() {
        // Test setup
        // Mock Micronaut controller class
        // Verify requests are correctly extracted
    }

    @Test
    fun testPathCombination() {
        // Test @Controller("/api") + @Get("/users") = /api/users
    }

    @Test
    fun testEmptyPath() {
        // Test @Get with no path = "/"
    }
}
```

#### **Manuálne testovanie**

1. **Vytvorenie testovacieho projektu**:
   - Vytvoriť Micronaut projekt v IntelliJ
   - Pridať Micronaut controller:
   ```java
   @Controller("/api/test")
   public class TestController {
       @Get("/hello")
       public String hello() {
           return "Hello Micronaut!";
       }
   }
   ```

2. **Test navigácie**:
   - Vytvoriť `.http` súbor:
   ```http
   GET http://localhost:8080/api/test/hello
   ```
   - Ctrl+Click na URL → malo by skočiť na `hello()` metódu

3. **Test hover dokumentácie**:
   - Hover nad URL → mala by sa zobraziť metóda signatura

4. **Test SearchEverywhere**:
   - Stlačiť 2x Shift
   - Zadať "test hello"
   - Malo by sa nájsť Micronaut endpoint

---

## 📦 Súbory na vytvorenie/úpravu

### Nové súbory:
1. ✅ `src/main/kotlin/org/javamaster/httpclient/enums/MicronautHttpMethod.kt`
2. ✅ `src/main/kotlin/org/javamaster/httpclient/scan/support/MicronautControllerScanService.kt`
3. ✅ `src/test/kotlin/org/javamaster/httpclient/scan/MicronautControllerScanServiceTest.kt`

### Upravované súbory:
1. ✅ `src/main/kotlin/org/javamaster/httpclient/enums/Control.kt` - pridať `MicronautController`
2. ✅ `src/main/kotlin/org/javamaster/httpclient/scan/ScanRequest.kt` - integrovať Micronaut scanning
3. ✅ `src/main/resources/META-INF/plugin.xml` - registrovať `MicronautControllerScanService`

---

## 🔧 Dodatočné vylepšenia (voliteľné)

### Fáza 7: Pokročilé features

1. **Support pre Micronaut @UriMapping**
   - Micronaut má aj všeobecnú `@UriMapping` anotáciu

2. **Support pre Micronaut validation**
   - `@Valid`, `@Validated`, `@NotNull` atď.

3. **Support pre Micronaut Swagger/OpenAPI**
   - `@Tag`, `@Operation`, `@ApiResponse` atď.

4. **Support pre reactive typy**
   - `Mono`, `Flux`, `Publisher` atď.

5. **Live templates pre Micronaut**
   - Pridať live templates pre rýchle vytváranie Micronaut controllerov

---

## 📊 Odhad času

| Fáza | Úloha | Čas |
|------|-------|-----|
| 1 | Základná infraštruktúra (enums) | 1 hodina |
| 2 | Skenovanie controllerov | 3-4 hodiny |
| 3 | Integrácia navigácie/dokumentácie | 1 hodina |
| 4 | JSON completion overenie | 1 hodina |
| 5 | Registrácia v plugin.xml | 30 minút |
| 6 | Testovanie | 2 hodiny |
| **Celkom** | | **8-9 hodín** |

---

## ✅ Kontrolný zoznam

- [ ] Vytvoriť `MicronautHttpMethod` enum
- [ ] Rozšíriť `Control` enum o Micronaut
- [ ] Vytvoriť `MicronautControllerScanService`
- [ ] Integrovať do `ScanRequest`
- [ ] Otestovať navigáciu (Ctrl+Click)
- [ ] Otestovať hover dokumentáciu
- [ ] Otestovať JSON completion
- [ ] Otestovať SearchEverywhere
- [ ] Napísať unit testy
- [ ] Aktualizovať `plugin.xml`
- [ ] Aktualizovať `README.md` o Micronaut support
- [ ] Code review
- [ ] Commit a push

---

## 📖 Použitie po implementácii

### Príklad: Micronaut Controller

```java
package com.example;

import io.micronaut.http.annotation.*;

@Controller("/api/users")
public class UserController {

    @Get("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Post
    public User createUser(@Body User user) {
        return userService.save(user);
    }
}
```

### Príklad: .http súbor

```http
### Get user by ID (Micronaut endpoint)
GET http://localhost:8080/api/users/123

### Create new user (Micronaut endpoint)
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Funkcionalita**:
- ✅ Ctrl+Click na `/api/users/123` → skočí na `getUser()` metódu
- ✅ Hover nad URL → zobrazí metódu signatúru
- ✅ JSON keys completion pre `User` objekt
- ✅ SearchEverywhere nájde Micronaut endpointy

---

*Implementačný plán vytvoril: Claude AI*
*Dátum: 2025-11-07*
