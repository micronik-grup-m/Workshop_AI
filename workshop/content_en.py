"""English slide content — 13 slides matching the spec's outline exactly."""

SLIDES = [
    {
        "kind": "title",
        "title": "Building AI Agents with Local Models",
        "subtitle": "Spring Boot, Angular, MCP & LM Studio",
        "presenter_line": "(Your name — date)",
    },
    {
        "kind": "bullets",
        "title": "Agenda",
        "bullets": [
            "Why local AI (15 min)",
            "What is MCP (15 min)",
            "Architecture walkthrough (20 min)",
            "Live demo: CRUD + natural language (30 min)",
            "LM Studio performance tuning (15 min)",
            "Security discussion (10 min)",
            "Recommendations & Q&A (15 min)",
        ],
    },
    {
        "kind": "bullets",
        "title": "Why Local AI Matters",
        "bullets": [
            "No data leaves your infrastructure — full confidentiality for sensitive business data",
            "No per-token API costs — predictable, one-time hardware investment",
            "No network latency — the model runs on the same machine as your app",
            "Full control over model versions and behavior — no silent vendor updates",
        ],
    },
    {
        "kind": "bullets",
        "title": "What is MCP (Model Context Protocol)",
        "bullets": [
            "An open standard for connecting AI models to real tools and data",
            "Instead of just generating text, the model can call functions your application exposes",
            "The application defines the tools (name, description, parameters); the model decides when to call them",
            "Same protocol works with local models (LM Studio) and cloud models (Claude, GPT)",
        ],
    },
    {
        "kind": "architecture",
        "title": "How the Pieces Fit Together",
        "frontend_label": "Angular 21\n(browser)",
        "backend_label": "Spring Boot 4.1.1",
        "lmstudio_label": "LM Studio\n(Qwen, local)",
        "rest_label": "REST + JWT",
        "mcp_label": "MCP (Streamable HTTP)",
    },
    {
        "kind": "bullets",
        "title": "Tech Stack",
        "bullets": [
            "Backend: Spring Boot 4.1.1, Spring AI 2.0.1, Spring Security, H2",
            "Frontend: Angular 21, standalone components, signals",
            "AI runtime: LM Studio, Qwen2.5-7B-Instruct or Qwen3-8B",
            "The same business logic (EmployeeService) is called through both REST and MCP — no duplicated code",
        ],
    },
    {
        "kind": "bullets",
        "title": "Live Demo",
        "bullets": [
            "First: normal CRUD in the Angular app — login, create, edit, delete an employee",
            "Then: the same operations, driven entirely by natural-language prompts in LM Studio",
            "Watch the Angular UI update live as the model calls the tools",
        ],
    },
    {
        "kind": "bullets",
        "title": "Example Prompts",
        "bullets": [
            "\"Add an employee Maria Ionescu, email maria.ionescu@example.com, "
            "department Marketing, salary 6000\" (hire date is optional — defaults to today)",
            "\"Which employees earn more than 5000?\"",
            "\"Delete the employee with id 3\"",
        ],
    },
    {
        "kind": "bullets",
        "title": "LM Studio Performance Tuning",
        "bullets": [
            "Quantization: Q4_K_M is the recommended default — best balance of quality and speed",
            "GPU offload: around 80% of layers is usually the performance/cost sweet spot",
            "Context length vs. VRAM: the KV cache grows linearly with context length",
            "Flash Attention: can cut KV cache memory by up to 75%",
            "KV-cache quantization: further reduces memory at a small quality cost",
        ],
    },
    {
        "kind": "two_column",
        "title": "A Deliberate Trade-off — Worth Discussing",
        "left_heading": "REST API",
        "left_bullets": ["JWT required", "ADMIN can write, USER read-only", "Enforced by Spring Security"],
        "right_heading": "MCP Endpoint",
        "right_bullets": ["No authentication", "Bound to localhost only", "Deliberate simplification for this demo"],
    },
    {
        "kind": "bullets",
        "title": "Recommendations",
        "bullets": [
            "Good fit: internal tools, data-sensitive workflows, agentic automation over your own APIs",
            "Good fit: prototyping tool-calling behavior before committing to a cloud model",
            "Not a fit: consumer-facing products needing frontier-model reasoning quality",
            "Not a fit: workloads needing elastic scale beyond your own hardware",
        ],
    },
    {
        "kind": "bullets",
        "title": "Resources",
        "bullets": [
            "Spring AI MCP docs: https://docs.spring.io/spring-ai/reference/api/mcp/",
            "LM Studio: https://lmstudio.ai",
            "Angular: https://angular.dev",
            "This demo's source code: (add your repository link here)",
        ],
    },
    {
        "kind": "closing",
        "title": "Thank You!",
        "subtitle": "Questions?",
    },
]
