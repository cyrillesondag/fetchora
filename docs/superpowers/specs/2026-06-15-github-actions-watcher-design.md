# Design : GitHub Actions Watcher Agent

**Date :** 2026-06-15
**Auteur :** cyrillesondag
**Statut :** Approuvé

## Objectif

Un agent Claude Code planifié qui surveille chaque semaine les repos GitHub actifs, crée automatiquement des PRs pour corriger les workflows en échec, et capitalise le feedback laissé en commentaire dans une mémoire persistante par repo.

---

## Section 1 — Architecture globale

L'agent est une routine Claude Code planifiée (`/schedule`) qui s'exécute chaque lundi à 8h. Il tourne dans le cloud Anthropic avec accès aux outils (`gh`, Bash).

```
[Lundi 8h]
     │
     ▼
1. Scan repos actifs (pushedAt < 90j, non archivés)
     │
     ▼
2. Pour chaque repo : vérifier le dernier run de workflow
     │
     ├─ Run OK → skip
     │
     └─ Run KO ──► 3. Analyser le log d'échec
                        │
                        ├─ Fix dans .github/workflows/*.yml ──► Créer PR (label: github-watcher)
                        │
                        └─ Fix hors workflows ──► Ouvrir Issue avec proposition de correction
     │
     ▼
4. PRs ouvertes avec label [github-watcher]
   → Lire commentaires : author == "cyrillesondag" uniquement
     │
     ▼
5. Extraire feedback → append dans cyrillesondag/claude-memory/<repo>.md
```

---

## Section 2 — Comportement détaillé des composants

### 2.1 Scan des repos actifs

```bash
gh repo list cyrillesondag --limit 100 --json name,pushedAt,isArchived
```

Critères de sélection :
- `pushedAt` dans les 90 derniers jours
- `isArchived == false`

### 2.2 Vérification des workflows

Pour chaque repo actif :

```bash
gh run list --repo cyrillesondag/<repo> --limit 1 \
  --json status,conclusion,databaseId,workflowName
```

- Si `conclusion == "failure"` → passer à l'analyse
- Si un PR avec label `github-watcher` est déjà ouvert sur ce repo → skip (pas de doublon)
- Si `status == "in_progress"` → ignoré

### 2.3 Analyse du log d'échec

```bash
gh run view <databaseId> --repo cyrillesondag/<repo> --log-failed
```

Claude analyse le log et identifie la cause racine.

**Périmètre de fix** : uniquement les fichiers `.github/workflows/*.yml`.

- Si le fix est dans les workflows → créer une PR
- Si le fix nécessite du code applicatif → ouvrir une Issue avec proposition

### 2.4 Création de PR

- **Branche :** `fix/watcher-<workflow-name>-<YYYY-MM-DD>`
- **Titre :** `[github-watcher] fix: <description courte>`
- **Body :** cause identifiée + changement appliqué + lien vers le run échoué
- **Label :** `github-watcher`

### 2.5 Ouverture d'Issue (si fix hors périmètre)

L'Issue contient :
- La cause identifiée
- Une proposition de correction détaillée (diff ou étapes concrètes) en texte, sans modification du code
- Le lien vers le run échoué

### 2.6 Lecture des commentaires et mise à jour mémoire

```bash
gh pr list --repo cyrillesondag/<repo> --label github-watcher --state open \
  --json number,title
gh api /repos/cyrillesondag/<repo>/issues/<n>/comments
```

Filtrage : `user.login == "cyrillesondag"` uniquement. Les commentaires d'autres utilisateurs sont ignorés.

---

## Section 3 — Format de la mémoire

### Emplacement

Repo privé : `cyrillesondag/claude-memory`
Créé automatiquement à la première exécution s'il n'existe pas.

Un fichier par repo surveillé : `<repo>.md`

### Format d'une entrée

```markdown
## 2026-06-15 — PR #42 fix/watcher-ci-release-2026-06-15
**Contexte :** Workflow `ci-release.yml` échouait sur l'étape sign APK.
**Ton commentaire :** "Ne pas utiliser base64 inline, passer par un fichier temporaire."
**Note retenue :** Préférer un fichier temporaire pour les secrets encodés en base64 dans les workflows Android.

---
```

### Règles

- Les entrées sont **appendées à la fin du fichier** (ordre chronologique croissant)
- Une entrée = une PR avec au moins un commentaire de `cyrillesondag`
- Si une PR n'a aucun commentaire de `cyrillesondag`, elle n'est pas enregistrée

---

## Section 4 — Gestion des cas limites

| Situation | Comportement |
|---|---|
| Repo sans workflow | Ignoré silencieusement |
| PR `[github-watcher]` déjà ouverte sur ce repo | Skip — pas de doublon |
| Log d'échec illisible / trop long | Issue avec la portion disponible du log |
| Fix workflow identifié mais PR échoue à la création | Retry une fois, sinon Issue |
| Repo `claude-memory` absent | Créé automatiquement (privé) à la première exécution |
| Aucun commentaire de `cyrillesondag` sur les PRs | Aucune entrée mémoire, pas d'erreur |
| Run encore `in_progress` | Ignoré — on ne traite que `conclusion == "failure"` |

---

## Contraintes techniques

- **Outil d'exécution :** `gh` CLI (authentifié via token GitHub dans les secrets de la routine)
- **Périmètre de modification :** uniquement `.github/workflows/*.yml`
- **Identification des PRs agent :** label `github-watcher`
- **Identification des commentaires utilisateur :** `user.login == "cyrillesondag"`
- **Planification :** chaque lundi à 8h (`0 8 * * 1`)
