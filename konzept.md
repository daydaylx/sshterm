konzept_ssh_client_android_revised.md
1. Ziel

Ziel ist eine private Android-APK für mein eigenes Smartphone, die als stabiler SSH-Client mit Terminal-Fokus funktioniert. Die App soll sich in der Nutzung eher an ConnectBot orientieren als an aufgeblasenen Admin-Suiten: schnell verbinden, vernünftig tippen, zuverlässig im Hintergrund laufen, und bei Tailscale-Hosts sauber funktionieren. ConnectBot ist ein etablierter Android-SSH-Client; Termux ist ein etablierter Android-Terminal-Emulator. Allein diese beiden Referenzen zeigen schon, dass „SSH-Client“ und „Terminal-Emulation“ keine kleine UI-Spielerei sind, sondern zwei separate technische Kerne.

2. Produktdefinition

Die App ist kein allgemeines Netzwerktool, kein Cloud-Produkt, kein Team-Tool und kein Web/Desktop-Projekt. Es geht um eine native Android-App, optimiert für ein einzelnes Gerät, einen einzelnen Nutzer und einen klaren Hauptzweck: per SSH auf eigene Systeme zugreifen, vor allem über Tailscale-IP oder MagicDNS-Hostnamen. MagicDNS registriert Gerätenamen im Tailnet automatisch als DNS-Namen, sodass statt roher Tailscale-IP meist einfach der Maschinenname verwendet werden kann.

3. Harte Produktgrenzen
3.1 Was die App in Version 1 können muss
Hosts speichern
Verbindung per Passwort oder SSH-Key
Host-Key/Fingerprint prüfen
Terminal-Session öffnen
Copy / Paste
Sondertastenleiste für Ctrl, Alt, Esc, Tab, Pfeile usw.
Tailscale-Ziele sauber unterstützen
Session im Hintergrund bestmöglich stabil halten
Reconnect-Logik
Grace-Period nach versehentlichem Schließen
optional automatisches tmux-Attach auf dem Server
3.2 Was ausdrücklich nicht Teil von Version 1 ist
Cloud-Sync
Multi-Device-Sync
Team-/Vault-Funktionen
SFTP-Dateimanager
mehrere Tabs/Sessions parallel
Tailscale-Login in der App
direkte Tailscale-API-Verwaltung
Tailscale SSH als eigener Modus
Desktop-/Web-Wiederverwendung
aufwendige Visuals, die nur hübsch sind und Bedienung verschlechtern
4. Technische Leitentscheidung

Die App wird nativ für Android umgesetzt, nicht mit WebView und nicht mit xterm.js. Für einen reinen Handy-Only-Privatclient bringt xterm.js mehr Ballast als Nutzen. Die sinnvolle Basis ist Kotlin + Jetpack Compose für die UI und eine Java/Kotlin-taugliche SSH-Bibliothek wie SSHJ für die Verbindungsebene. Compose ist Googles aktueller nativer UI-Stack; SSHJ hat seit 0.39.0 harte Bouncy-Castle-Abhängigkeiten entfernt, verbessert Curve25519-Handling und enthält seit 0.38.0 die Terrapin-Mitigation. Gleichzeitig existieren reale Android-Probleme mit X25519/Provider-Konflikten in SSHJ-Issues. Deshalb wird die Kryptoseite nicht blind vorausgesetzt, sondern über einen frühen Kompatibilitätstest auf echtem Gerät abgesichert.

5. Architekturprinzip

Die wichtigste Grundregel lautet:

Die SSH-Verbindung darf niemals an der Activity hängen.
Die UI ist nur Anzeige und Eingabe. Die laufende Session muss in einem Foreground Service leben, sonst fliegt die Verbindung bei Hintergrundnutzung oder UI-Wechseln irgendwann auseinander. Android verlangt für Foreground Services seit Android 14 einen gültigen Typ im Manifest; fehlende Typdeklaration führt zu MissingForegroundServiceTypeException. Seit Android 12 gelten zusätzlich Startbeschränkungen für Foreground Services aus dem Hintergrund.

6. Empfohlener Stack
6.1 UI
Kotlin
Jetpack Compose
Material 3 nur dezent, kein UI-Zirkus

Compose ist zustandsgetrieben. Für dieses Projekt ist das wichtig, weil Verbindungsstatus, Terminalzustand, Modifier-Tasten, Scrollzustand und Hintergrundstatus sauber modelliert werden müssen. Android empfiehlt State Hoisting und unidirektionalen Datenfluss, um genau diese Zustände nicht in der UI zu zerstreuen. Stable Keys in LazyColumn sind zusätzlich Pflicht, damit Listen und Scrollzustände nicht „springen“.

6.2 Daten / Persistenz
Room für Hostprofile, bekannte Fingerprints, Einstellungen, Session-Metadaten
Android Keystore für Schutz kryptografischer Schlüssel
optional BiometricPrompt für geschützten Zugriff auf gespeicherte SSH-Keys

Android empfiehlt, Provider nur beim Android Keystore explizit anzugeben. Außerhalb davon kann das explizite Festlegen eines Providers zu Kompatibilitätsproblemen führen; die Android-Doku nennt Bouncy-Castle-Algorithmen dabei ausdrücklich als problematisch, wenn BC direkt angefordert wird.

6.3 SSH-Layer
SSHJ als Client-Library
eigene Wrapper-Schicht statt direkte Library-Aufrufe aus ViewModels oder UI
frühe Android-Kompatibilitätsmatrix für Schlüssel/KEX/Hostkeys

SSHJ ist grundsätzlich eine gute Basis, aber auf Android dürfen Provider-/Algorithmus-Probleme nicht erst „irgendwann später“ auffallen. ConnectBot liefert sogar zwei Varianten aus: eine mit Google-Play-Services-gestütztem Provider-Upgrade und eine OSS-Variante, die den Provider direkt im APK mitbringt. Allein das zeigt, dass Kryptographie auf Android hier keine Nebensache ist.

6.4 Hintergrundbetrieb
ForegroundService
START_STICKY
kein FLAG_STOP_WITH_TASK
onTaskRemoved() bewusst behandeln
Notification als Kontrollzentrum
optionale, kurzlebige Wake-/Wifi-Locks nur sehr gezielt
Session-Wiederherstellung statt naive Annahme „Socket bleibt ewig offen“

Android dokumentiert, dass onTaskRemoved() nicht aufgerufen wird, wenn FLAG_STOP_WITH_TASK gesetzt ist; dann wird der Service einfach gestoppt. START_STICKY ist der etablierte Mechanismus, damit ein gestarteter Service nach Prozessverlust neu erzeugt werden kann. Wake Locks sollen laut Android nur sparsam und kurz gehalten werden, idealerweise im Rahmen eines Foreground Service.

7. Neue Kernannahmen
7.1 Terminal-Engine ist ein eigenes Subprojekt

Das Terminal ist kein Textfeld und kein Compose-Widget, sondern eine eigene Engine. vim, less, tmux, nano oder htop erzeugen kontinuierlich VT-/ANSI-Steuersequenzen für Cursorbewegung, Farbwechsel, Löschoperationen und Bildschirmbereiche. Daraus folgt: Die App plant das Terminal nicht als „Rendertext + bisschen Farbe“, sondern als separaten Parser-/Puffer-/Renderer-Komplex. Termux und ConnectBot existieren genau deshalb als eigenständige Terminal-/SSH-Projekte und nicht bloß als hübsche UI-Hüllen.

7.2 Krypto-Provider-Kompatibilität wird aktiv getestet

Die App geht nicht davon aus, dass SSHJ + Android + moderne Schlüssel auf Anhieb perfekt zusammenspielen. Stattdessen wird vor echter UI-Arbeit eine Krypto-/Provider-Testphase geplant:

RSA Host Key
Ed25519 Host Key
X25519 KEX
Passwort-Auth
Key-Auth
Host-Key-Änderung
Test auf echtem Zielgerät

Der Grund ist banal und unerquicklich: SSHJ hat reale Android-Issues zu X25519/Provider BC, Android rät vom expliziten Provider-Pinning außerhalb des Keystore ab, und ConnectBot trägt sichtbar Extra-Logik für Provider-Handling mit sich herum.

7.3 Hintergrundbetrieb heißt nicht „ewig offene TCP-Verbindung“

Die App wird nicht auf die Illusion gebaut, dass ein SSH-Socket über Stunden mit ausgeschaltetem Display garantiert offen bleibt. Androids Doze kann Netzwerkzugriff, Jobs und andere Aktivitäten einschränken; die Doku nennt persistent verbundene Apps ausdrücklich als betroffen. Deshalb lautet die Architekturannahme:

best effort keepalive im wachen Zustand
FGS für legitimen Hintergrundbetrieb
Reconnect nach Doze/Netzverlust
tmux-Reattach für echte Session-Kontinuität
8. Gesamtarchitektur
8.1 Schichten
ui/ nur Darstellung und Eingabekomponenten
presentation/ ViewModels und UI-State
service/ Session-Lebensdauer und Hintergrundbetrieb
domain/ Use Cases und Regeln
ssh/ Verbindungsebene
terminal/ Eingabe-/Ausgabe-Logik, Parser, Buffer, Renderer-State
data/ Persistenz und Repositories
8.2 Neue Regel für Terminal

TerminalViewport ist nur Anzeige.
Die echte Terminal-Logik lebt in:

TerminalState
TerminalBuffer
TerminalOutputParser
AnsiSequenceInterpreter
InputController
KeyMapper
9. Bedienkonzept
9.1 Grundlayout

Oben:

Hostname
Verbindungsstatus
Reconnect
Overflow

Mitte:

Terminalfläche
keine Sidebar
keine zweite Navigationsleiste
keine springenden Panels

Unten:

feste Sondertastenleiste
darunter Android-IME

Damit die UI nicht springt, wird Edge-to-Edge sauber eingerichtet und adjustResize plus Compose-Inset-Handling verwendet. Android empfiehlt genau das, damit IME-Insets synchron ins Layout einfließen statt chaotische Sprünge zu erzeugen.

9.2 Sondertastenleiste

Sichtbar und direkt erreichbar:

Ctrl
Alt
Esc
Tab
← ↑ ↓ →
Home
End
PgUp
PgDn

Ctrl, Alt und optional Shift werden als sticky keys umgesetzt:

ein Tap = für die nächste Taste aktiv
Doppeltap = eingerastet
erneuter Tap = lösen
10. UI-Stabilität und Anti-Springen-Regeln
Zustände werden hoisted, nicht in zig Composables verstreut
Hostlisten und Verlauf nutzen stabile Item-Keys
LazyListState wird sinnvoll gehalten/restauriert
Insets werden sauber behandelt
Sondertastenleiste bleibt höhenstabil
keine Netzwerk-/Seiteneffekte direkt in LazyColumn-Items

Compose empfiehlt State Hoisting als Single Source of Truth und stabile Keys in Lazy-Listen, damit Zustand und Scrollposition bei Datensatzänderungen korrekt erhalten bleiben. Compose-Stabilität ist außerdem ein echtes Performance-Thema, besonders bei häufigen Aktualisierungen.

11. Tailscale-Konzept

Die App integriert Tailscale nicht selbst, sondern nutzt das bereits laufende Tailscale-Netz auf dem Gerät. Sie bleibt ein SSH-Client. Tailscale ist nur die darunterliegende Netzschicht. Bei Fehlern liefert die App aber bessere Hinweise für:

DNS-Auflösung fehlgeschlagen
Host im Tailnet nicht erreichbar
SSH-Port nicht erreichbar
Tailscale auf Gerät möglicherweise nicht aktiv
12. Verbindungsstabilität: korrigiertes Kernkonzept
12.1 Session-Lebensdauer

Die aktive SSH-Verbindung läuft in einem Foreground Service.
Die Activity zeigt an und bindet sich an, hält die Verbindung aber nicht selbst. Foreground Services müssen deklariert werden und einen gültigen Typ tragen; specialUse ist für Fälle gedacht, die in keine andere FGS-Kategorie passen und nicht sinnvoll mit JobInfo abbildbar sind. Dafür braucht die App zusätzlich FOREGROUND_SERVICE_SPECIAL_USE und eine Service-Property android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE.

12.2 Wahl des FGS-Typs

Für diese private, sideloaded App ist specialUse die richtige Wahl.
Für einen Play-Store-Release wäre das riskanter, weil specialUse restriktiv behandelt werden kann. Hier ist das aber egal, weil das Projekt ausdrücklich nicht als Store-Produkt geplant ist.

12.3 Verhalten bei Hintergrund, Task-Entfernung und App-Stop
Hintergrund: Session bleibt im FGS aktiv
Aus Recents gewischt: onTaskRemoved() startet Grace-Mode, Service läuft weiter
Prozesskill: Service kann via START_STICKY neu aufgebaut werden, aber die alte SSH-Verbindung ist weg
Task-Manager / Active apps Stop: alles ist hart beendet, kein Callback, keine Magie

Android 13+ dokumentiert explizit, dass der Stop-Button in „Active apps“ die gesamte App aus dem Speicher entfernt, den Back Stack löscht und keine Callbacks liefert. Das ist nicht verhandelbar.

12.4 Grace-Period

Nach versehentlichem Wegwischen bleibt die Session für eine definierte Zeit bestehen:

10 Minuten Standard
30 Minuten optional
danach kontrolliertes Trennen bei Inaktivität
12.5 Korrigierte Stabilitätsdefinition

Die Architektur garantiert nicht „ewig offene Verbindung“, sondern:

FGS hält den Session-Manager lebendig
Heartbeat erkennt defekte Verbindungen
Reconnect springt bei Netz-/Doze-Abbruch an
tmux hält den Arbeitskontext serverseitig am Leben
12.6 tmux wird praktisch empfohlen

tmux ist nicht mehr bloß „später vielleicht nett“, sondern für reale Robustheit fast Pflicht.
Ohne tmux verlierst du bei Prozesskill/Netzverlust die interaktive Sitzung.
Mit tmux verlierst du im besten Fall nur die Leitung und attachst danach wieder.

13. Notification-Konzept

Die Foreground-Service-Notification ist aktiver Teil der Bedienung:

Hostname
Status
Laufzeit
tmux-Hinweis falls aktiv

Aktionen:

Zur Session zurückkehren
Reconnect
Disconnect
Grace-Period verlängern
Session beenden
14. Energiemanagement
14.1 Wake Locks

Wake Locks werden nicht dauerhaft gehalten.
Android warnt klar vor deutlichem Akkuverbrauch und empfiehlt, Wake Locks nur kurz und nur dann zu halten, wenn es keine bessere Alternative gibt. Am sinnvollsten sind sie, wenn überhaupt, im Kontext eines Foreground Service.

14.2 Battery Optimization

Eine Akku-Optimierungs-Ausnahme wird nicht automatisch eingefordert, sondern optional als „Stabilitätsmodus“ angeboten.

14.3 Doze-Realismus

Doze kann Netzwerkzugriffe verzögern oder aussetzen. Für persistente Live-Verbindungen ist das ein echter Gegner. Deshalb plant die App nicht auf „Doze wird schon nichts machen“, sondern auf:

Reconnect nach Aufwachen
schnelle Fehlerklassifikation
serverseitige Resilienz via tmux
15. Sicherheit
15.1 Host-Key-Handling

Beim ersten Verbindungsaufbau:

Host-Key anzeigen
Fingerprint anzeigen
Entscheidung: einmal erlauben / dauerhaft vertrauen / abbrechen

Danach:

bekannte Fingerprints lokal speichern
bei Änderung harte Warnung
keine stille Überschreibung
15.2 Passwortspeicherung

In Version 1:

Passwörter standardmäßig nicht dauerhaft speichern
bevorzugt SSH-Key-Nutzung
15.3 Krypto-Provider-Strategie

Nicht vorab dogmatisch festlegen.
Stattdessen:

SSHJ-Version >= 0.39.x einplanen
Android-Gerätetest mit echter Zielumgebung
Provider-/Key-Kombinationen validieren
erst dann final entscheiden, ob zusätzliche Provider-Logik nötig ist
16. Neue Umsetzungsphasen
Phase 0A: Terminal-Spike

Ziel: Terminal-Engine-Risiko prüfen, bevor UI ausgebaut wird.

Tests:

normale Shell
less
nano
tmux attach

Erst danach:

vim
htop

Wenn diese Phase scheitert, wird nicht blind weitergebaut.

Phase 0B: Krypto-/Provider-Spike

Ziel: SSHJ auf dem realen Android-Zielgerät absichern.

Tests:

RSA Host Key
Ed25519 Host Key
X25519 KEX
Passwort-Login
Key-Login
Host-Key-Wechsel
Test mit Android Keystore-geschütztem Material
Phase 1: Grundgerüst
Projekt aufsetzen
Compose-Navigation
Host CRUD
Settings-Grundlage
Phase 2: SSH-Basis
SSHJ integrieren
Passwortlogin
Host-Key-Prüfung
Shell-Channel
Phase 3: Terminal-Kern
Buffer
Parser
ANSI/VT-Interpretation
Sondertasten
KeyMapper
Phase 4: Stabilität
Foreground Service
Notification
START_STICKY
onTaskRemoved()
Grace-Period
Reconnect
Keepalive
Phase 5: Sicherheit
Keystore
SSH-Key-Speicherung
Biometrie optional
Fingerprint-Management
Phase 6: Tailscale-Feinschliff
bessere Fehlermeldungen
Hosttyp TAILSCALE
Verbindungstests
Phase 7: Robustheit
tmux-Auto-Attach
Logging
Doze-Tests
Akku-/Hintergrundtests
17. Verschärfte Teststrategie
17.1 Terminal
Shell
less
nano
tmux
vim
htop
17.2 Eingabe
Ctrl+C
Ctrl+D
Ctrl+L
Tab
Pfeile
Esc
Home/End/PgUp/PgDn
Hardware-Tastatur
17.3 Stabilität
App im Hintergrund
Display aus / wieder an
App aus Recents gewischt
Prozesskill
Reconnect nach WLAN-Wechsel
Reconnect nach Tailscale-Unterbrechung
Reconnect nach Doze
tmux-Reattach
17.4 Krypto
RSA
Ed25519
X25519
Passwort
Keyfile
Host-Key-Mismatch
18. Abnahmekriterien

Das Projekt gilt erst dann als erfolgreich, wenn:

Hostspeicherung und Verbindungsaufbau stabil funktionieren.
Fingerprints korrekt geprüft und verwaltet werden.
Ctrl, Alt, Esc, Tab und Pfeiltasten zuverlässig funktionieren.
Die Terminalansicht bei IME-Änderungen nicht springt.
Die Session im Hintergrund sinnvoll weiterlebt.
Nach versehentlichem Wegwischen die Grace-Period korrekt greift.
Nach Netz-/Doze-Abbruch ein sauberer Reconnect erfolgt.
Mit aktiviertem tmux die Arbeitsumgebung praktisch fortgesetzt werden kann.
RSA/Ed25519/X25519 auf dem Zielgerät getestet wurden.
Die App klein, fokussiert und privat bleibt, statt in Feature-Müll zu zerfallen.
19. Klare Schlussentscheidung

Die App wird als native Android-SSH-App geplant, mit:

Kotlin + Compose
SSHJ
Foreground Service
specialUse als bevorzugter FGS-Typ
sticky Sondertasten
Terminal-Engine als eigenes Risiko-Subsystem
Tailscale nur als Netzebene
tmux als praktisch empfohlener Robustheitsbaustein
explizitem Krypto-/Provider-Spike vor echter UI-Verliebtheit

Das ist die realistische Version des Plans. Nicht die gemütliche.

projektstruktur_ssh_client_android_revised.md
1. Zweck dieses Dokuments

Dieses Dokument definiert die konkrete technische Projektstruktur für die private Android-SSH-App.

Es beschreibt:

die Ordnerstruktur
die vorgesehenen Klassen und Dateien
die Verantwortung jeder Datei
die Abhängigkeiten zwischen den Schichten
die sinnvolle Reihenfolge der Umsetzung

Es enthält bewusst keinen Quellcode.

2. Grundsatzänderungen gegenüber der ersten Fassung

Die neue Struktur wurde an drei Stellen verschärft:

Terminal-Engine wird als eigenes Subsystem behandelt, nicht als Compose-Anhängsel. Termux und ConnectBot zeigen durch ihre Existenz bereits, dass Terminal- und SSH-Logik jeweils eigenständige Kerne sind.
SSH-/Krypto-Kompatibilität auf Android wird als Frühphase geplant, weil SSHJ zwar moderner geworden ist, aber reale Android-Probleme mit Provider-/Algorithmus-Kombinationen dokumentiert sind.
Service-Lebensdauer wird auf Wiederherstellbarkeit statt auf naive Dauerverbindung ausgelegt, weil Doze, Prozessverlust und aktives Stoppen der App reale Grenzen setzen.
3. Zielstruktur des Projekts
app/
└── src/main/java/com/example/privatessh/
    ├── MainActivity.kt
    ├── App.kt
    │
    ├── core/
    │   ├── constants/
    │   │   ├── AppConstants.kt
    │   │   ├── Defaults.kt
    │   │   └── TerminalDefaults.kt
    │   ├── dispatchers/
    │   │   └── DispatcherProvider.kt
    │   ├── logging/
    │   │   ├── AppLogger.kt
    │   │   └── LogTags.kt
    │   ├── result/
    │   │   ├── AppError.kt
    │   │   ├── AppResult.kt
    │   │   └── ErrorMapper.kt
    │   ├── network/
    │   │   ├── NetworkType.kt
    │   │   └── ReachabilityClassifier.kt
    │   └── utils/
    │       ├── TimeUtils.kt
    │       ├── StringUtils.kt
    │       └── FingerprintFormatter.kt
    │
    ├── navigation/
    │   ├── AppRoutes.kt
    │   ├── AppNavHost.kt
    │   └── NavActions.kt
    │
    ├── ui/
    │   ├── theme/
    │   │   ├── Color.kt
    │   │   ├── Theme.kt
    │   │   └── Type.kt
    │   ├── components/
    │   │   ├── AppTopBar.kt
    │   │   ├── StatusChip.kt
    │   │   ├── SectionHeader.kt
    │   │   ├── ConfirmDialog.kt
    │   │   ├── EmptyStateView.kt
    │   │   └── LoadingView.kt
    │   ├── hostlist/
    │   │   ├── HostListScreen.kt
    │   │   ├── HostListTopBar.kt
    │   │   ├── HostListContent.kt
    │   │   ├── HostCard.kt
    │   │   └── HostListEvent.kt
    │   ├── hostedit/
    │   │   ├── HostEditScreen.kt
    │   │   ├── HostEditForm.kt
    │   │   ├── HostEditSections.kt
    │   │   └── HostEditEvent.kt
    │   ├── terminal/
    │   │   ├── TerminalScreen.kt
    │   │   ├── TerminalTopBar.kt
    │   │   ├── TerminalViewport.kt
    │   │   ├── SpecialKeyBar.kt
    │   │   ├── SessionStatusBar.kt
    │   │   ├── TerminalSelectionToolbar.kt
    │   │   └── TerminalEvent.kt
    │   ├── settings/
    │   │   ├── SettingsScreen.kt
    │   │   ├── SettingsContent.kt
    │   │   └── SettingsEvent.kt
    │   └── dialogs/
    │       ├── FingerprintDialog.kt
    │       ├── ReconnectDialog.kt
    │       ├── DisconnectDialog.kt
    │       └── GracePeriodDialog.kt
    │
    ├── presentation/
    │   ├── hostlist/
    │   │   ├── HostListViewModel.kt
    │   │   ├── HostListUiState.kt
    │   │   └── HostListUiEffect.kt
    │   ├── hostedit/
    │   │   ├── HostEditViewModel.kt
    │   │   ├── HostEditUiState.kt
    │   │   └── HostEditValidator.kt
    │   ├── terminal/
    │   │   ├── TerminalViewModel.kt
    │   │   ├── TerminalUiState.kt
    │   │   ├── TerminalUiEffect.kt
    │   │   └── TerminalBinder.kt
    │   └── settings/
    │       ├── SettingsViewModel.kt
    │       └── SettingsUiState.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── HostProfile.kt
    │   │   ├── KnownHostEntry.kt
    │   │   ├── SessionPolicy.kt
    │   │   ├── SessionSnapshot.kt
    │   │   ├── SessionStatus.kt
    │   │   ├── AuthType.kt
    │   │   ├── NetworkTargetType.kt
    │   │   └── TerminalMetrics.kt
    │   ├── usecase/
    │   │   ├── host/
    │   │   │   ├── GetHostsUseCase.kt
    │   │   │   ├── GetHostByIdUseCase.kt
    │   │   │   ├── SaveHostUseCase.kt
    │   │   │   ├── DeleteHostUseCase.kt
    │   │   │   └── DuplicateHostUseCase.kt
    │   │   ├── session/
    │   │   │   ├── StartSessionUseCase.kt
    │   │   │   ├── StopSessionUseCase.kt
    │   │   │   ├── ReconnectSessionUseCase.kt
    │   │   │   ├── ObserveSessionUseCase.kt
    │   │   │   ├── ResizeTerminalUseCase.kt
    │   │   │   └── SendTerminalInputUseCase.kt
    │   │   ├── security/
    │   │   │   ├── VerifyHostKeyUseCase.kt
    │   │   │   ├── TrustHostKeyUseCase.kt
    │   │   │   ├── UnlockPrivateKeyUseCase.kt
    │   │   │   └── LoadPrivateKeyUseCase.kt
    │   │   ├── settings/
    │   │   │   ├── GetSettingsUseCase.kt
    │   │   │   └── UpdateSettingsUseCase.kt
    │   │   └── network/
    │   │       ├── ResolveTargetUseCase.kt
    │   │       ├── CheckReachabilityUseCase.kt
    │   │       └── ClassifyConnectionFailureUseCase.kt
    │   └── repository/
    │       ├── HostRepository.kt
    │       ├── KnownHostRepository.kt
    │       ├── SessionRepository.kt
    │       ├── SettingsRepository.kt
    │       └── SecureKeyRepository.kt
    │
    ├── data/
    │   ├── local/
    │   │   ├── db/
    │   │   │   ├── AppDatabase.kt
    │   │   │   ├── dao/
    │   │   │   │   ├── HostDao.kt
    │   │   │   │   ├── KnownHostDao.kt
    │   │   │   │   └── SessionSnapshotDao.kt
    │   │   │   ├── entity/
    │   │   │   │   ├── HostEntity.kt
    │   │   │   │   ├── KnownHostEntity.kt
    │   │   │   │   └── SessionSnapshotEntity.kt
    │   │   │   └── mapper/
    │   │   │       ├── HostEntityMapper.kt
    │   │   │       ├── KnownHostEntityMapper.kt
    │   │   │       └── SessionSnapshotMapper.kt
    │   │   ├── datastore/
    │   │   │   ├── SettingsDataStore.kt
    │   │   │   └── SettingsSerializer.kt
    │   │   └── secure/
    │   │       ├── KeystoreManager.kt
    │   │       ├── SecureKeyStorage.kt
    │   │       └── BiometricGate.kt
    │   ├── repository/
    │   │   ├── HostRepositoryImpl.kt
    │   │   ├── KnownHostRepositoryImpl.kt
    │   │   ├── SessionRepositoryImpl.kt
    │   │   ├── SettingsRepositoryImpl.kt
    │   │   └── SecureKeyRepositoryImpl.kt
    │   └── mapper/
    │       └── FailureClassifierMapper.kt
    │
    ├── ssh/
    │   ├── SshClientFactory.kt
    │   ├── SshSessionEngine.kt
    │   ├── SshSessionConfig.kt
    │   ├── SshSessionState.kt
    │   ├── SshErrorClassifier.kt
    │   ├── provider/
    │   │   ├── CryptoProviderProbe.kt
    │   │   ├── AndroidCryptoCompatibility.kt
    │   │   └── AlgorithmSupportMatrix.kt
    │   ├── auth/
    │   │   ├── AuthStrategy.kt
    │   │   ├── PasswordAuthStrategy.kt
    │   │   ├── PrivateKeyAuthStrategy.kt
    │   │   └── KeyboardInteractiveAuthStrategy.kt
    │   ├── hostkey/
    │   │   ├── HostKeyDecision.kt
    │   │   ├── HostKeyVerifierAdapter.kt
    │   │   └── KnownHostsBridge.kt
    │   ├── io/
    │   │   ├── ShellChannelAdapter.kt
    │   │   ├── OutputPump.kt
    │   │   ├── ErrorPump.kt
    │   │   └── InputWriter.kt
    │   ├── reconnect/
    │   │   ├── ReconnectController.kt
    │   │   ├── ReconnectBackoffPolicy.kt
    │   │   └── ReconnectTrigger.kt
    │   └── keepalive/
    │       ├── KeepAliveController.kt
    │       └── HeartbeatMonitor.kt
    │
    ├── terminal/
    │   ├── TerminalState.kt
    │   ├── TerminalBuffer.kt
    │   ├── TerminalLine.kt
    │   ├── TerminalCursor.kt
    │   ├── TerminalColor.kt
    │   ├── TerminalRendererState.kt
    │   ├── TerminalResizeController.kt
    │   ├── capabilities/
    │   │   ├── TerminalCapabilityLevel.kt
    │   │   ├── TerminalFeatureMatrix.kt
    │   │   └── TerminalProbeReport.kt
    │   ├── input/
    │   │   ├── InputController.kt
    │   │   ├── ModifierKeyState.kt
    │   │   ├── KeyAction.kt
    │   │   ├── KeyMapper.kt
    │   │   ├── SpecialKey.kt
    │   │   └── HardwareKeyMapper.kt
    │   ├── output/
    │   │   ├── TerminalOutputParser.kt
    │   │   ├── AnsiSequenceInterpreter.kt
    │   │   ├── TerminalTextAppender.kt
    │   │   └── TerminalDamageTracker.kt
    │   └── selection/
    │       ├── SelectionState.kt
    │       ├── SelectionController.kt
    │       └── ClipboardController.kt
    │
    ├── service/
    │   ├── TerminalSessionService.kt
    │   ├── SessionServiceBinder.kt
    │   ├── SessionForegroundController.kt
    │   ├── SessionNotificationFactory.kt
    │   ├── SessionGraceController.kt
    │   ├── SessionRegistry.kt
    │   └── SessionActionReceiver.kt
    │
    ├── diagnostics/
    │   ├── terminal/
    │   │   ├── TerminalSpikeRunner.kt
    │   │   └── TerminalScenarioSuite.kt
    │   ├── crypto/
    │   │   ├── CryptoProbeRunner.kt
    │   │   └── SshAlgorithmScenarioSuite.kt
    │   └── background/
    │       ├── DozeTestPlan.kt
    │       └── SessionRecoveryScenarioSuite.kt
    │
    └── di/
        ├── AppModule.kt
        ├── RepositoryModule.kt
        ├── UseCaseModule.kt
        ├── ServiceModule.kt
        └── SshModule.kt
4. Neue oder geänderte Klassen
ssh/provider/CryptoProviderProbe.kt

Aufgabe:

prüft auf echtem Gerät, welche Algorithmen/Kombinationen tatsächlich verfügbar sind
dokumentiert, ob RSA, Ed25519 und X25519 unter der gewählten SSHJ-/Android-Kombination funktionieren

Begründung: Android rät vom harten Provider-Pinning außerhalb des Keystore ab, und SSHJ hat reale Android-Issues zu X25519/BC. Also wird das nicht blind geraten, sondern gemessen.

ssh/provider/AndroidCryptoCompatibility.kt

Aufgabe:

bündelt Entscheidungen zur Android-spezifischen Provider-Kompatibilität
verhindert, dass Provider-Workarounds quer durch den Code verstreut werden
ssh/provider/AlgorithmSupportMatrix.kt

Aufgabe:

hält die validierten Kombinationen von:
Host Key Typ
KEX
Auth-Methode
Android-Version
Geräteteststatus
terminal/capabilities/TerminalCapabilityLevel.kt

Aufgabe:

definiert Funktionsstufen des Terminals:
BASIC_SHELL
LINE_EDITOR_OK
TMUX_OK
VIM_READY
FULLSCREEN_TUI_READY

Damit wird das Projekt nicht so getan, als wäre vim automatisch selbstverständlich, nur weil echo hallo funktioniert.

terminal/capabilities/TerminalFeatureMatrix.kt

Aufgabe:

listet, welche Features aktuell sicher unterstützt werden:
Cursorbewegung
Farben
Clear Screen
Scrollregion
Alternate Screen
Home/End/PgUp/PgDn
Resize
Bracketed Paste
Mouse Reporting später optional
terminal/output/TerminalDamageTracker.kt

Aufgabe:

markiert nur geänderte Bildschirmbereiche
verhindert unnötiges Komplett-Neuzeichnen bei schneller Terminalausgabe

Bei hohem Output ist das wichtig, weil Compose-Stabilität und Recomposition sonst schnell unerquicklich werden. Compose selbst macht klar, dass Instabilität und ungünstige Datenmodelle zu unnötiger Recomposition führen können.

diagnostics/terminal/TerminalSpikeRunner.kt

Aufgabe:

führt gezielt Terminal-Szenarien aus, bevor volle UI-Politur beginnt
diagnostics/crypto/CryptoProbeRunner.kt

Aufgabe:

führt gezielt SSH-/Krypto-Szenarien auf echtem Android aus
diagnostics/background/SessionRecoveryScenarioSuite.kt

Aufgabe:

dokumentiert und prüft:
Hintergrund
Bildschirm aus
Recents-Wisch
Prozesskill
Reconnect
tmux-Reattach
5. Schärfere Verantwortlichkeiten
TerminalViewport.kt

Verantwortung:

nur Anzeige
keine VT-/ANSI-Interpretation
keine Sessionlogik
keine Netzwerklogik
AnsiSequenceInterpreter.kt

Verantwortung:

ANSI-/VT-Sequenzen verarbeiten
Cursor-/Lösch-/Scrolloperationen in Terminalzustandsänderungen übersetzen
TerminalOutputParser.kt

Verantwortung:

Rohbytes aus der SSH-Session lesen
Sequenzen und Text trennen
an Interpreter und Buffer weiterreichen
TerminalBuffer.kt

Verantwortung:

Bildschirm- und Scrollback-Inhalt halten
Änderungen effizient speichern
Grundlage für Anzeige liefern
SshSessionEngine.kt

Verantwortung:

SSH verbinden
authentifizieren
Shell öffnen
I/O pumpen
Heartbeat/Keepalive anstoßen
Reconnect-Hooks liefern
TerminalSessionService.kt

Verantwortung:

Session im Foreground Service halten
Notification pflegen
onTaskRemoved() behandeln
Grace-Period verwalten
UI-unabhängiges Weiterleben der Session

onTaskRemoved() ist für diesen Plan wichtig, weil Android genau diesen Callback liefert, wenn der Task entfernt wird, solange FLAG_STOP_WITH_TASK nicht gesetzt ist.

6. Verschärfte Abhängigkeitsregeln
ui kennt nie SSHJ
ui kennt nie direkte Terminal-Parsing-Logik
presentation kennt nie Provider-Workarounds
service ist die einzige Schicht, die Session-Lebensdauer orchestriert
terminal/output kennt keine Compose-APIs
ssh/provider ist isoliert, damit Provider-/Kompatibilitätskram nicht das ganze Projekt infiziert
7. Neue Reihenfolge der Umsetzung
Phase 0A: Terminal-Spike zuerst

Dateien zuerst:

TerminalState.kt
TerminalBuffer.kt
TerminalOutputParser.kt
AnsiSequenceInterpreter.kt
TerminalFeatureMatrix.kt
TerminalSpikeRunner.kt
TerminalScenarioSuite.kt

Ziel:

prüfen, ob die gewählte Terminalstrategie für Shell, less, nano, tmux überhaupt tragfähig ist

Abnahme:

normale Shell stabil
less stabil
nano stabil
tmux attach stabil

vim und htop kommen erst danach.

Phase 0B: Crypto-/Provider-Spike

Danach:

CryptoProviderProbe.kt
AndroidCryptoCompatibility.kt
AlgorithmSupportMatrix.kt
CryptoProbeRunner.kt
SshAlgorithmScenarioSuite.kt

Ziel:

SSHJ auf dem echten Zielgerät absichern

Abnahme:

RSA ok
Ed25519 ok oder klar dokumentiert nicht ok
X25519 ok oder klar dokumentiert nicht ok
Host-Key-Prüfung stabil
Key-Login stabil
Phase 1: Fundament
App.kt
MainActivity.kt
Navigation
Theme
Constants
Phase 2: Domain und Persistenz
Domain-Modelle
Repository-Verträge
Room / DataStore / Keystore
Phase 3: Hostverwaltung
Host CRUD
Hostliste
Hostedit
Validation
Phase 4: SSH-Kern
SshClientFactory.kt
Auth-Strategien
Host-Key-Brücke
SshSessionEngine.kt
Phase 5: Terminal-Eingabe
ModifierKeyState.kt
KeyMapper.kt
InputController.kt
HardwareKeyMapper.kt
SpecialKeyBar.kt
Phase 6: Service und Stabilität
TerminalSessionService.kt
SessionForegroundController.kt
SessionGraceController.kt
ReconnectController.kt
KeepAliveController.kt
HeartbeatMonitor.kt
Phase 7: Terminal-UI
TerminalViewModel.kt
TerminalScreen.kt
TerminalViewport.kt
SessionStatusBar.kt
Dialoge
Phase 8: Hintergrund-/Doze-Härtung
DozeTestPlan.kt
SessionRecoveryScenarioSuite.kt
Notification-Aktionen
tmux-Auto-Attach
8. Neue MVP-Dateien

Wenn extrem fokussiert gestartet wird, sind diese Dateien die kleinste sinnvolle senkrechte Scheibe:

MainActivity.kt
AppNavHost.kt
HostProfile.kt
HostRepository.kt
HostRepositoryImpl.kt
HostListViewModel.kt
HostEditViewModel.kt
HostListScreen.kt
HostEditScreen.kt
SshSessionEngine.kt
CryptoProviderProbe.kt
TerminalState.kt
TerminalBuffer.kt
TerminalOutputParser.kt
AnsiSequenceInterpreter.kt
InputController.kt
ModifierKeyState.kt
KeyMapper.kt
TerminalSessionService.kt
TerminalViewModel.kt
TerminalScreen.kt
SpecialKeyBar.kt
9. Verschärfte Teststruktur
Unit Tests
KeyMapper
ModifierKeyState
ReconnectBackoffPolicy
SshErrorClassifier
HostEditValidator
SessionGraceController
AnsiSequenceInterpreter
TerminalOutputParser
Instrumentation / Device Tests
echte Host-Key-Dialoge
Hintergrund / Bildschirm aus
App aus Recents gewischt
Notification-Aktionen
Reconnect
tmux-Reattach
IME / Insets / Rotation

Android weist bei State-Saving und Prozessverlust ausdrücklich darauf hin, dass UI-Zustand durch Konfigurationsänderungen oder systeminitiierte Prozessrekreation verloren gehen kann. Genau deshalb werden Session- und Terminalzustände hier strikt getrennt behandelt.

10. Klare Bauempfehlung

Die sinnvolle Reihenfolge lautet jetzt:

Terminalrisiko validieren
Kryptorisiko validieren
Hostverwaltung sauber bauen
SSH-Kern einziehen
Eingabe korrekt modellieren
Service-Lebensdauer bauen
danach erst UI-Feinschliff

Nicht andersherum.
Wer erst das hübsche Terminal malt und erst danach fragt, ob vim, X25519 und Doze überhaupt mitspielen, baut sich nur eine elegante Ruine.
