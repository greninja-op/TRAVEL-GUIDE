/* Travel Guide landing page — navigation, reveals, small state. */
(function () {
  "use strict";

  var nav = document.getElementById("nav");
  var toggle = document.getElementById("menuToggle");
  var menu = document.getElementById("mobileMenu");
  var year = document.getElementById("year");
  var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

  if (year) {
    year.textContent = String(new Date().getFullYear());
  }

  /* Sticky nav condenses after the first screenful. */
  function onScroll() {
    if (!nav) return;
    nav.classList.toggle("is-scrolled", window.scrollY > 24);
  }
  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });

  /* Mobile menu — one open/close path, real aria state, Escape closes. */
  function setMenu(open) {
    if (!menu || !toggle) return;
    menu.classList.toggle("is-open", open);
    menu.setAttribute("aria-hidden", open ? "false" : "true");
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
    toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    document.body.style.overflow = open ? "hidden" : "";
    var icon = toggle.querySelector("use");
    if (icon) {
      icon.setAttribute("href", open ? "assets/icons.svg#i-x" : "assets/icons.svg#i-menu");
    }
    if (open) {
      var first = menu.querySelector("a");
      if (first) first.focus();
    }
  }

  if (toggle) {
    toggle.addEventListener("click", function () {
      setMenu(toggle.getAttribute("aria-expanded") !== "true");
    });
  }

  if (menu) {
    menu.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        setMenu(false);
      });
    });
  }

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") setMenu(false);
  });

  /* Hero audio pill — visual state only. No audio element, no speech. */
  var audioPill = document.getElementById("audioPill");

  if (audioPill) {
    audioPill.addEventListener("click", function () {
      var playing = !audioPill.classList.contains("is-playing");
      audioPill.classList.toggle("is-playing", playing);
      audioPill.setAttribute("aria-pressed", playing ? "true" : "false");
    });
  }

  /* Scroll reveals: one gesture, once, capped groups. */
  var revealTargets = document.querySelectorAll(".reveal, .reveal-img, .trail-draw");

  function revealAll() {
    revealTargets.forEach(function (element) {
      element.classList.add("is-visible");
    });
  }

  if (reduceMotion.matches || !("IntersectionObserver" in window)) {
    revealAll();
  } else {
    /* .reveal-img is fully clipped before it reveals, so it has no visible
       area for the observer to intersect. Watch its laid-out parent instead. */
    var observedTargets = new Map();
    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          var targets = observedTargets.get(entry.target) || [entry.target];
          targets.forEach(function (element) {
            element.classList.add("is-visible");
          });
          observer.unobserve(entry.target);
        });
      },
      { threshold: 0.16, rootMargin: "0px 0px -8% 0px" }
    );
    revealTargets.forEach(function (element) {
      var host = element.classList.contains("reveal-img") && element.parentElement
        ? element.parentElement
        : element;
      var targets = observedTargets.get(host);
      if (targets) {
        targets.push(element);
      } else {
        observedTargets.set(host, [element]);
      }
      observer.observe(host);
    });
  }

  /* Scroll-driven GPS simulation on the three stage trails.
     Nodes and the progress path are generated at load from the real path,
     so no fragile coordinates live in the HTML. */
  var SVG_NS = "http://www.w3.org/2000/svg";
  var TRAIL_FRACTIONS = [0.22, 0.55, 0.82];
  var trails = [];

  function setTrailProgress(trail, progress) {
    if (trail.progressValue === progress) return;
    trail.progressValue = progress;

    var offset = trail.total * (1 - progress);
    trail.progress.style.strokeDasharray = trail.total + " " + trail.total;
    trail.progress.style.strokeDashoffset = offset;

    var point = trail.primary.getPointAtLength(trail.total * progress);
    trail.dot.setAttribute("cx", point.x.toFixed(1));
    trail.dot.setAttribute("cy", point.y.toFixed(1));

    TRAIL_FRACTIONS.forEach(function (fraction, index) {
      var hit = progress >= fraction - 0.001;
      trail.nodes[index].classList.toggle("is-hit", hit);
      if (trail.cards[index]) {
        trail.cards[index].classList.toggle("is-hit", hit);
      }
    });
  }

  function initTrail(stage) {
    var svg = stage.querySelector(".stage__trail");
    var primary = svg ? svg.querySelector("path:not(.dash)") : null;
    if (!svg || !primary) return;

    var d = primary.getAttribute("d");
    var total = primary.getTotalLength();
    if (!d || !total) return;

    var progress = document.createElementNS(SVG_NS, "path");
    progress.setAttribute("class", "trail-progress");
    progress.setAttribute("d", d);
    progress.setAttribute("aria-hidden", "true");
    primary.parentNode.insertBefore(progress, primary.nextSibling);

    /* Pins live in a second, aligned SVG above the phone so the GPS dot
       stays visible; the route itself remains behind the device. */
    var overlay = document.createElementNS(SVG_NS, "svg");
    overlay.setAttribute("class", "stage__trail stage__trail--overlay");
    overlay.setAttribute("viewBox", svg.getAttribute("viewBox"));
    overlay.setAttribute("aria-hidden", "true");
    svg.parentNode.insertBefore(overlay, svg.nextSibling);

    var dot = document.createElementNS(SVG_NS, "circle");
    dot.setAttribute("class", "trail-dot");
    dot.setAttribute("r", "7");
    dot.setAttribute("aria-hidden", "true");
    overlay.appendChild(dot);

    var nodes = TRAIL_FRACTIONS.map(function (fraction) {
      var point = primary.getPointAtLength(total * fraction);
      var node = document.createElementNS(SVG_NS, "circle");
      node.setAttribute("class", "trail-node");
      node.setAttribute("cx", point.x.toFixed(1));
      node.setAttribute("cy", point.y.toFixed(1));
      node.setAttribute("r", "6");
      node.setAttribute("aria-hidden", "true");
      overlay.appendChild(node);
      return node;
    });

    stage.classList.add("is-trail-live");

    trails.push({
      stage: stage,
      primary: primary,
      progress: progress,
      dot: dot,
      total: total,
      nodes: nodes,
      cards: Array.prototype.slice.call(stage.querySelectorAll(".float-card")),
      progressValue: -1
    });
  }

  function updateTrail(trail) {
    var rect = trail.stage.getBoundingClientRect();
    var vh = window.innerHeight || document.documentElement.clientHeight;
    var start = vh * 0.8;
    var end = -rect.height + vh * 0.2;
    var progress = (start - rect.top) / (start - end);
    setTrailProgress(trail, Math.max(0, Math.min(1, progress)));
  }

  function updateAllTrails() {
    trails.forEach(updateTrail);
  }

  var trailFrame = 0;

  function onTrailScroll() {
    if (trailFrame) return;
    trailFrame = window.requestAnimationFrame(function () {
      trailFrame = 0;
      updateAllTrails();
    });
  }

  Array.prototype.forEach.call(document.querySelectorAll(".stage"), initTrail);

  if (reduceMotion.matches) {
    trails.forEach(function (trail) {
      setTrailProgress(trail, 1);
    });
  } else {
    updateAllTrails();
    window.addEventListener("scroll", onTrailScroll, { passive: true });
    window.addEventListener("resize", updateAllTrails, { passive: true });
  }

  reduceMotion.addEventListener("change", function (event) {
    if (event.matches) {
      revealAll();
      trails.forEach(function (trail) {
        setTrailProgress(trail, 1);
      });
      window.removeEventListener("scroll", onTrailScroll);
      window.removeEventListener("resize", updateAllTrails);
    } else {
      window.addEventListener("scroll", onTrailScroll, { passive: true });
      window.addEventListener("resize", updateAllTrails, { passive: true });
      updateAllTrails();
    }
  });

  /* ---------------------------------------------------------------------
     Custom overlay scroll thumb
     No track, no native bar: a coral capsule that appears while scrolling or
     when the pointer nears the right edge, then fades away. Movement is eased
     with a lerp, and the thumb can be dragged.
     --------------------------------------------------------------------- */
  (function initScrollThumb() {
    var finePointer = window.matchMedia("(pointer: fine)").matches;
    if (!finePointer || window.innerWidth < 900) return;

    var doc = document.documentElement;
    var thumb = document.createElement("div");
    var visible = false;
    var dragging = false;
    var hovering = false;
    var nearPointer = false;
    var hideTimer = 0;
    var raf = 0;
    var targetY = 0;
    var targetH = 0;
    var currentY = 0;
    var currentH = 0;
    var PAD = 10;
    var EDGE = 88;
    var EASE = 0.22;
    var navGuard = 8;

    thumb.className = "scroll-thumb";
    thumb.setAttribute("aria-hidden", "true");
    document.body.appendChild(thumb);
    doc.classList.add("has-custom-scrollbar");

    function refreshNavGuard() {
      var cssNav = parseFloat(window.getComputedStyle(doc).getPropertyValue("--nav-h")) || 0;
      var currentNav = nav ? nav.getBoundingClientRect().height : 0;
      navGuard = Math.max(cssNav, currentNav, 0) + 8;
    }

    refreshNavGuard();

    function topInset() {
      return navGuard;
    }

    function metrics() {
      var viewport = window.innerHeight;
      var total = Math.max(doc.scrollHeight, document.body.scrollHeight);
      var maxScroll = Math.max(total - viewport, 1);
      var top = topInset();
      var track = Math.max(viewport - top - PAD, 1);
      var ratio = viewport / total;
      var height = Math.max(42, Math.min(track, track * ratio));
      var y = top + (window.scrollY / maxScroll) * (track - height);
      return { viewport: viewport, total: total, maxScroll: maxScroll, track: track, height: height, y: y, top: top };
    }

    function setTarget() {
      var m = metrics();
      if (m.total <= m.viewport + 1) {
        targetH = 0;
        targetY = m.top;
        return;
      }
      targetH = m.height;
      targetY = m.y;
    }

    function render() {
      raf = 0;
      if (dragging || reduceMotion.matches) {
        currentY = targetY;
        currentH = targetH;
      } else {
        currentY += (targetY - currentY) * EASE;
        currentH += (targetH - currentH) * EASE;
        if (Math.abs(targetY - currentY) < 0.1 && Math.abs(targetH - currentH) < 0.1) {
          currentY = targetY;
          currentH = targetH;
        }
      }
      thumb.style.height = currentH.toFixed(1) + "px";
      thumb.style.transform = "translate3d(0," + currentY.toFixed(1) + "px,0)";
      if (!reduceMotion.matches && (Math.abs(targetY - currentY) > 0.1 || Math.abs(targetH - currentH) > 0.1)) {
        raf = window.requestAnimationFrame(render);
      }
    }

    function kick() {
      if (!raf) raf = window.requestAnimationFrame(render);
    }

    function show() {
      if (visible) return;
      var m = metrics();
      if (m.total <= m.viewport + 1) return;
      visible = true;
      thumb.classList.add("is-visible");
      setTarget();
      kick();
    }

    function hide() {
      if (!visible || dragging || hovering || nearPointer) return;
      visible = false;
      thumb.classList.remove("is-visible");
    }

    function scheduleHide(delay) {
      window.clearTimeout(hideTimer);
      if (dragging || hovering || nearPointer) return;
      hideTimer = window.setTimeout(hide, delay || 900);
    }

    function onScroll() {
      show();
      setTarget();
      kick();
      scheduleHide(900);
    }

    function onPointerMove(event) {
      nearPointer = window.innerWidth - event.clientX <= EDGE;
      if (nearPointer) {
        show();
        window.clearTimeout(hideTimer);
      } else if (!dragging && !hovering) {
        scheduleHide(180);
      }
    }

    function onThumbDown(event) {
      event.preventDefault();
      dragging = true;
      hovering = false;
      visible = true;
      thumb.classList.add("is-dragging", "is-visible");
      thumb.setPointerCapture(event.pointerId);
      var previousScrollBehavior = doc.style.scrollBehavior;
      doc.style.scrollBehavior = "auto";
      var m = metrics();
      var startY = event.clientY;
      var startScroll = window.scrollY;
      var maxScroll = m.maxScroll;
      var travel = Math.max(m.track - m.height, 1);
      var ratio = maxScroll / travel;

      function move(ev) {
        var next = startScroll + (ev.clientY - startY) * ratio;
        window.scrollTo(0, Math.max(0, Math.min(maxScroll, next)));
        setTarget();
        kick();
      }

      function end() {
        dragging = false;
        doc.style.scrollBehavior = previousScrollBehavior;
        thumb.classList.remove("is-dragging");
        if (thumb.hasPointerCapture(event.pointerId)) {
          thumb.releasePointerCapture(event.pointerId);
        }
        thumb.removeEventListener("pointermove", move);
        thumb.removeEventListener("pointerup", end);
        thumb.removeEventListener("pointercancel", end);
        setTarget();
        kick();
        scheduleHide(600);
      }

      thumb.addEventListener("pointermove", move);
      thumb.addEventListener("pointerup", end);
      thumb.addEventListener("pointercancel", end);
    }

    thumb.addEventListener("pointerenter", function () {
      hovering = true;
      show();
      window.clearTimeout(hideTimer);
    });

    thumb.addEventListener("pointerleave", function () {
      hovering = false;
      if (!dragging) scheduleHide(180);
    });

    thumb.addEventListener("pointerdown", onThumbDown);

    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("wheel", onScroll, { passive: true });
    window.addEventListener("pointermove", onPointerMove, { passive: true });
    window.addEventListener("resize", function () {
      refreshNavGuard();
      setTarget();
      kick();
    }, { passive: true });

    if ("ResizeObserver" in window) {
      new ResizeObserver(function () {
        setTarget();
        kick();
      }).observe(document.body);
    }

    window.addEventListener("load", function () {
      setTarget();
      currentY = targetY;
      currentH = targetH;
      thumb.style.height = currentH.toFixed(1) + "px";
      thumb.style.transform = "translate3d(0," + currentY.toFixed(1) + "px,0)";
    });

    // Set a sane initial position without showing the thumb.
    setTarget();
    currentY = targetY;
    currentH = targetH;
    thumb.style.height = currentH.toFixed(1) + "px";
    thumb.style.transform = "translate3d(0," + currentY.toFixed(1) + "px,0)";
  })();
})();
