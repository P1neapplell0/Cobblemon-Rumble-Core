const clamp = x => Math.min(1, Math.max(0, x));
const mix = (a,b,t) => a+(b-a)*t;
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
  const expr = String(value).replace(/(?:query|q)\.anim_time/g,'t').replace(/math\.(sin|cos)/gi,'m.$1');
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
