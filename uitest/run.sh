#!/bin/bash
# Drives the DS Admin app on an emulator and captures screenshots.
APK="$1"; OUT="$2"; PKG=com.digitalseba.admin
mkdir -p "$OUT"
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
ui() { python3 uitest/ui.py "$@"; }
back() { adb shell input keyevent KEYCODE_BACK; sleep 2; }

adb install -r "$APK" || exit 1
adb root; sleep 4
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0

# Prefill server so login goes to the mock API
adb shell am start -n $PKG/.MainActivity; sleep 5
adb shell am force-stop $PKG
cat > /tmp/ds_admin.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="server">http://10.0.2.2:8099</string>
    <boolean name="asked_notif" value="true" />
</map>
XML
U=$(adb shell stat -c %u /data/data/$PKG | tr -d '\r')
adb shell mkdir -p /data/data/$PKG/shared_prefs
adb push /tmp/ds_admin.xml /data/data/$PKG/shared_prefs/ds_admin.xml
adb shell chown $U:$U /data/data/$PKG/shared_prefs /data/data/$PKG/shared_prefs/ds_admin.xml
adb shell chmod 660 /data/data/$PKG/shared_prefs/ds_admin.xml

adb shell am start -n $PKG/.MainActivity
shot 01_login 6
ui edit 0 admin@test.com
ui edit 1 secret123
adb shell input keyevent KEYCODE_BACK; sleep 1
shot 02_login_filled 1
ui tap "লগইন"
shot 03_home 8
ui swipe_up; shot 04_home_2 2
ui swipe_up; shot 05_home_3 2
ui swipe_up; shot 06_home_4 2

ui tap_last "অর্ডার"; shot 07_orders 5
ui tap "Pending"; shot 08_orders_pending 4
ui tap "All"; sleep 3
ui tap "#2430"; shot 09_order_detail 5
ui swipe_up; shot 10_order_detail_2 2
ui swipe_up; shot 11_order_detail_3 2
ui tap "Status"; shot 12_status_sheet 3
back; back

ui tap_last "প্রোডাক্ট"; shot 13_products 5
ui swipe_up; shot 14_products_2 2
ui tap "Edit stock"; shot 15_stock_dialog 3
ui tap "Cancel"; sleep 1

ui tap_last "কাস্টমার"; shot 16_customers 5
ui tap "Rahim Uddin"; shot 17_customer_orders 5
back

ui tap_last "আরও"; shot 18_more 4
ui tap "Reports"; shot 19_reports 5
ui swipe_up; shot 20_reports_2 2
ui swipe_up; shot 21_reports_3 2
back
ui tap "WhatsApp Center"; shot 22_whatsapp 5
back
ui tap "Notifications"; shot 23_notifications 5
back
ui tap "Sync Logs"; shot 24_synclogs 5
back
ui tap "Store Health"; shot 25_health 5
back
ui tap "Settings"; shot 26_settings 5
ui swipe_up; shot 27_settings_2 2
ui swipe_up; shot 28_settings_3 2
ui swipe_up; shot 29_settings_4 2
back
ui tap "Connect Store"; shot 30_connect 5
back
ui tap "Search"; sleep 3
adb shell input text cap; shot 31_search 5
back; back

# Realtime: a new order arrives
ui tap_last "হোম"; sleep 3
touch /tmp/ds_new_order
shot 32_new_order_banner 6
adb shell cmd statusbar expand-notifications
shot 33_notification_shade 3
adb shell cmd statusbar collapse

# Dark mode
adb shell cmd uimode night yes
shot 34_dark_home 5
ui tap_last "অর্ডার"; shot 35_dark_orders 5
ui tap "#2430"; shot 36_dark_detail 5

adb logcat -d -b crash > "$OUT/crash.txt" 2>&1
adb logcat -d | grep -E "AndroidRuntime|FATAL|com.digitalseba" | tail -300 > "$OUT/logcat.txt" 2>&1
cp /tmp/mock_requests.log "$OUT/requests.log" 2>/dev/null || true
echo done
