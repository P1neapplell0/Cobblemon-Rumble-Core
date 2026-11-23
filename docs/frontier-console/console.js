'use strict';
const dex = {
  pikachu:['皮卡丘','light_ball',['volt','quick','iron','knock']], lucario:['路卡利欧','life_orb',['aura','iron','quick','pulse']], charizard:['喷火龙','expert_belt',['flame','slash','pulse','quick']],
  gengar:['耿鬼','focus_sash',['shadow','sludge','willow','knock']], eevee:['伊布','eviolite',['quick','bite','swift','knock']], blastoise:['水箭龟','leftovers',['water','ice','pulse','iron']],
  meowscarada:['魔幻假面喵','choice_band',['flower','bite','quick','knock']], mimikyu:['谜拟丘','life_orb',['shadow','rough','willow','quick']], dragonite:['快龙','expert_belt',['pulse','quick','flame','ice']],
  greninja:['甲贺忍蛙','focus_sash',['water','bite','ice','quick']], gardevoir:['沙奈朵','leftovers',['rough','psy','shadow','aura']], scizor:['巨钳螳螂','rocky_helmet',['iron','quick','bite','knock']]
};
const moves={volt:['十万伏特','electric','电',90,15],quick:['电光一闪','normal','普',40,30],iron:['铁尾','steel','钢',100,15],knock:['拍落','dark','恶',65,20],aura:['波导弹','fighting','斗',80,20],pulse:['龙之波动','dragon','龙',85,10],flame:['喷射火焰','fire','火',90,15],slash:['空气斩','flying','飞',75,15],shadow:['暗影球','ghost','幽',80,15],sludge:['污泥炸弹','poison','毒',90,10],willow:['鬼火','fire','火','—',15],bite:['咬住','dark','恶',60,25],swift:['高速星星','normal','普',60,20],water:['水炮','water','水',110,5],ice:['冰冻光束','ice','冰',90,10],flower:['千变万花','grass','草',70,10],rough:['嬉闹','fairy','妖',90,10],psy:['精神强念','psychic','超',90,10],thunder:['打雷','electric','电',110,10],volt_switch:['伏特替换','electric','电',70,20],tail:['摇尾巴','normal','普','—',30]};
const items={light_ball:'电气球',life_orb:'生命宝珠',expert_belt:'达人带',focus_sash:'气势披带',eviolite:'进化奇石',leftovers:'吃剩的东西',choice_band:'讲究头带',rocky_helmet:'凸凸头盔'};
const stones={charizardite_x:'喷火龙进化石 X',charizardite_y:'喷火龙进化石 Y',lucarionite:'路卡利欧进化石',gengarite:'耿鬼进化石',blastoisinite:'水箭龟进化石',gardevoirite:'沙奈朵进化石',scizorite:'巨钳螳螂进化石'};
const names={...items,...stones};
const makePick=id=>({id,item:dex[id][1],moves:[...dex[id][2]]});
const sampleTeam=()=>['pikachu','lucario','charizard'].map(makePick);
const team=sampleTeam();
const rival=['gengar','eevee','blastoise'].map(makePick);
const rooms=[
  {name:'RivalProbe',skin:'ari',stake:200},{name:'MistyRoute',skin:'alex',stake:500},
  {name:'Nacrene',skin:'efe',stake:0},{name:'IronLedger',skin:'kai',stake:100},
  {name:'Northstar',skin:'steve',stake:300},{name:'MintTea',skin:'makena',stake:50},
  {name:'Sundown',skin:'noor',stake:1000},{name:'RiverStone',skin:'sunny',stake:0},
];
const guest={name:'Calem',skin:'kai',picks:['eevee',null,'scizor']};
rooms.forEach((room,i)=>{
  room.picks=[['gengar','eevee','blastoise'],['mimikyu',null,'dragonite'],[null,null,null],['lucario',null,null],['greninja','charizard',null],['meowscarada',null,null],['scizor','gardevoir','gengar'],[null,null,null]][i];
  room.guest=i===2||i===5?guest:null;
});
const root=document.querySelector('#view');
const overlay=document.querySelector('#overlay');
const validPages=['lobby','lineup','pk'];
const params=new URLSearchParams(location.search);
const initialPage=params.get('view');
const state={page:validPages.includes(initialPage)?initialPage:'lobby',slot:0,move:0,rival:'RivalProbe',stake:200,ready:false,editing:true,owner:true,opponent:rival,modalSlot:0,modal:null,category:'all',query:''};
const money=new Intl.NumberFormat('zh-CN');
const art=(id,cls='',title='')=>`<img class="${cls}" src="assets/${id}.png" alt="${title}">`;
const item=id=>id?art(id,'itemicon',names[id]):'<span class="empty-item" aria-hidden="true">—</span>';
const button=(action,label,cls='',id='',disabled=false)=>`<button class="button ${cls}" data-action="${action}" data-id="${id}" ${disabled?'disabled':''}>${label}</button>`;
const heading=(eyebrow,title,right='')=>`<div class="head"><div><span class="micro">${eyebrow}</span><h1>${title}</h1></div>${right}</div>`;
const wallet='<span class="wallet">余额 <b>1,200</b> 宝可币</span>';
const icon=(name,label)=>`<span class="glyph" aria-hidden="true">${name}</span><span class="visually-hidden">${label}</span>`;
const unknown='<span class="pending-icon" aria-hidden="true">?</span>';
function card(p,i,other){
  if(!p){
    const body=`${unknown}<span class="order">${i+1}P</span><span class="species">${other?'待定':'点此选择'}</span>`;
    return `<article class="pick empty-slot">${other?`<div class="slot-face">${body}</div>`:`<button class="slot-face pulse" data-action="species-open" data-id="${i}" aria-label="选择 ${i+1}P 宝可梦" ${state.ready?'disabled':''}>${body}</button>`}</article>`;
  }
  const selected=!other&&i===state.slot;
  const pict=`<span class="portrait">${art('head-'+p.id)}</span><span class="order">${i+1}P</span><strong class="species">${dex[p.id][0]}</strong>`;
  const itemTitle=p.item?`更换携带物：${names[p.item]}`:'选择携带物';
  const slotTitle=`${i+1}P ${dex[p.id][0]}`;
  const swap=(action,cls,label,content)=>`<button class="slot-tool ${cls}" data-action="${action}" data-id="${i}" title="${label}" aria-label="${label}" ${state.ready?'disabled':''}>${content}</button>`;
  return `<article class="pick ${selected?'selected':''}">${other?`<div class="slot-face">${pict}</div><span class="rival-item" title="${names[p.item]}">${item(p.item)}</span>`:`<button class="slot-face" data-action="slot" data-id="${i}" aria-pressed="${selected}" aria-label="配置 ${slotTitle} 的招式">${pict}</button>${swap('species-open','swap','更换宝可梦',icon('⇄','更换宝可梦'))}${swap('item-open',`held ${p.item||state.ready?'':'pulse'}`,itemTitle,p.item?item(p.item):'+')}`}</article>`;
}
function side(name,t,other){return `<section class="side ${other?'right':'left'}"><div class="tag"><b>${name}</b><small>${other?(t.some(Boolean)?'对手的队伍':'空位'):state.ready?'已准备':'我的队伍'}</small></div><div class="roster">${t.map((p,i)=>card(p,i,other)).join('')}</div></section>`;}
function moveRow(id,i,candidate=false){
  const [label,type,glyph,power,pp]=moves[id];
  return `<button class="move" data-action="${candidate?'replace':'move'}" data-id="${candidate?id:i}" ${candidate?'':`aria-pressed="${state.move===i}"`}><small>${candidate?'→':i+1}</small><span class="type ${type}">${glyph}</span><strong>${label}</strong><small>${power}</small><small>${pp} PP</small></button>`;
}
function editor(){
  const p=team[state.slot];
  if(!p)return '';
  const candidates=(p.id==='pikachu'?['thunder','volt_switch','tail',...dex[p.id][2]]:[...dex[p.id][2],'quick','swift','tail']).filter((id,i,a)=>a.indexOf(id)===i&&!p.moves.includes(id));
  return `<div class="editor"><div class="editor-head"><b>${state.slot+1}P · ${dex[p.id][0]}</b><button class="editor-toggle" data-action="toggle-moves" aria-expanded="${state.editing}" aria-controls="move-body">配招 <span aria-hidden="true">${state.editing?'▴':'▾'}</span></button></div><div class="editor-body" id="move-body" ${state.editing?'':'hidden'}><section><h3>当前招式</h3><div class="moves">${p.moves.map((m,i)=>moveRow(m,i)).join('')}</div></section><section><h3>替换第 ${state.move+1} 招 <span class="muted">／ 威力 · PP</span></h3><div class="moves">${candidates.length?candidates.slice(0,4).map(id=>moveRow(id,0,true)).join(''):'<p class="hint">暂无可替换的示例招式。</p>'}</div><p class="hint">先选当前招式，再点右侧候选招式。</p></section></div></div>`;
}
function lineup(){
  const selected=team.some(Boolean);
  return `<section class="screen">${heading('TEAM SELECT','配置出战阵容',`<div class="head-actions">${wallet}${button('help','<b aria-hidden="true">?</b> 帮助')}</div>`)}<div class="room-nav">${button('leave','‹ 离开房间')}</div><div class="match">${side('ArenaProbe',team,false)}<div class="rail"><em>VS</em><small>本场赌注</small><strong>$${money.format(state.stake)}</strong></div>${side(state.rival,state.opponent,true)}</div>${editor()}<div class="match-foot"><div class="actions">${button('ready',state.ready?'取消准备':'准备','', '',!selected)}${state.owner?button('page','开始对战','primary','pk',!state.ready||!state.opponent.some(Boolean)):''}</div></div></section>`;
}
function lobby(){
  const seat=(p,mirror)=>`<span class="room-seat ${mirror?'mirror':''}">${p?art('face-'+p.skin,'face'):unknown}<strong>${p?p.name:'等待对手'}</strong><span class="room-party">${(p?.picks??[null,null,null]).map(id=>id?art('head-'+id,'room-mon',dex[id][0]):unknown).join('')}</span></span>`;
  const card=(r,i)=>`<button class="room" data-action="join" data-id="${i}" ${r.guest?'disabled':''} aria-label="${r.name}，入场价 ${r.stake} 宝可币${r.guest?'，已满员':''}">${seat(r,false)}<span class="room-center"><b>VS</b><strong>$${money.format(r.stake)}</strong><small>${r.guest?'已满员':'可加入'}</small></span>${seat(r.guest,true)}</button>`;
  return `<section class="screen lobby-screen">${heading('ONLINE LOBBY','公开房间',`<div class="head-actions">${wallet}${button('create','<b aria-hidden="true">＋</b> 创建房间','primary')}</div>`)}<div class="lobby-meta"><span>${rooms.filter(r=>!r.guest).length} 间可加入</span><span>1 对 1 · 每人可带 1–3 只宝可梦</span></div><div class="room-grid">${rooms.map(card).join('')}</div></section>`;
}
function pk(){const squad=(name,t,other)=>`<section class="side pk-side ${other?'right':'left'}"><h2>${name}</h2><div class="pk-cards">${t.map((p,i)=>`<article class="battle-card"><span class="order">${i+1}P</span><div class="artbox">${p?art(p.id,'battle-art'):unknown}</div><b>${p?dex[p.id][0]:'待定'}</b></article>`).join('')}</div></section>`;return `<section class="screen pk-screen">${heading('MATCH INTRO','阵容对决',button('page','返回配队','','lineup'))}<div class="duel">${squad('ArenaProbe',team,false)}<div class="vs">VS</div>${squad(state.rival,state.opponent,true)}</div><div class="pk-status"><span>赌注 <strong>$${money.format(state.stake)}</strong></span><div class="meter"></div><b>载入对局</b></div></section>`;}

function render(){
  root.innerHTML=({lobby,lineup,pk})[state.page]();
  document.querySelectorAll('[data-page]').forEach(el=>{
    if(el.dataset.page===state.page)el.setAttribute('aria-current','page');
    else el.removeAttribute('aria-current');
  });
}
function page(next){
  state.page=next;
  const url=new URL(location.href);url.searchParams.set('view',next);url.searchParams.delete('panel');
  history.replaceState(null,'',url);render();
}
const helpRows=[['⇄','切换宝可梦','点空槽选择；已有宝可梦时，使用卡片右上角的双箭头。'],['▤','调整配招','点击头图选中出战位，在展开栏左侧点一条当前招式，再从右侧选择替换招式。'],['◆','更换携带物','点击卡片右下角的物品格。可以搜索物品，或切到 Mega 进化石分类；也可以卸下道具。']];
const filters=[['all','全部'],['held','普通携带物'],['mega','Mega 进化石']];
const clean=id=>id.replaceAll('_',' ');
function choices(){
  const p=team[state.modalSlot],isSpecies=state.modal==='species';
  let entries;
  if(isSpecies)entries=[...(p?[['','移除宝可梦','']]:[]),...Object.entries(dex).map(([id,[name]])=>[id,name,'head-'+id])];
  else {
    const shown=state.category==='mega'?stones:state.category==='held'?items:names;
    entries=Object.entries(shown).map(([id,name])=>[id,name,id]);
  }
  const query=state.query.trim().toLowerCase();
  return entries.filter(([id,name])=>`${name} ${id} ${clean(id)}`.toLowerCase().includes(query)).map(([id,name,src])=>{
    const selected=isSpecies?p?.id===id:p?.item===id;
    const used=isSpecies&&team.some((m,i)=>i!==state.modalSlot&&m?.id===id);
    const blocked=used||(!isSpecies&&id in stones&&team.some((m,i)=>i!==state.modalSlot&&m?.item in stones));
    return `<button class="choice ${selected?'is-current':''}" data-action="choose" data-id="${id}" ${blocked?'disabled':''} aria-pressed="${selected}">${src?art(src):unknown}<strong>${name}</strong>${selected?'<small>当前</small>':used?'<small>已出战</small>':''}</button>`;
  }).join('')||'<p class="no-results">没有符合条件的结果。</p>';
}
function updateChoices(){overlay.querySelector('#choices').innerHTML=choices();}
function modalContents(){
  const selector=state.modal==='species'||state.modal==='item';
  const p=team[state.modalSlot];
  const titles={species:'切换宝可梦',item:'选择携带物',help:'操作说明',create:'创建房间'};
  let body;
  if(selector){
    const species=state.modal==='species';
    body=`<div class="modal-tools"><label class="visually-hidden" for="search">${species?'搜索宝可梦':'搜索携带物'}</label><input id="search" type="search" placeholder="${species?'搜索宝可梦名称':'搜索物品名称'}" autofocus>${!species?button('unequip','卸下携带物'):''}</div>${species?'':`<div class="filters" role="group" aria-label="携带物分类">${filters.map(([id,label])=>`<button class="button" data-action="filter" data-id="${id}" aria-pressed="${state.category===id}">${label}</button>`).join('')}</div>`}<div class="choices ${species?'species-choices':''}" id="choices">${choices()}</div>${species?'':'<p class="dialog-note">队伍最多携带一枚 Mega 进化石。</p>'}`;
  }else if(state.modal==='create'){
    body='<form id="create" class="create-form"><label for="stake">入场价（宝可币）</label><input id="stake" type="number" min="0" max="1200" step="1" value="200" autofocus required><p class="dialog-note">对手加入时支付同样的入场价。设为 0 即可免费入场。</p><button class="button primary" type="submit">创建</button></form>';
  }else{
    body=`<div class="help-list">${helpRows.map(([glyph,title,note])=>`<section><span aria-hidden="true">${glyph}</span><div><h3>${title}</h3><p>${note}</p></div></section>`).join('')}</div><div class="dialog-actions">${button('dismiss','知道了','primary')}</div>`;
  }
  return `<div class="dialog-head"><div><small>${selector?`${state.modalSlot+1}P · ${p?dex[p.id][0]:'待定'}`:'BATTLE FRONTIER'}</small><h2 id="dialog-title">${titles[state.modal]}</h2></div><button class="slot-tool dialog-close" data-action="dismiss" aria-label="关闭" title="关闭">×</button></div><div class="dialog-content">${body}</div>`;
}
function openModal(type,category='all'){
  state.modal=type;state.category=category;state.query='';
  overlay.className=`overlay overlay-${type}`;
  overlay.innerHTML=modalContents();overlay.showModal();
}
function dismiss(action,restoreSlot=false){
  overlay.close();state.modal=null;
  if(restoreSlot){render();root.querySelector(`[data-action="${action}"][data-id="${state.slot}"]`)?.focus();}
}
overlay.addEventListener('close',()=>{state.modal=null;});
document.addEventListener('click',event=>{
  const b=event.target.closest('button');if(!b||b.disabled)return;
  if(b.dataset.page){page(b.dataset.page);return;}
  const {action,id}=b.dataset;
  switch(action){
    case 'page':page(id);return;
    case 'leave':state.ready=false;page('lobby');return;
    case 'join':{
      const room=rooms[+id];
      team.fill(null);state.slot=0;state.rival=room.name;state.opponent=room.picks.map(p=>p?makePick(p):null);
      state.stake=room.stake;state.owner=false;state.ready=false;page('lineup');return;
    }
    case 'slot':state.slot=+id;state.move=0;state.editing=true;break;
    case 'species-open':case 'item-open':state.modalSlot=+id;openModal(action.split('-')[0]);return;
    case 'create':case 'help':openModal(action);return;
    case 'dismiss':dismiss();return;
    case 'filter':
      state.category=id;
      overlay.querySelectorAll('[data-action="filter"]').forEach(el=>el.setAttribute('aria-pressed',el.dataset.id===id));
      updateChoices();return;
    case 'choose':{
      const species=state.modal==='species';
      state.slot=state.modalSlot;state.move=0;
      if(species&&team[state.slot]?.id!==id)team[state.slot]=id?{...makePick(id),item:''}:null;
      else if(!species)team[state.slot].item=id;
      dismiss(species?'species-open':'item-open',true);return;
    }
    case 'unequip':state.slot=state.modalSlot;state.move=0;team[state.slot].item='';dismiss('item-open',true);return;
    case 'toggle-moves':state.editing=!state.editing;break;
    case 'move':state.move=+id;break;
    case 'replace':if(!state.ready)team[state.slot].moves[state.move]=id;break;
    case 'ready':if(team.some(Boolean))state.ready=!state.ready;break;
    default:return;
  }
  render();root.querySelector(`[data-action="${action}"]${id?`[data-id="${id}"]`:''}`)?.focus({preventScroll:true});
});
overlay.addEventListener('submit',event=>{
  if(event.target.id!=='create')return;
  event.preventDefault();state.stake=Number(overlay.querySelector('#stake').value);state.rival='等待对手';state.opponent=[null,null,null];state.owner=true;state.ready=false;state.slot=0;team.fill(null);dismiss();page('lineup');
});
overlay.addEventListener('input',event=>{if(event.target.id==='search'){state.query=event.target.value;updateChoices();}});
if(params.get('empty')==='1'){team.fill(null);state.opponent=[null,null,null];state.rival='等待对手';}
render();
const panel=params.get('panel');
if(state.page==='lineup'&&['species','item','mega','help'].includes(panel))openModal(panel==='mega'?'item':panel,panel==='mega'?'mega':'all');
if(state.page==='lobby'&&panel==='create')openModal('create');
