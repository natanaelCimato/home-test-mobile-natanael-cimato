FROM budtmo/docker-android:emulator_11.0

USER root

RUN python3 - <<'PY'
from pathlib import Path

path = Path("/home/androidusr/docker-android/cli/src/device/emulator.py")
source = path.read_text()
source = source.replace(
    'raise RuntimeError("/dev/kvm cannot be found!")',
    'self.logger.warning("/dev/kvm cannot be found; continuing with software acceleration")'
)
source = source.replace(
    '-gpu swiftshader_indirect -accel on -writable-system -verbose',
    '-gpu swiftshader_indirect -accel off -no-boot-anim -no-audio -writable-system -verbose'
)
path.write_text(source)
PY

USER androidusr
