# Application architecture

## Dependency direction

```text
app (navigation, authenticated-action coordination, dependency composition)
 ├─ feature:home / feature:projects / feature:settings / feature:auth
 │   ├─ core:domain (repository contracts, business use cases)
 │   ├─ core:model (models, session states, typed errors)
 │   └─ core:designsystem (Compose components, localized error presentation)
 └─ core:data (repository implementations and Hilt bindings)
     ├─ core:domain
     ├─ core:network (Retrofit, response validation, session transport/storage adapter)
     └─ core:datastore (preferences)
```

Feature ViewModels depend on domain contracts, not Retrofit, CookieJar or repository implementations. `app` owns dependency composition; it must not submit HTTP requests. DTOs are consumed only inside the network/data boundary and mapped to `core:model` before reaching features. Settings retains a typealias for source compatibility; new callers import the domain contract.

## Authentication responsibilities

- `SessionManager` implements `SessionRepository` and projects encrypted session snapshots into `Restoring`, `SignedOut`, `SignedIn` or `Unavailable`.
- `LoginViewModel` owns credential validation, registration and login submission. `feature:auth` owns the form and its localized text. Registration success is remembered separately so a failed automatic login does not re-register an existing account.
- `AuthViewModel` owns app-level login requests and authenticated-action coordination. It does not own credentials or submit login requests.
- `CollectArticleUseCase` checks restored session state and calls the collection contract. Repository implementations serialize login/logout/collection operations with a shared mutex.
- `PendingAuthActionStore` stores one versioned, serialized `LoginRequest` containing a strong action type, replacing separate `pending_id`, `pending_desired` and login flags.

Clicking collect attempts the requested final state. If authentication is needed, the coordinator stores `CollectArticle(articleId, collected)` and a unique request ID. Navigation presents `Login(requestId)`. Success consumes only the matching request and continues that action. Cancellation discards it. Avatar login has no action. Repeated callbacks cannot consume the same request twice.

The continuation is not a durable background job: a process crash after consumption but before the server response can leave the result unknown. Do not claim server-side exactly-once semantics. Adding durable replay requires a server idempotency contract and an outbox; the current API does not supply that contract.

## Session and transport rules

`SessionStorage` exposes immutable snapshots and revision-checked updates. `EncryptedSessionStorage` handles Android Keystore AES-GCM and atomic file persistence outside Android backup. It can read the earlier session file format (missing version defaults to 1). Unreadable/invalid-key sessions require login again. Ordinary I/O failures are exposed as storage errors rather than silently overwritten.

Storage initialization runs on an IO dispatcher. No file or Keystore loading occurs while a ViewModel is constructed or while the UI composes. `SessionManager`'s scope intentionally lives for the application's lifetime.

`SessionInterceptor` attaches matching cookies only to the canonical HTTPS API host. It saves cookies only if the request's session generation is still current. Automatic API redirects are disabled so session cookies cannot be forwarded to another host. Image requests use their own client and never receive account cookies. Password form values are held only in memory; cookies that may contain credentials are encrypted and must never be logged.

`ApiExecutor` is the sole envelope validation path for banner, article, project and authentication APIs. It normalizes business errors, malformed responses, HTTP failures, timeouts and network failures. `-1001` and HTTP 401 invalidate the corresponding session generation; late responses cannot invalidate a newer account. Cancellation always propagates. Do not implement generic automatic retries for account writes.

Local `SignedIn` means a restored session exists; only the server can authoritatively validate it. Protected requests handle expiration centrally. Logout removes local credentials even when server logout fails; the UI can still report the network failure.

## UI rules

- UI state is exposed as read-only `StateFlow`; mutation stays inside its owner.
- User-facing strings belong in resource files. `AppError` contains semantic failures; `core:designsystem` maps them to localized text.
- Notifications carry IDs and remain state until acknowledged, avoiding replay/removal races.
- Main tabs share the main navigation entry's ViewModel lifetime and use distinct saveable-state keys. Login and settings remain separate entries.
- New authenticated actions extend `PendingAuthAction` and the coordinator's exhaustive handler rather than adding more loosely coupled saved-state fields.

## Verification for this refactor

Per request, tests were not changed or executed. Build and production-source Lint commands:

```text
gradlew.bat :app:assembleDebug
gradlew.bat :app:lintDebug -x lintAnalyzeDebugAndroidTest -x lintAnalyzeDebugUnitTest
```

Existing auth/session tests refer to the previous coordinator/repository constructors and cookie implementation; migrate those fixtures to the new contracts before re-enabling that suite. Compile/Lint do not replace runtime acceptance, real-account integration, release verification or a security review.
