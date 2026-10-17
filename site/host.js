/*
 * JavaScript host for the WebAssembly build of Herp Fortress.
 *
 * The Java side (com.mojang.herpfortress.Web) talks to the global `HerpHost`
 * object defined here. Responsibilities:
 *   - decode the PNG artwork with the browser and hand the raw ARGB pixels to Java
 *   - track mouse/keyboard state and expose it to Java
 *   - blit the finished ARGB frame buffer onto the canvas
 *   - drive the fixed timestep game loop with requestAnimationFrame
 */
(function () {
  "use strict";

  var WIDTH = 320;
  var HEIGHT = 240;
  var SCALE = 3;
  var GAME_W = WIDTH * SCALE;
  var GAME_H = HEIGHT * SCALE;

  // Classpath names used by com.mojang.herpfortress.Art.
  var ASSETS = [
    "/chars/red.png",
    "/chars/blu.png",
    "/cursors.png",
    "/panel.png",
    "/particles.png",
    "/pickups.png",
    "/missiles.png",
    "/tiles.png",
    "/levels/level.png"
  ];

  // Keys the game binds (values match AWT KeyEvent / legacy KeyboardEvent.keyCode).
  var GAME_KEYS = new Set([
    9, 37, 38, 39, 40,
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77,
    78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
    96, 97, 98, 99, 100, 101, 102, 103, 104, 105
  ]);

  var state = {
    x: 0,
    y: 0,
    onScreen: false,
    buttons: [false, false, false], // [0] left, [1] right, [2] middle (AWT order)
    down: new Set(),
    wheelX: 0,
    wheelY: 0
  };

  var canvas = null;
  var ctx = null;
  var imageData = null;
  var statusEl = null;
  var fpsEl = null;

  // Decoded artwork, keyed by the classpath names in ASSETS.
  var images = new Map();

  function setStatus(text, isError) {
    if (!statusEl) return;
    statusEl.textContent = text;
    statusEl.className = isError ? "error" : "";
  }

  function toGameSpace(clientX, clientY) {
    var rect = canvas.getBoundingClientRect();
    var sx = rect.width > 0 ? GAME_W / rect.width : 1;
    var sy = rect.height > 0 ? GAME_H / rect.height : 1;
    return [(clientX - rect.left) * sx, (clientY - rect.top) * sy];
  }

  function updateMouse(event) {
    var p = toGameSpace(event.clientX, event.clientY);
    state.x = p[0];
    state.y = p[1];
    state.onScreen = p[0] >= 0 && p[1] >= 0 && p[0] < GAME_W && p[1] < GAME_H;
  }

  var NAMED_KEYS = {
    Tab: 9,
    ArrowLeft: 37,
    ArrowUp: 38,
    ArrowRight: 39,
    ArrowDown: 40
  };

  // `KeyboardEvent.keyCode` is deprecated but is still the only thing that
  // matches the AWT codes the game was written against, so prefer it and fall
  // back to `key`/`code` if a browser ever drops it.
  function keyCodeOf(event) {
    if (event.keyCode) return event.keyCode;

    var key = event.key;
    if (key) {
      if (key.length === 1) {
        var upper = key.toUpperCase();
        if (upper >= "A" && upper <= "Z") return upper.charCodeAt(0);
        if (key >= "0" && key <= "9") return key.charCodeAt(0);
      }
      if (NAMED_KEYS[key] !== undefined) return NAMED_KEYS[key];
    }

    var numpad = /^Numpad([0-9])$/.exec(event.code || "");
    if (numpad) return 96 + Number(numpad[1]);
    return 0;
  }

  function buttonIndex(domButton) {
    if (domButton === 0) return 0; // left
    if (domButton === 2) return 1; // right
    if (domButton === 1) return 2; // middle
    return -1;
  }

  function attachInput() {
    canvas.addEventListener("mousemove", updateMouse, { passive: true });
    canvas.addEventListener("mouseenter", updateMouse, { passive: true });
    canvas.addEventListener("mouseleave", function () {
      state.onScreen = false;
    });
    canvas.addEventListener("mousedown", function (event) {
      updateMouse(event);
      var i = buttonIndex(event.button);
      if (i >= 0) state.buttons[i] = true;
      event.preventDefault();
    });
    window.addEventListener("mouseup", function (event) {
      var i = buttonIndex(event.button);
      if (i >= 0) state.buttons[i] = false;
    });
    canvas.addEventListener("contextmenu", function (event) {
      event.preventDefault();
    });

    // The wheel (and trackpad two-finger scrolling, which arrives as wheel
    // events) pans the camera. Deltas are normalised to pixels and consumed by
    // the game once per frame.
    canvas.addEventListener("wheel", function (event) {
      var factor = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? 100 : 1;
      state.wheelX += event.deltaX * factor;
      state.wheelY += event.deltaY * factor;
      event.preventDefault();
    }, { passive: false });

    window.addEventListener("keydown", function (event) {
      var code = keyCodeOf(event);
      if (!GAME_KEYS.has(code)) return;
      state.down.add(code);
      event.preventDefault();
    });
    window.addEventListener("keyup", function (event) {
      var code = keyCodeOf(event);
      if (!GAME_KEYS.has(code)) return;
      state.down.delete(code);
      event.preventDefault();
    });
    window.addEventListener("blur", function () {
      state.down.clear();
      state.buttons[0] = state.buttons[1] = state.buttons[2] = false;
    });
  }

  // Called from Java, once per frame. The game only ever inspects the handful of
  // keys it has bound, so shipping the currently pressed set is cheap.
  function downKeys() {
    var keys = new Int32Array(state.down.size);
    var i = 0;
    state.down.forEach(function (code) {
      keys[i++] = code;
    });
    return keys;
  }

  // Called from Java, once per frame, with the freshly rendered ARGB buffer.
  function present(pixels, w, h) {
    var data = imageData.data;
    var count = w * h;
    for (var i = 0, j = 0; i < count; i++, j += 4) {
      var argb = pixels[i];
      data[j] = (argb >> 16) & 0xff;
      data[j + 1] = (argb >> 8) & 0xff;
      data[j + 2] = argb & 0xff;
      data[j + 3] = (argb >>> 24) & 0xff;
    }
    ctx.putImageData(imageData, 0, 0);
  }

  function artwork(name) {
    return images.get(name);
  }

  window.HerpHost = {
    imageWidth: function (name) {
      var image = artwork(name);
      return image ? image.w : 0;
    },
    imageHeight: function (name) {
      var image = artwork(name);
      return image ? image.h : 0;
    },
    imagePixels: function (name) {
      var image = artwork(name);
      return image ? image.pixels : new Int32Array(0);
    },
    mouseX: function () { return state.x; },
    mouseY: function () { return state.y; },
    mouseButton: function (button) { return !!state.buttons[button]; },
    mouseOnScreen: function () { return state.onScreen; },
    downKeys: downKeys,
    takeWheelX: function () {
      var value = state.wheelX;
      state.wheelX = 0;
      return value;
    },
    takeWheelY: function () {
      var value = state.wheelY;
      state.wheelY = 0;
      return value;
    },
    present: present
  };

  function decodeImage(url) {
    return new Promise(function (resolve, reject) {
      var image = new Image();
      image.onload = function () {
        var scratch = document.createElement("canvas");
        scratch.width = image.naturalWidth;
        scratch.height = image.naturalHeight;
        var scratchCtx = scratch.getContext("2d", { willReadFrequently: true });
        scratchCtx.drawImage(image, 0, 0);
        var rgba = scratchCtx.getImageData(0, 0, scratch.width, scratch.height).data;

        // RGBA bytes -> ARGB ints, which is what Bitmap works with.
        var argb = new Int32Array(scratch.width * scratch.height);
        for (var i = 0, j = 0; i < argb.length; i++, j += 4) {
          argb[i] = (rgba[j + 3] << 24) | (rgba[j] << 16) | (rgba[j + 1] << 8) | rgba[j + 2];
        }
        resolve({ w: scratch.width, h: scratch.height, pixels: argb });
      };
      image.onerror = function () {
        reject(new Error("Could not load " + url));
      };
      image.src = url;
    });
  }

  function loadAssets() {
    return Promise.all(ASSETS.map(function (name) {
      return decodeImage("res" + name);
    }));
  }

  function run(decoded) {
    return TeaVM.wasmGC.load("teavm/classes.wasm").then(function (teavm) {
      for (var i = 0; i < ASSETS.length; i++) {
        images.set(ASSETS[i], decoded[i]);
      }
      teavm.exports.main([]);

      setStatus("Left-click to select a unit, right-click to give an order.");

      var frames = 0;
      var lastFps = performance.now();
      function loop(now) {
        teavm.exports.frame();
        frames++;
        if (now - lastFps >= 1000) {
          fpsEl.textContent = Math.round((frames * 1000) / (now - lastFps)) + " fps";
          frames = 0;
          lastFps = now;
        }
        requestAnimationFrame(loop);
      }
      requestAnimationFrame(loop);
    });
  }

  function start() {
    canvas = document.getElementById("screen");
    statusEl = document.getElementById("status");
    fpsEl = document.getElementById("fps");

    canvas.width = WIDTH;
    canvas.height = HEIGHT;
    ctx = canvas.getContext("2d", { alpha: false });
    ctx.imageSmoothingEnabled = false;
    imageData = ctx.createImageData(WIDTH, HEIGHT);
    attachInput();

    setStatus("Decoding artwork\u2026");
    return loadAssets().then(function (decoded) {
      setStatus("Loading WebAssembly\u2026");
      return run(decoded);
    });
  }

  function showFatal(error) {
    console.error(error);
    setStatus("Failed to start: " + (error && error.message ? error.message : error), true);
  }

  window.addEventListener("error", function (event) {
    if (event.message) setStatus("Error: " + event.message, true);
  });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      start().catch(showFatal);
    });
  } else {
    start().catch(showFatal);
  }
})();
