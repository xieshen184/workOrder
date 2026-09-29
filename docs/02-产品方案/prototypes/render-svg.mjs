import { mkdir, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import sharp from 'file:///C:/Users/xutong1.li/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp/dist/index.mjs';

const root = 'D:/person/workspace/workOrder-ui/docs/02-产品方案/ui';
await mkdir(`${root}/mobile`, { recursive: true });
await mkdir(`${root}/pc`, { recursive: true });

const esc = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
const t = (x,y,s,size=13,color='#445466',weight=400,anchor='start') => `<text x="${x}" y="${y}" font-size="${size}" fill="${color}" font-weight="${weight}" text-anchor="${anchor}" font-family="Microsoft YaHei,Arial,sans-serif">${esc(s)}</text>`;
const rect = (x,y,w,h,fill='#fff',r=10,stroke='none') => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}"/>`;

function mobileSvg(screen){
  let y=90, body='';
  const addCard=(title,rows=[],accent='#2fc8c3')=>{body+=rect(12,y,351,Math.max(70,42+rows.length*25),'#fff',8)+rect(12,y,5,Math.max(70,42+rows.length*25),accent,3)+t(28,y+24,title,14,'#273444',600);let ry=y+48;for(const row of rows){body+=t(28,ry,row[0],11,'#8995a5')+t(346,ry,row[1],11,'#445466',400,'end');ry+=24}y+=Math.max(70,42+rows.length*25)+10};
  const addForm=(label,value,h=44)=>{body+=t(18,y+14,label,11,'#657384',500)+rect(12,y+24,351,h,'#fff',6,'#e5e9ef')+t(24,y+50,value,12,'#9aa5b1');y+=h+44};
  const addTimeline=(events)=>{body+=rect(12,y,351,56+events.length*56,'#fff',8)+t(28,y+26,'处理进度',14,'#273444',600);let ey=y+60;events.forEach((e,i)=>{body+=`<line x1="35" y1="${ey-8}" x2="35" y2="${ey+36}" stroke="#d7e1ea" stroke-width="2"/>`+`<circle cx="35" cy="${ey}" r="6" fill="#2fc8c3"/>`+t(52,ey+4,e[0],12,'#374658',600)+t(52,ey+23,e[1],10,'#8a97a6');ey+=56});y+=56+events.length*56+10};
  if(screen.kind==='profile'){
    body+=`<rect x="0" y="76" width="375" height="140" fill="url(#profile)"/>`+`<circle cx="63" cy="141" r="32" fill="#fff"/>`+t(63,149,'人',22,'#2381ee',500,'middle')+t(112,133,'示例用户',17,'#fff',600)+t(112,158,'综合服务中心',12,'#e8fbff');y=236;screen.items.forEach(x=>{body+=rect(12,y,351,48,'#fff',5)+t(28,y+29,x,13)+t(344,y+29,'›',19,'#a0a8b2',400,'end');y+=50});
  } else {
    if(screen.status) body+=rect(0,76,375,36,'#dcf9f5',0)+t(188,99,screen.status,12,'#168f88',500,'middle'),y=126;
    if(screen.tabs){body+=rect(0,76,375,48,'#fff',0);screen.tabs.forEach((x,i)=>{body+=t(48+i*92,106,x,12,i===0?'#27bdb5':'#808b98',i===0?600:400,'middle');if(i===0)body+=rect(14+i*92,121,68,3,'#27bdb5',1)});y=140}
    for(const block of screen.blocks){if(block.type==='card')addCard(block.title,block.rows,block.accent);if(block.type==='form')addForm(block.label,block.value,block.height);if(block.type==='timeline')addTimeline(block.events);if(block.type==='upload'){body+=t(18,y+14,block.label,11,'#657384',500)+`<rect x="12" y="${y+24}" width="351" height="${block.height||120}" rx="7" fill="#fff" stroke="#cfd8e3" stroke-dasharray="6 5"/>`+t(188,y+80,'＋ '+block.value,13,'#9aa5b1',400,'middle');y+=(block.height||120)+44}if(block.type==='notice'){body+=rect(12,y,351,72,'#fff',8)+`<circle cx="30" cy="${y+25}" r="5" fill="${block.color}"/>`+t(45,y+25,block.title,13,'#334155',600)+t(45,y+46,block.text,11,'#788696')+t(344,y+62,block.time,10,'#a0a9b4',400,'end');y+=82}}
  }
  if(screen.button) body+=rect(12,744,351,48,screen.buttonColor||'#2fc8c3',6)+t(188,774,screen.button,14,'#fff',600,'middle');
  return `<svg xmlns="http://www.w3.org/2000/svg" width="375" height="812" viewBox="0 0 375 812"><defs><linearGradient id="profile" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#3187f2"/><stop offset="1" stop-color="#47c7d4"/></linearGradient></defs><rect width="375" height="812" fill="#f3f6f9"/>${rect(0,0,375,28,'#fff',0)}${t(14,19,'7:43',11,'#1f2937',500)}${t(360,19,'▮▮ 4G ▰',10,'#1f2937',400,'end')}${rect(0,28,375,48,'#fff',0)}${screen.back?t(14,60,'‹',24):''}${t(188,59,screen.title,16,'#273444',600,'middle')}${body}</svg>`;
}

const mobile={
  '01-入口与工作台/m22-register-prototype':{title:'用户注册',back:true,blocks:[{type:'form',label:'账号 *',value:'请输入账号'},{type:'form',label:'密码 *',value:'请输入密码'},{type:'form',label:'确认密码 *',value:'请再次输入密码'},{type:'form',label:'验证码 *',value:'请输入验证码'}],button:'注册',buttonColor:'#2a7ff0'},
  '01-入口与工作台/m02-workbench-prototype':{title:'工作台',blocks:[{type:'card',title:'常用工作',rows:[['工单申请','工单派发　工单处理'],['待派单','8'],['待接单','5'],['执行中','12']]},{type:'card',title:'最近工单',rows:[['WO2025070008','处理中'],['类型','设备维修'],['更新','10 分钟前']],accent:'#2fc8c3'}]},
  '06-个人与消息/m11-profile-prototype':{title:'我的',kind:'profile',items:['个人信息','修改密码','消息通知（3）','帮助与反馈','应用设置','退出登录']},
  '02-报修与进度/m12-my-orders-prototype':{title:'我的工单',back:true,tabs:['全部','待处理','处理中','已完成'],blocks:[{type:'card',title:'WO2025070012',rows:[['状态','处理中'],['类型','设备维修'],['预计完成','今日 16:30']],accent:'#2fc8c3'},{type:'card',title:'WO2025070007',rows:[['状态','待确认'],['类型','环境维修'],['提示','请确认处理结果']],accent:'#ff9800'},{type:'card',title:'WO2025070002',rows:[['状态','待派单'],['类型','综合维修']],accent:'#7a24df'}]},
  '02-报修与进度/m13-progress-prototype':{title:'工单进度',back:true,status:'处理中 · 预计今日 16:30 完成',blocks:[{type:'card',title:'WO2025070012',rows:[['报修类型','设备维修'],['报修位置','二层公共区域'],['当前处理人','示例工程师']]},{type:'timeline',events:[['正在维修','14:10 · 已更换配件并调试'],['已到场','13:35 · 已上传到场照片'],['已接单','13:20'],['已派单','13:12'],['已提交','12:58']]}],button:'联系处理人'},
  '05-完工确认与评价/m14-confirm-prototype':{title:'完工确认',back:true,status:'处理人已提交完工，请核验现场',blocks:[{type:'card',title:'处理结果',rows:[['处理方式','更换零件并完成调试'],['完成时间','今日 15:42'],['现场照片','查看 2 张 ›']]},{type:'form',label:'补充说明（选填）',value:'请输入确认说明',height:90}],button:'确认完工'},
  '04-维修处理/m15-arrival-prototype':{title:'到场确认',back:true,blocks:[{type:'card',title:'WO2025070012',rows:[['位置','二层公共区域'],['接单时间','13:20']]},{type:'upload',label:'到场凭证 *',value:'拍摄现场照片',height:180},{type:'card',title:'定位信息',rows:[['当前位置','距工单点位 18m'],['到场时间','系统自动记录']]}],button:'确认到场并开始诊断'},
  '04-维修处理/m16-assessment-prototype':{title:'故障评估',back:true,blocks:[{type:'form',label:'故障原因 *',value:'填写现场诊断结果',height:74},{type:'form',label:'处理方案 *',value:'填写维修步骤和所需资源',height:74},{type:'form',label:'预计完成时间 *',value:'选择日期与时间　›'},{type:'form',label:'是否需要配件',value:'否　›'},{type:'upload',label:'评估照片（选填）',value:'上传照片',height:80}],button:'提交评估并开始处理'},
  '04-维修处理/m17-delay-prototype':{title:'延期申请',back:true,blocks:[{type:'card',title:'当前时限',rows:[['原截止时间','今日 16:30'],['剩余时长','01:25:18']],accent:'#ff9800'},{type:'form',label:'申请延期至 *',value:'选择日期与时间　›'},{type:'form',label:'延期原因 *',value:'说明无法按时完成的客观原因',height:110},{type:'upload',label:'佐证照片（选填）',value:'上传照片',height:80}],button:'提交延期申请',buttonColor:'#ff9800'},
  '04-维修处理/m18-finish-prototype':{title:'提交完工',back:true,blocks:[{type:'form',label:'处理结果 *',value:'填写实际处理内容与结果',height:92},{type:'upload',label:'完工照片 *',value:'至少上传 1 张',height:110},{type:'form',label:'使用材料（选填）',value:'添加材料名称与数量　›'},{type:'form',label:'后续建议（选填）',value:'填写维护建议',height:74}],button:'提交并等待确认'},
  '03-派单调度/m19-engineers-prototype':{title:'人员状态',back:true,tabs:['全部','空闲','处理中','离线'],blocks:[{type:'card',title:'示例人员 A',rows:[['专业','设备维修'],['状态','空闲 · 当前 0 单'],['联系电话','一键呼叫']],accent:'#2fc8c3'},{type:'card',title:'示例人员 B',rows:[['专业','综合维修'],['状态','处理中 · 当前 2 单'],['联系电话','一键呼叫']],accent:'#ff9800'},{type:'card',title:'示例人员 C',rows:[['专业','环境维修'],['状态','离线'],['联系电话','一键呼叫']],accent:'#9aa5b1'}]},
  '06-个人与消息/m20-notifications-prototype':{title:'消息中心',back:true,tabs:['全部','工单','审批','系统'],blocks:[{type:'notice',title:'工单已派发',text:'有一条工单已分配给处理人员',time:'5 分钟前',color:'#ef5656'},{type:'notice',title:'延期申请待审批',text:'有一条延期申请需要处理',time:'25 分钟前',color:'#ff9800'},{type:'notice',title:'处理完成',text:'请确认现场并完成评价',time:'今天 09:30',color:'#2fc8c3'},{type:'notice',title:'系统通知',text:'服务规则已更新',time:'昨天 16:20',color:'#aab3bf'}]},
  '06-个人与消息/m23-avatar-prototype':{title:'修改头像',back:true,blocks:[{type:'upload',label:'头像裁剪',value:'选择照片并拖动裁剪区域',height:320},{type:'card',title:'操作提示',rows:[['移动图片','调整头像位置'],['拖动边框','调整裁剪范围']]}],button:'保存头像',buttonColor:'#2a7ff0'},
  '06-个人与消息/m24-profile-info-prototype':{title:'个人信息',back:true,blocks:[{type:'card',title:'账号资料',rows:[['昵称','示例用户'],['手机号码','138****0012'],['邮箱','example@demo.com'],['岗位','报修人员'],['角色','普通用户'],['创建日期','2025-07-01']]}],button:'编辑资料',buttonColor:'#2a7ff0'},
  '06-个人与消息/m25-profile-edit-prototype':{title:'编辑资料',back:true,blocks:[{type:'form',label:'用户昵称 *',value:'示例用户'},{type:'form',label:'手机号码 *',value:'13800000012'},{type:'form',label:'邮箱',value:'example@demo.com'},{type:'form',label:'性别 *',value:'男　›'}],button:'保存',buttonColor:'#2a7ff0'},
  '06-个人与消息/m26-password-prototype':{title:'修改密码',back:true,blocks:[{type:'form',label:'旧密码 *',value:'请输入旧密码'},{type:'form',label:'新密码 *',value:'请输入新密码'},{type:'form',label:'确认密码 *',value:'请再次输入新密码'},{type:'card',title:'密码要求',rows:[['长度','8–20 位'],['内容','包含字母和数字']]}],button:'确认修改',buttonColor:'#2a7ff0'},
  '06-个人与消息/m27-settings-prototype':{title:'应用设置',back:true,blocks:[{type:'card',title:'账号与应用',rows:[['修改密码','›'],['检查更新','当前已是最新版本'],['清理缓存','18.6 MB']]},{type:'card',title:'登录状态',rows:[['当前账号','示例用户'],['当前设备','本机']]}],button:'退出登录',buttonColor:'#ef5656'},
  '06-个人与消息/m28-help-prototype':{title:'常见问题',back:true,blocks:[{type:'card',title:'账号与登录',rows:[['如何修改密码','›'],['如何退出登录','›']]},{type:'card',title:'个人资料',rows:[['如何更换头像','›'],['如何修改联系方式','›']]},{type:'card',title:'工单操作',rows:[['如何提交工单','›'],['如何查看处理进度','›']]}]},
  '06-个人与消息/m29-about-prototype':{title:'关于',back:true,blocks:[{type:'card',title:'工单管理移动端',rows:[['当前版本','v1.0.0'],['客服邮箱','service@example.com'],['客服电话','400-000-0000'],['服务网站','www.example.com']]},{type:'card',title:'相关协议',rows:[['用户协议','›'],['隐私政策','›']]}]},
  '07-通用状态/m21-empty-state-prototype':{title:'我的工单',back:true,tabs:['全部','待处理','处理中','已完成'],blocks:[{type:'card',title:'暂无待处理工单',rows:[['提示','新提交的工单会显示在这里']],accent:'#d8e0e8'}],button:'去提交工单',buttonColor:'#2a7ff0'},
  '08-通用内容/m30-webview-prototype':{title:'网页内容',back:true,blocks:[{type:'card',title:'页面标题',rows:[['加载状态','已完成'],['来源','受信任页面']]},{type:'card',title:'网页正文区域',rows:[['内容','在应用内安全展示'],['外部链接','确认后打开浏览器']]}]},
  '08-通用内容/m31-textview-prototype':{title:'内容详情',back:true,blocks:[{type:'card',title:'如何查看工单进度',rows:[['步骤一','进入“我的工单”'],['步骤二','选择需要查看的工单'],['步骤三','查看状态和处理时间线']]},{type:'card',title:'提示',rows:[['需要帮助','请联系服务人员']]}]}
};

for(const [name,spec] of Object.entries(mobile)) { const file=`${root}/mobile/${name}.svg`; const svg=mobileSvg(spec); await mkdir(dirname(file),{recursive:true}); await writeFile(file,svg,'utf8'); await sharp(Buffer.from(svg)).png().toFile(file.replace(/\.svg$/,'.png')); }

function pcSvg(spec){
  let body=`${rect(0,0,1440,900,'#f0f2f5',0)}${rect(0,0,210,900,'#263445',0)}${rect(0,0,210,54,'#203040',0)}${rect(18,13,30,30,'#409eff',7)}${t(33,34,'修',15,'#fff',600,'middle')}${t(58,34,'工单管理平台',17,'#fff',600)}`;
  const menus=['工作台','工单管理','人员调度','基础配置','统计分析','消息管理','短信账户','系统管理'];menus.forEach((m,i)=>{const my=75+i*48;if(m===spec.menu)body+=rect(0,my-20,210,48,'#1f2d3d',0)+rect(0,my-20,3,48,'#409eff',0);body+=t(24,my+9,m,14,m===spec.menu?'#409eff':'#bfcbd9')+t(188,my+9,'›',15,'#8ea0b3')});
  body+=rect(210,0,1230,54,'#fff',0)+t(232,34,`首页 / ${spec.menu}`,13,'#606266')+t(1418,34,'消息　帮助　示例管理员 ▾',13,'#606266',400,'end')+t(232,94,spec.title,21,'#303133',500);
  let y=118;
  if(spec.stats){spec.stats.forEach((s,i)=>{const x=228+i*296;body+=rect(x,y,276,102,'#fff',4)+t(x+18,y+27,s[0],13,'#909399')+t(x+18,y+72,s[1],30,s[2]||'#303133',600)+`<circle cx="${x+233}" cy="${y+50}" r="25" fill="#ecf5ff"/>`+t(x+233,y+57,s[3]||'•',20,'#409eff',500,'middle')});y+=122}
  if(spec.filters){body+=rect(228,y,1194,82,'#fff',4);spec.filters.forEach((f,i)=>{const x=246+i*210;body+=t(x,y+23,f,11,'#606266')+rect(x,y+32,184,32,'#fff',4,'#dcdfe6')+t(x+10,y+53,'全部 / 请输入',11,'#a3a6ad')});body+=rect(1095,y+31,76,34,'#409eff',4)+t(1133,y+53,'搜索',12,'#fff',500,'middle')+rect(1182,y+31,76,34,'#fff',4,'#dcdfe6')+t(1220,y+53,'重置',12,'#606266',500,'middle');y+=98}
  if(spec.chart){body+=rect(228,y,760,310,'#fff',4)+t(248,y+30,spec.chart,15,'#303133',500);for(let i=0;i<5;i++)body+=`<line x1="260" y1="${y+70+i*48}" x2="956" y2="${y+70+i*48}" stroke="#ebeef5"/>`;body+=`<polyline points="270,${y+245} 380,${y+190} 490,${y+215} 600,${y+130} 710,${y+160} 820,${y+105} 940,${y+145}" fill="none" stroke="#409eff" stroke-width="4"/>`;body+=rect(1004,y,418,310,'#fff',4)+t(1024,y+30,'分类与 SLA',15,'#303133',500)+`<circle cx="1210" cy="${y+165}" r="90" fill="none" stroke="#409eff" stroke-width="34" stroke-dasharray="340 226"/>`+`<circle cx="1210" cy="${y+165}" r="90" fill="none" stroke="#67c23a" stroke-width="34" stroke-dasharray="150 416" stroke-dashoffset="-340"/>`+t(1210,y+175,'86%',26,'#303133',600,'middle');y+=330}
  body+=rect(228,y,1194,Math.min(900-y-20,330),'#fff',4)+t(248,y+32,spec.section||'数据列表',15,'#303133',500);
  const cols=spec.columns||['编号','类型','位置','负责人','状态','剩余时间','操作'];const widths=[160,130,210,150,120,150,160];let x=248;cols.forEach((c,i)=>{body+=rect(x,y+48,widths[i]||140,38,'#f8f8f9',0)+t(x+10,y+72,c,12,'#515a6e',500);x+=widths[i]||140});for(let r=0;r<4;r++){x=248;const vals=spec.rows?.[r]||[`WO20250700${12-r}`,'设备维修',`${r+1}层公共区域`,`示例人员 ${String.fromCharCode(65+r)}`,r===0?'处理中':r===1?'待派单':'已完成',r<2?'01:25:18':'—','查看　编辑'];cols.forEach((_,i)=>{body+=t(x+10,y+112+r*48,vals[i]||'—',11,i===cols.length-1?'#409eff':'#606266');x+=widths[i]||140});body+=`<line x1="248" y1="${y+125+r*48}" x2="1400" y2="${y+125+r*48}" stroke="#ebeef5"/>`}
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="900" viewBox="0 0 1440 900">${body}</svg>`;
}

const pc={
  '01-运营驾驶舱/p01-dashboard-prototype':{title:'工单运营概览',menu:'工作台',stats:[['今日新增','28'],['待派单','8'],['处理中','15'],['超时预警','3','#f56c6c','!']],chart:'近 7 日工单趋势',section:'超时风险工单'},
  '02-工单管理/p02-order-list-prototype':{title:'工单列表',menu:'工单管理',filters:['工单编号','工单状态','报修类型','提交时间'],section:'全部工单'},
  '02-工单管理/p03-order-detail-prototype':{title:'工单详情 · WO2025070012',menu:'工单管理',stats:[['当前状态','处理中'],['当前处理人','示例人员 B'],['计划完成','今日 16:30'],['剩余时间','01:25:18','#e6a23c']],section:'处理记录',columns:['时间','动作','操作人','前状态','后状态','说明']},
  '02-工单管理/p04-dispatch-prototype':{title:'工单派发',menu:'工单管理',filters:['工单编号','派单方式','处理人员','完成时限'],section:'推荐处理人员',columns:['姓名','专业方向','当前工单','及时完成率','距离','状态','操作']},
  '03-人员调度/p05-engineers-prototype':{title:'维修人员状态',menu:'人员调度',stats:[['在线人员','18'],['空闲','7'],['处理中','9'],['离线','2']],filters:['专业方向','在线状态','姓名'],section:'人员负载',columns:['姓名','专业','状态','当前工单','今日完成','及时率','操作']},
  '04-基础配置/p06-sla-config-prototype':{title:'工单分类与 SLA',menu:'基础配置',filters:['分类名称','启用状态'],section:'分类规则',columns:['分类名称','编码','响应时限','完成时限','默认专业','启用','操作']},
  '04-基础配置/p07-users-prototype':{title:'报修人员管理',menu:'系统管理',filters:['姓名 / 手机号','所属组织','账号状态'],section:'人员列表',columns:['姓名','手机号','所属组织','累计报修','最近报修','状态','操作']},
  '05-统计分析/p08-performance-prototype':{title:'维修绩效分析',menu:'统计分析',stats:[['平均响应','14 分'],['按时完成率','96.4%'],['平均满意度','4.8'],['返工率','1.6%']],chart:'人员完成量与趋势',section:'人员绩效明细',columns:['姓名','完成量','平均响应','按时率','满意度','返工率','操作']},
  '06-消息与短信/p09-messages-prototype':{title:'消息模板与发送记录',menu:'消息管理',filters:['消息类型','发送渠道','发送状态','发送时间'],section:'模板与记录',columns:['模板名称','事件','渠道','最近发送','成功率','状态','操作']},
  '06-消息与短信/p10-sms-prototype':{title:'短信账户',menu:'短信账户',stats:[['可用短信','18,620'],['今日发送','128'],['发送失败','2','#f56c6c'],['本月发送','3,842']],section:'最近发送记录',columns:['发送时间','业务类型','接收号码','条数','状态','原因','操作']},
  '02-工单管理/p11-delay-approval-prototype':{title:'延期审批',menu:'工单管理',filters:['申请状态','工单 / 申请人'],section:'延期申请列表',columns:['工单编号','工单标题','申请人','原截止时间','申请延期至','状态','操作'],rows:[['WO2025070012','设备运行异常','示例人员 A','07-10 16:30','07-10 19:30','待审批','同意　拒绝'],['WO2025070011','公共区域故障','示例人员 B','07-10 14:00','07-10 17:00','已同意','查看'],['WO2025070010','设施异常','示例人员 C','07-10 12:30','07-10 15:30','已拒绝','查看'],['WO2025070009','设备告警','示例人员 D','07-10 11:00','07-10 13:00','已取消','查看']]},
  '04-基础配置/p12-sla-rules-prototype':{title:'SLA 规则',menu:'基础配置',filters:['规则名称 / 编码','适用分类','紧急程度','状态'],section:'SLA 规则列表',columns:['规则名称','适用范围','紧急程度','响应 / 到场','完成时限','状态','操作'],rows:[['普通维修规则','全部分类','一般','30 / 60 分钟','240 分钟','正常','编辑　停用'],['设备紧急规则','设备维修','紧急','10 / 20 分钟','120 分钟','正常','编辑　停用'],['特急响应规则','全部分类','特急','5 / 10 分钟','60 分钟','正常','编辑　停用'],['历史规则','综合维修','一般','60 / 120 分钟','480 分钟','停用','编辑　启用']]}
};
for(const [name,spec] of Object.entries(pc)) { const file=`${root}/pc/${name}.svg`; const svg=pcSvg(spec); await mkdir(dirname(file),{recursive:true}); await writeFile(file,svg,'utf8'); await sharp(Buffer.from(svg)).png().toFile(file.replace(/\.svg$/,'.png')); }
console.log(`generated ${Object.keys(mobile).length} mobile and ${Object.keys(pc).length} pc SVG prototypes`);
