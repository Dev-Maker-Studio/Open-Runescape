import "./bridge.js";

const start = document.querySelector("#start");
const close = document.querySelector("#close");
const status = document.querySelector("#status");
let started = false;

async function run() {
  if (started) return;
  started = true;
  start.disabled = true;
  status.textContent = "Checking original lobby and world protocol. No rendered scene or gameplay is implied.";
  const result = await window.openrs.run("standalone-login");
  status.textContent = JSON.stringify(result, null, 2);
}

start.addEventListener("click", run);
close.addEventListener("click", async () => {
  const result = await window.openrs.run("standalone-session-close");
  status.textContent = JSON.stringify(result, null, 2);
});
setInterval(async () => {
  const result = await window.openrs.run("standalone-session-state");
  if (result.data) status.textContent = JSON.stringify(result, null, 2);
}, 500);
if (new URLSearchParams(location.search).get("autologin") === "1") run();
