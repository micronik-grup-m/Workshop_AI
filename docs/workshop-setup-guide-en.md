# Workshop Setup Guide — AI Agents with Local Models

This guide gets you ready for the workshop: a demo Spring Boot + Angular
CRUD application, controlled both through the normal web UI and through
natural-language prompts sent to a local AI model via MCP (Model Context
Protocol).

## Prerequisites

- Java 21 (JDK)
- Apache Maven 3.9+
- Node.js 20+ (for the Angular frontend)
- [LM Studio](https://lmstudio.ai) installed
- ~8 GB of free disk space for the AI models (chat + embedding)

## 1. Download the Model in LM Studio

1. Open LM Studio and go to the search/download tab.
2. Search for `Qwen2.5-7B-Instruct` (or `Qwen3-8B` if your machine has
   more VRAM).
3. Download the `Q4_K_M` quantized version — the best balance of quality
   and speed for a laptop.
4. Load the model in LM Studio's chat tab once the download finishes.
5. Also search for and download `nomic-embed-text-v1.5` (GGUF) — this is
   a separate, much smaller model used only for the PDF search feature
   (section 6 below). Load it in LM Studio's **embedding** tab, not the
   chat tab — embedding models are loaded and used differently from chat
   models in LM Studio.

## 2. Start the Backend

From the project root:

```bash
mvn spring-boot:run
```

Wait until you see the application has started on port `8080`. Two
accounts are seeded automatically:

| Username | Password   | Role  |
|----------|------------|-------|
| `admin`  | `admin123` | ADMIN |
| `user`   | `user123`  | USER  |

## 3. Start the Frontend

In a second terminal, from the `frontend/` directory:

```bash
npm install
npx ng serve
```

(`npm install` is only needed once, or after pulling changes that touch
`frontend/package.json`.)

Open `http://localhost:4200` in your browser and log in with either
account above.

## 4. Connect LM Studio to the Demo via MCP

In LM Studio, open the MCP / integrations settings and add a new MCP
server pointing at:

```
http://localhost:8080/mcp
```

LM Studio should report that it discovered several tools
(`listEmployees`, `getEmployee`, `searchBySalaryRange`, `createEmployee`,
`updateEmployee`, `deleteEmployee`, `searchDocuments`, `listDocuments`).

## 5. Prompts to Try

The database starts empty (no employees are pre-seeded), so these prompts
build on each other in order:

1. "Add an employee Maria Ionescu, email maria.ionescu@example.com,
   department Marketing, salary 6000" (the hire date is optional — it
   defaults to today if you don't mention one)
2. "Add an employee Ion Popescu, email ion.popescu@example.com,
   department IT, salary 5500"
3. "Which employees earn more than 5000?"
4. "List all employees in the IT department"
5. "Update Maria Ionescu's salary to 7000"
6. "Delete the employee with id 2" (check the actual id shown in the
   Angular UI or in the model's own response — ids are assigned by the
   database in creation order, so don't assume a fixed number)

Watch the Angular app (still open in your browser) — the changes appear
there immediately, since both the UI and the AI model are operating on
the same data through the same backend.

## 6. Try RAG: Upload a PDF and Query It

This feature requires the Docker stack (`docker compose up --build`), not
`mvn spring-boot:run` — the PDF search index lives in Postgres with the
`pgvector` extension, which has no equivalent on the default local H2
database.

1. Log in as `admin` (only admins can upload documents) and get a token:
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
   ```
2. Upload any PDF (replace the path with a real file on your machine):
   ```bash
   curl -X POST http://localhost:8080/api/documents \
     -H "Authorization: Bearer $TOKEN" \
     -F "file=@/path/to/your-document.pdf"
   ```
   Uploads are limited to 25MB. Also note that a scanned/image-only PDF
   (no extractable text layer) won't index any searchable text.
3. In LM Studio, with both models loaded (Qwen for chat, tool-calling
   enabled; `nomic-embed-text-v1.5` for embeddings) and the MCP server
   still connected, try:
   - "List the documents you have access to" (calls `listDocuments`)
   - "Search the documents for <a topic you know is in your PDF>" (calls
     `searchDocuments`)

Watch the model's response — it should quote the filename and page number
alongside the answer, since that's what `searchDocuments` returns.

## Troubleshooting

- **Port 8080 or 4200 already in use** — stop whatever else is running on
  that port, or edit `server.port` in `application.yml` / the Angular
  dev server's `--port` flag.
- **LM Studio doesn't call any tools** — confirm the model was loaded
  with tool-calling/function-calling enabled, and that the MCP server URL
  is exactly `http://localhost:8080/mcp` (watch for trailing-slash
  mismatches).
- **A prompt fails with a validation error** — the model may have
  produced an invalid value (e.g. a malformed email). Try rephrasing the
  prompt to be more explicit about each field.
- **A salary-range prompt like "which employees earn more than 5000?"
  needs both a min and a max** — the model has to invent an upper bound
  since the tool requires one; if it seems stuck, give it both explicitly
  ("between 5000 and 100000").
