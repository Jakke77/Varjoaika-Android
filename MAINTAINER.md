# Julkaisun ylläpito

Actions rakentaa allekirjoittamattoman release-APK:n, debug-APK:n ja virallisen Android SDK:n `apksigner.jar`-työkalun `android-apks`-artefaktiin. Julkaise vasta koonnin, lintin, yksikkötestien sekä API 21- ja 36-laitetestien läpäistyä. Laitetestien artefakteissa ovat kalenterin ja widgettien kuvat.

Allekirjoita ladattu release-APK paikallisesti:

```sh
bash scripts/sign-release.sh app-release-unsigned.apk apksigner.jar dist/Varjoaika-1.0.0.apk
```

Ensimmäinen kerta luo `.signing/varjoaika.p12`-avaimen ja `.signing/password`-tiedoston. Ne jäävät paikallisesti yksityisiksi, eivätkä kuulu Gitiin tai julkaisuihin. **Varmuuskopioi koko `.signing`-hakemisto turvalliseen paikkaan.** Samalla avaimella allekirjoitettu päivitys säilyttää sovelluksen tiedot. Kadonnutta avainta ei voi korvata tavallisessa APK-päivityksessä. Älä luo uutta avainta seuraavaa julkaisua varten.

Seuraava julkaisu: nosta `versionCode` ja `versionName` tiedostossa `app/build.gradle`, aja tarkistukset, allekirjoita samalla avaimella ja lisää APK sekä SHA256 GitHub-julkaisuun. Tagin pitää osoittaa testattuun lähdekoodiin. Älä julkaise debug-APK:ta tavalliseksi päivitykseksi.
