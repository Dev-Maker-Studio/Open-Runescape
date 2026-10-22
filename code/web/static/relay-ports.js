const DEFAULT_LOBBY_PORT = 8110;
const DEFAULT_JS5_PORT = 8111;

function port(params, name, fallback) {
  const raw = params.get(name);
  if (raw === null) return fallback;
  if (!/^[1-9][0-9]{3,4}$/.test(raw) || Number(raw) > 65535) throw new Error(`${name} must be a port from 1000 to 65535`);
  return Number(raw);
}

export function relayUrls(search, hostname) {
  const params = new URLSearchParams(search);
  const lobbyPort = port(params, "lobbyPort", DEFAULT_LOBBY_PORT);
  const js5Port = port(params, "js5Port", DEFAULT_JS5_PORT);
  const host = hostname.includes(":") ? `[${hostname}]` : hostname;
  return {
    lobby: `ws://${host}:${lobbyPort}/lobby`,
    world: `ws://${host}:${lobbyPort}/world`,
    js5: `ws://${host}:${js5Port}/js5`,
    accountJs5: `ws://${host}:${lobbyPort}/js5`,
  };
}
