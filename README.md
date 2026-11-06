# Danger Sense

App nativo Android (Kotlin + Jetpack Compose) de monitoramento sensorial para usuários neurodivergentes (TEA e TDAH).

## Recursos

- **SensorTelemetryService** — Foreground Service que monitora acelerômetro, giroscópio e microfone em tempo real, detectando picos de movimento (estereotipias/STIMs) e ambientes barulhentos (sobrecarga sensorial).
- **SensoryVoiceTriggerReceiver** — escuta por voz "Estou em crise", registra data/hora/dia no Room e inicia triagem por TTS em pt-BR (voz pausada estilo Steve Jobs): sobrecarga sensorial / meltdown / shutdown, respondendo com dicas de regulação (respiração quadrada, STIMs motores, binaural sub-grave + UI a zero).
- **SoundMapManager** — mapa sonoro contínuo do dia (maior pico em dB + horário exato) e `Sensor.TYPE_AMBIENT_TEMPERATURE`.
- **AppUsageTracker** — identifica o aplicativo em primeiro plano via `UsageStatsManager` e vincula o pacote aos picos de estresse.
- **LocalCrisisEngine + CrisisPredictionEngine** — banco Room local e predição diária de crise com pesos de sono (exponencial), ruído (80–85 dB), dieta (eixo intestino-cérebro), temperatura, atividades e histórico.
- **Diagnóstico rotineiro** — questionário (sono, dieta, atividades) agendável por `AlarmManager` com notificação diária e percentual de chance de crise.
- **Quick Settings Tile** — botão de ligar/desligar o monitoramento na Central de Notificações.
- **UI OneUI 9** — Compose com Verde Grafite, acentos Aurora, alertas em Amarelo, raios de 32 dp e barra de descarga cognitiva contínua. Alerta: **"Sentidos Sobrecarregados"**.

## Build

```bash
./gradlew assembleRelease
```

APK assinado: `app/build/outputs/apk/release/app-release.apk`

## Permissões

- `RECORD_AUDIO` — nível de ruído ambiente
- `PACKAGE_USAGE_STATS` — app em primeiro plano (conceder em Ajustes → Uso de dados)
- `FOREGROUND_SERVICE_MICROPHONE` / `SPECIAL_USE` — telemetria em primeiro plano

## Licença

MIT
