'use strict';
const $=s=>document.querySelector(s), view=$('#view'), modal=$('#modal');
const assets='../frontier-console/assets/', cash=n=>`$${n.toLocaleString('zh-CN')}`;
const people=[['ArenaProbe','steve'],['Moss','alex'],['Kite','kai'],['Nova','ari'],['Atlas','efe'],['Sora','noor'],['Rune','sunny'],['Lyra','makena'],['Clover','alex'],['Ember','ari'],['Indigo','noor']].map(([name,skin],id)=>({id,name,skin}));
const me=people[0], [_,moss,kite,nova,atlas,sora,rune]=people;
const catalog=[['pikachu','皮卡丘'],['gengar','耿鬼'],['lucario','路卡利欧'],['scizor','巨钳螳螂'],['charizard','喷火龙'],['greninja','甲贺忍蛙']];
const prizeRates=n=>n===3?[60,25,15]:[50,25,15,10];
let filter='all', room=null, tournament=null, page='lobby', picks=[], notice='', incoming=null, phaseTimer=0, roomSeq=5;
const inviteCooldowns=new Map();
let rooms=sample();
function sample(){return [
  {id:1,type:'solo',stake:200,members:[moss],ready:new Set(),invited:new Set()},
  {id:2,type:'solo',stake:500,members:[nova,rune],ready:new Set(),invited:new Set(),picks:true},
  {id:3,type:'multi',stake:400,members:[atlas,sora,kite,nova],ready:new Set([atlas.id,sora.id]),invited:new Set()},
  {id:4,type:'multi',stake:800,members:[rune,moss],ready:new Set([rune.id]),invited:new Set()}
];}
// Sample cards represent separate sessions; for the active demo only its own roster and the simulated instance state count.
const btn=(act,label,cls='',id='',off=false)=>`<button data-action="${act}" data-id="${id}" class="${cls}" ${off?'disabled':''}>${label}</button>`;
const avatar=(p,cls='')=>p?`<img class="avatar ${cls}" src="${assets}face-${p.skin}.png" alt="">`:`<span class="avatar empty ${cls}" aria-hidden="true">?</span>`;
const head=(p,cls='mini')=>p?`<img class="${cls}" src="${assets}head-${p[0]}.png" alt="${p[1]}">`:`<span class="${cls}" aria-label="待选">?</span>`;
const title=(tag,text,actions='')=>`<div class="title"><div><small>${tag}</small><h1>${text}</h1></div><div class="row">${actions}</div></div>`;
const filtered=(id,label)=>`<button data-action="filter" data-id="${id}" aria-pressed="${filter===id}">${label}</button>`;
const cap=r=>r.type==='solo'?2:r.maxPlayers??8;
function card(r){
  const seat=(p,half)=>`<span>${avatar(p)}<strong>${p?.name??'等待对手'}</strong><span class="room-party">${(r.picks?catalog.slice(half*3,half*3+3):[null,null,null]).map(p=>head(p)).join('')}</span></span>`;
  const middle=r.type==='solo'?`<div class="room-versus">${seat(r.members[0],0)}<b>VS</b>${seat(r.members[1],1)}</div>`:`<div class="row" aria-label="已入场玩家">${r.members.slice(0,5).map(p=>avatar(p,'tiny')).join('')}${r.members.length>5?`<b>+${r.members.length-5}</b>`:''}</div>`;
  return `<button class="room" data-action="join" data-id="${r.id}" aria-label="${r.members[0].name} 的${r.type==='solo'?'1v1':'多人'}房，入场价 ${r.stake} 宝可币" ${r.members.length===cap(r)?'disabled':''}><span class="room-head"><span class="chip ${r.type==='multi'?'multi':''}">${r.type==='solo'?'1v1':'多人淘汰赛'}</span><strong class="currency">${cash(r.stake)}</strong></span>${middle}${r.type==='multi'?`<p class="lobby-note">${r.members[0].name} · ${r.members.length} / ${cap(r)} 人</p>`:''}</button>`;
}
function lobby(){return title('ROOM DIRECTORY','房间大厅',btn('create','＋ 创建房间','primary'))+`<div class="toolbar"><div class="filters" aria-label="房间筛选">${filtered('all','全部')}${filtered('solo','1v1')}${filtered('multi','多人')}</div><span class="state">先列 1v1，再列多人</span></div><div class="room-grid">${rooms.filter(r=>filter==='all'||r.type===filter).sort((a,b)=>(a.type==='multi')-(b.type==='multi')||a.id-b.id).map(card).join('')||'<p>暂时没有符合条件的房间。</p>'}</div><p class="lobby-note">入场价按人计算。多人房确认开赛后抽签，每场仍是 1v1。</p><div class="dev">${btn('mock-incoming','模拟收到聊天邀请','quiet')}</div>`;}
function slot(p){return `<div class="slot">${avatar(p)}<b>${p?.name??'等待加入'}${p&&p.id===room.members[0].id?' <span class="chip">房主</span>':''}</b><span class="state ${p&&room.ready.has(p.id)?'ok':''}">${p?(room.ready.has(p.id)?'已准备':'未准备'):'空位'}</span></div>`;}
function waiting(){
  const n=room.members.length, max=cap(room), split=Math.ceil(max/2), all=n>=3&&room.members.every(p=>room.ready.has(p.id)), owner=room.members[0]===me;
  return `<div class="back">${btn('leave','‹ 退出房间','quiet')}</div>`+title('TOURNAMENT LOBBY','多人准备房',btn('invite','＋ 邀请玩家','primary'))+`<div class="toolbar"><span class="chip multi">${n} / ${max} 人 · 至少 3 人开赛</span><span>每人 <b class="currency">${cash(room.stake)}</b>　预计奖金池 <b class="currency">${cash(n*room.stake)}</b></span></div><div class="seats">${[0,split].map(i=>`<section class="recess" aria-label="${i?'右侧':'左侧'}候场席">${Array.from({length:i?max-split:split},(_,j)=>slot(room.members[i+j])).join('')}</section>`).join('')}</div><div class="bar">${btn('ready-room',room.ready.has(me.id)?'取消准备':'准备','primary')}${owner?btn('start','开始赛程','','',!all):''}</div><div class="dev row">模拟多人：${btn('fill','补到 3 人','quiet',3,n>=3)}${btn('fill','补到 5 人','quiet',5,n>=5||max<5)}${btn('new-join','一位玩家入场','quiet','',n>=max)}${btn('other-ready','其他玩家准备','quiet')}${!owner?btn('start','模拟房主开赛','quiet','',!all):''}</div>`;
}
function match(a=null,b=null){return {a,b,winner:null,loser:null,status:'pending'};}
function finish(m,winner,bye=false){m.winner=winner;m.loser=winner===m.a?m.b:m.a;m.status=bye?'bye':'done';}
function launch(){
  if(!room||room.members.length<3||!room.members.every(p=>room.ready.has(p.id)))return;
  // Reproducible seed order for the demo; the server design calls for a random draw.
  const seed=[me,...room.members.filter(p=>p!==me)];
  const layout=seed.length<=4?[0,3,1,2]:[0,7,3,4,1,6,2,5];
  const first=[];for(let i=0;i<layout.length;i+=2){const m=match(seed[layout[i]]??null,seed[layout[i+1]]??null);if(!m.a||!m.b)finish(m,m.a??m.b,true);first.push(m);}
  const finals=seed.length===3?[match()]:[match(),match()];
  const rounds=first.length===4?[first,[match(),match()],finals]:[first,finals];
  tournament={rounds,round:0,pool:seed.length*room.stake,rates:prizeRates(seed.length),third:null,breadcrumb:'抽签结果',rivalReady:false,eliminated:false,done:false};
  showRound('抽签结果');
}
function showRound(caption){
  clearTimeout(phaseTimer);picks=[];tournament.rivalReady=false;tournament.breadcrumb=caption;
  if(page==='hidden') {suspended='draw';render();}
  else {page='draw';render();}
  phaseTimer=setTimeout(enterRound,3600);
}
function active(){return tournament?.rounds[tournament.round].find(m=>m.a===me||m.b===me);}
function enterRound(){
  clearTimeout(phaseTimer);if(!tournament||page!=='draw'&&page!=='hidden')return;
  const m=active(), playing=m&&!m.winner&&!tournament.eliminated;
  if(page==='hidden'){suspended=playing?'lineup':'between';render();return;}
  page=playing?'lineup':'between';render();
}
function prizeValues(pool,rates){const rest=rates.slice(1).map(n=>Math.floor(pool*n/100));return [pool-rest.reduce((a,b)=>a+b,0),...rest];}
function nextRound(){
  const t=tournament,current=t.rounds[t.round];if(!current.every(m=>m.status==='done'||m.status==='bye'))return;
  if(t.round===t.rounds.length-1){t.done=true;page='awards';render();return;}
  const next=t.rounds[++t.round];
  if(t.round===t.rounds.length-1){
    next[0].a=current[0].winner;next[0].b=current[1].winner;
    if(next[1]){next[1].a=current[0].loser;next[1].b=current[1].loser;}
    else t.third=current[0].loser??current[1].loser;
  }
  else next.forEach((m,i)=>{m.a=current[i*2].winner;m.b=current[i*2+1].winner;});
  showRound(t.round===t.rounds.length-1?(next[1]?'决赛与季军赛开始':'决赛开始'):'赛程进入下一轮');
}
function bracket(){
  const t=tournament,names=t.rounds.length===3?['首轮','半决赛','决赛轮']:['半决赛','决赛轮'];
  const matchHtml=(m,i,final)=>`<div class="match ${m.a===me||m.b===me?'mine':''} ${final&&i===1?'bronze':''}"><small>${final?(i===0?'冠军赛':'季军赛'):`对局 ${i+1}`} · ${{pending:m.a&&m.b?'待配队':'待定',ready:'配队中',fight:'战斗中',done:'已结束',bye:'轮空'}[m.status]}</small>${[m.a,m.b].map(p=>`<div class="match-player ${p&&p===m.winner?'win':p&&p===m.loser?'loss':''}">${avatar(p,'tiny')}<b>${p?.name??'—'}</b>${p&&p===m.winner?`<strong>${m.status==='bye'?'轮空晋级':'胜'}</strong>`:''}</div>`).join('')}</div>`;
  return `<p>奖金池 <b class="currency">${cash(t.pool)}</b>　｜　高亮为你的对局</p><div class="bracket-wrap"><div class="bracket" style="--columns:${names.length}">${t.rounds.map((round,i)=>`<section><h3>${names[i]} ${i===t.round?'<span class="chip">当前轮</span>':''}</h3><div class="match-stack">${round.map((m,j)=>`<div class="match-cell ${i<names.length-1?'connector':''} ${i===names.length-2?'final-feed':''}">${matchHtml(m,j,i===names.length-1)}</div>`).join('')}</div></section>`).join('')}</div></div>`;
}
function controls(){const m=active(), current=tournament.rounds[tournament.round], unfinished=current.some(m=>m.status==='pending'||m.status==='ready'||m.status==='fight');return `<div class="dev row">模拟赛程：${btn('other-finish','结算其它对局','quiet','',!current.some(x=>x!==m&&!x.winner))}${btn('advance','进入下一轮／结算','quiet','',unfinished)}</div>`;}
function draw(){return title('LIVE BRACKET',tournament.breadcrumb,btn('hide','‹ 返回世界','quiet')+btn('enter','确认','primary'))+`<section class="recess">${bracket()}</section>`;}
function between(){const m=active(), status=tournament.eliminated?'已淘汰 · 仍可查看赛程':m?.status==='bye'?'本轮轮空 · 已晋级':m?.winner===me?'本场获胜 · 等待本轮结算':'等待本轮结算';return title('LIVE BRACKET','赛程进度',btn('hide','‹ 返回世界','quiet'))+`<div class="toolbar"><span class="chip">${status}</span></div><section class="recess">${bracket()}</section>${controls()}`;}
function lineup(){
  const m=active(), rival=m?(m.a===me?m.b:m.a):room.members.find(p=>p!==me), tour=!!tournament;
  const admitted=tour?m?.status==='ready'||m?.status==='fight':room.ready.has(me.id);
  const rivalReady=tour?tournament.rivalReady:rival&&room.ready.has(rival.id);
  const side=(p,other)=>`<section class="recess ${other?'rival-side':''}" style="text-align:center"><div class="row" style="justify-content:center">${avatar(p)}<b>${p?.name??'等待对手'}</b><span class="chip">${(other?rivalReady:admitted)?'已准备':'未准备'}</span></div><div class="duel-party">${Array.from({length:3},(_,i)=>`<div class="duel-slot"><span class="slot-number">${i+1}P</span>${other?head(rivalReady?catalog[i+3]:null,'mon'):btn('pick',head(picks[i],'mon'),!picks[i]&&!admitted?'ring':'quiet',i,admitted)}</div>`).join('')}</div></section>`;
  return title('1V1 TEAM SELECT',tour?'淘汰赛 · 阵容配置':'1v1 · 阵容配置',btn(tour?'hide':'leave',tour?'返回世界':'退出房间','quiet')+btn('invite','＋ 邀请玩家','quiet')+(tour?btn('bracket','查看赛程','primary'):''))+`<div class="toolbar"><span>入场价 <b class="currency">${cash(room.stake)}</b></span><span class="state">${tour?'双方准备后自动开战':'沿用现有 1v1 开赛流程'}</span></div><div class="duel">${side(me,false)}${side(rival,true)}</div><div class="bar">${btn('ready-duel',admitted?'取消准备':'准备','primary','',!picks.length)}${!tour&&room.members[0]===me?btn('solo-start','开始对局','','',!admitted||!rivalReady):''}</div><div class="dev row">模拟多人：${rival?btn('rival-ready','对手准备','quiet','',!!rivalReady):btn('new-join','一位玩家入场','quiet')}</div>`;
}
function fight(){return title('LIVE MATCH','对战进行中',btn('bracket','查看赛程','quiet','',!tournament))+`<div class="notice">${tournament?'双方已准备，自动进入实例对战。':'房主已开始对局。'} 此处模拟 1v1 战斗。</div><div class="dev row">${btn('win','模拟我方获胜')}${btn('lose','模拟我方落败')}</div>`;}
function awards(){const [final,bronze]=tournament.rounds.at(-1), winners=[final.winner,final.loser,...(bronze?[bronze.winner,bronze.loser]:[tournament.third])], {pool,rates}=tournament, prizes=prizeValues(pool,rates);return title('FINAL STANDINGS','赛程结束',btn('bracket','查看完整赛程'))+`<p>奖金池总额 <b class="currency">${cash(pool)}</b>，按名次自动发放。</p><div class="reward-list">${winners.map((p,i)=>`<div class="payout"><span class="chip">第 ${i+1} 名</span>${avatar(p)}<b>${p?.name??'—'}</b><span class="state">${rates[i]}%</span><b class="currency">${cash(prizes[i])}</b></div>`).join('')}</div><div class="bar">${btn('leave','返回大厅','primary')}</div>`;}
function hidden(){return title('WORLD VIEW','世界活动中')+`<p>这是挂起状态示意。<kbd>R</kbd> 可以恢复赛程界面。</p>${controls()}`;}
function render(){
  view.innerHTML=({lobby,waiting,draw,lineup,fight,awards,hidden,between}[page]??lobby)();
  $('#pin').innerHTML=page==='hidden'?`<div class="pin-card"><small>对战开拓区</small><div>${active()?.status==='bye'?'本轮轮空':'赛程已挂起'}　${btn('resume','R · 返回赛程','quiet')}</div></div>`:'';
  if(page==='lineup'&&!tournament){const leave=view.querySelector('[data-action="leave"]');const back=document.createElement('div');back.className='back';back.append(leave);view.prepend(back);}
  $('#chat').innerHTML=incoming?`<div class="chat-msg">${btn('chat-close','×','chat-close')}<span class="state">[系统]</span> ${incoming.members[0].name} 邀请你加入${incoming.type==='solo'?' 1v1 ':'多人'}房间（${cash(incoming.stake)}）。${btn('accept','[接受邀请]','chat-link')}</div>`:'';
}
function open(tag,heading,body,wide=false){
  modal.className=wide?'wide':'';
  modal.innerHTML=`<div class="title"><div><small>${tag}</small><h2 id="modal-title">${heading}</h2></div>${btn('dismiss','×','quiet')}</div><div class="content">${body}</div>`;
  if(!modal.open)modal.showModal();
}
function alertBox(text){open('FIELD TERMINAL','提示',`<p>${text}</p><div class="bar">${btn('dismiss','确定','primary')}</div>`);}
function newRoom(type,stake,members=[me],maxPlayers=8){return {id:roomSeq++,type,stake,members,maxPlayers,ready:new Set(),invited:new Set()};}
function join(r){
  if(!r||r.members.length===cap(r)||room)return;
  room=r;room.members.push(me);incoming=null;picks=[];page=r.type==='solo'?'lineup':'waiting';render();
}
function available(){return people.filter(p=>p!==me&&p.name!=='Indigo'&&!rooms.some(r=>r.members.includes(p))&&!room?.members.includes(p));}
function inviteRows(q=''){
  const eligible=available(), candidates=people.filter(p=>(eligible.includes(p)||room.invited.has(p.id))&&p.name.toLowerCase().includes(q.trim().toLowerCase()));
  return candidates.length?candidates.map(p=>{
    const sent=room.invited.has(p.id), cooldown=(inviteCooldowns.get(p.id)??0)>Date.now();
    return `<div class="invite-row">${avatar(p)}<b>${p.name}</b>${btn('send',sent?'已邀请':cooldown?'冷却中':'邀请','',p.id,sent||cooldown||room.members.length>=cap(room))}</div>`;
  }).join(''):'<p>没有符合条件的空闲玩家。</p>';
}
function refreshInvites(){const search=$('#search'), list=$('#invite-list');if(modal.open&&search&&list)list.innerHTML=inviteRows(search.value);}
function invite(){
  if(tournament)return alertBox('名单已锁定，开赛后不能再邀请玩家。');
  if(room.members.length>=cap(room))return alertBox('房间已满。');
  open('ONLINE PLAYERS','邀请玩家','<label for="search">按玩家名搜索</label><input type="search" id="search" placeholder="输入玩家名" autocomplete="off"><div id="invite-list">'+inviteRows()+'</div>');
}
function simulateGuest(){const p=people.find(p=>p!==me&&!room.members.includes(p));if(p&&room.members.length<cap(room))room.members.push(p);}
function queueNext(){
  clearTimeout(phaseTimer);
  if(tournament.rounds[tournament.round].every(m=>m.winner))phaseTimer=setTimeout(()=>{modal.close();nextRound();},1500);
}
let suspended='';
document.addEventListener('click',e=>{
  const b=e.target.closest('[data-action]');if(!b||b.disabled)return;
  const a=b.dataset.action,id=b.dataset.id;
  switch(a){
    case 'reset': location.reload();return;
    case 'filter':filter=id;break;
    case 'create':return open('ROOM OPTIONS','创建房间',`<form id="create"><fieldset><legend>对战模式</legend><label class="option"><input type="radio" name="mode" value="solo" checked> 1v1</label><label class="option"><input type="radio" name="mode" value="multi"> 多人淘汰赛</label></fieldset><div class="room-settings"><label>每人入场价（宝可币）<input id="stake" name="stake" type="number" value="400" required min="0" max="1000000" step="1"></label><label id="capacity" hidden>人数上限<input name="maxPlayers" type="number" min="4" max="8" value="8" required disabled></label></div><button class="primary" type="submit">创建房间</button></form>`);
    case 'join':return join(rooms.find(r=>r.id===+id));
    case 'leave':
      clearTimeout(phaseTimer);if(room){room.members=room.members.filter(p=>p!==me);room.ready.delete(me.id);}
      room=null;tournament=null;picks=[];page='lobby';modal.close();break;
    case 'dismiss':modal.close();return;
    case 'invite':return invite();
    case 'send': {
      const p=available().find(p=>p.id===+id);
      if(!room||tournament||!p||room.members.length>=cap(room)||room.invited.has(p.id)||(inviteCooldowns.get(p.id)??0)>Date.now())return refreshInvites();
      room.invited.add(p.id);inviteCooldowns.set(p.id,Date.now()+60000);
      setTimeout(()=>{if(room)refreshInvites();},60000);
      return refreshInvites();
    }
    case 'mock-incoming':incoming=newRoom('multi',450,[sora,kite,atlas]);break;
    case 'chat-close':incoming=null;break;
    case 'accept':return join(incoming);
    case 'fill':while(room.members.length<Math.min(+id,cap(room)))simulateGuest();break;
    case 'new-join':simulateGuest();break;
    case 'other-ready':room.members.filter(p=>p!==me).forEach(p=>room.ready.add(p.id));break;
    case 'ready-room':room.ready.has(me.id)?room.ready.delete(me.id):room.ready.add(me.id);break;
    case 'start':return launch();
    case 'enter':return enterRound();
    case 'bracket':if(tournament)open('LIVE BRACKET','赛程进度',bracket(),true);return;
    case 'hide':if(!tournament)return;suspended=page;page='hidden';break;
    case 'resume':page=suspended;break;
    case 'pick':
      return open('1V1 TEAM SELECT',`选择第 ${+id+1} 位宝可梦`,`<div class="picklist">${catalog.map((p,i)=>btn('choose',head(p)+p[1],'',`${id},${i}`,picks.some((x,j)=>x===p&&j!==+id))).join('')}</div>`);
    case 'choose': {const [slot,i]=id.split(',').map(Number);picks[slot]=catalog[i];modal.close();break;}
    case 'ready-duel': {
      if(!picks.some(Boolean))break;
      const m=active();if(!m){room.ready.has(me.id)?room.ready.delete(me.id):room.ready.add(me.id);break;}
      m.status=m.status==='ready'?'pending':'ready';
      if(m.status==='ready'&&tournament.rivalReady){m.status='fight';page='fight';}break;
    }
    case 'rival-ready':
      if(!tournament){const rival=room.members.find(p=>p!==me);if(rival)room.ready.add(rival.id);break;}
      tournament.rivalReady=true;if(active().status==='ready'){active().status='fight';page='fight';}break;
    case 'solo-start':page='fight';break;
    case 'win':case 'lose': {
      const m=active();if(!m){page='lineup';room.ready.delete(me.id);return render();}
      finish(m,a==='win'?me:(m.a===me?m.b:m.a));
      // A semifinal loss leads to the third-place match, not immediate elimination.
      tournament.eliminated=a==='lose'&&(tournament.rates.length===3||tournament.round<tournament.rounds.length-2);
      page='between';queueNext();break;
    }
    case 'other-finish':tournament.rounds[tournament.round].filter(m=>m!==active()&&!m.winner).forEach(m=>finish(m,m.a));queueNext();break;
    case 'advance':clearTimeout(phaseTimer);return nextRound();
    default:return;
  }
  render();
});
modal.addEventListener('submit',e=>{
  if(e.target.id!=='create')return;e.preventDefault();
  const form=new FormData(e.target);room=newRoom(form.get('mode'),Number(form.get('stake')),[me],Number(form.get('maxPlayers')??8));picks=[];page=room.type==='solo'?'lineup':'waiting';modal.close();render();
});
modal.addEventListener('input',e=>{
  if(e.target.id==='search')refreshInvites();
  if(e.target.name==='mode'){
    const multi=e.target.value==='multi';$('#capacity').hidden=!multi;$('#capacity input').disabled=!multi;
  }
});
document.addEventListener('keydown',e=>{
  if(modal.open||e.target.matches('input,textarea')||e.repeat)return;
  if(e.key==='Escape'&&['draw','between','lineup'].includes(page)&&tournament){suspended=page;page='hidden';render();}
  if(e.key.toLowerCase()==='r'&&page==='hidden'){page=suspended;render();}
});
const demo=new URLSearchParams(location.search).get('demo');
if(['ready','bracket','bye','lineup','awards','awards3'].includes(demo)){
  room=newRoom('multi',400,people.slice(0,demo==='awards3'?3:demo==='lineup'?4:5));page='waiting';
  if(demo!=='ready'){
    room.members.forEach(p=>room.ready.add(p.id));launch();
    if(demo!=='bracket')enterRound();
    if(demo.startsWith('awards')){
      while(!tournament.done){const t=tournament;t.rounds[t.round].filter(m=>!m.winner).forEach(m=>finish(m,m.a));nextRound();}
      clearTimeout(phaseTimer);
    }
  }
}
render();
if(demo==='create')$('[data-action="create"]').click();
