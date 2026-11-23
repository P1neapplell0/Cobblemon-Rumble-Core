/* global THREE */
'use strict';
const $ = id => document.getElementById(id);
const DURATION = 5.4;
const matchups = [['steve','trainer'],['steve','alex'],['steve','charizard'],['charizard','mewtwo']];
// Label, subtitle, backdrop word, crop centre, visible height, three-quarter yaw.
const roster = {
  steve: ['史蒂夫','STEVE','STEVE',23,22,.38],
  alex: ['艾利克斯','ALEX','ALEX',23,22,.38],
  trainer: ['火系馆主','FIRE GYM LEADER','LEADER',23,22,.41],
  charizard: ['超级喷火龙 X','MEGA CHARIZARD X','MEGA X',34,37,.47],
  mewtwo: ['超梦','MEWTWO','MEWTWO',34,31,.32],
};
const clamp = x => Math.min(1, Math.max(0, x));
const mix = (a,b,t) => a+(b-a)*t;
const ease = x => 1-(1-clamp(x))**3;
const smooth = x => { x = clamp(x); return x*x*(3-2*x); };
const envelope = (t, start, rise, hold, end) => smooth((t-start)/rise)*(1-smooth((t-hold)/(end-hold)));
// Cobblemon uses Java ModelPart rotations. Moving from Java's down/back axes
// to the preview's up/front axes preserves pitch and reverses yaw and roll.
const rot = (object, [x,y,z]) => object.rotation.set(x*Math.PI/180,-y*Math.PI/180,-z*Math.PI/180,'ZYX');
const zero = [0,0,0], one = [1,1,1];
const reduced = matchMedia('(prefers-reduced-motion: reduce)');

// Bedrock box UVs, read from the skin atlas in pixel space. Three faces are ±X, ±Y, ±Z.
function cubeGeometry(cube, bone, w, h) {
  const [x,y,z] = cube.size;
  const [u,v] = cube.uv;
  const inflate = 2*(cube.inflate || 0);
  const geometry = new THREE.BoxGeometry(...[x,y,z].map(s => Math.max(.001,s+inflate)));
  const uv = geometry.attributes.uv;
  const mirror = cube.mirror ?? bone.mirror ?? false;
  const rects = [
    [u,v+z,z,y], [u+z+x,v+z,z,y], [u+z,v,x,z], [u+z+x,v,x,z],
    [u+z,v+z,x,y], [u+2*z+x,v+z,x,y],
  ];
  if (mirror) [rects[0],rects[1]] = [rects[1],rects[0]];
  rects.forEach(([a,b,dx,dy], face) => {
    for (let j=0;j<4;j++) {
      const right = (j%2===1) !== mirror;
      const bottom = (j>=2) !== (face===3);
      uv.setXY(face*4+j,(a+(right ? dx : 0))/w,1-(b+(bottom ? dy : 0))/h);
    }
  });
  return geometry;
}

function buildRig(geo, map, glow, human) {
  const {description,bones} = geo['minecraft:geometry'][0];
  const root = new THREE.Group();
  const parts = new Map();
  const material = new THREE.MeshLambertMaterial({map,alphaTest:.4,side:THREE.DoubleSide});
  const emissive = glow.length ? new THREE.MeshBasicMaterial({map:glow[0],alphaTest:.4,
    side:THREE.DoubleSide,polygonOffset:true,polygonOffsetFactor:-1,polygonOffsetUnits:-2}) : null;
  for (const bone of bones) {
    const group = new THREE.Group();
    parts.set(bone.name,{group,bone});
    if (human && /^(eyelid_|mouth|leg_)/.test(bone.name)) group.visible = false;
    for (const cube of bone.cubes || []) {
      const pivot = cube.pivot || bone.pivot;
      const hinge = new THREE.Group();
      hinge.position.set(pivot[0]-bone.pivot[0],pivot[1]-bone.pivot[1],bone.pivot[2]-pivot[2]);
      if (cube.rotation) rot(hinge,cube.rotation);
      group.add(hinge);
      const geometry = cubeGeometry(cube,bone,description.texture_width,description.texture_height);
      const [x,y,z] = cube.origin.map((o,i) => o+cube.size[i]/2-pivot[i]);
      const mesh = new THREE.Mesh(geometry,material);
      mesh.position.set(x,y,-z);
      hinge.add(mesh);
      if (emissive) mesh.add(new THREE.Mesh(geometry,emissive));
    }
  }
  for (const {group,bone} of parts.values()) {
    const parent = parts.get(bone.parent);
    const p = parent?.bone.pivot || zero;
    group.position.set(bone.pivot[0]-p[0],bone.pivot[1]-p[1],p[2]-bone.pivot[2]);
    (parent?.group || root).add(group);
    parts.get(bone.name).rest = group.position.clone();
  }
  return {root,parts,emissive,glow};
}

// Only the arithmetic and degree-based trig used by the selected local animation clips.
const trig = {sin:x=>Math.sin(x*Math.PI/180),cos:x=>Math.cos(x*Math.PI/180)};
function scalar(value) {
  if (typeof value === 'number') return () => value;
  const expr = String(value).replace(/(?:query|q)\.anim_time/g,'t').replace(/math\.(sin|cos)/g,'m.$1');
  if (!/^[\d.()+* /\-]+$/.test(expr.replace(/m\.(sin|cos)|\bt\b/g,'0'))) throw Error('Unsupported animation expression: '+value);
  // Disallow comments and exponentiation tokens as well as arbitrary names.
  if (/\/\/|\/\*|\*\*/.test(expr)) throw Error('Unsupported animation operator: '+value);
  const calculate = Function('t','m',`return (${expr});`);
  return t => calculate(t,trig);
}
function vector(value) {
  const entries = (Array.isArray(value) ? value : [value,value,value]).map(scalar);
  return t => entries.map(f=>f(t));
}
function channel(value) {
  if (Array.isArray(value) || typeof value !== 'object') return vector(value);
  const keys = Object.entries(value).map(([time,v]) => ({time:Number(time),
    pre:vector(v.pre ?? v.post ?? v),post:vector(v.post ?? v.pre ?? v),curve:v.lerp_mode==='catmullrom'})).sort((a,b)=>a.time-b.time);
  return t => {
    let i=0;
    while (i+1<keys.length && keys[i+1].time<=t) i++;
    const a=keys[i],b=keys[i+1];
    if (t<a.time) return a.pre(t);
    if (!b) return a.post(t);
    const p=a.post(t),q=b.pre(t),f=clamp((t-a.time)/(b.time-a.time));
    if (!a.curve && !b.curve) return p.map((v,j)=>mix(v,q[j],f));
    const prev=keys[i-1]?.post(t) || p,next=keys[i+2]?.pre(t) || q;
    return p.map((v,j) => .5*((2*v)+(-prev[j]+q[j])*f+(2*prev[j]-5*v+4*q[j]-next[j])*f*f+(-prev[j]+3*v-3*q[j]+next[j])*f*f*f));
  };
}
function compile(clip) {
  return Object.fromEntries(Object.entries(clip.bones).map(([name,bone])=>[name,
    Object.fromEntries(['position','rotation','scale'].filter(key=>bone[key]!==undefined).map(key=>[key,channel(bone[key])]))]));
}
function pose(rig,id,time,age,entry,side) {
  const {idle,cry,emissive,glow} = rig;
  const isMega = id==='charizard';
  const elapsed = Math.max(0,age);
  const mew = id==='mewtwo' ? envelope(age,0,.24,.63,1.27) : 0;
  for (const [name,{group,bone,rest}] of rig.parts) {
    const base = idle?.[name];
    const call = age>=0 && isMega && age<=2.4167 ? cry?.[name] : undefined;
    const accent = mew ? cry?.[name] : undefined;
    const r = base?.rotation?.(time) || zero;
    const p = base?.position?.(time) || zero;
    const s = base?.scale?.(time) || one;
    const dr = (call || accent)?.rotation?.(elapsed) || zero;
    const dp = (call || accent)?.position?.(elapsed) || zero;
    const ds = (call || accent)?.scale?.(elapsed) || one;
    const weight=call ? 1-smooth((age-2.23)/.1867) : mew;
    const angles = r.map((v,j)=>v+(bone.rotation?.[j]||0)+dr[j]*weight);
    if (id==='mewtwo') {
      if (name==='torso3') angles[0]-=10*mew;
      if (name==='head_angle') angles[0]-=16*mew;
      if (name==='arm_left' || name==='arm_right') angles[2]+=(name==='arm_left'?-19:19)*mew;
    }
    if (!idle) {
      const hand = side===0 ? 'arm_left' : 'arm_right';
      const guard = id==='trainer' ? 53 : id==='alex' ? 43 : 62;
      if (name===hand) angles[0]-=guard*entry;
      else if (name.startsWith('arm_') && name!=='arm_ball') angles[0]-=25*entry;
      if (name===hand) angles[2]+=(side===0?1:-1)*10*entry;
      if (name==='head') angles[1]+=(side===0?-1:1)*9*entry;
      if (name==='torso') angles[2]+=Math.sin(time*3)*.8;
    }
    rot(group,angles);
    group.position.set(rest.x+p[0]+dp[0]*weight,rest.y+p[1]+dp[1]*weight,rest.z-p[2]-dp[2]*weight);
    group.scale.set(...s.map((v,j)=>v*mix(1,ds[j],weight)));
  }
  if (emissive) emissive.map=glow[Math.floor(time*10)%glow.length];
}

const stage=$('stage'), view=$('view'), seek=$('seek'), play=$('play');
const modeButtons = [...document.querySelectorAll('[data-mode]')];
let renderer, camera, scenes, rigs, assets, observer;
let mode=3, time=0, running=false, startTime=0, raf=0, sound=false;
let auditions=[];
const timings = () => matchups[mode].map((id,i)=>assets[id].cry ? (mode===3 && i===0 ? 1.05 : mode===3 ? 2.6 : 1.43) : Infinity);
function stopAudio() { auditions.forEach(({audio})=>audio.pause()); }
function soundBetween(previous,next) {
  if (!sound) return;
  for (const {at,audio} of auditions) {
    if (previous<at && at<=next) {
      audio.currentTime=0;
      audio.play().catch(()=>{});
    }
  }
}
function draw() {
  if (!renderer || !rigs) return;
  const width=view.clientWidth,height=view.clientHeight;
  if (!width || !height) return;
  renderer.setSize(width,height,false);
  renderer.setScissorTest(false);
  renderer.clear();
  renderer.setScissorTest(true);
  const split=Math.floor(width/2);
  const ids=matchups[mode], starts=timings();
  for (let i=0;i<2;i++) {
    const id=ids[i],rig=rigs[id];
    const [, , ,center,span,yaw]=roster[id];
    const dir=i===0 ? 1 : -1;
    const entry=ease((time-(i===0?.10:.29))/.73);
    const squash=reduced.matches ? 0 : envelope(time,.72,.13,.85,1.16);
    const exit=reduced.matches ? 0 : smooth((time-4.68)/.55);
    const panel=i===0?split:width-split;
    const halfH=span/2*(1+.09*exit-.035*squash);
    // Preserve a readable portrait on narrow screens without changing its proportions.
    const halfW=Math.max(halfH*panel/height, id==='charizard'?14:9.5);
    const visibleH=halfW*height/panel;
    const centerY=center+(visibleH-halfH)*.56;
    camera.left=-halfW; camera.right=halfW;
    camera.top=centerY+visibleH; camera.bottom=centerY-visibleH;
    camera.updateProjectionMatrix();
    rig.root.position.set(dir*(-halfW*1.64*(1-entry)-halfW*.28*exit),0,0);
    rig.root.rotation.y=dir*(yaw+(1-entry)*.72);
    pose(rig,id,time,time-starts[i],entry,i);
    renderer.setViewport(i===0?0:split,0,panel,height);
    renderer.setScissor(i===0?0:split,0,panel,height);
    renderer.render(scenes[i],camera);
    const card=$('card'+i),ghost=$('ghost'+i);
    card.style.opacity=entry*(1-exit*.72);
    card.style.transform=`translateX(${dir*(entry-1)*65}px)`;
    ghost.style.transform=`translateX(${dir*(entry-1)*85}px)`;
    ghost.style.opacity=entry;
  }
  renderer.setScissorTest(false);
  const hit=ease((time-.82)/.27);
  const leave=smooth((time-4.18)/.23);
  const showFight=smooth((time-4.31)/.16);
  const v=$('versus'),f=$('fight');
  v.style.opacity=hit*(1-leave);
  v.style.transform=`translate(-50%,-50%) scale(${reduced.matches?1:mix(1.5,1,hit)})`;
  f.style.opacity=showFight;
  f.style.transform=`translate(-50%,-50%) scale(${reduced.matches?1:mix(1.34,1,showFight)})`;
  $('flash').style.opacity=reduced.matches ? 0 : .20*(envelope(time,.82,.04,.87,1.09)+envelope(time,4.34,.04,4.39,4.57));
  const phase=time<.8?'ENTRANCE':time<4.32?'FACE OFF':time<DURATION?'FIGHT':'END';
  $('phase').textContent=phase;
  stage.dataset.phase=phase;
  seek.value=time;
  $('time').value=`${time.toFixed(2)} / ${DURATION.toFixed(2)}`;
}
function pause() {
  running=false; cancelAnimationFrame(raf); stopAudio();
  play.textContent='▶  播放';
}
function frame(now) {
  const prev=time;
  time=Math.min(DURATION,(now-startTime)/1000);
  soundBetween(prev,time);
  draw();
  if (time>=DURATION) pause();
  else if (running) raf=requestAnimationFrame(frame);
}
function start(restart=false) {
  if (!rigs) return;
  pause();
  if (restart || time>=DURATION) time=0;
  startTime=performance.now()-time*1000;
  running=true; play.textContent='Ⅱ  暂停';
  draw(); raf=requestAnimationFrame(frame);
}
function select(next,autoplay) {
  pause(); mode=next; time=autoplay?0:1.1;
  modeButtons.forEach((button,i)=>button.setAttribute('aria-pressed',i===mode));
  const starts=timings();
  auditions=[];
  matchups[mode].forEach((id,i)=>{
    const [name,english,ghost]=roster[id];
    const kind=assets[id].cry ? 'POKÉMON' : id==='trainer' ? 'NPC / TRAINER' : i===0 ? 'P1 / PLAYER' : 'P2 / PLAYER';
    $('kind'+i).textContent=kind; $('name'+i).textContent=name; $('en'+i).textContent=english; $('ghost'+i).textContent=ghost;
    scenes[i].clear();
    scenes[i].add(...lights(i),rigs[id].root);
    if (assets[id].cry) auditions.push({at:starts[i],audio:assets[id].audio});
  });
  if (autoplay) start(true); else draw();
}
function lights(i) {
  const key=new THREE.DirectionalLight(0xfff1e2,2.05);
  key.position.set(i===0 ? 16 : -16,50,36);
  const fill=new THREE.DirectionalLight(i===0?0x57ccff:0xff8bdd,1.75);
  fill.position.set(i===0?-34:34,20,-18);
  return [key,fill,new THREE.HemisphereLight(0xdaedff,0x74688a,1.65)];
}
async function init() {
  try {
    assets=JSON.parse($('assets').textContent);
    renderer=new THREE.WebGLRenderer({alpha:true,antialias:true});
    renderer.setPixelRatio(Math.min(2,window.devicePixelRatio || 1));
    renderer.setClearColor(0x000000,0);
    view.append(renderer.domElement);
    camera=new THREE.OrthographicCamera(-1,1,1,-1,1,220);
    camera.position.z=100;
    scenes=[new THREE.Scene(),new THREE.Scene()];
    const loader=new THREE.TextureLoader();
    async function texture(url) {
      const map=await loader.loadAsync(url);
      map.magFilter=THREE.NearestFilter; map.minFilter=THREE.NearestFilter;
      map.generateMipmaps=false; map.colorSpace=THREE.SRGBColorSpace;
      return map;
    }
    rigs=Object.fromEntries(await Promise.all(Object.entries(assets).map(async ([id,a])=>{
      const [map,...glow]=await Promise.all([a.image,...(a.glow || [])].map(texture));
      const rig=buildRig(a.geo,map,glow,!a.cry);
      if (a.clips) {
        rig.idle=compile(a.clips.battle_idle || a.clips.ground_idle);
        rig.cry=compile(a.clips.cry || a.clips.surprised);
      }
      if (a.cry) { a.audio=new Audio(a.cry); a.audio.volume=.64; a.audio.preload='auto'; }
      return [id,rig];
    })));
    for (const button of modeButtons) button.onclick=()=>select(Number(button.dataset.mode),true);
    for (const button of [$('replay'),play,seek]) button.disabled=false;
    $('replay').onclick=()=>start(true);
    play.onclick=()=>running?pause():start();
    seek.oninput=()=>{pause(); time=Number(seek.value); draw();};
    $('sound').onclick=()=>{
      sound=!sound;
      if (!sound) stopAudio();
      $('sound').setAttribute('aria-pressed',sound);
      $('sound').textContent=`声音：${sound?'开':'关'}`;
    };
    window.addEventListener('keydown',e=>{
      if (e.altKey || e.ctrlKey || e.metaKey || e.repeat || /BUTTON|INPUT/.test(e.target.tagName)) return;
      if (e.code==='Space') { e.preventDefault(); running?pause():start(); }
      if (e.code==='KeyR') start(true);
    });
    observer=new ResizeObserver(draw);
    observer.observe(view);
    select(mode,!reduced.matches);
  } catch (error) {
    console.error(error);
    $('error').textContent='无法载入 3D 预览。请使用启用了 WebGL 的浏览器。';
    $('error').hidden=false;
  }
}
init();
