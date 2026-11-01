# 🧪 Yuletide – Client-Testplan

Schritt-für-Schritt-Anleitung, um die Mod mit einem echten Minecraft-Client zu prüfen. Haken setzen, was funktioniert – Abweichungen unter „Notizen“ festhalten.

---

## 1. Vorbereitung

**Du brauchst:** Minecraft Java **26.1.2** mit Fabric Loader (≥ 0.20.1), Fabric API `0.155.3+26.1.2`, Java 25.

### Variante A – Testserver lokal (empfohlen)
1. `build.cmd` ausführen → `dist\yuletide-1.0.0.jar`.
2. Fabric-Server 26.1.2 einrichten, `fabric-api` und `yuletide-1.0.0.jar` in `mods\` legen.
3. `eula=true` setzen, in `server.properties`: `online-mode=false`, `pause-when-empty-seconds=-1`.
4. Server starten, mit dem Client auf `localhost` verbinden.
5. Dich selbst zum Op machen: in der Server-Konsole `op <DeinName>`.

> Alternativ im Repo: `gradlew runServer` (Ordner `run\`, dort `eula.txt` und `server.properties` wie oben anlegen).

### Variante B – Einzelspieler
Die JAR zusätzlich im Client-`mods`-Ordner ablegen und eine Welt mit aktivierten Cheats erstellen. Die Mod ist serverseitig, läuft aber auch im integrierten Server.

### Start-Check
- [ ] Server-Log enthält `Yuletide loaded` und `Loaded 34 calendar gifts`, keine Warnungen/Fehler.
- [ ] Ordner `config/yuletide/` mit `config.json`, `gifts.json`, `messages.json` wurde erstellt.
- [ ] Client verbindet ohne Mod auf dem Client (Vanilla-Client genügt).

**Tipp für den Kalender:** Der Kalender ist nur im **Dezember** nutzbar. Außerhalb des Dezembers erst `/adventcalendar test` (als Op) ausführen.

---

## 2. Adventskalender

| # | Schritt | Erwartung | ✔ |
|---|---|---|---|
| 2.1 | `/adventcalendar` | 6-reihiges Menü mit 25 Türchen, farbig (grün/gelb/rot) | ☐ |
| 2.2 | Alias testen: `/ac`, `/calendar`, `/advent` | Wie 2.1 | ☐ |
| 2.3 | Item aus dem Menü ziehen, Shift-Klick, Doppelklick, Drag | Nichts lässt sich entnehmen oder verschieben | ☐ |
| 2.4 | Menü schließen, mit Inventar-Taste (E) | Schließt normal, nichts geht verloren | ☐ |
| 2.5 | `/adventcalendar test`, dann ein Türchen anklicken | Meldung „Test mode enabled“, Geschenk wird vergeben | ☐ |
| 2.6 | Dasselbe Türchen im Testmodus nochmals anklicken | Mehrfach abholbar (Testmodus-Verhalten) | ☐ |
| 2.7 | `/adventcalendar test` erneut | „Test mode disabled“ | ☐ |
| 2.8 | Im Dezember: heutiges Türchen anklicken | Geschenk + Meldung, Türchen wird gelb | ☐ |
| 2.9 | Heutiges Türchen nochmals | „Already claimed“ | ☐ |
| 2.10 | Zukünftiges Türchen anklicken | „Not available yet“ | ☐ |
| 2.11 | Inventar komplett füllen, Türchen anklicken | Überschuss wird vor dir gedroppt | ☐ |
| 2.12 | Server neu starten, Kalender öffnen | Abgeholte Tage sind weiterhin gelb | ☐ |
| 2.13 | `/adventcalendar reset <Name> 5` / `reset <Name> all` | Bestätigung, Tage wieder abholbar | ☐ |
| 2.14 | Ohne Op: `/adventcalendar reset …` und `test` | „You don't have permission“ | ☐ |
| 2.15 | Tab-Vervollständigung bei `/adventcalendar ` | Nur für Ops `reset`/`test` | ☐ |

**Geschenkgruppen:** `/tag <Name> add christmas_vip` → Kalender öffnen/abholen → Geschenk der Gruppe „vip“. Tag mit `/tag <Name> remove christmas_vip` wieder entfernen → Standardgeschenk.

---

## 3. Weihnachtsmann-Event

Als Op ausführen, am besten auf ebenem Boden.

| # | Schritt | Erwartung | ✔ |
|---|---|---|---|
| 3.1 | `/christmas santa start 30s` | Broadcast mit Welt/Koordinaten/`30s`, Santa-Villager mit Namensschild neben dir | ☐ |
| 3.2 | Santa ansehen | Schnee-Villager ohne Beruf, Name sichtbar | ☐ |
| 3.3 | Santa schlagen / mit Pfeil beschießen | Kein Schaden, bleibt am Leben | ☐ |
| 3.4 | Ein bis zwei Minuten beobachten (nutze `start 5m`) | Gelegentlich Geschenk-Drop mit Herz-Partikeln und Level-up-Sound | ☐ |
| 3.5 | Hinter Santa herlaufen | Schneeflocken-Partikel; dünne Schneeschichten, die nach ca. 5 s verschwinden | ☐ |
| 3.6 | `/christmas santa start` während er läuft | „already running“ | ☐ |
| 3.7 | Zeit abwarten | Santa verschwindet, Broadcast „Santa Claus has left“ | ☐ |
| 3.8 | `/christmas santa start 10m overworld 100 70 100` | Start an den Koordinaten | ☐ |
| 3.9 | Weit weglaufen, dann `/christmas santa teleport` | Du landest bei Santa | ☐ |
| 3.10 | `/christmas santa stop` | Santa weg, Schneespuren zurückgesetzt, Broadcast | ☐ |
| 3.11 | `/christmas santa stop` nochmals | „event is not running“ | ☐ |
| 3.12 | `/christmas santa start 1m random` | Start an einer konfigurierten Position + Zufalls-Broadcast | ☐ |
| 3.13 | Event starten, Server stoppen und neu starten | Kein übrig gebliebener Santa in der Welt | ☐ |
| 3.14 | Falsche Eingaben: `start 5m foo 0 0 0`, `start 5m overworld a b c` | Meldung „World not found“ bzw. „Invalid coordinates“ | ☐ |
| 3.15 | Ohne Op: `/christmas santa start` | Keine Berechtigung | ☐ |

---

## 4. Knallbonbons (Crackers)

| # | Schritt | Erwartung | ✔ |
|---|---|---|---|
| 4.1 | `/cracker list` | Header + `basic`, `premium` | ☐ |
| 4.2 | `/cracker get basic` | Papier „Basic Cracker“ mit Glanz und Lore im Inventar | ☐ |
| 4.3 | `/cracker get premium 5` | 5 Premium-Cracker (Texture-Pack mit Model-Data optional) | ☐ |
| 4.4 | `/cracker give <anderer Spieler> basic 3` | Beide bekommen Meldungen, Ziel hat 3 Stück | ☐ |
| 4.5 | Rechtsklick in die Luft mit Cracker | 1 Stück verbraucht, alle Belohnungen (Diamant, 3 Smaragde, 2 Goldäpfel) | ☐ |
| 4.6 | Rechtsklick auf einen Block / eine Truhe | Cracker wird geöffnet, Truhe öffnet sich nicht | ☐ |
| 4.7 | Cracker in der **Nebenhand**, Haupthand leer | Genau der Nebenhand-Stack wird verbraucht | ☐ |
| 4.8 | Cracker in der Haupthand, anderes Item in der Nebenhand | Nebenhand bleibt unberührt | ☐ |
| 4.9 | Umbenanntes Papier „Basic Cracker“ aus dem Amboss | Wird **nicht** als Cracker erkannt | ☐ |
| 4.10 | Voll gefülltes Inventar + `/cracker get basic` | „Your inventory is full“, nichts vergeben | ☐ |
| 4.11 | `/cracker give <Name> basic 0` und `65` | „Amount must be between 1 and 64“ | ☐ |
| 4.12 | `/cracker give <Name> foo`, unbekannter Spieler | „Cracker type not found“ / „Player not found“ | ☐ |
| 4.13 | Tab-Vervollständigung bei `give`/`get` | Spieler, Typen, Mengen `1,5,10,32,64` | ☐ |

---

## 5. Schnee-Effekt

| # | Schritt | Erwartung | ✔ |
|---|---|---|---|
| 5.1 | `/christmas snow` | „Snow effect enabled“, Schneeflocken fallen um dich | ☐ |
| 5.2 | Zweiter Spieler daneben | Sieht **deinen** Schnee nicht | ☐ |
| 5.3 | `/christmas snow 1` … `5` | Intensität steigt spürbar, Meldung mit Stufe | ☐ |
| 5.4 | `/christmas snow 6` / `abc` | „Snow level must be between 0 and 5“ | ☐ |
| 5.5 | `/christmas snow 0` oder erneut `/christmas snow` | Schnee aus | ☐ |
| 5.6 | Schnee an, in den Nether gehen | Kein Schnee dort; zurück in die Oberwelt → wieder Schnee | ☐ |
| 5.7 | Neu verbinden (Rejoin ohne Serverneustart) | Einstellung bleibt erhalten | ☐ |
| 5.8 | In `config.json` `snow-effect.enabled: false`, `/christmas reload`, `/christmas snow` | „not enabled globally“ | ☐ |

---

## 6. Konfiguration, Sprache & Sammelbefehl

| # | Schritt | Erwartung | ✔ |
|---|---|---|---|
| 6.1 | `config.json` ändern (z. B. Santa-Dauer), `/christmas reload` | „Configuration reloaded successfully!“, Änderung aktiv | ☐ |
| 6.2 | `general.language` auf `de` und Dateien `messages.json` löschen, `reload` | Meldungen auf Deutsch | ☐ |
| 6.3 | Kaputtes JSON in `gifts.json`, `reload` | Fehler im Log, Standardwerte bleiben aktiv, kein Absturz | ☐ |
| 6.4 | `/yuletide` | Usage-Zeile | ☐ |
| 6.5 | `/yuletide calendar`, `/yuletide snow`, `/yuletide cracker list`, `/yuletide santa stop`, `/yuletide reload` | Verhalten wie die Einzelbefehle | ☐ |
| 6.6 | `/christmas`, `/cracker`, `/christmas santa` | Jeweils passende Usage-Zeile | ☐ |
| 6.7 | Konsolen-Befehl `/christmas snow` und `/adventcalendar` | „players only“ | ☐ |

---

## 7. Berechtigungen (mit einem Nicht-Op-Spieler)

| Befehl | Erwartung | ✔ |
|---|---|---|
| `/adventcalendar`, `/cracker list`, `/christmas snow` | Funktionieren | ☐ |
| `/adventcalendar reset|test` | Abgelehnt | ☐ |
| `/christmas santa …`, `/christmas reload` | Abgelehnt | ☐ |
| `/cracker give|get` | Abgelehnt | ☐ |
| Tab-Vervollständigung | Zeigt nur erlaubte Unterbefehle | ☐ |

---

## 8. Fehler melden

Notiere bei jedem Problem:
- **Schritt-Nr.** und was du getan hast
- **Erwartet vs. tatsächlich**
- **Auszug aus `logs/latest.log`** (Serverseite, Zeilen mit `yuletide`, `WARN`, `ERROR`)
- Minecraft-/Fabric-/Mod-Version

| Schritt | Beobachtung | Log-Auszug |
|---|---|---|
| | | |

**Gesamtergebnis:** ☐ bestanden ☐ bestanden mit Anmerkungen ☐ nicht bestanden
