## Run your own lobby and world (quick start)

Follow these steps in order. Do not skip any.

### 1. Install three things

- **Git**: <https://git-scm.com/downloads>
- **Java 26 (JDK)**: <https://jdk.java.net/26/> or <https://adoptium.net>. Check it worked: `java -version` prints `26`.
- **MongoDB** (the database). Pick ONE:
  - Docker installed? Run `docker run -d --name darkan-mongo -p 27017:27017 mongo:7` and you are done.
  - No Docker? Install MongoDB Community Server from <https://www.mongodb.com/try/download/community> with the default settings.

### 2. Download the server

Open a terminal (Windows: PowerShell) and run:

```bash
git clone --recurse-submodules https://gitlab.com/darkanrs/server.git
cd server
```

`--recurse-submodules` is required. Without it there is no game cache and nothing works.

### 3. Create the settings file

In the `server` folder, create a file named exactly `.env` (with the dot, no `.txt` on the end) containing:

```env
DEBUG=true
SOCIAL_GATEWAY_URL=ws://localhost:4040/social/ws
```

That is the whole file. It tells the world to join your own lobby instead of the public dev lobby.

### 4. Start the lobby

In the terminal, inside the `server` folder:

```bash
./gradlew :lobby:run
```

On Windows use `.\gradlew.bat :lobby:run` instead.

The first start downloads dependencies and builds the cache, so it takes a few minutes. Wait until you see
`All services started`. **Leave this terminal open.** Closing it stops the lobby.

### 5. Start the world

Open a **second** terminal, go into the same `server` folder and run:

```bash
./gradlew :world:run
```

On Windows use `.\gradlew.bat :world:run` instead.

Wait until you see `World server started`. **Leave this terminal open too.**

### 6. Play

1. Download the official Darkan launcher for your system from
   <https://gitlab.com/darkanrs/client-loader/-/releases> and open it.
2. In the **Lobby** dropdown pick **Custom lobby…**.
3. Type `localhost` in the address box and leave the port at `43594`.
4. Click **Play**.
5. When the game opens, create a new account (accounts on your lobby are separate from the real Darkan ones),
   log in, and pick **world 100** from the world list.

To play from another computer, type the server computer's IP address instead of `localhost` in step 3, and
add `WORLD_PUBLIC_HOST=<that IP>` to `.env` so the world list points other computers at it. To stop
everything, press `Ctrl+C` in both terminals.

### Something went wrong?

- **Gradle says it cannot find a Java installation matching version 26**: install Java 26 and open a new terminal.
- **The lobby cannot connect to Mongo**: MongoDB is not running. Start the Docker container (`docker start darkan-mongo`) or the MongoDB service.
- **World 100 is missing from the world list**: the world is not running, or `.env` is missing the two lines from step 3. Check the world terminal for errors.
- **Cache errors or "Darkan has been updated"**: the submodule is missing or stale. Run `git submodule update --init`, then restart the lobby and then the world.

## Running

### Lobby (includes SocialGateway + HTTP API + lobby login server)

```bash
./gradlew :lobby:run
```

### World

```bash
./gradlew :world:run
```

### Smoke test

`./smoke-test.sh` builds both servers, boots them against the database in `.env`, waits for the
world to register with the lobby gateway and checks `/health`. See the script header for running
it alongside the legacy servers on other ports.

### Migrating a legacy database

See `docs/migration.md` (`migration/` module: `inspect`, `migrate --dry-run`, `migrate`, `verify`).

## SocialGateway (world <-> lobby)

The lobby HTTP server exposes a persistent WebSocket endpoint for world-servers:

- `ws://<lobby-host>:<LOBBY_API_PORT>/social/ws`

World-servers connect and authenticate with `SOCIAL_GATEWAY_TOKEN`, then send presence + social events.
With no configuration a world joins the dev lobby at `dev.darkan.org` as a community world; see
`docs/community-worlds.md` to run your own.

### Environment variables

```bash
# Existing
MONGO_URI=mongodb://localhost:27017/darkan-server?retryWrites=true&w=majority
LOBBY_API_PORT=4040
API_KEY=TEST_API_KEY
SERVER_NAME=Darkan

# World <-> lobby (defaults shown)
SOCIAL_GATEWAY_URL=ws://dev.darkan.org:4040/social/ws
SOCIAL_GATEWAY_TOKEN=darkan-community-world
WORLD_ID=100
```

### Admin announcements API

- `POST /admin/announcements`
- Auth: `Authorization: Bearer <API_KEY>` or `X-API-Key: <API_KEY>`
- Body: JSON `AnnouncementBroadcast` (`type` discriminator required)

See `docs/social-gateway.md` for routing behavior and rollout steps.