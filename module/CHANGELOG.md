# Box for Root (Boxy edition)

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
