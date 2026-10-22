/* tslint:disable */
/* eslint-disable */

export class GameHandshakeProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    finish(): void;
    constructor();
    outgoing(): Uint8Array;
    report(): string;
}

export class HslRgbLookup {
    free(): void;
    [Symbol.dispose](): void;
    constructor(random_unit: any);
    report(): string;
    rgbTable(): Uint32Array;
    rgb(hsl: any): number;
    unsignedRgbTable(): Uint32Array;
    unsignedRgb(hsl: any): number;
}

export class Js5AssetProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    file(id: number): Uint8Array;
    finish(): void;
    constructor(token: string, index: number, archive: number);
    outgoing(): Uint8Array;
    report(): string;
}

export class Js5Probe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    finish(): void;
    constructor(token: string);
    outgoing(): Uint8Array;
    report(): string;
}

export class Js5ScriptGraphProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    file(id: any): Uint8Array;
    finish(): void;
    constructor(token: string, root: any);
    outgoing(): Uint8Array;
    report(): string;
    run(ints_json: string): string;
}

export class Js5TextureSet {
    free(): void;
    [Symbol.dispose](): void;
    addGraph(id: any, bytes: Uint8Array): void;
    addSprite(id: any, bytes: Uint8Array): void;
    needs(material: any): string;
    constructor();
    render(material: any): Uint8Array;
    report(): string;
    setTable(bytes: Uint8Array): number;
    size(material: any): number;
}

export class Js5WorldFloorProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    finish(): void;
    constructor(token: string);
    outgoing(): Uint8Array;
    overlay(id: any): string;
    report(): string;
    underlay(id: any): string;
}

export class Js5WorldMaterialProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    feed(bytes: Uint8Array): void;
    finish(): void;
    material(id: any): string;
    constructor(token: string);
    outgoing(): Uint8Array;
    report(): string;
}

export class Js5WorldTerrainProbe {
    free(): void;
    [Symbol.dispose](): void;
    complete(): boolean;
    composePalette(floors: Js5WorldFloorProbe): string;
    composeUnderwater(base_x: any, base_y: any, map_size: any): string;
    compose(base_x: any, base_y: any, map_size: any): string;
    feed(bytes: Uint8Array): void;
    finish(): void;
    groundTile(plane: any, x: any, y: any): string;
    hasUnderwaterSurface(): boolean;
    height(region: any, plane: any, x: any, y: any): number;
    lowerGroundLit(rgb: HslRgbLookup, materials: Js5WorldMaterialProbe, underwater: Js5WorldTerrainProbe): string;
    lowerGround(rgb: HslRgbLookup, materials: Js5WorldMaterialProbe): string;
    loweredIndices(material: any): Uint32Array;
    loweredVertices(material: any): Uint8Array;
    constructor(token: string, regions_json: string);
    outgoing(): Uint8Array;
    paletteColor(plane: any, x: any, y: any): any;
    prepareGround(floors: Js5WorldFloorProbe, materials: Js5WorldMaterialProbe, plane: any, max_vertices: any): string;
    report(): string;
    sceneHeight(plane: any, x: any, y: any): any;
    sceneTile(plane: any, x: any, y: any): string;
    static underwater(token: string, regions_json: string): Js5WorldTerrainProbe;
}

export class OriginalLobbySession {
    free(): void;
    [Symbol.dispose](): void;
    close(): void;
    confirm(): void;
    feed(bytes: any): string;
    finish(): void;
    markFlushed(): void;
    constructor(config_json: any);
    outgoing(): Uint8Array;
    phase(): string;
    report(): string;
    requestWorldList(version: any): void;
    startWorldState(config_json: any, varp_count: any): OriginalWorldSession;
    startWorld(config_json: any): OriginalWorldSession;
    tick(): void;
}

export class OriginalWorldSession {
    private constructor();
    free(): void;
    [Symbol.dispose](): void;
    close(): void;
    confirm(): void;
    container(id: any, negative_key: any): string;
    enableInventory(): void;
    enablePlayerStats(): void;
    feed(bytes: any): string;
    finish(): void;
    gameTick(): void;
    item(id: any, negative_key: any, slot: any): string;
    markFlushed(): void;
    outgoing(): Uint8Array;
    phase(): string;
    playerStats(): string;
    report(): string;
    skill(id: any): string;
    tick(): string;
    varp(id: any): number;
}

export function runCoreScript(bytes: Uint8Array): string;

export type InitInput = RequestInfo | URL | Response | BufferSource | WebAssembly.Module;

export interface InitOutput {
    readonly memory: WebAssembly.Memory;
    readonly __wbg_gamehandshakeprobe_free: (a: number, b: number) => void;
    readonly __wbg_hslrgblookup_free: (a: number, b: number) => void;
    readonly __wbg_js5assetprobe_free: (a: number, b: number) => void;
    readonly __wbg_js5probe_free: (a: number, b: number) => void;
    readonly __wbg_js5scriptgraphprobe_free: (a: number, b: number) => void;
    readonly __wbg_js5textureset_free: (a: number, b: number) => void;
    readonly __wbg_js5worldfloorprobe_free: (a: number, b: number) => void;
    readonly __wbg_js5worldmaterialprobe_free: (a: number, b: number) => void;
    readonly __wbg_js5worldterrainprobe_free: (a: number, b: number) => void;
    readonly __wbg_originallobbysession_free: (a: number, b: number) => void;
    readonly __wbg_originalworldsession_free: (a: number, b: number) => void;
    readonly gamehandshakeprobe_complete: (a: number) => number;
    readonly gamehandshakeprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly gamehandshakeprobe_finish: (a: number, b: number) => void;
    readonly gamehandshakeprobe_new: () => number;
    readonly gamehandshakeprobe_outgoing: (a: number, b: number) => void;
    readonly gamehandshakeprobe_report: (a: number, b: number) => void;
    readonly hslrgblookup_new: (a: number, b: number) => void;
    readonly hslrgblookup_report: (a: number, b: number) => void;
    readonly hslrgblookup_rgb: (a: number, b: number, c: number) => void;
    readonly hslrgblookup_rgbTable: (a: number, b: number) => void;
    readonly hslrgblookup_unsignedRgb: (a: number, b: number, c: number) => void;
    readonly hslrgblookup_unsignedRgbTable: (a: number, b: number) => void;
    readonly js5assetprobe_complete: (a: number) => number;
    readonly js5assetprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5assetprobe_file: (a: number, b: number, c: number) => void;
    readonly js5assetprobe_finish: (a: number, b: number) => void;
    readonly js5assetprobe_new: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5assetprobe_outgoing: (a: number, b: number) => void;
    readonly js5assetprobe_report: (a: number, b: number) => void;
    readonly js5probe_complete: (a: number) => number;
    readonly js5probe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5probe_finish: (a: number, b: number) => void;
    readonly js5probe_new: (a: number, b: number, c: number) => void;
    readonly js5probe_outgoing: (a: number, b: number) => void;
    readonly js5probe_report: (a: number, b: number) => void;
    readonly js5scriptgraphprobe_complete: (a: number) => number;
    readonly js5scriptgraphprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5scriptgraphprobe_file: (a: number, b: number, c: number) => void;
    readonly js5scriptgraphprobe_finish: (a: number, b: number) => void;
    readonly js5scriptgraphprobe_new: (a: number, b: number, c: number, d: number) => void;
    readonly js5scriptgraphprobe_outgoing: (a: number, b: number) => void;
    readonly js5scriptgraphprobe_report: (a: number, b: number) => void;
    readonly js5scriptgraphprobe_run: (a: number, b: number, c: number, d: number) => void;
    readonly js5textureset_addGraph: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5textureset_addSprite: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5textureset_needs: (a: number, b: number, c: number) => void;
    readonly js5textureset_new: () => number;
    readonly js5textureset_render: (a: number, b: number, c: number) => void;
    readonly js5textureset_report: (a: number, b: number) => void;
    readonly js5textureset_setTable: (a: number, b: number, c: number, d: number) => void;
    readonly js5textureset_size: (a: number, b: number, c: number) => void;
    readonly js5worldfloorprobe_complete: (a: number) => number;
    readonly js5worldfloorprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5worldfloorprobe_finish: (a: number, b: number) => void;
    readonly js5worldfloorprobe_new: (a: number, b: number, c: number) => void;
    readonly js5worldfloorprobe_outgoing: (a: number, b: number) => void;
    readonly js5worldfloorprobe_overlay: (a: number, b: number, c: number) => void;
    readonly js5worldfloorprobe_report: (a: number, b: number) => void;
    readonly js5worldfloorprobe_underlay: (a: number, b: number, c: number) => void;
    readonly js5worldmaterialprobe_complete: (a: number) => number;
    readonly js5worldmaterialprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5worldmaterialprobe_finish: (a: number, b: number) => void;
    readonly js5worldmaterialprobe_material: (a: number, b: number, c: number) => void;
    readonly js5worldmaterialprobe_new: (a: number, b: number, c: number) => void;
    readonly js5worldmaterialprobe_outgoing: (a: number, b: number) => void;
    readonly js5worldmaterialprobe_report: (a: number, b: number) => void;
    readonly js5worldterrainprobe_complete: (a: number) => number;
    readonly js5worldterrainprobe_compose: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_composePalette: (a: number, b: number, c: number) => void;
    readonly js5worldterrainprobe_composeUnderwater: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_feed: (a: number, b: number, c: number, d: number) => void;
    readonly js5worldterrainprobe_finish: (a: number, b: number) => void;
    readonly js5worldterrainprobe_groundTile: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_hasUnderwaterSurface: (a: number) => number;
    readonly js5worldterrainprobe_height: (a: number, b: number, c: number, d: number, e: number, f: number) => void;
    readonly js5worldterrainprobe_lowerGround: (a: number, b: number, c: number, d: number) => void;
    readonly js5worldterrainprobe_lowerGroundLit: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_loweredIndices: (a: number, b: number, c: number) => void;
    readonly js5worldterrainprobe_loweredVertices: (a: number, b: number, c: number) => void;
    readonly js5worldterrainprobe_new: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_outgoing: (a: number, b: number) => void;
    readonly js5worldterrainprobe_paletteColor: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_prepareGround: (a: number, b: number, c: number, d: number, e: number, f: number) => void;
    readonly js5worldterrainprobe_report: (a: number, b: number) => void;
    readonly js5worldterrainprobe_sceneHeight: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_sceneTile: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly js5worldterrainprobe_underwater: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly originallobbysession_close: (a: number) => void;
    readonly originallobbysession_confirm: (a: number, b: number) => void;
    readonly originallobbysession_feed: (a: number, b: number, c: number) => void;
    readonly originallobbysession_finish: (a: number, b: number) => void;
    readonly originallobbysession_markFlushed: (a: number, b: number) => void;
    readonly originallobbysession_new: (a: number, b: number) => void;
    readonly originallobbysession_outgoing: (a: number, b: number) => void;
    readonly originallobbysession_phase: (a: number, b: number) => void;
    readonly originallobbysession_report: (a: number, b: number) => void;
    readonly originallobbysession_requestWorldList: (a: number, b: number, c: number) => void;
    readonly originallobbysession_startWorld: (a: number, b: number, c: number) => void;
    readonly originallobbysession_startWorldState: (a: number, b: number, c: number, d: number) => void;
    readonly originallobbysession_tick: (a: number, b: number) => void;
    readonly originalworldsession_close: (a: number) => void;
    readonly originalworldsession_confirm: (a: number, b: number) => void;
    readonly originalworldsession_container: (a: number, b: number, c: number, d: number) => void;
    readonly originalworldsession_enableInventory: (a: number, b: number) => void;
    readonly originalworldsession_enablePlayerStats: (a: number, b: number) => void;
    readonly originalworldsession_feed: (a: number, b: number, c: number) => void;
    readonly originalworldsession_finish: (a: number, b: number) => void;
    readonly originalworldsession_gameTick: (a: number, b: number) => void;
    readonly originalworldsession_item: (a: number, b: number, c: number, d: number, e: number) => void;
    readonly originalworldsession_markFlushed: (a: number, b: number) => void;
    readonly originalworldsession_outgoing: (a: number, b: number) => void;
    readonly originalworldsession_phase: (a: number, b: number) => void;
    readonly originalworldsession_playerStats: (a: number, b: number) => void;
    readonly originalworldsession_report: (a: number, b: number) => void;
    readonly originalworldsession_skill: (a: number, b: number, c: number) => void;
    readonly originalworldsession_tick: (a: number, b: number) => void;
    readonly originalworldsession_varp: (a: number, b: number, c: number) => void;
    readonly runCoreScript: (a: number, b: number, c: number) => void;
    readonly __wbindgen_export: (a: number, b: number) => number;
    readonly __wbindgen_export2: (a: number, b: number, c: number, d: number) => number;
    readonly __wbindgen_export3: (a: number) => void;
    readonly __wbindgen_add_to_stack_pointer: (a: number) => number;
    readonly __wbindgen_export4: (a: number, b: number, c: number) => void;
}

export type SyncInitInput = BufferSource | WebAssembly.Module;

/**
 * Instantiates the given `module`, which can either be bytes or
 * a precompiled `WebAssembly.Module`.
 *
 * @param {{ module: SyncInitInput }} module - Passing `SyncInitInput` directly is deprecated.
 *
 * @returns {InitOutput}
 */
export function initSync(module: { module: SyncInitInput } | SyncInitInput): InitOutput;

/**
 * If `module_or_path` is {RequestInfo} or {URL}, makes a request and
 * for everything else, calls `WebAssembly.instantiate` directly.
 *
 * @param {{ module_or_path: InitInput | Promise<InitInput> }} module_or_path - Passing `InitInput` directly is deprecated.
 *
 * @returns {Promise<InitOutput>}
 */
export default function __wbg_init (module_or_path?: { module_or_path: InitInput | Promise<InitInput> } | InitInput | Promise<InitInput>): Promise<InitOutput>;
