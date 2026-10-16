## CI/CD

Проект использует GitHub Actions для автоматизации:

- **Backend CI** — сборка, Checkstyle, тесты, покрытие
- **Frontend CI** — ESLint и сборка Vite
- **Commit Lint** — проверка сообщений коммитов
### Защита веток

Ветки `main` и `develop` защищены:
- Только через Pull Request
- Обязательное прохождение всех status checks
- Запрет force-push и удаления
