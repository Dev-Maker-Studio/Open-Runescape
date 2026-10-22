use crate::world_resources::{INDEX_ANIM_BASE, Lookup, ResourceStore};
use openrs_entity::anim::{AnimBase, BaseStore, FrameSet, SeqType, Sequence};
use openrs_entity::pose::Pose;
use std::collections::HashMap;

const MAX_CATCH_UP: u64 = 2000;

pub type TrackKey = (i32, i32);

#[derive(Debug, Clone)]
struct Track {
    seq_id: i32,
    started: u64,
    last: u64,
    sequence: Sequence,
    ty: SeqType,
}

pub struct PoseCache {
    tracks: HashMap<TrackKey, Track>,
    store: ResourceStore,
    bases: BaseStore,
    sets: HashMap<i32, Option<FrameSet>>,
    frame_base: HashMap<(i32, i32), i32>,
    seqs: HashMap<i32, Option<SeqType>>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct Selection {
    group: i32,
    file: i32,
    next: Option<(i32, i32)>,
    cycle: i32,
    length: i32,
}

#[derive(Debug, Clone, Copy)]
pub struct TrackStart {
    pub seq_id: i32,
    pub started: u64,
    pub speed: i32,
    pub replay: i32,
}

fn selection_of(ty: &SeqType, sequence: &Sequence) -> Option<Selection> {
    let frames = ty.frames.as_ref()?;
    let durations = ty.durations.as_ref()?;
    let current = usize::try_from(sequence.current_frame_id)
        .ok()
        .filter(|index| *index < frames.len())
        .unwrap_or(frames.len() - 1);
    let frame = frames[current];
    let next = if ty.tweened {
        usize::try_from(sequence.next_frame_id)
            .ok()
            .and_then(|index| frames.get(index))
            .map(|id| (id >> 16, id & 0xffff))
    } else {
        None
    };
    Some(Selection {
        group: frame >> 16,
        file: frame & 0xffff,
        next,
        cycle: sequence.current_frame_duration,
        length: *durations.get(current)?,
    })
}

impl PoseCache {
    pub fn new(store: ResourceStore) -> Self {
        let source = store.clone();
        Self {
            tracks: HashMap::new(),
            store,
            bases: BaseStore::new(Box::new(move |id| source.anim_base(id).bytes())),
            sets: HashMap::new(),
            frame_base: HashMap::new(),
            seqs: HashMap::new(),
        }
    }

    pub fn clear(&mut self) {
        self.sets.clear();
        self.frame_base.clear();
        self.seqs.clear();
        self.tracks.clear();
        self.bases.loaded.clear();
    }

    pub fn sequence(&mut self, id: i32) -> Option<SeqType> {
        self.sequence_state(id).flatten()
    }

    /// `None` while the sequence config has not arrived, `Some(None)` when it does not exist.
    pub fn sequence_state(&mut self, id: i32) -> Option<Option<SeqType>> {
        if let Some(cached) = self.seqs.get(&id) {
            return Some(cached.clone());
        }
        match self.store.sequence_type(id) {
            Lookup::Unknown => None,
            Lookup::Absent => {
                self.seqs.insert(id, None);
                Some(None)
            }
            Lookup::Present(bytes) => {
                let decoded = SeqType::decode_entry(id, Some(&bytes)).ok();
                self.seqs.insert(id, decoded.clone());
                Some(decoded)
            }
        }
    }

    fn ensure_group(&mut self, group: i32) -> bool {
        if let Some(entry) = self.sets.get(&group) {
            return entry.is_some();
        }
        let files = match self.store.frameset_files(group) {
            None => return false,
            Some(None) => {
                self.sets.insert(group, None);
                return false;
            }
            Some(Some(files)) => files,
        };
        let mut bases = Vec::with_capacity(files.len());
        for (file, bytes) in &files {
            if bytes.len() < 3 {
                continue;
            }
            bases.push((*file, i32::from(u16::from_be_bytes([bytes[1], bytes[2]]))));
        }
        match FrameSet::decode(group, &files, &mut self.bases) {
            Ok(set) => {
                for (file, base) in bases {
                    self.frame_base.insert((group, file), base);
                }
                self.sets.insert(group, Some(set));
                true
            }
            Err(_) => {
                let waiting = self
                    .store
                    .needs()
                    .iter()
                    .any(|(index, _)| *index == INDEX_ANIM_BASE);
                if !waiting {
                    self.sets.insert(group, None);
                }
                false
            }
        }
    }

    fn track(
        &mut self,
        key: TrackKey,
        seq_id: i32,
        started: u64,
        speed: i32,
        replay: i32,
        cycle: u64,
    ) -> Option<Selection> {
        let stale = self.tracks.get(&key).is_none_or(|track| {
            track.seq_id != seq_id
                || (started != 0 && track.started != started)
                || cycle.saturating_sub(track.last) > MAX_CATCH_UP
                || cycle < track.last
        });
        if stale {
            let ty = self.sequence(seq_id)?;
            ty.frames.as_ref().filter(|frames| !frames.is_empty())?;
            let mut sequence = Sequence::new(ty.tweened);
            let lookup_type = ty.clone();
            sequence
                .animate(seq_id, speed, replay, false, &move |_| {
                    Some(lookup_type.clone())
                })
                .ok()?;
            let begin = if started == 0 {
                cycle
            } else {
                started.min(cycle)
            };
            self.tracks.insert(
                key,
                Track {
                    seq_id,
                    started,
                    last: begin,
                    sequence,
                    ty,
                },
            );
        }
        let track = self.tracks.get_mut(&key)?;
        while track.last < cycle {
            track.last += 1;
            if !track.sequence.reset_animation {
                track.sequence.progress_cycle(&track.ty, 1).ok()?;
            }
        }
        selection_of(&track.ty, &track.sequence)
    }

    pub fn track_finished(&self, key: TrackKey) -> bool {
        self.tracks
            .get(&key)
            .is_some_and(|track| track.sequence.reset_animation)
    }

    pub fn animate_tracked(
        &mut self,
        pose: &mut Pose,
        key: TrackKey,
        start: TrackStart,
        cycle: u64,
        hold_last: bool,
    ) -> bool {
        let Some(selection) = self.track(
            key,
            start.seq_id,
            start.started,
            start.speed,
            start.replay,
            cycle,
        ) else {
            return false;
        };
        if !hold_last && self.track_finished(key) {
            return false;
        }
        self.pose_selection(pose, selection, 0)
    }

    /// Applies the frame a caller-owned `Sequence` is on (`animateRotatedModel`); a sequence
    /// without frames or whose frame sets are not loaded yet leaves the pose alone.
    pub fn pose_sequence(
        &mut self,
        pose: &mut Pose,
        sequence: &Sequence,
        ty: &SeqType,
        rotation: i32,
    ) -> bool {
        let usable = ty.frames.as_ref().is_some_and(|f| !f.is_empty());
        if !usable {
            return false;
        }
        let Some(selection) = selection_of(ty, sequence) else {
            return false;
        };
        self.pose_selection(pose, selection, rotation)
    }

    fn pose_selection(&mut self, pose: &mut Pose, selection: Selection, rotation: i32) -> bool {
        if !self.ensure_group(selection.group) {
            return false;
        }
        let Some(&base_id) = self.frame_base.get(&(selection.group, selection.file)) else {
            return false;
        };
        let next = selection.next.and_then(|(group, file)| {
            if !self.ensure_group(group) {
                return None;
            }
            (self.frame_base.get(&(group, file)) == Some(&base_id)).then_some((group, file))
        });
        let Some(base): Option<&AnimBase> = self.bases.loaded.get(&base_id) else {
            return false;
        };
        let Some(Some(first_set)) = self.sets.get(&selection.group) else {
            return false;
        };
        let Ok(first) = first_set.frame(selection.file) else {
            return false;
        };
        let next_frame = next.and_then(|(group, file)| {
            self.sets
                .get(&group)
                .and_then(Option::as_ref)
                .and_then(|set| set.frame(file).ok())
        });
        pose.animate_model_frame(
            base,
            first,
            next_frame,
            selection.cycle,
            selection.length,
            rotation,
        )
        .is_ok()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn cache(replay_interval: i32) -> PoseCache {
        let mut cache = PoseCache::new(ResourceStore::new());
        let mut ty = SeqType::new(5);
        ty.frames = Some(vec![0, 1, 2]);
        ty.durations = Some(vec![2, 2, 2]);
        ty.replay_interval = replay_interval;
        ty.replay_count = 99;
        cache.seqs.insert(5, Some(ty));
        cache
    }

    fn frame(cache: &mut PoseCache, started: u64, cycle: u64) -> Option<i32> {
        cache
            .track((2, 1), 5, started, 0, 0, cycle)
            .map(|selection| selection.file)
    }

    #[test]
    fn track_counts_from_the_start_cycle_not_the_scene_cycle() {
        let mut cache = cache(-1);
        assert_eq!(frame(&mut cache, 100, 100), Some(0));
        assert_eq!(frame(&mut cache, 100, 102), Some(0));
        assert_eq!(frame(&mut cache, 100, 103), Some(1));
        assert_eq!(frame(&mut cache, 100, 105), Some(2));
        assert!(!cache.track_finished((2, 1)));
        assert_eq!(frame(&mut cache, 100, 108), Some(2));
        assert!(cache.track_finished((2, 1)));
    }

    #[test]
    fn track_restarts_when_the_server_replays_the_animation() {
        let mut cache = cache(-1);
        assert_eq!(frame(&mut cache, 10, 17), Some(2));
        assert_eq!(frame(&mut cache, 17, 17), Some(0));
        assert!(!cache.track_finished((2, 1)));
    }

    #[test]
    fn track_loops_a_replaying_sequence() {
        let mut cache = cache(3);
        assert_eq!(frame(&mut cache, 0, 20), Some(0));
        assert_eq!(frame(&mut cache, 0, 21), Some(0));
        assert_eq!(frame(&mut cache, 0, 23), Some(1));
        assert!(!cache.track_finished((2, 1)));
    }

    #[test]
    fn an_unknown_start_counts_from_the_first_observation() {
        let mut cache = cache(-1);
        assert_eq!(frame(&mut cache, 0, 500), Some(0));
        assert_eq!(frame(&mut cache, 0, 503), Some(1));
    }
}
