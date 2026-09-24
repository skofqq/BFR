#!/system/bin/sh

# Definisi variabel
box_dir="/data/adb/box"
box_run="${box_dir}/run"
box_pid="${box_run}/box.pid"

# UI language for user-visible messages: Russian when the system language is Russian.
if [ -z "$BOX_LANG" ]; then
  sys_locale=$(getprop persist.sys.locale)
  [ -z "$sys_locale" ] && sys_locale=$(getprop ro.product.locale)
  case "$sys_locale" in
    ru*) BOX_LANG="ru" ;;
    *) BOX_LANG="en" ;;
  esac
fi

# t "English" "Русский"
t() {
  if [ "$BOX_LANG" = "ru" ]; then
    printf '%s' "$2"
  else
    printf '%s' "$1"
  fi
}

run_as_su() {
    su -c "$1"
}

stop_service() {
    echo "$(t 'Service is shutting down' 'Сервис останавливается')"
    run_as_su "${box_dir}/scripts/box.iptables disable"
    run_as_su "${box_dir}/scripts/box.service stop"
}

start_service() {
    echo "$(t 'Service is starting, please wait for a moment' 'Сервис запускается, подождите немного')"
    run_as_su "${box_dir}/scripts/box.service start"
    run_as_su "${box_dir}/scripts/box.iptables enable"
}

if [ -f "${box_pid}" ]; then
    PID=$(cat "${box_pid}")
    if [ -e "/proc/${PID}" ]; then
        stop_service
    else
        start_service
    fi
else
    start_service
fi