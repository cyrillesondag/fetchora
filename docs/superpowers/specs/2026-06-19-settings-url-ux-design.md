# Design : Amélioration UX du champ URL serveur Cobalt

**Date :** 2026-06-19
**Scope :** Remplacer les boutons "Test Connection" + "Save" séparés par un bouton unique "Tester et sauvegarder", avec un bouton "Réinitialiser", et vider la section Info dès que l'URL est modifiée.

---

## Contexte

L'écran Settings actuel comporte deux boutons pour l'URL Cobalt :
- "Test Connection" — lance un ping vers le serveur
- "Save" — activé uniquement si `serverInfoState is Success`

Ce comportement est peu clair : le bouton Save reste actif si l'URL a été testée avec succès puis modifiée, et le spinner de chargement s'affiche dans Save plutôt que dans Test.

## Comportement cible

### États du champ URL

| Situation | Boutons | Section Info |
|-----------|---------|--------------|
| URL inchangée (`draft == sauvegardée`) | "Tester et sauvegarder" désactivé · "Réinitialiser" désactivé | Infos du serveur sauvegardé |
| URL modifiée (`draft != sauvegardée`) | Les deux actifs | Vide ("No information available") |
| En cours (loading) | Les deux désactivés | Spinner centré |
| Succès | Boutons désactivés (draft réinitialisé = URL sauvegardée) | Nouvelles infos serveur |
| Échec | Les deux actifs | Message d'erreur |

### Transitions

- **onValueChange (URL)** : si `newValue != cobaltUrl` → appelle `clearServerInfo()` (section Info → Idle)
- **Clic "Tester et sauvegarder"** : appelle `testAndSave(draft)` → Loading → Success (save + info) ou Error (info = erreur)
- **Clic "Réinitialiser"** : remet `cobaltUrlDraft = cobaltUrl` dans l'UI + appelle `reloadServerInfo()` pour recharger les infos de l'URL sauvegardée

## Composants modifiés

### `SettingsViewModel`

Nouvelles méthodes (remplacent `testCobaltUrl` et `saveCobaltUrl`) :

- `testAndSave(url: String)` : set Loading → appelle `cobaltRepository.getInfo(url)` → si succès : `settings.setCobaltUrl(url)` + set Success → si échec : set Error
- `reloadServerInfo()` : set Loading → lit `settings.cobaltUrl.first()` → appelle `getInfo()` → set Success ou Error
- `clearServerInfo()` : set `_serverInfoState = Idle`

`testCobaltUrl(url)` et `saveCobaltUrl(url)` sont supprimées (plus utilisées).

### `SettingsScreen`

Dans `SettingsContent` :

- `isDirty = cobaltUrlDraft != cobaltUrl`
- `onValueChange` du champ URL :
  - si `newValue != cobaltUrl` → `onClearServerInfo()` (section Info → Idle)
  - si `newValue == cobaltUrl` → `onReloadServerInfo()` (l'utilisateur est revenu à l'URL sauvegardée, on recharge les infos pour être cohérent avec l'état "URL inchangée")
- Remplacement des deux anciens boutons par :
  - Bouton "Réinitialiser" (`OutlinedButton`, `enabled = isDirty && !isLoading`) : `cobaltUrlDraft = cobaltUrl; onReloadServerInfo()`
  - Bouton "Tester et sauvegarder" (`Button`, `enabled = isDirty && !isLoading`) : `onTestAndSave(cobaltUrlDraft)`
- Le bouton "Tester et sauvegarder" est simplement désactivé pendant le chargement (pas de spinner dans le bouton)
- La section Info affiche un `CircularProgressIndicator` centré quand `isLoading`

Nouveaux callbacks dans `SettingsContent` :
- `onTestAndSave: (String) -> Unit`
- `onReloadServerInfo: () -> Unit`
- `onClearServerInfo: () -> Unit`

Callbacks supprimés :
- `onSaveCobaltUrl: (String) -> Unit`
- `onTestCobaltUrl: (String) -> Unit`

## Flux de données

```
Utilisateur modifie l'URL (newValue != cobaltUrl)
    → onValueChange → clearServerInfo() → serverInfoState = Idle → section Info vide

Utilisateur revient à l'URL sauvegardée (newValue == cobaltUrl)
    → onValueChange → reloadServerInfo() → serverInfoState = Loading → Success/Error
    → section Info affiche les infos du serveur sauvegardé

Utilisateur clique "Tester et sauvegarder"
    → testAndSave(draft)
    → serverInfoState = Loading → section Info vide, boutons désactivés
    → getInfo(url)
    → [succès] setCobaltUrl(url) + serverInfoState = Success(info)
               → cobaltUrl Flow met à jour → cobaltUrlDraft se réinitialise
               → isDirty = false → boutons désactivés
    → [échec]  serverInfoState = Error(message)
               → boutons restent actifs

Utilisateur clique "Réinitialiser"
    → cobaltUrlDraft = cobaltUrl (dans l'UI)
    → reloadServerInfo()
    → serverInfoState = Loading → section Info vide
    → getInfo(cobaltUrl sauvegardée)
    → serverInfoState = Success(info) ou Error(message)
```

## Décisions

- **Pas de nouvel état "Dirty"** dans le ViewModel : `isDirty` est calculé dans l'UI (`cobaltUrlDraft != cobaltUrl`), ce qui évite de dupliquer l'état draft dans le ViewModel.
- **`clearServerInfo()` déclenché côté UI** via callback, pas via observation du draft dans le ViewModel (le ViewModel ne connaît pas le draft).
- **`testCobaltUrl` et `saveCobaltUrl` supprimées** : le nouveau flow `testAndSave` les remplace entièrement.

## Hors scope

- Validation syntaxique de l'URL (format https://)
- Debounce sur onValueChange
- Historique des URLs testées
