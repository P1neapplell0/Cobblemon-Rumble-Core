/* global THREE */
'use strict';

// Project branched arcs from the rig's cheek bones. Separate front/back layers let the arcs wrap the body.
function createDischarge(stage, rendererCanvas) {
  const back = document.createElement('canvas');
  const front = document.createElement('canvas');
  const behind = back.getContext('2d');
  const ahead = front.getContext('2d');
  stage.insertBefore(back, rendererCanvas);
  stage.append(front);
  const cheekLeft = new THREE.Vector3();
  const cheekRight = new THREE.Vector3();
  const center = new THREE.Vector3();
  const vertex = new THREE.Vector3();
  let width = 0;
  let height = 0;
  let visible = false;

  const fraction = value => value - Math.floor(value);
  const sample = (key, step) => fraction(Math.sin(key * 91.73 + step * 39.21) * 4705.52);
  function resize(w, h, ratio) {
    width = w;
    height = h;
    for (const ctx of [behind, ahead]) {
      ctx.canvas.width = Math.round(w * ratio);
      ctx.canvas.height = Math.round(h * ratio);
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    }
  }
  function projection(position, camera) {
    vertex.copy(position).project(camera);
    return [(vertex.x + 1) * width / 2, (1 - vertex.y) * height / 2];
  }
  function spark(ctx, x, y, dx, dy, key, step, brightness) {
    const path = new Path2D();
    const bends = 6;
    const points = [[x, y]];
    path.moveTo(x, y);
    for (let j = 1; j <= bends; j++) {
      const t = j / bends;
      const noise = j === bends ? 0 : (sample(key + j * 6, step) - .5) * .42;
      const point = [x + dx * t - dy * noise, y + dy * t + dx * noise];
      points.push(point);
      path.lineTo(...point);
    }
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    const size = Math.max(.7, Math.min(1.75, height / 590));
    ctx.strokeStyle = `rgba(255,166,17,${brightness * .24})`;
    ctx.lineWidth = 7 * size;
    ctx.stroke(path);
    ctx.strokeStyle = `rgba(255,204,47,${brightness * .72})`;
    ctx.lineWidth = 3 * size;
    ctx.stroke(path);
    ctx.strokeStyle = `rgba(255,248,175,${brightness})`;
    ctx.lineWidth = size;
    ctx.stroke(path);
    const [splitX, splitY] = points[3];
    const fork = .12 + .1 * sample(key + 7, step);
    ctx.beginPath();
    ctx.moveTo(splitX, splitY);
    ctx.lineTo(splitX + dx * .13 + dy * fork, splitY + dy * .13 - dx * fork);
    ctx.lineTo(splitX + dx * .24 + dy * fork, splitY + dy * .24 - dx * fork);
    ctx.stroke();
  }
  function draw(rig, camera, age, power) {
    if (visible) for (const ctx of [behind, ahead]) ctx.clearRect(0, 0, width, height);
    visible = power > 0;
    if (!visible) return;
    rig.parts.get('cheek_active_left').group.getWorldPosition(cheekLeft);
    rig.parts.get('cheek_active_right').group.getWorldPosition(cheekRight);
    center.copy(cheekLeft).lerp(cheekRight, .5);
    const step = Math.floor(age * 17);
    const amplitude = rig.root.scale.x * (13 + 9 * power);
    const brightness = Math.min(1, power * (1.15 + .3 * sample(97, step)));
    for (let i = 0; i < 10; i++) {
      const angle = i / 10 * Math.PI * 2 + (sample(i + 27, step) - .5) * .53 + age * .3;
      const cheek = i % 2 === 0 ? cheekLeft : cheekRight;
      const rim = new THREE.Vector3(center.x + Math.cos(angle) * amplitude,
        center.y + Math.sin(angle) * amplitude * .9,
        center.z + (sample(i + 65, step) - .54) * amplitude);
      const ctx = rim.z > center.z ? ahead : behind;
      const [x, y] = projection(cheek, camera);
      const [endX, endY] = projection(rim, camera);
      spark(ctx, x, y, endX - x, endY - y, i, step, brightness * (.5 + .5 * sample(i, step)));
      const dx = endX - x;
      const dy = endY - y;
      ctx.fillStyle = `rgba(255,231,110,${brightness * .8})`;
      ctx.fillRect(endX + dx * .11, endY + dy * .11, 2 + (i % 3), 2 + (i % 2));
      ctx.fillRect(endX - dy * .15, endY + dx * .15, 1.8, 1.8);
    }
  }
  return {resize, draw};
}
