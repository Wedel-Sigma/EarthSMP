"use strict";

// All API and squaremap tile requests stay on the same origin. Private data never enters squaremap JSON.
const el = id => document.getElementById(id);
const status = message => { el("map-status").textContent = message; };
const say = message => { el("message").textContent = message; };
const layers = { countries: null, cores: null, players: null };
let map, tileLayer, worldName, scale, loggedIn = false, ready = false;
const playerMarkers = new Map();

async function json(url, options = {}) {
  const res = await fetch(url, { ...options, cache: "no-store", credentials: "same-origin" });
  const value = await res.json().catch(() => ({}));
  if (!res.ok) throw Object.assign(new Error(value.error || `HTTP ${res.status}`), { status: res.status });
  return value;
}
function toLatLng(x, z) { return L.latLng(-z * scale, x * scale); }
function safeElement(text) { const node = document.createElement("span"); node.textContent = text || ""; return node; }

function toggleLayer(name, checkbox) {
  if (!map || !layers[name]) return;
  if (checkbox.checked) layers[name].addTo(map);
  else layers[name].removeFrom(map);
}
function coordinates(points) {
  if (Array.isArray(points)) return points.map(coordinates);
  return toLatLng(points.x, points.z);
}
function addPublicMarker(target, marker) {
  let shape = null;
  const options = {
    color: marker.color || marker.strokeColor || "#c6d0dd",
    weight: marker.weight ?? marker.strokeWeight ?? 2,
    opacity: marker.opacity ?? marker.strokeOpacity ?? 0.9,
    fillColor: marker.fillColor || marker.color || "#6b95b1",
    fillOpacity: marker.fillOpacity ?? 0.25,
    stroke: marker.stroke ?? true,
    fill: marker.fill ?? true,
    interactive: true,
  };
  try {
    switch (marker.type) {
      case "rectangle": shape = L.rectangle(coordinates(marker.points), options); break;
      case "polygon": shape = L.polygon(coordinates(marker.points), options); break;
      case "polyline": shape = L.polyline(coordinates(marker.points), options); break;
      case "circle": shape = L.circle(toLatLng(marker.center.x, marker.center.z), { ...options, radius: marker.radius * scale }); break;
      case "icon": {
        const key = marker.icon;
        if (!/^[a-zA-Z0-9._-]+$/.test(key)) return;
        const size = marker.size || {x:30,z:30};
        const anchor = marker.anchor || {x:size.x/2,z:size.z/2};
        shape = L.marker(toLatLng(marker.point.x, marker.point.z), {
          icon: L.icon({iconUrl:`/map-data/images/icon/registered/${key}.png`,
            iconSize:[size.x,size.z], iconAnchor:[anchor.x,anchor.z]})
        });
        break;
      }
      default: return;
    }
    if (marker.tooltip) shape.bindTooltip(safeElement(marker.tooltip));
    else if (marker.popup) shape.bindTooltip(safeElement(marker.popup));
    shape.addTo(target);
  } catch (error) { console.warn("EarthSMP marker skipped",error); }
}

async function updatePublic() {
  if (!ready) return;
  try {
    const response = await json(`/map-data/tiles/${worldName}/markers.json`);
    const nextCountries = L.layerGroup(), nextCores = L.layerGroup();
    for (const layer of Array.isArray(response) ? response : []) {
      if (layer.id !== "earthsmp-political-territories" && layer.id !== "earthsmp-protection-cores") continue;
      const target = layer.id === "earthsmp-protection-cores" ? nextCores : nextCountries;
      for (const marker of Object.values(layer.markers || {})) addPublicMarker(target, marker);
    }
    for (const [name, candidate] of [["countries", nextCountries], ["cores", nextCores]]) {
      if (layers[name]) layers[name].removeFrom(map);
      layers[name] = candidate;
      toggleLayer(name, el(name === "countries" ? "show-countries" : "show-cores"));
    }
  } catch (error) { status("Mapowe znaczniki: oczekiwanie na squaremap…"); }
}

async function initMap() {
  if (typeof L === "undefined") { status("Nie udało się pobrać Leaflet. Sprawdź połączenie z Internetem."); return; }
  try {
    const settings = await json("/map-data/tiles/settings.json");
    const world = settings.worlds?.find(w => w.type === "normal") || settings.worlds?.[0];
    if (!world || !/^[a-zA-Z0-9_-]+$/.test(world.name)) throw new Error("Nie znaleziono świata squaremap");
    worldName = world.name;
    const details = await json(`/map-data/tiles/${worldName}/settings.json`);
    scale = 1 / Math.pow(2, details.zoom.max);
    map = L.map("map", { crs: L.CRS.Simple, center:toLatLng(details.spawn.x,details.spawn.z),
      zoom:details.zoom.def, minZoom:0, maxZoom:details.zoom.max + (details.zoom.extra || 0), preferCanvas:true });
    tileLayer = L.tileLayer(`/map-data/tiles/${worldName}/{z}/{x}_{y}.png`, {
      tileSize:512, minNativeZoom:0,maxNativeZoom:details.zoom.max,
      maxZoom:details.zoom.max+(details.zoom.extra||0),errorTileUrl:"",noWrap:true
    }).addTo(map);
    layers.players = L.layerGroup();
    toggleLayer("players",el("show-players"));
    ready = true;
    status(`Mapa: ${world.display_name || world.name}`);
    await updatePublic();
    setInterval(updatePublic,8000);
    setInterval(() => { if (tileLayer && document.visibilityState === "visible") tileLayer.redraw(); }, 120000);
  } catch (error) { status(`Mapa niedostępna: ${error.message}. Sprawdź port squaremap.`); }
}

function clearPlayers() {
  for (const marker of playerMarkers.values()) marker.remove();
  playerMarkers.clear();
  el("member-list").replaceChildren();
}
function updatePlayerMarkers(value) {
  if (!ready) return;
  const list = el("member-list");
  list.replaceChildren();
  const seen = new Set();
  for (const player of value.players || []) {
    seen.add(player.uuid);
    const point = toLatLng(player.x,player.z);
    let marker = playerMarkers.get(player.uuid);
    if (!marker) {
      marker = L.circleMarker(point,{radius:9,color:"#091c30",weight:2,fillColor:"#6bdbff",fillOpacity:.95})
        .bindTooltip(safeElement(player.name),{permanent:true,direction:"top",offset:[0,-12]});
      marker.addTo(layers.players);
      playerMarkers.set(player.uuid,marker);
    } else marker.setLatLng(point);
    const row = document.createElement("div"); row.className="member";
    const info = document.createElement("div");
    info.append(safeElement(player.name),document.createElement("br"));
    const coords = document.createElement("small"); coords.textContent = `${player.x}, ${player.y}, ${player.z}`;
    info.append(coords);
    const go = document.createElement("button"); go.type="button"; go.textContent="Pokaż";
    go.addEventListener("click",()=>map.panTo(toLatLng(player.x,player.z)));
    row.append(info,go); list.append(row);
  }
  for(const [id,marker] of playerMarkers) if (!seen.has(id)) { marker.remove(); playerMarkers.delete(id); }
  el("account-name").textContent = value.viewer.name;
  el("nation-name").textContent = `Państwo: ${value.viewer.nation || "Brak"} · widoczni: ${(value.players || []).length}`;
}
function setLoggedIn(value) {
  loggedIn=value;
  el("login-section").hidden=value;
  el("account-section").hidden=!value;
  if (!value) clearPlayers();
}
async function updatePrivate() {
  if (!loggedIn || !ready) return;
  try {
    const value = await json("/api/positions");
    updatePlayerMarkers(value);
    say("");
  } catch (error) {
    clearPlayers();
    if (error.status === 401) { setLoggedIn(false); say("Sesja wygasła. Zaloguj się hasłem; Minecraft nie jest potrzebny."); }
    else say(error.message);
  }
}
async function login(event) {
  event.preventDefault();
  const username = el("login-username").value.trim();
  const password = el("login-password").value;
  try {
    await json("/api/login",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:new URLSearchParams({username,password}).toString()});
    el("login-password").value="";
    setLoggedIn(true);
    await updatePrivate();
  } catch (error) { say(error.message); }
}
async function linkAccount(event) {
  event.preventDefault();
  const code = el("link-code").value.trim().toUpperCase();
  const password = el("link-password").value;
  if (password !== el("link-confirm").value) { say("Hasła muszą być identyczne."); return; }
  if (password.length < 12 || password.length > 128) { say("Hasło musi mieć 12–128 znaków."); return; }
  try {
    await json("/api/link",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},
      body:new URLSearchParams({code,password}).toString()});
    el("link-code").value="";
    el("link-password").value="";
    el("link-confirm").value="";
    setLoggedIn(true);
    await updatePrivate();
    say("Konto połączone. Od teraz logujesz się nickiem i hasłem, także bez Minecrafta.");
  } catch (error) { say(error.message); }
}
function showAuthTab(tab) {
  el("login-form").hidden=tab!=="login";
  el("link-form").hidden=tab!=="link";
  el("tab-login").className=tab==="login"?"active":"subtle";
  el("tab-link").className=tab==="link"?"active":"subtle";
  say("");
}
async function logout() {
  try { await json("/api/logout",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:"logout=1"}); }
  catch (error) { say("Wylogowanie po stronie serwera nie powiodło się. Spróbuj ponownie."); return; }
  setLoggedIn(false); say("Wylogowano. ");
}

async function init() {
  for (const [name,id] of [["countries","show-countries"],["cores","show-cores"],["players","show-players"]]) {
    el(id).addEventListener("change",()=>toggleLayer(name,el(id)));
  }
  el("login-form").addEventListener("submit",login);
  el("link-form").addEventListener("submit",linkAccount);
  el("tab-login").addEventListener("click",()=>showAuthTab("login"));
  el("tab-link").addEventListener("click",()=>showAuthTab("link"));
  el("logout").addEventListener("click",logout);
  el("refresh-tiles").addEventListener("click", () => {
    if (tileLayer) { tileLayer.redraw(); status(`Mapa: ${worldName} · kafelki odświeżone`); }
  });
  await initMap();
  try { await json("/api/me"); setLoggedIn(true); } catch { setLoggedIn(false); }
  await updatePrivate();
  setInterval(updatePrivate,1500);
}
init();
