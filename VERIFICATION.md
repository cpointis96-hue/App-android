# Vérification — 4 octobre 2026

Source initiale : `fe1d8f0`. Branche de préparation : `prepare/portfolio-documentation`.

Le SDK a été localisé dans `/opt/homebrew/share/android-commandlinetools` : plateforme `android-35` seulement ; outils de compilation 34.0.0 et 35.0.0. La plateforme 34 demandée par le projet manque. JDK 21 local disponible ; le projet et le workflow demandent une cible Java 17.

Commande diagnostique, sans installation de SDK ni acceptation de licences :

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
GRADLE_USER_HOME=/private/tmp/codex-portfolio-gradle \
sh ./gradlew assembleDebug test lint --offline --no-daemon
```

Premier essai : téléchargement du wrapper Gradle bloqué par `UnknownHostException: services.gradle.org` dans le bac à sable. Nouvel essai avec accès réseau : Gradle 8.7 téléchargé, puis échec en mode hors ligne car le plugin `com.android.application:8.3.2` n'était pas dans le cache (`BUILD FAILED in 39s`).

Un essai avec résolution réseau et `-Dcom.android.builder.sdkDownload=false` a résolu les dépendances, mais AGP a néanmoins annoncé la préparation de l'installation de la plateforme 34. Le processus a été interrompu (code 130), avant compilation. Une inspection après arrêt constate un nouveau dossier `platforms/android-34` : une écriture SDK automatique a donc commencé malgré le paramètre. Le dossier a été conservé, sans nettoyage destructif ni affirmation d'installation complète. Le message « License ... accepted » désigne une licence déjà présente dans le SDK ; aucune acceptation de licence n'a été effectuée par l'agent. Ce paramètre n'a pas assuré la désactivation attendue. Ne pas reprendre ce même essai sur un SDK partagé ; utiliser un SDK de test explicitement préparé.

Aucun APK nouveau, test Android, lint réussi ou capture n'est revendiqué. Aucun média téléchargé, aucune donnée personnelle consultée, aucune licence nouvellement acceptée. Le Mac verrouillé exclut les captures natives.

Pour reprendre : fournir Gradle 8.7 et les dépendances résolues, JDK 17, plateforme SDK 34 et environnement de test disponible ; lancer `assembleDebug test lint`, puis vérifier sur un émulateur vide l'accueil, URL partagée, formats, sous-titres, segment, progression, fichiers, suppression et redémarrage. Le code ne contient pas de suite de tests Android dédiée.
