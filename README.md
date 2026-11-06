# Danger Sense

App nativo Android (Kotlin + Jetpack Compose) de monitoramento sensorial para usuários neurodivergentes (TEA e TDAH).

## Recursos

- **SensorTelemetryService** — Foreground Service que monitora acelerômetro, giroscópio e microfone em tempo real, detectando picos de movimento (estereotipias/STIMs) e ambientes barulhentos (sobrecarga sensorial).
- **AppUsageTracker** — identifica o aplicativo em primeiro plano via `UsageStatsManager` e vincula o pacote aos picos de estresse.
- **LocalCrisisEngine** — banco Room local (timestamp, dia da semana, hora, pico de movimento, decibéis, app ativo) com predição de crise iminente por cluster histórico ponderado temporalmente.
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
