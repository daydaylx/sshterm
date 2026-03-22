# Projektstruktur SSH Client Android (Revised)

## 1. Zweck dieses Dokuments

Dieses Dokument definiert die konkrete technische Projektstruktur für die private Android-SSH-App.

Es beschreibt:

- die Ordnerstruktur
- die vorgesehenen Klassen und Dateien
- die Verantwortung jeder Datei
- die Abhängigkeiten zwischen den Schichten
- die sinnvolle Reihenfolge der Umsetzung

Es enthält bewusst keinen Quellcode.

## 2. Grundsatzänderungen gegenüber der ersten Fassung

Die neue Struktur wurde an drei Stellen verschärft:

1. **Terminal-Engine wird als eigenes Subsystem behandelt**, nicht als Compose-Anhängsel. Termux und ConnectBot zeigen durch ihre Existenz bereits, dass Terminal- und SSH-Logik jeweils eigenständige Kerne sind.
2. **SSH-/Krypto-Kompatibilität auf Android wird als Frühphase geplant**, weil SSHJ zwar moderner geworden ist, aber reale Android-Probleme mit Provider-/Algorithmus-Kombinationen dokumentiert sind.
3. **Service-Lebensdauer wird auf Wiederherstellbarkeit statt auf naive Dauerverbindung ausgelegt**, weil Doze, Prozessverlust und aktives Stoppen der App reale Grenzen setzen.

## 3. Zielstruktur des Projekts

```
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
```

## 4. Neue oder geänderte Klassen

### `ssh/provider/CryptoProviderProbe.kt`

**Aufgabe:**
- prüft auf echtem Gerät, welche Algorithmen/Kombinationen tatsächlich verfügbar sind
- dokumentiert, ob RSA, Ed25519 und X25519 unter der gewählten SSHJ-/Android-Kombination funktionieren

**Begründung:** Android rät vom harten Provider-Pinning außerhalb des Keystore ab, und SSHJ hat reale Android-Issues zu X25519/BC. Also wird das nicht blind geraten, sondern gemessen.

### `ssh/provider/AndroidCryptoCompatibility.kt`

**Aufgabe:**
- bündelt Entscheidungen zur Android-spezifischen Provider-Kompatibilität
- verhindert, dass Provider-Workarounds quer durch den Code verstreut werden

### `ssh/provider/AlgorithmSupportMatrix.kt`

**Aufgabe:**
- hält die validierten Kombinationen von:
  - Host Key Typ
  - KEX
  - Auth-Methode
  - Android-Version
  - Geräteteststatus

### `terminal/capabilities/TerminalCapabilityLevel.kt`

**Aufgabe:**
- definiert Funktionsstufen des Terminals:
  - BASIC_SHELL
  - LINE_EDITOR_OK
  - TMUX_OK
  - VIM_READY
  - FULLSCREEN_TUI_READY

Damit wird das Projekt nicht so getan, als wäre vim automatisch selbstverständlich, nur weil echo hallo funktioniert.

### `terminal/capabilities/TerminalFeatureMatrix.kt`

**Aufgabe:**
- listet, welche Features aktuell sicher unterstützt werden:
  - Cursorbewegung
  - Farben
  - Clear Screen
  - Scrollregion
  - Alternate Screen
  - Home/End/PgUp/PgDn
  - Resize
  - Bracketed Paste
  - Mouse Reporting später optional

### `terminal/output/TerminalDamageTracker.kt`

**Aufgabe:**
- markiert nur geänderte Bildschirmbereiche
- verhindert unnötiges Komplett-Neuzeichnen bei schneller Terminalausgabe

Bei hohem Output ist das wichtig, weil Compose-Stabilität und Recomposition sonst schnell unerquicklich werden. Compose selbst macht klar, dass Instabilität und ungünstige Datenmodelle zu unnötiger Recomposition führen können.

### `diagnostics/terminal/TerminalSpikeRunner.kt`

**Aufgabe:**
- führt gezielt Terminal-Szenarien aus, bevor volle UI-Politur beginnt

### `diagnostics/crypto/CryptoProbeRunner.kt`

**Aufgabe:**
- führt gezielt SSH-/Krypto-Szenarien auf echtem Android aus

### `diagnostics/background/SessionRecoveryScenarioSuite.kt`

**Aufgabe:**
- dokumentiert und prüft:
  - Hintergrund
  - Bildschirm aus
  - Recents-Wisch
  - Prozesskill
  - Reconnect
  - tmux-Reattach

## 5. Schärfere Verantwortlichkeiten

### `TerminalViewport.kt`

**Verantwortung:**
- nur Anzeige
- keine VT-/ANSI-Interpretation
- keine Sessionlogik
- keine Netzwerklogik

### `AnsiSequenceInterpreter.kt`

**Verantwortung:**
- ANSI-/VT-Sequenzen verarbeiten
- Cursor-/Lösch-/Scrolloperationen in Terminalzustandsänderungen übersetzen

### `TerminalOutputParser.kt`

**Verantwortung:**
- Rohbytes aus der SSH-Session lesen
- Sequenzen und Text trennen
- an Interpreter und Buffer weiterreichen

### `TerminalBuffer.kt`

**Verantwortung:**
- Bildschirm- und Scrollback-Inhalt halten
- Änderungen effizient speichern
- Grundlage für Anzeige liefern

### `SshSessionEngine.kt`

**Verantwortung:**
- SSH verbinden
- authentifizieren
- Shell öffnen
- I/O pumpen
- Heartbeat/Keepalive anstoßen
- Reconnect-Hooks liefern

### `TerminalSessionService.kt`

**Verantwortung:**
- Session im Foreground Service halten
- Notification pflegen
- onTaskRemoved() behandeln
- Grace-Period verwalten
- UI-unabhängiges Weiterleben der Session

onTaskRemoved() ist für diesen Plan wichtig, weil Android genau diesen Callback liefert, wenn der Task entfernt wird, solange FLAG_STOP_WITH_TASK nicht gesetzt ist.

## 6. Verschärfte Abhängigkeitsregeln

- `ui` kennt nie SSHJ
- `ui` kennt nie direkte Terminal-Parsing-Logik
- `presentation` kennt nie Provider-Workarounds
- `service` ist die einzige Schicht, die Session-Lebensdauer orchestriert
- `terminal/output` kennt keine Compose-APIs
- `ssh/provider` ist isoliert, damit Provider-/Kompatibilitätskram nicht das ganze Projekt infiziert

## 7. Neue Reihenfolge der Umsetzung

### Phase 0A: Terminal-Spike zuerst

**Dateien zuerst:**
- TerminalState.kt
- TerminalBuffer.kt
- TerminalOutputParser.kt
- AnsiSequenceInterpreter.kt
- TerminalFeatureMatrix.kt
- TerminalSpikeRunner.kt
- TerminalScenarioSuite.kt

**Ziel:**
- prüfen, ob die gewählte Terminalstrategie für Shell, less, nano, tmux überhaupt tragfähig ist

**Abnahme:**
- normale Shell stabil
- less stabil
- nano stabil
- tmux attach stabil

vim und htop kommen erst danach.

### Phase 0B: Crypto-/Provider-Spike

**Danach:**
- CryptoProviderProbe.kt
- AndroidCryptoCompatibility.kt
- AlgorithmSupportMatrix.kt
- CryptoProbeRunner.kt
- SshAlgorithmScenarioSuite.kt

**Ziel:**
- SSHJ auf dem echten Zielgerät absichern

**Abnahme:**
- RSA ok
- Ed25519 ok oder klar dokumentiert nicht ok
- X25519 ok oder klar dokumentiert nicht ok
- Host-Key-Prüfung stabil
- Key-Login stabil

### Phase 1: Fundament
- App.kt
- MainActivity.kt
- Navigation
- Theme
- Constants

### Phase 2: Domain und Persistenz
- Domain-Modelle
- Repository-Verträge
- Room / DataStore / Keystore

### Phase 3: Hostverwaltung
- Host CRUD
- Hostliste
- Hostedit
- Validation

### Phase 4: SSH-Kern
- SshClientFactory.kt
- Auth-Strategien
- Host-Key-Brücke
- SshSessionEngine.kt

### Phase 5: Terminal-Eingabe
- ModifierKeyState.kt
- KeyMapper.kt
- InputController.kt
- HardwareKeyMapper.kt
- SpecialKeyBar.kt

### Phase 6: Service und Stabilität
- TerminalSessionService.kt
- SessionForegroundController.kt
- SessionGraceController.kt
- ReconnectController.kt
- KeepAliveController.kt
- HeartbeatMonitor.kt

### Phase 7: Terminal-UI
- TerminalViewModel.kt
- TerminalScreen.kt
- TerminalViewport.kt
- SessionStatusBar.kt
- Dialoge

### Phase 8: Hintergrund-/Doze-Härtung
- DozeTestPlan.kt
- SessionRecoveryScenarioSuite.kt
- Notification-Aktionen
- tmux-Auto-Attach

## 8. Neue MVP-Dateien

Wenn extrem fokussiert gestartet wird, sind diese Dateien die kleinste sinnvolle senkrechte Scheibe:

```
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
```

## 9. Verschärfte Teststruktur

### Unit Tests
- KeyMapper
- ModifierKeyState
- ReconnectBackoffPolicy
- SshErrorClassifier
- HostEditValidator
- SessionGraceController
- AnsiSequenceInterpreter
- TerminalOutputParser

### Instrumentation / Device Tests
- echte Host-Key-Dialoge
- Hintergrund / Bildschirm aus
- App aus Recents gewischt
- Notification-Aktionen
- Reconnect
- tmux-Reattach
- IME / Insets / Rotation

Android weist bei State-Saving und Prozessverlust ausdrücklich darauf hin, dass UI-Zustand durch Konfigurationsänderungen oder systeminitiierte Prozessrekreation verloren gehen kann. Genau deshalb werden Session- und Terminalzustände hier strikt getrennt behandelt.

## 10. Klare Bauempfehlung

Die sinnvolle Reihenfolge lautet jetzt:

1. Terminalrisiko validieren
2. Kryptorisiko validieren
3. Hostverwaltung sauber bauen
4. SSH-Kern einziehen
5. Eingabe korrekt modellieren
6. Service-Lebensdauer bauen
7. danach erst UI-Feinschliff

Nicht andersherum.

Wer erst das hübsche Terminal malt und erst danach fragt, ob vim, X25519 und Doze überhaupt mitspielen, baut sich nur eine elegante Ruine.
