/* global THREE, rot, zero, one, reduced, buildRig, compile, createDischarge */
'use strict';

const assets = JSON.parse(document.getElementById('assets').textContent);
const stage = document.getElementById('stage');
const panel = document.getElementById('panel');
const panelTitle = document.getElementById('panel-title');
const panelContent = document.getElementById('panel-content');
const menu = document.querySelector('.menu');
const exitScreen = document.querySelector('.exit-screen');
const exitButton = document.getElementById('return-to-menu');
const copy = {
  zh: {play: '开始游戏', multiplayer: '多人游戏', settings: '设置', mods: '模组管理', language: '语言',
    tools: '主菜单工具', back: '返回', animation: '场景动画', audio: '场景音效', discord: 'Discord 社群',
    quit: '退出游戏', confirmQuit: '确认退出', returnToTitle: '返回标题界面'},
  en: {play: 'Singleplayer', multiplayer: 'Multiplayer', settings: 'Settings', mods: 'Manage Mods', language: 'Language',
    tools: 'Main menu tools', back: 'Back', animation: 'Scene animation', audio: 'Scene audio', discord: 'Discord Community',
    quit: 'Quit Game', confirmQuit: 'Confirm Quit', returnToTitle: 'Return to Title'},
};
const playback = {motion: !reduced.matches, audio: false};
const audios = Object.fromEntries(Object.entries(assets.sounds).map(([id, url]) => [id, new Audio(url)]));
for (const audio of Object.values(audios)) audio.volume = .45;
audios.lightning.volume = .56;
let locale = 'zh';
let activePanel;
let exited = false;
let refreshStage = () => {};

function toggleButton(text, selected, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'dialog-button';
  button.textContent = text;
  button.setAttribute('aria-pressed', selected);
  button.addEventListener('click', onClick);
  return button;
}

function syncLanguage() {
  document.documentElement.lang = locale === 'zh' ? 'zh-CN' : 'en';
  document.querySelectorAll('[data-label]').forEach(element => { element.textContent = copy[locale][element.dataset.label]; });
  document.querySelectorAll('[data-aria]').forEach(element => { element.setAttribute('aria-label', copy[locale][element.dataset.aria]); });
}

function setting(text, key) {
  const button = toggleButton(text, playback[key], () => {
    playback[key] = !playback[key];
    button.setAttribute('aria-pressed', playback[key]);
    if (!playback.motion || !playback.audio) Object.values(audios).forEach(audio => audio.pause());
    // Load the effects on the user's audio toggle. The scene's cues then control playback.
    if (key === 'audio' && playback.audio) Object.values(audios).forEach(audio => audio.load());
    refreshStage();
  });
  return button;
}

function setExited(value) {
  exited = value;
  menu.inert = value;
  exitScreen.hidden = !value;
  if (value) {
    Object.values(audios).forEach(audio => audio.pause());
    exitButton.focus();
  } else {
    document.querySelector('[data-open=play]').focus();
  }
  refreshStage();
}

exitButton.addEventListener('click', () => setExited(false));

function openPanel(name) {
  if (name === 'discord') {
    window.open('https://discord.gg/UfVF8b2GFk', '_blank', 'noopener,noreferrer');
    return;
  }
  activePanel = name;
  const text = copy[locale];
  panelTitle.textContent = text[name];
  panelContent.replaceChildren();
  if (name === 'settings') {
    panelContent.append(setting(text.animation, 'motion'), setting(text.audio, 'audio'));
  } else if (name === 'quit') {
    const confirm = document.createElement('button');
    confirm.type = 'button';
    confirm.className = 'dialog-button';
    confirm.textContent = text.confirmQuit;
    confirm.addEventListener('click', () => {
      panel.close();
      setExited(true);
    });
    panelContent.append(confirm);
  } else if (name === 'language') {
    for (const [language, label] of [['zh', '简体中文'], ['en', 'English']]) {
      panelContent.append(toggleButton(label, locale === language, () => {
        locale = language;
        syncLanguage();
        panel.close();
      }));
    }
  } else if (name === 'mods') {
    for (const mod of ['Cobblemon', 'Mega Showdown', 'Cobblemon Rumble Core']) {
      const row = document.createElement('p');
      row.textContent = mod;
      panelContent.append(row);
    }
  }
  if (!panel.open) panel.showModal();
}

for (const button of document.querySelectorAll('[data-open]')) {
  button.addEventListener('click', () => openPanel(button.dataset.open));
}
reduced.addEventListener('change', () => {
  playback.motion = !reduced.matches;
  if (!playback.motion) Object.values(audios).forEach(audio => audio.pause());
  if (panel.open && activePanel === 'settings') openPanel('settings');
  refreshStage();
});

const smoothstep = value => {
  const t = Math.min(1, Math.max(0, value));
  return t * t * (3 - 2 * t);
};
const interpolate = (from, to, weight) => from + (to - from) * weight;

const pulse = (time, start, attack, release, end) =>
  smoothstep((time - start) / attack) * (1 - smoothstep((time - release) / (end - release)));

function animateRig(rig, time, age) {
  const idleTime = rig.idleLength ? time % rig.idleLength : time;
  const moveWeight = rig.move ? pulse(age, 0, .18, 2.58, 3) : 0;
  const blinkTime = time % 5.3 - 4.9;
  for (const [name, {group, bone, rest}] of rig.parts) {
    const idle = rig.idle[name];
    const move = moveWeight ? rig.move[name] : undefined;
    const blink = blinkTime >= 0 && blinkTime <= rig.blinkLength ? rig.blink[name] : undefined;
    const sample = (key, fallback) => {
      const resting = idle?.[key]?.(idleTime) ?? fallback;
      const moving = move?.[key]?.(age);
      // Blend absolute channel poses to keep the cry pitch from stacking onto the idle neck.
      if (moving) return resting.map((value, i) => interpolate(value, moving[i], moveWeight));
      const blinking = blink?.[key]?.(blinkTime);
      return blinking ? resting.map((value, i) => value + blinking[i] - fallback[i]) : [...resting];
    };
    const rotation = sample('rotation', zero);
    const position = sample('position', zero);
    const scale = sample('scale', one);
    if (name.startsWith('cheek_active_') && !moveWeight) scale.fill(0);
    rot(group, rotation.map((value, i) => value + (bone.rotation?.[i] || 0)));
    group.position.set(rest.x + position[0], rest.y + position[1], rest.z - position[2]);
    group.scale.set(...scale);
  }
  if (rig.emissive) rig.emissive.map = rig.glow[Math.floor(time * 10) % rig.glow.length];
}

// Each team has five ground fighters and one flyer. Wide/portrait entries are
// [horizontal screen fraction, camera depth, optional altitude above the floor].
const lineup = {
  salamence: {wide: [.19, 205, 38], narrow: [.258, 202, 28], scale: .53, yaw: .96},
  scizor:    {wide: [.153, 181], narrow: [.159, 174], scale: .77, yaw: .67, shadowWidth: 44},
  meowscarada: {wide: [.326, 178], narrow: [.371, 158], scale: .72, yaw: .78, shadowWidth: 36},
  hitmontop: {wide: [.096, 121], narrow: [.119, 108], scale: .88, yaw: .65, shadowWidth: 30},
  lucario:   {wide: [.228, 133], narrow: [.265, 110], scale: .57, yaw: .89, shadowWidth: 36},
  pikachu:   {wide: [.365, 103], narrow: [.381, 88], scale: .52, yaw: 1.05, shadowWidth: 31},
  charizard: {wide: [.803, 203, 26], narrow: [.733, 202, 26], scale: .80, yaw: -.97},
  garchomp:  {wide: [.864, 187], narrow: [.865, 178], scale: .92, yaw: -.75, shadowWidth: 44},
  blaziken:  {wide: [.694, 187], narrow: [.608, 178], scale: .75, yaw: -.64, shadowWidth: 42},
  greninja:  {wide: [.611, 115], narrow: [.592, 94], scale: .62, yaw: -.61, shadowWidth: 47},
  ceruledge: {wide: [.759, 136], narrow: [.715, 120], scale: .63, yaw: -.83, shadowWidth: 37},
  cinderace: {wide: [.862, 133], narrow: [.833, 109], scale: .5, yaw: -.72, shadowWidth: 35},
};

function makeShadowTexture() {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 64;
  const ctx = canvas.getContext('2d');
  const fade = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
  fade.addColorStop(0, '#080d1bd9');
  fade.addColorStop(.43, '#080d1b91');
  fade.addColorStop(1, '#080d1b00');
  ctx.fillStyle = fade;
  ctx.fillRect(0, 0, 64, 64);
  return new THREE.CanvasTexture(canvas);
}

async function initializeStage() {
  const renderer = new THREE.WebGLRenderer({alpha: true, antialias: true, powerPreference: 'high-performance'});
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
  renderer.domElement.className = 'model-stage';
  stage.append(renderer.domElement);
  const discharge = createDischarge(stage, renderer.domElement);
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(43, 1, 24, 500);
  const amber = new THREE.DirectionalLight(0xffbd75, 2.45);
  amber.position.set(-40, 28, 58);
  const cyan = new THREE.DirectionalLight(0xb7e3ff, 3.0);
  cyan.position.set(68, 55, -28);
  const fill = new THREE.HemisphereLight(0xd9efff, 0x86624a, 1.48);
  const blast = new THREE.PointLight(0xffde73, 0, 115, 1.35);
  scene.add(amber, cyan, fill, blast);

  const shadowMaterial = new THREE.MeshBasicMaterial({map: makeShadowTexture(), transparent: true, depthWrite: false});
  const shadowGeometry = new THREE.PlaneGeometry(1, 1);
  shadowGeometry.rotateX(-Math.PI / 2);
  const loader = new THREE.TextureLoader();
  async function loadTexture(url) {
    const map = await loader.loadAsync(url);
    map.magFilter = map.minFilter = THREE.NearestFilter;
    map.generateMipmaps = false;
    map.colorSpace = THREE.SRGBColorSpace;
    return map;
  }
  const rigs = Object.fromEntries(await Promise.all(Object.entries(assets.rigs).map(async ([id, asset], index) => {
    const [map, ...glow] = await Promise.all([asset.image, ...(asset.glow ?? [])].map(loadTexture));
    const rig = buildRig(asset.geo, map, glow, false);
    const {battle_idle, ground_idle, air_idle, air_battle_cry, special, blink} = asset.clips;
    const idle = air_idle ?? battle_idle ?? ground_idle;
    const move = special ?? air_battle_cry;
    const {shadowWidth} = lineup[id];
    const shadow = shadowWidth ? new THREE.Mesh(shadowGeometry, shadowMaterial) : null;
    scene.add(rig.root);
    if (shadow) scene.add(shadow);
    return [id, Object.assign(rig, {
      idle: compile(idle), idleLength: idle.animation_length, phase: index * .47,
      move: move && compile(move), blink: blink && compile(blink), blinkLength: blink?.animation_length,
      flying: Boolean(air_idle), shadow, origin: new THREE.Vector3(),
    })];
  })));

  function positionRigs() {
    const narrow = camera.aspect < 5 / 6;
    const halfFov = Math.tan(THREE.MathUtils.degToRad(camera.fov) / 2);
    // Ground fighters share a floor plane. Their distance controls both screen height and scale.
    const floor = narrow ? -16 : -24;
    const widthLimit = narrow ? .43 * Math.min(1, camera.aspect / .46) : Math.min(1, camera.aspect / 1.62);
    for (const [id, {wide, narrow: portrait, scale, yaw, shadowWidth}] of Object.entries(lineup)) {
      const [x, depth, altitude = 0] = narrow ? portrait : wide;
      const halfHeight = halfFov * depth;
      const rig = rigs[id];
      rig.origin.set((2 * x - 1) * halfHeight * camera.aspect, floor + altitude, -depth);
      rig.root.position.copy(rig.origin);
      rig.root.rotation.y = yaw;
      rig.root.scale.setScalar(scale * widthLimit);
      if (rig.shadow) {
        rig.shadow.position.copy(rig.origin);
        rig.shadow.position.y += .10;
        rig.shadow.scale.set(shadowWidth * scale * widthLimit, 1, shadowWidth * scale * widthLimit * .58);
      }
    }
  }

  const cycle = 13;
  const idleSpeed = .75;
  const chargeAt = 1.7;
  const charizardAt = 7.15;
  const cueTimes = {pikachu: chargeAt + .53, lightning: chargeAt + 1.1, charizard: charizardAt + 1.0833};
  let elapsed = 0;
  let lastFrame;
  let request;
  function draw() {
    const beat = elapsed % cycle;
    const chargeAge = beat - chargeAt;
    const power = pulse(chargeAge, .42, .42, 1.7, 2.12);
    const ground = rigs.pikachu.origin;
    blast.position.set(ground.x + 8, ground.y + 17, ground.z + 10);
    blast.intensity = 165 * power * (.95 + .05 * Math.sin(chargeAge * 24));
    for (const [id, rig] of Object.entries(rigs)) {
      const idleTime = elapsed * idleSpeed + rig.phase;
      if (rig.flying) {
        const flightTime = id === 'charizard' ? elapsed * idleSpeed : idleTime;
        rig.root.position.y = rig.origin.y + 2.9 * Math.sin(flightTime * Math.PI / 2);
        rig.root.rotation.z = Math.sin(flightTime * 1.17) * .035;
      }
      const age = id === 'pikachu' ? chargeAge : beat - charizardAt;
      animateRig(rig, idleTime, age);
    }
    renderer.render(scene, camera);
    discharge.draw(rigs.pikachu, camera, chargeAge, power);
  }

  const shouldAnimate = () => playback.motion && !document.hidden && !exited;

  function tick(now) {
    request = undefined;
    if (lastFrame !== undefined) {
      const prior = elapsed;
      elapsed += Math.min((now - lastFrame) / 1000, .15);
      if (playback.audio) {
        for (const [id, cue] of Object.entries(cueTimes)) {
          if (Math.floor((prior - cue) / cycle) < Math.floor((elapsed - cue) / cycle)) {
            const audio = audios[id];
            audio.currentTime = 0;
            audio.play().catch(() => {});
          }
        }
      }
    }
    lastFrame = now;
    draw();
    if (shouldAnimate()) request = requestAnimationFrame(tick);
  }
  refreshStage = () => {
    lastFrame = undefined;
    if (request !== undefined) cancelAnimationFrame(request);
    request = undefined;
    if (shouldAnimate()) request = requestAnimationFrame(tick);
    else draw();
  };
  function resize() {
    const {clientWidth: width, clientHeight: height} = stage;
    if (!width || !height) return;
    renderer.setSize(width, height, false);
    discharge.resize(width, height, renderer.getPixelRatio());
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    positionRigs();
    draw();
  }
  new ResizeObserver(resize).observe(stage);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) Object.values(audios).forEach(audio => audio.pause());
    refreshStage();
  });
  resize();
  refreshStage();
}

initializeStage().catch(error => {
  console.error('Cobblemon title scene:', error);
  stage.replaceChildren();
});
