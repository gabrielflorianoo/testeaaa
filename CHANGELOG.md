# ProjectE QoL+ changelog

## 1.13.0
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
