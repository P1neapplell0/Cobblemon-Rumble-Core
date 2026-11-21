'use strict';

// Original 24 px menu glyphs drawn into device-independent canvases.
const iconPaths = {
  settings: 'm10 2-1 3-2 .9-2.9-.7L2 8.9l2.1 2.1v2L2 15.1l2.1 3.7 2.9-.7 2 .9 1 3h4l1-3 2-.9 2.9.7 2.1-3.7-2.1-2.1v-2L22 8.9l-2.1-3.7-2.9.7-2-.9-1-3ZM15 12a3 3 0 1 1-6 0 3 3 0 1 1 6 0Z',
  discord: 'M5.3 6.5a16 16 0 0 1 4.1-1.3l.7 1.4a16 16 0 0 1 3.8 0l.7-1.4a16 16 0 0 1 4.1 1.3c2 3.1 2.8 6.2 2.4 9.2a15 15 0 0 1-5 2.6l-1.1-1.8 1.5-.7c-2.9 1.2-6.1 1.2-9 0l1.5.7-1.1 1.8a15 15 0 0 1-5-2.6c-.4-3 .4-6.1 2.4-9.2Z M10.9 12.2a1.4 1.6 0 1 0-2.8 0 1.4 1.6 0 1 0 2.8 0Z M15.9 12.2a1.4 1.6 0 1 0-2.8 0 1.4 1.6 0 1 0 2.8 0Z',
  mods: 'm12 2.5 8.5 4.75v9.5L12 21.5l-8.5-4.75v-9.5Zm0 19v-9.3m8.5-4.95L12 12.2 3.5 7.25m4.25-2.37 8.5 4.74v3.8',
  quit: 'M12.5 5V3H3v18h9.5v-2M8 12h13m-4-4 4 4-4 4',
  language: 'M21.5 12a9.5 9.5 0 1 1-19 0 9.5 9.5 0 0 1 19 0ZM2.5 12h19M12 2.5c2.5 2.6 3.5 6.1 3.5 9.5s-1 6.9-3.5 9.5M12 2.5C9.5 5.1 8.5 8.6 8.5 12s1 6.9 3.5 9.5',
};
for (const icon of document.querySelectorAll('canvas[data-icon]')) {
  const name = icon.dataset.icon;
  icon.width = icon.height = 72;
  const context = icon.getContext('2d');
  const color = getComputedStyle(icon).color;
  context.scale(3, 3);
  context.strokeStyle = context.fillStyle = color;
  context.lineWidth = 1.65;
  context.lineJoin = context.lineCap = 'round';
  const path = new Path2D(iconPaths[name]);
  if (name === 'discord') context.fill(path, 'evenodd');
  else context.stroke(path);
}
