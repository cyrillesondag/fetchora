# GitHub Actions Watcher — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Mettre en place une routine Claude Code planifiée qui surveille les workflows GitHub en échec, crée des PRs ou Issues de correction, et capitalise le feedback de l'utilisateur dans une mémoire persistante par repo.

**Architecture :** Agent cloud (`/schedule`, lundi 8h) qui utilise `gh` CLI pour scanner les repos actifs, analyser les logs d'échec, créer des PRs bornées aux fichiers `.github/workflows/*.yml`, et append les notes de feedback dans `cyrillesondag/claude-memory/<repo>.md`.

**Tech Stack :** `gh` CLI, GitHub API, Bash, Claude Code `/schedule`

---

## Structure des fichiers

| Fichier | Rôle |
|---|---|
| `scripts/github-watcher-bootstrap.sh` | Setup one-time : créer le repo mémoire + labels |
| `docs/superpowers/routines/github-actions-watcher.md` | Prompt complet de la routine hebdomadaire |
| `~/.claude/projects/.../memory/github-watcher-reference.md` | Entrée mémoire Claude Code pointant vers le repo GitHub |

---

## Task 1 : Script de bootstrap

**Goal :** Script idempotent qui crée le repo `cyrillesondag/claude-memory` (privé) et ajoute le label `github-watcher` sur tous les repos actifs (pushedAt < 90j).

**Files :**
- Create : `scripts/github-watcher-bootstrap.sh`

**Acceptance Criteria :**
- [ ] Le repo `cyrillesondag/claude-memory` existe et est privé après exécution
- [ ] Le label `github-watcher` (couleur `#0075ca`) existe sur tous les repos actifs
- [ ] Le script est idempotent (ré-exécutable sans erreur)

**Verify :** `bash scripts/github-watcher-bootstrap.sh` → aucune erreur, puis `gh repo view cyrillesondag/claude-memory` → repo visible

**Steps :**

- [ ] **Step 1 : Écrire le script**

```bash
#!/usr/bin/env bash
set -euo pipefail

GITHUB_USER="cyrillesondag"
MEMORY_REPO="$GITHUB_USER/claude-memory"
LABEL_NAME="github-watcher"
LABEL_COLOR="0075ca"
LABEL_DESC="PR ou Issue créée par la routine github-actions-watcher"
CUTOFF=$(date -d '90 days ago' --iso-8601=seconds)

echo "=== Bootstrap GitHub Actions Watcher ==="

# 1. Créer le repo claude-memory s'il n'existe pas
if gh repo view "$MEMORY_REPO" &>/dev/null; then
  echo "[skip] Repo $MEMORY_REPO existe déjà"
else
  echo "[create] Création du repo $MEMORY_REPO..."
  gh repo create "$MEMORY_REPO" \
    --private \
    --description "Mémoire persistante des routines Claude Code" \
    --add-readme
  echo "[ok] Repo $MEMORY_REPO créé"
fi

# 2. Lister les repos actifs
echo ""
echo "=== Ajout du label '$LABEL_NAME' sur les repos actifs ==="
ACTIVE_REPOS=$(gh repo list "$GITHUB_USER" --limit 100 \
  --json name,pushedAt,isArchived \
  | jq -r --arg cutoff "$CUTOFF" \
    '.[] | select(.isArchived == false) | select(.pushedAt > $cutoff) | .name')

for REPO in $ACTIVE_REPOS; do
  FULL_REPO="$GITHUB_USER/$REPO"
  # Créer le label s'il n'existe pas
  if gh label list --repo "$FULL_REPO" --json name | jq -e --arg l "$LABEL_NAME" '.[] | select(.name == $l)' &>/dev/null; then
    echo "[skip] Label déjà présent sur $REPO"
  else
    gh label create "$LABEL_NAME" \
      --repo "$FULL_REPO" \
      --color "$LABEL_COLOR" \
      --description "$LABEL_DESC" 2>/dev/null || echo "[warn] Impossible de créer le label sur $REPO (permissions ?)"
    echo "[ok] Label créé sur $REPO"
  fi
done

echo ""
echo "=== Bootstrap terminé ==="
```

- [ ] **Step 2 : Rendre le script exécutable et le lancer**

```bash
chmod +x scripts/github-watcher-bootstrap.sh
bash scripts/github-watcher-bootstrap.sh
```

Résultat attendu : lignes `[ok]` ou `[skip]` pour chaque repo, aucune ligne `ERROR`.

- [ ] **Step 3 : Vérifier le repo mémoire**

```bash
gh repo view cyrillesondag/claude-memory --json name,isPrivate,description
```

Résultat attendu :
```json
{"name":"claude-memory","isPrivate":true,"description":"Mémoire persistante des routines Claude Code"}
```

- [ ] **Step 4 : Vérifier un label sur un repo actif**

```bash
gh label list --repo cyrillesondag/fetchora --json name,color | jq '.[] | select(.name == "github-watcher")'
```

Résultat attendu : un objet JSON avec `"name": "github-watcher"`.

- [ ] **Step 5 : Committer**

```bash
git add scripts/github-watcher-bootstrap.sh
git commit -m "feat: add github-watcher bootstrap script"
```

---

## Task 2 : Prompt de la routine hebdomadaire

**Goal :** Fichier markdown contenant les instructions précises et auto-suffisantes que l'agent cloud exécutera chaque lundi à 8h.

**Files :**
- Create : `docs/superpowers/routines/github-actions-watcher.md`

**Acceptance Criteria :**
- [ ] Toutes les étapes du design sont couvertes (scan, check, analyse, PR, Issue, lecture commentaires, mémoire)
- [ ] Chaque commande `gh` est complète et exécutable telle quelle
- [ ] Les deux endpoints commentaires sont couverts (issues + pulls review comments)
- [ ] Le filtrage `user.login == "cyrillesondag"` est explicite
- [ ] Le format d'entrée mémoire correspond exactement au design

**Verify :** Relecture manuelle du fichier — chaque étape doit être auto-suffisante sans référence à un contexte externe.

**Steps :**

- [ ] **Step 1 : Écrire le prompt de la routine**

```markdown
# GitHub Actions Watcher — Routine Hebdomadaire

Tu es un agent de surveillance des workflows GitHub pour le compte `cyrillesondag`.
Exécute les étapes ci-dessous dans l'ordre. Si une étape échoue pour un repo donné,
logue l'erreur et continue avec le repo suivant — ne t'arrête pas.

## Configuration

```bash
GITHUB_USER="cyrillesondag"
MEMORY_REPO="cyrillesondag/claude-memory"
TODAY=$(date +%Y-%m-%d)
CUTOFF=$(date -d '90 days ago' --iso-8601=seconds)
```

---

## Étape 1 — Scanner les repos actifs

```bash
gh repo list "$GITHUB_USER" --limit 100 \
  --json name,pushedAt,isArchived \
  | jq -r --arg cutoff "$CUTOFF" \
    '.[] | select(.isArchived == false) | select(.pushedAt > $cutoff) | .name'
```

Mémorise la liste dans `ACTIVE_REPOS`. Exécuter les étapes 2 à 5 pour chaque repo.

---

## Étape 2 — Vérifier si une PR github-watcher est déjà ouverte

```bash
EXISTING=$(gh pr list --repo "$GITHUB_USER/$REPO" \
  --label github-watcher --state open --json number | jq length)
```

Si `EXISTING > 0` → afficher `[skip] $REPO : PR github-watcher déjà ouverte` et passer au repo suivant.

---

## Étape 3 — Vérifier le dernier run de workflow

```bash
RUN=$(gh run list --repo "$GITHUB_USER/$REPO" --limit 1 \
  --json status,conclusion,databaseId,workflowName)
CONCLUSION=$(echo "$RUN" | jq -r '.[0].conclusion // "none"')
STATUS=$(echo "$RUN"    | jq -r '.[0].status // "none"')
RUN_ID=$(echo "$RUN"    | jq -r '.[0].databaseId // ""')
WORKFLOW=$(echo "$RUN"  | jq -r '.[0].workflowName // ""')
```

- Si `CONCLUSION != "failure"` → afficher `[ok] $REPO ($STATUS/$CONCLUSION)` et passer au suivant.
- Si `STATUS == "in_progress"` → afficher `[skip] $REPO : run en cours` et passer au suivant.
- Si `RUN_ID == ""` → afficher `[skip] $REPO : aucun workflow` et passer au suivant.

---

## Étape 4 — Analyser le log d'échec

```bash
FAILED_LOG=$(gh run view "$RUN_ID" --repo "$GITHUB_USER/$REPO" --log-failed 2>&1 | head -200)
```

Analyser `FAILED_LOG` pour identifier la cause racine.

**Décision :**
- Si la correction ne porte que sur des fichiers `.github/workflows/*.yml` → **Étape 5A**
- Sinon → **Étape 5B**

---

## Étape 5A — Créer une PR de fix workflow

```bash
BRANCH="fix/watcher-$(echo "$WORKFLOW" | tr ' /' '--')-$TODAY"
TMPDIR=$(mktemp -d)

gh repo clone "$GITHUB_USER/$REPO" "$TMPDIR/$REPO"
cd "$TMPDIR/$REPO"
git checkout -b "$BRANCH"

# Appliquer le fix sur le(s) fichier(s) .github/workflows/*.yml concerné(s)
# <Claude applique ici les modifications identifiées à l'étape 4>

git add .github/workflows/
git commit -m "fix: [github-watcher] $WORKFLOW — <description courte>"
git push origin "$BRANCH"

gh pr create \
  --repo "$GITHUB_USER/$REPO" \
  --title "[github-watcher] fix: $WORKFLOW" \
  --body "## Cause identifiée
<cause>

## Fix appliqué
<description du changement>

## Run échoué
https://github.com/$GITHUB_USER/$REPO/actions/runs/$RUN_ID" \
  --label github-watcher \
  --head "$BRANCH"

cd / && rm -rf "$TMPDIR"
```

---

## Étape 5B — Ouvrir une Issue avec proposition de correction

```bash
gh issue create \
  --repo "$GITHUB_USER/$REPO" \
  --title "[github-watcher] Workflow '$WORKFLOW' en échec — correction requise" \
  --body "## Cause identifiée
<cause racine>

## Proposition de correction
<diff ou étapes concrètes en texte — ne pas modifier le code>

## Run échoué
https://github.com/$GITHUB_USER/$REPO/actions/runs/$RUN_ID" \
  --label github-watcher
```

---

## Étape 6 — Lire les commentaires sur les PRs github-watcher ouvertes

Pour chaque repo dans `ACTIVE_REPOS` :

```bash
PRS=$(gh pr list --repo "$GITHUB_USER/$REPO" \
  --label github-watcher --state open \
  --json number,title,headRefName)
```

Pour chaque PR retournée (`PR_NUMBER`, `PR_TITLE`, `PR_BRANCH`) :

```bash
# Commentaires généraux de la PR
COMMENTS=$(gh api "/repos/$GITHUB_USER/$REPO/issues/$PR_NUMBER/comments" \
  | jq -c '[.[] | select(.user.login == "cyrillesondag")]')

# Review comments inline
REVIEW_COMMENTS=$(gh api "/repos/$GITHUB_USER/$REPO/pulls/$PR_NUMBER/comments" \
  | jq -c '[.[] | select(.user.login == "cyrillesondag")]')

ALL_COMMENTS=$(echo "$COMMENTS $REVIEW_COMMENTS" | jq -s 'add // []')
```

Si `ALL_COMMENTS` est vide → pas d'entrée mémoire pour cette PR.
Sinon → **Étape 7**.

---

## Étape 7 — Mettre à jour la mémoire

```bash
MEMORY_TMPDIR=$(mktemp -d)
gh repo clone "$MEMORY_REPO" "$MEMORY_TMPDIR/claude-memory"
cd "$MEMORY_TMPDIR/claude-memory"

MEMORY_FILE="$REPO.md"

# Créer le fichier avec en-tête si absent
if [ ! -f "$MEMORY_FILE" ]; then
  echo "# claude-memory: $REPO" > "$MEMORY_FILE"
  echo "" >> "$MEMORY_FILE"
fi

# Composer l'entrée
{
  echo "## $TODAY — PR #$PR_NUMBER $PR_BRANCH"
  echo "**Contexte :** $PR_TITLE"
  # Pour chaque commentaire de l'utilisateur :
  echo "$ALL_COMMENTS" | jq -r '.[] | "**Ton commentaire :** \"\(.body)\""'
  echo "**Note retenue :** <synthèse actionnable en une phrase>"
  echo ""
  echo "---"
  echo ""
} >> "$MEMORY_FILE"

git add "$MEMORY_FILE"
git commit -m "memory($REPO): notes from PR #$PR_NUMBER on $TODAY"
git push

cd / && rm -rf "$MEMORY_TMPDIR"
```

> **Note :** À l'étape "Note retenue", synthétise le ou les commentaires en une règle actionnable
> pour les prochaines itérations sur ce repo.

---

## Résumé de fin d'exécution

Afficher un tableau récapitulatif :

```
=== Résumé GitHub Actions Watcher — <TODAY> ===
Repos scannés    : N
Repos OK         : N
PRs créées       : N (repos: ...)
Issues créées    : N (repos: ...)
Entrées mémoire  : N (repos: ...)
```
```

- [ ] **Step 2 : Committer**

```bash
git add docs/superpowers/routines/github-actions-watcher.md
git commit -m "feat: add github-actions-watcher routine prompt"
```

---

## Task 3 : Entrée mémoire Claude Code

**Goal :** Ajouter une entrée `reference` dans le système de mémoire Claude Code pointant vers `cyrillesondag/claude-memory`, pour que l'agent puisse retrouver les notes lors des sessions futures.

**Files :**
- Create : `~/.claude/projects/-home-cyrille-IdeaProjects-twitterDownloadVid/memory/github-watcher-reference.md`
- Modify : `~/.claude/projects/-home-cyrille-IdeaProjects-twitterDownloadVid/memory/MEMORY.md`

**Acceptance Criteria :**
- [ ] Le fichier de mémoire existe avec le bon frontmatter
- [ ] `MEMORY.md` contient le pointeur vers ce fichier

**Verify :** `cat ~/.claude/projects/-home-cyrille-IdeaProjects-twitterDownloadVid/memory/MEMORY.md | grep github-watcher`

**Steps :**

- [ ] **Step 1 : Créer le fichier mémoire**

Écrire dans `~/.claude/projects/-home-cyrille-IdeaProjects-twitterDownloadVid/memory/github-watcher-reference.md` :

```markdown
---
name: github-watcher-reference
description: Emplacement des notes de feedback extraites par la routine github-actions-watcher
metadata:
  type: reference
---

Les notes de feedback laissées en commentaire sur les PRs `[github-watcher]` sont
stockées dans le repo privé GitHub : **cyrillesondag/claude-memory**.

Un fichier par repo surveillé : `<repo>.md`

Pour lire la mémoire d'un repo spécifique :
```bash
gh api /repos/cyrillesondag/claude-memory/contents/<repo>.md \
  | jq -r '.content' | base64 -d
```

Ou cloner le repo entier :
```bash
gh repo clone cyrillesondag/claude-memory /tmp/claude-memory
```
```

- [ ] **Step 2 : Ajouter le pointeur dans MEMORY.md**

Ajouter à la fin de `MEMORY.md` :
```
- [GitHub Watcher Memory](github-watcher-reference.md) — Emplacement des notes feedback des PRs github-watcher dans cyrillesondag/claude-memory
```

---

## Task 4 : Enregistrement du schedule

**Goal :** Créer la routine planifiée via `/schedule` dans Claude Code avec le bon cron et le token GitHub configuré.

**Files :** Aucun fichier créé — configuration interne à Claude Code `/schedule`.

**Acceptance Criteria :**
- [ ] La routine existe dans `/schedule list`
- [ ] Le cron est `0 8 * * 1` (lundi 8h UTC)
- [ ] `GH_TOKEN` est configuré dans l'environnement de la routine

**Verify :** `/schedule list` → la routine `github-actions-watcher` apparaît avec le cron correct.

**Steps :**

- [ ] **Step 1 : Récupérer ton token GitHub**

```bash
gh auth token
```

Copier le token affiché.

- [ ] **Step 2 : Créer la routine via `/schedule`**

Dans une session Claude Code, taper :

```
/schedule create
```

Quand demandé :
- **Nom :** `github-actions-watcher`
- **Cron :** `0 8 * * 1`
- **Prompt :** Copier-coller le contenu de `docs/superpowers/routines/github-actions-watcher.md`
- **Variables d'environnement :** `GH_TOKEN=<token copié à l'étape 1>`

- [ ] **Step 3 : Vérifier**

```
/schedule list
```

La routine `github-actions-watcher` doit apparaître avec `cron: 0 8 * * 1`.

---

## Task 5 : Test d'intégration manuel

**Goal :** Déclencher la routine une fois manuellement et vérifier qu'elle scanne les repos, crée des PRs/Issues si des workflows sont en échec, et met à jour la mémoire si des commentaires existent.

**Files :** Aucun — vérification de comportement.

**Acceptance Criteria :**
- [ ] La routine termine sans erreur fatale
- [ ] Au moins un repo est scanné
- [ ] Si un workflow est en échec : une PR ou Issue est créée avec le label `github-watcher`
- [ ] Si une PR `github-watcher` avec commentaire de `cyrillesondag` existe : une entrée est ajoutée dans `cyrillesondag/claude-memory/<repo>.md`
- [ ] Le résumé final s'affiche

**Verify :**
```bash
# Vérifier les PRs créées par la routine
gh pr list --search "label:github-watcher" --author "@me" --json title,url

# Vérifier la mémoire
gh repo clone cyrillesondag/claude-memory /tmp/claude-memory-check
ls /tmp/claude-memory-check/
```

**Steps :**

- [ ] **Step 1 : Déclencher la routine manuellement**

```
/schedule run github-actions-watcher
```

Surveiller l'exécution dans le terminal Claude Code.

- [ ] **Step 2 : Vérifier les PRs créées**

```bash
gh pr list \
  --search "label:github-watcher in:title [github-watcher]" \
  --json title,url,repository \
  --jq '.[] | "\(.repository.name) — \(.title)\n  \(.url)"'
```

- [ ] **Step 3 : Vérifier les Issues créées**

```bash
gh issue list \
  --search "label:github-watcher" \
  --json title,url,repository \
  --jq '.[] | "\(.repository.name) — \(.title)\n  \(.url)"'
```

- [ ] **Step 4 : Vérifier la mémoire**

```bash
gh api /repos/cyrillesondag/claude-memory/contents \
  | jq -r '.[].name'
```

Si des entrées existent :
```bash
gh api /repos/cyrillesondag/claude-memory/contents/<repo>.md \
  | jq -r '.content' | base64 -d | tail -20
```
