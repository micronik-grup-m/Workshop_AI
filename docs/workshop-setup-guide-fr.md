# Guide de préparation — Agents IA avec modèles locaux

Ce guide vous prépare pour l'atelier : une application de démonstration
Spring Boot + Angular (CRUD), pilotable à la fois via l'interface web
classique et via des prompts en langage naturel envoyés à un modèle IA
local par MCP (Model Context Protocol).

## Prérequis

- Java 21 (JDK)
- Apache Maven 3.9+
- Node.js 20+ (pour le frontend Angular)
- [LM Studio](https://lmstudio.ai) installé
- ~8 Go d'espace disque libre pour les modèles IA (chat + embedding)

## 1. Télécharger le modèle dans LM Studio

1. Ouvrez LM Studio et allez dans l'onglet recherche/téléchargement.
2. Cherchez `Qwen2.5-7B-Instruct` (ou `Qwen3-8B` si votre machine dispose
   de plus de VRAM).
3. Téléchargez la version quantifiée `Q4_K_M` — le meilleur compromis
   qualité/vitesse pour un ordinateur portable.
4. Chargez le modèle dans l'onglet chat de LM Studio une fois le
   téléchargement terminé.
5. Cherchez et téléchargez également `nomic-embed-text-v1.5` (GGUF) — un
   modèle séparé, bien plus petit, utilisé uniquement pour la
   fonctionnalité de recherche PDF (section 6 ci-dessous). Chargez-le dans
   l'onglet **embedding** de LM Studio, pas dans l'onglet chat — les
   modèles d'embedding se chargent et s'utilisent différemment des
   modèles de chat dans LM Studio.

## 2. Démarrer le backend

Depuis la racine du projet :

```bash
mvn spring-boot:run
```

Attendez que l'application ait démarré sur le port `8080`. Deux comptes
sont créés automatiquement :

| Utilisateur | Mot de passe | Rôle  |
|-------------|--------------|-------|
| `admin`     | `admin123`   | ADMIN |
| `user`      | `user123`    | USER  |

## 3. Démarrer le frontend

Dans un second terminal, depuis le dossier `frontend/` :

```bash
npm install
npx ng serve
```

(`npm install` n'est nécessaire qu'une seule fois, ou après avoir
récupéré des changements touchant `frontend/package.json`.)

Ouvrez `http://localhost:4200` dans votre navigateur et connectez-vous
avec l'un des comptes ci-dessus.

## 4. Connecter LM Studio à la démo via MCP

Dans LM Studio, ouvrez les paramètres MCP/intégrations et ajoutez un
nouveau serveur MCP pointant vers :

```
http://localhost:8080/mcp
```

LM Studio doit signaler qu'il a découvert plusieurs outils
(`listEmployees`, `getEmployee`, `searchBySalaryRange`, `createEmployee`,
`updateEmployee`, `deleteEmployee`, `searchDocuments`, `listDocuments`).

## 5. Prompts à essayer

La base de données démarre vide (aucun employé préchargé), donc ces
prompts s'appuient les uns sur les autres, dans l'ordre :

1. « Ajoute un employé Maria Ionescu, email maria.ionescu@example.com,
   département Marketing, salaire 6000 » (la date d'embauche est
   optionnelle — par défaut la date du jour si vous ne la mentionnez pas)
2. « Ajoute un employé Ion Popescu, email ion.popescu@example.com,
   département IT, salaire 5500 »
3. « Quels employés gagnent plus de 5000 ? »
4. « Liste tous les employés du département IT »
5. « Mets à jour le salaire de Maria Ionescu à 7000 »
6. « Supprime l'employé avec l'id 2 » (vérifiez l'id réel affiché dans
   l'interface Angular ou dans la réponse du modèle — les id sont
   attribués par la base de données dans l'ordre de création, ne
   supposez pas un numéro fixe)

Observez l'application Angular (toujours ouverte dans votre navigateur) —
les changements y apparaissent immédiatement, puisque l'interface et le
modèle IA agissent sur les mêmes données via le même backend.

## 6. Essayer le RAG : charger un PDF et l'interroger

Cette fonctionnalité nécessite la pile Docker (`docker compose up --build`),
pas `mvn spring-boot:run` — l'index de recherche PDF vit dans Postgres
avec l'extension `pgvector`, qui n'a pas d'équivalent sur la base H2
locale par défaut.

1. Connectez-vous en tant qu'`admin` (seuls les admins peuvent charger des
   documents) et récupérez un jeton :
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
   ```
2. Chargez un PDF quelconque (remplacez le chemin par un fichier réel sur
   votre machine) :
   ```bash
   curl -X POST http://localhost:8080/api/documents \
     -H "Authorization: Bearer $TOKEN" \
     -F "file=@/chemin/vers/votre-document.pdf"
   ```
   Le chargement est limité à 25 Mo. Notez aussi qu'un PDF scanné/image
   (sans couche de texte extractible) n'indexera aucun texte cherchable.
3. Dans LM Studio, avec les deux modèles chargés (Qwen pour le chat,
   appel d'outils activé ; `nomic-embed-text-v1.5` pour les embeddings)
   et le serveur MCP toujours connecté, essayez :
   - « Liste les documents auxquels tu as accès » (appelle `listDocuments`)
   - « Cherche dans les documents <un sujet présent dans votre PDF> »
     (appelle `searchDocuments`)

Observez la réponse du modèle — elle devrait citer le nom du fichier et
le numéro de page en plus de la réponse, puisque c'est ce que retourne
`searchDocuments`.

## Dépannage

- **Le port 8080 ou 4200 est déjà utilisé** — arrêtez ce qui tourne déjà
  sur ce port, ou modifiez `server.port` dans `application.yml` /
  l'option `--port` du serveur de développement Angular.
- **LM Studio n'appelle aucun outil** — vérifiez que le modèle a été
  chargé avec l'appel de fonctions/outils activé, et que l'URL du serveur
  MCP est exactement `http://localhost:8080/mcp` (attention aux barres
  obliques finales).
- **Un prompt échoue avec une erreur de validation** — le modèle a
  peut-être produit une valeur invalide (par ex. un email mal formé).
  Reformulez le prompt en précisant chaque champ plus explicitement.
- **Un prompt sur un intervalle de salaire comme « qui gagne plus de
  5000 ? » a besoin à la fois d'un minimum et d'un maximum** — le modèle
  doit inventer une borne supérieure puisque l'outil en exige une ; s'il
  semble bloqué, donnez-lui les deux valeurs explicitement (« entre 5000
  et 100000 »).
