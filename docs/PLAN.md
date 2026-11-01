# 🎄 YULETIDE – Weihnachts-Mod (Spezifikation, basiert auf ChristmasPlugin)

> **Modname:** **Yuletide** · Mod-ID: `yuletide` · Untertitel: *„Dein Server im Winterwunderland“*
> **Plattform-Empfehlung:** Fabric/NeoForge (alternativ MCreator, siehe 7). Alle Features aus Teil A (Original) sind Pflicht, Teil B (Abschnitt 10+) macht daraus die **Ausbaustufe**.
> **Aufbau:** Teil A = vollständige Beschreibung des Original-Plugins (Abschnitte 0–9) · Teil B = neue Features, Verbesserungen und Roadmap (Abschnitte 10–16).
> **Name im Code:** Das Original heißt `ChristmasPlugin`; in der Mod überall `Yuletide` / `yuletide:` als Namespace verwenden.

---

# TEIL A – Das Original-Plugin (ChristmasPlugin)

## Dokumentziel

> Dieses Dokument beschreibt das Paper-Plugin **ChristmasPlugin v1.1.0** (Autor: niliees, Paket `fun.vynofc.christmasPlugin`) **vollständig**.
> Ziel: Aus diesem Dokument allein soll eine funktional gleichwertige **Minecraft-Mod** (z. B. Fabric / NeoForge / MCreator) gebaut werden können, ohne den Plugin-Quellcode lesen zu müssen.

---

## 0. Eckdaten des Originals

| Eigenschaft | Wert |
|---|---|
| Plattform | Paper (Server-Plugin, `paper-plugin.yml`) |
| Minecraft-Version | **26.2+** (`api-version: '26.2'`, `paper-api [26.2.build,)`) |
| Java | **25** |
| Version | 1.1.0 |
| Optionale Abhängigkeiten | Vault (Rechte vergeben), WorldGuard (nur erkannt, nicht genutzt) |
| Ladezeitpunkt | `POSTWORLD` |
| Persistente Daten | `calendar-data.yml` (abgeholte Kalendertage pro Spieler-UUID) |
| Konfigurationsdateien | `config.yml`, `gifts.yml`, `messages.yml` |

### Die 4 Features

1. **Adventskalender** – GUI mit 25 Türchen (1.–25. Dezember), gruppenabhängige Belohnungen, Speicherung pro Spieler.
2. **Weihnachtsmann-Event** – spawnt ein „Santa“-Wesen, das zufällig Geschenke fallen lässt und eine Schneespur hinterlässt; zeitlich begrenzt.
3. **Knallbonbons (Christmas Crackers)** – spezielle Items, die man per Rechtsklick öffnet, um alle darin enthaltenen Belohnungen zu bekommen.
4. **Schnee-Effekt** – pro Spieler umschaltbare, clientseitig sichtbare Schneeflocken-Partikel um den Spieler.

---

## 1. Architektur (Original)

```
ChristmasPlugin (Hauptklasse, Singleton)
├── ConfigManager          – lädt messages.yml und gifts.yml
├── MessageManager         – Texte, Prefix, Platzhalter, &-Farbcodes
├── GiftManager            – Kalender-Geschenke aus gifts.yml, Gruppenwahl, Ausgabe
├── AdventCalendarManager  – GUI, Freischalt-Logik, Speicherung, Testmodus
├── SantaManager           – Santa-Event (Spawn, Timer, Geschenke, Schneespur)
├── CrackerManager         – Cracker-Typen, Item-Erstellung, Öffnen
├── SnowEffectManager      – Schnee-Partikel pro Spieler
├── Commands: /adventcalendar, /christmas, /cracker
├── Listeners: AdventCalendarListener (GUI-Klicks), CrackerListener (Rechtsklick), SantaListener (Schaden)
└── Models: Gift, GiftType {ITEM, COMMAND, PERMISSION}
```

### Lebenszyklus

**Start (`onEnable`)**
1. Standarddateien `config.yml`, `messages.yml`, `gifts.yml` in den Datenordner kopieren, falls nicht vorhanden.
2. `ConfigManager` lädt `messages.yml` und `gifts.yml`.
3. Vault einbinden (per Reflection, falls installiert und Permission-Provider vorhanden).
4. WorldGuard erkennen (setzt nur ein Flag, sonst keine Funktion).
5. Manager in dieser Reihenfolge erstellen: Message → Gift → AdventCalendar → Santa → Cracker → SnowEffect (der Schnee-Task startet sofort).
6. Befehle programmatisch registrieren (Namespace `christmasplus`).
7. Listener registrieren.

**Stopp (`onDisable`)**
1. Laufendes Santa-Event beenden (Wesen entfernen, Tasks abbrechen, Broadcast).
2. Schnee-Task stoppen.
3. Kalenderdaten speichern.

**Reload (`/christmas reload`)**
`config.yml`, `messages.yml`, `gifts.yml` neu laden; Kalenderdaten neu von der Festplatte laden; Cracker-Typen neu laden; Schnee-Task neu starten. (Ein laufendes Santa-Event bleibt unberührt.)

---

## 2. Befehle

Alle Befehle haben eine Grundberechtigung (siehe Abschnitt 3). Ausgaben laufen über die Nachrichtenschlüssel aus `messages.yml` (Abschnitt 6). Konsolen-Sonderfall-Texte sind fest codiert (`§c…`).

- Fehlt ein Unterrecht → `general.no-permission`.
- Reine Spieler-Befehle (in den Tabellen „nur Spieler“) melden der Konsole den festen Text `§cThis command can only be used by players!`.
- Spielernamen werden nur unter **Online-Spielern** gesucht (`Bukkit.getPlayer`).

### 2.1 `/adventcalendar` (Aliase: `ac`, `calendar`, `advent`) – Grundrecht `christmasplus.adventcalendar`

| Syntax | Recht | Nur Spieler | Verhalten |
|---|---|---|---|
| `/adventcalendar` | `christmasplus.adventcalendar` | ja | Öffnet die Kalender-GUI und sendet `advent-calendar.gui-opened`. |
| `/adventcalendar reset <spieler> <tag\|all>` | `christmasplus.adventcalendar.reset` | nein | `all`: alle Tage des Spielers zurücksetzen → `advent-calendar.reset-all-success`. Zahl 1–25: einen Tag zurücksetzen → `advent-calendar.reset-success`. Außerhalb von 1–25 → fester Text `§cDay must be between 1 and 25!`. Keine Zahl → `general.invalid-number` (`{input}`). Spieler offline/unbekannt → `general.player-not-found`. Weniger als 3 Argumente → `usage.advent-calendar`. |
| `/adventcalendar test` | `christmasplus.adventcalendar.test` | ja | Schaltet den Testmodus um (nicht gespeichert, nur im Speicher). Sendet `advent-calendar.test-mode-enabled` bzw. `…-disabled`. |
| sonstiges | – | – | `usage.advent-calendar` |

Tab-Vervollständigung: Argument 1 → `reset` / `test` (nur mit dem jeweiligen Recht); Argument 2 bei `reset` → Online-Spieler; Argument 3 bei `reset` → `all`, `1`…`25`.

### 2.2 `/christmas` (Alias: `xmas`) – Grundrecht `christmasplus.christmas`

| Syntax | Recht | Verhalten |
|---|---|---|
| `/christmas` | – | `usage.christmas` |
| `/christmas snow` | `christmasplus.christmas.snow` (nur Spieler) | Wenn `snow-effect.enabled` = false → `snow.not-enabled-globally`. Sonst Schnee für den Spieler umschalten → `snow.enabled` / `snow.disabled`. |
| `/christmas santa start [zeit] [welt x y z]` | `christmasplus.christmas.santa` | Siehe unten. |
| `/christmas santa start [zeit] random` | `christmasplus.christmas.santa` | Startet an einer zufälligen konfigurierten Position. |
| `/christmas santa stop` | `christmasplus.christmas.santa` | Läuft nichts → `santa.event-not-running`; sonst Event beenden. |
| `/christmas santa teleport` | `christmasplus.christmas.santa` (nur Spieler) | Läuft nichts → `santa.event-not-running`; sonst Teleport zur aktuellen Santa-Position → `santa.teleported-to-santa`. |
| `/christmas santa` (ohne Unterbefehl) / unbekannt | – | `usage.santa` |
| `/christmas reload` | `christmasplus.christmas.reload` | Reload (s. o.) → `general.config-reloaded`. |

**Ablauf von `santa start`:**
1. Läuft bereits ein Event → `santa.event-already-running`.
2. Dauer = `santa-claus.default-duration` (600 s).
3. Ist das nächste Argument ein Zeitstring (Regex `^\d+[smh]$`, z. B. `30s`, `5m`, `1h`), wird es in Sekunden umgerechnet (s = ×1, m = ×60, h = ×3600, Fehler → 600) und übersprungen.
4. Ist das nächste Argument `random` → `startEventRandom(dauer)`.
5. Folgen ≥ 4 weitere Argumente → `welt x y z`. Unbekannte Welt → `santa.invalid-world` (`{world}`). Keine Zahl bei den Koordinaten → `santa.invalid-duration` (Original-Verhalten, inhaltlich eigentlich der falsche Text).
6. Sonst: Wenn der Absender ein Spieler ist → Start an dessen Position. Konsole → fester Text `§cYou must specify coordinates when using from console!` + `usage.santa`.

Tab-Vervollständigung: Argument 1 → `snow`/`santa`/`reload` (nur mit dem jeweiligen Recht); Argument 2 nach `santa` → `start`, `stop`, `teleport`; Argument 3 nach `santa start` → `5m`, `10m`, `30m`, `1h`, `random`, alle Weltnamen; Argument 4 (wenn Argument 3 ein Zeitstring ist) → `random`, alle Weltnamen.

### 2.3 `/cracker` (Alias: `crackers`) – Grundrecht `christmasplus.cracker`

| Syntax | Recht | Verhalten |
|---|---|---|
| `/cracker` | – | `usage.cracker` |
| `/cracker list` | `christmasplus.cracker.list` | `crackers.list-header`, dann für jede Typ-ID `crackers.list-entry` (`{cracker}` = ID). |
| `/cracker give <spieler> <typ> [anzahl]` | `christmasplus.cracker.give` | Weniger als 3 Argumente → `usage.cracker`. Spieler unbekannt → `general.player-not-found`. Typ unbekannt → `crackers.cracker-not-found`. Anzahl Standard 1, erlaubt 1–64 (sonst `crackers.invalid-amount`; keine Zahl → `general.invalid-number`). Hat das Ziel **keinen leeren Slot** → `crackers.inventory-full` an den Absender, nichts wird vergeben. Sonst Item vergeben → Absender `crackers.given-to-player`, Ziel `crackers.received-from-player` (`{player}` = Name des Absenders, `{amount}` = Anzahl, `{cracker}` = **Typ-ID**, nicht der farbige Name). |
| `/cracker get <typ> [anzahl]` | `christmasplus.cracker.get` (nur Spieler) | Wie `give` an sich selbst. Für eine Anzahl-Angabe zusätzlich `christmasplus.cracker.get.multiple` nötig, sonst `crackers.no-permission-multiple`. Erfolg → `crackers.received-from-player` (`{player}` = eigener Name). |
| sonstiges | – | `usage.cracker` |

Tab-Vervollständigung: Argument 1 → `list`/`give`/`get` (nur mit Recht); `give` Argument 2 → Online-Spieler; `get` Argument 2 → Typ-IDs; `give` Argument 3 → Typ-IDs; `give` Argument 4 → `1,5,10,32,64`; `get` Argument 3 → `1,5,10,32,64` (nur mit `.multiple`).

---

## 3. Berechtigungen

| Recht | Standard | Beschreibung |
|---|---|---|
| `christmasplus.*` | op | Enthält alle unten (außer den Gruppenrechten) |
| `christmasplus.adventcalendar` | **alle** | Kalender öffnen |
| `christmasplus.adventcalendar.reset` | op | Tage zurücksetzen |
| `christmasplus.adventcalendar.test` | op | Testmodus |
| `christmasplus.christmas` | **alle** | `/christmas` |
| `christmasplus.christmas.snow` | **alle** | Schnee umschalten |
| `christmasplus.christmas.santa` | op | Santa-Event verwalten |
| `christmasplus.christmas.reload` | op | Reload |
| `christmasplus.cracker` | **alle** | `/cracker` |
| `christmasplus.cracker.list` | **alle** | Cracker auflisten |
| `christmasplus.cracker.give` | op | Cracker vergeben |
| `christmasplus.cracker.get` | op | Cracker für sich selbst |
| `christmasplus.cracker.get.multiple` | op | Mehrere auf einmal |
| `christmasplus.group.vip` | false | Kalender-Geschenkgruppe „vip“ |
| `christmasplus.group.premium` | false | Gruppe „premium“ |
| `christmasplus.group.donor` | false | Gruppe „donor“ |

**Mod-Hinweis:** In einer Mod gibt es kein Bukkit-Rechtesystem. Empfohlen: „op“ → Permission-Level 2 (`source.hasPermissionLevel(2)`), „alle“ → Level 0. Für die Gruppenrechte entweder die Fabric Permissions API / LuckPerms (optional) nutzen oder Gruppen über Scoreboard-Tags (z. B. Tag `christmas_vip`) abbilden.

---

## 4. Feature-Details

### 4.1 Adventskalender

**Freischaltlogik `canClaimDay(spieler, tag)`** (in dieser Reihenfolge):
1. Testmodus aktiv → **immer true** (auch für bereits abgeholte Tage! → mehrfach abholbar).
2. Bereits abgeholt → false.
3. Aktueller Monat (Systemzeit des Servers, `LocalDate.now()`) ist nicht Dezember → false.
4. `advent-calendar.allow-previous-days` = true → `tag <= heutigerTag`, sonst `tag == heutigerTag`.

(`advent-calendar.enabled`, `start-date`, `end-date` stehen in der Config, werden aber **nicht** ausgewertet.)

**GUI**
- Truhen-GUI, Größe `advent-calendar.gui.size` (Standard 54 = 6 Reihen), Titel `advent-calendar.gui.title` (Farbcodes übersetzt).
- Ist `fill-empty-slots` = true, werden alle Slots mit `fill-material` (Standard `RED_STAINED_GLASS_PANE`, bei ungültigem Wert ebenfalls dieses) und dem Namen `" "` gefüllt.
- Feste Slot-Zuordnung (Tag → Slot):

| Tage | Slots |
|---|---|
| 1–7 | 10, 11, 12, 13, 14, 15, 16 |
| 8–14 | 19, 20, 21, 22, 23, 24, 25 |
| 15–21 | 28, 29, 30, 31, 32, 33, 34 |
| 22–25 | 37, 38, 39, 40 |

- Tages-Item je nach Status:

| Status | Material (Config-Schlüssel, Standard) | Name | Lore |
|---|---|---|---|
| abgeholt | `day-item.claimed` = `YELLOW_WOOL` | `&e&lDay N` | `&7Status: &eClaimed`, leer, `&7You already claimed this day!` |
| verfügbar | `day-item.available` = `GREEN_WOOL` | `&a&lDay N` | `&7Status: &aAvailable`, leer, `&aClick to claim your gifts!` |
| gesperrt | `day-item.locked` = `RED_WOOL` | `&c&lDay N` | `&7Status: &cLocked`, leer, `&cCome back on December N!` |

(„abgeholt“ hat Vorrang vor „verfügbar“; im Testmodus erscheinen abgeholte Tage also gelb, sind aber trotzdem anklickbar.)

**Klick-Verhalten**
- Jeder Klick in einem Inventar mit genau diesem Titel wird abgebrochen (Items lassen sich nicht herausnehmen).
- Der Tag wird aus dem farbcode-bereinigten Anzeigenamen `Day N` gelesen.
- Nicht abholbar: bereits abgeholt → `advent-calendar.already-claimed`; sonst → `advent-calendar.not-available-yet` (`{day}`).
- Abholbar: `claimDay` → GUI schließen → nach **5 Ticks** neu öffnen (dabei wird erneut `gui-opened` gesendet).

**`claimDay(spieler, tag)`**
1. Nochmals `canClaimDay` prüfen.
2. Geschenkliste über `GiftManager.getGiftsForDay(tag, spieler)` holen. Leer → Warnung ins Log, **nichts wird als abgeholt markiert**.
3. Geschenke ausgeben, Tag als abgeholt markieren und **sofort speichern**.
4. `advent-calendar.claimed-success` (`{day}`, `{amount}` = Anzahl Geschenk-Einträge).

**Gruppenwahl (`getPlayerGroup`)**
- Alle Einträge unter `gift-groups` in `config.yml` durchgehen. Eine Gruppe gilt, wenn sie **kein** `permission` hat oder der Spieler das Recht besitzt.
- Die gültige Gruppe mit der **höchsten `priority`** gewinnt (Standard „default“).
- Geschenke = `gifts.yml → day-N.groups.<gruppe>`; existiert die Gruppe für den Tag nicht → Fallback `default`; existiert auch die nicht → leer.
- **Keine Vererbung**: Ein VIP bekommt nur die VIP-Liste, nicht zusätzlich die Default-Liste.

**Geschenkausgabe (`giveGifts`)**
- `ITEM`: Item ins Inventar; was nicht passt, wird am Spieler gedroppt.
- `COMMAND`: `{player}` durch den Spielernamen ersetzen, als Konsole ausführen.
- `PERMISSION`: nur wenn Vault aktiv ist → `Permission.playerAdd(spieler, recht)`. `duration` wird ignoriert (immer dauerhaft).

**Persistenz `calendar-data.yml`**
```yaml
<spieler-uuid>:
- 1
- 2
- 5
```
Wird bei jedem Abholen/Zurücksetzen und beim Herunterfahren gespeichert. Hinweis: Bei `resetAll` wird der Eintrag nur aus dem Speicher entfernt, der alte Schlüssel bleibt in der Datei stehen (Fehler, siehe Abschnitt 8). Der Testmodus wird nicht gespeichert.

### 4.2 Weihnachtsmann-Event

**Start (`startEvent(ort, dauerSek)`)** – nur wenn kein Event läuft:
1. Santa spawnen:
   - Wesen-Typ `santa-claus.entity-type` (Standard `VILLAGER`, ungültig → Villager).
   - Name `santa-claus.name` (`&c&lSanta Claus &f&l🎅`), Namensschild sichtbar laut `show-name-tag`.
   - Despawnt nicht durch Entfernung; KI an; keine Kollision.
   - Bei Villager: Beruf `NONE`, Biom-Typ `SNOW`.
2. Timer: nach `dauer × 20` Ticks → `stopEvent()`.
3. Geschenk-Drop (wenn `gift-drop.enabled`): alle `interval` Sekunden (Standard 5) mit `chance` % (Standard 15) ein Geschenk an Santas Position fallen lassen:
   - Zufällig eines von: `DIAMOND, EMERALD, GOLD_INGOT, GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE, COOKIE, CAKE, PUMPKIN_PIE` (Anzahl 1).
   - 10 `HEART`-Partikel (Streuung 0,5; Geschwindigkeit 0,1), Sound `ENTITY_PLAYER_LEVELUP` (Lautstärke 0,5, Tonhöhe 1,5).
   - (Die Nachricht `santa.gift-dropped` wird **nicht** gesendet.)
4. Schneespur (wenn `snow-trail.enabled`): alle **10 Ticks**:
   - `particle-amount` (10) `SNOWFLAKE`-Partikel an Santa (Streuung 0,5; Geschwindigkeit 0,01).
   - Ist der Block unter Santa fest und der Block auf Santas Höhe Luft → `SNOW` (Schneeschicht) setzen und nach `fade-delay` Ticks (100) wieder zu Luft machen, falls dort noch Schnee liegt.
   - Beide Tasks brechen ab, wenn Santa tot oder entfernt ist.
5. Broadcast an alle Online-Spieler: `santa.event-started` mit `{world}`, `{x}`, `{y}`, `{z}` (Block-Koordinaten des Startpunkts), `{time}` (formatiert: <60 → `Ns`, <3600 → `Nm`, sonst `Nh`; ganzzahlig abgerundet).

**Zufallsstart (`startEventRandom`)**: zufällige Position aus `santa-claus.spawn-locations` → `startEvent` → zusätzlich Broadcast `santa.event-started-random`. Keine Positionen → nur Log-Warnung (`santa.no-spawn-locations` wird nicht genutzt).

**Stopp (`stopEvent`)**: alle Tasks abbrechen, Santa entfernen, Broadcast `santa.event-stopped`.

**Schutz von Santa**: Gedacht ist, dass Santa unverwundbar ist. Die Original-Umsetzung ist fehlerhaft (siehe Abschnitt 8). **Für die Mod:** Santa-UUID merken und jeglichen Schaden an genau diesem Wesen abbrechen (bzw. `setInvulnerable(true)`).

Nicht ausgewertete Config-Schlüssel: `santa-claus.enabled`, `skin.*`, `speed`, `can-fly`, `ignore-worldguard`.

### 4.3 Knallbonbons (Crackers)

**Typ-Definition** (aus `config.yml → crackers.types.<id>`):

| Feld | Standard | Bedeutung |
|---|---|---|
| `name` | `&eCracker` | Anzeigename (Farbcodes) – **dient im Original zur Erkennung!** |
| `material` | `PAPER` | Basis-Item (groß/klein-sensitiv, **kein Fallback** – ungültig → Exception beim Laden) |
| `custom-model-data` | 0 | Wenn > 0, wird CustomModelData gesetzt |
| `lore` | – | Beschreibungszeilen |
| `glow` | false | Wenn true: Verzauberung `UNBREAKING 1` + `HIDE_ENCHANTS` (Leuchteffekt) |
| `rewards` | – | Liste von Belohnungen (s. u.) |

**Belohnungen** (Liste von Maps):
- `type: ITEM`, `item: <MATERIAL>`, `amount: <int, Standard 1>`
- `type: COMMAND`, `command: "<befehl mit {player}>"`
- `type: PERMISSION`, `permission`, `duration` – wird geladen, beim Öffnen aber **übersprungen**.
- Ohne `type` bzw. ohne Pflichtfeld → Eintrag wird verworfen (Log-Warnung).
- Unbekannter `type` (z. B. `XP`) wird geladen, beim Öffnen aber stillschweigend ignoriert.
- Ungültiges Material bei ITEM → beim Öffnen Log-Warnung, Belohnung entfällt.
- Ein `chance`-Wert existiert intern (immer 100), wird nicht genutzt: **Es gibt immer alle Belohnungen.**

**Öffnen (Rechtsklick in Luft oder auf Block)**
1. Hält der Spieler ein Item, dessen Anzeigename exakt dem eingefärbten `name` eines Typs entspricht → Event abbrechen (kein Platzieren/Benutzen).
2. Ein Item vom Stapel entfernen (bei 1 → Haupthand leeren; Achtung: auch wenn das Item in der Nebenhand war, siehe Abschnitt 8).
3. `crackers.opened` (`{cracker}` = eingefärbter Name).
4. Für jede Belohnung:
   - ITEM: Item vergeben, Überschuss droppen, `crackers.received-item` (`{item}` = Material-Name, `{amount}`).
   - COMMAND: `{player}` ersetzen, als Konsole ausführen, `crackers.received-command-reward`.

**Mod-Hinweis:** Erkennung nicht über den Namen, sondern über ein eigenes Item (z. B. `christmas:cracker`) mit einer Data Component / NBT `cracker_type: "<id>"`, oder ein eigenes Item pro Typ.

### 4.4 Schnee-Effekt

- Globaler Schalter `snow-effect.enabled`; wenn false, läuft kein Task, und `/christmas snow` meldet `snow.not-enabled-globally`.
- Pro Spieler umschaltbar (nur im Speicher, nach UUID: bleibt beim Neuverbinden erhalten, geht beim Serverneustart verloren). Beim Einschalten wird die Stufe auf `default-intensity` (3) gesetzt.
- Task läuft **jeden Tick**. Für jeden Spieler mit aktivem Schnee, der nicht in einer Welt aus `disabled-worlds` ist:
  - Stufe L (1–5) → `particles-per-tick` und `radius` aus `snow-effect.levels.L` (Fallback 3 / 15).
  - Für jeden Partikel: x/z-Versatz zufällig in `[-radius, +radius)`, y-Versatz zufällig in `[5, 15)` über dem Spieler.
  - `SNOWFLAKE`-Partikel, Anzahl 1, Versatz (0, -0,5, 0), Geschwindigkeit 0,02 – **nur für diesen Spieler sichtbar** (`player.spawnParticle`).
- Es gibt `setSnowLevel(1–5)`, aber **keinen Befehl dafür** (Nachricht `snow.intensity-changed` ungenutzt).

**Mod-Hinweis:** Ideal clientseitig umsetzen (Client-Tick + `ParticleTypes.SNOWFLAKE`), der Server schickt nur „an/aus + Stufe“ per Netzwerkpaket. Alternativ serverseitig `ServerLevel.sendParticles(player, …)` nur an diesen Spieler.

---

## 5. Konfiguration (Standardwerte)

### 5.1 `config.yml`

```yaml
general:
  prefix: "&c&l[Christmas] &r"   # ungenutzt (Prefix kommt aus messages.yml)
  language: "en"                  # ungenutzt

advent-calendar:
  enabled: true                   # ungenutzt
  allow-previous-days: true
  start-date: "12-01"             # ungenutzt
  end-date: "12-25"               # ungenutzt
  gui:
    title: "&c&lAdvent Calendar"
    size: 54
    fill-empty-slots: true
    fill-material: "RED_STAINED_GLASS_PANE"
    day-item:
      available: "GREEN_WOOL"
      claimed: "YELLOW_WOOL"
      locked: "RED_WOOL"

santa-claus:
  enabled: true                   # ungenutzt
  default-duration: 600
  entity-type: "VILLAGER"
  name: "&c&lSanta Claus &f&l🎅"
  show-name-tag: true
  skin: { enabled: false, texture: "", signature: "" }   # ungenutzt
  speed: 0.3                      # ungenutzt
  can-fly: false                  # ungenutzt
  gift-drop:
    enabled: true
    chance: 15.0                  # Prozent
    interval: 5                   # Sekunden
  snow-trail:
    enabled: true
    fade-delay: 100               # Ticks
    particle-amount: 10
  spawn-locations:                # Liste (siehe Fehler in Abschnitt 8)
    - { world: "world", x: 0,   y: 64, z: 0 }
    - { world: "world", x: 100, y: 64, z: 100 }
  ignore-worldguard: true         # ungenutzt

crackers:
  enabled: true                   # ungenutzt
  types:
    basic:
      name: "&e&lBasic Cracker"
      material: "PAPER"
      custom-model-data: 0
      lore: ["&7Right-click to open!", "&7You will receive all items inside!"]
      glow: true
      rewards:
        - { type: "ITEM", item: "DIAMOND", amount: 1 }
        - { type: "ITEM", item: "EMERALD", amount: 3 }
        - { type: "COMMAND", command: "give {player} golden_apple 2" }
    premium:
      name: "&6&lPremium Cracker"
      material: "PAPER"
      custom-model-data: 1
      lore: ["&7Right-click to open!", "&6Premium rewards inside!", "&7You will receive all items!"]
      glow: true
      rewards:
        - { type: "ITEM", item: "DIAMOND", amount: 5 }
        - { type: "ITEM", item: "NETHERITE_SCRAP", amount: 2 }
        - { type: "ITEM", item: "ENCHANTED_GOLDEN_APPLE", amount: 1 }
        - { type: "COMMAND", command: "give {player} netherite_ingot 1" }

snow-effect:
  enabled: true
  default-intensity: 3
  levels:
    1: { particles-per-tick: 1,  radius: 10 }
    2: { particles-per-tick: 3,  radius: 15 }
    3: { particles-per-tick: 5,  radius: 20 }
    4: { particles-per-tick: 8,  radius: 25 }
    5: { particles-per-tick: 12, radius: 30 }
  disabled-worlds: ["world_nether", "world_the_end"]

gift-groups:
  default: { priority: 0 }
  vip:     { priority: 1, permission: "christmasplus.group.vip" }
  premium: { priority: 2, permission: "christmasplus.group.premium" }
  donor:   { priority: 3, permission: "christmasplus.group.donor" }
```

### 5.2 `gifts.yml` – Format

```yaml
day-<1..25>:
  groups:
    <gruppenname>:
      items:
        - type: ITEM            # ITEM | COMMAND | PERMISSION (Standard ITEM)
          material: DIAMOND     # Standard STONE; ungültig → STONE
          amount: 1             # Standard 1
          name: "&bName"        # optional
          lore: ["&7..."]       # optional
          enchantments: ["SHARPNESS:3", "UNBREAKING:2"]   # NAME:LEVEL, ignoriert Level-Grenzen
          stored-enchantments: ["MENDING:1"]              # für Verzauberte Bücher (im Original ignoriert!)
        - type: COMMAND
          command: "give {player} emerald 5"
        - type: PERMISSION
          permission: "some.permission"
          duration: -1          # ignoriert
```

> ⚠️ Die Einträge unter `items` sind eine **YAML-Liste**, der Original-Code liest sie aber als **Map** (`getConfigurationSection`). Siehe Abschnitt 8 – in der Mod als Liste einlesen.

### 5.3 Standard-Geschenke (`gifts.yml`)

Gruppe `default`, wenn nicht anders angegeben. Verz. = Verzauberungen.

| Tag | Belohnungen |
|---|---|
| 1 | **default:** 1× Diamant „&b&lDay 1 Diamond“ (Lore „&7A special gift for day 1!“) · **vip:** 3× Diamant „&b&lDay 1 VIP Diamond“ (Lore „&7A special VIP gift!“) + Befehl `give {player} emerald 5` |
| 2 | 2× Goldener Apfel |
| 3 | 1× Verzauberter Goldener Apfel |
| 4 | Befehl `give {player} netherite_scrap 1` |
| 5 | Diamantschwert (Schärfe 3, Haltbarkeit 2) |
| 6 | 10× Smaragd |
| 7 | Verzaubertes Buch (gespeichert: Reparatur 1) |
| 8 | Totem der Unsterblichkeit |
| 9 | Elytren |
| 10 | 5× Diamantblock |
| 11 | 1× Netheritbarren |
| 12 | Befehl `give {player} diamond 10` |
| 13 | Verzaubertes Buch (gespeichert: Glück 3) |
| 14 | Diamantspitzhacke (Effizienz 5, Haltbarkeit 3) |
| 15 | Leuchtfeuer |
| 16 | 3× Netherstern |
| 17 | 5× Verzauberter Goldener Apfel |
| 18 | Diamantbrustplatte (Schutz 4, Haltbarkeit 3) |
| 19 | Dreizack (Treue 3) |
| 20 | Shulkerkiste |
| 21 | 3× Netheritbarren |
| 22 | Verzaubertes Buch (gespeichert: Schärfe 5) |
| 23 | Drachenkopf |
| 24 | Netheritschwert (Schärfe 5, Plünderung 3, Haltbarkeit 3) + Befehl `give {player} emerald_block 10` |
| 25 | **default:** Netherit-Brustplatte (Schutz 4, Haltbarkeit 3, Reparatur 1), Netherit-Helm, -Hose, -Stiefel (je Schutz 4, Haltbarkeit 3) + Befehl `give {player} diamond_block 32` · **vip:** Netherit-Brustplatte (Schutz 4, Haltbarkeit 3, Reparatur 1, Dornen 3) + Recht `christmasplus.christmas.vip` (dauerhaft) |

Material-IDs (für Mods in Kleinbuchstaben mit `minecraft:`): `diamond, golden_apple, enchanted_golden_apple, diamond_sword, emerald, enchanted_book, totem_of_undying, elytra, diamond_block, netherite_ingot, diamond_pickaxe, beacon, nether_star, diamond_chestplate, trident, shulker_box, dragon_head, netherite_sword, netherite_chestplate, netherite_helmet, netherite_leggings, netherite_boots`.
Verzauberungs-IDs: `sharpness, unbreaking, mending, fortune, efficiency, protection, loyalty, looting, thorns`.

---

## 6. Nachrichten (`messages.yml`)

- `prefix: "&c&l[Christmas] &r"` – ersetzt `{prefix}` in jeder Nachricht.
- Farbcodes: `&` → `§` (Legacy). MiniMessage wird zwar erwähnt, aber nicht verwendet.
- Fehlender Schlüssel → `&cMessage not found: <pfad>`.
- Platzhalter: `{prefix}`, `{player}`, `{day}`, `{time}`, `{cracker}`, `{amount}`, `{world}`, `{x}`, `{y}`, `{z}`, `{input}`, `{item}`.

| Schlüssel | Text |
|---|---|
| general.no-permission | `{prefix}&cYou don't have permission to do that!` |
| general.player-not-found | `{prefix}&cPlayer '{player}' not found!` |
| general.invalid-number | `{prefix}&cInvalid number: {input}` |
| general.config-reloaded | `{prefix}&aConfiguration reloaded successfully!` |
| advent-calendar.gui-opened | `{prefix}&aOpened the advent calendar!` |
| advent-calendar.already-claimed | `{prefix}&cYou have already claimed this day's gift!` |
| advent-calendar.not-available-yet | `{prefix}&cThis day is not available yet! Come back on day {day}!` |
| advent-calendar.previous-days-disabled | `{prefix}&cYou can only claim today's gift!` *(ungenutzt)* |
| advent-calendar.claimed-success | `{prefix}&aYou have claimed your gift for day {day}!` |
| advent-calendar.reset-success | `{prefix}&aReset day {day} for player {player}!` |
| advent-calendar.reset-all-success | `{prefix}&aReset all days for player {player}!` |
| advent-calendar.test-mode-enabled | `{prefix}&aTest mode enabled! You can now claim all gifts.` |
| advent-calendar.test-mode-disabled | `{prefix}&cTest mode disabled!` |
| advent-calendar.not-in-calendar-period | `{prefix}&cThe advent calendar is only available in December!` *(ungenutzt)* |
| advent-calendar.multiple-gifts | `{prefix}&aYou received {amount} gifts!` *(ungenutzt)* |
| santa.event-started | `{prefix}&aSanta Claus has arrived at &e{world} ({x}, {y}, {z})&a!` |
| santa.event-started-random | `{prefix}&aSanta Claus has arrived somewhere in the world!` |
| santa.event-stopped | `{prefix}&cSanta Claus has left! See you next time!` |
| santa.event-already-running | `{prefix}&cSanta Claus event is already running!` |
| santa.event-not-running | `{prefix}&cSanta Claus event is not running!` |
| santa.teleported-to-santa | `{prefix}&aYou have been teleported to Santa Claus!` |
| santa.invalid-duration | `{prefix}&cInvalid duration format! Use format like: 5m, 10s, 1h` |
| santa.invalid-world | `{prefix}&cWorld '{world}' not found!` |
| santa.gift-dropped | `{prefix}&aSanta Claus dropped a gift!` *(ungenutzt)* |
| santa.no-spawn-locations | `{prefix}&cNo spawn locations configured for random spawn!` *(ungenutzt)* |
| crackers.opened | `{prefix}&aYou opened a {cracker}&a!` |
| crackers.received-item | `{prefix}&aYou received &e{item} x{amount}&a!` |
| crackers.received-permission | `{prefix}&aYou received permission: &e{permission}&a!` *(ungenutzt)* |
| crackers.received-command-reward | `{prefix}&aYou received a special reward!` |
| crackers.given-to-player | `{prefix}&aYou gave {amount}x {cracker} &ato {player}!` |
| crackers.received-from-player | `{prefix}&aYou received {amount}x {cracker} &afrom {player}!` |
| crackers.cracker-not-found | `{prefix}&cCracker type '{cracker}' not found!` |
| crackers.invalid-amount | `{prefix}&cAmount must be between 1 and 64!` |
| crackers.no-permission-multiple | `{prefix}&cYou can only get 1 cracker at a time!` |
| crackers.inventory-full | `{prefix}&cYour inventory is full!` |
| crackers.list-header | `{prefix}&aAvailable crackers:` |
| crackers.list-entry | `&7- &e{cracker}` |
| snow.enabled | `{prefix}&aSnow effect enabled!` |
| snow.disabled | `{prefix}&cSnow effect disabled!` |
| snow.not-enabled-globally | `{prefix}&cSnow effect is not enabled globally!` |
| snow.intensity-changed | `{prefix}&aSnow intensity set to level {level}!` *(ungenutzt)* |
| usage.advent-calendar | `&cUsage: /adventcalendar [reset <player> <day\|all> \| test]` |
| usage.santa | `&cUsage: /christmas santa <start [time] [world] [x] [y] [z] \| start [time] random \| stop \| teleport>` |
| usage.christmas | `&cUsage: /christmas <snow \| santa \| reload>` |
| usage.cracker | `&cUsage: /cracker <list \| give <player> <cracker> [amount] \| get <cracker> [amount]>` |

**Mod-Hinweis:** Texte als Sprachdatei `assets/christmas/lang/en_us.json` (+ `de_de.json`) mit `Component.translatable(...)` ablegen, oder – für serverseitige Mods ohne Client-Ressourcen – weiterhin aus einer Config lesen und `&`-Codes in `Style`/`Formatting` umwandeln.

---

## 7. Zuordnung Bukkit/Paper → Mod-API

| Plugin (Bukkit/Paper) | Fabric (Yarn/Mojmap) | NeoForge |
|---|---|---|
| `JavaPlugin.onEnable/onDisable` | `ModInitializer.onInitialize`, `ServerLifecycleEvents.SERVER_STARTED/STOPPING` | `@Mod`-Konstruktor, `ServerStartedEvent`/`ServerStoppingEvent` |
| Befehle über CommandMap | `CommandRegistrationCallback` + Brigadier | `RegisterCommandsEvent` + Brigadier |
| Tab-Vervollständigung | Brigadier `SuggestionProvider` / `EntityArgument.player()` | dito |
| `hasPermission` | `source.hasPermission(level)` / Fabric Permissions API | `PermissionAPI` / `hasPermission(level)` |
| `BukkitRunnable.runTaskTimer` | `ServerTickEvents.END_SERVER_TICK` + Tick-Zähler | `ServerTickEvent.Post` + Tick-Zähler |
| `runTaskLater` | eigene Warteschlange `(ausführenBeiTick, Runnable)` | dito |
| Chest-GUI + `InventoryClickEvent` | `SimpleMenuProvider` + `ChestMenu.sixRows` mit eigener `clicked()`-Überschreibung, die alles abbricht | dito |
| `PlayerInteractEvent` (Rechtsklick) | eigenes `Item` mit `use()`-Überschreibung **oder** `UseItemCallback` | eigenes `Item` **oder** `PlayerInteractEvent.RightClickItem` |
| `EntityDamageByEntityEvent` | `ServerLivingEntityEvents.ALLOW_DAMAGE` | `LivingIncomingDamageEvent` |
| `spawnEntity(VILLAGER)` | `EntityType.VILLAGER.create(level, …)`, `setVillagerData(…VillagerType.SNOW, VillagerProfession.NONE…)`, `setPersistenceRequired()`, `setInvulnerable(true)` | dito |
| `spawnParticle` (alle) | `ServerLevel.sendParticles(...)` | dito |
| `player.spawnParticle` (nur einer) | `ServerLevel.sendParticles(player, ...)` oder clientseitig | dito |
| `dispatchCommand(console, cmd)` | `server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd)` | dito |
| `dropItemNaturally` | `ItemEntity` spawnen / `player.drop(stack, false)` | dito |
| YAML-Configs im Datenordner | `config/christmas/*.json` (Gson) oder Cloth Config / owo-config | `ModConfigSpec` (TOML) oder eigene JSON |
| `calendar-data.yml` | `SavedData` in `world/data/christmas_calendar.dat` **oder** Player-Attachment (Fabric Data Attachment API) | `SavedData` oder `AttachmentType` mit `serialize`/`copyOnDeath` |
| Material-Namen (`DIAMOND`) | `BuiltInRegistries.ITEM.get(Identifier.parse("minecraft:diamond"))` | dito |
| Verzauberungen per Name | `registryAccess().lookupOrThrow(Registries.ENCHANTMENT)` + `EnchantmentHelper` / `DataComponents.STORED_ENCHANTMENTS` für Bücher | dito |
| CustomModelData | `DataComponents.CUSTOM_MODEL_DATA` | dito |
| Leuchten ohne Verzauberung | `DataComponents.ENCHANTMENT_GLINT_OVERRIDE = true` | dito |
| Item-Erkennung über Namen | eigene Data Component `christmas:cracker_type` (String) | dito |
| Vault `playerAdd` | LuckPerms-API (optional) oder Scoreboard-Tag | dito |

### Empfohlene Mod-Struktur

```
christmas-mod/
├── ChristmasMod.java              (Einstieg, registriert alles)
├── config/ChristmasConfig.java    (config + gifts + messages laden, Standarddateien erzeugen)
├── text/Messages.java             (Prefix, Platzhalter, &-Farbcodes → Component)
├── calendar/
│   ├── CalendarData.java          (SavedData: Map<UUID, Set<Integer>>)
│   ├── CalendarMenu.java          (ChestMenu, Klicks abbrechen, Tag aus Slot statt aus Namen ableiten)
│   ├── CalendarLogic.java         (canClaim, claim, Gruppenwahl, Geschenkausgabe)
│   └── Gift.java / GiftType.java
├── santa/SantaEvent.java          (Spawn, Tick-Logik, Drop, Schneespur, Unverwundbarkeit)
├── cracker/
│   ├── CrackerItem.java           (Item mit use(); Typ via Data Component)
│   └── CrackerTypes.java
├── snow/SnowEffect.java           (Server: Status pro Spieler; Client: Partikel)
├── command/ (AdventCalendarCommand, ChristmasCommand, CrackerCommand)  – Brigadier
└── resources/
    ├── fabric.mod.json | META-INF/neoforge.mods.toml
    ├── assets/christmas/lang/en_us.json, de_de.json
    ├── assets/christmas/models/item/cracker.json (+ Texturen)
    └── data/christmas/... (optional: Geschenke als Datapack-JSON)
```

### Tipps für die Umsetzung
- **Kalender-Slot-Lookup:** Tag über die Slot-Tabelle (Abschnitt 4.1) aus dem geklickten Slot berechnen – robuster als Namensparsing.
- **Datum:** `LocalDate.now()` (Serverzeit) beibehalten; optional Zeitzone konfigurierbar machen.
- **Santa:** UUID speichern; beim Serverstopp entfernen. Da `setPersistenceRequired` gesetzt wird, nach einem Absturz beim Start verwaiste Santas (z. B. per Tag `christmas_santa`) aufräumen.
- **Schneespur:** gesetzte Schneeblöcke in einer Liste mit Ablauf-Tick speichern und beim Stopp sofort entfernen.
- **Rein serverseitige Mod** (keine Client-Installation nötig) ist möglich, wenn: Vanilla-Items (Papier) + Data Components statt eigenem Item-Typ, Vanilla-ChestMenu, serverseitige Partikel, Texte aus Config statt Sprachdatei.

### Umsetzung mit MCreator (falls „mmc“ = MCreator)

| Plugin-Teil | MCreator-Element |
|---|---|
| `/adventcalendar`, `/christmas`, `/cracker` | **Command**-Elemente (Argumente: `literal`, `entity`/`player`, `integer`, `string`); Rechte über „Required permission level“ (op = 2, alle = 0) |
| Kalender-GUI | **GUI**-Element (6 Reihen, 25 Buttons/Slots an den Positionen aus 4.1) + **Procedure** „on button pressed“ mit `canClaimDay`/`claimDay`-Logik |
| Abgeholte Tage | **Global Variable** (Scope `PLAYER_PERSISTENT`), z. B. String `claimed_days` = „1,2,5“, oder 25 Boolean-Variablen `day_1` … `day_25` |
| Testmodus, Schnee an/aus, Schnee-Stufe | Player-Variablen (`PLAYER_LIFETIME` bzw. `PLAYER_PERSISTENT`) |
| Santa | **Living Entity**-Element (Villager-Modell/eigene Textur, „Immune to all damage“, „Despawn when idle“ aus) + Global Variable `santa_active`, `santa_ticks_left` (Map-Scope) |
| Geschenk-Drop & Schneespur | **Procedure** im Entity-Tick („On entity tick update“) mit Zählern: alle 100 Ticks 15 %-Wurf, alle 10 Ticks Partikel + temporärer Schnee |
| Cracker | **Item**-Element pro Typ (`basic_cracker`, `premium_cracker`) mit Procedure „When right clicked in air/on block“: 1 Stück entfernen, Belohnungen geben |
| Schnee-Effekt | Procedure „On player tick update“: wenn Variable an und Dimension = Overworld → Partikel `snowflake` gemäß Stufe spawnen |
| Datum (Dezember-Prüfung) | Procedure-Block „Get current date“ bzw. Java-Code-Snippet `LocalDate.now()` |
| Konsolen-Befehle als Belohnung | Block „Execute command“ mit `{player}` → Spielername |
| Texte | **Language**-Einträge (`en_us`, `de_de`) mit den Texten aus Abschnitt 6 |

Konfigurierbarkeit (YAML) ist in MCreator nur über eigene Java-Elemente möglich – sonst Standardwerte aus Abschnitt 5 fest in die Procedures übernehmen.

---

## 8. Bekannte Fehler / Eigenheiten des Originals (in der Mod beheben)

1. **`gifts.yml`-Format:** `items` ist eine YAML-Liste, `GiftManager` liest sie als Map (`getConfigurationSection(group + ".items")` → `null`). Dadurch werden **keine Geschenke geladen**, und `claimDay` bricht mit „No gifts configured“ ab. → In der Mod als Liste parsen.
2. **`spawn-locations`:** ist eine Liste, `SantaManager.getSpawnLocations` liest sie als Map (`getConfigurationSection(...)` liefert `null` → `.getKeys` wirft eine NullPointerException) → `random`-Start schlägt fehl. → Als Liste parsen.
3. **`stored-enchantments`** wird gelesen, aber nie auf Verzauberte Bücher angewandt → Bücher sind leer. → `STORED_ENCHANTMENTS` setzen.
4. **`Enchantment.getByName`** ist veraltet und kennt moderne Namen teils nicht. → Registry-Lookup per ID.
5. **Santa-Schutz:** `SantaListener` (nur `EntityDamageByEntityEvent`, also nur Schaden durch Wesen – Feuer, Fall, Lava usw. treffen Santa immer) vergleicht das geschädigte Wesen mit dem *ersten beliebigen* Wesen im 1-Block-Umkreis um Santa (kann auch ein Spieler oder ein anderes Mob sein). Ist das zufällig nicht Santa, wird Santa normal verletzt; ist es ein anderes Wesen, wird stattdessen dieses geschützt. `onEntityDeath` ist leer. → UUID-Vergleich bzw. `setInvulnerable(true)`.
6. **Testmodus:** erlaubt erneutes Abholen bereits abgeholter Tage (gewollt für Tests, aber unbegrenzt).
7. **`resetAll`:** entfernt den Spieler nur aus dem Speicher; der alte Eintrag bleibt in `calendar-data.yml` und taucht nach Reload/Neustart wieder auf. → Schlüssel auch in der Datei auf `null` setzen.
8. **Cracker-Erkennung über Anzeigenamen:** Jedes umbenannte Item (Amboss) mit gleichem Farbnamen wird zum Cracker. → Data Component/NBT.
9. **Cracker in der Nebenhand:** Bei Stapelgröße 1 wird die **Haupthand** geleert, nicht die tatsächlich benutzte Hand.
10. **`/cracker give/get` bei vollem Inventar:** Prüft nur „kein leerer Slot“; gibt bei Teil-Stapeln trotzdem nichts.
11. **Permission-Belohnungen:** `duration` wird ignoriert; in Crackern komplett übersprungen.
12. **Ungenutzte Config-Werte / Nachrichten:** siehe Markierungen „ungenutzt“ in Abschnitt 5/6 – in der Mod entweder implementieren (z. B. `advent-calendar.enabled`, `start-date`/`end-date`, `santa-claus.speed`, `/christmas snow <1-5>` mit `snow.intensity-changed`, `santa.gift-dropped`, `santa.no-spawn-locations`) oder weglassen.
13. **Koordinaten-Fehler** bei `santa start` meldet `santa.invalid-duration` statt einer passenden Meldung.
14. **Schnee-Task:** erstellt jeden Tick pro Spieler neue `Random`-Objekte und liest die Config – unkritisch, aber in der Mod Werte cachen.
15. **Kein Material-Fallback:** `day-item.*` (Kalender) und Cracker-`material` nutzen `Material.valueOf` ohne Fehlerbehandlung → ein Tippfehler in der Config lässt das Öffnen der GUI bzw. das Laden der Cracker abstürzen. → Mit Fallback/Log-Warnung parsen.
16. **Stilles Scheitern beim Abholen:** Liefert `claimDay` wegen fehlender Geschenke nichts, schließt der Listener die GUI trotzdem und öffnet sie neu – der Spieler bekommt keine Fehlermeldung. → Meldung senden.
17. **GUI-Erkennung über den Titel:** Jedes Inventar mit demselben Titel (z. B. eine umbenannte Truhe „§c§lAdvent Calendar“) wird blockiert/als Kalender behandelt. → Eigenes Menü-Objekt.
18. **Nicht-lebende `entity-type`:** Ist der Typ kein `LivingEntity` (z. B. `ARMOR_STAND` geht, `MINECART` nicht), wird das Wesen gespawnt, aber nie gespeichert → es wird beim Stopp nicht entfernt, und die Drop/Schnee-Tasks beenden sich sofort.
19. **Platzhalter werden mit eingefärbt:** Werte werden vor der `&`-Umwandlung eingesetzt, d. h. `&`-Codes in Spielernamen/Eingaben werden als Farbe interpretiert.
20. **`saveResource` beim Start** wird für `messages.yml`/`gifts.yml` mit `replace=false` aufgerufen und erzeugt bei vorhandenen Dateien nur eine Log-Warnung.

---

## 9. Checkliste für die Mod (Akzeptanzkriterien)

- [ ] `/adventcalendar` öffnet ein 6-reihiges, nicht veränderbares Menü mit 25 Türchen an den angegebenen Slots, farbig nach Status.
- [ ] Türchen sind nur im Dezember und nur bis zum heutigen Tag (bzw. nur heute) abholbar; jeder Tag einmal pro Spieler; Fortschritt überlebt Neustarts.
- [ ] Geschenke gruppenabhängig (höchste Priorität gewinnt, Fallback `default`); Items/Befehle/Rechte werden korrekt vergeben; Überschuss wird gedroppt.
- [ ] `/adventcalendar reset <spieler> <tag|all>` und `/adventcalendar test` funktionieren.
- [ ] `/christmas santa start [zeit] [welt x y z | random]`, `stop`, `teleport` funktionieren; Santa ist unverwundbar, droppt Geschenke (15 % alle 5 s), hinterlässt temporären Schnee und Partikel; endet automatisch.
- [ ] `/cracker list|give|get` funktionieren; Rechtsklick auf einen Cracker verbraucht 1 Stück und gibt alle Belohnungen.
- [ ] `/christmas snow` schaltet einen nur für den Spieler sichtbaren Schneefall um; deaktiviert in Nether/End.
- [ ] `/christmas reload` lädt alle Konfigurationen neu.
- [ ] Alle Texte aus Abschnitt 6 mit Prefix und Platzhaltern.
- [ ] Rechte/Permission-Level gemäß Abschnitt 3.
- [ ] Alle Fehler aus Abschnitt 8 behoben (v. a. Geschenke-Format, Spawn-Liste, Santa-Schutz, Bücher).

---

# TEIL B – YULETIDE: Die Ausbaustufe

> Leitidee: Aus „ein paar Befehle und ein Villager“ wird ein **Winter-Erlebnis**, das vom 1. Dezember bis Neujahr jeden Tag einen Grund zum Einloggen gibt – für Singleplayer **und** Server.

## 10. Verbesserte Kernfeatures

### 10.1 Adventskalender 2.0 – „Das Türchen-Erlebnis“
| Verbesserung | Beschreibung |
|---|---|
| **Animierte Türchen** | Beim Öffnen schwingt die Tür auf (Frame-Animation im Menü, 0,5 s), Konfetti-Partikel + Glockenklang; Geschenk „springt“ aus dem Türchen. |
| **Streak-System** | Aufeinanderfolgende Tage ergeben eine Serie: 7 Tage = Bonus-Item, 14 = Cosmetic, 24 = Titel „Adventskönig“. Verpasst man einen Tag, kann man ihn mit einem **Rettungs-Zuckerstange-Item** (1×/Spieler, craftbar am Weihnachtstisch) nachholen. |
| **Vorschau** | Gesperrte Türchen zeigen ein **verhülltes Geschenk-Symbol** („???“), nach Klick auf Rechtsklick eine Teaser-Silhouette. Am Vortag: Hinweis „Morgen: ★★★ selten“. |
| **Seltenheitsstufen** | Geschenke haben Seltenheit (Gewöhnlich / Selten / Episch / Legendär) mit farbigem Glühen und eigener Sound-Fanfare. |
| **Wahl-Geschenke** | Manche Tage bieten **„Wähle 1 von 3“**-Truhen (z. B. Tag 12: Werkzeug, Rüstung oder Ressourcen). |
| **Gemeinschaftstage** | An Tag 6, 13, 20 gibt es **Server-Ziele** („Alle zusammen sammeln 10 000 Schneebälle“) – bei Erfolg bekommt **jeder** Spieler Bonus-Geschenke. |
| **Echte Zeitzonen** | Konfigurierbar (`timezone`, Standard Serverzeit) + **Mitternachts-Glockenläuten** & Chat-Hinweis beim Login: „Dein Türchen wartet!“. |
| **Admin-Editor** | Ingame-Menü `/yuletide editor` zum Bearbeiten der Geschenke per Drag&Drop (statt YAML-Fehler wie im Original). |
| **Persönlicher Kalender** | Pro Spieler **eigene Kalender-Skin** (Holz, Eis, Lebkuchen, Steampunk) – freischaltbar über Erfolge. |
| **Jahres-Reset** | Fortschritt gilt pro Jahr (`calendar-data` getrennt nach Jahr), Archiv-Ansicht „Meine Weihnachten 2024/2025/…“. |
| **Benachrichtigungen** | Actionbar/Toast beim Login, wenn ein Türchen offen ist; optional Bossbar „Noch N Tage bis Weihnachten“. |

### 10.2 Santa 2.0 – „Der Weihnachtsmann reist wirklich“
- **Echter Schlitten-Flug:** Santa fliegt auf einem **Rentier-Schlitten** (eigene Entity, Glöckchen-Sound, Funkenspur) eine **Route** über mehrere Orte (Wegpunkte aus Config oder `/yuletide santa route`), statt an einem Punkt zu stehen.
- **Landungs-Stopps:** An jedem Wegpunkt landet er 60 s: Spieler können mit ihm **handeln** (Santa-Shop gegen Schneeflocken-Münzen), Fotos machen (`/yuletide photo`), ihm einen **Wunschzettel** übergeben.
- **Wunschzettel-System:** Spieler schreiben ein Buch „Wunschzettel“ (Item) und geben es Santa; er „erfüllt“ nach Zufall/Regeln einen Wunsch (Item-Whitelist aus Config) – mit Chat-Dialog („Ho ho ho, brav gewesen?“).
- **Brav/Unartig-Liste:** Heimlicher Punktestand pro Spieler (Hilfsaktionen, Kämpfe, Griefing-freie Zeit). Brav → Premium-Geschenk, Unartig → Kohle (Spaß-Item mit Easter-Egg).
- **Geschenk-Säcke statt Zufalls-Drop:** Santa wirft **animierte Geschenkpakete** (Ballon-Fallschirm) ab, die man öffnen muss; Inhalt aus Loot-Table `yuletide:santa_gift` (konfigurierbar, Datapack-fähig), Seltenheit sichtbar durch Schleifenfarbe.
- **Boss-Variante „Krampus-Alarm“:** Optionaler Gegenspieler (Boss-Mob mit Bossbar, Ketten-Angriff, Kohle-Drops, Fackel-Partikel) – spawnt nach Santa-Event mit 10 % Chance. Besiegt = Legendär-Beute + Erfolg.
- **Unverwundbarkeit richtig:** `setInvulnerable`, Santa ist **nicht schubsbar**, folgt Route per Pathfinding/Spline, nutzt `santa_active`-Tag und wird nach Absturz bereinigt.
- **Zeitplan-Automatik:** Cron-artige Events (`daily 18:00`, `24.12. 20:00 – Heiligabend-Special`), Ankündigung 5 min vorher mit Countdown-Bossbar.
- **Weltweite Fanfare:** Titel „🎅 Santa ist unterwegs!“, Glockenklang, Nordlicht-Himmel (siehe 11.1) während des Events.
- **Karten-Marker:** Santas Position als Marker auf Karten/Minimap (JourneyMap/Xaero-optionale Hooks) + Kompass-Item „Santa-Kompass“.

### 10.3 Knallbonbons 2.0 – „Cracker-Kultur“
- **Eigenes Item** (kein Name-Matching): Data Component `yuletide:cracker { type, owner?, tier }`.
- **Aufreißen zu zweit:** Echte Cracker braucht **zwei Spieler** – Rechtsklick auf anderen Spieler mit Cracker in der Hand → beide ziehen (Minispiel mit Timing-Leiste); Gewinner bekommt Beute, Verlierer Papierkrone + Witz.
- **Papierkronen & Witze:** Jeder Cracker enthält einen **Papierkrone-Helm** (Cosmetic, 12 Farben, optional sichtbar für alle) und einen **zufälligen Cracker-Witz** (Liste in `lang`).
- **Loot-Tables statt feste Liste:** Beute per Gewichtung (`weight`), Mindest-/Maximalmenge, Bedingungen (Biom, Zeit, Spielerstufe) – kompatibel mit Datapack-Loot-Tables.
- **Tiers:** Basic / Premium / Mythic / **Mystery** (Zufalls-Tier). Pity-Zähler garantiert nach 10 Crackern einen Treffer.
- **Handwerk:** Cracker herstellbar (Papier + Schießpulver + Geschenkband + Zutat) – Zutat bestimmt Tier. Neues Rezeptbuch-Tab „Yuletide“.
- **Tausch-/Handelbar** und mit Namensschild **signierbar** (zeigt „Von <Spieler>“).
- **Knall-Effekt:** Partikel-Feuerwerk, Sound, kleine Rückstoß-Animation; bei Mythic Mini-Feuerwerks-Show.
- **Anti-Exploit:** Rechtsklick-Cooldown, Stapel-Öffnen (Shift = alle), Inventar-Voll-Check mit Teil-Stapeln (korrekt, behebt Original-Fehler 10).

### 10.4 Schnee 2.0 – „Echter Winter“
- **Clientseitiger Wetter-Effekt:** Schnee-Partikel nur im Client (kein Server-Traffic), mit Windrichtung, Böen und **Schneesturm** (Sicht reduziert, Nebel, Wind-Sound).
- **Intensität 1–5 per Befehl** `/yuletide snow [0-5]` plus Mausrad-Shortcut im Mod-Menü; Speicherung pro Spieler (clientseitige Config, überlebt Neustarts).
- **Schnee-Akkumulation (optional, serverseitig):** Während Schneefall bildet sich langsam Schnee auf Böden in Spawn-Umgebung (konfigurierbarer Radius, Blacklist für Redstone-Bauten & Wasser), schmilzt nach Event.
- **Gefrorene Gewässer:** Seen frieren im Radius um Spieler zu **Eisflächen** (Schlittschuhlaufen: Eis-Speed-Boost mit „Schlittschuh“-Item).
- **Weihnachtsbiome-Look:** Optionaler Resourcepack-/Shader-Look: wärmere Lichtfarbe, Lichterketten-Glühen, Himmelsfarbe.
- **Schneeball-Mechanik:** Schneemänner bauen (Kürbis + 2 Schneeblöcke) gibt **Frosty-Begleiter**, Schneeball-Schlacht-Modus (siehe 12.3).

## 11. Neue Content-Features (der „1000×“-Teil)

### 11.1 Weltatmosphäre
- **Winter-Atmosphäre-Schalter** `/yuletide atmosphere on|off`: Nordlichter am Himmel (Shader/Partikelbänder), sanfter Glocken-Ambient, dezente **Weihnachtsmusik** (eigene Tracks, Jukebox-fähig, im Spiel-Menü lautstärkeregelbar).
- **Dekorations-Blöcke:** Lichterkette (verbindet sich automatisch zu Nachbarn, blinkt in 4 Modi), **Weihnachtsbaum** (mehrstufig, schmückbar mit Kugeln/Stern/Lametta, Geschenke darunter als Container), Adventskranz (4 Kerzen, zündet je Sonntag), Zuckerstangen-Zaun, Lebkuchenhaus-Bausatz, Kranz-Dekoration, Strümpfe am Kamin (Container pro Spieler), Schneekugel (Item-Rahmen), Eiszapfen (Tropf-Sound).
- **Kamin-Block:** Gemütliches Feuer + Wärmewirkung („Gemütlich“-Buff: schnellere Regeneration im Umkreis) + Stiefel zum Aufhängen.
- **Kuschelzone / Glühweinstand:** Blöcke für Märkte (siehe 11.3).

### 11.2 Wichtel-Dorf & Weihnachtsmarkt (Weltgenerierung)
- **Weihnachtsmarkt-Struktur:** Spawnt (konfigurierbar) im Schneebiom oder am Weltspawn: Holzbuden, Lichter, Glühweinstand, Karussell, Eisbahn, großer Baum.
- **NPC-Wichtel:** Händler mit saisonalem Angebot (Tausch gegen **Schneeflocken-Münzen**), Quest-Geber, Handwerker (veredelt Items „mit Weihnachtsmotiv“).
- **Festliche Dorfbewohner:** Optional tragen Villager im Dezember Zipfelmützen (Cosmetic-Layer).
- **Händler-Rotation:** Täglich wechselndes Sortiment; seltene „Heiligabend-Specials“.

### 11.3 Währung & Fortschritt
- **Schneeflocken-Münzen** (Währung): verdient durch Türchen, Quests, Events, Cracker. Ausgabe im Shop, **Cap & Anti-Inflation**-Regeln.
- **Weihnachts-Level & Battle-Pass-light:** Zählt Aktionen („Geschenke verteilt“, „Cracker geöffnet“, „Schneemänner gebaut“). 50 Stufen mit Cosmetics, Titeln und Ressourcen. Kostenlos, kein Pay-to-win.
- **Erfolge (Advancements):** Eigener Tab „Yuletide“ mit ~40 Erfolgen (Alle 24 Türchen, Santa gesehen, Krampus besiegt, 10 Cracker mit Partner, Lebkuchenhaus gebaut, Schlittschuh-Marathon …).
- **Sammelalbum „Winterschatz“:** 30 sammelbare Dekorationen/Cosmetics, Seltenheit sichtbar, Duplikate werden zu Münzen.
- **Jahres-Rangliste:** Top-Spender-Geschenke / Top-Sammler pro Server (Anzeige am Weihnachtsmarkt).

### 11.4 Cosmetics (rein optisch)
- **Zipfelmütze, Papierkrone, Lichterketten-Rückenpack, Schlittschuhe, Rentier-Geweih, Schal** – als Helm/Rücken/Fuß-Slots, mit Mod-Menü-Vorschau (ohne Rüstungswert, kein Balancing-Eingriff).
- **Partikel-Spuren:** Schneeflocken-, Sternen-, Zuckerstangen-Spur beim Gehen (freischaltbar).
- **Spieler-Emotes:** `/yuletide emote jingle|cheers|bow|snowangel` – Animationen + Sound.
- **Namensschild-Tags:** „🎅 Bester Beschenker“, „⛄ Schneemann-Meister“ als Prefix über dem Kopf.

### 11.5 Mini-Games & Aktivitäten
- **Schneeballschlacht-Arena** (`/yuletide arena`): Teams Rot/Grün, Festungen aus Schnee bauen, Fahne klauen; Gewinner-Münzen.
- **Lebkuchenhaus-Bauwettbewerb:** Bausatz (Teig/Zuckerguss/Bonbons) → Bauflächen, Jury-Voting per GUI-Abstimmung, Ranking & Preis.
- **Geschenke-Wichteln („Secret Santa“):** Anmeldung per `/yuletide wichteln join`, Server losst Paare, jeder gibt anonym ein Geschenk (Item in Versandbox). Auflösung an Heiligabend, **Anti-Missbrauch** durch Regeln (Wert-Limit).
- **Eisangel-Event:** Spezielle Angel an gefrorenen Seen, seltene „Eisfische“ als Zutaten für Rezepte.
- **Schlittenfahrt:** Reittier-Schlitten (Items/Entities), Rennstrecken mit Checkpoints, Highscore-Liste.
- **Rentier-Reiten:** Zähme Rentiere (Zucker-Köder), reite sie (Pferd-Mechanik), optionale Fähigkeit „Sprung der Sterne“.
- **Schatzsuche „Verlorene Geschenke“:** Täglich verstecken sich 3 Päckchen im Umkreis (Kompass-Hinweise), exklusive Beute.
- **Rätsel-Tür:** Adventskalender-Rätsel pro Tag (z. B. Redstone, Worträtsel, Parkour) als optionaler **Bonus-Modus** (Schwierigkeitsstufen).

### 11.6 Kulinarik & Crafting
- **Neue Nahrung:** Plätzchen (4 Sorten, kleine Buffs), Lebkuchenmann (Lauf-Buff), **Glühwein** (Wärme gegen Frost, Alk-freier Kinderpunsch), Bratapfel, Stollen, Eierpunsch, heiße Schokolade (Regeneration).
- **Backofen/Gebäck-Station:** Neuer Block mit Rezepten und Backzeit-Minispiel (Timing).
- **Wärme/Frost-System (optional):** In Schneebiomen unterkühlt man leicht (Frost-Overlay) → Kamin/Glühwein/Pelz-Rüstung wärmen. In Config abschaltbar.
- **Weihnachts-Rüstungssets:** Zipfelmützen-Rüstung (Kälteschutz), Rentier-Set (Sprungkraft), Eis-Set (Schlittschuh, langsamer Verbrauch).
- **Werkzeuge/Waffen (optional):** Zuckerstangen-Spitzhacke (Effizienz), Schneeschaufel (schnell), Weihnachts-Bogen (Funkenpfeile) – Balancing moderat, nur Cosmetic-Fokus bei Servern.

### 11.7 Quests & Storyline
- **Tägliche Wichtel-Quests** (3/Tag): „Sammle 20 Schneebälle“, „Liefere 5 Plätzchen an NPC“, „Finde das verlorene Geschenk“.
- **Haupt-Story „Der gestohlene Stern“:** 8 Kapitel (Dialog-GUI, Questmarker), Boss „Krampus“, Belohnung: Legendäre Stern-Spitze für den Baum + Titel.
- **Server-weite Ereignisse:** „Eiszeit“ (Schnee + Wärmeverlust, Münzen ×2), „Nordlicht-Nacht“ (seltene Drops), „Geschenke-Regen“.

## 12. Technik & Qualität

### 12.1 Konfiguration & Datenhaltung
- **JSON/TOML + Datapack-Support:** Geschenke, Cracker, Quests, Loot als Datapack-Ressourcen (`data/yuletide/...`); Hot-Reload via `/reload`.
- **Schema-Validierung:** Beim Laden werden Fehler mit **Datei/Zeile/Grund** gemeldet, nie stilles Scheitern (Verbesserung zu Original-Fehlern 1, 2, 15).
- **Speicherung:** `SavedData` + optional **SQLite/JSON-Backup** pro Spieler, Migration vom alten `calendar-data.yml` (`/yuletide import-legacy`).
- **Config-Screen:** Ingame-UI (Cloth Config/NeoForge Config) mit allen Werten, Live-Vorschau.

### 12.2 Kompatibilität & Server
- **Singleplayer & Dedicated:** Alles funktioniert ohne Server-Mod auf Clients **optional** (reine Server-Variante = Vanilla-Client sicher), Client-Mod bringt nur Optik/Shader/Emotes.
- **Permissions:** Fabric Permissions API / LuckPerms / NeoForge PermissionAPI mit **Fallback** auf OP-Level; Gruppen-Prioritäten per Tags oder Rang.
- **Mehrsprachig:** `en_us`, `de_de`, `fr_fr`, `es_es` + Community-Packs; Platzhalter weiterhin nutzbar, **MiniMessage/Component-JSON** statt veralteter `&`-Codes (Legacy trotzdem lesbar).
- **Kompatibilitäts-Hooks:** WorldGuard-Äquivalent (FTB Chunks/Open Parties and Claims) für Schneespuren, Vault-Äquivalent (Economy-API: Fabric Economy API/Impactor) für Münzen, Placeholder API für Scoreboards.
- **Performance:** Partikel & Zufall gecacht, Tick-Budget pro Feature (max. 1 ms/Tick), Schneeakkumulation in Chunk-Batches; Telemetrie `/yuletide debug perf`.

### 12.3 Admin- & Spielerwerkzeuge
- `/yuletide` als Hauptbefehl mit Unterbefehlen (Aliase `/xmas`, `/advent`, `/cracker` bleiben für Kompatibilität).
- **Eventplaner:** `/yuletide schedule` – Santa, Eiszeit, Wichteln, Arena zeitgesteuert.
- **Statistiken:** `/yuletide stats [spieler]` – geöffnete Türchen, Cracker, Münzen, Erfolge; Export als CSV.
- **Anti-Cheat/Schutz:** Cooldowns, Inventar-Voll-Prüfung, Item-Signaturen gegen Duplikate, Audit-Log (wer bekam welches Legendär).
- **Debug-Werkzeuge:** `/yuletide test calendar|santa|cracker`, `/yuletide fastforward <tag>` (Testmodus pro Spieler, **mit** Einmal-Schutz).

## 13. Neue Befehle (Übersicht)

| Befehl | Zweck |
|---|---|
| `/yuletide` | Hauptmenü (Kalender, Quests, Cosmetics, Erfolge) |
| `/yuletide calendar` | Kalender öffnen (Alias `/advent`) |
| `/yuletide snow [0–5]` | Schnee-Stufe (Alias `/christmas snow`) |
| `/yuletide santa start\|stop\|route\|tp` | Santa verwalten |
| `/yuletide cracker give\|get\|list\|craft` | Cracker (Alias `/cracker`) |
| `/yuletide shop` | Wichtel-Shop |
| `/yuletide quests` | Quest-Log |
| `/yuletide cosmetic <equip\|list>` | Cosmetics |
| `/yuletide emote <name>` | Emotes |
| `/yuletide arena join\|leave\|create` | Schneeballschlacht |
| `/yuletide wichteln join\|status` | Secret Santa |
| `/yuletide atmosphere on\|off` | Atmosphäre |
| `/yuletide schedule add\|list\|remove` | Eventplaner |
| `/yuletide stats [spieler]` | Statistiken |
| `/yuletide reload` | Konfiguration neu laden |
| `/yuletide editor` | Geschenke-Editor |
| `/yuletide import-legacy` | Alte Plugin-Daten importieren |

Rechte (Vorschlag): `yuletide.use` (alle), `yuletide.admin` (op), `yuletide.santa`, `yuletide.cracker.give`, `yuletide.schedule`, `yuletide.editor`.

## 14. UI/UX-Konzept
- **Einheitliches Design:** Winter-GUI mit Tannenzweig-Rahmen, Schneeflocken-Cursor, Tooltips mit Seltenheitsfarben.
- **Toast-Benachrichtigungen:** Erfolg, Türchen bereit, Event startet.
- **Bossbar-Banner:** „🎅 Santa landet in 3:00 – (x,z)“ mit Fortschritt.
- **HUD (optional, Client):** Tageszähler bis Weihnachten, Münzen, Schnee-Stufe.
- **Barrierefreiheit:** Reduzierte-Bewegung-Modus (weniger Partikel), Farbenblind-freundliche Seltenheitsfarben, Untertitel für alle Sounds.

## 15. Roadmap

| Phase | Inhalt |
|---|---|
| **MVP (v1.0)** | Parität mit Original: Kalender, Santa, Cracker, Schnee + alle Fehlerfixes aus Abschnitt 8 |
| **v1.1** | Kalender 2.0 (Animation, Streaks, Seltenheit), Cracker als Item + Loot-Tables, Schnee-Stufen/Sturm |
| **v1.2** | Währung, Shop, Wichtel-NPC, Erfolge, Cosmetics, Dekorations-Blöcke |
| **v1.3** | Santa-Route/Schlitten, Wunschzettel, Krampus-Boss, Eventplaner |
| **v2.0** | Weihnachtsmarkt-Weltgen, Quests/Story, Mini-Games (Arena, Wichteln, Eisangeln), Wärme-System |
| **Danach** | Community-Datapacks, Workshop-Integration, jährliche Content-Drops („Yuletide 2026/2027“) |

## 16. Akzeptanzkriterien Teil B (Auszug)
- [ ] Alle Teil-A-Kriterien erfüllt (Abschnitt 9) – ohne die bekannten Fehler.
- [ ] Datapack-fähige Geschenke/Cracker/Loot mit klaren Fehlermeldungen beim Laden.
- [ ] Santa fliegt Route, landet, handelt, wirft Geschenkpakete; Boss-Variante optional.
- [ ] Cracker nur als Item mit Komponente; Zwei-Spieler-Öffnen und Papierkrone funktionieren.
- [ ] Schnee-Stufen 0–5 clientseitig, Sturm-Modus, Reduzierte-Bewegung-Option.
- [ ] Münzen/Shop/Level/Erfolge funktionieren, ohne Pay-to-win.
- [ ] Mindestens 15 Dekorationsblöcke und 6 Cosmetics ingame craft-/freischaltbar.
- [ ] Mehrsprachig (en/de), Konfig-Screen vorhanden, Performance-Budget eingehalten.
- [ ] Rückwärtskompatible Befehle (`/adventcalendar`, `/christmas`, `/cracker`) bleiben erhalten.
