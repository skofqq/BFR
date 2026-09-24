#!/system/bin/sh

(
    # Wait for boot to finish. Devices started without a boot animation (e.g. emulators
    # with -no-boot-anim) never report bootanim=stopped, so boot_completed counts too.
    until [ "$(getprop init.svc.bootanim)" = "stopped" ] || [ "$(getprop sys.boot_completed)" = "1" ]; do
        sleep 10
    done

    if [ -f "/data/adb/box/scripts/start.sh" ]; then
        chmod -R 755 /data/adb/box/scripts/
        /data/adb/box/scripts/start.sh >/dev/null 2>&1
    else
        echo "File /data/adb/box/scripts/start.sh not found" > "/data/adb/box/run/box_service.log"
    fi
) &
