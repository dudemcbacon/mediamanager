#!/bin/sh
# Optionally remap the unprivileged "app" user to a host-provided PUID/PGID, then
# drop privileges and run the app as that user.
#
# Set PUID and/or PGID (e.g. via the compose env_file / .env) to make the JVM run
# as a specific host uid/gid — handy for lining up ownership of any bind mounts.
# When neither is set, nothing is remapped and the app runs as the image's
# original "app" user (its build-time, system-assigned uid/gid) — identical to the
# previous `USER app` behavior, just reached via gosu.
#
# The container starts as root only long enough to adjust the user and fix file
# ownership; the JVM itself never runs as root.
set -eu

# Apply the timezone from $TZ (e.g. America/Los_Angeles). The JVM reads $TZ from
# the environment directly (gosu preserves it), so this is really about keeping
# OS-level timestamps and /etc/localtime in agreement. tzdata ships in the base
# image; an unknown zone is left as-is rather than creating a broken symlink.
if [ -n "${TZ:-}" ] && [ -f "/usr/share/zoneinfo/$TZ" ]; then
    ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime
    echo "$TZ" > /etc/timezone
fi

if [ -n "${PGID:-}" ]; then
    # -o allows reusing a gid that already exists in the image.
    groupmod -o -g "$PGID" app
fi
if [ -n "${PUID:-}" ]; then
    usermod -o -u "$PUID" app
fi

# Re-assert ownership so the (possibly relocated) uid/gid can read the jar and
# write to its home directory.
chown -R app:app /app /home/app

export HOME=/home/app
exec gosu app sh -c 'exec java $JAVA_OPTS -jar /app/app.jar'
