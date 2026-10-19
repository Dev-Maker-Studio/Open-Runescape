#!/usr/bin/env bash
# The restart loop every Darkan server runs under: check out the channel's ref (a release tag, or the
# branch head for a dev channel), download that build's jar, run it, repeat when it exits.
# Configuration comes from .env.unit next to the checkout (kind, channel, project, token, JVM args),
# or from the environment when there is none. Game configuration stays in .env. The server gets the
# checkout's commit so its watcher can tell a release that changes nothing for it from one that does.
set -u
cd "$(dirname "$0")/.." || exit 1
API="https://gitlab.com/api/v4"

configure() {
  if [ -f .env.unit ]; then set -a; . ./.env.unit; set +a; fi
  KIND="${DARKAN_KIND:?DARKAN_KIND must be lobby or world}"
  CHANNEL="${AUTO_UPDATE_CHANNEL:-release}"
  PROJECT_ID="${DARKAN_GITLAB_PROJECT_ID:-67255206}"
  TOKEN="${GITLAB_ACCESS_TOKEN:?GITLAB_ACCESS_TOKEN must be set}"
  ARTIFACT="$KIND-server"
  JAR="$ARTIFACT.jar"
  JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
  export AUTO_UPDATE="${AUTO_UPDATE:-true}" AUTO_UPDATE_CHANNEL="$CHANNEL" DARKAN_GITLAB_PROJECT_ID="$PROJECT_ID"
}

api() { curl -fsS --header "PRIVATE-TOKEN: $TOKEN" "$@"; }

checkout_release() {
  local release
  release=$(api "$API/projects/$PROJECT_ID/releases?order_by=released_at&sort=desc&per_page=1") || return 1
  VERSION=$(jq -r '.[0].tag_name // empty' <<<"$release")
  URL=$(jq -r ".[0].assets.links | map(select(.name | test(\"^$ARTIFACT-.*\\\\.jar\$\"))) | .[0].url // empty" <<<"$release")
  [ -n "$VERSION" ] || return 1
  git fetch -q --depth 1 origin "refs/tags/$VERSION:refs/tags/$VERSION" && git checkout -q --detach "$VERSION"
}

checkout_branch() {
  git fetch -q --depth 1 origin "$CHANNEL" && git checkout -q -B "$CHANNEL" FETCH_HEAD || return 1
  VERSION=$(api "$API/projects/$PROJECT_ID/packages/maven/org/darkan/$ARTIFACT/maven-metadata.xml" \
    | grep -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g' | grep -- "-$CHANNEL-" | sort -t- -k3,3n | tail -1)
  URL="$API/projects/$PROJECT_ID/packages/maven/org/darkan/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.jar"
}

while :; do
  configure
  VERSION=""; URL=""
  if [ "$CHANNEL" = release ]; then checkout_release; else checkout_branch; fi \
    || echo "Could not update the checkout for channel $CHANNEL; running it as is"
  git submodule update -q --init --depth 1 cache-source || echo "cache-source update failed; building the tree as is"

  RUNNING=$(cat .jar.version 2>/dev/null || true)
  if [ -n "$VERSION" ] && [ "$VERSION" != "$RUNNING" ] && api -L "$URL" -o "$JAR.new"; then
    mv "$JAR.new" "$JAR" && echo "$VERSION" > .jar.version && RUNNING="$VERSION"
    echo "Downloaded $ARTIFACT $VERSION"
  else
    rm -f "$JAR.new"
    echo "Keeping $JAR (${RUNNING:-unknown version})"
  fi

  BUILD_VERSION="$RUNNING" CHECKOUT_COMMIT="$(git rev-parse HEAD 2>/dev/null || true)" \
    "$JAVA" ${DARKAN_JAVA_VM_ARGS:-} -jar "$JAR" >> mainlog.txt 2>&1

  echo "Restarting in 5 seconds (Ctrl-C to stop)"
  sleep 5
done
