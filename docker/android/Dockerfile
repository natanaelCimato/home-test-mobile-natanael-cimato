FROM budtmo/docker-android:emulator_11.0

ARG SOFTWARE_ACCELERATION=true

USER root

RUN cat > /tmp/patch-emulator.py <<'PY'
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

RUN if [ "${SOFTWARE_ACCELERATION}" = "true" ]; then \
      python3 /tmp/patch-emulator.py; \
    else \
      echo "Keeping upstream KVM/hardware acceleration checks enabled"; \
    fi && rm /tmp/patch-emulator.py

USER androidusr
