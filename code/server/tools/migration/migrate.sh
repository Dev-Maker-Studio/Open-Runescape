#!/bin/bash
# Legacy content migration script
# Usage: ./migrate.sh <input-file> [output-file]
#
# Applies regex transformations to convert legacy Java/Kotlin content files
# into the modern Darkan server Kotlin codebase format.
#
# If output-file is omitted, prints to stdout. Use -i flag to edit in-place.

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 [-i] <input-file> [output-file]"
    echo "  -i    Edit file in-place"
    exit 1
fi

INPLACE=false
if [[ "$1" == "-i" ]]; then
    INPLACE=true
    shift
fi

INPUT="$1"
OUTPUT="${2:-}"

if [[ ! -f "$INPUT" ]]; then
    echo "Error: File not found: $INPUT"
    exit 1
fi

# Build sed command array
SED_ARGS=()

# --- Package imports ---
# Player/Entity/NPC core
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.player\.Player|org.darkan.world.entity.Player|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.npc\.NPC|org.darkan.world.entity.NPC|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.Entity|org.darkan.world.entity.Entity|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.player\.Skills|org.darkan.world.entity.player.Skills|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.npc\.combat|org.darkan.world.combat|g')

# World
SED_ARGS+=(-e 's|com\.rs\.game\.World|org.darkan.world.entity.World|g')

# Tile / WorldTile
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.Tile|world.gregs.voidps.type.Tile|g')
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.WorldTile|world.gregs.voidps.type.Tile|g')
SED_ARGS+=(-e 's|WorldTile|Tile|g')

# Items
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.Item|org.darkan.core.model.Item|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.player\.Equipment|org.darkan.world.entity.player.Equipment|g')
SED_ARGS+=(-e 's|com\.rs\.cache\.loaders\.ItemDefinitions|org.darkan.world.content.defs.ItemDefinitions|g')

# Game objects
SED_ARGS+=(-e 's|com\.rs\.game\.model\.object\.GameObject|org.darkan.core.model.WorldObject|g')
SED_ARGS+=(-e 's|GameObject|WorldObject|g')

# Animation/SpotAnim
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.Animation|org.darkan.core.model.Animation|g')
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.SpotAnim|org.darkan.core.update.SpotAnim|g')

# Dialogue
SED_ARGS+=(-e 's|com\.rs\.engine\.dialogue\.Dialogue|org.darkan.world.engine.dialogue.Conversation|g')
SED_ARGS+=(-e 's|com\.rs\.engine\.dialogue\.HeadE|org.darkan.world.engine.dialogue.HeadE|g')
SED_ARGS+=(-e 's|com\.rs\.engine\.dialogue|org.darkan.world.engine.dialogue|g')

# Plugin system
SED_ARGS+=(-e 's|com\.rs\.plugin\.annotations\.PluginEventHandler||g')
SED_ARGS+=(-e 's|com\.rs\.plugin\.annotations\.ServerStartupEvent|org.darkan.world.plugin.ServerStartupEvent|g')
SED_ARGS+=(-e 's|com\.rs\.plugin\.events|org.darkan.world.plugin|g')
SED_ARGS+=(-e 's|com\.rs\.plugin\.handlers|org.darkan.world.plugin|g')

# Skills/content
SED_ARGS+=(-e 's|com\.rs\.game\.content|org.darkan.world.content|g')
SED_ARGS+=(-e 's|com\.rs\.game\.tasks|org.darkan.world.engine|g')
SED_ARGS+=(-e 's|com\.rs\.engine\.quest|org.darkan.world.quest|g')

# Utils
SED_ARGS+=(-e 's|com\.rs\.lib\.util\.Utils|org.darkan.world.util.Utils|g')
SED_ARGS+=(-e 's|com\.rs\.utils\.shop|org.darkan.world.content.shops|g')

# Vars
SED_ARGS+=(-e 's|com\.rs\.lib\.game\.Vars|org.darkan.core.model.Vars|g')

# Combat
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.player\.managers\.InterfaceManager|org.darkan.world.entity.player.InterfaceManager|g')
SED_ARGS+=(-e 's|com\.rs\.game\.model\.entity\.Hit|org.darkan.core.model.Hit|g')

# --- Class/method renames ---

# Tile.of() factory → Tile()
SED_ARGS+=(-e 's|Tile\.of(\s*\(|Tile(|g')

# .getPlane() → .level
SED_ARGS+=(-e 's|\.getPlane()|.level|g')
SED_ARGS+=(-e 's|\.plane\b|.level|g')

# Constants → Skills
SED_ARGS+=(-e 's|Constants\.ATTACK\b|Skills.ATTACK|g')
SED_ARGS+=(-e 's|Constants\.DEFENSE\b|Skills.DEFENCE|g')
SED_ARGS+=(-e 's|Constants\.DEFENCE\b|Skills.DEFENCE|g')
SED_ARGS+=(-e 's|Constants\.STRENGTH\b|Skills.STRENGTH|g')
SED_ARGS+=(-e 's|Constants\.HITPOINTS\b|Skills.HITPOINTS|g')
SED_ARGS+=(-e 's|Constants\.RANGE\b|Skills.RANGED|g')
SED_ARGS+=(-e 's|Constants\.RANGED\b|Skills.RANGED|g')
SED_ARGS+=(-e 's|Constants\.PRAYER\b|Skills.PRAYER|g')
SED_ARGS+=(-e 's|Constants\.MAGIC\b|Skills.MAGIC|g')
SED_ARGS+=(-e 's|Constants\.COOKING\b|Skills.COOKING|g')
SED_ARGS+=(-e 's|Constants\.WOODCUTTING\b|Skills.WOODCUTTING|g')
SED_ARGS+=(-e 's|Constants\.FLETCHING\b|Skills.FLETCHING|g')
SED_ARGS+=(-e 's|Constants\.FISHING\b|Skills.FISHING|g')
SED_ARGS+=(-e 's|Constants\.FIREMAKING\b|Skills.FIREMAKING|g')
SED_ARGS+=(-e 's|Constants\.CRAFTING\b|Skills.CRAFTING|g')
SED_ARGS+=(-e 's|Constants\.SMITHING\b|Skills.SMITHING|g')
SED_ARGS+=(-e 's|Constants\.MINING\b|Skills.MINING|g')
SED_ARGS+=(-e 's|Constants\.HERBLORE\b|Skills.HERBLORE|g')
SED_ARGS+=(-e 's|Constants\.AGILITY\b|Skills.AGILITY|g')
SED_ARGS+=(-e 's|Constants\.THIEVING\b|Skills.THIEVING|g')
SED_ARGS+=(-e 's|Constants\.SLAYER\b|Skills.SLAYER|g')
SED_ARGS+=(-e 's|Constants\.FARMING\b|Skills.FARMING|g')
SED_ARGS+=(-e 's|Constants\.RUNECRAFTING\b|Skills.RUNECRAFTING|g')
SED_ARGS+=(-e 's|Constants\.HUNTER\b|Skills.HUNTER|g')
SED_ARGS+=(-e 's|Constants\.CONSTRUCTION\b|Skills.CONSTRUCTION|g')
SED_ARGS+=(-e 's|Constants\.SUMMONING\b|Skills.SUMMONING|g')
SED_ARGS+=(-e 's|Constants\.DUNGEONEERING\b|Skills.DUNGEONEERING|g')

# Utils.random → Random.nextInt (simple cases)
SED_ARGS+=(-e 's|Utils\.random(\(|Utils.random(|g')

# Remove @PluginEventHandler annotation lines
SED_ARGS+=(-e '/@PluginEventHandler/d')

# WorldTasks scheduling
SED_ARGS+=(-e 's|WorldTasks\.schedule\b|WorldTasks.schedule|g')

# --- Annotation cleanup ---
# Remove empty import lines that may result from removed packages
# (handled as a second pass)

if [[ "$INPLACE" == true ]]; then
    sed -i "${SED_ARGS[@]}" "$INPUT"
    # Remove blank import lines
    sed -i '/^import\s*$/d' "$INPUT"
    echo "Migrated in-place: $INPUT"
elif [[ -n "$OUTPUT" ]]; then
    sed "${SED_ARGS[@]}" "$INPUT" | sed '/^import\s*$/d' > "$OUTPUT"
    echo "Migrated: $INPUT → $OUTPUT"
else
    sed "${SED_ARGS[@]}" "$INPUT" | sed '/^import\s*$/d'
fi
