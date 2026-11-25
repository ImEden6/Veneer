# Veneer

Veneer is an atlas-time composition library for Minecraft 1.21.1 that supports Fabric and NeoForge. It adds a sprite source called `veneer:composite`, which stacks textures into a single sprite while the atlas gets stitched, using a normal atlas JSON. Since the result is just one texture, you don't need extra model quads, there's no z-fighting, and resource pack overrides of the layers carry over on their own.

It's all JSON, so you don't need to write any Java or add a Gradle dependency. The mod only has to be installed.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x, or Fabric Loader 0.16.5+ with Fabric API
- Client only

## A first sprite

I'll put an orange tinted iron ore on top of stone. This goes in `assets/minecraft/atlases/blocks.json`, in either a resource pack or a mod.

```json
{
  "sources": [
    {
      "type": "veneer:composite",
      "sprite": "mypack:block/tinted_ore",
      "layers": [
        { "texture": "minecraft:block/stone" },
        { "texture": "minecraft:block/iron_ore", "tint": "#FF8844" }
      ]
    }
  ]
}
```

Then point a model at `mypack:block/tinted_ore` like any other texture. Layers go from bottom to top.

## Sprite options

| field | default | what it does |
|---|---|---|
| `sprite` | required | id of the sprite that comes out |
| `layers` | required | list of layers, at least one |
| `metadata_source` | `0` | index of the layer whose other `.mcmeta` sections get carried over |
| `max_frames` | `64` | cap on unique animation frames |
| `interpolation` | `"auto"` | `"auto"` or `"ignore"` |
| `interpolation_step` | `1` | ticks between samples when an animation gets baked |
| `pbr` | `true` | build `_n` and `_s` textures for this sprite |
| `skip_if_exists` | `false` | do nothing if a texture file for `sprite` already exists |

`skip_if_exists` is for letting a pack ship a hand drawn texture instead. The file still has to be picked up by the atlas, which vanilla already does for anything in `textures/block/` and `textures/item/`.

## Layer options

| field | default | what it does |
|---|---|---|
| `texture` | required | texture id, same format as in models |
| `blend` | `"normal"` | `normal`, `multiply`, `add`, `screen`, `overlay`, `darken` or `lighten` |
| `tint` | none | `"#RRGGBB"`, `"RRGGBB"` or an integer |
| `opacity` | `1.0` | from `0.0` to `1.0` |
| `mask` | none | texture whose alpha cuts out this layer |
| `invert_mask` | `false` | flip the mask |
| `rotate` | `0` | `0`, `90`, `180` or `270`, clockwise |
| `flip_x`, `flip_y` | `false` | mirror the layer, applied before `rotate` |
| `optional` | `false` | skip the layer if its texture is missing |

`optional` is handy for compat overlays from a mod that might not be there. It only covers the texture though, a missing mask is still an error. Masks also stay put when you rotate or flip a layer.

Texture sizes have to be powers of two with matching aspect ratios, and smaller ones get scaled up to the largest with nearest neighbor. Quarter turns need square frames.

## Animations

Animated layers work as they are, with their usual `.mcmeta`, as long as the frames are in a vertical strip. Layers with different frame counts and timings get merged into one loop that covers all of them.

Interpolation goes one of two ways. If every animated layer has `interpolate` on and their frame timings line up, Veneer keeps the keyframes and lets vanilla do the blending, which costs no extra frames. If they don't line up, or some are smooth and some aren't, it bakes the in-between frames itself, sampling every `interpolation_step` ticks. `"interpolation": "ignore"` makes everything snap.

The catch is that loops which don't divide into each other get long fast. Vanilla prismarine alone is 6600 ticks. Past `max_frames` the animation gets cut short and you get a warning in the log, so either raise the cap, raise the step, or pick layers whose loops fit together.

## Normal and specular maps

If any layer has a `_n` or `_s` texture next to it, Veneer composites those too, following [LabPBR 1.3](https://shaderlabs.org/wiki/LabPBR_Material_Standard). They're handed out when something asks the resource manager for `<sprite>_n.png` or `<sprite>_s.png`, which is how Iris finds them, so they never take up space in the block atlas.

- Layers without a map count as flat and rough.
- Normals follow `rotate` and the flips.
- Material values in `_s` (everything except smoothness) switch over instead of fading, since a halfway value means a different material.
- For that reason `_s` animations don't get vanilla's runtime interpolation.
- A real `_n` or `_s` file in a pack always wins over the generated one.

## Building

Java 21.

```
./gradlew build
```

Jars end up in `fabric/build/libs` and `neoforge/build/libs`. To try it with shaders on Fabric, `./gradlew :fabric:shaderClient` launches the game with Sodium, Iris and Euphoria Patches. Drop a shader pack into `fabric/run/shaderpacks` first.
