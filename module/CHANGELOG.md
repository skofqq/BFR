# Box for Root (Boxy edition)

## v1.10.2-ru.4

- `box.tool subs` exits with 0 after a successful update, apps no longer report a failure (upstream #219)
- sing-box: only the active config is formatted, local rule sets in the folder no longer break the start (upstream #203)
- `dns_hijack=false`: port 53 is left to the system or another module such as AdGuard Home (upstream #213)
- `user_agent` for downloads and subscriptions in settings.ini (upstream #138)

- `box.tool subs` завершается с кодом 0 после успешного обновления, приложения больше не пишут об ошибке (#219)
- sing-box: форматируется только активная конфигурация, локальные наборы правил в папке больше не ломают запуск (#203)
- `dns_hijack=false`: порт 53 остаётся системе или другому модулю, например AdGuard Home (#213)
- `user_agent` для загрузок и подписок в settings.ini (#138)

## v1.10.2-ru.3

- Updates come from github.com/skofqq/BFR (updateJson), not from upstream, so the ru changes are not replaced
- Subscription update: the new config is tested by the core first; if it fails, the previous config is kept
- Mobile data rules by SIM operator: `use_sim_matching`, `use_sim_list_mode`, `sim_operators_list` (name or MCC+MNC)
- Installer tests the active config with an installed core and reports errors
- `config_docs.yaml` comments in English

- Обновления модуля идут из github.com/skofqq/BFR, а не из оригинального репозитория, поэтому ru-изменения не затираются
- Обновление подписки: новый конфиг сначала проверяется ядром, при ошибке остаётся прежний
- Правила для мобильной сети по оператору SIM: `use_sim_matching`, `use_sim_list_mode`, `sim_operators_list` (название или MCC+MNC)
- Установщик проверяет активную конфигурацию установленным ядром и сообщает об ошибках
- Комментарии в `config_docs.yaml` на английском

## v1.10.2-ru.2

- Hotspot client MAC filter, Wi-Fi matching by router MAC (BSSID), installer keeps settings.ini values
