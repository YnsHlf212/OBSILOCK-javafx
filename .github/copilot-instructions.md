<!-- Copilot / AI agent instructions for the Coffre‑fort JavaFX client -->

# Quick Orientation

This repo contains a small JavaFX sample client (educational MVP) for a "coffre‑fort numérique". Focus your changes in:

- `src/main/java/com/coffrefort/client` — application entrypoints and UI wiring
- `src/main/resources/com/coffrefort/client` — FXML screens (`login.fxml`, `main.fxml`)
- `src/main/java/com/coffrefort/client/model` — simple data models (intent: `FileEntry`, `NodeItem`, `Quota`)
- `pom.xml` — Maven + `javafx-maven-plugin` configuration; main launcher set to `com.coffrefort.client.Launcher`

Key runtime commands:

- From terminal (recommended):
  - `mvn clean javafx:run` — builds and runs the JavaFX sample (plugin uses `Launcher` mainClass)
- From IDE: run `com.coffrefort.client.Launcher` (not `App`) to avoid "JavaFX runtime components missing" errors.

**Dependencies to be aware of**: JavaFX 21 (module path managed by Maven plugin), Jackson (`jackson-databind`) and OkHttp (HTTP client).

**Important runtime note**: The sample `ApiClient` defaults to `http://localhost:8888/`. Either start a backend that implements the small API contract or adapt `ApiClient.setBaseUrl(...)` during tests.

**Quick API shapes discovered in code**:

- Login request body: `{ "email": "...", "password": "..." }` → success response expected to contain `{ "token": "<jwt>" }`.
- Files endpoint variations: handlers accept either an array of file objects or `{ "files": [...] }` or `{ "data": [...] }`.

---

**Project-specific patterns & conventions**

- Controller injection: `App` uses `FXMLLoader#setControllerFactory(...)` to construct controllers and inject a shared `ApiClient` instance and callbacks. When adding new controllers follow the same factory pattern.
- UI wiring: FXML files live under resources and are loaded via `getResource("/com/coffrefort/client/<name>.fxml")`. Keep resource paths exact.
- Simulated behaviour: Many `ApiClient` methods and `MainController` flows are intentionally stubbed/simulated (look for `TODO: Remplacer par un vrai appel API` or `// TODO`). Implementations should replace these simulated methods rather than change UI wiring.
- Error messages and comments are mostly in French — prefer keeping consistency when adding UI strings or log messages.

---

**Files that matter for common tasks**

- `pom.xml` — build/run via Maven plugin; JDK 17+ expected.
- `src/main/java/com/coffrefort/client/Launcher.java` — program entry (use this in IDE)
- `src/main/java/com/coffrefort/client/App.java` — JavaFX `Application` subclass and controller factory wiring
- `src/main/java/com/coffrefort/client/ApiClient.java` — current HTTP client; contains simulated `listRoot()` and `getQuota()`; real HTTP methods exist for login and listFiles().
- `src/main/java/com/coffrefort/client/controllers/*.java` — `LoginController` (async login via Task), `MainController` (UI interactions, many TODOs for API calls)
- `src/main/resources/com/coffrefort/client/*.fxml` — UI layout files

---

**Discovered issues / places to double-check before coding**

- `src/main/java/com/coffrefort/client/model/FileEntry.java` currently appears to contain duplicated `NodeItem` code (same class content as `NodeItem.java`). This is likely a copy/paste error; fix or implement `FileEntry` model before adding features that depend on it.
- Several controller actions are simulated (create/rename/delete folder, upload, download, share). The safe approach: implement API methods in `ApiClient` first and then call them from controllers.

---

**How to implement a backend integration task**

1. Add/implement the required endpoint call in `ApiClient` (use OkHttp + Jackson patterns already present). Respect the token header `Authorization: Bearer <token>`; reuse existing `login(...)` pattern for error handling.
2. Replace the simulated methods (`listRoot`, `getQuota`, or TODOs in `MainController`) with calls to the new `ApiClient` methods. Keep UI interactions on the JavaFX thread — use `Task` as in `LoginController` for blocking calls.
3. Keep FXML and controller method signatures unchanged where possible — controllers expect `ApiClient` injection via `App`'s controller factory.

Example: add `public List<NodeItem> listRootFromServer() throws IOException` in `ApiClient` and call it from `MainController.loadData()` inside a background `Task` then update the UI on success.

---

**Testing & debugging tips**

- If JavaFX UI fails to start with missing runtime: run `mvn clean javafx:run` (plugin supplies JavaFX on module-path).
- To simulate backend failures quickly, change `ApiClient.baseUrl` to an invalid host/port and observe the error handling messages in `LoginController`.
- Use `System.err.println(...)` or `e.printStackTrace()` for quick traces — existing code uses both.

---

If anything below is unclear or you want this tailored (e.g., an expanded checklist for implementing API endpoints, or a suggested `FileEntry` model to replace the broken file), tell me what to add and I'll iterate.
