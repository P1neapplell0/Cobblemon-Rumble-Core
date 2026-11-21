// Export the textures shared with the game and early loader from the offline preview.
// Usage: node scripts/export-title-art.mjs <Chrome debugging port>
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import {pathToFileURL} from 'node:url';

const port = Number(process.argv[2]);
assert(port, 'Pass a Chrome debugging port.');
const endpoint = `http://127.0.0.1:${port}/json`;
const tab = await fetch(`${endpoint}/new?about:blank`, {method: 'PUT'}).then(r => r.json());
const socket = new WebSocket(tab.webSocketDebuggerUrl);
await new Promise(resolve => socket.addEventListener('open', resolve, {once: true}));
let sequence = 0;
const pending = new Map();
socket.addEventListener('message', event => {
  const msg = JSON.parse(event.data);
  if (!pending.has(msg.id)) return;
  const {resolve, reject} = pending.get(msg.id);
  pending.delete(msg.id);
  if (msg.error) reject(msg.error);
  else resolve(msg.result);
});
function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    pending.set(++sequence, {resolve, reject});
    socket.send(JSON.stringify({id: sequence, method, params}));
  });
}
try {
  await send('Emulation.setEmulatedMedia', {features: [{name: 'prefers-reduced-motion', value: 'reduce'}]});
  await send('Page.navigate', {url: pathToFileURL(`${process.cwd()}/build/title-screen/index.html`).href});
  await new Promise(resolve => setTimeout(resolve, 1200));
  const result = await send('Runtime.evaluate', {returnByValue: true, awaitPromise: true, expression: `(${exportArt})()`});
  if (result.exceptionDetails) throw Error(result.exceptionDetails.exception.description);
  await fs.mkdir('docs/title-screen/game', {recursive: true});
  for (const [key, data] of Object.entries(result.result.value)) {
    await fs.writeFile('docs/title-screen/game/' + key + '.png', Buffer.from(data.split(',')[1], 'base64'));
  }
  console.log('Exported ' + Object.keys(result.result.value).length + ' game textures');
} finally {
  socket.close();
  await fetch(`${endpoint}/close/${tab.id}`);
}

async function exportArt() {
  const result = {};
  const canvas = (w, h) => Object.assign(document.createElement('canvas'), {width: w, height: h});
  for (const icon of document.querySelectorAll('[data-icon]')) result[icon.dataset.icon] = icon.toDataURL();
  for (const [id, text] of [['singleplayer', 'SINGLEPLAYER'], ['multiplayer', 'MULTIPLAYER']]) {
    const art = canvas(540, 90), ctx = art.getContext('2d');
    ctx.font = '950 58px "Arial Black", system-ui';
    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.translate(270, 41);
    ctx.transform(1, 0, -.158, 1, 0, 0);
    ctx.strokeStyle = '#181020';
    ctx.lineWidth = 6;
    ctx.strokeText(text, 0, 5);
    ctx.fillStyle = id === 'singleplayer' ? '#a75228' : '#5c4e54';
    ctx.fillText(text, 0, 4);
    ctx.fillStyle = id === 'singleplayer' ? '#ffd493' : '#f6eee6';
    ctx.fillText(text, 0, 0);
    result[id] = art.toDataURL();
  }
  result.shadow = makeShadowTexture().image.toDataURL();
  for (let frame = 0; frame < 8; frame++) {
    const art = canvas(128, 128), ctx = art.getContext('2d');
    ctx.lineCap = ctx.lineJoin = 'round';
    for (let ray = 0; ray < 10; ray++) {
      const angle = (ray + frame * .12) * Math.PI * 2 / 10;
      const dx = Math.cos(angle), dy = Math.sin(angle);
      const length = 38 + 13 * Math.sin(ray * 37 + frame * 13);
      const x = 64 + (ray % 2 ? -5 : 5), y = 64;
      const path = new Path2D();
      let forkX, forkY;
      path.moveTo(x, y);
      for (let step = 1; step <= 6; step++) {
        const r = length * step / 6;
        const bend = step === 6 ? 0 : Math.sin(frame * 17 + ray * 41 + step * 23) * 7;
        const px = x + dx * r - dy * bend, py = y + dy * r + dx * bend;
        path.lineTo(px, py);
        if (step === 3) { forkX = px; forkY = py; }
      }
      path.moveTo(forkX, forkY);
      path.lineTo(forkX + dx * 7 + dy * 7, forkY + dy * 7 - dx * 7);
      path.lineTo(forkX + dx * 12 + dy * 8, forkY + dy * 12 - dx * 8);
      for (const [size, color] of [[4, '#ffa61136'], [1.65, '#ffcc2fb5'], [.68, '#fff8af']]) {
        ctx.lineWidth = size;
        ctx.strokeStyle = color;
        ctx.stroke(path);
      }
      ctx.fillStyle = '#ffe76eb8';
      ctx.fillRect(x + dx * (length + 4), y + dy * (length + 4), .8, .8);
    }
    result['electric-' + frame] = art.toDataURL();
  }

  const asset = assets.rigs.pikachu;
  const map = await new THREE.TextureLoader().loadAsync(asset.image);
  map.minFilter = map.magFilter = THREE.NearestFilter;
  map.colorSpace = THREE.SRGBColorSpace;
  const rig = buildRig(asset.geo, map, [], false);
  const clip = asset.clips.ground_run;
  Object.assign(rig, {idle: compile(clip), idleLength: clip.animation_length});
  rig.root.rotation.y = 1.25;
  const scene = new THREE.Scene();
  scene.add(rig.root, new THREE.HemisphereLight(0xfff6d7, 0x877297, 2.5));
  const sun = new THREE.DirectionalLight(0xffe5a4, 2.3);
  sun.position.set(10, 30, 40);
  scene.add(sun);
  const camera = new THREE.OrthographicCamera(-23, 23, 23, -23, 1, 200);
  camera.position.set(0, 20, 70);
  camera.lookAt(0, 11, 0);
  const renderer = new THREE.WebGLRenderer({alpha: true, antialias: true, preserveDrawingBuffer: true});
  renderer.setSize(96, 96);
  const atlas = canvas(96 * 12, 96), ctx = atlas.getContext('2d');
  for (let i = 0; i < 12; i++) {
    animateRig(rig, (clip.animation_length || .5) * i / 12, -1);
    renderer.render(scene, camera);
    ctx.drawImage(renderer.domElement, i * 96, 0);
  }
  renderer.dispose();
  result.runner = atlas.toDataURL();
  return result;
}
