#!/usr/bin/env bash
#
# deploy.sh — commit & push local changes, sync the local .env onto the remote host, and pull on the remote.
#
# Steps:
#   1. git pull (current branch)
#   2. git commit — prompts whether to include untracked files and for a commit message (skipped if clean)
#   3. git push
#   4. Sync .env onto the remote host over ssh:
#        - a var in local but not remote  -> added to the remote file (with the local value)
#        - a var in both with same value   -> left as-is
#        - a var in both with diff values  -> remote value KEPT, a warning is printed (unless whitelisted below)
#        - a var in remote but not local   -> removed from the remote file
#   5. git pull in the remote repo
#
# ssh auth is via keys; the remote host is assumed already authorized.

set -euo pipefail

# --- config ---------------------------------------------------------------------------------------

REMOTE_HOST="butt.report"
REMOTE_PORT="1022"
REMOTE_USER="bburnett"
REMOTE_ENV="/home/bburnett/config/mediamanager/.env"
REMOTE_REPO="/home/bburnett/config/mediamanager/mediamanager"
# Private key on the remote host used by the remote `git pull` to authenticate to the git remote.
REMOTE_GIT_SSH_KEY="/home/bburnett/.ssh/id_ed25519"

# Env vars exempt from the "value differs between local and remote" warning (they're expected to differ).
WHITELIST=(
    NEW_RELIC_APP_NAME
    PLEX_CACHE_DIR
    LOCAL_FILE_SYSTEM_PREFIX
)

# --- setup ----------------------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_ENV="$SCRIPT_DIR/.env"
cd "$SCRIPT_DIR" # run git against this repo regardless of the caller's working directory

if [[ ! -f "$LOCAL_ENV" ]]; then
    echo "ERROR: local env file not found at $LOCAL_ENV" >&2
    exit 1
fi

remote() { ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" "$@"; }

TMPDIR_DEPLOY="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_DEPLOY"' EXIT
REMOTE_TMP="$TMPDIR_DEPLOY/remote.env"
MERGED_TMP="$TMPDIR_DEPLOY/merged.env"

# --- 1. pull --------------------------------------------------------------------------------------

echo "==> git pull"
git pull

# --- 2. commit ------------------------------------------------------------------------------------

if [[ -n "$(git status --porcelain)" ]]; then
    read -r -p "Add untracked files to the commit? [y/N] " include_untracked
    if [[ "$include_untracked" =~ ^[Yy]$ ]]; then
        git add -A
    else
        git add -u
    fi

    if git diff --cached --quiet; then
        echo "==> nothing staged to commit"
    else
        commit_msg=""
        while [[ -z "$commit_msg" ]]; do
            read -r -p "Commit message: " commit_msg
        done
        echo "==> git commit"
        git commit -m "$commit_msg"
    fi
else
    echo "==> working tree clean; nothing to commit"
fi

# --- 3. push --------------------------------------------------------------------------------------

echo "==> git push"
git push

# --- 4. sync .env onto the remote -----------------------------------------------------------------

echo "==> syncing $LOCAL_ENV -> $REMOTE_USER@$REMOTE_HOST:$REMOTE_ENV"
if ! remote "cat '$REMOTE_ENV'" >"$REMOTE_TMP" 2>/dev/null; then
    echo "ERROR: could not read remote env file $REMOTE_ENV (does it exist?)" >&2
    exit 1
fi

# Merge: stdout = the new remote file (remote structure preserved), stderr = warnings/notes.
# The remote file is rebuilt from itself so its existing values, ordering, and comments are kept; only
# keys are added/removed to match the local keyset.
awk -v whitelist="${WHITELIST[*]}" '
    function keyof(line,   k) {
        if (line ~ /^[ \t]*#/) return ""        # comment
        if (line !~ /=/)       return ""        # not a KEY=VALUE line
        k = line; sub(/=.*/, "", k); gsub(/^[ \t]+|[ \t]+$/, "", k)
        return (k ~ /^[A-Za-z_][A-Za-z0-9_]*$/) ? k : ""
    }
    function valof(line,   v) { v = line; sub(/^[^=]*=/, "", v); return v }
    BEGIN { n = split(whitelist, w, " "); for (i = 1; i <= n; i++) white[w[i]] = 1 }
    # First file: the local env.
    FNR == NR {
        k = keyof($0)
        if (k != "") { lval[k] = valof($0); if (!(k in lseen)) { lseen[k] = 1; lorder[++ln] = k } }
        next
    }
    # Second file: the current remote env.
    {
        k = keyof($0)
        if (k == "") { print; next }            # preserve comments / blank lines
        rseen[k] = 1
        if (k in lseen) {
            print                                # keep the remote line (preserves the remote value)
            if (valof($0) != lval[k] && !(k in white))
                print "  WARNING: " k " differs between local and remote (kept remote value)" > "/dev/stderr"
        } else {
            print "  removed " k " (not in local)" > "/dev/stderr"
        }
    }
    END {
        for (i = 1; i <= ln; i++) {
            k = lorder[i]
            if (!(k in rseen)) { print k "=" lval[k]; print "  added " k > "/dev/stderr" }
        }
    }
' "$LOCAL_ENV" "$REMOTE_TMP" >"$MERGED_TMP"

# Write back atomically (temp + mv on the remote) so a dropped connection can't leave a truncated file.
remote "cat > '$REMOTE_ENV.deploy-tmp' && mv '$REMOTE_ENV.deploy-tmp' '$REMOTE_ENV'" <"$MERGED_TMP"
echo "==> remote .env updated"

# --- 5. pull on the remote ------------------------------------------------------------------------

echo "==> git pull on remote ($REMOTE_REPO)"
remote "cd '$REMOTE_REPO' && GIT_SSH_COMMAND='ssh -i $REMOTE_GIT_SSH_KEY' git pull"

echo "==> done"
