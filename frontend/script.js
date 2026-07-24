const form = document.getElementById('reportForm');
const queryInput = document.getElementById('query');
const statusEl = document.getElementById('status');
const reportEl = document.getElementById('report');
const clearBtn = document.getElementById('clearBtn');

// Fixed backend endpoint (no UI field). Load runtime config from ./config.json with fallback to example and localhost.
const DEFAULT_ENDPOINT = '/api/audit';
let resolvedEndpoint = DEFAULT_ENDPOINT;

async function loadConfig(){
  // try runtime config first (frontend/config.json - should be created in deployment if needed)
  try{
    const r = await fetch('config.json', {cache: 'no-store'});
    if (r.ok){
      const cfg = await r.json();
      if (cfg && cfg.backendUrl) { resolvedEndpoint = cfg.backendUrl; return; }
    }
  }catch(e){ /* ignore */ }

  // fallback to shipped example
  try{
    const r2 = await fetch('config.example.json', {cache: 'no-store'});
    if (r2.ok){
      const cfg2 = await r2.json();
      if (cfg2 && cfg2.backendUrl) { resolvedEndpoint = cfg2.backendUrl; return; }
    }
  }catch(e){ /* ignore */ }

  // final fallback when opened via file:// (dev convenience)
  if (location.protocol === 'file:') resolvedEndpoint = 'http://localhost:8080/api/audit';
}

const configLoaded = loadConfig();

function setStatus(text){ statusEl.textContent = text; }

function renderJSON(obj){
  const pre = document.createElement('pre');
  pre.className = 'json';
  pre.textContent = JSON.stringify(obj, null, 2);
  return pre;
}

function renderError(err){
  reportEl.innerHTML = '';
  const c = document.createElement('div'); c.className = 'card error-card';
  const title = document.createElement('strong'); title.textContent = (err.error || 'Error') + ' — ' + (err.statusCode || '');
  c.appendChild(title);

  const rows = [
    ['Message', err.message || ''],
    ['Path', err.path || ''],
    ['Timestamp', err.timestamp || '']
  ];
  rows.forEach(([k,v]) => {
    const row = document.createElement('div'); row.className = 'kv';
    const key = document.createElement('div'); key.className = 'k'; key.textContent = k;
    const val = document.createElement('div'); val.className = 'v'; val.textContent = v;
    row.appendChild(key); row.appendChild(val);
    c.appendChild(row);
  });

  reportEl.appendChild(c);
  // also show raw JSON
  reportEl.appendChild(renderJSON(err));
}

function renderReport(data){
  reportEl.innerHTML = '';
  if (!data) return;

  // If data has recognizable fields, render nicely
  if (data.title || data.summary || data.items || data.pageTitle){
    // Support AuditResponse shape
    if (data.pageTitle || data.pageTitle === ""){
      const container = document.createElement('div');
      container.className = 'card';

      const rows = [
        ['HTTP Status', data.httpStatus || data.statusCode || ''],
        ['Response Time (ms)', data.responseTime || ''],
        ['Page Title', data.pageTitle || data.pageTitle === '' ? data.pageTitle : (data.pageTitle || data.pageTitle)],
        ['Meta Description', data.metaDescription || ''],
        ['H1 Count', data.h1Count ?? ''],
        ['Missing Alt Count', data.missingAltCount ?? ''],
        ['Word Count', data.wordCount ?? '']
      ];

      rows.forEach(([k, v]) => {
        const row = document.createElement('div'); row.className = 'kv';
        const key = document.createElement('div'); key.className = 'k'; key.textContent = k;
        const val = document.createElement('div'); val.className = 'v'; val.textContent = v;
        row.appendChild(key); row.appendChild(val);
        container.appendChild(row);
      });
      reportEl.appendChild(container);

      // show raw JSON as fallback
      reportEl.appendChild(renderJSON(data));
      return;
    }

    if (data.title){
      const h = document.createElement('h2'); h.textContent = data.title; reportEl.appendChild(h);
    }
    if (data.summary){
      const p = document.createElement('p'); p.textContent = data.summary; reportEl.appendChild(p);
    }
    if (Array.isArray(data.items)){
      data.items.forEach(it => {
        const c = document.createElement('div'); c.className = 'card';
        if (it.title) c.appendChild(Object.assign(document.createElement('strong'),{textContent: it.title}));
        if (it.text){ const t = document.createElement('p'); t.textContent = it.text; c.appendChild(t); }
        reportEl.appendChild(c);
      });
    }

    // fall back to showing raw JSON for remaining keys
    const leftover = Object.assign({}, data);
    delete leftover.title; delete leftover.summary; delete leftover.items;
    if (Object.keys(leftover).length) reportEl.appendChild(renderJSON(leftover));
    return;
  }

  // otherwise show whole JSON
  if (typeof data === 'object'){
    reportEl.appendChild(renderJSON(data));
    return;
  }

  // text response
  const p = document.createElement('p'); p.textContent = String(data); reportEl.appendChild(p);
}

async function tryFetch(endpoint, q){
  setStatus('Sending POST...');
  try{
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: q })
    });

    if (res.status === 404 || res.status === 405){
      // try GET fallback
      setStatus('POST not supported, trying GET...');
      const getRes = await fetch(endpoint + '?url=' + encodeURIComponent(q));
      return getRes;
    }

    return res;
  }catch(err){
    // network or CORS error — bubble up
    throw err;
  }
}

form.addEventListener('submit', async (ev) =>{
  ev.preventDefault();
  await configLoaded; // ensure config resolved before using it
  const endpoint = resolvedEndpoint;
  const q = queryInput.value.trim();
  if (!q) return;
  setStatus('Loading...');
  reportEl.innerHTML = '';
  try{
    const res = await tryFetch(endpoint, q);
    if (!res) throw new Error('No response');
    if (!res.ok){
      // try to parse structured JSON error
      const ct = res.headers.get('content-type') || '';
      if (ct.includes('application/json')){
        const errJson = await res.json();
        setStatus('Error: ' + (errJson.statusCode || res.status));
        renderError(errJson);
      } else {
        const text = await res.text();
        setStatus('Error: ' + res.status);
        renderReport(text);
      }
      return;
    }

    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')){
      const data = await res.json();
      setStatus('Report received');
      renderReport(data);
    } else {
      const txt = await res.text();
      setStatus('Report received (text)');
      renderReport(txt);
    }
  }catch(err){
    setStatus('Fetch error: ' + err.message);
    renderError({
      timestamp: new Date().toISOString(),
      statusCode: 0,
      error: 'NETWORK_ERROR',
      message: err.message || 'Failed to fetch',
      path: endpoint
    });
  }
});

clearBtn.addEventListener('click', ()=>{ queryInput.value=''; reportEl.innerHTML=''; setStatus('Idle'); });

// quick UX: allow Enter in query input to submit
queryInput.addEventListener('keydown', (e)=>{ if (e.key === 'Enter') form.dispatchEvent(new Event('submit')) });

// Footer live URL helper — call window.setLiveUrl('https://your-live-url') after deployment
const _liveLink = document.getElementById('liveUrl');
function setLiveUrl(url){
  if (!_liveLink) return;
  if (!url){ _liveLink.textContent = 'Not deployed'; _liveLink.removeAttribute('href'); return; }
  _liveLink.textContent = url;
  _liveLink.href = url;
}
window.setLiveUrl = setLiveUrl;
