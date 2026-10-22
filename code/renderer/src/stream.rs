use crate::{
    frame::{self, Frame, NO_INSTANCES, NO_TEXTURE, REFLECTION, SHADOW_MAP},
    pass::Pass,
};
use std::collections::{HashMap, HashSet};

pub const HELLO: u8 = 0;
pub const RESOURCE: u8 = 1;
pub const FRAME: u8 = 2;
pub const PING: u8 = 3;
pub const RESET: u8 = 4;
pub const EPOCHS: u8 = 3;
pub const RESOURCE_IDS: usize = 65536;
pub const RESOURCE_BYTES: usize = 512 * 1024 * 1024;
pub const INPUT: u8 = 0;
pub const ACK: u8 = 1;
pub const PONG: u8 = 2;
pub const KEY_DOWN: u8 = 0;
pub const KEY_UP: u8 = 1;
pub const MOVE: u8 = 2;
pub const DOWN: u8 = 3;
pub const UP: u8 = 4;
pub const WHEEL: u8 = 5;
pub const RESIZE: u8 = 6;
pub const FOCUS: u8 = 7;
pub const WINDOW: u32 = 2;

#[derive(Clone, Debug, PartialEq, Default)]
pub struct Input {
    pub seq: u32,
    pub kind: u8,
    pub button: u8,
    pub a: f32,
    pub b: f32,
    pub code: String,
}

#[derive(Debug, PartialEq)]
pub enum ToHost {
    Input(Input),
    Ack(u32),
    Pong,
    Epochs,
}

#[derive(Debug, PartialEq)]
pub enum ToBrowser<'a> {
    Hello(&'a str),
    Resource(&'a [u8]),
    Frame {
        number: u32,
        seq: u32,
        dropped: u32,
        orf: &'a [u8],
    },
    Ping(u32),
    Reset {
        epoch: u32,
        after: u32,
    },
}

fn u32_at(b: &[u8], at: usize) -> Option<u32> {
    Some(u32::from_le_bytes(b.get(at..at + 4)?.try_into().ok()?))
}

pub fn hello(game: &str, width: u32, height: u32, tick: u32) -> Vec<u8> {
    let json =
        format!("{{\"game\":{game:?},\"width\":{width},\"height\":{height},\"tick\":{tick}}}");
    [&[HELLO][..], json.as_bytes()].concat()
}

pub fn epoch_hello(game: &str, width: u32, height: u32, tick: u32) -> Vec<u8> {
    let mut h = hello(game, width, height, tick);
    h.pop();
    h.extend(b",\"resourceEpochs\":1}");
    h
}

pub fn reset(epoch: u32, after: u32) -> Vec<u8> {
    [&[RESET, 1][..], &epoch.to_le_bytes(), &after.to_le_bytes()].concat()
}

pub fn resource(f: &Frame) -> Vec<u8> {
    [&[RESOURCE][..], &frame::write(f)].concat()
}

pub fn frame_message(number: u32, seq: u32, dropped: u32, f: &Frame) -> Vec<u8> {
    let mut out = vec![FRAME];
    [number, seq, dropped]
        .iter()
        .for_each(|v| out.extend(v.to_le_bytes()));
    out.extend(frame::write(f));
    out
}

pub fn ping(number: u32) -> Vec<u8> {
    [&[PING][..], &number.to_le_bytes()].concat()
}

pub fn decode_to_browser(b: &[u8]) -> Option<ToBrowser<'_>> {
    Some(match *b.first()? {
        HELLO => ToBrowser::Hello(std::str::from_utf8(&b[1..]).ok()?),
        RESOURCE => ToBrowser::Resource(&b[1..]),
        FRAME => ToBrowser::Frame {
            number: u32_at(b, 1)?,
            seq: u32_at(b, 5)?,
            dropped: u32_at(b, 9)?,
            orf: b.get(13..)?,
        },
        PING => ToBrowser::Ping(u32_at(b, 1)?),
        RESET if b.len() == 10 && b[1] == 1 => ToBrowser::Reset {
            epoch: u32_at(b, 2)?,
            after: u32_at(b, 6)?,
        },
        _ => return None,
    })
}

pub fn input(i: &Input) -> Vec<u8> {
    let mut out = vec![INPUT];
    out.extend(i.seq.to_le_bytes());
    out.extend([i.kind, i.button]);
    out.extend(i.a.to_le_bytes());
    out.extend(i.b.to_le_bytes());
    out.push(i.code.len().min(255) as u8);
    out.extend(&i.code.as_bytes()[..i.code.len().min(255)]);
    out
}

pub fn ack(number: u32) -> Vec<u8> {
    [&[ACK][..], &number.to_le_bytes()].concat()
}

pub fn decode_to_host(b: &[u8]) -> Option<ToHost> {
    Some(match *b.first()? {
        INPUT => {
            let len = *b.get(15)? as usize;
            ToHost::Input(Input {
                seq: u32_at(b, 1)?,
                kind: b[5],
                button: b[6],
                a: f32::from_bits(u32_at(b, 7)?),
                b: f32::from_bits(u32_at(b, 11)?),
                code: std::str::from_utf8(b.get(16..16 + len)?).ok()?.to_string(),
            })
        }
        ACK => ToHost::Ack(u32_at(b, 1)?),
        PONG => ToHost::Pong,
        EPOCHS if b == [EPOCHS, 1] => ToHost::Epochs,
        _ => return None,
    })
}

#[derive(Default, Clone, Copy, Debug, PartialEq)]
pub struct Stats {
    pub messages: u32,
    pub bytes: u64,
    pub resources: u32,
    pub received: u32,
    pub rendered: u32,
    pub dropped: u32,
    pub host_dropped: u32,
    pub host_frame: u32,
    pub first_frame: u32,
    pub shown: u32,
    pub input_seq: u32,
    pub frame_bytes: u32,
}

#[derive(Default)]
pub struct Stream {
    pub hello: Option<String>,
    pending: Frame,
    newest: Option<Frame>,
    pub stats: Stats,
    pub epoch: u32,
    epochs: bool,
    epoch_frame: bool,
    meshes: HashMap<u32, Pass>,
    textures: HashSet<u32>,
    resource_bytes: usize,
}

impl Stream {
    pub fn push(&mut self, bytes: &[u8]) -> Result<Option<Vec<u8>>, String> {
        self.stats.messages += 1;
        self.stats.bytes += bytes.len() as u64;
        match decode_to_browser(bytes).ok_or("bad stream message")? {
            ToBrowser::Hello(json) => {
                if self.hello.is_some() {
                    return Err("duplicate stream hello".into());
                }
                let h: serde_json::Value =
                    serde_json::from_str(json).map_err(|_| "bad stream hello")?;
                if let Some(version) = h.get("resourceEpochs") {
                    if version.as_u64() != Some(1) {
                        return Err("unsupported resource epoch version".into());
                    }
                    self.epochs = true;
                }
                self.hello = Some(json.to_string());
                if self.epochs {
                    return Ok(Some(vec![EPOCHS, 1]));
                }
            }
            ToBrowser::Reset { epoch, after } => {
                if !self.epochs
                    || !self.epoch_frame
                    || self.epoch.checked_add(1) != Some(epoch)
                    || self.stats.received == 0
                    || after != self.stats.shown
                    || after != self.stats.host_frame
                    || self.newest.is_some()
                    || !self.pending.meshes.is_empty()
                    || !self.pending.textures.is_empty()
                {
                    return Err("invalid resource epoch barrier".into());
                }
                self.pending = Frame::default();
                self.meshes.clear();
                self.textures.clear();
                self.resource_bytes = 0;
                self.epoch = epoch;
                self.epoch_frame = false;
            }
            ToBrowser::Resource(orf) => {
                let f = frame::parse(orf).ok_or("bad resource message")?;
                if self.epochs {
                    if !f.uniforms.is_empty() || !f.instances.is_empty() || !f.draws.is_empty() {
                        return Err("non-resource epoch record".into());
                    }
                    let mut ids = HashSet::new();
                    if f.meshes
                        .iter()
                        .map(|m| m.id)
                        .chain(f.textures.iter().map(|t| t.id))
                        .any(|id| {
                            !ids.insert(id)
                                || self.meshes.contains_key(&id)
                                || self.textures.contains(&id)
                        })
                    {
                        return Err("duplicate epoch resource id".into());
                    }
                    let bytes = f
                        .meshes
                        .iter()
                        .map(|m| m.vertices.len() + m.indices.len() * 4)
                        .sum::<usize>()
                        + f.textures.iter().map(|t| t.rgba.len()).sum::<usize>();
                    if self.meshes.len() + self.textures.len() + ids.len() > RESOURCE_IDS
                        || self
                            .resource_bytes
                            .checked_add(bytes)
                            .is_none_or(|n| n > RESOURCE_BYTES)
                    {
                        return Err("resource epoch exceeds budget".into());
                    }
                    self.resource_bytes += bytes;
                    self.meshes.extend(f.meshes.iter().map(|m| (m.id, m.pass)));
                    self.textures.extend(f.textures.iter().map(|t| t.id));
                }
                self.stats.resources += (f.meshes.len() + f.textures.len()) as u32;
                self.pending.meshes.extend(f.meshes);
                self.pending.textures.extend(f.textures);
            }
            ToBrowser::Frame {
                number,
                seq,
                dropped,
                orf,
            } => {
                let f = frame::parse(orf).ok_or("bad frame message")?;
                if self.epochs {
                    if !f.meshes.is_empty()
                        || !f.textures.is_empty()
                        || (self.stats.received > 0 && number <= self.stats.host_frame)
                        || seq < self.stats.input_seq
                    {
                        return Err("invalid epoch frame sequence or inline resources".into());
                    }
                    let uniforms: HashSet<_> = f.uniforms.iter().map(|u| u.id).collect();
                    let instances: HashSet<_> = f.instances.iter().map(|i| i.id).collect();
                    for d in f.draws.iter().filter(|d| d.skip.is_none()) {
                        if self
                            .meshes
                            .get(&d.mesh)
                            .is_none_or(|p| p.stride() != d.pass.stride())
                            || !uniforms.contains(&d.uniform)
                            || (d.instances != NO_INSTANCES && !instances.contains(&d.instances))
                            || d.textures.iter().any(|t| {
                                ![NO_TEXTURE, SHADOW_MAP, REFLECTION].contains(t)
                                    && !self.textures.contains(t)
                            })
                        {
                            return Err("missing current epoch resource".into());
                        }
                    }
                }
                if self.stats.received == 0 {
                    self.stats.first_frame = number;
                }
                self.stats.received += 1;
                self.stats.dropped += self.newest.is_some() as u32;
                self.stats.host_dropped = dropped;
                self.stats.host_frame = number;
                self.stats.input_seq = seq;
                self.stats.frame_bytes = bytes.len() as u32;
                self.newest = Some(f);
                self.epoch_frame = true;
            }
            ToBrowser::Ping(_) => return Ok(Some(vec![PONG])),
        }
        Ok(None)
    }

    pub fn take(&mut self) -> Option<Frame> {
        let mut f = self.newest.take()?;
        f.meshes.append(&mut self.pending.meshes);
        f.textures.append(&mut self.pending.textures);
        self.stats.rendered += 1;
        self.stats.shown = self.stats.host_frame;
        Some(f)
    }

    pub fn active(&self) -> bool {
        self.hello.is_some()
    }

    pub fn json(&self) -> String {
        let s = &self.stats;
        format!(
            "{{\"hello\":{},\"messages\":{},\"bytes\":{},\"resources\":{},\"received\":{},\"rendered\":{},\"dropped\":{},\"hostDropped\":{},\"hostFrame\":{},\"firstFrame\":{},\"shown\":{},\"inputSeq\":{},\"frameBytes\":{},\"epoch\":{},\"epochResources\":{},\"epochBytes\":{}}}",
            self.hello.as_deref().unwrap_or("null"),
            s.messages,
            s.bytes,
            s.resources,
            s.received,
            s.rendered,
            s.dropped,
            s.host_dropped,
            s.host_frame,
            s.first_frame,
            s.shown,
            s.input_seq,
            s.frame_bytes,
            self.epoch,
            self.meshes.len() + self.textures.len(),
            self.resource_bytes
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::scene;

    #[test]
    fn messages_round_trip() {
        let i = Input {
            seq: 7,
            kind: DOWN,
            button: 2,
            a: 10.5,
            b: -3.,
            code: "KeyW".into(),
        };
        assert_eq!(decode_to_host(&input(&i)), Some(ToHost::Input(i)));
        assert_eq!(decode_to_host(&ack(99)), Some(ToHost::Ack(99)));
        assert_eq!(decode_to_host(&[PONG]), Some(ToHost::Pong));
        assert_eq!(decode_to_host(&[EPOCHS, 1]), Some(ToHost::Epochs));
        assert_eq!(decode_to_host(&[EPOCHS, 2]), None);
        assert_eq!(decode_to_host(&[EPOCHS, 1, 0]), None);
        assert_eq!(decode_to_host(&[9]), None);
        let h = hello("orbit", 800, 600, 60);
        assert_eq!(
            decode_to_browser(&h),
            Some(ToBrowser::Hello(
                "{\"game\":\"orbit\",\"width\":800,\"height\":600,\"tick\":60}"
            ))
        );
        let f = scene::build("model", 3, 320, 240).unwrap();
        let m = frame_message(3, 7, 1, &f);
        let Some(ToBrowser::Frame {
            number,
            seq,
            dropped,
            orf,
        }) = decode_to_browser(&m)
        else {
            panic!()
        };
        assert_eq!((number, seq, dropped), (3, 7, 1));
        assert_eq!(frame::parse(orf).unwrap().draws.len(), f.draws.len());
        assert_eq!(decode_to_browser(&ping(4)), Some(ToBrowser::Ping(4)));
    }

    #[test]
    fn stream_keeps_resources_and_drops_stale_frames() {
        let mut s = Stream::default();
        assert!(!s.active());
        assert_eq!(s.push(&hello("model", 320, 240, 60)).unwrap(), None);
        assert!(s.active());
        let first = scene::build("model", 0, 320, 240).unwrap();
        let res = Frame {
            meshes: first.meshes.clone(),
            textures: first.textures.clone(),
            ..Default::default()
        };
        s.push(&resource(&res)).unwrap();
        assert_eq!(s.stats.resources, 2);
        assert!(s.take().is_none());
        s.push(&frame_message(
            0,
            0,
            0,
            &scene::build("model", 1, 320, 240).unwrap(),
        ))
        .unwrap();
        s.push(&frame_message(
            2,
            5,
            1,
            &scene::build("model", 2, 320, 240).unwrap(),
        ))
        .unwrap();
        let f = s.take().unwrap();
        assert_eq!((f.meshes.len(), f.textures.len(), f.draws.len()), (1, 1, 3));
        assert_eq!(
            (
                s.stats.received,
                s.stats.rendered,
                s.stats.dropped,
                s.stats.host_dropped,
                s.stats.shown,
                s.stats.input_seq
            ),
            (2, 1, 1, 1, 2, 5)
        );
        assert!(s.take().is_none());
        assert_eq!(s.push(&ping(1)).unwrap(), Some(vec![PONG]));
        assert!(s.push(&[7]).is_err());
        assert!(s.json().contains("\"shown\":2"));
    }

    fn epoch_stream() -> Stream {
        let mut s = Stream::default();
        assert_eq!(
            s.push(&epoch_hello("native", 64, 64, 5)).unwrap(),
            Some(vec![EPOCHS, 1])
        );
        let mut f = scene::build("model", 0, 64, 64).unwrap();
        let r = Frame {
            meshes: std::mem::take(&mut f.meshes),
            textures: std::mem::take(&mut f.textures),
            ..Default::default()
        };
        s.push(&resource(&r)).unwrap();
        s.push(&frame_message(10, 17, 2, &f)).unwrap();
        s
    }

    #[test]
    fn epoch_barrier_rejects_malformed_unnegotiated_stale_and_unrendered_resets() {
        let mut legacy = Stream::default();
        legacy.push(&hello("model", 64, 64, 5)).unwrap();
        assert!(legacy.push(&reset(1, 0)).is_err());
        let mut s = epoch_stream();
        assert!(s.push(&reset(1, 10)).is_err());
        s.take().unwrap();
        assert!(s.push(&reset(2, 10)).is_err());
        assert!(s.push(&reset(1, 9)).is_err());
        let mut bad = reset(1, 10);
        bad[1] = 2;
        assert!(s.push(&bad).is_err());
        assert!(s.push(&reset(1, 10)[..9]).is_err());
        let mut bad = reset(1, 10);
        bad.push(0);
        assert!(s.push(&bad).is_err());
        s.push(&reset(1, 10)).unwrap();
        assert_eq!(s.epoch, 1);
        assert!(s.push(&reset(1, 10)).is_err());
        assert!(s.push(&reset(2, 10)).is_err());
        assert_eq!(
            (s.stats.shown, s.stats.input_seq, s.stats.received),
            (10, 17, 1)
        );
        assert!(s.json().contains("\"epochResources\":0"));
        assert!(s.json().contains("\"epochBytes\":0"));
        assert!(s.take().is_none());
        let mut invalid = Stream::default();
        assert!(invalid.push(b"\x00{\"resourceEpochs\":2}").is_err());
    }

    #[test]
    fn epoch_frame_requires_complete_current_resources_and_does_not_reset_other_streams() {
        let mut a = epoch_stream();
        let mut b = epoch_stream();
        a.take().unwrap();
        a.push(&reset(1, 10)).unwrap();
        let mut f = scene::build("model", 0, 64, 64).unwrap();
        let r = Frame {
            meshes: std::mem::take(&mut f.meshes),
            textures: std::mem::take(&mut f.textures),
            ..Default::default()
        };
        assert!(a.push(&frame_message(11, 18, 2, &f)).is_err());
        let partial = Frame {
            meshes: r.meshes.clone(),
            ..Default::default()
        };
        a.push(&resource(&partial)).unwrap();
        assert!(a.push(&frame_message(11, 18, 2, &f)).is_err());
        assert!(a.push(&resource(&partial)).is_err());
        a.push(&resource(&Frame {
            textures: r.textures,
            ..Default::default()
        }))
        .unwrap();
        assert!(a.push(&frame_message(10, 18, 2, &f)).is_err());
        assert!(a.push(&frame_message(11, 16, 2, &f)).is_err());
        a.push(&frame_message(11, 18, 2, &f)).unwrap();
        let displayed = a.take().unwrap();
        assert_eq!((displayed.meshes.len(), displayed.textures.len()), (1, 1));
        assert_eq!((a.stats.shown, a.stats.input_seq), (11, 18));
        assert_eq!(b.epoch, 0);
        let displayed = b.take().unwrap();
        assert_eq!((displayed.meshes.len(), displayed.textures.len()), (1, 1));
        assert_eq!((b.stats.shown, b.stats.input_seq), (10, 17));
    }
}
