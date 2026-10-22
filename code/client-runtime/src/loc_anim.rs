//! Port of `AnimatedGameObject`'s sequence state: which sequence an animated or transform loc
//! plays, when it restarts and which frame it is on. The geometry half lives in `world_draw`.
//!
//! Kotlin reads `Client.cycles` and the player's var provider; here both come in as arguments
//! (the cycle as `cycle`, the var state already folded into `Resolution`).

use openrs_entity::anim::{SeqType, Sequence};
use openrs_scene::loc_type::LocType;

/// Largest cycle gap replayed one cycle at a time; a bigger gap is one `progressCycle` call.
const MAX_CATCH_UP: u64 = 2000;

#[allow(clippy::large_enum_variant)]
pub enum SeqLookup {
    Pending,
    Absent,
    Type(SeqType),
}

/// A sequence config the store has not delivered yet; the caller drops the state and retries.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Pending;

/// `loctypelist.list(locId)` and its `getMultiLoc` result for the current var state.
#[derive(Clone, Copy)]
pub struct Resolution<'a> {
    pub original: &'a LocType,
    pub computed: Option<&'a LocType>,
}

pub struct LocAnim {
    has_sequence_id: bool,
    pub sequence: Sequence,
    pub ty: Option<SeqType>,
    current_cycle: u64,
    game_object_id: i32,
    pub applied: Option<u32>,
    rng: u64,
}

/// `ObjectType.getRandomAnimation` over a uniform sample in [0, 1).
pub fn random_animation(loc: &LocType, sample: f64) -> i32 {
    let Some(animations) = loc.animations.as_ref() else {
        return -1;
    };
    if animations.len() <= 1 {
        return animations.first().copied().unwrap_or(-1);
    }
    let mut roll = (sample * 65535.0) as i32;
    let Some(odds) = loc.animation_odds.as_ref() else {
        return -1;
    };
    for (index, animation) in animations.iter().enumerate() {
        let odd = odds.get(index).copied().unwrap_or(0);
        if roll <= odd {
            return *animation;
        }
        roll = roll.wrapping_sub(odd);
    }
    -1
}

fn has_sequence(loc: &LocType, id: i32) -> bool {
    id != -1
        && loc
            .animations
            .as_ref()
            .is_some_and(|animations| animations.contains(&id))
}

impl LocAnim {
    /// `AnimatedGameObject.<init>` with `sequenceId`.
    pub fn new(
        seed: u64,
        sequence_id: i32,
        cycle: u64,
        res: Resolution<'_>,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<Self, Pending> {
        let mut state = Self {
            has_sequence_id: sequence_id != -1,
            sequence: Sequence::new(false),
            ty: None,
            current_cycle: cycle,
            game_object_id: -1,
            applied: None,
            rng: seed | 1,
        };
        state.animate(false, sequence_id, 1, res, cycle, lookup)?;
        Ok(state)
    }

    pub fn has_type(&self) -> bool {
        self.ty.is_some()
    }

    fn random(&mut self) -> f64 {
        self.rng ^= self.rng << 13;
        self.rng ^= self.rng >> 7;
        self.rng ^= self.rng << 17;
        (self.rng >> 11) as f64 / (1u64 << 53) as f64
    }

    fn reset_sequence(&mut self) {
        let _ = self.sequence.animate(-1, 0, 0, false, &|_| None);
        self.ty = None;
    }

    /// `animateSequence(seq, 0, flags, replay)`; an unusable sequence leaves the loc still.
    fn start_sequence(
        &mut self,
        seq: i32,
        flags: i32,
        replay: bool,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<(), Pending> {
        if seq == self.sequence.seq_id {
            return Ok(());
        }
        let ty = match lookup(seq) {
            SeqLookup::Pending => return Err(Pending),
            SeqLookup::Absent => {
                self.reset_sequence();
                return Ok(());
            }
            SeqLookup::Type(ty) => ty,
        };
        let frames = ty.frames.as_ref().map_or(0, Vec::len);
        let durations = ty.durations.as_ref().map_or(0, Vec::len);
        if frames == 0 || durations < frames {
            self.reset_sequence();
            return Ok(());
        }
        let source = ty.clone();
        if self
            .sequence
            .animate(seq, 0, flags, false, &move |_| Some(source.clone()))
            .is_err()
        {
            self.reset_sequence();
            return Ok(());
        }
        if replay {
            let start = (self.random() * frames as f64) as usize;
            let start = start.min(frames - 1);
            let duration = ty.durations.as_ref().map_or(0, |d| d[start]);
            self.sequence.current_frame_id = start as i32;
            self.sequence.current_frame_duration = (self.random() * f64::from(duration)) as i32;
            self.sequence.next_frame_id = start as i32 + 1;
            if self.sequence.next_frame_id >= frames as i32 {
                self.sequence.next_frame_id = -1;
            }
            self.sequence.reset_masks();
        }
        self.ty = Some(ty);
        Ok(())
    }

    /// `AnimatedGameObject.animate(isTransformed, seqId, animFlags)`.
    pub fn animate(
        &mut self,
        transformed: bool,
        seq_id: i32,
        flags: i32,
        res: Resolution<'_>,
        cycle: u64,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<(), Pending> {
        let mut flags = flags;
        let mut seq = seq_id;
        let mut replay = false;
        if seq_id == -1 {
            let Some(computed) = res.computed else {
                return Ok(());
            };
            let original = (res.original.id != computed.id).then_some(res.original);
            if computed.animations.is_some() {
                if transformed && self.has_type() && has_sequence(computed, self.sequence.seq_id) {
                    return Ok(());
                }
                if computed.id != self.game_object_id {
                    replay = computed.replay_sequence;
                }
                let sample = self.random();
                seq = random_animation(computed, sample);
                flags = if computed.animations.as_ref().is_some_and(|a| a.len() > 1) {
                    0
                } else {
                    1
                };
            } else if let Some(original) = original.filter(|loc| loc.animations.is_some()) {
                if transformed && self.has_type() && has_sequence(original, self.sequence.seq_id) {
                    return Ok(());
                }
                if self.game_object_id != computed.id {
                    replay = original.replay_sequence;
                }
                let sample = self.random();
                seq = random_animation(original, sample);
                flags = if original.animations.as_ref().is_some_and(|a| a.len() > 1) {
                    0
                } else {
                    1
                };
            }
        }
        if seq == -1 {
            self.reset_sequence();
        } else {
            self.start_sequence(seq, flags, replay, lookup)?;
            self.current_cycle = cycle;
        }
        Ok(())
    }

    /// The public `animate(seq)` a server `ObjAnim` calls on a live loc.
    pub fn play(
        &mut self,
        seq: i32,
        res: Resolution<'_>,
        cycle: u64,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<(), Pending> {
        self.has_sequence_id = true;
        self.animate(false, seq, 1, res, cycle, lookup)
    }

    fn step(
        &mut self,
        duration: i32,
        res: Resolution<'_>,
        now: u64,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<(), Pending> {
        let Some(ty) = self.ty.clone() else {
            return self.animate(false, -1, 0, res, now, lookup);
        };
        match self.sequence.progress_cycle(&ty, duration) {
            Ok(true) if self.sequence.reset_animation => {
                self.reset_sequence();
                self.has_sequence_id = false;
                self.animate(false, -1, 0, res, now, lookup)
            }
            Ok(_) => Ok(()),
            Err(_) => {
                self.reset_sequence();
                Ok(())
            }
        }
    }

    /// `progressAnimation`, replayed cycle by cycle so a restart lands where the client's would.
    pub fn progress(
        &mut self,
        res: Resolution<'_>,
        cycle: u64,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<(), Pending> {
        if cycle < self.current_cycle {
            self.current_cycle = cycle;
        }
        let start = self.current_cycle;
        let gap = cycle - start;
        if gap == 0 {
            if !self.has_type() {
                self.animate(false, -1, 0, res, cycle, lookup)?;
            }
        } else if gap <= MAX_CATCH_UP {
            for now in start + 1..=cycle {
                self.step(1, res, now, lookup)?;
            }
        } else {
            self.step(i32::try_from(gap).unwrap_or(i32::MAX), res, cycle, lookup)?;
        }
        self.current_cycle = cycle;
        Ok(())
    }

    /// The var-dependent half of `getAnimatedModel`; false when the transform hides the loc.
    pub fn advance(
        &mut self,
        res: Resolution<'_>,
        cycle: u64,
        lookup: &mut dyn FnMut(i32) -> SeqLookup,
    ) -> Result<bool, Pending> {
        let Some(computed) = res.computed else {
            self.game_object_id = -1;
            return Ok(false);
        };
        if !self.has_sequence_id && self.game_object_id != computed.id {
            self.animate(true, -1, 0, res, cycle, lookup)?;
        }
        self.progress(res, cycle, lookup)?;
        self.game_object_id = computed.id;
        Ok(true)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_scene::loc_type::decode_loc_type;

    fn loc(id: i32, animations: Option<Vec<i32>>, odds: Option<Vec<i32>>) -> LocType {
        let mut loc = decode_loc_type(id, None, true).expect("defaults");
        loc.animations = animations;
        loc.animation_odds = odds;
        loc
    }

    fn sequence(id: i32, durations: Vec<i32>, replay_interval: i32) -> SeqType {
        let mut ty = SeqType::new(id);
        ty.frames = Some((0..durations.len() as i32).collect());
        ty.durations = Some(durations);
        ty.replay_interval = replay_interval;
        ty
    }

    fn lookup(id: i32) -> SeqLookup {
        match id {
            7 => SeqLookup::Type(sequence(7, vec![2, 2, 2], 3)),
            8 => SeqLookup::Type(sequence(8, vec![2, 2], -1)),
            9 => SeqLookup::Pending,
            _ => SeqLookup::Absent,
        }
    }

    fn plain(loc: &LocType) -> Resolution<'_> {
        Resolution {
            original: loc,
            computed: Some(loc),
        }
    }

    #[test]
    fn random_animation_walks_the_odds_like_the_client() {
        let two = loc(1, Some(vec![10, 20]), Some(vec![30000, 35535]));
        assert_eq!(random_animation(&two, 0.0), 10);
        assert_eq!(random_animation(&two, 0.45), 10);
        assert_eq!(random_animation(&two, 0.5), 20);
        assert_eq!(random_animation(&two, 0.999), 20);
        let one = loc(2, Some(vec![5]), None);
        assert_eq!(random_animation(&one, 0.7), 5);
        assert_eq!(random_animation(&loc(3, None, None), 0.7), -1);
    }

    #[test]
    fn an_animated_loc_picks_its_sequence_and_loops() {
        let mut animated = loc(1, Some(vec![7]), None);
        animated.replay_sequence = false;
        let mut state = LocAnim::new(1, -1, 100, plain(&animated), &mut lookup).unwrap();
        assert_eq!(state.sequence.seq_id, 7);
        assert!(state.advance(plain(&animated), 100, &mut lookup).unwrap());
        assert_eq!(state.sequence.current_frame_id, 0);
        state.advance(plain(&animated), 103, &mut lookup).unwrap();
        assert_eq!(state.sequence.current_frame_id, 1);
        state.advance(plain(&animated), 120, &mut lookup).unwrap();
        assert!(state.has_type());
        assert!(!state.sequence.reset_animation);
    }

    #[test]
    fn a_replaying_loc_starts_on_a_random_frame() {
        let animated = loc(1, Some(vec![7]), None);
        let mut starts = std::collections::HashSet::new();
        for seed in 1..40 {
            let state = LocAnim::new(seed * 7919, -1, 0, plain(&animated), &mut lookup).unwrap();
            assert!((0..3).contains(&state.sequence.current_frame_id));
            assert!(state.sequence.current_frame_duration < 2);
            starts.insert(state.sequence.current_frame_id);
        }
        assert!(starts.len() > 1);
    }

    #[test]
    fn a_finished_server_animation_returns_to_the_resting_loc() {
        let resting = loc(1, None, None);
        let mut state = LocAnim::new(1, -1, 0, plain(&resting), &mut lookup).unwrap();
        assert!(!state.has_type());
        state.play(8, plain(&resting), 0, &mut lookup).unwrap();
        assert_eq!(state.sequence.seq_id, 8);
        state.advance(plain(&resting), 3, &mut lookup).unwrap();
        assert_eq!(state.sequence.seq_id, 8);
        state.advance(plain(&resting), 40, &mut lookup).unwrap();
        assert!(!state.has_type());
        assert_eq!(state.sequence.seq_id, -1);
    }

    #[test]
    fn a_transform_that_hides_the_loc_reports_hidden() {
        let original = loc(1, None, None);
        let res = Resolution {
            original: &original,
            computed: None,
        };
        let mut state = LocAnim::new(1, -1, 0, res, &mut lookup).unwrap();
        assert!(!state.advance(res, 5, &mut lookup).unwrap());
    }

    #[test]
    fn a_transform_into_an_animated_loc_starts_its_sequence() {
        let original = loc(1, None, None);
        let target = loc(2, Some(vec![7]), None);
        let hidden = Resolution {
            original: &original,
            computed: None,
        };
        let mut state = LocAnim::new(1, -1, 0, hidden, &mut lookup).unwrap();
        assert!(!state.has_type());
        let shown = Resolution {
            original: &original,
            computed: Some(&target),
        };
        assert!(state.advance(shown, 1, &mut lookup).unwrap());
        assert_eq!(state.sequence.seq_id, 7);
    }

    #[test]
    fn an_original_with_animations_keeps_playing_through_a_plain_transform() {
        let original = loc(1, Some(vec![7]), None);
        let target = loc(2, None, None);
        let res = Resolution {
            original: &original,
            computed: Some(&target),
        };
        let state = LocAnim::new(1, -1, 0, res, &mut lookup).unwrap();
        assert_eq!(state.sequence.seq_id, 7);
    }

    #[test]
    fn a_missing_sequence_config_is_pending_not_lost() {
        let animated = loc(1, Some(vec![9]), None);
        assert!(LocAnim::new(1, -1, 0, plain(&animated), &mut lookup).is_err());
        let still = loc(2, Some(vec![99]), None);
        let state = LocAnim::new(1, -1, 0, plain(&still), &mut lookup).unwrap();
        assert!(!state.has_type());
    }

    #[test]
    fn a_long_gap_is_one_progress_call() {
        let animated = loc(1, Some(vec![7]), None);
        let mut state = LocAnim::new(1, -1, 0, plain(&animated), &mut lookup).unwrap();
        state.advance(plain(&animated), 50_000, &mut lookup).unwrap();
        assert!(state.has_type());
    }
}
