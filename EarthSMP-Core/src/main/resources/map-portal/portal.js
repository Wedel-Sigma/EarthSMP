"use strict";

// All API and squaremap tile requests stay on the same origin. Private data never enters squaremap JSON.
const el = id => document.getElementById(id);
const status = message => { el("map-status").textContent = message; };
const say = message => { el("message").textContent = message; };
const layers = { countries: null, cores: null, players: null, worldborder: null };
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

// WorldBorder is a public server-side snapshot (not a hardcoded rectangle or squaremap marker).
async function updateWorldBorder() {
  if (!ready) return;
  try {
    const border = await json("/api/worldborder");
    const {centerX:x, centerZ:z, size} = border;
    if (![x,z,size].every(Number.isFinite) || size <= 0) throw new Error("Nieprawidłowa granica świata");
    const r = size / 2;
    const layer = L.rectangle([toLatLng(x-r,z+r),toLatLng(x+r,z-r)], {
      color:"#ef4444",weight:3,opacity:.95,fill:false,dashArray:"10 6",interactive:false
    });
    if (layers.worldborder) layers.worldborder.removeFrom(map);
    layers.worldborder=layer;
    toggleLayer("worldborder",el("show-worldborder"));
  } catch (error) { console.warn("WorldBorder unavailable",error); }
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

    // Publiczny znacznik spawnu w Makau.
    const spawnIcon = L.divIcon({
      className: "",
      iconSize: [32, 32],
      iconAnchor: [16, 16],
      html: '<span style="font-size:28px;line-height:32px;">🏠</span>'
    });

    L.marker(toLatLng(details.spawn.x, details.spawn.z), {
      icon: spawnIcon,
      interactive: false
    }).addTo(map);

    layers.players = L.layerGroup();
    toggleLayer("players",el("show-players"));
    ready = true;
    status(`Mapa: ${world.display_name || world.name}`);
    await Promise.allSettled([updatePublic(), updateWorldBorder()]);
    setInterval(updatePublic,8000);
    setInterval(updateWorldBorder,8000);
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
  if (!value) {
    clearPlayers();
    el("territory-section").hidden=true;
    if(territory.editing) editTerritory();
  }
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
    await updateTerritoryOverview();
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
    await updateTerritoryOverview();
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


// Political chunk editor: the display changes only after an administrator approves a request.
const territory = {
  editing:false, panning:false, brush:"add", painting:false, paintBefore:null, lastPaint:null,
  selected:new Map(), history:[], maxZoom:0, grid:null, selectionLayer:null, detailLayer:null,
};
const territoryInfo = message => { el("territory-status").textContent = message; };
const chunkKey = (x,z) => `${x},${z}`;
const chunkFromPoint = latlng => ({
  x:Math.floor(latlng.lng/(scale*16)),
  z:Math.floor(-latlng.lat/(scale*16)),
});
const chunkBounds = (x,z) => [toLatLng(x*16,(z+1)*16),toLatLng((x+1)*16,z*16)];
const selectedString = () => [...territory.selected.values()].map(p=>`${p.x},${p.z}`).join(";");
const territoryPost = (path, fields) => json(path,{
  method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"},
  body:new URLSearchParams(fields).toString()
});

function initTerritoryMap() {
  if (!map) return;
  const pane = map.createPane("esmp-grid-pane");
  pane.style.zIndex = "440";
  pane.style.pointerEvents = "none";
  const selectionPane = map.createPane("esmp-selection-pane");
  selectionPane.style.zIndex = "460";
  const Grid = L.GridLayer.extend({
    createTile(tileCoords) {
      const canvas=document.createElement("canvas");
      canvas.width=256; canvas.height=256;
      const pixelChunk=16*Math.pow(2,tileCoords.z-territory.maxZoom);
      if (pixelChunk<5) return canvas;
      const ctx=canvas.getContext("2d");
      ctx.strokeStyle="rgba(255,255,255,.42)";
      ctx.lineWidth=1;
      ctx.beginPath();
      const startX=Math.ceil(tileCoords.x*256/pixelChunk)*pixelChunk-tileCoords.x*256;
      const startY=Math.ceil(tileCoords.y*256/pixelChunk)*pixelChunk-tileCoords.y*256;
      for(let x=startX;x<=256;x+=pixelChunk){ctx.moveTo(x+.5,0);ctx.lineTo(x+.5,256);}
      for(let y=startY;y<=256;y+=pixelChunk){ctx.moveTo(0,y+.5);ctx.lineTo(256,y+.5);}
      ctx.stroke();
      return canvas;
    }
  });
  territory.grid=new Grid({tileSize:256,pane:"esmp-grid-pane",opacity:.75,minZoom:0});
  territory.selectionLayer=L.layerGroup();
  territory.detailLayer=L.layerGroup().addTo(map);
  const draw=e=>{
    if (!territory.editing || territory.panning) return;
    const point=chunkFromPoint(e.latlng);
    if(territory.painting && territory.lastPaint){
      // Interpolate skipped mousemove events so fast brush strokes do not leave gaps.
      const from=territory.lastPaint;
      const steps=Math.min(2048,Math.max(Math.abs(point.x-from.x),Math.abs(point.z-from.z)));
      for(let i=1;i<=steps;i++) {
        markChunk(Math.round(from.x+(point.x-from.x)*i/steps),
          Math.round(from.z+(point.z-from.z)*i/steps),territory.painting);
      }
      territory.lastPaint=point;
      return true;
    }
    if(territory.painting) territory.lastPaint=point;
    return markChunk(point.x,point.z,territory.painting || territory.brush);
  };
  map.on("mousedown",e=>{
    if (!territory.editing || territory.panning) return;
    const button=e.originalEvent?.button;
    if(button!==0 && button!==2) return;
    territory.paintBefore=new Map(territory.selected);
    territory.lastPaint=null;
    territory.painting=button===2 ? "remove" : territory.brush;
    draw(e);
  });
  map.on("mousemove",e=>{if(territory.painting) draw(e);});
  const finish=()=>{
    if(!territory.painting) return;
    if (territory.paintBefore && !equalSelection(territory.paintBefore,territory.selected)) saveUndo(territory.paintBefore);
    territory.painting=false; territory.paintBefore=null; territory.lastPaint=null;
  };
  map.on("mouseup",finish);
  document.addEventListener("mouseup",finish);
  // Desktop-only: middle mouse button pans without switching away from the LMB brush.
  // Capture before Leaflet sees MMB; keep touch devices' existing brush controls.
  const finePointer = () => window.matchMedia("(hover: hover) and (pointer: fine)").matches;
  let middlePan = null;
  const mapElement = map.getContainer();
  mapElement.addEventListener("mousedown", event => {
    if (!territory.editing || !finePointer() || event.button !== 1) return;
    event.preventDefault(); // Stop the browser's middle-click auto-scroll.
    event.stopPropagation();
    finish();
    middlePan = {x: event.clientX, y: event.clientY};
    mapElement.classList.add("esmp-middle-pan");
  }, true);
  document.addEventListener("mousemove", event => {
    if (middlePan === null) return;
    if ((event.buttons & 4) === 0) { // MMB released outside the map.
      middlePan = null;
      mapElement.classList.remove("esmp-middle-pan");
      return;
    }
    event.preventDefault();
    const dx = event.clientX - middlePan.x;
    const dy = event.clientY - middlePan.y;
    middlePan = {x: event.clientX, y: event.clientY};
    if (dx || dy) map.panBy([-dx, -dy], {animate: false});
  }, true);
  document.addEventListener("mouseup", event => {
    if (event.button === 1 && middlePan !== null) {
      middlePan = null;
      mapElement.classList.remove("esmp-middle-pan");
    }
  }, true);
  window.addEventListener("blur", () => {
    middlePan = null;
    mapElement.classList.remove("esmp-middle-pan");
  });
  map.on("click",e=>{
    // Touch screens generate click, but not always mousedown/mousemove.
    if(!territory.editing || territory.panning || territory.painting) return;
    const before=new Map(territory.selected);
    if(draw(e) && !equalSelection(before,territory.selected)) saveUndo(before);
  });
  map.on("contextmenu",e=>{
    if (!territory.editing || territory.panning) return;
    L.DomEvent.preventDefault(e.originalEvent);
    const before=new Map(territory.selected);
    const p=chunkFromPoint(e.latlng);
    if(markChunk(p.x,p.z,"remove") && !equalSelection(before,territory.selected)) saveUndo(before);
  });
}
function equalSelection(a,b){
  if(a.size!==b.size) return false;
  for(const k of a.keys()) if(!b.has(k)) return false;
  return true;
}
function saveUndo(before){
  territory.history.push(before);
  if(territory.history.length>30) territory.history.shift();
}
function selectionStyle(kind="selected"){
  const palette={selected:"#40d8bb",NEUTRAL:"#40d8bb",OWN:"#f2bf62",FOREIGN:"#f07070"};
  return {pane:"esmp-selection-pane",color:palette[kind]||palette.selected,
    weight:1,opacity:.95,fillColor:palette[kind]||palette.selected,fillOpacity:.42,interactive:false};
}
function markChunk(x,z,action){
  const key=chunkKey(x,z);
  if(action==="remove") {
    const item=territory.selected.get(key);
    if(!item) return false;
    territory.selectionLayer.removeLayer(item.layer);
    territory.selected.delete(key);
  }else{
    if(territory.selected.has(key)) return false;
    if(territory.selected.size>=1024){territoryInfo("Limit 1024 chunków w jednym wniosku.");return false;}
    const layer=L.rectangle(chunkBounds(x,z),selectionStyle()).addTo(territory.selectionLayer);
    territory.selected.set(key,{x,z,layer});
  }
  el("territory-count").textContent=`${territory.selected.size} / 1024 chunków`;
  return true;
}
function restoreSelection(state){
  territory.selectionLayer.clearLayers();
  territory.selected.clear();
  for(const p of state.values()) markChunk(p.x,p.z,"add");
}
function chooseBrush(kind){
  territory.brush=kind;
  // On desktop LMB paints, RMB erases and MMB always pans. Phones retain the buttons.
  territory.panning=kind==="pan" && !window.matchMedia("(hover: hover) and (pointer: fine)").matches;
  if(territory.editing) {
    if(territory.panning) map.dragging.enable(); else map.dragging.disable();
  }
  for(const [id,key] of [["brush-add","add"],["brush-remove","remove"],["brush-pan","pan"]]){
    el(id).className=kind===key?"active":"subtle";
  }
}
function editTerritory(){
  territory.editing=!territory.editing;
  el("territory-tools").hidden=!territory.editing;
  el("edit-territory").textContent=territory.editing?"Wyłącz zaznaczanie":"Włącz zaznaczanie na mapie";
  if(territory.editing){
    territory.grid.addTo(map);
    territory.selectionLayer.addTo(map);
    chooseBrush("add");
    territoryInfo(window.matchMedia("(hover: hover) and (pointer: fine)").matches
      ? "LPM: zaznaczanie, PPM: wymazywanie, ŚPM: przesuwanie."
      : "Wybierz tryb pędzla przyciskami, aby zaznaczać lub przesuwać mapę.");
  }else{
    territory.grid.remove();
    territory.selectionLayer.remove();
    territory.painting=false; territory.paintBefore=null; territory.lastPaint=null;
    map.dragging.enable();
  }
}
function territoryRequestRow(item,admin,allowCancel=false){
  const root=document.createElement("div"); root.className="territory-request";
  const meta=document.createElement("div"); meta.className="meta";
  meta.textContent=`#${item.id} · ${item.nation} · ${item.action==="ADD"?"Przyznanie":"Oddanie"} · ${item.count} chunków`;
  const controls=document.createElement("div"); controls.className="territory-row";
  const show=document.createElement("button"); show.className="subtle"; show.textContent="Pokaż na mapie";
  show.addEventListener("click",()=>showTerritoryRequest(item.id)); controls.append(show);
  if(admin){
    for(const [label,decision] of [["Zatwierdź","approve"],["Odrzuć","reject"]]){
      const b=document.createElement("button"); b.className=decision==="reject"?"subtle":"";b.textContent=label;
      b.addEventListener("click",()=>reviewTerritoryRequest(item.id,decision)); controls.append(b);
    }
  }else if(allowCancel){
    const cancel=document.createElement("button"); cancel.className="subtle";cancel.textContent="Anuluj";
    cancel.addEventListener("click",()=>cancelTerritoryRequest(item.id));controls.append(cancel);
  }
  root.append(meta,controls);
  return root;
}
function renderTerritoryRequests(items,id,admin,allowCancel=false){
  const holder=el(id);holder.replaceChildren();
  if(!items?.length){
    const p=document.createElement("p");p.className="note";p.textContent="Brak oczekujących wniosków.";holder.append(p);return;
  }
  for(const item of items) holder.append(territoryRequestRow(item,admin,allowCancel));
}
async function updateTerritoryOverview(){
  if(!loggedIn){el("territory-section").hidden=true;return;}
  try{
    const data=await json("/api/territory/overview");
    if(!loggedIn) return;
    el("territory-section").hidden=false;
    el("territory-manager").hidden=!data.manager;
    el("territory-admin").hidden=!data.admin;
    if(!data.manager && territory.editing) editTerritory();
    renderTerritoryRequests(data.own,"territory-own-requests",false,data.manager);
    if(data.admin) renderTerritoryRequests(data.pending,"territory-admin-requests",true);
  }catch(err){territoryInfo(`Nie udało się wczytać wniosków: ${err.message}`);}
}
async function showTerritoryRequest(id){
  try{
    const data=await json(`/api/territory/detail?id=${encodeURIComponent(id)}`);
    territory.detailLayer.clearLayers();
    const style={...selectionStyle(),color:"#eab3ff",fillColor:"#b75cea",fillOpacity:.32};
    const conflicts = new Set((data.conflicts||[]).map(([x,z])=>chunkKey(x,z)));
    if(data.sparse) for(const [x,z] of data.chunks) {
      const problematic=conflicts.has(chunkKey(x,z));
      L.rectangle(chunkBounds(x,z),problematic ? selectionStyle("FOREIGN") : style).addTo(territory.detailLayer);
    }
    if(!data.sparse){
      const [minX,minZ,maxX,maxZ]=data.bounds;
      L.rectangle([toLatLng(minX*16,(maxZ+1)*16),toLatLng((maxX+1)*16,minZ*16)],style).addTo(territory.detailLayer);
    }
    const [minX,minZ,maxX,maxZ]=data.bounds;
    map.fitBounds([toLatLng(minX*16,(maxZ+1)*16),toLatLng((maxX+1)*16,minZ*16)],
      {padding:[42,42],maxZoom:territory.maxZoom});
    territoryInfo(`Wniosek #${id}: ${data.nation}, ${data.action}. Fioletowy = propozycja, czerwony = konflikt (${conflicts.size}). Stare prostokątne wnioski mają tylko podgląd obszaru.`);
  }catch(err){territoryInfo(err.message);}
}
async function reviewTerritoryRequest(id,decision){
  if(!confirm(`Czy na pewno ${decision==="approve"?"zatwierdzić":"odrzucić"} wniosek #${id}?`)) return;
  try{
    await territoryPost("/api/territory/review",{id:String(id),decision});
    territory.detailLayer.clearLayers();
    await updateTerritoryOverview();
    if(decision==="approve") await updatePublic();
    territoryInfo(`Wniosek #${id} ${decision==="approve"?"zatwierdzono":"odrzucono"}.`);
  }catch(err){territoryInfo(err.message);}
}
async function cancelTerritoryRequest(id){
  if(!confirm(`Anulować wniosek #${id}?`)) return;
  try{
    await territoryPost("/api/territory/cancel",{id:String(id)});
    territory.detailLayer.clearLayers();
    await updateTerritoryOverview();
    territoryInfo(`Anulowano wniosek #${id}.`);
  }catch(err){territoryInfo(err.message);}
}
async function previewTerritory(){
  if(!territory.selected.size){territoryInfo("Najpierw zaznacz chunki na mapie.");return;}
  try{
    const data=await territoryPost("/api/territory/preview",{chunks:selectedString()});
    let foreign=0,own=0,neutral=0;
    for(const c of data.chunks){
      const layer=territory.selected.get(chunkKey(c.x,c.z))?.layer;
      if(layer) layer.setStyle(selectionStyle(c.status));
      if(c.status==="FOREIGN")foreign++;else if(c.status==="OWN")own++;else neutral++;
    }
    territoryInfo(`Sprawdzono ${data.chunks.length}: neutralne ${neutral}, własne ${own}, cudze ${foreign} (czerwone).`);
  }catch(err){territoryInfo(err.message);}
}
async function submitTerritory(){
  if(!territory.selected.size){territoryInfo("Najpierw zaznacz chunki na mapie.");return;}
  const action=el("territory-action").value;
  try{
    // Preview is informational. Submission and approval re-check all owners on the server.
    const data=await territoryPost("/api/territory/submit",{action,chunks:selectedString()});
    restoreSelection(new Map());territory.history=[];
    await updateTerritoryOverview();
    territoryInfo(`Wysłano wniosek #${data.id} (${action}). Czeka na administratora.`);
  }catch(err){territoryInfo(err.message);}
}
function setupTerritoryUI(){
  el("edit-territory").addEventListener("click",editTerritory);
  el("brush-add").addEventListener("click",()=>chooseBrush("add"));
  el("brush-remove").addEventListener("click",()=>chooseBrush("remove"));
  el("brush-pan").addEventListener("click",()=>chooseBrush("pan"));
  el("territory-clear").addEventListener("click",()=>{
    if(territory.selected.size){saveUndo(new Map(territory.selected));restoreSelection(new Map());}
  });
  el("territory-undo").addEventListener("click",()=>{
    const previous=territory.history.pop();if(previous)restoreSelection(previous);
  });
  el("territory-preview").addEventListener("click",previewTerritory);
  el("territory-submit").addEventListener("click",submitTerritory);
  el("territory-refresh").addEventListener("click",updateTerritoryOverview);
}

async function init() {
  for (const [name,id] of [["countries","show-countries"],["cores","show-cores"],["players","show-players"],["worldborder","show-worldborder"]]) {
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
  setupTerritoryUI();
  await initMap();
  if(ready) { territory.maxZoom=Math.round(Math.log2(1/scale));initTerritoryMap(); }
  try { await json("/api/me"); setLoggedIn(true); } catch { setLoggedIn(false); }
  await updatePrivate();
  await updateTerritoryOverview();
  setInterval(updatePrivate,1500);
}
init();
