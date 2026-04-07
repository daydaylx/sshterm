# UI Reference — sshterm

## 1. Kurzfazit
- Die Oberfläche ist ein Dark Theme (Ink-Farben), das stark technisch, aber gleichzeitig modern und aufgeräumt wirkt.
- Konsistenz entsteht durch den konsequenten Einsatz des "AppScreenScaffold" mit einem spezifischen "AppBackdrop" (Hintergrund-Glow-Effekte).
- Wiederkehrende Muster: Hero-Panels mit Metriken oben, gefolgt von Listen oder Formularen in abgerundeten Cards/Panels.
- Visuelle Hierarchie: Wenige starke Akzente (z.B. Mint500 für Primary/Success, Cyan500 für Tailscale) auf extrem dunklen, leicht bläulichen Hintergründen (Ink980 bis Ink840).
- Monospace-Fonts werden gezielt für technische Daten (Labels, Metriken) eingesetzt, Sans-Serif für den normalen Lesetext, was den Produktcharakter stärkt.
- Die Terminal-Ansicht (`TerminalScreen`) ist ein stark abweichender Sonderfall, der sich durch eine Immersive-Mode-ähnliche Bedienung mit ausblendbaren Overlays vom Rest abhebt.
- Produktgebunden und nicht blind übertragbar: Das sehr technische, tiefe Dark Theme, das Terminal-spezifische Wisch-Menü und die dedizierte Sonderbehandlung von Tailscale-Verbindungen.

## 2. Visuelle Leitidee
- **Stimmung**: Konzentriert, technisch, "Hacker-Ästhetik", aber professionell und poliert.
- **Charakter**: Ein Werkzeug für Administratoren/Entwickler, kein Consumer-Spielzeug.
- **Dichte**: Luftig im allgemeinen Layout (große Paddings), aber dicht bei technischen Informationen (Terminal, Diagnostics).
- **Grad an Technikalität**: Sehr hoch. Es werden bewusst Monospace-Schriften, harte Statusfarben und technische Begriffe prominent platziert.
- **Verhältnis von Ruhe, Funktionalität und visueller Betonung**: Der Großteil der Fläche ist extrem dunkel und ruhig. Betonung erfolgt punktuell durch Neon-artige Akzente (Mint, Cyan, Amber, Rose) in Metrik-Chips und Status-Indikatoren.
- **Wirkungserzeugung**: Konkrete Umsetzung durch komplexe Hintergrund-Brushes (`AppBackdrop` mit `glowColor`), stark gerundete Container (`large = 30.dp`, `extraLarge = 36.dp`) gepaart mit harten 1dp-Bordern, was an moderne Kommandozeilen-Tools auf High-End-Monitoren erinnert.

## 3. Screen-Architektur

### Host List
- **Struktureller Aufbau**: AppScreenScaffold (Titel, Subtitel) -> Scrollable Column -> Hero Panel -> Gruppierte Liste von Hosts.
- **Header / Top-Area**: CenterAlignedTopAppBar, transparent, mit Subtitel. Einstellungs-Icon oben rechts. FAB ("Host hinzufügen") unten rechts.
- **Hero-/Intro-Bereich**: `HostListHero` mit SectionIntro, drei `MetricChip`s nebeneinander (Hosts, Tailnet, Keys) und zwei Buttons (Hinzufügen, Einstellungen).
- **Content-Hierarchie**: Sticky Headers (Kategorien wie "Kürzlich", "Tailscale", "Direkt") gefolgt von `HostCard`s.
- **Aktionsbereiche**: Swipe-to-Dismiss auf den HostCards (Rechts=Connect, Links=Delete). FAB für Neu.
- **Status-/Metrikdarstellung**: MetricChips im Hero. "TS"-StatusChip in der HostCard. Letzte Verbindungszeit als Subtext.
- **Wiederverwendete Muster vs. Sonderfälle**: Nutzt Standard-Hero und List-Scaffold. Swipe-Interaktion ist spezifisch für Listen.

### Host Edit
- **Struktureller Aufbau**: Scaffold -> Scrollable Column -> Hero Panel -> Mehrere `ExpandableSection`s.
- **Header / Top-Area**: CenterAlignedAppBar. Bei Editieren ein Delete-Icon oben rechts.
- **Hero-/Intro-Bereich**: Kurze Einleitung (SectionIntro) im HeroPanel.
- **Content-Hierarchie**: Gegliedert in "Verbindung", "Authentifizierung" und "Netzwerkziel".
- **Aktionsbereiche**: Sticky "Speichern"/"Abbrechen" Buttons am Ende der ScrollView. Upload-Button für Private Keys. SegmentedControls für Auswahl (Passwort/Key).
- **Besonderheiten**: Nutzung von `ExpandableSection` (ElevatedCard mit AnimatedVisibility) um die Formularlänge optisch zu reduzieren. Inline-Validation bei Fehlern in Textfeldern.

### Terminal
- **Struktureller Aufbau**: AppBackdrop -> Scaffold ohne TopBar/BottomBar (Custom Overlays).
- **Header / Top-Area**: `TerminalTopBar` als Overlay (animiert einblendbar). Zeigt Hostname, Verbindungsstatus und Diagnostics-Link.
- **Content-Hierarchie**: Die gesamte Viewport ist das Terminal (`TerminalViewport`).
- **Aktionsbereiche**: `TerminalSelectionToolbar` für Copy/Paste, Text-Eingabefeld, `SpecialKeyBar` (Strg, Alt, Tab etc.) unten in einem `AppPanel`.
- **Besonderheiten**: Immersive UI. Tap auf den Hintergrund blendet TopBar und BottomPanel ein/aus (`isUiVisible`). Stark abweichendes Layout vom Rest der App.
- **Sonderfall**: Hier gibt es keine echten "Heroes" oder Standard-Listen.

### Settings
- **Struktureller Aufbau**: Scaffold -> Scrollable Column -> Hero Panel -> Abschnitte mit `SettingsCard`s.
- **Hero-/Intro-Bereich**: HeroPanel mit drei MetricChips (Grace Period, Font Size, Biometrics).
- **Content-Hierarchie**: Unterteilt durch `SettingsSectionHeader` (z.B. "Sitzung", "Terminal", "Netzwerk").
- **Aktionsbereiche**: Switches, Slider und einfache Textfelder. Diagnostics-Link ganz unten.
- **Wiederverwendete Muster**: Standard-Hero, klassischer Einstellungs-Listen-Aufbau.

### Diagnostics
- **Struktureller Aufbau**: Scaffold -> Scrollable List. TopBar hat Copy/Clear-Icons.
- **Hero-/Intro-Bereich**: HeroPanel mit Host-Info, Anzahl der Events, Status der Verbindung (Idle, Connected, etc.).
- **Content-Hierarchie**: Suchfeld, gefolgt von `DiagnosticEventCard`s.
- **Aktionsbereiche**: Cards sind klickbar (expandieren) um Log-Details zu zeigen.
- **Status-/Metrikdarstellung**: Events haben farblich kodierte StatusChips (INFO, WARN, ERROR) und verfärben den Card-Border und Container-Hintergrund entsprechend dem Level.

## 4. Wiederverwendbare UI-Primitiven

### Screen Scaffold (`AppScreenScaffold`)
- **Zweck**: Standard-Gerüst für alle Non-Terminal-Screens.
- **Visuelle Rolle**: Setzt den Dark-Theme Glow-Background (`AppBackdrop`) und platziert die TopBar zentriert.
- **Typische Platzierung**: Root jedes Screens (außer pure Overlays).
- **Eigenschaften**: Verbindlicher Background-Glow, verbindlich transparente TopBar.

### Hero Panel (`HeroPanel` / `SectionIntro`)
- **Zweck**: Einstieg in einen Screen, Kontext geben, Key-Metriken zeigen.
- **Visuelle Rolle**: Gradient-Hintergrund (`heroBrush`), große Typografie, abgesetzt von der Liste darunter.
- **Typische Inhalte**: Eyebrow (Label), Title, Subtitle, Metric Chips, Haupt-Actions (Buttons).
- **Wiederverwendungsgrad**: Sehr hoch (HostList, Edit, Settings, Diagnostics).
- **Eigenschaften**: Verbindliche Border (1dp), transparentes Container-Color mit darunterliegendem Brush.

### Content Panel (`AppPanel`)
- **Zweck**: Container für zusammenhängende Aktionen oder Infos (z.B. Terminal Input Bar).
- **Visuelle Rolle**: Hebt Inhalt leicht vom Hintergrund ab (`panelColor` / `panelStrongColor`).
- **Typische Inhalte**: Formulare, Toolbars.
- **Eigenschaften**: Optionale `emphasized` Property (verstärkt Color und fügt 8dp Shadow hinzu).

### Metric Chips (`MetricChip`)
- **Zweck**: Darstellung von KPIs (Zahlen, Zustände).
- **Visuelle Rolle**: Farbiger (getönter) Kasten mit harter Border.
- **Typische Inhalte**: Label (uppercase Monospace) + Value.
- **Wiederverwendungsgrad**: Hoch (in jedem HeroPanel).
- **Eigenschaften**: Akzentfarbe ist wählbar (Primary, Warning, Info).

### Expandable Section (`ExpandableSection`)
- **Zweck**: Gruppierung von Formularfeldern.
- **Visuelle Rolle**: Elevated Card mit Chevron-Icon.
- **Typische Inhalte**: TextFields, Switches, Segmented Controls.
- **Wiederverwendungsgrad**: Mittel (hauptsächlich in `HostEditScreen`).

### Status Chip (`StatusChip`)
- **Zweck**: Kleinere Label für den Zustand eines Items (z.B. "TS" für Tailscale, "ERROR" in Logs).
- **Visuelle Rolle**: Kleiner, runder Chip, stark farblich kodiert.
- **Typische Platzierung**: Neben Titeln in Listen (`HostCard`, `DiagnosticEventCard`).

## 5. Farben und Surface-System
- **Hintergrundebenen**: Hauptsächlich extrem dunkel. Gradient von `Ink980` (0xFF04080C) bis `Ink900` (0xFF0E1B24).
- **Panels / Surfaces**: Surfaces nutzen meist `Ink900` bis `Ink840` mit hoher Opacity (0.85f - 0.98f).
- **Akzente**: Primär ist `Mint500` (Neon-Grün), Sekundär `Cyan500` (Neon-Blau, stark für Tailscale genutzt).
- **Statusfarben**:
  - Connected/Success: `Mint500`
  - Connecting/Warning: `Amber500`
  - Disconnected: `Sand400`
  - Error: `Rose500`
- **Border-System**: Exzessive Nutzung von 1dp starken Bordern (`AppTheme.panelBorder` / `Ink720` / `Ink780`) um Container in dem dunklen Theme optisch zu trennen.
- **Glow-System**: Der `AppBackdrop` platziert weiche, große Kreise (`Mint500` mit 0.18f Alpha) im Hintergrund, um einen "Neon-Schein" von hinten zu simulieren.
- **Semantik**: Farben sind stark semantisch (Error=Rot, Tailscale=Cyan, Primary=Grün). Fast keine reinen Dekor-Farben abgesehen von den Glow-Kreisen.

## 6. Formensprache, Spacing und Density
- **Shape-System**: Sehr stark abgerundete Ecken (`AppShapes`).
  - Small: 18.dp (Chips, Inputs)
  - Medium: 24.dp (Panels, Cards)
  - Large: 30.dp (HeroPanel)
  - ExtraLarge: 36.dp (Sonderfälle)
- **Abstände**: Großzügige horizontale Paddings (20.dp - 24.dp an den Rändern).
- **Dichte**: Layouts wirken eher luftig (`Arrangement.spacedBy(16.dp)`), Formulare sind nicht gequetscht.
- **Prominenz**: Metriken im Hero sind sehr prominent (farbig hinterlegt). Listen sind visuell zurückhaltender (OutlinedCards) bis man interagiert.

## 7. Typografie
- **Einsatz von Sans vs. Monospace**: Die Typografie vermischt bewusst Sans-Serif (für Lesbarkeit in Body, Titles) und Monospace (für technische Präzision).
- **Monospace Rolle**: `displayMedium`, `labelLarge`, und `labelSmall` nutzen Monospace. Es wird für Eyebrows, Metric-Labels und technische IDs genutzt. Es etabliert den "Terminal-Look".
- **Hierarchie**:
  - `headlineMedium`/`Large` (Sans) für Screen- und Hero-Titel.
  - `titleMedium` (Sans, bold) für Listen-Items (Host-Namen).
  - `labelSmall` (Monospace) für Subtexte bei Metriken.

## 8. Interaktionsmuster
- **Navigation**: Zurück-Buttons als `FilledTonalIconButton` in der TopBar. Bottom-Navigation existiert nicht.
- **Actions / FAB**: FABs (`ExtendedFloatingActionButton`) werden prominent für Primäraktionen genutzt (Neuer Host). Listen-Aktionen primär über Swipe-to-Dismiss (Rechts/Links).
- **Expand/Collapse**: Formulare und Logs nutzen stark das Expandable-Muster (Chevron up/down), um bei komplexen Inhalten die Übersicht zu behalten.
- **Sichtbare Statusrückmeldungen**: Toast/Snackbars bei Kopieren/Fehlern. Inline-Loading in Buttons/Panels (z.B. "Verbinde...").
- **Terminal als Sonderfall**: Interaktion ist hier Touch-to-Toggle-UI. Die UI (Top/Bottom Bar) verschwindet, um maximalen Platz für den Text zu lassen.

## 9. SSHterm-spezifische Sondermuster
- **TerminalViewport**: Ein hochspezifisches Render-Target für ANSI-Text, das absolut Projekt-spezifisch ist.
- **SpecialKeyBar**: Eine Reihe von Buttons für Ctrl, Alt, Tab, Esc – macht nur in Terminal-Emulatoren Sinn.
- **Immersive Mode**: Das Ausblenden der UI bei Tap auf den Hintergrund im TerminalScreen ist zu irritierend für normale Info-Apps (wie Arbeitszeit).
- **Tailscale-Fokus**: Das UI-System hat hart eincodierte Status-Farben und Metriken für Tailscale ("TS" Badges, Neon-Blau).
- **Was NICHT übertragen werden darf**: Die `SpecialKeyBar`, die Terminal-Text-Auswahl-Logik und das dunkle "Hacker-Theme" passen nicht zu einer normalen Productivity-App.

## 10. Übertragbarkeit auf andere Projekte (z.B. Arbeitszeit-App)
- **Direkt übernehmbar**:
  - `MetricChip`: Hervorragend geeignet für z.B. "Gearbeitete Stunden", "Überstunden".
  - `SectionIntro`: Gute Struktur für Seitenköpfe.
  - `ExpandableSection`: Perfekt für Settings oder seltener genutzte Formularbereiche.
- **Sinngemäß adaptierbar**:
  - `HeroPanel`: Das Konzept (Header + Metriken) ist super, das `heroBrush` (dunkler Gradient) muss an das Light/Corporate Theme der Arbeitszeit-App angepasst werden.
  - `SwipeToDismiss` in Listen (z.B. Zeiteintrag löschen).
  - Das Shape-System (24dp/30dp Rundungen) kann übernommen werden, wenn ein weicher "Bubble"-Look erwünscht ist.
- **Nur mit Anpassung übernehmbar**:
  - Typografie: Die Monospace-Eyebrows sind vielleicht zu technisch für Arbeitszeiten. Besser durch eine normale, fette Sans ersetzen.
  - Das Color-Theme: Das Dark-Ink-Theme ist nicht für eine Mainstream-App zu empfehlen. Ein Light Theme mit viel Weiß/Hellgrau und einer klaren Primary-Color ist nötig.
- **Nicht übernehmen**:
  - `AppBackdrop` mit Glowing-Circles (wirkt zu spielerisch/Sci-Fi für Business-Apps).
  - Terminal-spezifische Leisten und Immersive UI.

## 11. Konkrete Designregeln
1. Jeder Hauptscreen MUSS ein `AppScreenScaffold` (oder äquivalentes Standard-Scaffold) verwenden.
2. Der Hintergrund-Aufbau darf nicht pro Screen neu implementiert werden.
3. Kein Screen darf harte, absolute Pixel-Höhen für Container nutzen; alles MUSS scrollbar oder flexibel sein (`weight(1f)`).
4. Jeder Content-Bereich (Listen, Formulare) MUSS ein horizontales Padding von `20.dp` bis `24.dp` aufweisen.
5. Hero-Bereiche MÜSSEN immer oben stehen und über ein `SectionIntro` (Eyebrow, Title) verfügen.
6. Key-Metriken MÜSSEN im Hero-Bereich via `MetricChip` dargestellt werden, niemals lose im Text.
7. Metrik-Labels (Eyebrows) MÜSSEN die Typografie `labelSmall` bzw. `labelLarge` (Monospace, Uppercase) verwenden.
8. Lesetexte und Beschreibungen (`bodyMedium`, `bodySmall`) dürfen NIE Monospace sein.
9. Container und Panels MÜSSEN abgerundete Ecken gemäß dem zentralen `AppShapes`-System nutzen (keine hart eincodierten `.clip(RoundedCornerShape(x))`).
10. Abgegrenzte UI-Container (Panels, Cards) MÜSSEN einen 1dp breiten Border (`panelBorder` bzw. `outlineVariant`) erhalten.
11. Status-Informationen in Listen MÜSSEN visuell über `StatusChip`s kodiert werden.
12. Gefahren-Aktionen (Löschen) MÜSSEN primär über Swipe-to-Dismiss in Listen (Richtung EndToStart) oder als rotes Icon oben rechts im Editor realisiert werden.
13. Dialoge MÜSSEN vor destruktiven Aktionen (Löschen, Disconnect) eingeblendet werden.
14. Lange Formulare (> 5 Felder) MÜSSEN in logische `ExpandableSection`s unterteilt werden.
15. Listen ohne Einträge MÜSSEN ein `EmptyStateView` zentriert anzeigen.
16. "Loading"-Zustände MÜSSEN visuell dargestellt werden (z.B. via `LoadingView` oder `InlineLoadingView`), nicht durch das Blockieren der UI ohne Feedback.
17. Farben dürfen NIE als Hex-Werte in den Screens stehen; sie MÜSSEN aus dem `MaterialTheme.colorScheme` oder `AppTheme` kommen.
18. Akzentfarben (Mint, Amber, Rose, Cyan) dürfen NUR für Status, Floating Action Buttons oder extrem wichtige Metriken genutzt werden, niemals für große Hintergrundflächen.
19. Die Navigation-TopBar MUSS zentrierte Titel verwenden.
20. Es darf nur eine einzige primäre Aktion (`ExtendedFloatingActionButton` oder Haupt-Button im Formular) pro Ansicht geben.

## 12. Relevante Dateien / Pfade
- **Theme**: `app/src/main/java/com/example/privatessh/ui/theme/Theme.kt`
  - *Begründung*: Definiert Dark/Light ColorSchemes, die Shapes und das System-Bar-Handling. Zentraler Startpunkt.
- **Farben**: `app/src/main/java/com/example/privatessh/ui/theme/Color.kt` & `AppTheme.kt`
  - *Begründung*: Zeigt die spezifischen Ink-Hexwerte und die semantischen Helper (z.B. `AppTheme.panelBorder`, `heroBrush`), die die App einzigartig machen.
- **Typografie**: `app/src/main/java/com/example/privatessh/ui/theme/Type.kt`
  - *Begründung*: Wichtig für das Verständnis der bewussten Trennung von Sans und Monospace-Schriften.
- **Gemeinsame Komponenten**: `app/src/main/java/com/example/privatessh/ui/components/AppChrome.kt`
  - *Begründung*: Enthält `AppScreenScaffold`, `HeroPanel`, `MetricChip`, `AppBackdrop` – die fundamentalen Bausteine der App.
- **Listen-Muster**: `app/src/main/java/com/example/privatessh/ui/hostlist/HostCard.kt`
  - *Begründung*: Zeigt fortgeschrittene Material3-Konzepte wie das moderne `SwipeToDismissBox` und das OutlinedCard-Design.
- **Formular-Muster**: `app/src/main/java/com/example/privatessh/ui/hostedit/HostEditScreen.kt`
  - *Begründung*: Referenz für `ExpandableSection`, Input-Validierung und SegmentedControls in diesem Projekt.

**Lese-Reihenfolge für einen Folge-Agent:**
1. `Theme.kt` & `Color.kt` (Um die visuelle Basis zu verstehen)
2. `AppChrome.kt` (Um die Shell/Scaffolds zu verstehen)
3. `HostListScreen.kt` & `HostCard.kt` (Standard-Implementierung einer Liste)
4. `HostEditScreen.kt` (Standard-Implementierung eines Formulars)
