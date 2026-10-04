# TubeExtract

Prototype Android pour récupérer une vidéo depuis une URL, choisir une sortie vidéo ou audio, des sous-titres et éventuellement un segment temporel. Ce dépôt s'appelle historiquement `App-android` ; il contient TubeExtract, un projet distinct de VérifScoot. Le dépôt existant reste privé.

## Ce que contient le projet

L'interface Kotlin / Jetpack Compose comporte accueil, choix du téléchargement, progression et bibliothèque. Elle accepte une URL saisie ou partagée via Android. `DownloadViewModel` interroge le wrapper yt-dlp Android, prépare les options audio/vidéo et sous-titres, télécharge puis utilise FFmpeg pour découper un segment. Les fichiers sont placés dans le répertoire externe propre à l'application, sous `Downloads/TubeExtract`.

Stack : Kotlin 1.9.24, Compose Material 3, coroutines / StateFlow, Coil, Android Gradle Plugin 8.3.2, Gradle 8.7, cible JVM 17. Android minimum 8.0 (API 26), compilation et cible API 34.

## Installation et téléchargement

Avec un accès au dépôt privé, cloner ou télécharger les sources depuis GitHub. Installer JDK 17 et Android SDK API 34 avec les outils de compilation 34.0.0, puis définir `ANDROID_HOME` ou `sdk.dir` dans un `local.properties` non versionné.

```sh
sh ./gradlew assembleDebug test lint --no-daemon
```

Après une compilation réussie, l'APK debug se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. Son installation sur un appareil de test peut se faire avec `adb install -r` suivi de ce chemin. Cet APK est un build de développement ; aucune nouvelle release validée n'a été produite dans cette préparation.

Le workflow GitHub existant construit un APK debug, le copie vers `apk/TubeExtract.apk`, le committe et publie un artefact conservé 30 jours. Sa présence dans le workflow ne prouve pas qu'un build disponible a été exécuté ou testé ; consulter le résultat Actions correspondant avant de télécharger un APK.

## Limites observées dans le code

- Les dépendances yt-dlp / FFmpeg sont déclarées en `0.17.+` : leur résolution peut évoluer.
- L'initialisation yt-dlp est asynchrone et FFmpeg n'est pas explicitement initialisé dans `TubeExtractApp`. Le parcours réseau et le découpage restent à vérifier sur appareil.
- La liste de bibliothèque reste en mémoire et n'est pas reconstruite depuis les fichiers après redémarrage. La suppression retire un fichier et son entrée ; aucune lecture ou export n'est proposé dans cet écran.
- Un `DownloadService` existe, mais aucun appel pour le démarrer n'a été trouvé. Le téléchargement s'exécute dans le ViewModel ; la continuité en arrière-plan n'est pas établie.
- L'option nommée `burnSubtitles` utilise `--embed-subs`, ce qui demande l'intégration des sous-titres plutôt qu'une incrustation visuelle garantie. Le découpage copie les flux et sa précision n'est pas validée.
- Une copie peut remplacer un fichier portant le même nom. Aucune persistance d'historique, politique de collision ou récupération après interruption n'est validée.

Utiliser uniquement des médias auxquels vous avez accès et dont le téléchargement est autorisé. Aucun média ni historique personnel n'a été téléchargé pour cette préparation. Voir [VERIFICATION.md](VERIFICATION.md) pour les blocages exacts. Les captures réelles et le test sur appareil restent à réaliser.
