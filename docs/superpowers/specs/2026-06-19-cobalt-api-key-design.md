# Design : API Key optionnelle pour le serveur Cobalt

**Date :** 2026-06-19
**Scope :** Ajout d'un champ API key dans les settings, injectée en header `Authorization` sur les requêtes POST vers Cobalt.

---

## Contexte

L'application Fetchora communique avec une instance Cobalt configurable. Certaines instances privées requièrent une API key transmise via le header `Authorization: Api-Key <key>`. Ce champ doit être optionnel — les instances publiques n'en ont pas besoin.

## Composants modifiés

### 1. `SettingsDataStore`

- Nouvelle clé DataStore : `cobalt_api_key` (stringPreferencesKey)
- Nouveau Flow : `cobaltApiKey: Flow<String?>` — retourne `null` si absent ou vide
- Nouvelle méthode : `setCobaltApiKey(key: String)` — supprime la préférence si `key` est vide

### 2. `HostSettingInterceptor`

- Lit `cobaltApiKey` depuis le DataStore via `runBlocking` (même pattern que `cobaltUrl`)
- Si la valeur est non-null et non-vide, injecte `Authorization: Api-Key <key>` sur la requête
- Uniquement sur les requêtes POST (le bloc `if (GET) return` existant protège les GET)

### 3. `SettingsViewModel`

- Nouveau StateFlow : `cobaltApiKey: StateFlow<String?>` depuis `settings.cobaltApiKey`
- Nouvelle méthode : `saveApiKey(key: String)` — appelle `settings.setCobaltApiKey(key)`

### 4. `SettingsScreen`

Dans la section "Service Configuration", sous le champ URL existant :

- `OutlinedTextField` avec `visualTransformation = PasswordVisualTransformation()`
- Label : "API Key (optionnel)"
- Bouton "Save" dédié, toujours actif, indépendant du flow Test/Save de l'URL

## Flux de données

```
Utilisateur saisit la clé
    → SettingsScreen (draft local)
    → saveApiKey() sur SettingsViewModel
    → setCobaltApiKey() sur SettingsDataStore
    → DataStore persiste la clé

À chaque requête POST :
    HostSettingInterceptor.intercept()
    → lit cobaltApiKey (runBlocking)
    → si non-null/non-vide : ajoute Authorization header
    → proceed()
```

## Décisions

- **Pas d'intercepteur séparé** : la logique d'injection reste dans `HostSettingInterceptor` qui gère déjà les POST.
- **Pas de test de la clé** : impossible de valider une API key sans faire une vraie requête de téléchargement. Le champ Save est donc toujours actif.
- **PasswordVisualTransformation** : masque les caractères pour éviter l'exposition accidentelle de la clé.
- **Stockage en clair** dans DataStore : acceptable pour une clé API locale, cohérent avec la façon dont l'URL est stockée.

## Hors scope

- Chiffrement de la clé dans DataStore
- Validation de la clé via un endpoint dédié
- Migration des préférences existantes (aucune clé existante à migrer)
