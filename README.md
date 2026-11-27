# Spyglass
**Spyglass** – веб сервис для парсинга майнкрафт логов игроков:
сервис разбирает `.log` файл, выделяет список модов и ресурспаков
и хранит историю проверок

## воркинг

```bash
docker compose up -d
docker compose down
```

Параметры подключения по умолчанию (`spyglass/spyglass` на `localhost:5432`)
можно переопределить переменными окружения `DB_URL`, `DB_USER` и `DB_PASSWORD`.
