# Design : Wizard d'onboarding au premier lancement

**Date :** 2026-06-19
**Scope :** Afficher un wizard fullscreen au premier lancement de l'app pour guider l'utilisateur dans la configuration d'une instance Cobalt.

---

## Contexte

L'écran Settings permet de configurer l'URL d'une instance Cobalt, mais un nouvel utilisateur ne sait pas forcément qu'il doit le faire, ni ce qu'est Cobalt. L'idée est d'afficher un wizard interactif au premier lancement pour l'y inviter.

## Comportement cible

### Déclenchement

- Le wizard s'affiche **une seule fois**, au tout premier lancement de l'application.
- Un flag `onboardingDone` (booléen) est stocké en DataStore. Tant qu'il est `false`, le wizard est le point d'entrée.
- Dès que l'utilisateur clique "Passer" ou termine le wizard, `onboardingDone = true` est sauvegardé.

### Structure du wizard

**2 étapes**, navigables avec le Navigation Component (Jetpack Compose Navigation) :

#### Étape 1 — Bienvenue (`onboarding/welcome`)

- Icône Fetchora + titre "Bienvenue sur Fetchora"
- Texte d'introduction : présente Cobalt comme un outil open-source qui extrait les médias depuis des plateformes de partage de vidéos et de réseaux sociaux — **sans référence à des marques tierces** (pas de YouTube, Twitter, TikTok, etc.)
- Encart "C'est quoi Cobalt ?" : définition générique
- Bouton **"Suivant →"** → navigue vers étape 2
- Bouton **"Passer"** (haut droite) → `setOnboardingDone()` + navigue vers `history`

#### Étape 2 — Configuration de l'instance (`onboarding/url`)

- Titre "Configurez votre instance"
- Champ URL pré-rempli avec `https://api.cobalt.tools/` (instance officielle)
- Lien externe "↗ Trouver d'autres instances sur instances.cobalt.best" → ouvre le navigateur système
- Zone d'état (Idle / Loading / Success / Error) — réutilise `ServerInfoState` existant
- Bouton **"Tester et continuer"** → appelle `testAndSave(url)` → si succès : `setOnboardingDone()` + navigue vers `history`
- Bouton **"← Retour"** → navigue vers étape 1
- Bouton **"Passer"** (haut droite) → `setOnboardingDone()` + navigue vers `history`

### Navigation

- La `BottomNavigationBar` est masquée sur les routes `onboarding/*`
- Le `startDestination` du `NavHost` est déterminé au lancement :
  - `onboardingDone == false` → `"onboarding/welcome"`
  - `onboardingDone == true` → `"history"`
- La lecture initiale de DataStore se fait via le `MainViewModel` (StateFlow). Pendant le chargement, un écran vide ou un splash est affiché pour éviter un flash.

## Architecture

### Composants modifiés / créés

| Action | Fichier | Responsabilité |
|--------|---------|----------------|
| Modifier | `data/local/datastore/SettingsDataStore.kt` | Ajouter `onboardingDone: Flow<Boolean>` et `setOnboardingDone()` |
| Créer | `ui/main/MainViewModel.kt` | Lire `onboardingDone`, exposer `completeOnboarding()` |
| Modifier | `ui/main/MainActivity.kt` | `startDestination` dynamique, routes onboarding, masquer BottomBar sur `onboarding/*` |
| Créer | `ui/onboarding/OnboardingWelcomeScreen.kt` | Composable étape 1 |
| Créer | `ui/onboarding/OnboardingUrlScreen.kt` | Composable étape 2 |

### `SettingsDataStore` — nouvelles API

```kotlin
private val keyOnboardingDone = booleanPreferencesKey("onboarding_done")

val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[keyOnboardingDone] ?: false
}

suspend fun setOnboardingDone() {
    context.dataStore.edit { it[keyOnboardingDone] = true }
}
```

### `MainViewModel`

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(private val settings: SettingsDataStore) : ViewModel() {
    val onboardingDone = settings.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, null) // null = en cours de chargement

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }
}
```

### `MainActivity` — logique de startDestination

```kotlin
val onboardingDone by viewModel.onboardingDone.collectAsState()

// Pendant le chargement (null), afficher un écran vide
if (onboardingDone == null) return

val startDestination = if (onboardingDone == true) "history" else "onboarding/welcome"

NavHost(navController, startDestination = startDestination) {
    composable("onboarding/welcome") { OnboardingWelcomeScreen(...) }
    composable("onboarding/url")     { OnboardingUrlScreen(...) }
    composable("history")            { HistoryScreen() }
    composable("settings")           { SettingsScreen() }
}
```

La `BottomNavigationBar` n'est rendue que si la route courante ne commence pas par `"onboarding"`.

### `OnboardingUrlScreen`

Réutilise `SettingsViewModel` pour `testAndSave(url)` et `serverInfoState`. À la navigation vers `history` après succès, le ViewModel est partagé et l'Info section de Settings sera déjà peuplée.

## Flux de données

```
App lance → MainViewModel lit onboardingDone depuis DataStore
    → false : NavHost démarre à onboarding/welcome
    → true  : NavHost démarre à history (flux normal)

Étape 1 → "Suivant" → navController.navigate("onboarding/url")
Étape 1 → "Passer" → completeOnboarding() + navController.navigate("history") { popUpTo(0) }

Étape 2 → "Tester et continuer" → settingsViewModel.testAndSave(url)
    → Success : completeOnboarding() + navController.navigate("history") { popUpTo(0) }
    → Error   : affiche l'erreur, reste sur l'étape 2
Étape 2 → "← Retour" → navController.popBackStack()
Étape 2 → "Passer" → completeOnboarding() + navController.navigate("history") { popUpTo(0) }
```

## Hors scope

- Animation de transition entre les étapes
- Possibilité de revenir au wizard depuis les Settings
- Onboarding multi-étapes (dossier, clé API)
- Deep link vers l'onboarding
