export const SOURCE_RATE = 22050;
export const BLOCK_FRAMES = 256;

const PROCESSOR = "openrs-pcm";
const RING_SECONDS = 2;

const WORKLET_SOURCE = `
class PcmProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    const config = options.processorOptions;
    this.channels = config.channels;
    this.step = config.sourceRate / sampleRate;
    this.size = config.sourceRate * config.seconds;
    this.ring = new Float32Array(this.size * this.channels);
    this.head = 0;
    this.fill = 0;
    this.phase = 0;
    this.gain = 1;
    this.underruns = 0;
    this.port.onmessage = (event) => this.message(event.data);
  }

  message(data) {
    if (data.type === "pcm") {
      const samples = new Int16Array(data.buffer);
      const frames = Math.floor(samples.length / this.channels);
      for (let frame = 0; frame < frames; frame++) {
        if (this.fill >= this.size) break;
        const slot = ((this.head + this.fill) % this.size) * this.channels;
        for (let c = 0; c < this.channels; c++) this.ring[slot + c] = samples[frame * this.channels + c] / 32768;
        this.fill++;
      }
    } else if (data.type === "gain") {
      this.gain = data.value;
    } else if (data.type === "flush") {
      this.head = 0;
      this.fill = 0;
      this.phase = 0;
    } else if (data.type === "stats") {
      this.port.postMessage({ type: "stats", queued: this.fill, underruns: this.underruns });
    }
  }

  process(inputs, outputs) {
    const out = outputs[0];
    const frames = out[0].length;
    for (let i = 0; i < frames; i++) {
      let left = 0;
      let right = 0;
      if (this.fill > 1) {
        const a = this.head * this.channels;
        const nextIndex = ((this.head + 1) % this.size) * this.channels;
        const t = this.phase;
        left = this.ring[a] * (1 - t) + this.ring[nextIndex] * t;
        right = this.channels > 1
          ? this.ring[a + 1] * (1 - t) + this.ring[nextIndex + 1] * t
          : left;
        this.phase += this.step;
        while (this.phase >= 1 && this.fill > 1) {
          this.phase -= 1;
          this.head = (this.head + 1) % this.size;
          this.fill--;
        }
      } else {
        this.underruns++;
      }
      out[0][i] = left * this.gain;
      if (out.length > 1) out[1][i] = right * this.gain;
    }
    return true;
  }
}
registerProcessor("${PROCESSOR}", PcmProcessor);
`;

export class PcmOutput {
  constructor(context, node, channels) {
    this.context = context;
    this.node = node;
    this.channels = channels;
    this.pushed = 0;
    this.stats = null;
    node.port.onmessage = (event) => {
      if (event.data.type === "stats") this.stats = event.data;
    };
  }

  static async create({ stereo = true, context = null } = {}) {
    const audio = context || new AudioContext({ latencyHint: "interactive" });
    const url = URL.createObjectURL(new Blob([WORKLET_SOURCE], { type: "text/javascript" }));
    try {
      await audio.audioWorklet.addModule(url);
    } finally {
      URL.revokeObjectURL(url);
    }
    const channels = stereo ? 2 : 1;
    const node = new AudioWorkletNode(audio, PROCESSOR, {
      numberOfInputs: 0,
      numberOfOutputs: 1,
      outputChannelCount: [2],
      processorOptions: { channels, sourceRate: SOURCE_RATE, seconds: RING_SECONDS },
    });
    node.connect(audio.destination);
    return new PcmOutput(audio, node, channels);
  }

  push(pcm) {
    const samples = pcm instanceof Int16Array ? pcm : Int16Array.from(pcm);
    const copy = samples.slice();
    this.node.port.postMessage({ type: "pcm", buffer: copy.buffer }, [copy.buffer]);
    this.pushed += Math.floor(copy.length / this.channels);
  }

  setGain(value) {
    this.node.port.postMessage({ type: "gain", value: Math.max(0, Math.min(1, value)) });
  }

  flush() {
    this.node.port.postMessage({ type: "flush" });
    this.pushed = 0;
  }

  requestStats() {
    this.node.port.postMessage({ type: "stats" });
  }

  resume() {
    return this.context.state === "running" ? Promise.resolve() : this.context.resume();
  }

  suspend() {
    return this.context.suspend();
  }

  async close() {
    this.node.disconnect();
    await this.context.close();
  }
}

export function blockSeconds(blocks) {
  return (blocks * BLOCK_FRAMES) / SOURCE_RATE;
}


const CYCLE_MS = 20;
const MAX_CYCLES = 5;
const MAX_BLOCKS = 32;
const MAX_FETCH = 6;
const GESTURES = ["pointerdown", "keydown", "touchstart"];

export function createWorldAudio({ fetchArchive = null, relay = null, token = "", target = globalThis, onStatus = () => {}, loadRuntime = null } = {}) {
  let engine = null;
  let output = null;
  let outputPending = false;
  let unlocked = false;
  let closed = false;
  let failure = null;
  let last = null;
  let cycleDebt = 0;
  let blockDebt = 0;
  const stats = { cycles: 0, blocks: 0, pushed: 0, events: 0, fetched: 0, absent: 0 };

  const fail = (error) => {
    failure = String(error?.message ?? error);
    onStatus(`world audio: ${failure}`);
  };

  const ready = (async () => {
    try {
      const module = loadRuntime ? await loadRuntime() : await import("./audio-runtime.js");
      await module.default();
      engine = new module.AudioEngine(true);
    } catch (error) { fail(error); }
  })();

  const startOutput = async () => {
    if (outputPending || output || closed) return;
    outputPending = true;
    try {
      output = await PcmOutput.create({ stereo: true });
      await output.resume();
    } catch (error) { fail(error); }
    outputPending = false;
  };

  const gesture = () => {
    if (unlocked) return;
    unlocked = true;
    for (const name of GESTURES) target.removeEventListener?.(name, gesture, true);
    startOutput();
  };
  for (const name of GESTURES) target.addEventListener?.(name, gesture, true);

  const load = async (index, archive) => {
    try { return await fetchArchive(relay, token, index, archive); }
    catch { return null; }
  };

  const pump = async () => {
    if (!engine || !fetchArchive || closed) return;
    const flat = engine.needs();
    const groups = [];
    for (let at = 0; at + 1 < flat.length; at += 2) groups.push([flat[at], flat[at + 1]]);
    for (let at = 0; at < groups.length && !closed; at += MAX_FETCH) {
      const slice = groups.slice(at, at + MAX_FETCH);
      const loaded = await Promise.all(slice.map(([index, archive]) => load(index, archive)));
      slice.forEach(([index, archive], i) => {
        const files = loaded[i]?.files;
        if (!files || files.size === 0) stats.absent += 1;
        else {
          for (const [file, bytes] of files) engine.addFile(index, archive, file, bytes);
          stats.fetched += 1;
        }
        engine.finishGroup(index, archive);
      });
    }
  };

  let pumping = false;
  const schedulePump = () => {
    if (pumping || closed) return;
    pumping = true;
    pump().catch(fail).finally(() => { pumping = false; });
  };

  const update = ({ events = [], tile = null, base = [], plane = 0, roll = 0, now = Date.now() } = {}) => {
    if (!engine || closed || failure) return false;
    try {
      if (events.length) { stats.events += engine.feed(Int32Array.from(events)); }
      if (tile && base.length === 2) engine.setListener(plane, (tile.x << 9) + 256, (tile.y << 9) + 256, 1, roll, base[0], base[1]);
      const elapsed = last === null ? 0 : Math.min(Math.max(now - last, 0), 250);
      last = now;
      cycleDebt += elapsed;
      let cycles = Math.min(Math.floor(cycleDebt / CYCLE_MS), MAX_CYCLES);
      cycleDebt = Math.min(cycleDebt - Math.floor(cycleDebt / CYCLE_MS) * CYCLE_MS, CYCLE_MS);
      while (cycles-- > 0) { engine.cycle(); stats.cycles += 1; }
      blockDebt += (elapsed * SOURCE_RATE) / 1000 / BLOCK_FRAMES;
      const blocks = Math.min(Math.floor(blockDebt), MAX_BLOCKS);
      blockDebt -= Math.floor(blockDebt);
      if (blocks > 0) {
        const pcm = engine.render(blocks);
        stats.blocks += blocks;
        if (output) { output.push(pcm); stats.pushed += blocks; }
      }
      schedulePump();
      return true;
    } catch (error) { fail(error); return false; }
  };

  return Object.freeze({
    update,
    unlock: gesture,
    ready: () => ready,
    summary: () => ({ ready: engine !== null, unlocked, output: output !== null, error: failure, stats: { ...stats }, music: engine?.musicVolume?.() ?? null, voices: engine?.voices?.() ?? null }),
    close: () => {
      closed = true;
      for (const name of GESTURES) target.removeEventListener?.(name, gesture, true);
      output?.close().catch(() => {});
      engine?.free?.();
      engine = null;
    },
  });
}

export default PcmOutput;
