use crate::Error;

const SIZE: usize = 256;
const GOLDEN_RATIO: u32 = 0x9e3779b9;

pub struct Isaac {
    results: [u32; SIZE],
    memory: [u32; SIZE],
    remaining: usize,
    accumulator: u32,
    last_result: u32,
    counter: u32,
}

impl Isaac {
    pub fn new(seed: &[u32]) -> Result<Self, Error> {
        if seed.len() > SIZE {
            return Err(Error::InvalidSeedLength);
        }
        let mut cipher = Self {
            results: [0; SIZE],
            memory: [0; SIZE],
            remaining: 0,
            accumulator: 0,
            last_result: 0,
            counter: 0,
        };
        cipher.results[..seed.len()].copy_from_slice(seed);
        let mut state = [GOLDEN_RATIO; 8];
        for _ in 0..4 {
            state = mix(state);
        }
        for offset in (0..SIZE).step_by(8) {
            for (index, word) in state.iter_mut().enumerate() {
                *word = word.wrapping_add(cipher.results[offset + index]);
            }
            state = mix(state);
            cipher.memory[offset..offset + 8].copy_from_slice(&state);
        }
        for offset in (0..SIZE).step_by(8) {
            for (index, word) in state.iter_mut().enumerate() {
                *word = word.wrapping_add(cipher.memory[offset + index]);
            }
            state = mix(state);
            cipher.memory[offset..offset + 8].copy_from_slice(&state);
        }
        cipher.generate();
        cipher.remaining = SIZE;
        Ok(cipher)
    }

    pub fn next_u32(&mut self) -> u32 {
        if self.remaining == 0 {
            self.generate();
            self.remaining = SIZE;
        }
        self.remaining -= 1;
        self.results[self.remaining]
    }

    pub fn peek_u32(&mut self) -> u32 {
        if self.remaining == 0 {
            self.generate();
            self.remaining = SIZE;
        }
        self.results[self.remaining - 1]
    }

    fn generate(&mut self) {
        self.counter = self.counter.wrapping_add(1);
        self.last_result = self.last_result.wrapping_add(self.counter);
        for index in 0..SIZE {
            let x = self.memory[index];
            self.accumulator ^= match index & 3 {
                0 => self.accumulator << 13,
                1 => self.accumulator >> 6,
                2 => self.accumulator << 2,
                _ => self.accumulator >> 16,
            };
            self.accumulator = self
                .accumulator
                .wrapping_add(self.memory[(index + 128) & 255]);
            let y = self.memory[((x >> 2) & 255) as usize]
                .wrapping_add(self.accumulator)
                .wrapping_add(self.last_result);
            self.memory[index] = y;
            self.last_result = self.memory[((y >> 10) & 255) as usize].wrapping_add(x);
            self.results[index] = self.last_result;
        }
    }
}

fn mix(state: [u32; 8]) -> [u32; 8] {
    let [mut a, mut b, mut c, mut d, mut e, mut f, mut g, mut h] = state;
    a ^= b << 11;
    d = d.wrapping_add(a);
    b = b.wrapping_add(c);
    b ^= c >> 2;
    e = e.wrapping_add(b);
    c = c.wrapping_add(d);
    c ^= d << 8;
    f = f.wrapping_add(c);
    d = d.wrapping_add(e);
    d ^= e >> 16;
    g = g.wrapping_add(d);
    e = e.wrapping_add(f);
    e ^= f << 10;
    h = h.wrapping_add(e);
    f = f.wrapping_add(g);
    f ^= g >> 4;
    a = a.wrapping_add(f);
    g = g.wrapping_add(h);
    g ^= h << 8;
    b = b.wrapping_add(g);
    h = h.wrapping_add(a);
    h ^= a >> 9;
    c = c.wrapping_add(h);
    a = a.wrapping_add(b);
    [a, b, c, d, e, f, g, h]
}
