```markdown
# Spring Boot Task -- Mini Instagram

Bu proje, teknik bir değerlendirme kapsamında geliştirilmiş küçük bir sosyal medya servisidir. Spring Security **kullanılmadan**, manuel bir kimlik doğrulama mekanizması üzerine inşa edilmiştir.

Kimlik doğrulama, "Opaque Access Token" (veritabanında SHA-256 hash'i olarak saklanan, zaman aşımına uğrayan ve `logout` ile iptal edilebilen) yöntemiyle sağlanmaktadır. Sistem `ADMIN` ve `USER` olmak üzere iki rol içerir.

---

## 🔧 Kullanılan Teknolojiler (Stack)

* **Java:** 17+ (Bu proje JDK 25 ile derlenmiştir)
* **Framework:** Spring Boot 3+ (Spring Web, Spring Data JPA)
* **Veritabanı:** PostgreSQL
* **Veritabanı Geçişi (Migration):** Flyway
* **Şifreleme (Hashing):** BCrypt (`at.favre.lib:bcrypt`)
* **API Dokümantasyonu:** Postman
* **Derleme (Build):** Maven

---

## 📁 Proje Yapısı (Özet)

```

src/main/java/com/example/social
├─ SocialApplication.java
├─ config/            \# AppProperties, WebConfig (resimler için)
├─ domain/
│  ├─ Role.java       \# Enum (ADMIN, USER)
│  └─ entity/        \# User, Token, Post, Comment, Like
├─ repository/        \# UserRepository, TokenRepository, PostRepository vb.
├─ bootstrap/         \# AdminSeeder (Başlangıçta admin kullanıcısı oluşturur)
├─ security/          \# AuthFilter, CurrentUser(Holder), TokenUtils
├─ service/           \# AuthService, UserService, PostService, FileStorageService
└─ web/
├─ controller/     \# AuthController, UserController, PostController
├─ dto/            \# AuthDTOs, UserDTOs, PostDTOs
└─ exception/      \# ApiError, GlobalExceptionHandler, Özel Hata Sınıfları

````

---

## 🚀 Hızlı Başlangıç

### 1. Gereksinimler

* JDK 25
* Lokal PostgreSQL 18+
* Maven 4.0.0

### 2. PostgreSQL Kurulumu

Lokal PostgreSQL sunucunuzda, `application.properties` dosyanızla eşleşen bir veritabanı ve kullanıcı oluşturmanız gerekmektedir.

Örnek komutlar (veya pgAdmin ile manuel olarak):
```sql
CREATE DATABASE social_db;
CREATE USER social_user WITH ENCRYPTED PASSWORD 'social_pass';
GRANT ALL PRIVILEGES ON DATABASE social_db TO social_user;
````

### 3\. Konfigürasyon

Projenin `src/main/resources/application.properties` dosyasını kendi lokal ayarlarınıza göre düzenleyin.

**Örnek `application.properties`:**

server.port=8080
spring.application.name=social
# DATABASE CONFIGURATION
spring.datasource.url=jdbc:postgresql://localhost:5432/social_db
spring.datasource.username=postgres
spring.datasource.password=569748Tm
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/HIBERNATE CONFIGURATION
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# FLYWAY CONFIGURATION
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB


### 4\. Çalıştırma

Uygulama, Maven kullanılarak terminalden çalıştırılabilir:

```bash
# Projeyi derle, paketle ve çalıştır
mvn clean package && java -jar target/social-0.0.1-SNAPSHOT.jar
```

Veya IntelliJ IDEA üzerinden `SocialApplication.java`'yı çalıştırın.

Uygulama ilk kez başladığında, **Flyway** (`V1__init_schema.sql`) otomatik olarak veritabanı şemasını (tüm tabloları) kuracak ve **AdminSeeder** (`bootstrap/AdminSeeder.java`) `app.admin` ayarlarını kullanarak `ADMIN` kullanıcısını oluşturacaktır.

-----

## 🔐 Kimlik Doğrulama (Opaque Token)

Spring Security kullanılmadan, `Filter` ve `ThreadLocal` tabanlı özel bir kimlik doğrulama mekanizması kurulmuştur.

  * **Signup:** Kullanıcı adı benzersizdir. Şifre, `BCrypt` ile hash'lenerek veritabanına kaydedilir.
  * **Login:** Rastgele 32-byte (256-bit) güvenli bir token üretilir. Bu token'ın `Base64URL` formatlı ham hali (`raw`) istemciye `accessToken` olarak döndürülür. Token'ın `SHA-256 hash`'i ise `expires_at` (son kullanma tarihi) ile birlikte `tokens` tablosuna kaydedilir.
  * **Yetkilendirme:** Korumalı endpoint'lere gelen her istekte `Authorization: Bearer <token>` başlığı beklenir. `AuthFilter`, bu `raw` token'ı alır, `SHA-256` hash'ini hesaplar ve veritabanında bu hash'e sahip, süresi dolmamış (`expires_at`) ve iptal edilmemiş (`revoked_at == null`) bir token arar. Bulursa, kullanıcıyı `CurrentUserHolder`'a (ThreadLocal) atar.
  * **Logout:** İlgili token'ın `revoked_at` alanını `Instant.now()` olarak güncelleyerek token'ı anında geçersiz kılar.

**Güvenlik Notu:** Veritabanında token'ın ham hali (`raw`) **asla** saklanmaz. Sadece geri döndürülemez `hash`'i saklanır.

-----

## 🧪 Hata Yanıtları (Tutarlı Format)

Case study gereği, tüm hata yanıtları (4xx ve 5xx) standart ve tutarlı bir JSON formatı döndürür. Bu, `GlobalExceptionHandler` ve `ApiError` sınıfları ile yönetilir.

**Örnek Hata Yanıtı (404):**

```json
{
  "timestamp": "2025-11-03T03:15:00.123Z",
  "path": "/api/users/999",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "User not found"
}
```

  * `timestamp`: Hatanın oluştuğu an (ISO-8601).
  * `path`: İsteğin yapıldığı API yolu.
  * `status`: HTTP statü kodu (örn: 400, 401, 403, 404).
  * `code`: Hatayı programatik olarak tanımlayan kod (örn: `NOT_FOUND`, `UNAUTHORIZED`, `BAD_REQUEST`).
  * `message`: Geliştiriciye yönelik, hatayı açıklayan net bir mesaj.

-----

## 📡 API Sözleşmesi (Özet)

Tüm korumalı uç noktalar `Authorization: Bearer {{accessToken}}` başlığını gerektirir.

### AUTH

  * `POST /api/auth/signup` (Body: `{username, password}`)
      * Yeni kullanıcı kaydı (rol varsayılan olarak `USER`).
  * `POST /api/auth/login` (Body: `{username, password}`)
      * Başarılı girişte `200 OK` ve token döner:
    <!-- end list -->
    ```json
    {
      "accessToken": "...",
      "expiresInSeconds": 3600
    }
    ```
  * `POST /api/auth/logout`
      * Aktif token'ı `revoked_at` olarak işaretleyerek geçersiz kılar.
  * `GET /api/auth/me`
      * Aktif kullanıcının `{id, username, role}` bilgilerini döner.

### USERS

  * `GET /api/users/{id}`
      * Tekil kullanıcı profilini döner (Silinmiş kullanıcılar `404` döner).
  * `PUT /api/users/me/password` (Body: `{currentPassword, newPassword}`)
      * Aktif kullanıcının şifresini günceller. Mevcut şifre doğrulaması yapılır.
      * **Güvenlik:** Başarılı olursa, o kullanıcıya ait *tüm* aktif token'ları iptal eder (tüm cihazlardan çıkış yapılır).
  * `DELETE /api/users/me`
      * Aktif kullanıcının hesabını "soft delete" (geri alınabilir silme) yapar.
      * **Güvenlik:** Kullanıcının *tüm* aktif token'larını iptal eder.
  * `DELETE /api/admin/users/{id}` (Sadece `ADMIN` rolü)
      * Belirtilen ID'ye sahip kullanıcıyı "soft delete" yapar ve tüm token'larını iptal eder.

### POSTS

  * `POST /api/posts` (Tip: `multipart/form-data`)
      * **Body:** `image` (Dosya) ve `description` (Metin, opsiyonel).
      * `201 Created` yanıtı döner.
  * `GET /api/posts/{id}`
      * Post detaylarını (yazar, sayaçlar ve yorum listesi dahil) döner.
  * `PUT /api/posts/{id}` (Tip: `multipart/form-data`) (Sadece sahibi veya `ADMIN`)
      * **Body:** `image` (Dosya, opsiyonel) ve `description` (Metin, opsiyonel).
      * Postun resmini ve/veya açıklamasını günceller.
  * `DELETE /api/posts/{id}` (Sadece sahibi veya `ADMIN`)
      * Postu "soft delete" yapar. `204 No Content` döner.
  * `POST /api/posts/{id}/view`
      * Postun `view_count` sayacını +1 artırır.
  * `GET /api/posts`
      * Tüm aktif postları listeler (yorumlar hariç).

### COMMENTS

  * `POST /api/posts/{id}/comments` (Body: `{content}`)
      * İlgili posta yorum ekler.
  * `GET /api/posts/{id}/comments`
      * İlgili postun yorumlarını listeler.
  * `DELETE /api/comments/{commentId}` (Sadece yorum sahibi, post sahibi veya `ADMIN`)
      * Yorumu "soft delete" yapar. `204 No Content` döner.

### LIKES

  * `POST /api/posts/{id}/likes`
      * Postu beğenir (Kullanıcı başına tek beğeni, idempotent).
  * `DELETE /api/posts/{id}/likes`
      * Beğeniyi geri alır. `204 No Content` döner.

-----

## 🖼️ Dosya Yükleme (Resimler)

  * Yüklemeler, proje kök dizininde (working directory) oluşturulan `uploads/` klasörüne kaydedilir.
  * `WebConfig.java`, bu klasörü `/files/**` URL'i altında web'e sunar.
  * Dosyalar, güvenlik ve çakışmaları önlemek için `UUID` ile yeniden adlandırılır ve `yyyy-MM` (örn: `uploads/2025-11/`) şeklinde tarih bazlı alt klasörlerde saklanır.
  * `image_path` olarak veritabanına `FileStorageService` tarafından üretilen tam URL yolu (örn: `/files/2025-11/uuid.png`) kaydedilir.

-----

## ✅ Varsayımlar & Kısıtlamalar

  * **Opaque Token:** Case study "DB'de aktif olarak saklanır" ve "logout ile sonlandırılır" dediği için, `stateless` JWT yerine veritabanı destekli `stateful` (durumlu) Opaque Token mimarisi tercih edilmiştir.
  * **Soft Delete:** Veri bütünlüğünü korumak (örn: bir kullanıcı silinse bile eski yorumlarının 'Bilinmeyen Kullanıcı' olarak kalabilmesi) ve geri almayı kolaylaştırmak için `User`, `Post` ve `Comment` silme işlemleri `deleted=true` bayrağı ile "soft delete" olarak uygulanmıştır. Bu yaklaşım, veritabanı seviyesinde fiziksel silme (`DELETE FROM ...`) ve `ON DELETE CASCADE` kurallarını kullanmaya **bilinçli olarak tercih edilmiştir**. Çünkü fiziksel silme (hard delete), denetim (auditing) ve geri alma imkanını ortadan kaldırır.
  * **Mapping:** DTO (`record`) ve Entity (`@Entity`) dönüşümleri, projenin basitliği nedeniyle `MapStruct` gibi bir kütüphane olmadan, manuel olarak yapılmıştır.
  * **Dosya Temizliği:** Post güncellendiğinde veya silindiğinde, `uploads/` klasöründeki eski/yetim kalan resim dosyaları *silinmez*. Bu, basitlik için alınmış bir karardır (Gerçek bir uygulamada bu işlem bir "garbage collector" veya CDN lifecycle kuralı ile yönetilmelidir).
  * **Limitler:** Güvenlik için `application.properties`'de 5MB dosya yükleme limiti belirlenmiştir, ancak API "rate limiting" (örn: brute-force login denemelerini engelleme) içermemektedir.

<!-- end list -->

```
```