# Tuneo

Clone fonctionnel de Lark Player (audio + vidéo locaux), scope v1.

## Ce qui marche dans cette version
- Écran d'accueil avec header "Tuneo" + onglets (Vidéos, Chansons, Playlists, Dossiers, Artists, Albums)
- Onglet **Chansons** : liste réelle de ta musique locale (scan MediaStore)
- Onglet **Vidéos** : grille réelle de tes vidéos locales (scan MediaStore)
- Écran lecteur (Now Playing) : lecture, pause, suivant/précédent, barre de progression
- Lecture en fond (notification media) via un service Android
- Permission d'accès aux médias au premier lancement
- Onglets Playlists / Dossiers / Artists / Albums : visibles mais pas encore fonctionnels ("bientôt disponible")

## Comment obtenir l'APK sans PC (via GitHub Actions)

1. Crée un compte GitHub si tu n'en as pas (gratuit, depuis ton tel).
2. Crée un nouveau repository (ex: "tuneo"), en **privé** ou public, peu importe.
3. Upload tous les fichiers/dossiers de ce projet dans le repo, en gardant exactement la même arborescence de dossiers (important : les dossiers cachés comme `.github` doivent être uploadés aussi).
4. Une fois uploadé, va dans l'onglet **Actions** de ton repo GitHub.
5. Un workflow "Build Tuneo APK" se lance automatiquement (ou clique sur "Run workflow" s'il ne démarre pas seul).
6. Attends la fin du build (quelques minutes).
7. Clique sur le run terminé → descends jusqu'à **Artifacts** → télécharge `tuneo-debug-apk`.
8. C'est un fichier `.zip` contenant `app-debug.apk` — extrais-le, puis ouvre l'APK sur ton téléphone pour l'installer (autorise "sources inconnues" si demandé).

## Prochaines étapes (non incluses dans ce scope)
- Lecture vidéo (clic sur une vidéo)
- Playlists fonctionnelles
- Regroupement par Dossiers / Artists / Albums
- Égaliseur, sync paroles (LRC), sleep timer
