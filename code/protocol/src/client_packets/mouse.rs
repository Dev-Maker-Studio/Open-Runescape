use super::menu::ClientBody;
use super::writer::Writer;
use crate::registry::ClientPacket;
use std::collections::VecDeque;

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MouseRecord {
    pub x: i32,
    pub y: i32,
    pub time: i64,
    pub click_meta: i32,
    pub constant: i32,
}

pub fn click_type_from_meta(meta: i32) -> i32 {
    match meta {
        160 | 512 => -1,
        161 | 163 | 513 | 515 => 0,
        162 | 514 => 3,
        164 | 166 | 516 | 518 => 2,
        165 | 517 => 5,
        167 | 169 | 519 | 521 => 1,
        168 | 520 => 4,
        170 | 522 => 6,
        _ => -2,
    }
}

impl MouseRecord {
    pub fn click_type(&self) -> i32 {
        click_type_from_meta(self.click_meta)
    }
}

const STALE_MILLIS: i64 = 2000;

#[derive(Clone, Debug)]
struct Batch {
    last_server_submission: i64,
    last_update: i64,
    last_x: i32,
    last_y: i32,
    pending: VecDeque<MouseRecord>,
    with_constant: bool,
}

impl Batch {
    fn new(with_constant: bool) -> Self {
        Self {
            last_server_submission: -1,
            last_update: -1,
            last_x: -1,
            last_y: -1,
            pending: VecDeque::new(),
            with_constant,
        }
    }

    fn reset(&mut self) {
        self.pending.clear();
        self.last_server_submission = -1;
        self.last_update = -1;
        self.last_x = -1;
        self.last_y = -1;
    }

    fn submission_delta(&mut self, time: i64, delta: i32) -> i32 {
        let value = if self.last_server_submission == -1 {
            i64::from(delta)
        } else {
            (time - self.last_server_submission).min(i64::from(delta))
        };
        self.last_server_submission = time;
        value as i32
    }

    fn submit_all(&mut self) -> Option<Vec<u8>> {
        let limit = 252 - (6 + usize::from(self.with_constant));
        let mut w: Option<Writer> = None;
        let mut start = 0usize;
        let mut remainder = 0i64;
        let mut count = 0i64;
        while let Some(record) = self.pending.front().copied() {
            if let Some(writer) = &w {
                if writer.len() - start >= limit {
                    break;
                }
            }
            self.pending.pop_front();
            let y = record.y.clamp(-1, 65534);
            let x = record.x.clamp(-1, 65534);
            if x == self.last_x && y == self.last_y {
                continue;
            }
            if w.is_none() {
                let mut writer = Writer::new();
                start = writer.len();
                writer.byte(0).byte(0);
                w = Some(writer);
                remainder = 0;
                count = 0;
            }
            let writer = w.as_mut().unwrap();
            let (dx, dy, cycle);
            if self.last_update != -1 {
                dx = x - self.last_x;
                dy = y - self.last_y;
                cycle = ((record.time - self.last_update) / 20) as i32;
                remainder += (record.time - self.last_update) % 20;
            } else {
                dx = x;
                dy = y;
                cycle = i32::MAX;
            }
            self.last_x = x;
            self.last_y = y;
            let packed = if x != -1 && y != -1 {
                x | (y << 16)
            } else {
                i32::MIN
            };
            if cycle < 8 && (-32..=31).contains(&dx) && (-32..=31).contains(&dy) {
                writer.short((cycle << 12) + (dy + 32) + ((dx + 32) << 6));
            } else if cycle < 32 && (-128..=127).contains(&dx) && (-128..=127).contains(&dy) {
                writer.byte(cycle + 128);
                writer.short((dy + 128) + ((dx + 128) << 8));
            } else if cycle < 32 {
                writer.byte(cycle + 192);
                writer.int(packed);
            } else {
                writer.short((cycle & 0x1fff) + 57344);
                writer.int(packed);
            }
            count += 1;
            if self.with_constant {
                writer.byte(record.constant);
            }
            self.last_update = record.time;
        }
        let mut writer = w?;
        let mut bytes = std::mem::take(&mut writer.0);
        bytes[start] = (remainder / count) as u8;
        bytes[start + 1] = (remainder % count) as u8;
        Some(bytes)
    }
}

#[derive(Clone, Debug)]
pub struct MouseSubmitter {
    click: Batch,
    movement: Batch,
    click_queue: VecDeque<MouseRecord>,
    pub last_key_submitted: i64,
    pub applet_has_focus: bool,
    pub camera_cooldown: i32,
}

impl Default for MouseSubmitter {
    fn default() -> Self {
        Self::new()
    }
}

impl MouseSubmitter {
    pub fn new() -> Self {
        Self {
            click: Batch::new(true),
            movement: Batch::new(false),
            click_queue: VecDeque::new(),
            last_key_submitted: -1,
            applet_has_focus: true,
            camera_cooldown: 0,
        }
    }

    pub fn reset(&mut self) {
        self.click.reset();
        self.movement.reset();
        self.last_key_submitted = -1;
        self.applet_has_focus = true;
    }

    pub fn append_click_batch(&mut self, record: MouseRecord) {
        self.click.pending.push_back(record);
    }

    pub fn append_movement_batch(&mut self, record: MouseRecord) {
        self.movement.pending.push_back(record);
    }

    pub fn queue_click(&mut self, record: MouseRecord) {
        self.click_queue.push_back(record);
        if self.click_queue.len() > 10 {
            self.click_queue.pop_front();
        }
    }

    pub fn submit_mouse(
        &mut self,
        now: i64,
        movement_head: Option<MouseRecord>,
    ) -> Vec<ClientBody> {
        let mut out = Vec::new();
        if movement_head.is_some() || self.movement.last_update < now - STALE_MILLIS {
            if let Some(body) = self.movement.submit_all() {
                out.push(batch_body(ClientPacket::MOVE_MOUSE_2, body));
            }
        }
        if let Some(record) = movement_head {
            let delta = self.movement.submission_delta(record.time, 32767);
            let y = record.y.clamp(0, 65535);
            let x = record.x.clamp(0, 65535);
            let click_type = i32::from(record.click_type() == 2);
            let mut w = Writer::new();
            w.int_little(x | (y << 16)).short(delta | (click_type << 15));
            out.push(ClientBody {
                packet: ClientPacket::MOUSE_CLICK,
                body: w.into_bytes(),
            });
        }
        if !self.click_queue.is_empty() || self.click.last_update < now - STALE_MILLIS {
            if let Some(body) = self.click.submit_all() {
                out.push(batch_body(ClientPacket::MOVE_MOUSE, body));
            }
        }
        if let Some(record) = self.click_queue.pop_front() {
            let delta = self.click.submission_delta(record.time, 0xFFFF);
            let mut w = Writer::new();
            w.int_little(record.x | (record.y << 16))
                .byte_add((record.click_meta << 1) | (record.constant & 1))
                .short_little(delta);
            out.push(ClientBody {
                packet: ClientPacket::MOUSE_BUTTON_CLICK,
                body: w.into_bytes(),
            });
        }
        out
    }

    pub fn submit_queued(
        &mut self,
        now: i64,
        movement_head: Option<MouseRecord>,
        held_keys: &[KeyPress],
        camera: &mut Option<(f32, f32)>,
        focus: bool,
    ) -> Vec<ClientBody> {
        let mut out = self.submit_mouse(now, movement_head);
        if !held_keys.is_empty() {
            let mut w = Writer::new();
            for key in held_keys {
                let delta = (key.time - self.last_key_submitted).min(16_777_215);
                self.last_key_submitted = key.time;
                w.byte(key.code).medium(delta as i32);
            }
            out.push(ClientBody {
                packet: ClientPacket::KEY_PRESS,
                body: w.into_bytes(),
            });
        }
        if self.camera_cooldown > 0 {
            self.camera_cooldown -= 1;
        }
        if self.camera_cooldown <= 0 {
            if let Some((x, y)) = camera.take() {
                self.camera_cooldown = 20;
                out.push(ClientBody {
                    packet: ClientPacket::MOVE_CAMERA,
                    body: super::bodies::move_camera(x, y),
                });
            }
        }
        if self.applet_has_focus != focus {
            self.applet_has_focus = focus;
            out.push(ClientBody {
                packet: ClientPacket::CLIENT_FOCUS,
                body: super::bodies::flag(focus),
            });
        }
        out
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct KeyPress {
    pub code: i32,
    pub time: i64,
}

fn batch_body(packet: ClientPacket, body: Vec<u8>) -> ClientBody {
    ClientBody { packet, body }
}
