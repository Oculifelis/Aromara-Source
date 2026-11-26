# NeoForge 1.21.1 build

This folder builds Aromara 0.6.0 for Minecraft 1.21.1 (NeoForge 21.1) on Thaumaturge 1.21.1-1.1.0. It is a hand port
of the 26.1.2 source in the repo root. Gameplay, data formats and numbers are the same.

Build: `gradlew build --no-daemon` here (through jobrunner). The Thaumaturge and JEI API jars go in `libs/`.

## What differs from 26.1.2

- Thaumaturgic Brewing brews are vanilla potion items on 1.21.1 too, so the same brazier brew tag covers them. The data
  is the same as 26.1.2.
- Item models use 1.21.1 model overrides instead of the 26.1 `items/` model definitions: `aromara:bundle` (scent and
  dried state) on the Redolent Bundle, `aromara:filled` on the Scent Phial, `custom_model_data` 1 on a finished
  Research Brief. The predicates are registered in `client/AromaraClient`.
- Recipe ingredients are written as `{"item": ...}` / `{"tag": ...}` objects.
- `util/ValueInput` and `util/ValueOutput` are small wrappers over CompoundTag so the save code matches 26.1.
- `util/CodecJsonListener` stands in for the 26.1 codec reload listener.
- Block entities drop their contents from `Block.onRemove`, since 1.21.1 has no `preRemoveSideEffects`.
- The Brazier Cart reports itself as a chest-type minecart to NeoForge (not rideable, not powered). It is placed by its
  own item code since `AbstractMinecart.createMinecart` only builds vanilla carts on 1.21.1.
- JEI uses the 1.21.1 API (`RecipeType`, `addRecipeCatalysts`).
- No selftest source set here.
