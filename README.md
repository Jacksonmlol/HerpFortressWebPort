# Herp Fortress

Herp Fortress is a small real-time strategy prototype that **Notch** (Markus Persson)
released in 2012 as plain Java / AWT source. This repository contains two things:

* the **original desktop source** in `src/`, and
* a **WebAssembly port** that runs that same game in a web browser.

Both share the same game code. The browser build is produced by compiling `src/`
with [TeaVM](https://teavm.org/) and adding a small platform layer, so the game
logic itself is the original code.

## Links
* [Released via twitter by Notch (Markus Persson)](https://twitter.com/notch/status/208469324485242881)
* [Direct link to original source hosted at 0x10c](http://0x10c.com/tmp/HerpFortress.zip)
* License: ["do whatever you want, but don't blame us."](https://twitter.com/notch/status/208469381003489280)

Please support Mojang and buy their games so they continue to make cool things like this!
* [Mojang Website](http://www.mojang.com/)

---

# Play it

You need three things: a **JDK**, **Maven** (to build once) and **Node.js**
(to serve the files). No prior experience with the code is needed.

## 1. Build the game

From the repository root:

```
mvn -B -DskipTests package
```

This creates the playable site in the `target/site` folder. You only have to do
this once, or again after you change the code.

## 2. Start the server

A WebAssembly game cannot be loaded from a `file://` page, so the built site has
to be served over HTTP. A tiny server is included:

```
node tools/serve.js
```

It prints the address it is serving on. Defaults: **port 8137** serving
`target/site`. You can pick a different port or folder if you want:

```
node tools/serve.js 8080 target/site
```

Leave this terminal window open while you play.

## 3. Open the game

Go to <http://localhost:8137/> in a modern browser (Chrome, Edge or Firefox).
The status line under the game should read
*"Left-click to select a unit, right-click to give an order."*

## Stopping the server

The server runs in the foreground of the terminal that started it, so the normal
way to stop it is:

* **Press `Ctrl`+`C`** in that terminal, or
* simply **close that terminal window**.

If you started it in the background and no longer have that terminal, stop it by
the port it is holding (8137 by default):

Windows (PowerShell):

```powershell
Get-NetTCPConnection -LocalPort 8137 |
  Select-Object -ExpandProperty OwningProcess |
  ForEach-Object { Stop-Process -Id $_ -Force }
```

macOS / Linux:

```
lsof -ti tcp:8137 | xargs kill
```

After rebuilding you don't need to restart the server &mdash; just **refresh the
page**. It is served with caching disabled, so the browser always fetches the
latest files.

## Controls

* **Left-click** a unit (or its portrait at the bottom) to select it. Click it
  again to centre the camera on it.
* **Right-click** the map to order the selected unit to move there.
* **Middle-mouse drag**, the **arrow keys** / **WASD**, or the **mouse wheel**
  pan the camera.
* **1**&#8211;**9** (or the numpad cluster, or `R T Y F G H V B N`) select a unit
  class; **Q** / **E** (or **Tab**) cycle through your units.

> If the keyboard seems dead, click on the game first so the page has focus.

## How to fight

There is **no fire button**: units shoot on their own. A unit automatically aims
and fires at the closest enemy within its weapon range (about 150 world units,
roughly 9 tiles) whenever it has ammo.

Red and blue teams start on opposite sides of the map, so at the beginning
nothing is in range. To make a unit attack, **select it and right-click close to
an enemy** &mdash; it walks over and opens fire by itself once the target is in
range.

---

# For developers

## What this project is

The desktop game and the web game are the **same game logic**. Only the parts
that talk to the operating system differ, so the repository is organised as:

| Path | What it is |
| --- | --- |
| `src/` | The original game code (Notch's source). Shared by both builds, essentially unmodified. |
| `platform/java/` | The **browser** replacements for the few desktop-only classes, plus the `Web` bridge. |
| `site/` | The web page: HTML, CSS and the JavaScript host (`host.js`). |
| `pom.xml` | The Maven build that compiles `src/` + `platform/java/` to WebAssembly with TeaVM. |
| `tools/serve.js` | Dependency-free static web server used to run the built site locally. |
| `res/` | Original game artwork (PNG sprites, tiles, level map). |

## How the port works

[TeaVM](https://teavm.org/) compiles Java bytecode straight to a real
WebAssembly module (`target/site/teavm/classes.wasm`, ~155&nbsp;KB). It is not a
re-implementation and not a JVM running in the browser: the compiled game code is
the original Java.

Only three original files touch the desktop window, and those are the only ones
replaced for the web build (they are excluded from the web compile and re-provided
in `platform/java/`):

| Original (desktop) | Web replacement |
| --- | --- |
| `HerpFortress.java` &mdash; AWT `Canvas`, `BufferStrategy`, game thread | A fixed-timestep loop driven by `requestAnimationFrame`, blitting to a canvas |
| `Art.java` &mdash; loads images with `ImageIO` | Images are decoded by the browser and pulled in through `Web` |
| `InputHandler.java` &mdash; AWT mouse/key listeners | Polls mouse/keyboard state from `host.js` via `Web` |

Everything else &mdash; level, units, weapons, pathfinding, particles, physics and
all the rendering into the ARGB `Bitmap` &mdash; is the untouched `src/` code.

Changes made inside `src/` are mechanical only (so the desktop build still works):

* `Input.java` uses the new platform-neutral `Keys.VK_*` constants instead of
  `java.awt.event.KeyEvent`.
* `Bitmap.java` dropped its unused `BufferedImage` constructor.
* The desktop `HerpFortress.java` now builds its `Bitmap` from the
  `BufferedImage`'s `DataBufferInt`.

The JavaScript host (`site/host.js`) is responsible for:

* decoding the PNGs with a canvas and handing raw ARGB pixels to Java,
* tracking mouse and keyboard state,
* blitting the finished frame onto the `<canvas>`,
* driving the game loop with `requestAnimationFrame`.

> **Note:** the mouse-wheel camera panning is an addition to the original game
> (which only panned with the arrow keys and the middle mouse button). Everything
> else matches the original behaviour.

## Building

Requirements:

* **JDK 17 or newer** (tested with JDK 25)
* **Apache Maven 3.9 or newer**
* **Node.js 18 or newer** &mdash; only needed to run the local server

Commands:

```
mvn -B -DskipTests package     # build the browser game            -> target/site
mvn -B clean package           # build from scratch (recommended after changing sources)
```

The deployable site ends up in `target/site`:

* `index.html`, `style.css`, `host.js`
* `teavm/classes.wasm` and its JS runtime
* `res/**` &mdash; the artwork

`target/` is build output and is already ignored by git.

## Serving, starting and stopping

The web build must be served over HTTP. Start it with:

```
node tools/serve.js            # or: node tools/serve.js <port> <dir>
```

Stop it with **`Ctrl`+`C`** in that terminal (see *Stopping the server* above for
how to kill it by port if the terminal is gone). Change the port if 8137 is taken.

## Running the original desktop version

The original build is an Eclipse project (`.project`), but it is an ordinary Java
app: `com.mojang.herpfortress.HerpFortress` has a `main` method that opens a
`JFrame`. Make sure `res/` is on the classpath root (Eclipse marks it as a source
folder), otherwise the artwork will not load.

## Troubleshooting

* **"Port already in use"** when starting the server &mdash; another server is
  still running on that port. Stop it (see above) or use a different port, e.g.
  `node tools/serve.js 8080 target/site`.
* **Blank page, or the status never changes** &mdash; make sure you opened the
  page over `http://localhost:...`, not as a `file://` path.
* **Nothing loads at all** &mdash; the browser must support WebAssembly GC. Use a
  recent Chrome, Edge or Firefox.
* **Arrow keys do nothing** &mdash; click on the game so the page has keyboard
  focus.
* **A unit won't shoot** &mdash; it only fires when an enemy is within about 9
  tiles. Move it closer with a right-click.
