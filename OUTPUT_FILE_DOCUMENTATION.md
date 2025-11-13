# 📁 Dokumentácia: Ukladanie HTTP Response do súboru

## 📋 Prehľad

HttpClient plugin podporuje automatické ukladanie HTTP response do súboru pomocou operátora `>>`.

---

## ✅ Syntax

```http
GET https://api.example.com/data

>> cesta/k/suboru.json
```

### Syntax pravidlá:
- Operátor: `>>` (dve lomené zátvorky a medzera)
- Umiestnenie: Na konci HTTP requestu (po response handleri ak existuje)
- Cesta: Relatívna alebo absolútna

---

## 🎯 Podporované typy requestov

| Request typ | Podporované |
|-------------|-------------|
| GET | ✅ Áno |
| POST | ✅ Áno |
| PUT | ✅ Áno |
| DELETE | ✅ Áno |
| PATCH | ✅ Áno |
| HEAD | ✅ Áno |
| OPTIONS | ✅ Áno |
| MOCK_SERVER | ❌ Nie |
| WebSocket | ❌ Nie |
| Dubbo | ❌ Nie |

---

## 📝 Príklady podľa typu súboru

### JSON súbory
```http
GET https://api.example.com/users
Accept: application/json

>> users.json
```

### HTML súbory
```http
GET https://example.com

>> page.html
```

### XML súbory
```http
GET https://api.example.com/data.xml
Accept: application/xml

>> data.xml
```

### CSV súbory
```http
GET https://api.example.com/export.csv
Accept: text/csv

>> export.csv
```

### Binárne súbory (obrázky, PDF)
```http
### PDF
GET https://example.com/document.pdf

>> document.pdf

### Obrázok
GET https://example.com/image.png

>> image.png
```

---

## 📂 Typy ciest

### 1. Relatívna cesta (v tom istom priečinku)
```http
GET https://api.example.com/data

>> output.json
```
→ Uloží sa v priečinku kde je `.http` súbor

### 2. Relatívna cesta (podpriečinok)
```http
GET https://api.example.com/data

>> responses/output.json
```
→ Uloží sa v `responses/` podpriečinku (musí existovať!)

### 3. Absolútna cesta (Linux/Mac)
```http
GET https://api.example.com/data

>> /home/user/Downloads/data.json
```

### 4. Absolútna cesta (Windows)
```http
GET https://api.example.com/data

>> C:\Users\username\Downloads\data.json
```

---

## 🔄 Kombinácia s Response Handler

### JavaScript post-handler PRED uložením
```http
GET https://api.example.com/users/1

> {%
    // Spracovanie response pred uložením
    client.global.set("userId", response.body.id);
    client.log("User: " + response.body.name);
%}

>> user.json
```

**Poradie vykonania**:
1. Odošle sa HTTP request
2. Príde response
3. Spustí sa JavaScript handler (`> {%...%}`)
4. Uloží sa response do súboru (`>> ...`)

---

## 🎨 Pokročilé príklady

### Workflow: Login → Download Data
```http
### Krok 1: Login
POST https://api.example.com/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "secret123"
}

> {%
    client.global.set("token", response.body.access_token);
%}

>> auth/login-response.json

###

### Krok 2: Download protected data
GET https://api.example.com/protected/data
Authorization: Bearer {{token}}

>> data/protected-data.json
```

---

### Multiple endpoints → Multiple files
```http
### Endpoint 1
GET https://api.example.com/users

>> api-dumps/users.json

###

### Endpoint 2
GET https://api.example.com/posts

>> api-dumps/posts.json

###

### Endpoint 3
GET https://api.example.com/comments

>> api-dumps/comments.json
```

---

### Testing workflow
```http
### Test 1: Create user
POST https://api.example.com/users
Content-Type: application/json

{
  "name": "Test User",
  "email": "test@example.com"
}

> {%
    client.test("User created", () => {
        client.assert(response.status === 201);
    });
    client.global.set("newUserId", response.body.id);
%}

>> test-results/01-create-user.json

###

### Test 2: Get created user
GET https://api.example.com/users/{{newUserId}}

> {%
    client.test("User retrieved", () => {
        client.assert(response.status === 200);
        client.assert(response.body.email === "test@example.com");
    });
%}

>> test-results/02-get-user.json
```

---

## ⚠️ Obmedzenia a poznámky

### 1. **Priečinok musí existovať**
```http
# ❌ Zlyhá - priečinok neexistuje
>> non-existent/output.json

# ✅ Funguje - priečinok existuje
>> existing-folder/output.json
```

**Riešenie**:
```bash
mkdir -p responses
```

---

### 2. **Prepísanie súborov**
Ak súbor už existuje, bude **automaticky prepísaný** bez varovania!

```http
GET https://api.example.com/data

>> data.json  # ← Prepíše existujúci data.json
```

---

### 3. **Len response body**
Do súboru sa uloží **iba response body**, nie headers ani status code.

**Uložené**:
```json
{"id": 1, "name": "John"}
```

**Nie sú uložené**:
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 28
```

---

### 4. **Encoding**
Response sa uloží v **UTF-8** encodingu.

---

### 5. **Prázdne response**
Ak je response prázdna (napr. HTTP 204 No Content), vytvorí sa prázdny súbor.

---

### 6. **Binárne súbory**
Binárne súbory (obrázky, PDF) sa uložia korektne v binárnom formáte.

---

## 🧪 Testovanie

### Jednoduchý test:
```http
### Test uloženia súboru
GET https://jsonplaceholder.typicode.com/users/1

>> test-output.json
```

**Overenie**:
1. Spustite request (▶️ Run button)
2. Skontrolujte, či súbor `test-output.json` existuje v rovnakom priečinku
3. Otvorte súbor a overte obsah

---

## 📚 Use Cases

### 1. **API Testing - Zachytávanie responses**
```http
### Zachytiť response pre debugging
GET https://api.example.com/complex-endpoint

>> debug/response-$(date +%Y%m%d-%H%M%S).json
```

### 2. **Data Export**
```http
### Exportovať dáta z API
GET https://api.example.com/reports/monthly
Accept: text/csv

>> exports/monthly-report.csv
```

### 3. **Backup API responses**
```http
### Záloha dát pred migráciou
GET https://old-api.example.com/users

>> backup/users-backup.json
```

### 4. **Documentation generation**
```http
### Získať API response pre dokumentáciu
GET https://api.example.com/v1/users/1

>> docs/examples/get-user-response.json
```

---

## 🔧 Tipy a triky

### Použitie premenných v názve súboru
```http
@timestamp = {{$timestamp}}

###

GET https://api.example.com/data

>> outputs/data-{{timestamp}}.json
```

### Organizácia výstupov do štruktúry
```
project/
├── api-tests/
│   ├── requests.http
│   └── responses/
│       ├── users/
│       │   ├── get-all-users.json
│       │   └── get-user-1.json
│       └── posts/
│           ├── get-all-posts.json
│           └── create-post.json
```

---

## 🎓 Best Practices

1. ✅ **Vytvorte samostatný priečinok pre output súbory**
   ```
   mkdir responses
   ```

2. ✅ **Používajte popisné názvy súborov**
   ```http
   >> responses/2024-11-07-users-list.json
   ```

3. ✅ **Gitignore output súbory**
   ```gitignore
   # .gitignore
   responses/
   outputs/
   *.output.json
   ```

4. ✅ **Kombinujte s testami**
   ```http
   > {%
       client.test("Valid JSON", () => {
           client.assert(typeof response.body === 'object');
       });
   %}

   >> validated-response.json
   ```

5. ✅ **Dokumentujte účel každého output súboru**
   ```http
   ### Získať používateľov pre testing migrácie
   # Výstup: users.json - Zoznam všetkých používateľov pred migráciou
   GET https://api.example.com/users

   >> backup/users-pre-migration.json
   ```

---

## ❓ FAQ

**Q: Môžem použiť `>>` s Mock Serverom?**
A: Nie, `>>` funguje len s HTTP requestami (GET, POST, atď.), nie s MOCK_SERVER.

**Q: Ako prepísať súbor bez varovania?**
A: Plugin automaticky prepíše existujúci súbor. Ak chcete zálohu, použite iný názov súboru.

**Q: Funguje to s veľkými súbormi?**
A: Áno, plugin korektne ukladá aj veľké binárne súbory (video, zip, atď.).

**Q: Môžem použiť premenné v názve súboru?**
A: Áno, môžete použiť environment premenné: `>> outputs/{{env}}-data.json`

**Q: Uloží sa aj HTTP status code?**
A: Nie, uloží sa len response body. Pre status code použite JavaScript handler.

---

*Dokumentácia vytvorená: 2025-11-07*
*Verzia pluginu: 5.8.4*
