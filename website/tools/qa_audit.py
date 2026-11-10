"""Strict QA audit for the Travel Guide landing page.

Measures the 12 verification checks at five viewports, captures full-page
screenshots, and writes JSON results. Read-only except for screenshots.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "shots"
OUT.mkdir(exist_ok=True)
URL = "http://localhost:8080"

VIEWPORTS = [
    (1440, 1000, False),
    (1180, 900, False),
    (768, 1024, False),
    (390, 844, True),
    (320, 740, True),
]

GENERAL = r"""
() => {
  const abs = (el) => {
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return {
      left: +r.left.toFixed(1), top: +(r.top + scrollY).toFixed(1),
      right: +r.right.toFixed(1), bottom: +(r.bottom + scrollY).toFixed(1),
      width: +r.width.toFixed(1), height: +r.height.toFixed(1)
    };
  };
  const overlapW = (a, b) => Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
  const overlapH = (a, b) => Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
  const intersects = (a, b) => overlapW(a, b) > 0.5 && overlapH(a, b) > 0.5;

  const phoneType = (p) => {
    if (p.querySelector('.app-detail')) return 'poi-detail';
    if (p.querySelector('.app-player')) return 'now-playing';
    if (p.querySelector('.app-nearby')) return 'nearby';
    if (p.querySelector('.app-packs')) return 'packs';
    if (p.querySelector('.app--dark')) return 'companion-dark';
    if (p.querySelector('.app-map')) return 'map';
    return 'unknown';
  };

  const phones = [...document.querySelectorAll('.phone')].map((p, i) => ({
    i, type: phoneType(p),
    status: !!p.querySelector('.app-status'),
    home: !!p.querySelector('.app-home'),
    screen: !!p.querySelector('.phone__screen'),
    island: !!p.querySelector('.phone__island'),
    rect: abs(p),
  }));

  const revealImgs = [...document.querySelectorAll('.reveal-img')].map((el) => {
    const cs = getComputedStyle(el);
    const r = el.getBoundingClientRect();
    return {
      cls: el.className,
      visible: el.classList.contains('is-visible'),
      clipPath: cs.clipPath,
      opacity: cs.opacity,
      rect: abs(el),
      img: el.querySelector('img') ? {
        complete: el.querySelector('img').complete,
        naturalWidth: el.querySelector('img').naturalWidth,
        visible: el.querySelector('img').getBoundingClientRect().width > 0
      } : null
    };
  });

  const imgSelectors = [
    '.float-card img', '.app-map__img', '.value-card__media img',
    '.poi-row__img img', '.detail-hero img', '.player-art img',
    '.cta__media img', '.fieldnotes > img'
  ];
  const imageChecks = [];
  for (const sel of imgSelectors) {
    document.querySelectorAll(sel).forEach((img, idx) => {
      const r = img.getBoundingClientRect();
      const cs = getComputedStyle(img);
      imageChecks.push({
        sel, idx,
        loaded: img.complete && img.naturalWidth > 0,
        naturalWidth: img.naturalWidth, naturalHeight: img.naturalHeight,
        renderedWidth: +r.width.toFixed(1), renderedHeight: +r.height.toFixed(1),
        upscalePx: +(r.width - img.naturalWidth).toFixed(1),
        objectFit: cs.objectFit,
        src: img.getAttribute('src')
      });
    });
  }
  const allImages = [...document.images].map((img) => ({
    src: img.getAttribute('src'),
    loaded: img.complete && img.naturalWidth > 0,
    naturalWidth: img.naturalWidth,
    renderedWidth: +img.getBoundingClientRect().width.toFixed(1),
    upscalePx: +(img.getBoundingClientRect().width - img.naturalWidth).toFixed(1)
  }));

  const pill = document.querySelector('#audioPill');
  const bars = [...document.querySelectorAll('.audio-pill__wave i')];
  const audio = {
    exists: !!pill,
    pressed: pill ? pill.getAttribute('aria-pressed') : null,
    playingClass: pill ? pill.classList.contains('is-playing') : null,
    text: pill ? pill.innerText.replace(/\s+/g, ' ').trim() : null,
    meta: document.querySelector('.audio-pill__meta')?.innerText || null,
    srOnly: document.querySelector('#audioPill .sr-only')?.innerText || null,
    audioElements: document.querySelectorAll('audio').length,
    bars: bars.map((b) => ({
      animationName: getComputedStyle(b).animationName,
      animationDuration: getComputedStyle(b).animationDuration,
      animationPlayState: getComputedStyle(b).animationPlayState
    }))
  };

  const stages = [...document.querySelectorAll('.stage')].map((s) => {
    const primary = s.querySelector('.stage__trail path:not(.dash):not(.trail-progress)');
    return {
      dots: s.querySelectorAll('.trail-dot').length,
      nodes: s.querySelectorAll('.trail-node').length,
      progressPaths: s.querySelectorAll('.trail-progress').length,
      cards: s.querySelectorAll('.float-card').length,
      cardsHit: s.querySelectorAll('.float-card.is-hit').length,
      nodesHit: s.querySelectorAll('.trail-node.is-hit').length,
      trailDrawn: primary ? getComputedStyle(primary).strokeDashoffset : null
    };
  });

  const firstStage = document.querySelector('#how .stage');
  let firstTrail = null;
  if (firstStage) {
    const primary = firstStage.querySelector('.stage__trail path:not(.dash):not(.trail-progress)');
    const dot = firstStage.querySelector('.trail-dot');
    const total = primary ? primary.getTotalLength() : 0;
    const start = primary ? primary.getPointAtLength(0) : null;
    const end = primary ? primary.getPointAtLength(total) : null;
    const d = dot ? { cx: +dot.getAttribute('cx'), cy: +dot.getAttribute('cy') } : null;
    const dist = (a, b) => a && b ? +Math.hypot(a.cx - b.x, a.cy - b.y).toFixed(2) : null;
    firstTrail = {
      dot: d, start: start ? { x: +start.x.toFixed(1), y: +start.y.toFixed(1) } : null,
      end: end ? { x: +end.x.toFixed(1), y: +end.y.toFixed(1) } : null,
      distToStart: dist(d, start), distToEnd: dist(d, end),
      cardsHit: firstStage.querySelectorAll('.float-card.is-hit').length,
      nodesHit: firstStage.querySelectorAll('.trail-node.is-hit').length
    };
  }

  const svg = {
    iconCount: document.querySelectorAll('svg.icon').length,
    shapeRenderings: [...new Set([...document.querySelectorAll('svg.icon')].map((el) => getComputedStyle(el).shapeRendering))],
    trailPathCount: document.querySelectorAll('.stage__trail path').length,
    trailVectorEffects: [...new Set([...document.querySelectorAll('.stage__trail path')].map((el) => getComputedStyle(el).vectorEffect))],
    routePathCount: document.querySelectorAll('.app-map__route path').length,
    routeVectorEffects: [...new Set([...document.querySelectorAll('.app-map__route path')].map((el) => getComputedStyle(el).vectorEffect))]
  };

  const blockSel = '.hero__copy,.hero__aside,.hero__banner,.stat,.section-head,.feature__copy,.stage,.value-card,.companion__media,.fieldnotes,.privacy-item,.privacy-note,.cta__copy,.cta__media';
  const sections = [...document.querySelectorAll('main > section')].map((sec) => {
    const sr = abs(sec);
    const blocks = [...sec.querySelectorAll(blockSel)].filter((b) => b.getClientRects().length).map(abs);
    const intervals = blocks.map((b) => [Math.max(sr.top, b.top), Math.min(sr.bottom, b.bottom)])
      .filter(([a, b]) => b > a).sort((a, b) => a[0] - b[0]);
    const merged = [];
    for (const iv of intervals) {
      if (!merged.length || iv[0] > merged[merged.length - 1][1]) merged.push([...iv]);
      else merged[merged.length - 1][1] = Math.max(merged[merged.length - 1][1], iv[1]);
    }
    let largestGap = 0, gapAt = null, cursor = sr.top;
    for (const [a, b] of merged) {
      if (a - cursor > largestGap) { largestGap = a - cursor; gapAt = cursor; }
      cursor = Math.max(cursor, b);
    }
    if (sr.bottom - cursor > largestGap) { largestGap = sr.bottom - cursor; gapAt = cursor; }
    return {
      id: sec.id || null, cls: sec.className, height: sr.height,
      top: sr.top, bottom: sr.bottom,
      contentTop: merged.length ? +merged[0][0].toFixed(1) : null,
      contentBottom: merged.length ? +merged[merged.length - 1][1].toFixed(1) : null,
      largestGap: +largestGap.toFixed(1), gapAt: gapAt === null ? null : +gapAt.toFixed(1)
    };
  });

  // Satellite positioning is only switched on below 640px.
  let satellites = null;
  const howStage = document.querySelector('#how .stage');
  if (howStage) {
    const phone = howStage.querySelector('.stage__phone .phone');
    const pr = abs(phone);
    const cards = [...howStage.querySelectorAll('.float-card')].map((c) => {
      const cr = abs(c);
      return {
        cls: c.className.replace('float-card ', ''),
        rect: cr,
        overlapWidth: +overlapW(cr, pr).toFixed(1),
        overlapHeight: +overlapH(cr, pr).toFixed(1),
        overlapArea: +(overlapW(cr, pr) * overlapH(cr, pr)).toFixed(1)
      };
    });
    const guarded = [
      ['place-card', howStage.querySelector('.app-place-card')],
      ['preview-title', howStage.querySelector('.app-preview__main h4')],
      ... [...howStage.querySelectorAll('.app-nav__item')].map((el, i) => ['nav-' + (i + 1), el])
    ].map(([name, el]) => {
      const r = abs(el);
      const covers = cards.filter((c) => intersects(r, c.rect)).map((c) => c.cls);
      return { name, text: el ? el.innerText.replace(/\s+/g, ' ').trim() : null, rect: r, coveredBy: covers };
    });
    const badges = [...howStage.querySelectorAll('.float-card')].map((c) => {
      const b = c.querySelector('.trigger-badge');
      const l = c.querySelector('.trigger-badge__label');
      const br = abs(b);
      return {
        card: c.className.replace('float-card ', ''),
        badgeText: l ? l.innerText.replace(/\s+/g, ' ').trim() : null,
        badgeWidth: br ? br.width : null,
        cardWidth: abs(c).width,
        labelScrollWidth: l ? l.scrollWidth : null,
        labelClientWidth: l ? l.clientWidth : null,
        textOverflowPx: l ? l.scrollWidth - l.clientWidth : null,
        insideLayout: b ? (b.offsetLeft >= -0.5 && b.offsetLeft + b.offsetWidth <= c.clientWidth + 0.5) : null,
        cardClientWidth: c.clientWidth,
        badgeOffsetLeft: b ? b.offsetLeft : null,
        badgeOffsetWidth: b ? b.offsetWidth : null
      };
    });
    satellites = { phone: pr, cards, guarded, badges };
  }

  const cs = getComputedStyle(document.documentElement);
  return {
    viewport: { innerWidth, innerHeight, dpr: devicePixelRatio },
    doc: {
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
      bodyScrollWidth: document.body.scrollWidth,
      scrollHeight: document.documentElement.scrollHeight,
      bodyScrollHeight: document.body.scrollHeight,
      scrollY
    },
    horizontalOverflowPx: document.documentElement.scrollWidth - innerWidth,
    phones, phoneCount: phones.length,
    revealImgs, revealCount: revealImgs.length,
    imageChecks, allImages,
    brokenImages: allImages.filter((i) => !i.loaded),
    upscaledImages: allImages.filter((i) => i.upscalePx > 1),
    audio, pill,
    stages, firstTrail,
    svg,
    sections,
    satellites,
    bodyFont: getComputedStyle(document.body).fontFamily,
    overflowX: cs.overflowX
  };
}
"""

MASKS = r"""
() => {
  const style = document.createElement('style');
  style.id = 'qa-no-rotate';
  style.textContent = '.stage__phone, .hero__devices .phone { transform: none !important; }';
  document.head.appendChild(style);

  const abs = (el) => {
    const r = el.getBoundingClientRect();
    return { left: r.left, top: r.top, right: r.right, bottom: r.bottom, width: r.width, height: r.height };
  };
  const clamp = (v, a, b) => Math.min(Math.max(v, a), b);
  const ellipseViolation = (x, y, cx, cy, rx, ry) => {
    const dx = (x - cx) / rx, dy = (y - cy) / ry;
    const d = Math.hypot(dx, dy);
    return d <= 1 ? 0 : (d - 1) * Math.min(rx, ry);
  };
  const cornerOverflow = (r, sr, rx, ry) => {
    const inner = { left: sr.left + rx, right: sr.right - rx, top: sr.top + ry, bottom: sr.bottom - ry };
    const corners = [[r.left, r.top], [r.right, r.top], [r.left, r.bottom], [r.right, r.bottom]];
    let max = 0;
    for (const [x, y] of corners) {
      let v = 0;
      if (x < inner.left && y < inner.top) v = ellipseViolation(x, y, inner.left, inner.top, rx, ry);
      else if (x > inner.right && y < inner.top) v = ellipseViolation(x, y, inner.right, inner.top, rx, ry);
      else if (x < inner.left && y > inner.bottom) v = ellipseViolation(x, y, inner.left, inner.bottom, rx, ry);
      else if (x > inner.right && y > inner.bottom) v = ellipseViolation(x, y, inner.right, inner.bottom, rx, ry);
      else if (x < sr.left) v = sr.left - x;
      else if (x > sr.right) v = x - sr.right;
      else if (y < sr.top) v = sr.top - y;
      else if (y > sr.bottom) v = y - sr.bottom;
      max = Math.max(max, v);
    }
    return +max.toFixed(2);
  };

  const out = [...document.querySelectorAll('.phone')].map((p, i) => {
    const screen = p.querySelector('.phone__screen');
    const sr = abs(screen);
    const srAbs = { left: sr.left, right: sr.right, top: sr.top, bottom: sr.bottom, width: sr.width, height: sr.height };
    // Percentages in the stylesheet: 15.79% / 7.09%.
    const rx = sr.width * 0.1579, ry = sr.height * 0.0709;
    const time = p.querySelector('.app-status__time');
    const icons = p.querySelector('.app-status__icons');
    const home = p.querySelector('.app-home');
    const status = p.querySelector('.app-status');
    const part = (el) => {
      if (!el) return null;
      const r = abs(el);
      return {
        rect: { left: +r.left.toFixed(1), top: +r.top.toFixed(1), right: +r.right.toFixed(1), bottom: +r.bottom.toFixed(1), width: +r.width.toFixed(1), height: +r.height.toFixed(1) },
        cornerOverflow: cornerOverflow(r, srAbs, rx, ry),
        inset: {
          left: +(r.left - sr.left).toFixed(1), top: +(r.top - sr.top).toFixed(1),
          right: +(sr.right - r.right).toFixed(1), bottom: +(sr.bottom - r.bottom).toFixed(1)
        }
      };
    };
    return {
      i,
      type: p.querySelector('.app-detail') ? 'poi-detail' : p.querySelector('.app-player') ? 'now-playing'
        : p.querySelector('.app-nearby') ? 'nearby' : p.querySelector('.app-packs') ? 'packs'
        : p.querySelector('.app--dark') ? 'companion-dark' : p.querySelector('.app-map') ? 'map' : 'unknown',
      screenRadius: { rx: +rx.toFixed(2), ry: +ry.toFixed(2) },
      statusContainer: part(status), time: part(time), icons: part(icons), home: part(home),
      statusVerticalInside: status ? (status.getBoundingClientRect().top >= sr.top - 0.5 && status.getBoundingClientRect().bottom <= sr.bottom + 0.5) : null
    };
  });
  style.remove();
  return out;
}
"""

TRAIL_SCROLL = r"""
(which) => {
  const s = document.querySelector('#how .stage');
  const r = s.getBoundingClientRect();
  const top = r.top + scrollY;
  const y = which === 'center' ? top + r.height / 2 - innerHeight / 2
                               : top + r.height - innerHeight * 0.2 + 12;
  window.scrollTo({ top: y, behavior: 'instant' });
  return { target: +y.toFixed(1), scrollY: +scrollY.toFixed(1), stageTop: +s.getBoundingClientRect().top.toFixed(1) };
}
"""

SAT_COVER = r"""
() => {
  const stage = document.querySelector('#how .stage');
  if (!stage) return null;
  const r = stage.getBoundingClientRect();
  window.scrollTo({ top: r.top + scrollY + r.height / 2 - innerHeight / 2, behavior: 'instant' });
  const cards = [...stage.querySelectorAll('.float-card')];
  const guarded = [
    ['status-time', stage.querySelector('.app-status__time')],
    ['status-icons', stage.querySelector('.app-status__icons')],
    ['place-icon', stage.querySelector('.app-place-card__icon')],
    ['place-text', stage.querySelector('.app-place-card__text')],
    ['place-sliders', stage.querySelector('.app-square-btn')],
    ... [...stage.querySelectorAll('.app-pills .app-pill')].map((el, i) => ['pill-' + (i + 1), el]),
    ['preview-head', stage.querySelector('.app-preview__head')],
    ['preview-title', stage.querySelector('.app-preview__main h4')],
    ['preview-actions', stage.querySelector('.app-preview__actions')],
    ... [...stage.querySelectorAll('.app-nav__item')].map((el, i) => ['nav-' + (i + 1), el]),
    ... [...stage.querySelectorAll('.app-nav__pill')].map((el, i) => ['nav-pill-' + (i + 1), el])
  ];
  const out = guarded.map(([name, el]) => {
    if (!el) return { name, missing: true };
    const er = el.getBoundingClientRect();
    let total = 0, covered = 0, examples = [];
    const cols = 7, rows = 5;
    for (let i = 0; i < cols; i++) {
      for (let j = 0; j < rows; j++) {
        const x = er.left + 1 + (er.width - 2) * (i / (cols - 1));
        const y = er.top + 1 + (er.height - 2) * (j / (rows - 1));
        if (x < 0 || y < 0 || x > innerWidth || y > innerHeight) continue;
        total++;
        const stack = document.elementsFromPoint(x, y);
        const top = stack[0];
        if (top && top.closest && top.closest('.float-card')) {
          covered++;
          if (examples.length < 3) examples.push({
            x: +x.toFixed(1), y: +y.toFixed(1),
            top: top.className || top.tagName,
            card: top.closest('.float-card').className.replace('float-card ', '')
          });
        }
      }
    }
    return {
      name, text: el.innerText ? el.innerText.replace(/\s+/g, ' ').trim().slice(0, 50) : '',
      rect: { left: +er.left.toFixed(1), top: +er.top.toFixed(1), right: +er.right.toFixed(1), bottom: +er.bottom.toFixed(1) },
      samples: total, coveredSamples: covered, coveredPct: total ? +(covered / total * 100).toFixed(0) : 0, examples
    };
  });
  const cardsNow = cards.map((c) => {
    const cr = c.getBoundingClientRect();
    return { cls: c.className.replace('float-card ', ''), hit: c.classList.contains('is-hit'), rect: { left: +cr.left.toFixed(1), top: +cr.top.toFixed(1), right: +cr.right.toFixed(1), bottom: +cr.bottom.toFixed(1) } };
  });
  return { guarded: out, cards: cardsNow, staged: stage.classList.contains('is-trail-live') };
}
"""

TRAIL_STATE = r"""
() => {
  const stage = document.querySelector('#how .stage');
  if (!stage) return null;
  const primary = stage.querySelector('.stage__trail path:not(.dash):not(.trail-progress)');
  const progress = stage.querySelector('.trail-progress');
  const dot = stage.querySelector('.trail-dot');
  const total = primary.getTotalLength();
  const start = primary.getPointAtLength(0);
  const end = primary.getPointAtLength(total);
  const d = { cx: +dot.getAttribute('cx'), cy: +dot.getAttribute('cy') };
  const sr = stage.getBoundingClientRect();
  const vh = innerHeight;
  const expected = Math.max(0, Math.min(1, (vh * 0.8 - sr.top) / (vh * 0.8 + sr.height - vh * 0.2)));
  const off = parseFloat(progress.style.strokeDashoffset || '0');
  return {
    scrollY: +scrollY.toFixed(1),
    stageTop: +sr.top.toFixed(1), stageHeight: +sr.height.toFixed(1),
    progressFromOffset: +(1 - off / total).toFixed(3),
    progressExpected: +expected.toFixed(3),
    dot: d,
    start: { x: +start.x.toFixed(1), y: +start.y.toFixed(1) },
    end: { x: +end.x.toFixed(1), y: +end.y.toFixed(1) },
    distToStart: +Math.hypot(d.cx - start.x, d.cy - start.y).toFixed(2),
    distToEnd: +Math.hypot(d.cx - end.x, d.cy - end.y).toFixed(2),
    cardsHit: stage.querySelectorAll('.float-card.is-hit').length,
    cardsTotal: stage.querySelectorAll('.float-card').length,
    nodesHit: stage.querySelectorAll('.trail-node.is-hit').length,
    nodesTotal: stage.querySelectorAll('.trail-node').length,
    dashoffset: off
  };
}
"""

REDUCED = r"""
() => {
  const abs = (el) => { const r = el.getBoundingClientRect(); return { w: r.width, h: r.height, top: r.top + scrollY }; };
  const revealImgs = [...document.querySelectorAll('.reveal-img')].map((el) => ({
    cls: el.className,
    visible: el.classList.contains('is-visible'),
    clipPath: getComputedStyle(el).clipPath,
    opacity: getComputedStyle(el).opacity,
    rect: abs(el)
  }));
  const stages = [...document.querySelectorAll('.stage')].map((s) => {
    const primary = s.querySelector('.stage__trail path:not(.dash):not(.trail-progress)');
    const dot = s.querySelector('.trail-dot');
    const total = primary.getTotalLength();
    const end = primary.getPointAtLength(total);
    const d = { cx: +dot.getAttribute('cx'), cy: +dot.getAttribute('cy') };
    return {
      dots: s.querySelectorAll('.trail-dot').length,
      nodes: s.querySelectorAll('.trail-node').length,
      progressPaths: s.querySelectorAll('.trail-progress').length,
      nodesHit: s.querySelectorAll('.trail-node.is-hit').length,
      cards: s.querySelectorAll('.float-card').length,
      cardsHit: s.querySelectorAll('.float-card.is-hit').length,
      dotDistToEnd: +Math.hypot(d.cx - end.x, d.cy - end.y).toFixed(2),
      dot: d, end: { x: +end.x.toFixed(1), y: +end.y.toFixed(1) },
      progressOffset: parseFloat(s.querySelector('.trail-progress').style.strokeDashoffset || '0'),
      total: +total.toFixed(1)
    };
  });
  return {
    revealImgs,
    stages,
    waveAnimation: [...document.querySelectorAll('.audio-pill__wave i')].map((b) => ({
      name: getComputedStyle(b).animationName,
      duration: getComputedStyle(b).animationDuration
    })),
    pageHeight: document.documentElement.scrollHeight,
    horizontalOverflowPx: document.documentElement.scrollWidth - innerWidth
  };
}
"""


def attach_errors(page, sink):
    page.on("console", lambda m: sink["console"].append(f"{m.type}: {m.text}") if m.type == "error" else None)
    page.on("pageerror", lambda e: sink["pageerrors"].append(str(e)))
    page.on("requestfailed", lambda r: sink["failed"].append(f"{r.url} :: {r.failure}"))


def block_media(page):
    page.add_init_script("""
      window.__qaSpeech = 0;
      window.__qaAudioCtor = 0;
      if (window.speechSynthesis && window.speechSynthesis.speak) {
        const orig = window.speechSynthesis.speak.bind(window.speechSynthesis);
        window.speechSynthesis.speak = function () { window.__qaSpeech++; return orig.apply(null, arguments); };
      }
      const OrigAudio = window.Audio;
      if (OrigAudio) {
        window.Audio = function () { window.__qaAudioCtor++; return new OrigAudio(...arguments); };
        window.Audio.prototype = OrigAudio.prototype;
      }
    """)


def human_scroll(page):
    total = page.evaluate("document.body.scrollHeight")
    vh = page.viewport_size["height"]
    step = max(380, vh // 2)
    for y in range(0, total + step, step):
        page.evaluate(f"window.scrollTo(0, {y})")
        page.wait_for_timeout(90)
    page.evaluate("window.scrollTo(0, 0)")
    page.wait_for_timeout(1300)


def run_viewport(browser, width, height, mobile):
    context = browser.new_context(
        viewport={"width": width, "height": height},
        device_scale_factor=1,
        is_mobile=mobile,
        has_touch=mobile,
    )
    page = context.new_page()
    sink = {"console": [], "pageerrors": [], "failed": []}
    attach_errors(page, sink)
    block_media(page)
    page.goto(URL, wait_until="networkidle")
    page.evaluate("document.fonts.ready")
    page.wait_for_timeout(250)

    # 8 / page-load trail state before any scrolling.
    load_trail = page.evaluate(TRAIL_STATE)

    # 7 / audio pill initial + toggled states.
    pill_initial = page.evaluate("""() => {
      const p = document.querySelector('#audioPill');
      const b = document.querySelector('.audio-pill__wave i');
      return {
        exists: !!p, pressed: p?.getAttribute('aria-pressed'),
        cls: p?.className, animation: b ? getComputedStyle(b).animationName : null,
        playState: b ? getComputedStyle(b).animationPlayState : null,
        audioElements: document.querySelectorAll('audio').length,
        speechCalls: window.__qaSpeech || 0, audioCtor: window.__qaAudioCtor || 0
      };
    }""")
    page.click("#audioPill")
    page.wait_for_timeout(220)
    pill_playing = page.evaluate("""() => {
      const p = document.querySelector('#audioPill');
      const b = document.querySelector('.audio-pill__wave i');
      return {
        pressed: p?.getAttribute('aria-pressed'), cls: p?.className,
        animation: b ? getComputedStyle(b).animationName : null,
        playState: b ? getComputedStyle(b).animationPlayState : null,
        iconPauseDisplay: getComputedStyle(document.querySelector('.audio-pill__icon--pause')).display,
        iconPlayDisplay: getComputedStyle(document.querySelector('.audio-pill__icon--play')).display,
        speechCalls: window.__qaSpeech || 0, audioCtor: window.__qaAudioCtor || 0
      };
    }""")
    page.click("#audioPill")
    page.wait_for_timeout(120)
    pill_off = page.evaluate("""() => {
      const p = document.querySelector('#audioPill');
      const b = document.querySelector('.audio-pill__wave i');
      return { pressed: p?.getAttribute('aria-pressed'), cls: p?.className,
               animation: b ? getComputedStyle(b).animationName : null };
    }""")

    # 10 / keyboard run on a fresh page, so sequential focus starts at the top.
    focus_page = context.new_page()
    focus_sink = {"console": [], "pageerrors": [], "failed": []}
    attach_errors(focus_page, focus_sink)
    block_media(focus_page)
    focus_page.goto(URL, wait_until="networkidle")
    focus_page.evaluate("document.fonts.ready")
    focus_page.wait_for_timeout(250)
    focus_trail = []
    for _ in range(18):
        focus_page.keyboard.press("Tab")
        focus_page.wait_for_timeout(40)
        focus_trail.append(focus_page.evaluate("""() => {
          const el = document.activeElement;
          if (!el || el === document.body) return { tag: 'BODY' };
          const cs = getComputedStyle(el);
          return {
            tag: el.tagName,
            cls: typeof el.className === 'string' ? el.className : '',
            text: (el.innerText || el.getAttribute('aria-label') || '').replace(/\\s+/g, ' ').trim().slice(0, 60),
            href: el.getAttribute('href'),
            focusVisible: el.matches(':focus-visible'),
            outlineStyle: cs.outlineStyle, outlineWidth: cs.outlineWidth, outlineColor: cs.outlineColor,
            rect: (() => { const r = el.getBoundingClientRect(); return { x: +r.x.toFixed(1), y: +r.y.toFixed(1), w: +r.width.toFixed(1), h: +r.height.toFixed(1) }; })()
          };
        }"""))
        if focus_trail[-1].get("cls", "").find("audio-pill") >= 0:
            break
    focus_page.close()

    # Mobile menu Escape test.
    menu_test = None
    if width <= 640:
        menu_test = {"opened": None, "after_escape": None}
        page.click("#menuToggle")
        page.wait_for_timeout(200)
        menu_test["opened"] = page.evaluate("""() => {
          const m = document.querySelector('#mobileMenu');
          const t = document.querySelector('#menuToggle');
          return { open: m.classList.contains('is-open'), ariaHidden: m.getAttribute('aria-hidden'),
                   expanded: t.getAttribute('aria-expanded'), bodyOverflow: document.body.style.overflow,
                   focus: document.activeElement?.innerText?.replace(/\\s+/g,' ').trim().slice(0,40) };
        }""")
        page.keyboard.press("Escape")
        page.wait_for_timeout(250)
        menu_test["after_escape"] = page.evaluate("""() => {
          const m = document.querySelector('#mobileMenu');
          const t = document.querySelector('#menuToggle');
          return { open: m.classList.contains('is-open'), ariaHidden: m.getAttribute('aria-hidden'),
                   expanded: t.getAttribute('aria-expanded'), bodyOverflow: document.body.style.overflow };
        }""")

    # 4 / human-like scroll to trigger every reveal.
    human_scroll(page)
    general = page.evaluate(GENERAL)
    masks = page.evaluate(MASKS)

    # 8 / trail center and exit states on the first stage.
    page.evaluate(TRAIL_SCROLL, "center")
    page.wait_for_timeout(450)
    sat_cover = page.evaluate(SAT_COVER)
    page.wait_for_timeout(120)
    trail_center = page.evaluate(TRAIL_STATE)
    page.evaluate(TRAIL_SCROLL, "exit")
    page.wait_for_timeout(450)
    trail_exit = page.evaluate(TRAIL_STATE)

    page.evaluate("window.scrollTo(0,0)")
    page.wait_for_timeout(500)
    page.screenshot(path=str(OUT / f"qa-{width}.png"), full_page=True)

    # 9 / reduced motion in a fresh context.
    rm_context = browser.new_context(
        viewport={"width": width, "height": height},
        device_scale_factor=1,
        is_mobile=mobile,
        has_touch=mobile,
        reduced_motion="reduce",
    )
    rm_page = rm_context.new_page()
    rm_sink = {"console": [], "pageerrors": [], "failed": []}
    attach_errors(rm_page, rm_sink)
    block_media(rm_page)
    rm_page.goto(URL, wait_until="networkidle")
    rm_page.evaluate("document.fonts.ready")
    rm_page.wait_for_timeout(500)
    reduced = rm_page.evaluate(REDUCED)
    reduced["errors"] = rm_sink
    rm_context.close()

    result = {
        "width": width, "height": height, "mobile": mobile,
        "errors": sink,
        "loadTrail": load_trail,
        "pillInitial": pill_initial, "pillPlaying": pill_playing, "pillOff": pill_off,
        "focusTrail": focus_trail,
        "focusErrors": focus_sink,
        "menuTest": menu_test,
        "general": general,
        "masks": masks,
        "satCover": sat_cover,
        "trailCenter": trail_center,
        "trailExit": trail_exit,
        "reduced": reduced,
    }
    context.close()
    return result


def main():
    results = []
    only = [int(x) for x in sys.argv[1:]] or None
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge", headless=True)
        for width, height, mobile in VIEWPORTS:
            if only and width not in only:
                continue
            print(f"\n===== {width}x{height} =====", flush=True)
            r = run_viewport(browser, width, height, mobile)
            results.append(r)
            g = r["general"]
            print(json.dumps({
                "errors": {k: len(v) for k, v in r["errors"].items()},
                "hscroll": g["horizontalOverflowPx"],
                "phones": g["phoneCount"],
                "phoneTypes": [x["type"] for x in g["phones"]],
                "reveal": [(x["visible"], x["clipPath"]) for x in g["revealImgs"]],
                "broken": len(g["brokenImages"]),
                "upscaled": g["upscaledImages"],
                "pill": [r["pillInitial"]["pressed"], r["pillPlaying"]["pressed"], r["pillOff"]["pressed"]],
                "pillAnim": [r["pillInitial"]["animation"], r["pillPlaying"]["animation"]],
                "stages": g["stages"],
                "loadTrail": r["loadTrail"],
                "center": {k: r["trailCenter"][k] for k in ("progressFromOffset", "cardsHit", "nodesHit")} if r["trailCenter"] else None,
                "exit": {k: r["trailExit"][k] for k in ("progressFromOffset", "cardsHit", "nodesHit")} if r["trailExit"] else None,
                "menu": r["menuTest"],
                "rm": {
                    "reveal": [(x["visible"], x["clipPath"]) for x in r["reduced"]["revealImgs"]],
                    "stages": r["reduced"]["stages"],
                    "wave": r["reduced"]["waveAnimation"][0] if r["reduced"]["waveAnimation"] else None,
                    "errors": {k: len(v) for k, v in r["reduced"]["errors"].items()},
                },
                "height": g["doc"]["scrollHeight"],
            }, indent=1))
            json_path = OUT / f"qa-report-{width}.json"
            json_path.write_text(json.dumps(r, indent=2), encoding="utf-8")
            print(f"wrote {json_path}", flush=True)
        browser.close()
    if len(results) > 1:
        (OUT / "qa-report-all.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
        print(f"\nwrote {OUT / 'qa-report-all.json'}")


if __name__ == "__main__":
    main()
