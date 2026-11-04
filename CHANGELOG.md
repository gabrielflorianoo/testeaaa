# ProjectE QoL+ changelog

## 1.19.0
- Hourglass of Undoing: Ctrl+Z for blocks you broke or placed (64 actions, 5 minutes; broken blocks come back for their EMC)
- Reality Seal: explosions near you don't destroy blocks while it is active (EMC per block saved, Curios belt)

## 1.18.0
- Alchemical Crafting Table (block) and Alchemical Crafting Tablet (portable): crafting grid whose slots can hold
  ghost ingredients taken from your transmutation knowledge; each craft pays their EMC value instead of consuming
  them. Searchable panel of learned items, shift-click bulk crafting, real items still work, JEI recipe transfer
  fills the grid with ghosts (missing knowledge highlighted)

## 1.17.0
- Seven new magic items with their own mechanics:
  - Tempus Stone: accelerate a single block (random ticks and block entity tickers), stacking up to 32x;
    #peqol:tempus_blacklist keeps ProjectE collectors/relays out
  - Dowsing Rod: attune to a block, private particle trail to the nearest one (forge:ores tags match variants)
  - Courier's Sigil: bind a container, send the off-hand stack or the whole backpack there from anywhere
  - Gaia's Brush: soak in a biome and paint it elsewhere (biomes resent to clients)
  - Wayfinder's Compass: locate 14 kinds of structures through #peqol:wayfinder/* structure tags, trail while held
  - Soulbinding Rune: soulbound items are kept on death (stored on the player until respawn)
  - Architect's Bottle: bottle up to 12x12x12 of blocks and pour them out elsewhere, rotated to your facing

## 1.16.0
- Six more magic items:
  - Storm Staff: lightning where you look (sneak: harmless flash)
  - Prismatic Wand: instant rainbow beam with magic damage
  - Recall Mirror: back to your bed / respawn anchor (or world spawn) from any dimension
  - Cloud Scepter: temporary cloud platforms that cushion falls and fade away
  - Soul Jar: trap a creature (villagers keep their trades) and release it elsewhere
  - Warding Lantern: no natural hostile spawns around you while active (Curios belt slot)

## 1.15.0
- Eight EMC-powered magic items:
  - Blink Rod: teleport to where you look (cost per block, no fall damage)
  - Chronal Echo: rewind your position and health a few seconds back in time
  - Lumen Wand: invisible light sources (shown while holding the wand), sneak to dispel nearby ones
  - Transmutation Scepter: imprint a block and transmute 1/9/25 connected blocks into it, paying only the EMC difference
  - Verdant Orb: bonemeal every plant around a block, paid per growth
  - Void Sponge: drain every fluid in a sphere and dry waterlogged blocks
  - Eye of the Seer: every creature nearby glows through walls
  - Gravitational Staff: pull items and XP to you, sneak for a shockwave against hostile mobs
- New `magic` config section, textures, recipes, translations, JEI pages, game tests

## 1.14.0
- Effect charms paid with EMC per minute: Vision (night vision), Haste (Haste II), Breath (water breathing), Strength
- Charm of Undying: cheat death like a totem for a large EMC fee (cooldown, not against /kill or the void)
- Feather Charm: fall damage paid with EMC per block
- Charm of Wisdom: experience orbs become EMC
- Curios slots and advancement criteria for all of them
- Alchemist's Toolkit: crafting table, anvil, stonecutter, smithing table, grindstone, loom and cartography table in one item
- Pocket Smelter: instantly smelt the stack in your other hand for EMC

## 1.13.0
- Alchemical Gauge threshold mode (`/peqol gauge threshold <amount>`, sneak + empty hand toggles)
- 5 new advancements (Excavator, Interface, Harvester/Pasture, Trade Post, Hourglass)
- `/peqol shops [item]`: world-wide Trade Post market listing (saved data, works for unloaded chunks)
- Master switch keybind that turns every charm off and back on
- Autostock screen (`/peqol autostock menu` + keybind)
- Exporter filter screen with stock amounts (stack size = keep N, single item = unlimited)
- Filter screens with 27 ghost slots for the Siphon Charm (with a junk mode button) and the Alchemical Vacuum

## 1.12.0
- Alchemical Pasture: breeds nearby animal pairs paying their food with EMC, with a population cap
- Celestial Hourglass: skip to morning / clear the weather for EMC (server toggle)
- Alchemical Harvester: harvests and replants crops (wheat, carrots, potatoes, beetroots, nether wart, melons, pumpkins, sugar cane, cactus, bamboo); produce into EMC; upgradeable radius

## 1.11.0
- Alchemical Display: floating text with the owner's EMC and EMC/s (block entity renderer)
- Handbook pages for the newer blocks and server features
- Alchemical Excavator: EMC-powered quarry (drops with EMC become personal EMC), upgradeable, protection aware,
  Silk Touch / Fortune via enchanted books

## 1.10.0
- `/peqolclient history`: sparkline of your EMC over the last 10 minutes
- Alchemical Spring: infinite water or lava paid with EMC, pushed into adjacent tanks/pipes or drained

## 1.9.0
- Alchemical Trade Post: player shops selling from the chest below for EMC (offline earnings are kept)
- Trade Post buy mode: the owner buys the product from other players (sneak + empty hand toggles)
- `/peqol shop price <amount>`
- Mod list logo

## 1.8.0
- Alchemical Interface: every learned item exposed as an item handler slot (extract creates from EMC, insert burns) for pipes, AE2 and RS
- `blocks.disabledRecipes` config + `peqol:recipe_enabled` recipe condition on every QoL+ recipe

## 1.7.0
- Jade integration: QoL+ machine info and ProjectE EMC block info inside Jade's tooltip (own panel hides when Jade is present)
- Named homes: `/peqol home set|del|list|<name>` with EMC-paid teleports and clickable list
- Favorites: `/peqol fav add|remove <item>` and `/peqol fav` with quick-buy buttons, plus a keybind

## 1.6.0
- Alchemical Machine Upgrade: tiers 1-3 for the Exporter (speed, items), Vacuum (speed, +2 radius) and Downlink (rate); kept when wrenched
- `peqol_emc` scoreboard objective kept up to date with every player's EMC

## 1.5.0
- Buttons on ProjectE's transmutation screen: **Learn Inventory** and **Burn Inventory**
- EMC goal progress bar on the HUD (`/peqolclient goal <item> [count]`)
- Inventory slots holding unlearned items with EMC get a small purple dot
- Siphon Charm junk mode (`#peqol:siphon_junk` tag: cobblestone, dirt, gravel, netherrack...)
- JEI info page for every item; `/peqol tablet`, `/peqol bag [color]`
- `/peqol share` + `/peqol accept`; `/peqolclient` to tweak HUD/tooltips in game
- Alchemist's Handbook (translated in-game guide); richer look-at info (condenser target/progress, EMC/s)
- Alchemical Prospector; `/peqol gift`; starter knowledge and learn-on-craft server options
- Pocket Workbench, Void Pouch, `/peqol back`, Siphon catches mob drops

## 1.4.0
- Look-at info panel for QoL+ machines and ProjectE EMC blocks
- `/peqol learnall` (+ keybind): learn everything burning only one of each
- Appraisal Lens can burn a container's contents (double sneak-click)
- Alchemical Gauge (comparator by EMC magnitude), `/peqol sell|xp|stats`, sneak quick-burn, learn toasts

## 1.3.0
- Philosopher's Wrench, Alchemical Downlink, `/peqol autostock`
- Alchemical Vacuum, Alchemical Anchor, Charm of Smelting, broken tool replacement
- Custom statistics, advancements tab, server blacklist, Klein Star total on the HUD

## 1.2.0
- Construction Wand with preview, Charm of Illumination, Appraisal Lens
- `/peqol known` with clickable buy buttons, knowledge progress, HUD held value, milestone toasts

## 1.1.0
- Alchemical pick block, Mending / Satiation / Stellar Link charms
- `/peqol buy|value|unlearned|stars|help`, Exporter stock mode, Uplink comparator, Curios support

## 1.0.0
- EMC HUD, keybinds (tablet, bag, quick burn), extra tooltips
- Alchemical Restocker, Siphon Charm, EMC Cheques, Knowledge Scrolls
- Alchemical Uplink / Importer / Exporter, `/peqol` commands
