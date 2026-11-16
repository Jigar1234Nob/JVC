import { useState } from "react";
import { ShieldCheck, Mic, Camera, Bell, Clipboard, ExternalLink, RefreshCw, LockKeyhole } from "lucide-react";
const CHAT_URL = "https://chat.jigar.au";
type P = "unknown" | "granted" | "denied" | "unsupported";
export default function App() {
  const [permissions,setPermissions] = useState<Record<string,P>>({microphone:"unknown",camera:"unknown",notifications:"unknown",clipboard:"unknown"});
  const [show,setShow] = useState(false); const [reload,setReload] = useState(0);
  const media = async (kind:"microphone"|"camera") => {
    try { const stream = await navigator.mediaDevices.getUserMedia(kind==="microphone"?{audio:true}:{video:true}); stream.getTracks().forEach(t=>t.stop()); setPermissions(p=>({...p,[kind]:"granted"})); }
    catch { setPermissions(p=>({...p,[kind]:"denied"})); }
  };
  const notifications = async () => {
    if (!("Notification" in window)) return setPermissions(p=>({...p,notifications:"unsupported"}));
    const result=await Notification.requestPermission(); setPermissions(p=>({...p,notifications:result==="granted"?"granted":"denied"}));
  };
  const clipboard = async () => { try { await navigator.clipboard.readText(); setPermissions(p=>({...p,clipboard:"granted"})); } catch { setPermissions(p=>({...p,clipboard:"denied"})); } };
  const rows=[{key:"microphone",name:"Microphone",Icon:Mic,action:()=>media("microphone")},{key:"camera",name:"Camera",Icon:Camera,action:()=>media("camera")},{key:"notifications",name:"Notifications",Icon:Bell,action:notifications},{key:"clipboard",name:"Clipboard",Icon:Clipboard,action:clipboard}];
  return <main className="shell"><header><div className="brand"><img src="/jvc-logo.svg"/>JVC</div><nav><button onClick={()=>setReload(x=>x+1)} title="Reload"><RefreshCw size={17}/></button><button className={show?"active":""} onClick={()=>setShow(x=>!x)}><ShieldCheck size={17}/> Permissions</button><a href={CHAT_URL} target="_blank" rel="noreferrer"><ExternalLink size={17}/></a></nav></header>
  <div className="workspace"><aside className="rail"><img src="/jvc-logo.svg"/><div className="lock"><LockKeyhole size={17}/></div></aside><section className="chat"><div className="heading"><div><small>JIGAR PRIVATE NETWORKING</small><h1>Chat</h1></div><span className="secure">● Secure connection</span></div><div className="frame" key={reload}><iframe title="JVC Chat" src={CHAT_URL} allow="camera; microphone; clipboard-read; clipboard-write; display-capture; fullscreen; autoplay; notifications" referrerPolicy="strict-origin-when-cross-origin"/></div></section>
  {show&&<aside className="panel"><div className="panelhead"><div><small>APP CONTROLS</small><h2>Permissions</h2></div><ShieldCheck/></div><p>Grant device access only when chat features need it. Your browser or operating system may show its own confirmation.</p>{rows.map(({key,name,Icon,action})=><div className="permission" key={key}><Icon size={18}/><div><b>{name}</b><small>{permissions[key]}</small></div><button onClick={action}>Allow</button></div>)}<p className="note">JVC restricts desktop permission requests to the configured chat origin. Site and OS permissions remain separately controlled.</p></aside>}</div><footer><span>JVC · Jigar Virtual Client</span><span>{(window as any).jvc?"Desktop app":"Web preview"} · chat.jigar.au</span></footer></main>;
}
