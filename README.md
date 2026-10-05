# Varjoaika Android

Oma Android-sovellus goottikalenterille: 13 kuukautta, 30 päivää, varjomaiset nimet, omat muistiinpanot ja muistutukset. Kalenteri ja widgetit toimivat offline eikä sovellus pyydä sijainti- tai puhelimen kalenterioikeuksia. Vain valinnainen viikoittainen MIKES-aikatarkistus käyttää Internet-oikeutta. Ei säätä, mainoksia, käyttäjätiliä eikä Pythonia puhelimeen.

**[Lataa asennettava APK julkaisuista](https://github.com/Jakke77/Varjoaika-Android/releases/latest)**

## Käyttö

Lataa julkaistu `Varjoaika-1.0.0.apk` puhelimelle, avaa se ja hyväksy asennus käyttämästäsi selaimesta/tiedostosovelluksesta. Avaa **Varjoaika**. Paina päivää ja lisää merkintä tai muistutus. Päivämäärissä ovat tutut päivät 0–29 ja kuukaudet 0–12; virallinen päivämäärä näkyy rinnalla. Tallennettuihin merkintöihin palaat Merkinnät-painikkeesta. Kuukausien nimet ja laskenta vastaavat [työpöytäkalenteria](https://github.com/Jakke77/goottikalenteri).

Kotinäytössä: paina tyhjää kohtaa pitkään → Widgetit → Varjoaika. Valitse malli ja sen ulkoasu. **⚙** muuttaa vain kyseisen widgetin asetuksia. Kalenterin nuolet vaihtavat kuukautta, päivän napautus avaa päivän sovelluksessa. Widgetiä voi venyttää kotinäytön tavallisilla eleillä.

| Widget | Oletuskoko | Sisältö |
|---|---|---|
| Kello | 2×2 | Pelkkä teksti, elävä kellonaika ja Varjoajan päiväys |
| Varjokupari | 2×3 | Tumma kuparipaneeli, kello ja molemmat päiväykset |
| Kalenteri | 3×3 | Selattava kuukausi ja merkintäpisteet |
| Suuri kalenteri | 4×4 | Kuukausi ja tulevat merkinnät |
| Merkinnät | 2×3 | Kello, päiväys ja tulevien merkintöjen lista |

Kaikissa ovat Varjokupari-, Yöhopea- ja läpinäkyvä teema. Taustan peittävyyden voi säätää nollaan. Tekstikoko ja sekunnit ovat widgetkohtaisia. Pienet kalenterit jättävät lisärivit pois, jotta päivät mahtuvat. Tarkka ruutukoko riippuu kotinäyttösovelluksesta: Androidin ruudukot eivät ole kaikkialla samanlaisia.

## Muistutukset ja ääni

Muistutukset tallennetaan puhelimen yksityiseen tietokantaan ja ajastetaan Androidin hälytyksinä. Ohjelmaa ei tarvitse pitää avoinna. Android 13+ kysyy ilmoitusluvan; Android 12+ voi vaatia erillisen luvan tarkkoihin hälytyksiin. **Asetukset → Ilmoitus- ja hälytysluvat** avaa oikean asetuksen. Ilman tarkkaa hälytyslupaa sovellus käyttää viivästyä voivaa hälytystä. Ilman ilmoituslupaa muistutusta ei kuitata toimitetuksi, ja luvan sallimisen jälkeen sovellus toimittaa myöhästyneen muistutuksen kerran.

Ilmoituksessa ovat **Siirrä 10 min** ja **Kuittaa**. Oletusääni on itse tuotettu huuhkajan huhuilun jäljitelmä. Voimakkuus on säädettävä; tiedostovalitsimella voit valita puhelimen tukeman oman WAV-, OGG-, MP3- tai muun äänitiedoston. Sovellus tarvitsee luvan vain valittuun tiedostoon. Oma ääni toistetaan enintään 8 sekunnin ajan. Puhelimen ilmoitusäänen voimakkuus ja Älä häiritse -tila vaikuttavat myös toistoon. Ilmoituskanava itsessään on äänetön, jotta sama ääni ei soi kahdesti.

Käynnistys puhelimen uudelleenkäynnistyksen ja sovelluspäivityksen jälkeen palauttaa ajastukset. Androidin **Pakota lopettamaan** estää hälytykset, kunnes avaat sovelluksen uudelleen. Valmistajan virransäästö voi viivästyttää hälytyksiä; sovellus ei ohita puhelimen sääntöjä. Tiedot säilyvät saman allekirjoituksen päivityksissä. Sovelluksen poisto poistaa myös paikalliset merkinnät, joten älä poista sovellusta päivitystä varten.

## Viikoittainen MIKES-aikatarkistus

Oletuksena sovellus tarkistaa ajan kerran viikossa julkisesta `time.mikes.fi`-palvelimesta. Sen aika on synkronoitu Suomen virallisesta ajasta ([VTT MIKES](https://www.vttresearch.com/fi/palvelut/suomen-aika-ntp-palvelu)). Oikea nimi on **time.mikes.fi**. Yksi NTP-pyyntö ja vastaus kulkevat UDP-portissa 123. Androidin verkkotyö odottaa tarvittaessa yhteyttä ja virransäästö voi viivästyttää tarkistusta. Asetuksissa näkyvät tulos, poikkeama sekunteina ja viimeinen onnistunut tarkistus. Tarkistuksen voi kytkeä pois, jolloin sovellus ei tee verkkopyyntöjä.

**Tarkistus ei muuta puhelimen kelloa.** Androidin tavallisella sovelluksella ei ole oikeutta asettaa järjestelmäaikaa. Kellot näyttävät puhelimen ajan. Pidä Androidin automaattinen päivämäärä ja aika käytössä; sovelluksen asetuksista voi avata puhelimen aika-asetukset. Ei sääpalveluita eikä muita verkkoyhteyksiä.

## Yhteensopivuus ja rakentaminen

Android 5.0+ (API 21), target/compile API 36. APK sisältää Java-koodia ja Androidin omia komponentteja, ei sovelluksen natiiveja kirjastoja: sama paketti sopii ARM-, ARM64-, x86- ja x86_64-laitteille. Puhelin tai tabletti tarvitsee kotinäyttösovelluksen, joka tukee widgetejä. Android TV-, Wear OS- ja muut kotinäytöt ilman widgettukea eivät ole tämän sovelluksen kohde. Kaikkien valmistajien ja kotinäyttöjen toimintaa ei voi taata.

Rakennus JDK 17:llä, Gradle 8.13:lla ja Android SDK 36:lla:

```sh
gradle testDebugUnitTest lintDebug assembleRelease
```

GitHub Actions ajaa päivämäärän testit, lintin ja laitetestit API 21- ja 36-emulaattoreissa. Laitetestit varmistavat tarpeettomien sijainti- ja kalenterioikeuksien puuttumisen, tietojen säilymisen ja muistutuksen kuittaamisen kerran sekä kaikkien widgetien RemoteViews-näkymien rakentumisen eri koossa. Emulaattorit eivät korvaa testausta omalla puhelimella. Actionsin debug-APK on kehittäjätestejä varten; käytä julkaisuissa olevaa samalla yksityisellä avaimella allekirjoitettua APK:ta päivityksiin.

[APK-julkaisun ylläpito ja allekirjoitusavaimen varmuuskopio](MAINTAINER.md).

MIT. Sovelluksen kuvat ja huhuilu ovat alkuperäisiä. [Androidin widgetit](https://developer.android.com/develop/ui/views/appwidgets/overview), [hälytykset](https://developer.android.com/develop/background-work/services/alarms), [ilmoituslupa](https://developer.android.com/develop/ui/views/notifications/notification-permission).
