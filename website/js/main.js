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
})();
