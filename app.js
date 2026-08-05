'use strict';

/* =========================================================================
   TUNEO — Lecteur multimédia local
   Architecture modulaire : Store (état) / Scanner / Audio Engine / Video
   Engine / Router (vues) / Renderers / UI events.
   Pensé pour évoluer : Store est isolé, prêt pour un futur backend
   (comptes, feed social, recommandations) sans réécrire l'UI.
   ========================================================================= */

/* ---------------------------- Utilitaires ---------------------------- */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const fmtTime = (s) => {
  if (!isFinite(s) || s < 0) s = 0;
  const m = Math.floor(s / 60);
  const sec = Math.floor(s % 60);
  return `${m}:${sec.toString().padStart(2, '0')}`;
};
const uid = () => Math.random().toString(36).slice(2, 10);
const AUDIO_EXT = ['mp3','m4a','aac','wav','ogg','flac','opus','wma'];
const VIDEO_EXT = ['mp4','mkv','webm','mov','avi','3gp','m4v'];

function extOf(name) {
  const m = /\.([a-z0-9]+)$/i.exec(name);
  return m ? m[1].toLowerCase() : '';
}

function guessArtistTitle(filename) {
  // Nettoie les noms de fichiers type "Artiste - Titre (Visual)_something.mp3"
  let base = filename.replace(/\.[^.]+$/, '');
  base = base.replace(/[_]+/g, ' ').trim();
  const sepMatch = base.match(/^(.+?)\s*-\s*(.+)$/);
  if (sepMatch) {
    return { artist: sepMatch[1].trim(), title: sepMatch[2].trim() };
  }
  return { artist: 'Artiste inconnu', title: base || 'Sans titre' };
}

function toast(msg, ms = 2000) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast._h);
  toast._h = setTimeout(() => t.classList.remove('show'), ms);
}

/* ---------------------------- Store ---------------------------- */
const Store = {
  tracks: [],          // {id, file, name, title, artist, album, duration, url, cover, dateAdded}
  videos: [],          // {id, file, name, title, duration, url, thumb, dateAdded}
  favorites: new Set(JSON.parse(localStorage.getItem('tuneo_favs') || '[]')),
  playlists: JSON.parse(localStorage.getItem('tuneo_playlists') || '[]'), // [{id,name,trackIds:[]}]
  recentlyPlayed: JSON.parse(localStorage.getItem('tuneo_recent') || '[]'), // [trackId]
  queue: [],
  queueIndex: -1,
  scanned: false,
  scanning: false,

  saveFavs() { localStorage.setItem('tuneo_favs', JSON.stringify([...this.favorites])); },
  savePlaylists() { localStorage.setItem('tuneo_playlists', JSON.stringify(this.playlists)); },
  saveRecent() { localStorage.setItem('tuneo_recent', JSON.stringify(this.recentlyPlayed.slice(0, 30))); },

  toggleFav(id) {
    if (this.favorites.has(id)) this.favorites.delete(id); else this.favorites.add(id);
    this.saveFavs();
  },

  pushRecent(id) {
    this.recentlyPlayed = [id, ...this.recentlyPlayed.filter((x) => x !== id)].slice(0, 30);
    this.saveRecent();
  },

  getTrack(id) { return this.tracks.find((t) => t.id === id); },

  albumsMap() {
    const map = new Map();
    for (const t of this.tracks) {
      const key = t.album || 'Album inconnu';
      if (!map.has(key)) map.set(key, { name: key, artist: t.artist, tracks: [] });
      map.get(key).tracks.push(t);
    }
    return map;
  },
  artistsMap() {
    const map = new Map();
    for (const t of this.tracks) {
      const key = t.artist || 'Artiste inconnu';
      if (!map.has(key)) map.set(key, { name: key, tracks: [] });
      map.get(key).tracks.push(t);
    }
    return map;
  },
  foldersMap() {
    const map = new Map();
    for (const t of this.tracks) {
      const key = t.folder || 'Racine';
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(t);
    }
    return map;
  },
};

/* ---------------------------- Scanner ---------------------------- */
const Scanner = {
  async pickFiles() {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      input.accept = 'audio/*,video/*,.mp3,.m4a,.flac,.wav,.ogg,.opus,.mp4,.mkv,.webm,.mov';
      // @ts-ignore - attribut non standard mais supporté sur Android/Chrome pour choisir un dossier entier
      input.setAttribute('webkitdirectory', '');
      input.style.display = 'none';
      document.body.appendChild(input);
      input.addEventListener('change', () => {
        resolve(Array.from(input.files || []));
        input.remove();
      });
      input.click();
    });
  },

  async pickFilesFlat() {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      input.accept = 'audio/*,video/*';
      input.style.display = 'none';
      document.body.appendChild(input);
      input.addEventListener('change', () => {
        resolve(Array.from(input.files || []));
        input.remove();
      });
      input.click();
    });
  },

  async processFiles(files) {
    Store.scanning = true;
    Renderer.renderScanBanner(0, files.length);

    let processed = 0;
    for (const file of files) {
      const ext = extOf(file.name);
      const isAudio = AUDIO_EXT.includes(ext) || file.type.startsWith('audio/');
      const isVideo = VIDEO_EXT.includes(ext) || file.type.startsWith('video/');
      const path = file.webkitRelativePath || file.name;
      const folder = path.includes('/') ? path.split('/').slice(0, -1).pop() : 'Racine';

      if (isAudio) {
        const { artist, title } = guessArtistTitle(file.name);
        const url = URL.createObjectURL(file);
        const track = {
          id: uid(),
          file, name: file.name, title, artist,
          album: folder && folder !== 'Racine' ? folder : 'Singles',
          folder,
          duration: 0,
          url,
          cover: null,
          dateAdded: file.lastModified || Date.now(),
        };
        Store.tracks.push(track);
        this.readAudioMeta(track);
      } else if (isVideo) {
        const url = URL.createObjectURL(file);
        const video = {
          id: uid(),
          file, name: file.name,
          title: file.name.replace(/\.[^.]+$/, ''),
          duration: 0, url, thumb: null, folder,
          dateAdded: file.lastModified || Date.now(),
        };
        Store.videos.push(video);
        this.readVideoMeta(video);
      }
      processed++;
      if (processed % 8 === 0) Renderer.renderScanBanner(processed, files.length);
    }

    Store.scanning = false;
    Store.scanned = true;
    Renderer.renderScanBanner(files.length, files.length, true);
    Router.renderCurrentView();
  },

  readAudioMeta(track) {
    const a = new Audio();
    a.preload = 'metadata';
    a.src = track.url;
    a.addEventListener('loadedmetadata', () => {
      track.duration = a.duration || 0;
      Router.softRefresh();
    }, { once: true });
    a.addEventListener('error', () => {}, { once: true });
  },

  readVideoMeta(video) {
    const v = document.createElement('video');
    v.preload = 'metadata';
    v.src = video.url;
    v.muted = true;
    v.addEventListener('loadedmetadata', () => {
      video.duration = v.duration || 0;
      // capture une miniature
      try {
        v.currentTime = Math.min(1, (v.duration || 1) / 3);
      } catch (e) {}
    }, { once: true });
    v.addEventListener('seeked', () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = 320; canvas.height = Math.round(320 * (v.videoHeight / v.videoWidth || 0.6));
        const ctx = canvas.getContext('2d');
        ctx.drawImage(v, 0, 0, canvas.width, canvas.height);
        video.thumb = canvas.toDataURL('image/jpeg', 0.6);
      } catch (e) { /* cross-origin / codec issue, ignore */ }
      Router.softRefresh();
    }, { once: true });
    v.addEventListener('error', () => {}, { once: true });
  },
};

/* ---------------------------- Audio Engine ---------------------------- */
const AudioEngine = {
  el: new Audio(),
  isPlaying: false,
  shuffle: false,
  repeat: 'off', // off | all | one
  sleepTimer: null,
  sleepEndsAt: null,
  ctx: null, source: null, filters: [],

  init() {
    this.el.addEventListener('timeupdate', () => Renderer.updateProgress());
    this.el.addEventListener('ended', () => this.onEnded());
    this.el.addEventListener('play', () => { this.isPlaying = true; Renderer.updatePlayState(); });
    this.el.addEventListener('pause', () => { this.isPlaying = false; Renderer.updatePlayState(); });
    this.el.addEventListener('loadedmetadata', () => Renderer.updateProgress());
  },

  setupEQ() {
    if (this.ctx) return;
    try {
      this.ctx = new (window.AudioContext || window.webkitAudioContext)();
      this.source = this.ctx.createMediaElementSource(this.el);
      const freqs = [60, 250, 1000, 4000, 12000];
      let node = this.source;
      this.filters = freqs.map((f) => {
        const filt = this.ctx.createBiquadFilter();
        filt.type = 'peaking';
        filt.frequency.value = f;
        filt.Q.value = 1;
        filt.gain.value = 0;
        node.connect(filt);
        node = filt;
        return filt;
      });
      node.connect(this.ctx.destination);
    } catch (e) { console.warn('EQ unavailable', e); }
  },

  setEQGains(gains) {
    this.setupEQ();
    if (this.ctx && this.ctx.state === 'suspended') this.ctx.resume();
    this.filters.forEach((f, i) => { if (gains[i] !== undefined) f.gain.value = gains[i]; });
  },

  playTrack(track, queue) {
    if (queue) { Store.queue = queue; Store.queueIndex = queue.findIndex((t) => t.id === track.id); }
    this.el.src = track.url;
    this.el.play().catch(() => {});
    Store.pushRecent(track.id);
    Renderer.renderNowPlaying(track);
    Renderer.showMiniPlayer();
    Renderer.renderHome(); // refresh "reprendre l'écoute"
  },

  toggle() {
    if (!Store.queue.length) return;
    if (this.el.paused) this.el.play().catch(() => {}); else this.el.pause();
  },

  next(auto = false) {
    if (!Store.queue.length) return;
    if (this.shuffle) {
      let idx = Math.floor(Math.random() * Store.queue.length);
      if (Store.queue.length > 1 && idx === Store.queueIndex) idx = (idx + 1) % Store.queue.length;
      Store.queueIndex = idx;
    } else {
      Store.queueIndex = (Store.queueIndex + 1) % Store.queue.length;
    }
    const t = Store.queue[Store.queueIndex];
    this.el.src = t.url;
    this.el.play().catch(() => {});
    Store.pushRecent(t.id);
    Renderer.renderNowPlaying(t);
  },

  prev() {
    if (!Store.queue.length) return;
    if (this.el.currentTime > 3) { this.el.currentTime = 0; return; }
    Store.queueIndex = (Store.queueIndex - 1 + Store.queue.length) % Store.queue.length;
    const t = Store.queue[Store.queueIndex];
    this.el.src = t.url;
    this.el.play().catch(() => {});
    Renderer.renderNowPlaying(t);
  },

  onEnded() {
    if (this.repeat === 'one') { this.el.currentTime = 0; this.el.play(); return; }
    if (this.repeat === 'off' && !this.shuffle && Store.queueIndex === Store.queue.length - 1) {
      Renderer.updatePlayState();
      return;
    }
    this.next(true);
  },

  seekTo(ratio) {
    if (!isFinite(this.el.duration)) return;
    this.el.currentTime = ratio * this.el.duration;
  },

  setSleepTimer(minutes) {
    clearTimeout(this.sleepTimer);
    if (!minutes) { this.sleepEndsAt = null; $('#sleep-value').textContent = 'Désactivé'; return; }
    this.sleepEndsAt = Date.now() + minutes * 60000;
    $('#sleep-value').textContent = `${minutes} min`;
    this.sleepTimer = setTimeout(() => {
      this.el.pause();
      this.sleepEndsAt = null;
      $('#sleep-value').textContent = 'Désactivé';
      toast('Minuteur sommeil : lecture en pause');
    }, minutes * 60000);
  },
};
AudioEngine.init();

/* ---------------------------- Video Engine ---------------------------- */
const VideoEngine = {
  el: null,
  current: null,
  controlsTimeout: null,

  init() {
    this.el = $('#video-el');
    this.el.addEventListener('timeupdate', () => this.updateProgress());
    this.el.addEventListener('play', () => this.updatePlayIcon());
    this.el.addEventListener('pause', () => this.updatePlayIcon());
    this.el.addEventListener('loadedmetadata', () => this.updateProgress());
    this.el.addEventListener('ended', () => this.updatePlayIcon());

    let lastTap = 0, gestureStartY = null, gestureStartX = null, startVolume = 1;
    const overlay = $('#video-overlay');

    this.el.addEventListener('click', () => {
      const now = Date.now();
      if (now - lastTap < 280) return; // évite conflit avec double-tap
      lastTap = now;
      overlay.classList.toggle('hide');
    });

    this.el.addEventListener('dblclick', (e) => {
      const rect = this.el.getBoundingClientRect();
      const x = e.clientX - rect.left;
      if (x < rect.width / 2) this.seekRelative(-10); else this.seekRelative(10);
      this.showGesture(x < rect.width / 2 ? '« 10s' : '10s »');
    });

    // gestes tactiles simplifiés pour mobile : swipe vertical gauche = luminosité (visuel), droite = volume
    this.el.addEventListener('touchstart', (e) => {
      if (e.touches.length !== 1) return;
      gestureStartY = e.touches[0].clientY;
      gestureStartX = e.touches[0].clientX;
      startVolume = this.el.volume;
    }, { passive: true });

    this.el.addEventListener('touchmove', (e) => {
      if (gestureStartY === null || e.touches.length !== 1) return;
      const dy = gestureStartY - e.touches[0].clientY;
      const rect = this.el.getBoundingClientRect();
      const rightSide = gestureStartX > rect.width / 2;
      if (Math.abs(dy) > 12 && rightSide) {
        const delta = dy / rect.height;
        this.el.volume = Math.min(1, Math.max(0, startVolume + delta));
        this.showGesture(`🔊 ${Math.round(this.el.volume * 100)}%`);
      }
    }, { passive: true });

    this.el.addEventListener('touchend', () => {
      gestureStartY = null;
      setTimeout(() => $('#gesture-indicator').style.display = 'none', 400);
    });
  },

  showGesture(text) {
    const el = $('#gesture-indicator');
    el.textContent = text;
    el.style.display = 'flex';
    clearTimeout(this.gestureHideT);
    this.gestureHideT = setTimeout(() => { el.style.display = 'none'; }, 700);
  },

  open(video) {
    this.current = video;
    this.el.src = video.url;
    $('#video-title-bar').textContent = video.title;
    $('#video-sheet').classList.add('show');
    $('#video-overlay').classList.remove('hide');
    this.el.play().catch(() => {});
    document.documentElement.requestFullscreen?.().catch(() => {});
  },

  close() {
    this.el.pause();
    $('#video-sheet').classList.remove('show');
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
  },

  toggle() { if (this.el.paused) this.el.play(); else this.el.pause(); },
  updatePlayIcon() {
    $('#video-play-icon').innerHTML = this.el.paused
      ? '<path d="M8 5v14l11-7z"/>'
      : '<path d="M6 5h4v14H6zM14 5h4v14h-4z"/>';
  },
  seekRelative(sec) { this.el.currentTime = Math.max(0, Math.min(this.el.duration || 0, this.el.currentTime + sec)); },
  updateProgress() {
    const ratio = this.el.duration ? this.el.currentTime / this.el.duration : 0;
    $('#video-seek-fill').style.width = `${ratio * 100}%`;
    $('#video-time-cur').textContent = fmtTime(this.el.currentTime);
    $('#video-time-dur').textContent = fmtTime(this.el.duration);
  },
};

/* ---------------------------- Renderer ---------------------------- */
const Renderer = {
  renderScanBanner(done, total, finished = false) {
    const el = $('#home-scan-banner');
    if (!Store.scanning && !finished) { el.innerHTML = ''; return; }
    if (finished) {
      setTimeout(() => { el.innerHTML = ''; }, 1400);
      el.innerHTML = `<div class="scan-banner fade-in"><div class="scan-spin" style="border-top-color:var(--sage); animation:none; border-color: var(--sage);"></div><div><div class="scan-text">Bibliothèque à jour</div><div class="scan-sub">${Store.tracks.length} morceaux · ${Store.videos.length} vidéos</div></div></div>`;
      return;
    }
    el.innerHTML = `<div class="scan-banner fade-in"><div class="scan-spin"></div><div><div class="scan-text">Analyse de l'appareil…</div><div class="scan-sub">${done} / ${total} fichiers traités</div></div></div>`;
  },

  renderHomeEmpty() {
    const hasMedia = Store.tracks.length || Store.videos.length;
    $('#home-empty').innerHTML = hasMedia ? '' : `
      <div class="empty-state fade-in">
        <svg class="empty-motif" viewBox="0 0 720 720" fill="none" style="color:var(--sage)"><use href="#headphones-motif"/></svg>
        <div class="empty-title">Ta bibliothèque est vide</div>
        <div class="empty-desc">Importe la musique et les vidéos stockées sur ton téléphone pour commencer à écouter.</div>
        <button class="btn-primary" id="btn-scan-empty">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
          Importer mes fichiers
        </button>
      </div>`;
    $('#home-content').classList.toggle('hidden', !hasMedia);
    const btn = $('#btn-scan-empty');
    if (btn) btn.onclick = () => Actions.startScan();
  },

  renderHome() {
    this.renderHomeEmpty();
    if (!Store.tracks.length && !Store.videos.length) return;

    const hour = new Date().getHours();
    $('#home-greeting').textContent = hour < 12 ? 'Bonjour' : hour < 18 ? 'Bon après-midi' : 'Bonsoir';

    // Continuer l'écoute
    const recentTracks = Store.recentlyPlayed.map((id) => Store.getTrack(id)).filter(Boolean).slice(0, 8);
    $('#home-continue').innerHTML = recentTracks.length
      ? recentTracks.map((t) => this.coverCardHTML(t)).join('')
      : `<div style="padding:20px 20px 4px; color:var(--text-faint); font-size:13px;">Rien pour le moment — lance un morceau !</div>`;

    // Favoris
    const favTracks = Store.tracks.filter((t) => Store.favorites.has(t.id)).slice(0, 10);
    $('#home-favorites').innerHTML = favTracks.length
      ? favTracks.map((t) => this.coverCardHTML(t)).join('')
      : `<div style="padding:4px 20px 4px; color:var(--text-faint); font-size:13px;">Appuie sur ♥ pendant l'écoute pour ajouter un favori.</div>`;

    // Récemment ajoutés
    const recent = [...Store.tracks].sort((a, b) => b.dateAdded - a.dateAdded).slice(0, 8);
    $('#home-recent').innerHTML = recent.map((t) => this.trackRowHTML(t)).join('');

    this.bindDynamicHandlers();
  },

  coverCardHTML(t) {
    const bg = t.cover ? `background-image:url(${t.cover})` : '';
    return `
    <div class="cover-card" data-track-id="${t.id}">
      <div class="cover-art" style="${bg}; ${!t.cover ? 'background:linear-gradient(135deg,#20252C,#151920);' : ''}" data-action="play-cover" data-id="${t.id}">
        ${!t.cover ? `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:var(--text-faint);"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg></div>` : ''}
        <div class="play-fab"><svg width="13" height="13" viewBox="0 0 24 24" fill="#fff"><path d="M8 5v14l11-7z"/></svg></div>
      </div>
      <div class="cover-title">${escapeHTML(t.title)}</div>
      <div class="cover-sub">${escapeHTML(t.artist)}</div>
    </div>`;
  },

  trackRowHTML(t, opts = {}) {
    const isPlaying = Store.queue[Store.queueIndex]?.id === t.id;
    const bg = t.cover ? `background-image:url(${t.cover})` : '';
    return `
    <div class="track-row ${isPlaying ? 'playing' : ''}" data-track-id="${t.id}" data-action="play-row">
      <div class="track-thumb" style="${bg}">
        ${!t.cover ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>` : ''}
      </div>
      <div class="track-info">
        <div class="track-name">${escapeHTML(t.title)}</div>
        <div class="track-meta">${escapeHTML(t.artist)} ${t.duration ? '· ' + fmtTime(t.duration) : ''}</div>
      </div>
      <div class="track-actions">
        <button class="icon-btn-sm" data-action="more" data-id="${t.id}" aria-label="Options">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><circle cx="5" cy="12" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="19" cy="12" r="1.8"/></svg>
        </button>
      </div>
    </div>`;
  },

  renderMusic() {
    const seg = MusicView.seg;
    $('#music-count-sub').textContent = `${Store.tracks.length} morceau${Store.tracks.length > 1 ? 'x' : ''}`;
    const body = $('#music-body');
    if (!Store.tracks.length) {
      body.innerHTML = this.emptyMiniHTML('musique');
      this.bindEmptyImport();
      return;
    }

    if (seg === 'songs') {
      const sorted = MusicView.sortTracks([...Store.tracks]);
      body.innerHTML = `<div class="track-list">${sorted.map((t) => this.trackRowHTML(t)).join('')}</div>`;
    } else if (seg === 'albums') {
      const albums = [...Store.albumsMap().values()];
      body.innerHTML = `<div class="grid-2">${albums.map((a) => `
        <div class="grid-tile" data-action="open-album" data-name="${escapeAttr(a.name)}">
          <div class="grid-cover" style="${a.tracks[0].cover ? `background-image:url(${a.tracks[0].cover})` : 'background:linear-gradient(135deg,#20252C,#171A14)'}">
            ${!a.tracks[0].cover ? emptyCoverIconSVG() : ''}
          </div>
          <div class="grid-name">${escapeHTML(a.name)}</div>
          <div class="grid-sub">${a.tracks.length} morceau${a.tracks.length > 1 ? 'x' : ''}</div>
        </div>`).join('')}</div>`;
    } else if (seg === 'artists') {
      const artists = [...Store.artistsMap().values()];
      body.innerHTML = `<div class="grid-2">${artists.map((a) => `
        <div class="grid-tile" data-action="open-artist" data-name="${escapeAttr(a.name)}">
          <div class="grid-cover" style="border-radius:50%; ${a.tracks[0].cover ? `background-image:url(${a.tracks[0].cover})` : 'background:linear-gradient(135deg,#8A9184,#20252C)'}">
            ${!a.tracks[0].cover ? emptyCoverIconSVG() : ''}
          </div>
          <div class="grid-name">${escapeHTML(a.name)}</div>
          <div class="grid-sub">${a.tracks.length} morceau${a.tracks.length > 1 ? 'x' : ''}</div>
        </div>`).join('')}</div>`;
    } else if (seg === 'folders') {
      const folders = [...Store.foldersMap().entries()];
      body.innerHTML = `<div class="track-list">${folders.map(([name, tracks]) => `
        <div class="track-row" data-action="open-folder" data-name="${escapeAttr(name)}">
          <div class="track-thumb"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></svg></div>
          <div class="track-info"><div class="track-name">${escapeHTML(name)}</div><div class="track-meta">${tracks.length} fichier${tracks.length > 1 ? 's' : ''}</div></div>
        </div>`).join('')}</div>`;
    }
    this.bindDynamicHandlers();
  },

  renderVideos() {
    $('#video-count-sub').textContent = `${Store.videos.length} vidéo${Store.videos.length > 1 ? 's' : ''}`;
    const body = $('#video-body');
    if (!Store.videos.length) {
      body.innerHTML = this.emptyMiniHTML('vidéo');
      this.bindEmptyImport();
      return;
    }
    const sorted = [...Store.videos].sort((a, b) => b.dateAdded - a.dateAdded);
    body.innerHTML = `<div class="grid-2">${sorted.map((v) => `
      <div class="grid-tile" data-action="play-video" data-id="${v.id}">
        <div class="grid-cover video-cover" style="${v.thumb ? `background-image:url(${v.thumb})` : 'background:#141618;'}">
          ${!v.thumb ? '<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:var(--text-faint);"><svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="5" width="15" height="14" rx="2"/><path d="M17 10l5-3v10l-5-3"/></svg></div>' : ''}
          <div class="video-dur">${v.duration ? fmtTime(v.duration) : '--:--'}</div>
        </div>
        <div class="grid-name">${escapeHTML(v.title)}</div>
        <div class="grid-sub">${v.folder || ''}</div>
      </div>`).join('')}</div>`;
    this.bindDynamicHandlers();
  },

  emptyMiniHTML(kind) {
    return `<div class="empty-state fade-in">
      <svg class="empty-motif" viewBox="0 0 720 720" fill="none" style="color:var(--sage)"><use href="#headphones-motif"/></svg>
      <div class="empty-title">Aucun fichier ${kind}</div>
      <div class="empty-desc">Importe des fichiers depuis ton téléphone pour remplir cette section.</div>
      <button class="btn-primary" id="btn-scan-inline">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        Importer
      </button>
    </div>`;
  },
  bindEmptyImport() {
    const b = $('#btn-scan-inline');
    if (b) b.onclick = () => Actions.startScan();
  },

  bindDynamicHandlers() {
    $$('[data-action="play-cover"]').forEach((el) => {
      el.onclick = () => Actions.playById(el.dataset.id, Store.recentlyPlayed.map((id) => Store.getTrack(id)).filter(Boolean));
    });
    $$('[data-action="play-row"]').forEach((el) => {
      el.onclick = (e) => {
        if (e.target.closest('[data-action="more"]')) return;
        const id = el.dataset.trackId;
        const list = MusicView.currentListIds().length ? MusicView.currentListIds() : Store.tracks;
        Actions.playById(id, list.length && list[0]?.id !== undefined ? list : Store.tracks);
      };
    });
    $$('[data-action="more"]').forEach((el) => {
      el.onclick = (e) => { e.stopPropagation(); Sheets.openTrackOptions(el.dataset.id); };
    });
    $$('[data-action="open-album"]').forEach((el) => {
      el.onclick = () => MusicView.openCollection('album', el.dataset.name);
    });
    $$('[data-action="open-artist"]').forEach((el) => {
      el.onclick = () => MusicView.openCollection('artist', el.dataset.name);
    });
    $$('[data-action="open-folder"]').forEach((el) => {
      el.onclick = () => MusicView.openCollection('folder', el.dataset.name);
    });
    $$('[data-action="play-video"]').forEach((el) => {
      el.onclick = () => {
        const v = Store.videos.find((x) => x.id === el.dataset.id);
        if (v) VideoEngine.open(v);
      };
    });
  },

  showMiniPlayer() { $('#mini-player').classList.add('show'); },

  renderNowPlaying(track) {
    $('#player-title').textContent = track.title;
    $('#player-artist').textContent = track.artist;
    $('#mini-title').textContent = track.title;
    $('#mini-artist').textContent = track.artist;
    const bg = track.cover ? `url(${track.cover})` : 'linear-gradient(135deg,#20252C,#151920)';
    $('#player-art').style.backgroundImage = track.cover ? `url(${track.cover})` : '';
    $('#player-art').style.background = track.cover ? `url(${track.cover}) center/cover` : 'linear-gradient(135deg,#2A2115,#171A14)';
    $('#mini-thumb').style.background = track.cover ? `url(${track.cover}) center/cover` : 'linear-gradient(135deg,#2A2115,#171A14)';
    $('#player-bg-glow').style.backgroundImage = track.cover ? `url(${track.cover})` : 'none';
    this.updateFavIcon(track.id);
    this.updatePlayState();
    // refresh liste pour indicateur "playing"
    Router.softRefresh();
  },

  updateFavIcon(trackId) {
    const isFav = Store.favorites.has(trackId);
    $('#player-fav').classList.toggle('toggled', isFav);
    $('#player-fav svg').setAttribute('fill', isFav ? 'currentColor' : 'none');
  },

  updatePlayState() {
    const playing = AudioEngine.isPlaying;
    $('#player-play-icon').innerHTML = playing ? '<path d="M6 5h4v14H6zM14 5h4v14h-4z"/>' : '<path d="M8 5v14l11-7z"/>';
    $('#mini-play-icon').innerHTML = playing ? '<path d="M6 5h4v14H6zM14 5h4v14h-4z"/>' : '<path d="M8 5v14l11-7z"/>';
  },

  updateProgress() {
    const el = AudioEngine.el;
    const ratio = el.duration ? el.currentTime / el.duration : 0;
    $('#player-seek-fill').style.width = `${ratio * 100}%`;
    $('#player-seek-handle').style.left = `${ratio * 100}%`;
    $('#player-time-cur').textContent = fmtTime(el.currentTime);
    $('#player-time-dur').textContent = fmtTime(el.duration);
    $('#mini-progress').style.width = `${ratio * 100}%`;
  },
};

function escapeHTML(str = '') {
  return str.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function escapeAttr(str = '') { return escapeHTML(str).replace(/`/g, '&#96;'); }
function emptyCoverIconSVG() {
  return `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:var(--text-faint);"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg></div>`;
}

/* ---------------------------- Music sub-view (albums/artists/folders drill-down) ---------------------------- */
const MusicView = {
  seg: 'songs',
  sort: 'name',
  drill: null, // {type, name}

  sortTracks(list) {
    if (this.sort === 'name') return list.sort((a, b) => a.title.localeCompare(b.title));
    if (this.sort === 'date') return list.sort((a, b) => b.dateAdded - a.dateAdded);
    if (this.sort === 'duration') return list.sort((a, b) => b.duration - a.duration);
    return list;
  },

  currentListIds() {
    return this.sortTracks([...Store.tracks]);
  },

  openCollection(type, name) {
    let list = [];
    if (type === 'album') list = Store.tracks.filter((t) => (t.album || 'Album inconnu') === name);
    if (type === 'artist') list = Store.tracks.filter((t) => (t.artist || 'Artiste inconnu') === name);
    if (type === 'folder') list = Store.tracks.filter((t) => (t.folder || 'Racine') === name);
    const body = $('#music-body');
    body.innerHTML = `
      <div class="section" style="padding-top:0;">
        <button id="drill-back" style="display:flex;align-items:center;gap:6px;color:var(--sage);font-weight:600;font-size:13.5px;margin-bottom:14px;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
          Retour
        </button>
        <div style="font-size:19px;font-weight:700;margin-bottom:2px;">${escapeHTML(name)}</div>
        <div style="font-size:12.5px;color:var(--text-faint);margin-bottom:16px;">${list.length} morceau${list.length > 1 ? 'x' : ''}</div>
      </div>
      <div class="track-list">${list.map((t) => Renderer.trackRowHTML(t)).join('')}</div>
    `;
    $('#drill-back').onclick = () => Renderer.renderMusic();
    Renderer.bindDynamicHandlers();
    $$('.track-row[data-track-id]').forEach((el) => {
      el.onclick = (e) => {
        if (e.target.closest('[data-action="more"]')) return;
        Actions.playById(el.dataset.trackId, list);
      };
    });
  },
};

/* ---------------------------- Sheets (bottom sheets) ---------------------------- */
const Sheets = {
  activeTrackId: null,

  open(id) {
    $('#overlay-scrim').classList.add('show');
    $(id).classList.add('show');
  },
  closeAll() {
    $('#overlay-scrim').classList.remove('show');
    $$('.bottom-sheet').forEach((s) => s.classList.remove('show'));
  },

  openTrackOptions(trackId) {
    const t = Store.getTrack(trackId);
    if (!t) return;
    this.activeTrackId = trackId;
    $('#sheet-track-title').textContent = t.title;
    $('#opt-fav-label').textContent = Store.favorites.has(trackId) ? 'Retirer des favoris' : 'Ajouter aux favoris';
    this.open('#sheet-track-options');
  },
};

/* ---------------------------- Router ---------------------------- */
const Router = {
  current: 'home',
  views: ['home', 'music', 'videos', 'search', 'profile'],

  goto(name) {
    this.current = name;
    this.views.forEach((v) => {
      $(`#view-${v}`).classList.toggle('active', v === name);
    });
    $$('.nav-item').forEach((el) => el.classList.toggle('active', el.dataset.view === name));
    this.renderCurrentView();
  },

  renderCurrentView() {
    if (this.current === 'home') Renderer.renderHome();
    else if (this.current === 'music') Renderer.renderMusic();
    else if (this.current === 'videos') Renderer.renderVideos();
    else if (this.current === 'profile') Profile.render();
    else if (this.current === 'search') SearchView.render();
  },

  softRefresh() {
    // rafraîchit sans changer de vue, pour métadonnées qui arrivent async
    if (this.current === 'home') Renderer.renderHome();
    else if (this.current === 'music' && !MusicView.drill) Renderer.renderMusic();
    else if (this.current === 'videos') Renderer.renderVideos();
  },
};

/* ---------------------------- Search ---------------------------- */
const SearchView = {
  query: '',
  render() {
    const q = this.query.trim().toLowerCase();
    const results = $('#search-results');
    if (!q) {
      results.innerHTML = `<div class="empty-state" style="padding-top:40px;">
        <svg class="empty-motif" viewBox="0 0 720 720" fill="none" style="color:var(--sage); opacity:.4;"><use href="#headphones-motif"/></svg>
        <div class="empty-desc">Cherche parmi ${Store.tracks.length} morceaux et ${Store.videos.length} vidéos.</div>
      </div>`;
      return;
    }
    const tracks = Store.tracks.filter((t) => t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q) || (t.album || '').toLowerCase().includes(q));
    const videos = Store.videos.filter((v) => v.title.toLowerCase().includes(q));

    if (!tracks.length && !videos.length) {
      results.innerHTML = `<div class="empty-state" style="padding-top:40px;"><div class="empty-title">Aucun résultat</div><div class="empty-desc">Essaie un autre mot-clé.</div></div>`;
      return;
    }

    let html = '';
    if (tracks.length) {
      html += `<div class="section"><div class="section-title" style="margin-bottom:10px;">Morceaux</div></div><div class="track-list">${tracks.map((t) => Renderer.trackRowHTML(t)).join('')}</div>`;
    }
    if (videos.length) {
      html += `<div class="section" style="margin-top:14px;"><div class="section-title" style="margin-bottom:10px;">Vidéos</div></div><div class="grid-2">${videos.map((v) => `
        <div class="grid-tile" data-action="play-video" data-id="${v.id}">
          <div class="grid-cover video-cover" style="${v.thumb ? `background-image:url(${v.thumb})` : 'background:#141618;'}"><div class="video-dur">${v.duration ? fmtTime(v.duration) : '--:--'}</div></div>
          <div class="grid-name">${escapeHTML(v.title)}</div>
        </div>`).join('')}</div>`;
    }
    results.innerHTML = html;
    Renderer.bindDynamicHandlers();
    $$('.track-row[data-track-id]').forEach((el) => {
      el.onclick = (e) => {
        if (e.target.closest('[data-action="more"]')) return;
        Actions.playById(el.dataset.trackId, tracks);
      };
    });
  },
};

/* ---------------------------- Profile ---------------------------- */
const Profile = {
  render() {
    $('#profile-stats').textContent = `${Store.tracks.length} morceaux · ${Store.videos.length} vidéos`;
    $('#profile-folders').textContent = Store.foldersMap().size;
  },
};

/* ---------------------------- Actions (haut niveau, liées aux events) ---------------------------- */
const Actions = {
  async startScan() {
    if (Store.scanning) return;
    const files = await Scanner.pickFiles();
    if (!files.length) { toast('Aucun fichier sélectionné'); return; }
    Router.goto('home');
    await Scanner.processFiles(files);
  },

  playById(id, list) {
    const t = Store.getTrack(id);
    if (!t) return;
    AudioEngine.playTrack(t, list && list.length ? list : Store.tracks);
    Player.open();
  },
};

/* ---------------------------- Player sheet controller ---------------------------- */
const Player = {
  open() { $('#player-sheet').classList.add('show'); },
  close() { $('#player-sheet').classList.remove('show'); },
};

/* ============================================================
   EVENT BINDINGS
   ============================================================ */
document.addEventListener('DOMContentLoaded', () => {
  VideoEngine.init();

  // Nav
  $$('.nav-item').forEach((el) => {
    el.addEventListener('click', () => Router.goto(el.dataset.view));
  });
  $('#btn-home-search').addEventListener('click', () => Router.goto('search'));

  // Mini player
  $('#mini-player').addEventListener('click', (e) => {
    if (e.target.closest('#mini-playpause') || e.target.closest('#mini-next')) return;
    Player.open();
  });
  $('#mini-playpause').addEventListener('click', () => AudioEngine.toggle());
  $('#mini-next').addEventListener('click', () => AudioEngine.next());

  // Full player
  $('#player-collapse').addEventListener('click', () => Player.close());
  $('#player-playpause').addEventListener('click', () => AudioEngine.toggle());
  $('#player-next').addEventListener('click', () => AudioEngine.next());
  $('#player-prev').addEventListener('click', () => AudioEngine.prev());
  $('#player-shuffle').addEventListener('click', (e) => {
    AudioEngine.shuffle = !AudioEngine.shuffle;
    e.currentTarget.classList.toggle('toggled', AudioEngine.shuffle);
    toast(AudioEngine.shuffle ? 'Lecture aléatoire activée' : 'Lecture aléatoire désactivée');
  });
  $('#player-repeat').addEventListener('click', (e) => {
    const modes = ['off', 'all', 'one'];
    AudioEngine.repeat = modes[(modes.indexOf(AudioEngine.repeat) + 1) % 3];
    e.currentTarget.classList.toggle('toggled', AudioEngine.repeat !== 'off');
    toast(AudioEngine.repeat === 'off' ? 'Répétition désactivée' : AudioEngine.repeat === 'all' ? 'Répéter tout' : 'Répéter le morceau');
  });
  $('#player-fav').addEventListener('click', () => {
    const t = Store.queue[Store.queueIndex];
    if (!t) return;
    Store.toggleFav(t.id);
    Renderer.updateFavIcon(t.id);
    Renderer.renderHome();
  });
  $('#player-sleep-btn').addEventListener('click', () => Sheets.open('#sheet-sleep'));
  $('#player-eq-btn').addEventListener('click', () => Sheets.open('#sheet-eq'));
  $('#player-addplaylist').addEventListener('click', () => {
    const t = Store.queue[Store.queueIndex];
    if (t) { Sheets.activeTrackId = t.id; PlaylistUI.openPicker(); }
  });
  $('#player-share').addEventListener('click', () => toast('Partage indisponible hors ligne'));

  // Seek bar (audio)
  let seeking = false;
  const seekEl = $('#player-seek');
  const doSeek = (clientX) => {
    const rect = seekEl.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    AudioEngine.seekTo(ratio);
    $('#player-seek-fill').style.width = `${ratio * 100}%`;
    $('#player-seek-handle').style.left = `${ratio * 100}%`;
  };
  seekEl.addEventListener('touchstart', (e) => { seeking = true; doSeek(e.touches[0].clientX); }, { passive: true });
  seekEl.addEventListener('touchmove', (e) => { if (seeking) doSeek(e.touches[0].clientX); }, { passive: true });
  seekEl.addEventListener('touchend', () => { seeking = false; });
  seekEl.addEventListener('mousedown', (e) => { seeking = true; doSeek(e.clientX); });
  window.addEventListener('mousemove', (e) => { if (seeking) doSeek(e.clientX); });
  window.addEventListener('mouseup', () => { seeking = false; });

  // Video player controls
  $('#video-close').addEventListener('click', () => VideoEngine.close());
  $('#video-playpause').addEventListener('click', () => VideoEngine.toggle());
  $('#video-back10').addEventListener('click', () => VideoEngine.seekRelative(-10));
  $('#video-fwd10').addEventListener('click', () => VideoEngine.seekRelative(10));
  $('#video-cc').addEventListener('click', () => toast('Aucun sous-titre trouvé pour cette vidéo'));
  $('#video-cast').addEventListener('click', () => toast('Diffusion indisponible hors ligne'));
  $('#video-rotate').addEventListener('click', async () => {
    try {
      const isLandscape = screen.orientation?.type?.includes('landscape');
      await screen.orientation?.lock(isLandscape ? 'portrait' : 'landscape');
    } catch (e) { toast('Rotation non supportée sur cet appareil'); }
  });
  let vseeking = false;
  const vseekEl = $('#video-seek');
  const doVSeek = (clientX) => {
    const rect = vseekEl.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    if (VideoEngine.el.duration) VideoEngine.el.currentTime = ratio * VideoEngine.el.duration;
  };
  vseekEl.addEventListener('touchstart', (e) => { vseeking = true; doVSeek(e.touches[0].clientX); }, { passive: true });
  vseekEl.addEventListener('touchmove', (e) => { if (vseeking) doVSeek(e.touches[0].clientX); }, { passive: true });
  vseekEl.addEventListener('touchend', () => { vseeking = false; });

  // Music segment tabs
  $$('#music-seg-tabs .seg-tab').forEach((el) => {
    el.addEventListener('click', () => {
      $$('#music-seg-tabs .seg-tab').forEach((s) => s.classList.remove('active'));
      el.classList.add('active');
      MusicView.seg = el.dataset.seg;
      Renderer.renderMusic();
    });
  });
  $('#btn-music-sort').addEventListener('click', () => Sheets.open('#sheet-sort'));
  $('#btn-video-sort').addEventListener('click', () => toast('Tri : plus récent en premier'));

  // Sort sheet
  $$('.sort-opt').forEach((el) => {
    el.addEventListener('click', () => {
      MusicView.sort = el.dataset.sort;
      Sheets.closeAll();
      Renderer.renderMusic();
      toast('Trié');
    });
  });

  // Sleep sheet
  $$('.sleep-opt').forEach((el) => {
    el.addEventListener('click', () => {
      const min = parseInt(el.dataset.min, 10);
      AudioEngine.setSleepTimer(min);
      Sheets.closeAll();
      toast(min ? `Minuteur réglé sur ${min} min` : 'Minuteur désactivé');
    });
  });

  // EQ sheet
  const eqBandsEl = $('#eq-bands');
  const eqLabels = ['60', '250', '1K', '4K', '12K'];
  eqLabels.forEach((label, i) => {
    const wrap = document.createElement('div');
    wrap.style.cssText = 'display:flex;flex-direction:column;align-items:center;gap:8px;flex:1;';
    wrap.innerHTML = `
      <input type="range" min="-12" max="12" value="0" data-band="${i}" style="writing-mode:bt-lr;-webkit-appearance:slider-vertical;width:24px;height:90px;accent-color:var(--amber);">
      <span style="font-size:10.5px;color:var(--text-faint);">${label}</span>`;
    eqBandsEl.appendChild(wrap);
  });
  $$('#eq-bands input[type=range]').forEach((slider) => {
    slider.addEventListener('input', () => {
      const gains = $$('#eq-bands input[type=range]').map((s) => parseFloat(s.value));
      AudioEngine.setEQGains(gains);
    });
  });
  $$('.eq-preset').forEach((el) => {
    el.addEventListener('click', () => {
      const presets = {
        flat: [0, 0, 0, 0, 0],
        bass: [7, 4, 0, -1, -1],
        vocal: [-2, 0, 4, 4, 1],
        treble: [-1, -1, 0, 4, 7],
      };
      const gains = presets[el.dataset.preset];
      $$('#eq-bands input[type=range]').forEach((s, i) => { s.value = gains[i]; });
      AudioEngine.setEQGains(gains);
      toast('Préréglage appliqué');
    });
  });

  // Track options sheet
  $('#opt-play').addEventListener('click', () => {
    Actions.playById(Sheets.activeTrackId, Store.tracks);
    Sheets.closeAll();
  });
  $('#opt-fav').addEventListener('click', () => {
    Store.toggleFav(Sheets.activeTrackId);
    Sheets.closeAll();
    Router.softRefresh();
    toast('Favoris mis à jour');
  });
  $('#opt-addqueue').addEventListener('click', () => {
    const t = Store.getTrack(Sheets.activeTrackId);
    if (t) { Store.queue.push(t); toast('Ajouté à la file de lecture'); }
    Sheets.closeAll();
  });
  $('#opt-playlist').addEventListener('click', () => { Sheets.closeAll(); PlaylistUI.openPicker(); });
  $('#opt-share').addEventListener('click', () => { toast('Partage indisponible hors ligne'); Sheets.closeAll(); });

  // Scrim closes sheets
  $('#overlay-scrim').addEventListener('click', () => Sheets.closeAll());

  // Search input
  $('#search-input').addEventListener('input', (e) => {
    SearchView.query = e.target.value;
    $('#search-clear').classList.toggle('hidden', !e.target.value);
    SearchView.render();
  });
  $('#search-clear').addEventListener('click', () => {
    $('#search-input').value = '';
    SearchView.query = '';
    $('#search-clear').classList.add('hidden');
    SearchView.render();
  });

  // Banner discover
  $('#banner-play').addEventListener('click', () => {
    if (!Store.tracks.length) { Actions.startScan(); return; }
    const shuffled = [...Store.tracks].sort(() => Math.random() - 0.5);
    Actions.playById(shuffled[0].id, shuffled);
  });

  // Profile actions
  $('#row-rescan').addEventListener('click', () => Actions.startScan());
  $('#row-sleep').addEventListener('click', () => Sheets.open('#sheet-sleep'));
  $('#row-eq').addEventListener('click', () => Sheets.open('#sheet-eq'));
  $('#switch-theme').addEventListener('click', (e) => {
    e.currentTarget.classList.toggle('on');
    toast('Le mode clair arrive bientôt');
  });
  $('#switch-autoresume').addEventListener('click', (e) => e.currentTarget.classList.toggle('on'));

  Renderer.renderHome();

  // Enregistrement du service worker (installabilité PWA)
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
});

/* ---------------------------- Playlist UI ---------------------------- */
const PlaylistUI = {
  openPicker() {
    const list = $('#playlist-pick-list');
    list.innerHTML = Store.playlists.map((p) => `
      <div class="sheet-option pick-playlist" data-id="${p.id}">
        <div class="sheet-option-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg></div>
        ${escapeHTML(p.name)} <span style="margin-left:auto;color:var(--text-faint);font-size:12px;">${p.trackIds.length}</span>
      </div>`).join('') || `<div style="padding:14px 4px;color:var(--text-faint);font-size:13px;">Aucune playlist pour l'instant.</div>`;
    $$('.pick-playlist').forEach((el) => {
      el.onclick = () => {
        const p = Store.playlists.find((x) => x.id === el.dataset.id);
        if (p && Sheets.activeTrackId && !p.trackIds.includes(Sheets.activeTrackId)) {
          p.trackIds.push(Sheets.activeTrackId);
          Store.savePlaylists();
          toast(`Ajouté à « ${p.name} »`);
        }
        Sheets.closeAll();
      };
    });
    Sheets.open('#sheet-playlist-pick');
  },

  createNew() {
    const name = prompt('Nom de la playlist :');
    if (!name) return;
    const p = { id: uid(), name: name.trim(), trackIds: Sheets.activeTrackId ? [Sheets.activeTrackId] : [] };
    Store.playlists.push(p);
    Store.savePlaylists();
    toast(`Playlist « ${p.name} » créée`);
    Sheets.closeAll();
  },
};

document.addEventListener('DOMContentLoaded', () => {
  $('#opt-new-playlist').addEventListener('click', () => PlaylistUI.createNew());
});
