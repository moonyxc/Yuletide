# 🎄 Yuletide

Serverseitige Fabric-Mod für **Minecraft 26.1.2** (Java 25) – Adventskalender, Weihnachtsmann-Event, Knallbonbons und Schneeeffekt nach `docs/PLAN.md` (Teil A). Spieler brauchen keine Mod auf dem Client.

## Installation

`./gradlew build` → `build/libs/yuletide-*.jar` in den `mods`-Ordner legen (zusammen mit der Fabric API). Beim ersten Start entstehen `config/yuletide/config.json`, `gifts.json` und `messages.json`.

## Befehle

| Befehl | Recht | Beschreibung |
|---|---|---|
| `/adventcalendar` (`ac`, `calendar`, `advent`) | alle | Kalender öffnen |
| `/adventcalendar reset <spieler> <tag\|all>`, `test` | Op | Tage zurücksetzen, Testmodus |
| `/christmas snow [0-5]` (`xmas`) | alle | Schnee für sich selbst umschalten bzw. Stufe setzen |
| `/christmas santa start [zeit] [welt x y z \| random]`, `stop`, `teleport` | Op | Santa-Event |
| `/christmas reload` | Op | Konfiguration neu laden |
| `/cracker list` (`crackers`) | alle | Cracker-Typen auflisten |
| `/cracker give <spieler> <typ> [1-64]`, `get <typ> [anzahl]` | Op | Cracker vergeben |
| `/yuletide <calendar\|snow\|santa\|cracker\|reload>` | wie oben | Sammelbefehl |

„Op“ entspricht Permission-Level 2.

## Geschenkgruppen

Gruppen stehen in `config.json` unter `gift-groups`. Ein Spieler gehört zu einer Gruppe, wenn er das dort angegebene Entity-Tag trägt (z. B. `/tag <spieler> add christmas_vip`). Die höchste Priorität gewinnt, Fallback ist `default`.

## Umfang

Umgesetzt ist **Teil A** der Spezifikation inklusive der Fehlerbehebungen aus Abschnitt 8 sowie `/yuletide`, Deutsch/Englisch (`general.language`), Zeitzone (`advent-calendar.timezone`) und Kalenderfortschritt pro Jahr. Die umfangreichen Inhalts-Features aus **Teil B** (Wichteldorf, Shop, Quests, Mini-Games, Cosmetics usw.) sind bewusst nicht enthalten.
