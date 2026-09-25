"""French slide content — full translation of content_en.py, same
structure (13 slides, same 'kind' sequence)."""

SLIDES = [
    {
        "kind": "title",
        "title": "Construire des agents IA avec des modèles locaux",
        "subtitle": "Spring Boot, Angular, MCP et LM Studio",
        "presenter_line": "(Votre nom — date)",
    },
    {
        "kind": "bullets",
        "title": "Ordre du jour",
        "bullets": [
            "Pourquoi l'IA locale (15 min)",
            "Qu'est-ce que MCP (15 min)",
            "Présentation de l'architecture (20 min)",
            "Démo live : CRUD + langage naturel (30 min)",
            "Optimisation des performances LM Studio (15 min)",
            "Discussion sécurité (10 min)",
            "Recommandations et questions (15 min)",
        ],
    },
    {
        "kind": "bullets",
        "title": "Pourquoi l'IA locale compte",
        "bullets": [
            "Aucune donnée ne quitte votre infrastructure — confidentialité totale pour les données sensibles",
            "Aucun coût par jeton — investissement matériel unique et prévisible",
            "Aucune latence réseau — le modèle tourne sur la même machine que l'application",
            "Contrôle total sur les versions du modèle — pas de mise à jour silencieuse côté fournisseur",
        ],
    },
    {
        "kind": "bullets",
        "title": "Qu'est-ce que MCP (Model Context Protocol)",
        "bullets": [
            "Un standard ouvert pour connecter des modèles IA à des outils et données réels",
            "Au lieu de simplement générer du texte, le modèle peut appeler des fonctions exposées par l'application",
            "L'application définit les outils (nom, description, paramètres) ; le modèle décide quand les appeler",
            "Le même protocole fonctionne avec des modèles locaux (LM Studio) et des modèles cloud (Claude, GPT)",
        ],
    },
    {
        "kind": "architecture",
        "title": "Comment les pièces s'assemblent",
        "frontend_label": "Angular 21\n(navigateur)",
        "backend_label": "Spring Boot 4.1.1",
        "lmstudio_label": "LM Studio\n(Qwen, local)",
        "rest_label": "REST + JWT",
        "mcp_label": "MCP (Streamable HTTP)",
    },
    {
        "kind": "bullets",
        "title": "Stack technique",
        "bullets": [
            "Backend : Spring Boot 4.1.1, Spring AI 2.0.1, Spring Security, H2",
            "Frontend : Angular 21, composants standalone, signals",
            "Exécution IA : LM Studio, Qwen2.5-7B-Instruct ou Qwen3-8B",
            "La même logique métier (EmployeeService) est appelée via REST et via MCP — aucun code dupliqué",
        ],
    },
    {
        "kind": "bullets",
        "title": "Démo live",
        "bullets": [
            "D'abord : CRUD classique dans l'application Angular — connexion, création, modification, suppression d'un employé",
            "Ensuite : les mêmes opérations, pilotées entièrement par des prompts en langage naturel dans LM Studio",
            "Observez l'interface Angular se mettre à jour en direct pendant que le modèle appelle les outils",
        ],
    },
    {
        "kind": "bullets",
        "title": "Exemples de prompts",
        "bullets": [
            "« Ajoute un employé Maria Ionescu, email maria.ionescu@example.com, "
            "département Marketing, salaire 6000 » (la date d'embauche est optionnelle — "
            "par défaut la date du jour)",
            "« Quels employés gagnent plus de 5000 ? »",
            "« Supprime l'employé avec l'id 3 »",
        ],
    },
    {
        "kind": "bullets",
        "title": "Optimisation des performances LM Studio",
        "bullets": [
            "Quantification : Q4_K_M est la valeur par défaut recommandée — meilleur équilibre qualité/vitesse",
            "Déchargement GPU : environ 80 % des couches sont généralement le point d'équilibre performance/coût",
            "Longueur de contexte et VRAM : le cache KV croît linéairement avec la longueur de contexte",
            "Flash Attention : peut réduire la mémoire du cache KV jusqu'à 75 %",
            "Quantification du cache KV : réduit encore la mémoire, au prix d'une légère perte de qualité",
        ],
    },
    {
        "kind": "two_column",
        "title": "Un compromis assumé — à discuter",
        "left_heading": "API REST",
        "left_bullets": ["JWT obligatoire", "ADMIN en écriture, USER en lecture seule", "Appliqué par Spring Security"],
        "right_heading": "Point d'accès MCP",
        "right_bullets": ["Aucune authentification", "Limité à localhost", "Simplification volontaire pour cette démo"],
    },
    {
        "kind": "bullets",
        "title": "Recommandations",
        "bullets": [
            "Adapté : outils internes, flux de données sensibles, automatisation agentique sur vos propres API",
            "Adapté : prototypage du comportement d'appel d'outils avant d'engager un modèle cloud",
            "Non adapté : produits grand public nécessitant la qualité de raisonnement d'un modèle frontière",
            "Non adapté : charges nécessitant une scalabilité élastique au-delà de votre propre matériel",
        ],
    },
    {
        "kind": "bullets",
        "title": "Ressources",
        "bullets": [
            "Documentation Spring AI MCP : https://docs.spring.io/spring-ai/reference/api/mcp/",
            "LM Studio : https://lmstudio.ai",
            "Angular : https://angular.dev",
            "Code source de cette démo : (ajoutez ici le lien vers votre dépôt)",
        ],
    },
    {
        "kind": "closing",
        "title": "Merci !",
        "subtitle": "Questions ?",
    },
]
