# Box for Root module (Boxy edition)

Based on [taamarin/box_for_magisk](https://github.com/taamarin/box_for_magisk) v1.10.2 (GPL-3.0).

Changes on top of v1.10.2:

- Unreleased upstream fixes up to `a872449`: masked subscription tokens in logs, simpler `enhanced-mode` rule,
  safer network-control state checks, simplified `net.inotify`
- Installer and action messages in English and Russian (system language, or `BOX_LANG=ru|en`)
- Installer keeps your `settings.ini` values (and `gid.list.cfg`) when there is no key input
- Wi‑Fi matching also by router MAC: `wifi_bssids_list`
- Hotspot client MAC filter: `mac_filter`, `mac_mode` (whitelist / blacklist), `macs_list`
- Boot start also waits for `sys.boot_completed` (devices without a boot animation)
- Updates from this repository (`updateJson` → `module/update.json`), so upstream releases do not replace these changes
- Subscription update keeps the previous config when the core rejects the new one
- Mobile data rules by SIM operator: `use_sim_matching`, `use_sim_list_mode`, `sim_operators_list`
- Installer tests the active config with an installed core
- `config_docs.yaml` comments in English

Build: `python module/build.py` → `module/box_for_root-<version>.zip`.
