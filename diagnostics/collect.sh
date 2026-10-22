#!/system/bin/sh
# Run as root; writes a compact diagnostic report to /sdcard/Download/hyperos-audio-diagnostic.txt
OUT=/sdcard/Download/hyperos-audio-diagnostic.txt
{
  echo '=== device ==='
  getprop ro.product.model
  getprop ro.build.version.release
  getprop ro.build.version.sdk
  getprop ro.build.version.incremental
  echo '=== sound settings ==='
  settings get global lock_sound
  settings get global unlock_sound
  echo '=== UI audio files ==='
  ls -lZ /product/media/audio/ui/Lock.ogg /product/media/audio/ui/Unlock.ogg 2>&1
  sha256sum /product/media/audio/ui/Lock.ogg /product/media/audio/ui/Unlock.ogg 2>&1
  echo '=== mount records ==='
  grep -E '/product/media/audio/ui/(Lock|Unlock)\.ogg' /proc/mounts 2>&1
  echo '=== module logs ==='
  for f in /data/adb/modules/Hyperos_audio_core/post-fs-data.log /data/adb/modules/Hyperos_audio_core/service.log; do
    [ -f "$f" ] && { echo "--- $f"; grep -E 'Lock\.ogg|Unlock\.ogg|UI=|跳过|挂载成功|挂载失败|selfmount' "$f" | tail -n 100; }
  done
  echo '=== matching logcat ==='
  logcat -d -t 3000 | grep -iE 'HyperOSAudioUnlock|SoundPool|MediaPlayer|Lock\.ogg|Unlock\.ogg|soundpool' | tail -n 300
} > "$OUT" 2>&1
chmod 0644 "$OUT"
echo "Wrote $OUT"
