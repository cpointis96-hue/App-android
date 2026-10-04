# Vérification — 4 octobre 2026

Source initiale : `fe1d8f0`. Branche de préparation : `prepare/portfolio-documentation`.

Au début de la préparation, le SDK localisé dans `/opt/homebrew/share/android-commandlinetools` contenait la plateforme `android-35` seulement, avec les outils de compilation 34.0.0 et 35.0.0. La plateforme 34 demandée par le projet manquait alors ; son ajout automatique pendant un essai est décrit ci-dessous. JDK 21 local disponible ; le projet et le workflow demandent une cible Java 17.

Commande diagnostique, sans installation de SDK ni acceptation de licences :

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
GRADLE_USER_HOME=/private/tmp/codex-portfolio-gradle \
sh ./gradlew assembleDebug test lint --offline --no-daemon
```

Premier essai : téléchargement du wrapper Gradle bloqué par `UnknownHostException: services.gradle.org` dans le bac à sable. Nouvel essai avec accès réseau : Gradle 8.7 téléchargé, puis échec en mode hors ligne car le plugin `com.android.application:8.3.2` n'était pas dans le cache (`BUILD FAILED in 39s`).

Un essai avec résolution réseau et `-Dcom.android.builder.sdkDownload=false` a résolu les dépendances, mais AGP a néanmoins annoncé la préparation de l'installation de la plateforme 34. Le processus a été interrompu (code 130), avant compilation. Une inspection après arrêt constate un nouveau dossier `platforms/android-34` : une écriture SDK automatique a donc commencé malgré le paramètre. Le dossier a été conservé, sans nettoyage destructif ni affirmation d'installation complète. Le message « License ... accepted » désigne une licence déjà présente dans le SDK ; aucune acceptation de licence n'a été effectuée par l'agent. Ce paramètre n'a pas assuré la désactivation attendue. Ne pas reprendre ce même essai sur un SDK partagé ; utiliser un SDK de test explicitement préparé.

À ce stade initial, aucun APK nouveau, test Android, lint réussi ou capture n'était obtenu. Aucun média téléchargé, aucune donnée personnelle consultée, aucune licence nouvellement acceptée. Le Mac verrouillé exclut les captures natives.

Après instruction du parent, dernier essai strictement hors ligne : `sh ./gradlew --offline -Pandroid.builder.sdkDownload=false :app:assembleDebug :app:testDebugUnitTest --no-daemon`, mêmes variables ci-dessus. Le bac à sable bloque d'abord la socket locale du daemon (`SocketException: Operation not permitted`) ; relance autorisée hors bac à sable, toujours `--offline`. Résultat : `:app:checkDebugAarMetadata FAILED`, `BUILD FAILED in 3s`, dépendances runtime absentes du cache (notamment Kotlin stdlib 1.9.24, Compose BOM 2024.06.00, bibliothèques AndroidX, Coil 2.7.0 et versions yt-dlp/FFmpeg 0.17.+). Aucun téléchargement réseau pendant cet essai final ; arrêt de la vérification.

## Correction minimale des dépendances

Une exécution réseau ultérieure du parent avec `--no-daemon -Pandroid.builder.sdkDownload=false :app:assembleDebug :app:testDebugUnitTest` a échoué sur `checkDebugAarMetadata` en 3 min 30 : les coordonnées `com.github.yausername.youtubedl-android:library/ffmpeg:0.17.+` ne se résolvent pas dans les dépôts configurés. Ce résultat constitue l'état rouge avant correction, sans le présenter comme un nouveau test exécuté par cet agent.

Deux lignes de build corrigées : groupe `io.github.junkfood02.youtubedl-android`, version fixe `0.17.4` pour library et ffmpeg, sans passer à 0.18 ni changer SDK/interface. Sources primaires : [README officiel](https://github.com/yausername/youtubedl-android#installation), POM Maven Central [library](https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.4/library-0.17.4.pom) et [ffmpeg](https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/ffmpeg/0.17.4/ffmpeg-0.17.4.pom), tous deux récupérés avec `curl -fsS --max-time 30`. Ils déclarent GPL-3.0 ; la [notice FFmpeg](https://ffmpeg.org/legal.html) distingue LGPL de base et composants GPL. Aucune licence propre au projet n'est inventée.

Build avec les mêmes variables JDK21/SDK/cache et `sh ./gradlew --no-daemon -Pandroid.builder.sdkDownload=false :app:assembleDebug :app:testDebugUnitTest` : dépendances résolues, mais AAPT bloque sur le thème historique inexistant `android:Theme.Material.NoTitleBar` (`BUILD FAILED in 4m 4s`). Remplacé par `android:Theme.Material.NoActionBar`, dont la présence est vérifiée dans les ressources du SDK34.

Essai suivant hors ligne : blocage sur kotlin-script-runtime 1.9.24 et kotlin-reflect 1.6.10 absents du cache (`BUILD FAILED in 5s`). Relance réseau pour ces dépendances : Kotlin révèle `OutlinedButtonDefaults`, propriétés `subtitles`/`automaticCaptions` de VideoInfo et `FFmpeg.execute` inexistants (`BUILD FAILED in 17s`). `javap` des classes AAR0.17.4 confirme que FFmpeg expose seulement `init`/`getInstance`. Le nom Material est corrigé par `ButtonDefaults.outlinedButtonColors`, sans changement de couleur ou disposition.

Le parent a autorisé les adaptations minimales réelles de ces API : lecture du JSON `--dump-json` par JSONObject, initialisation yt-dlp/FFmpeg avant les tâches, lancement du binaire `nativeLibraryDir/libffmpeg.so` avec `LD_LIBRARY_PATH` selon [YoutubeDL.kt 0.17.4](https://github.com/yausername/youtubedl-android/blob/0.17.4/library/src/main/java/com/yausername/youtubedl_android/YoutubeDL.kt) et [FFmpeg.kt 0.17.4](https://github.com/yausername/youtubedl-android/blob/0.17.4/ffmpeg/src/main/java/com/yausername/ffmpeg/FFmpeg.kt). Le runner vérifie un retour nul et une sortie non vide. `jniLibs.useLegacyPackaging=true` extrait les exécutables natifs, conformément au besoin fournisseur, sans attribut de manifest obsolète.

Un essai intermédiaire a échoué sur ObjectMapper inaccessible depuis le compile classpath (8 s) ; le parsing final utilise uniquement JSONObject déjà disponible, sans dépendance Jackson ajoutée. Relance finale : même commande réseau, `BUILD SUCCESSFUL in 37s`, 42 tâches dont 15 exécutées. `assembleDebug` et `testDebugUnitTest` réussis ; deux tests JVM, zéro échec/erreur, 0,777 s. Les tests utilisent des scripts locaux synthétiques pour contrôler arguments/environnement, rejeter retour non nul ou sortie vide ; ils ne valident pas FFmpeg Android ni un média réel.

APK produit : `app/build/outputs/apk/debug/app-debug.apk`, environ 132 Mio. SHA-256 : `c82d8f50c84c9e5653c0f89b8f2433a9a114fea2d7e892403143adbf5f196e8e`. `apksigner verify` réussit ; `aapt dump badging` confirme com.tubeextract 1.0, minSDK26/target34 et MainActivity démarrable, ABI arm64-v8a/armeabi-v7a/x86/x86_64. Ces contrôles statiques ne prouvent pas l'ouverture sur Android. Aucun téléchargement SDK ni licence nouvelle pendant ces builds corrigés ; seul le téléchargement de dépendances a eu lieu.

Pour le parcours réel : vérifier sur un émulateur vide l'accueil, URL partagée, formats, sous-titres, segment, progression, fichiers, suppression et redémarrage. Aucune capture ni exécution sur appareil dans cette tâche. Les deux tests JVM du runner ne constituent pas une suite UI Android. Lint non exécuté dans cette reprise. Deux avertissements de compilation restent : icône ArrowBack dépréciée et Elvis redondant du callback.
