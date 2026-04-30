# Rendering System Files Reference

## Overview
This document provides a comprehensive list of all files related to sprite rendering, block texturing, item rendering, and shader code in the Adventura voxel game engine.

---

## 1. SHADER FILES (.frag, .vert, .glsl)

### Chunk Rendering Shaders
- [client/src/main/resources/assets/voxelgame/shaders/chunk.vert](client/src/main/resources/assets/voxelgame/shaders/chunk.vert)
  - Vertex shader for terrain/chunk rendering
  - Handles vertex positions, normals, face UVs, and lighting calculations
  - Processes per-vertex attributes like block ID and shading

- [client/src/main/resources/assets/voxelgame/shaders/chunk.frag](client/src/main/resources/assets/voxelgame/shaders/chunk.frag)
  - Fragment shader for terrain/chunk rendering
  - Handles texture atlas lookups, lighting, ambient occlusion, fog, and bloom
  - Manages block coloring, emissive effects, and animated fluids
  - Implements face-based UV coordinate calculations (top, bottom, side faces)

### Entity Rendering Shaders
- [client/src/main/resources/assets/voxelgame/shaders/entity.vert](client/src/main/resources/assets/voxelgame/shaders/entity.vert)
  - Vertex shader for entity/player model rendering

- [client/src/main/resources/assets/voxelgame/shaders/entity.frag](client/src/main/resources/assets/voxelgame/shaders/entity.frag)
  - Fragment shader for entity/player models

### Particle Rendering Shaders
- [client/src/main/resources/assets/voxelgame/shaders/particle.vert](client/src/main/resources/assets/voxelgame/shaders/particle.vert)
  - Vertex shader for particle system

- [client/src/main/resources/assets/voxelgame/shaders/particle.frag](client/src/main/resources/assets/voxelgame/shaders/particle.frag)
  - Fragment shader for particle effects

### UI Rendering Shaders
- [client/src/main/resources/assets/voxelgame/shaders/ui.vert](client/src/main/resources/assets/voxelgame/shaders/ui.vert)
  - Vertex shader for UI elements

- [client/src/main/resources/assets/voxelgame/shaders/ui.frag](client/src/main/resources/assets/voxelgame/shaders/ui.frag)
  - Fragment shader for UI rendering

- [client/src/main/resources/assets/voxelgame/shaders/ui_sprite.vert](client/src/main/resources/assets/voxelgame/shaders/ui_sprite.vert)
  - Vertex shader for UI sprite rendering

- [client/src/main/resources/assets/voxelgame/shaders/ui_sprite.frag](client/src/main/resources/assets/voxelgame/shaders/ui_sprite.frag)
  - Fragment shader for UI sprites

### Debug Shaders
- [client/src/main/resources/assets/voxelgame/shaders/debug_line.vert](client/src/main/resources/assets/voxelgame/shaders/debug_line.vert)
  - Vertex shader for debug line rendering

- [client/src/main/resources/assets/voxelgame/shaders/debug_line.frag](client/src/main/resources/assets/voxelgame/shaders/debug_line.frag)
  - Fragment shader for debug visualizations

---

## 2. BLOCK FACE RENDERING & TEXTURE COORDINATES

### Core Mesh Building
- [client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java)
  - **Key class for chunk geometry generation**
  - Implements greedy meshing algorithm (`buildGreedySolidMesh()`, `appendGreedySolidFaces()`)
  - Handles face culling and visibility determination
  - Contains face orientation definitions with corner coordinates
  - Implements UV coordinate calculation (`faceUv()` method)
  - Per-face data: normal vectors (nx, ny, nz) and corner positions
  - Supports ambient occlusion calculations
  - Methods: `addFace()`, `addMergedFace()`, `addCrossSprite()`, `addSpriteQuad()`
  - Vertex format: 11 floats per vertex (position x3, normal x3, blockId, light, AO, UV x2)

- [client/src/main/java/dev/voxelgame/client/render/ChunkMesh.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesh.java)
  - Data structure holding vertex and index buffers for a single chunk
  - Stores compiled mesh data ready for GPU upload

- [client/src/main/java/dev/voxelgame/client/render/GpuChunkMesh.java](client/src/main/java/dev/voxelgame/client/render/GpuChunkMesh.java)
  - GPU-side representation of chunk mesh
  - Manages VAO/VBO objects for rendering
  - Handles vertex attribute binding and draw calls

### Block Properties & Rendering Configuration
- [client/src/main/java/dev/voxelgame/client/render/BlockRenderProperties.java](client/src/main/java/dev/voxelgame/client/render/BlockRenderProperties.java)
  - Stores per-block rendering properties (tint color, alpha, emissive intensity)
  - Contains material flags and texture gap filling indicators
  - Provides color/alpha/effects lookup tables for all 256+ block types
  - Converts render properties to shader-compatible float arrays

### Texture Coordinate Management
- [client/src/main/java/dev/voxelgame/client/render/assets/BlockTextureAtlas.java](client/src/main/java/dev/voxelgame/client/render/assets/BlockTextureAtlas.java)
  - **Central texture atlas management**
  - Loads and compiles block textures from individual files or sprite sheets
  - Creates UV coordinate lookup tables per block (side/top/bottom faces)
  - Handles fallback texture loading from bundled sprite sheets
  - Manages TextureFace enum (SIDE, TOP, BOTTOM)
  - UV table storage: 4 floats per block per face (x, y, width, height)
  - Methods: `bindAndApply()`, `loadDefault()`, `load()`
  - Searches texture candidates based on block name and face type
  - Processes sprite sheets by extracting regions and removing backgrounds

---

## 3. ITEM RENDERING

### Entity Model System
- [client/src/main/java/dev/voxelgame/client/render/entity/EntityModel.java](client/src/main/java/dev/voxelgame/client/render/entity/EntityModel.java)
  - Defines hierarchical entity models (skeletons with parts)
  - Used for player and NPC models
  - Contains armature and joint data

- [client/src/main/java/dev/voxelgame/client/render/entity/EntityModelPart.java](client/src/main/java/dev/voxelgame/client/render/entity/EntityModelPart.java)
  - Individual body part of an entity model
  - Handles model part geometry and transformations

- [client/src/main/java/dev/voxelgame/client/render/entity/EntityModelRegistry.java](client/src/main/java/dev/voxelgame/client/render/entity/EntityModelRegistry.java)
  - Registry for loading and managing entity models
  - Loads models for different entity types (players, mobs, NPCs)

- [client/src/main/java/dev/voxelgame/client/render/entity/EntityRenderer.java](client/src/main/java/dev/voxelgame/client/render/entity/EntityRenderer.java)
  - **Renders entities, players, and items in hand**
  - Handles model animation and transformation matrices
  - Renders tools/pickaxes in player hand
  - Manages entity pose updates and interpolation

### Item/Tool Rendering
- [client/src/main/java/dev/voxelgame/client/render/entity/EntityRenderer.java](client/src/main/java/dev/voxelgame/client/render/entity/EntityRenderer.java)
  - Contains item rendering pipeline
  - Tools (pickaxes, axes, etc.) rendered as part of player entity model
  - Hand item positioning and rotation

### UI Sprite Rendering (for inventory/hotbar items)
- [client/src/main/java/dev/voxelgame/client/ui/UiSpriteRenderer.java](client/src/main/java/dev/voxelgame/client/ui/UiSpriteRenderer.java)
  - Renders 2D UI sprites (item icons, hotbar items)
  - Used for inventory display

- [client/src/main/java/dev/voxelgame/client/ui/UiSprite.java](client/src/main/java/dev/voxelgame/client/ui/UiSprite.java)
  - Data structure for UI sprite definition

- [client/src/main/java/dev/voxelgame/client/ui/UiSpriteSheet.java](client/src/main/java/dev/voxelgame/client/ui/UiSpriteSheet.java)
  - Sprite sheet management for UI elements

- [client/src/main/java/dev/voxelgame/client/ui/GameSprites.java](client/src/main/java/dev/voxelgame/client/ui/GameSprites.java)
  - Registry of all game sprites used in UI

---

## 4. BLOCK TEXTURE/ASSET MAPPING

### Asset Management
- [tools/src/main/java/dev/voxelgame/tools/AssetAtlasReport.java](tools/src/main/java/dev/voxelgame/tools/AssetAtlasReport.java)
  - **Asset validation and reporting tool**
  - Checks for missing block textures
  - Maps TextureFace types to actual texture files
  - Provides texture candidate lookup based on block naming conventions
  - Reports texture atlas status and missing assets

- [tools/src/main/java/dev/voxelgame/tools/AssetToolMain.java](tools/src/main/java/dev/voxelgame/tools/AssetToolMain.java)
  - Entry point for asset validation tools

### Block Type Definitions
- [common/src/main/java/dev/voxelgame/common/block/BlockType.java](common/src/main/java/dev/voxelgame/common/block/BlockType.java)
  - Defines properties for each block type
  - References texture assets

- [common/src/main/java/dev/voxelgame/common/block/Blocks.java](common/src/main/java/dev/voxelgame/common/block/Blocks.java)
  - Registry of all block types with their rendering properties

- [common/src/main/java/dev/voxelgame/common/block/BlockRenderLayer.java](common/src/main/java/dev/voxelgame/common/block/BlockRenderLayer.java)
  - Enumeration of render layers (SOLID, TRANSPARENT, etc.)
  - Used to separate rendering passes

---

## 5. GREEDY MESHING & BLOCK CONNECTION SMOOTHING

### Greedy Meshing Implementation
- [client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java)
  - `buildGreedySolidMesh()` - Entry point for greedy mesh building
  - `appendGreedySolidFaces()` - Main greedy algorithm implementation
  - `addMergedFace()` - Renders merged/combined quad faces
  - `isGreedyBlock()` - Determines if block can participate in greedy merging
  - Conditions: SOLID layer, opaque, collidable blocks only
  - Face merging: combines adjacent same-type faces into larger rectangles
  - `GreedyCell` record: tracks blockId and light per cell
  - `uAxis()` / `vAxis()` - Calculate 2D axes for greedy merging

### Face Culling
- [client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java)
  - `opaque()` - Checks if block is opaque (face is not rendered on occluded side)
  - Face culling logic in `buildSimpleMesh()` and `appendSimpleFaces()`
  - Prevents rendering hidden faces between adjacent blocks

### Ambient Occlusion (Smoothing)
- [client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java)
  - `ambientOcclusion()` - Calculates AO value for vertices
  - `aoSampleOrigin()` - Determines sample position for AO calculation
  - Per-vertex AO shading for smooth shadowing
  - Darkens corners where blocks meet (corner darkening effect)

---

## 6. LIGHTING & SHADOWS

### Light Engine
- [common/src/main/java/dev/voxelgame/common/world/light/LightEngine.java](common/src/main/java/dev/voxelgame/common/world/light/LightEngine.java)
  - **Core lighting calculation system**
  - Computes skylight and blocklight values
  - Propagates light through chunks
  - Handles light updates on block changes

### Chunk Light Data
- [common/src/main/java/dev/voxelgame/common/world/ChunkSection.java](common/src/main/java/dev/voxelgame/common/world/ChunkSection.java)
  - Stores skyLight and blockLight per voxel
  - Each light value: 4-bit (0-15 range)
  - Methods: `setSkyLight()`, `setBlockLight()`, `copyBlockLight()`, `copySkyLight()`

- [common/src/main/java/dev/voxelgame/common/world/ChunkDataCodec.java](common/src/main/java/dev/voxelgame/common/world/ChunkDataCodec.java)
  - Serialization/deserialization of chunk light data
  - Encodes/decodes sky and block light arrays

### Per-Vertex Lighting in Mesher
- [client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java](client/src/main/java/dev/voxelgame/client/render/ChunkMesher.java)
  - `light()` - Samples light value at block position
  - Passed per-vertex to shader as `vLight`
  - Combined with ambient occlusion for final vertex shading

### Fragment-Level Lighting (Shader)
- [client/src/main/resources/assets/voxelgame/shaders/chunk.frag](client/src/main/resources/assets/voxelgame/shaders/chunk.frag)
  - Uses `vLight` input for per-fragment lighting
  - Applies global brightness adjustment
  - Combines with block color for final output

---

## 7. WORLD RENDERING & ORCHESTRATION

### Main Rendering System
- [client/src/main/java/dev/voxelgame/client/render/WorldRenderer.java](client/src/main/java/dev/voxelgame/client/render/WorldRenderer.java)
  - Orchestrates all chunk rendering
  - Manages rendering order (solid → transparent → effects)
  - Frustum culling of chunks
  - Batch rendering of GPU chunk meshes
  - Depth testing and blending management

### Chunk Border Visualization
- [client/src/main/java/dev/voxelgame/client/render/ChunkBorderRenderer.java](client/src/main/java/dev/voxelgame/client/render/ChunkBorderRenderer.java)
  - Debug renderer for chunk boundaries
  - Uses VAO/VBO for line drawing

---

## 8. SHADER PROGRAM MANAGEMENT

### Shader Compilation & Linking
- [client/src/main/java/dev/voxelgame/client/render/ShaderProgram.java](client/src/main/java/dev/voxelgame/client/render/ShaderProgram.java)
  - **Shader program wrapper**
  - Loads, compiles, and links GLSL shaders
  - Manages uniform variables
  - Methods: `setInt()`, `setVector4Array()`, `use()`, `close()`
  - Handles shader errors and compilation failures

---

## 9. RENDERING SETTINGS & CONFIGURATION

### Render Settings
- [client/src/main/java/dev/voxelgame/client/render/RenderSettings.java](client/src/main/java/dev/voxelgame/client/render/RenderSettings.java)
  - Global rendering configuration
  - Toggle ambient occlusion, fog, bloom, etc.
  - Adjustable render distance and graphics quality

- [client/src/main/java/dev/voxelgame/client/RenderPreset.java](client/src/main/java/dev/voxelgame/client/RenderPreset.java)
  - Predefined graphics quality presets
  - Different rendering profiles

### Resource Tracking
- [client/src/main/java/dev/voxelgame/client/render/RenderResourceTracker.java](client/src/main/java/dev/voxelgame/client/render/RenderResourceTracker.java)
  - Tracks GPU memory and resource usage
  - Monitors active meshes and textures

---

## 10. PARTICLE EFFECTS

- [client/src/main/java/dev/voxelgame/client/render/particle/ParticleSystem.java](client/src/main/java/dev/voxelgame/client/render/particle/ParticleSystem.java)
  - Particle system for effects (breaking blocks, explosions, etc.)
  - Uses dedicated particle shaders
  - Manages particle lifecycle and physics

---

## 11. UI RENDERING SYSTEM

### UI Renderer
- [client/src/main/java/dev/voxelgame/client/ui/UiRenderer.java](client/src/main/java/dev/voxelgame/client/ui/UiRenderer.java)
  - Renders UI elements (HUD, menus)
  - Handles text, buttons, panels

### UI Components
- [client/src/main/java/dev/voxelgame/client/ui/UiButton.java](client/src/main/java/dev/voxelgame/client/ui/UiButton.java)
  - Button UI component
  
- [client/src/main/java/dev/voxelgame/client/ui/UiColor.java](client/src/main/java/dev/voxelgame/client/ui/UiColor.java)
  - Color definition for UI
  
- [client/src/main/java/dev/voxelgame/client/ui/BitmapFont.java](client/src/main/java/dev/voxelgame/client/ui/BitmapFont.java)
  - Bitmap font rendering for text

---

## 12. BLOCK BREAK ANIMATION

- [client/src/main/java/dev/voxelgame/client/BlockBreakAnimation.java](client/src/main/java/dev/voxelgame/client/BlockBreakAnimation.java)
  - Animates block breaking/destruction overlay
  - Manages animation timing and crack stages
  - Rendered on top of target block faces

---

## 13. SUPPORTING STRUCTURES

### Item Types & Registry
- [common/src/main/java/dev/voxelgame/common/item/ItemType.java](common/src/main/java/dev/voxelgame/common/item/ItemType.java)
  - Defines item properties (including tools like pickaxes)

- [common/src/main/java/dev/voxelgame/common/item/Items.java](common/src/main/java/dev/voxelgame/common/item/Items.java)
  - Registry of all item types

- [common/src/main/java/dev/voxelgame/common/item/ItemStack.java](common/src/main/java/dev/voxelgame/common/item/ItemStack.java)
  - Data structure for item stacks in inventory

### Hotbar Management
- [client/src/main/java/dev/voxelgame/client/Hotbar.java](client/src/main/java/dev/voxelgame/client/Hotbar.java)
  - Manages player hotbar and selected item

---

## SUMMARY TABLE

| Category | File Count | Primary Purpose |
|----------|-----------|-----------------|
| Shaders | 12 files | GPU rendering programs for terrain, entities, UI, particles |
| Block Rendering | 5 files | Mesh generation, face rendering, UV mapping |
| Texture Management | 2 files | Texture atlas, asset loading and mapping |
| Entity/Item Rendering | 5 files | Player models, entity rendering, tool display |
| Lighting System | 3 files | Light propagation, per-vertex lighting |
| World Rendering | 2 files | Chunk orchestration, frustum culling |
| Supporting | 12+ files | UI, particles, configuration, utilities |
| **TOTAL** | **~40+ files** | Complete rendering pipeline |

---

## KEY TECHNICAL DETAILS

### Vertex Format (ChunkMesher)
- 11 floats per vertex:
  1. Position X
  2. Position Y
  3. Position Z
  4. Normal X
  5. Normal Y
  6. Normal Z
  7. Block ID
  8. Light value
  9. Ambient Occlusion
  10. Face UV X
  11. Face UV Y

### Face Orientation (6 faces per block)
```
- +X face (right)  : normal (1, 0, 0)
- -X face (left)   : normal (-1, 0, 0)
- +Y face (top)    : normal (0, 1, 0)
- -Y face (bottom) : normal (0, -1, 0)
- +Z face (front)  : normal (0, 0, 1)
- -Z face (back)   : normal (0, 0, -1)
```

### Texture Face Types
- **SIDE**: For X and Z facing sides
- **TOP**: For Y+ facing (top surface)
- **BOTTOM**: For Y- facing (bottom surface)

### Rendering Layers
- SOLID: Opaque blocks (greedy meshed)
- TRANSPARENT: Translucent blocks (no greedy mesh)
- Other specialized layers as needed

### Greedy Meshing Algorithm
- Merges adjacent same-type faces on the same plane
- Only applies to SOLID opaque collidable blocks
- Reduces vertex/index count significantly
- `GreedyCell` tracks blockId + light per 2D cell
