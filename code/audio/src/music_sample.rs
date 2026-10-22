use crate::audio_file::AudioBuffer;
use crate::reader::{at, put, AudioError, Buf, R};

const RANGE_LIST: [i32; 4] = [256, 128, 86, 64];
const MAX_FLOATS: i64 = 1 << 27;
const MAX_SAMPLES: i64 = 1 << 28;

#[rustfmt::skip]
const INVERSE_DB_TABLE: [f32; 256] = [
    1.0649863E-7, 1.1341951E-7, 1.2079015E-7, 1.2863978E-7, 1.369995E-7, 1.459025E-7, 1.5538409E-7, 1.6548181E-7,
    1.7623574E-7, 1.8768856E-7, 1.998856E-7, 2.128753E-7, 2.2670913E-7, 2.4144197E-7, 2.5713223E-7, 2.7384212E-7,
    2.9163792E-7, 3.1059022E-7, 3.307741E-7, 3.5226967E-7, 3.7516213E-7, 3.995423E-7, 4.255068E-7, 4.5315863E-7,
    4.8260745E-7, 5.1397E-7, 5.4737063E-7, 5.829419E-7, 6.208247E-7, 6.611694E-7, 7.041359E-7, 7.4989464E-7,
    7.98627E-7, 8.505263E-7, 9.057983E-7, 9.646621E-7, 1.0273513E-6, 1.0941144E-6, 1.1652161E-6, 1.2409384E-6,
    1.3215816E-6, 1.4074654E-6, 1.4989305E-6, 1.5963394E-6, 1.7000785E-6, 1.8105592E-6, 1.9282195E-6, 2.053526E-6,
    2.1869757E-6, 2.3290977E-6, 2.4804558E-6, 2.6416496E-6, 2.813319E-6, 2.9961443E-6, 3.1908505E-6, 3.39821E-6,
    3.619045E-6, 3.8542307E-6, 4.1047006E-6, 4.371447E-6, 4.6555283E-6, 4.958071E-6, 5.280274E-6, 5.623416E-6,
    5.988857E-6, 6.3780467E-6, 6.7925284E-6, 7.2339453E-6, 7.704048E-6, 8.2047E-6, 8.737888E-6, 9.305725E-6,
    9.910464E-6, 1.0554501E-5, 1.1240392E-5, 1.1970856E-5, 1.2748789E-5, 1.3577278E-5, 1.4459606E-5, 1.5399271E-5,
    1.6400005E-5, 1.7465769E-5, 1.8600793E-5, 1.9809577E-5, 2.1096914E-5, 2.2467912E-5, 2.3928002E-5, 2.5482977E-5,
    2.7139005E-5, 2.890265E-5, 3.078091E-5, 3.2781227E-5, 3.4911533E-5, 3.718028E-5, 3.9596467E-5, 4.2169668E-5,
    4.491009E-5, 4.7828602E-5, 5.0936775E-5, 5.424693E-5, 5.7772202E-5, 6.152657E-5, 6.552491E-5, 6.9783084E-5,
    7.4317984E-5, 7.914758E-5, 8.429104E-5, 8.976875E-5, 9.560242E-5, 1.0181521E-4, 1.0843174E-4, 1.1547824E-4,
    1.2298267E-4, 1.3097477E-4, 1.3948625E-4, 1.4855085E-4, 1.5820454E-4, 1.6848555E-4, 1.7943469E-4, 1.9109536E-4,
    2.0351382E-4, 2.167393E-4, 2.3082423E-4, 2.4582449E-4, 2.6179955E-4, 2.7881275E-4, 2.9693157E-4, 3.1622787E-4,
    3.3677815E-4, 3.5866388E-4, 3.8197188E-4, 4.0679457E-4, 4.3323037E-4, 4.613841E-4, 4.913675E-4, 5.2329927E-4,
    5.573062E-4, 5.935231E-4, 6.320936E-4, 6.731706E-4, 7.16917E-4, 7.635063E-4, 8.1312325E-4, 8.6596457E-4,
    9.2223985E-4, 9.821722E-4, 0.0010459992, 0.0011139743, 0.0011863665, 0.0012634633, 0.0013455702, 0.0014330129,
    0.0015261382, 0.0016253153, 0.0017309374, 0.0018434235, 0.0019632196, 0.0020908006, 0.0022266726, 0.0023713743,
    0.0025254795, 0.0026895993, 0.0028643848, 0.0030505287, 0.003248769, 0.0034598925, 0.0036847359, 0.0039241905,
    0.0041792067, 0.004450795, 0.004740033, 0.005048067, 0.0053761187, 0.005725489, 0.0060975635, 0.0064938175,
    0.0069158226, 0.0073652514, 0.007843887, 0.008353627, 0.008896492, 0.009474637, 0.010090352, 0.01074608,
    0.011444421, 0.012188144, 0.012980198, 0.013823725, 0.014722068, 0.015678791, 0.016697686, 0.017782796,
    0.018938422, 0.020169148, 0.021479854, 0.022875736, 0.02436233, 0.025945531, 0.027631618, 0.029427277,
    0.031339627, 0.03337625, 0.035545226, 0.037855156, 0.0403152, 0.042935107, 0.045725275, 0.048696756,
    0.05186135, 0.05523159, 0.05882085, 0.062643364, 0.06671428, 0.07104975, 0.075666964, 0.08058423,
    0.08582105, 0.09139818, 0.097337745, 0.1036633, 0.11039993, 0.11757434, 0.12521498, 0.13335215,
    0.14201812, 0.15124726, 0.16107617, 0.1715438, 0.18269168, 0.19456401, 0.20720787, 0.22067343,
    0.23501402, 0.25028655, 0.26655158, 0.28387362, 0.3023213, 0.32196787, 0.34289113, 0.36517414,
    0.3889052, 0.41417846, 0.44109413, 0.4697589, 0.50028646, 0.53279793, 0.5674221, 0.6042964,
    0.64356697, 0.6853896, 0.72993004, 0.777365, 0.8278826, 0.88168305, 0.9389798, 1.0,
];

pub fn ilog(value: i32) -> i32 {
    let mut num = value as u32;
    let mut shift = 0i32;
    if !(0..65536).contains(&value) {
        num >>= 16;
        shift += 16;
    }
    if num >= 256 {
        num >>= 8;
        shift += 8;
    }
    if num >= 16 {
        num >>= 4;
        shift += 4;
    }
    if num >= 4 {
        num >>= 2;
        shift += 2;
    }
    if num >= 1 {
        num >>= 1;
        shift += 1;
    }
    num as i32 + shift
}

fn idiv(a: i32, b: i32) -> R<i32> {
    if b == 0 {
        Err(AudioError::Invalid)
    } else {
        Ok(a.wrapping_div(b))
    }
}

fn irem(a: i32, b: i32) -> R<i32> {
    if b == 0 {
        Err(AudioError::Invalid)
    } else {
        Ok(a.wrapping_rem(b))
    }
}

fn alloc<T: Clone>(len: i64, fill: T, cap: i64) -> R<Vec<T>> {
    if len < 0 || len > cap {
        return Err(AudioError::Invalid);
    }
    Ok(vec![fill; len as usize])
}

fn power(base: i32, exponent: i32) -> i32 {
    let mut base = base;
    let mut exp = exponent;
    let mut result = 1i32;
    while exp > 1 {
        if exp & 1 != 0 {
            result = base.wrapping_mul(result);
        }
        base = base.wrapping_mul(base);
        exp >>= 1;
    }
    if exp == 1 {
        base.wrapping_mul(result)
    } else {
        result
    }
}

fn float32_unpack(input: i32) -> f32 {
    let mut mantissa = input & 0x1f_ffff;
    let sign = input & i32::MIN;
    let exponent = (input & 0x7fe0_0000) >> 21;
    if sign != 0 {
        mantissa = mantissa.wrapping_neg();
    }
    (f64::from(mantissa) * libm::pow(2.0, f64::from(exponent - 788))) as f32
}

fn lookup1_values(entries: i32, dim: i32) -> R<i32> {
    if entries == 0 && dim == 0 {
        return Err(AudioError::Invalid);
    }
    let mut r = (libm::pow(f64::from(entries), 1.0 / f64::from(dim)) as i32).wrapping_add(1);
    while power(r, dim) > entries {
        r = r.wrapping_sub(1);
    }
    Ok(r)
}

fn reverse_bits(input: i32, count: i32) -> i32 {
    let mut value = input as u32;
    let mut bits = count;
    let mut reversed = 0i32;
    while bits > 0 {
        reversed = (reversed << 1) | (value & 1) as i32;
        value >>= 1;
        bits -= 1;
    }
    reversed
}

pub struct Bits<'a> {
    data: &'a [u8],
    byte: i64,
    root: i32,
}

impl<'a> Bits<'a> {
    pub fn new(data: &'a [u8]) -> Self {
        Self {
            data,
            byte: 0,
            root: 0,
        }
    }

    fn current(&self) -> R<i32> {
        Ok(i32::from(at(self.data, self.byte)?))
    }

    pub fn bit(&mut self) -> R<i32> {
        let bit = (self.current()? >> self.root) & 1;
        self.root += 1;
        self.byte += i64::from(self.root >> 3);
        self.root &= 7;
        Ok(bit)
    }

    pub fn bits(&mut self, count: i32) -> R<i32> {
        let mut remaining = count;
        let mut res = 0i32;
        let mut index = 0u32;
        while remaining >= 8 - self.root {
            let chunk = 8 - self.root;
            let mask = (1i32 << chunk) - 1;
            res = res.wrapping_add(((self.current()? >> self.root) & mask).wrapping_shl(index));
            self.root = 0;
            self.byte += 1;
            index = index.wrapping_add(chunk as u32);
            remaining -= chunk;
        }
        if remaining > 0 {
            let mask = (1i32 << remaining) - 1;
            res = res.wrapping_add(((self.current()? >> self.root) & mask).wrapping_shl(index));
            self.root += remaining;
        }
        Ok(res)
    }
}

#[derive(Debug, Clone)]
pub struct Codebook {
    pub dimensions: i32,
    pub entries: i32,
    pub lengths: Vec<i32>,
    pub multiplicands: Option<Vec<f32>>,
    pub huffman_tree: Vec<i32>,
}

impl Codebook {
    pub fn new(bits: &mut Bits) -> R<Self> {
        bits.bits(24)?;
        let dimensions = bits.bits(16)?;
        let entries = bits.bits(24)?;
        let mut lengths = alloc(i64::from(entries), 0i32, 1 << 30)?;
        let ordered = bits.bit()? != 0;
        if ordered {
            let mut at_entry = 0i32;
            let mut current = bits.bits(5)? + 1;
            while at_entry < entries {
                let limit = bits.bits(ilog(entries - at_entry))?;
                for _ in 0..limit {
                    put(&mut lengths, i64::from(at_entry), current)?;
                    at_entry += 1;
                }
                current += 1;
            }
        } else {
            let present = bits.bit()? != 0;
            for slot in lengths.iter_mut() {
                if present && bits.bit()? == 0 {
                    *slot = 0;
                } else {
                    *slot = bits.bits(5)? + 1;
                }
            }
        }
        let huffman_tree = Self::huffman(&lengths)?;
        let lookup_type = bits.bits(4)?;
        let mut multiplicands = None;
        if lookup_type > 0 {
            let minimum = float32_unpack(bits.bits(32)?);
            let delta = float32_unpack(bits.bits(32)?);
            let value_bits = bits.bits(4)? + 1;
            let sequence = bits.bit()? != 0;
            let lookup_values = if lookup_type == 1 {
                lookup1_values(entries, dimensions)?
            } else {
                entries.wrapping_mul(dimensions)
            };
            let mut mults = alloc(i64::from(lookup_values), 0i32, 1 << 30)?;
            for slot in mults.iter_mut() {
                *slot = bits.bits(value_bits)?;
            }
            let total = i64::from(entries) * i64::from(dimensions);
            let mut table = alloc(total, 0.0f32, MAX_FLOATS)?;
            let dims = dimensions as usize;
            if lookup_type == 1 {
                for i in 0..entries {
                    let mut last = 0.0f32;
                    let mut div = 1i32;
                    for j in 0..dimensions {
                        let off = irem(idiv(i, div)?, lookup_values)?;
                        let value = at(&mults, i64::from(off))? as f32 * delta + minimum + last;
                        table[i as usize * dims + j as usize] = value;
                        if sequence {
                            last = value;
                        }
                        div = div.wrapping_mul(lookup_values);
                    }
                }
            } else {
                for i in 0..entries {
                    let mut last = 0.0f32;
                    let mut off = i.wrapping_mul(dimensions);
                    for j in 0..dimensions {
                        let value = at(&mults, i64::from(off))? as f32 * delta + minimum + last;
                        table[i as usize * dims + j as usize] = value;
                        if sequence {
                            last = value;
                        }
                        off = off.wrapping_add(1);
                    }
                }
            }
            multiplicands = Some(table);
        }
        Ok(Self {
            dimensions,
            entries,
            lengths,
            multiplicands,
            huffman_tree,
        })
    }

    fn huffman(lengths: &[i32]) -> R<Vec<i32>> {
        let entries = lengths.len();
        let mut codewords = vec![0i32; entries];
        let mut sorted = [0i32; 33];
        for entry in 0..entries {
            let code_length = lengths[entry];
            if code_length == 0 {
                continue;
            }
            let high_bit = 1i32.wrapping_shl((32 - code_length) as u32);
            let codeword = at(&sorted, i64::from(code_length))?;
            codewords[entry] = codeword;
            let next;
            if codeword & high_bit != 0 {
                next = at(&sorted, i64::from(code_length) - 1)?;
            } else {
                next = codeword | high_bit;
                let mut parent = code_length - 1;
                while parent >= 1 {
                    let parent_code = at(&sorted, i64::from(parent))?;
                    if parent_code != codeword {
                        break;
                    }
                    let mask = 1i32.wrapping_shl((32 - parent) as u32);
                    if parent_code & mask != 0 {
                        let lower = at(&sorted, i64::from(parent) - 1)?;
                        put(&mut sorted, i64::from(parent), lower)?;
                        break;
                    }
                    put(&mut sorted, i64::from(parent), parent_code | mask)?;
                    parent -= 1;
                }
            }
            put(&mut sorted, i64::from(code_length), next)?;
            for parent in (code_length + 1)..=32 {
                if at(&sorted, i64::from(parent))? == codeword {
                    put(&mut sorted, i64::from(parent), next)?;
                }
            }
        }
        let mut tree = vec![0i32; 8];
        let mut entry_idx = 0i32;
        for (index, &length) in lengths.iter().enumerate() {
            if length == 0 {
                continue;
            }
            let codeword = codewords[index];
            let mut next = 0i32;
            for parent in 0..length {
                let bit = (i32::MIN as u32 >> parent as u32) as i32;
                if codeword & bit != 0 {
                    if at(&tree, i64::from(next))? == 0 {
                        put(&mut tree, i64::from(next), entry_idx)?;
                    }
                    next = at(&tree, i64::from(next))?;
                } else {
                    next += 1;
                }
                if next >= 0 && next as usize >= tree.len() {
                    let mut grown = vec![0i32; tree.len() * 2];
                    grown[..tree.len()].copy_from_slice(&tree);
                    tree = grown;
                }
            }
            put(&mut tree, i64::from(next), !(index as i32))?;
            if next >= entry_idx {
                entry_idx = next + 1;
            }
        }
        Ok(tree)
    }

    pub fn decode(&self, bits: &mut Bits) -> R<i32> {
        let mut index = 0i32;
        loop {
            let node = at(&self.huffman_tree, i64::from(index))?;
            if node < 0 {
                return Ok(!node);
            }
            index = if bits.bit()? != 0 { node } else { index + 1 };
        }
    }

    pub fn decode_vector(&self, bits: &mut Bits) -> R<&[f32]> {
        let table = self.multiplicands.as_ref().ok_or(AudioError::Invalid)?;
        let entry = self.decode(bits)? as usize;
        let dims = self.dimensions as usize;
        table
            .get(entry * dims..entry * dims + dims)
            .ok_or(AudioError::Bounds)
    }
}

#[derive(Debug, Clone)]
pub struct Residue {
    pub residue_type: i32,
    pub begin: i32,
    pub end: i32,
    pub partition_size: i32,
    pub classifications: i32,
    pub classbook: i32,
    pub books: Vec<i32>,
}

impl Residue {
    pub fn new(bits: &mut Bits) -> R<Self> {
        let residue_type = bits.bits(16)?;
        let begin = bits.bits(24)?;
        let end = bits.bits(24)?;
        let partition_size = bits.bits(24)? + 1;
        let classifications = bits.bits(6)? + 1;
        let classbook = bits.bits(8)?;
        let mut cascade = vec![0i32; classifications as usize];
        for slot in cascade.iter_mut() {
            let mut high = 0;
            let low = bits.bits(3)?;
            if bits.bit()? != 0 {
                high = bits.bits(5)?;
            }
            *slot = (high << 3) | low;
        }
        let mut books = vec![0i32; classifications as usize * 8];
        for (k, slot) in books.iter_mut().enumerate() {
            *slot = if cascade[k >> 3] & (1 << (k & 7)) != 0 {
                bits.bits(8)?
            } else {
                -1
            };
        }
        Ok(Self {
            residue_type,
            begin,
            end,
            partition_size,
            classifications,
            classbook,
            books,
        })
    }

    pub fn decode(
        &self,
        bits: &mut Bits,
        codebooks: &[Codebook],
        output: &mut [f32],
        output_size: i64,
        skip: bool,
    ) -> R<()> {
        for i in 0..output_size {
            put(output, i, 0.0)?;
        }
        if skip {
            return Ok(());
        }
        let class_book = codebooks
            .get(self.classbook as usize)
            .ok_or(AudioError::Bounds)?;
        let classwords = class_book.dimensions;
        let window_size = self.end.wrapping_sub(self.begin);
        let windows = idiv(window_size, self.partition_size)?;
        let mut classification = alloc(i64::from(windows), 0i32, 1 << 30)?;
        if classwords == 0 && windows > 0 {
            return Err(AudioError::Invalid);
        }
        for pass in 0..8 {
            let mut pcount = 0i32;
            while pcount < windows {
                if pass == 0 {
                    let mut q = class_book.decode(bits)?;
                    let mut i = classwords - 1;
                    while i >= 0 {
                        if pcount + i < windows {
                            put(
                                &mut classification,
                                i64::from(pcount + i),
                                irem(q, self.classifications)?,
                            )?;
                        }
                        q = idiv(q, self.classifications)?;
                        i -= 1;
                    }
                }
                for _ in 0..classwords {
                    let c = at(&classification, i64::from(pcount))?;
                    let book_index = at(&self.books, i64::from(pass + c.wrapping_mul(8)))?;
                    if book_index >= 0 {
                        let offset = pcount
                            .wrapping_mul(self.partition_size)
                            .wrapping_add(self.begin);
                        let codebook = codebooks
                            .get(book_index as usize)
                            .ok_or(AudioError::Bounds)?;
                        if self.residue_type == 0 {
                            let step = idiv(self.partition_size, codebook.dimensions)?;
                            for index in 0..step {
                                let values = codebook.decode_vector(bits)?;
                                for dim in 0..codebook.dimensions {
                                    let slot = i64::from(offset)
                                        + i64::from(index)
                                        + i64::from(dim) * i64::from(step);
                                    let sum = at(output, slot)? + at(values, i64::from(dim))?;
                                    put(output, slot, sum)?;
                                }
                            }
                        } else {
                            if codebook.dimensions == 0 && self.partition_size > 0 {
                                return Err(AudioError::Invalid);
                            }
                            let mut pos = 0i32;
                            while pos < self.partition_size {
                                let values = codebook.decode_vector(bits)?;
                                for dim in 0..codebook.dimensions {
                                    let slot = i64::from(offset) + i64::from(pos);
                                    let sum = at(output, slot)? + at(values, i64::from(dim))?;
                                    put(output, slot, sum)?;
                                    pos += 1;
                                }
                            }
                        }
                    }
                    pcount += 1;
                    if pcount >= windows {
                        break;
                    }
                }
            }
        }
        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct Mapping {
    pub submaps: i32,
    pub mux: i32,
    pub submap_floors: Vec<i32>,
    pub submap_residues: Vec<i32>,
}

impl Mapping {
    pub fn new(bits: &mut Bits) -> R<Self> {
        bits.bits(16)?;
        let submaps = if bits.bit()? != 0 {
            bits.bits(4)? + 1
        } else {
            1
        };
        if bits.bit()? != 0 {
            bits.bits(8)?;
        }
        bits.bits(2)?;
        let mut mux = 0;
        if submaps > 1 {
            mux = bits.bits(4)?;
        }
        let mut submap_floors = vec![0i32; submaps as usize];
        let mut submap_residues = vec![0i32; submaps as usize];
        for index in 0..submaps as usize {
            bits.bits(8)?;
            submap_floors[index] = bits.bits(8)?;
            submap_residues[index] = bits.bits(8)?;
        }
        Ok(Self {
            submaps,
            mux,
            submap_floors,
            submap_residues,
        })
    }
}

#[derive(Debug, Clone, Default)]
pub struct FloorScratch {
    pub final_x: Vec<i32>,
    pub final_y: Vec<i32>,
    pub step2: Vec<bool>,
}

#[derive(Debug, Clone)]
pub struct Floor {
    pub partition_class_list: Vec<i32>,
    pub class_dimensions: Vec<i32>,
    pub class_subclasses: Vec<i32>,
    pub class_masterbooks: Vec<i32>,
    pub subclass_books: Vec<Vec<i32>>,
    pub multiplier: i32,
    pub x_list: Vec<i32>,
}

impl Floor {
    pub fn new(bits: &mut Bits, scratch: &mut FloorScratch) -> R<Self> {
        if bits.bits(16)? != 1 {
            return Err(AudioError::Invalid);
        }
        let partitions = bits.bits(5)?;
        let mut max_class = 0i32;
        let mut partition_class_list = vec![0i32; partitions as usize];
        for slot in partition_class_list.iter_mut() {
            *slot = bits.bits(4)?;
            if *slot >= max_class {
                max_class = *slot + 1;
            }
        }
        let classes = max_class as usize;
        let mut class_dimensions = vec![0i32; classes];
        let mut class_subclasses = vec![0i32; classes];
        let mut class_masterbooks = vec![0i32; classes];
        let mut subclass_books: Vec<Vec<i32>> = vec![Vec::new(); classes];
        for j in 0..classes {
            class_dimensions[j] = bits.bits(3)? + 1;
            let subclass = bits.bits(2)?;
            class_subclasses[j] = subclass;
            if subclass != 0 {
                class_masterbooks[j] = bits.bits(8)?;
            }
            let count = 1usize << subclass;
            let mut books = vec![0i32; count];
            for slot in books.iter_mut() {
                *slot = bits.bits(8)? - 1;
            }
            subclass_books[j] = books;
        }
        let multiplier = bits.bits(2)? + 1;
        let range_bits = bits.bits(4)?;
        let mut values = 2usize;
        for &class in &partition_class_list {
            values += class_dimensions[class as usize] as usize;
        }
        let mut x_list = vec![0i32; values];
        x_list[0] = 0;
        x_list[1] = 1 << range_bits;
        let mut size = 2usize;
        for &class in &partition_class_list {
            for _ in 0..class_dimensions[class as usize] {
                x_list[size] = bits.bits(range_bits)?;
                size += 1;
            }
        }
        if scratch.final_x.len() < size {
            scratch.final_x = vec![0; size];
            scratch.final_y = vec![0; size];
            scratch.step2 = vec![false; size];
        }
        Ok(Self {
            partition_class_list,
            class_dimensions,
            class_subclasses,
            class_masterbooks,
            subclass_books,
            multiplier,
            x_list,
        })
    }

    fn predict_point(x0: i32, y0: i32, x1: i32, y1: i32, target_x: i32) -> R<i32> {
        let dy = y1.wrapping_sub(y0);
        let dx = x1.wrapping_sub(x0);
        let abs_dy = if dy < 0 { dy.wrapping_neg() } else { dy };
        let scaled = target_x.wrapping_sub(x0).wrapping_mul(abs_dy);
        let interpolated = idiv(scaled, dx)?;
        Ok(if dy < 0 {
            y0.wrapping_sub(interpolated)
        } else {
            interpolated.wrapping_add(y0)
        })
    }

    fn draw_line(
        low_x: i32,
        high_x: i32,
        low_y: i32,
        high_y: i32,
        output: &mut [f32],
        limit: i32,
    ) -> R<()> {
        let mut x1 = low_y;
        let delta_x = high_y.wrapping_sub(high_x);
        let adx = x1.wrapping_sub(low_x);
        let mut abs_slope = if delta_x < 0 {
            delta_x.wrapping_neg()
        } else {
            delta_x
        };
        let base = idiv(delta_x, adx)?;
        let mut y = high_x;
        let mut err = 0i32;
        let sy = if delta_x < 0 {
            base.wrapping_sub(1)
        } else {
            base.wrapping_add(1)
        };
        let abs_base = if base < 0 { base.wrapping_neg() } else { base };
        abs_slope = abs_slope.wrapping_sub(abs_base.wrapping_mul(adx));
        let scaled = at(output, i64::from(low_x))? * at(&INVERSE_DB_TABLE, i64::from(high_x))?;
        put(output, i64::from(low_x), scaled)?;
        if x1 > limit {
            x1 = limit;
        }
        let mut x = i64::from(low_x) + 1;
        while x < i64::from(x1) {
            err = err.wrapping_add(abs_slope);
            if err >= adx {
                err = err.wrapping_sub(adx);
                y = y.wrapping_add(sy);
            } else {
                y = y.wrapping_add(base);
            }
            let scaled = at(output, x)? * at(&INVERSE_DB_TABLE, i64::from(y))?;
            put(output, x, scaled)?;
            x += 1;
        }
        Ok(())
    }

    pub fn decode_floor(
        &self,
        bits: &mut Bits,
        codebooks: &[Codebook],
        scratch: &mut FloorScratch,
    ) -> R<bool> {
        if bits.bit()? == 0 {
            return Ok(false);
        }
        let length = self.x_list.len();
        if scratch.final_x.len() < length {
            return Err(AudioError::Bounds);
        }
        scratch.final_x[..length].copy_from_slice(&self.x_list);
        let range = RANGE_LIST[(self.multiplier - 1) as usize];
        let width = ilog(range - 1);
        scratch.final_y[0] = bits.bits(width)?;
        scratch.final_y[1] = bits.bits(width)?;
        let mut offset = 2usize;
        for &class in &self.partition_class_list {
            let class = class as usize;
            let dim = self.class_dimensions[class];
            let cbits = self.class_subclasses[class];
            let csub = (1i32 << cbits) - 1;
            let mut cval = 0i32;
            if cbits > 0 {
                let master = at(&self.class_masterbooks, class as i64)?;
                cval = codebooks
                    .get(master as usize)
                    .ok_or(AudioError::Bounds)?
                    .decode(bits)?;
            }
            for _ in 0..dim {
                let book = self.subclass_books[class][(cval & csub) as usize];
                cval = ((cval as u32) >> cbits as u32) as i32;
                let value = if book >= 0 {
                    codebooks
                        .get(book as usize)
                        .ok_or(AudioError::Bounds)?
                        .decode(bits)?
                } else {
                    0
                };
                put(&mut scratch.final_y, offset as i64, value)?;
                offset += 1;
            }
        }
        Ok(true)
    }

    fn high_neighbor(x: &[i32], target: usize) -> i32 {
        let value = x[target];
        let mut high = -1i32;
        let mut closest = i32::MAX;
        for (index, &current) in x.iter().enumerate().take(target) {
            if current > value && current < closest {
                high = index as i32;
                closest = current;
            }
        }
        high
    }

    fn low_neighbor(x: &[i32], target: usize) -> i32 {
        let value = x[target];
        let mut low = -1i32;
        let mut closest = i32::MIN;
        for (index, &current) in x.iter().enumerate().take(target) {
            if current < value && current > closest {
                low = index as i32;
                closest = current;
            }
        }
        low
    }

    fn sort_by_x(scratch: &mut FloorScratch, low: i64, high: i64) -> R<()> {
        if low < high {
            let mut pivot_idx = low;
            let pivot_x = at(&scratch.final_x, low)?;
            let pivot_y = at(&scratch.final_y, low)?;
            let pivot_flag = at(&scratch.step2, low)?;
            for scan in (low + 1)..=high {
                let current = at(&scratch.final_x, scan)?;
                if current < pivot_x {
                    put(&mut scratch.final_x, pivot_idx, current)?;
                    let y = at(&scratch.final_y, scan)?;
                    put(&mut scratch.final_y, pivot_idx, y)?;
                    let flag = at(&scratch.step2, scan)?;
                    put(&mut scratch.step2, pivot_idx, flag)?;
                    pivot_idx += 1;
                    let x = at(&scratch.final_x, pivot_idx)?;
                    put(&mut scratch.final_x, scan, x)?;
                    let y = at(&scratch.final_y, pivot_idx)?;
                    put(&mut scratch.final_y, scan, y)?;
                    let flag = at(&scratch.step2, pivot_idx)?;
                    put(&mut scratch.step2, scan, flag)?;
                }
            }
            put(&mut scratch.final_x, pivot_idx, pivot_x)?;
            put(&mut scratch.final_y, pivot_idx, pivot_y)?;
            put(&mut scratch.step2, pivot_idx, pivot_flag)?;
            Self::sort_by_x(scratch, low, pivot_idx - 1)?;
            Self::sort_by_x(scratch, pivot_idx + 1, high)?;
        }
        Ok(())
    }

    pub fn compute_floor(
        &self,
        scratch: &mut FloorScratch,
        target: &mut [f32],
        half_block: i32,
    ) -> R<()> {
        let values = self.x_list.len();
        let range = RANGE_LIST[(self.multiplier - 1) as usize];
        scratch.step2[1] = true;
        scratch.step2[0] = true;
        for j in 2..values {
            let low = Self::low_neighbor(&scratch.final_x, j);
            let high = Self::high_neighbor(&scratch.final_x, j);
            let pred = Self::predict_point(
                at(&scratch.final_x, i64::from(low))?,
                at(&scratch.final_y, i64::from(low))?,
                at(&scratch.final_x, i64::from(high))?,
                at(&scratch.final_y, i64::from(high))?,
                scratch.final_x[j],
            )?;
            let val1 = scratch.final_y[j];
            let highroom = range.wrapping_sub(pred);
            let room = highroom.min(pred).wrapping_shl(1);
            if val1 != 0 {
                scratch.step2[high as usize] = true;
                scratch.step2[low as usize] = true;
                scratch.step2[j] = true;
                if val1 >= room {
                    scratch.final_y[j] = if highroom > pred {
                        pred.wrapping_add(val1.wrapping_sub(pred))
                    } else {
                        highroom
                            .wrapping_add(pred.wrapping_sub(val1))
                            .wrapping_sub(1)
                    };
                } else {
                    scratch.final_y[j] = if val1 & 1 != 0 {
                        pred.wrapping_sub(val1.wrapping_add(1) / 2)
                    } else {
                        (val1 / 2).wrapping_add(pred)
                    };
                }
            } else {
                scratch.step2[j] = false;
                scratch.final_y[j] = pred;
            }
        }
        Self::sort_by_x(scratch, 0, values as i64 - 1)?;
        let mut lx = 0i32;
        let mut ly = scratch.final_y[0].wrapping_mul(self.multiplier);
        for q in 1..values {
            if scratch.step2[q] {
                let hx = scratch.final_x[q];
                let hy = scratch.final_y[q].wrapping_mul(self.multiplier);
                Self::draw_line(lx, ly, hx, hy, target, half_block)?;
                if hx >= half_block {
                    return Ok(());
                }
                lx = hx;
                ly = hy;
            }
        }
        let factor = at(&INVERSE_DB_TABLE, i64::from(ly))?;
        let mut j = i64::from(lx);
        while j < i64::from(half_block) {
            let scaled = at(target, j)? * factor;
            put(target, j, scaled)?;
            j += 1;
        }
        Ok(())
    }
}

#[derive(Debug, Clone, Default)]
pub struct Trig {
    pub t1: Vec<f32>,
    pub t2: Vec<f32>,
    pub t3: Vec<f32>,
    pub reversed: Vec<i32>,
}

#[derive(Debug, Clone)]
pub struct SampleDecoder {
    pub block_size0: i32,
    pub block_size1: i32,
    pub window: Vec<f32>,
    pub trig: [Trig; 2],
    pub codebooks: Vec<Codebook>,
    pub floors: Vec<Floor>,
    pub residues: Vec<Residue>,
    pub mappings: Vec<Mapping>,
    pub no_residues: Vec<bool>,
    pub modes: Vec<i32>,
    pub scratch: FloorScratch,
}

impl SampleDecoder {
    pub fn new(setup: &[u8]) -> R<Self> {
        let mut bits = Bits::new(setup);
        let block_size0 = 1i32 << bits.bits(4)?;
        let block_size1 = 1i32 << bits.bits(4)?;
        let window = vec![0.0f32; block_size1 as usize];
        let mut trig: [Trig; 2] = [Trig::default(), Trig::default()];
        for (block_type, slot) in trig.iter_mut().enumerate() {
            let size = if block_type != 0 {
                block_size1
            } else {
                block_size0
            };
            let half = (size >> 1) as usize;
            let quarter = (size >> 2) as usize;
            let eighth = (size >> 3) as usize;
            let size_f = f64::from(size);
            let mut t1 = vec![0.0f32; half];
            for m in 0..quarter {
                let angle = f64::from((m * 4) as i32) * core::f64::consts::PI / size_f;
                t1[m * 2] = libm::cos(angle) as f32;
                t1[m * 2 + 1] = -(libm::sin(angle) as f32);
            }
            let mut t2 = vec![0.0f32; half];
            for m in 0..quarter {
                let angle = f64::from((m * 2 + 1) as i32) * core::f64::consts::PI
                    / f64::from(size.wrapping_mul(2));
                t2[m * 2] = libm::cos(angle) as f32;
                t2[m * 2 + 1] = libm::sin(angle) as f32;
            }
            let mut t3 = vec![0.0f32; quarter];
            for t in 0..eighth {
                let angle = f64::from((t * 4 + 2) as i32) * core::f64::consts::PI / size_f;
                t3[t * 2] = libm::cos(angle) as f32;
                t3[t * 2 + 1] = -(libm::sin(angle) as f32);
            }
            let log_size = ilog(eighth as i32 - 1);
            let reversed = (0..eighth as i32)
                .map(|i| reverse_bits(i, log_size))
                .collect();
            *slot = Trig {
                t1,
                t2,
                t3,
                reversed,
            };
        }
        let mut scratch = FloorScratch::default();
        let codebook_count = bits.bits(8)? + 1;
        let mut codebooks = Vec::new();
        for _ in 0..codebook_count {
            codebooks.push(Codebook::new(&mut bits)?);
        }
        let skip = bits.bits(6)? + 1;
        for _ in 0..skip {
            bits.bits(16)?;
        }
        let floor_count = bits.bits(6)? + 1;
        let mut floors = Vec::new();
        for _ in 0..floor_count {
            floors.push(Floor::new(&mut bits, &mut scratch)?);
        }
        let residue_count = bits.bits(6)? + 1;
        let mut residues = Vec::new();
        for _ in 0..residue_count {
            residues.push(Residue::new(&mut bits)?);
        }
        let mapping_count = bits.bits(6)? + 1;
        let mut mappings = Vec::new();
        for _ in 0..mapping_count {
            mappings.push(Mapping::new(&mut bits)?);
        }
        let mode_count = bits.bits(6)? + 1;
        let mut no_residues = vec![false; mode_count as usize];
        let mut modes = vec![0i32; mode_count as usize];
        for index in 0..mode_count as usize {
            no_residues[index] = bits.bit()? != 0;
            bits.bits(16)?;
            bits.bits(16)?;
            modes[index] = bits.bits(8)?;
        }
        Ok(Self {
            block_size0,
            block_size1,
            window,
            trig,
            codebooks,
            floors,
            residues,
            mappings,
            no_residues,
            modes,
            scratch,
        })
    }
}

macro_rules! rd {
    ($a:expr, $i:expr) => {
        at(&$a[..], i64::from($i))?
    };
}

macro_rules! wr {
    ($a:expr, $i:expr, $v:expr) => {{
        let value = $v;
        put(&mut $a[..], i64::from($i), value)?
    }};
}

#[derive(Debug, Clone)]
pub struct MusicSample {
    pub sample_rate: i32,
    pub sample_count: i32,
    pub start: i32,
    pub end: i32,
    pub loop_consistency: bool,
    pub packets: Vec<Vec<u8>>,
    pub t: Option<Vec<f32>>,
    pub y: i32,
    pub h: i32,
    pub block_flag: bool,
    pub samples: Option<Vec<f32>>,
    pub sample_length: i32,
    pub sound_indices: i32,
    pub audio_buffer_length: i32,
}

impl MusicSample {
    pub fn new(data: &[u8]) -> R<Self> {
        let mut stream = Buf::new(data);
        let sample_rate = stream.i32()?;
        let sample_count = stream.i32()?;
        let start = stream.i32()?;
        let mut end = stream.i32()?;
        let mut loop_consistency = false;
        if end < 0 {
            end = !end;
            loop_consistency = true;
        }
        let packet_count = stream.i32()?;
        if packet_count < 0 {
            return Err(AudioError::Invalid);
        }
        let mut packets = Vec::new();
        for _ in 0..packet_count {
            let mut size = 0i32;
            loop {
                let byte = stream.u8()?;
                size = size.wrapping_add(byte);
                if byte < 255 {
                    break;
                }
            }
            if size < 0 {
                return Err(AudioError::Invalid);
            }
            let from = stream.pos as usize;
            let to = from.checked_add(size as usize).ok_or(AudioError::Bounds)?;
            let chunk = data.get(from..to).ok_or(AudioError::Bounds)?;
            stream.pos = to as i64;
            packets.push(chunk.to_vec());
        }
        Ok(Self {
            sample_rate,
            sample_count,
            start,
            end,
            loop_consistency,
            packets,
            t: None,
            y: 0,
            h: 0,
            block_flag: false,
            samples: None,
            sample_length: 0,
            sound_indices: 0,
            audio_buffer_length: 0,
        })
    }

    pub fn mdct(&mut self, dec: &mut SampleDecoder, packet: i32) -> R<Option<Vec<f32>>> {
        let data = self
            .packets
            .get(usize::try_from(packet).map_err(|_| AudioError::Bounds)?)
            .ok_or(AudioError::Bounds)?;
        let mut bits = Bits::new(data);
        if self.t.is_none() {
            return Err(AudioError::Invalid);
        }
        bits.bit()?;
        let mode_number = bits.bits(ilog(dec.modes.len() as i32 - 1))?;
        let flag = rd!(dec.no_residues, mode_number);
        let blocksize = i64::from(if flag {
            dec.block_size1
        } else {
            dec.block_size0
        });
        let mut previous = false;
        let mut next = false;
        if flag {
            previous = bits.bit()? != 0;
            next = bits.bit()? != 0;
        }
        let bs0 = i64::from(dec.block_size0);
        let center = blocksize >> 1;
        let (left_start, left_end, left_n) = if flag && !previous {
            (
                (blocksize >> 2) - (bs0 >> 2),
                (bs0 >> 2) + (blocksize >> 2),
                bs0 >> 1,
            )
        } else {
            (0, center, blocksize >> 1)
        };
        let (right_start, right_end, right_n) = if flag && !next {
            (
                blocksize - (blocksize >> 2) - (bs0 >> 2),
                (bs0 >> 2) + (blocksize - (blocksize >> 2)),
                bs0 >> 1,
            )
        } else {
            (center, blocksize, blocksize >> 1)
        };
        let mapping_index = rd!(dec.modes, mode_number);
        let mapping = dec
            .mappings
            .get(usize::try_from(mapping_index).map_err(|_| AudioError::Bounds)?)
            .ok_or(AudioError::Bounds)?;
        let floor_number = rd!(mapping.submap_floors, mapping.mux);
        let floor = dec
            .floors
            .get(usize::try_from(floor_number).map_err(|_| AudioError::Bounds)?)
            .ok_or(AudioError::Bounds)?;
        let all_floors_empty = !floor.decode_floor(&mut bits, &dec.codebooks, &mut dec.scratch)?;
        for submap in 0..mapping.submaps {
            let residue_index = rd!(mapping.submap_residues, submap);
            let residue = dec
                .residues
                .get(usize::try_from(residue_index).map_err(|_| AudioError::Bounds)?)
                .ok_or(AudioError::Bounds)?;
            residue.decode(
                &mut bits,
                &dec.codebooks,
                &mut dec.window,
                blocksize >> 1,
                all_floors_empty,
            )?;
        }
        if !all_floors_empty {
            let floor_index = rd!(mapping.submap_floors, mapping.mux);
            let floor = dec
                .floors
                .get(usize::try_from(floor_index).map_err(|_| AudioError::Bounds)?)
                .ok_or(AudioError::Bounds)?;
            floor.compute_floor(
                &mut dec.scratch,
                &mut dec.window,
                (blocksize >> 1) as i32,
            )?;
        }
        if all_floors_empty {
            for w in (blocksize >> 1)..blocksize {
                wr!(dec.window, w, 0.0f32);
            }
        } else {
            let half = blocksize >> 1;
            let quarter = blocksize >> 2;
            let eighth = blocksize >> 3;
            let trig = &dec.trig[usize::from(flag)];
            let pcm = &mut dec.window;
            for i in 0..half {
                wr!(pcm, i, rd!(pcm, i) * 0.5f32);
            }
            for i in half..blocksize {
                wr!(pcm, i, -rd!(pcm, blocksize - i - 1));
            }
            for i in 0..quarter {
                let diff_a = rd!(pcm, i * 4) - rd!(pcm, blocksize - i * 4 - 1);
                let diff_b = rd!(pcm, i * 4 + 2) - rd!(pcm, blocksize - i * 4 - 3);
                let cos_val = rd!(trig.t1, i * 2);
                let sin_val = rd!(trig.t1, i * 2 + 1);
                wr!(pcm, blocksize - i * 4 - 1, diff_a * cos_val - diff_b * sin_val);
                wr!(pcm, blocksize - i * 4 - 3, diff_a * sin_val + diff_b * cos_val);
            }
            for i in 0..eighth {
                let diff_a = rd!(pcm, half + i * 4 + 3);
                let diff_b = rd!(pcm, half + i * 4 + 1);
                let cos_val = rd!(pcm, i * 4 + 3);
                let sin_val = rd!(pcm, i * 4 + 1);
                wr!(pcm, half + i * 4 + 3, diff_a + cos_val);
                wr!(pcm, half + i * 4 + 1, diff_b + sin_val);
                let alt_cos = rd!(trig.t1, half - 4 - i * 4);
                let alt_sin = rd!(trig.t1, half - 3 - i * 4);
                wr!(
                    pcm,
                    i * 4 + 3,
                    (diff_a - cos_val) * alt_cos - (diff_b - sin_val) * alt_sin
                );
                wr!(
                    pcm,
                    i * 4 + 1,
                    (diff_b - sin_val) * alt_cos + (diff_a - cos_val) * alt_sin
                );
            }
            let log_block = i64::from(ilog(blocksize as i32 - 1));
            for pass in 0..(log_block - 3).max(0) {
                let stride = blocksize >> (pass + 2);
                let trig_step = 8i64 << pass;
                for group in 0..(2i64 << pass) {
                    let upper = blocksize - stride * group * 2;
                    let lower = blocksize - stride * (group * 2 + 1);
                    for pair in 0..(blocksize >> (pass + 4)) {
                        let offset = pair * 4;
                        let upper_real = rd!(pcm, upper - 1 - offset);
                        let upper_imag = rd!(pcm, upper - 3 - offset);
                        let lower_real = rd!(pcm, lower - 1 - offset);
                        let lower_imag = rd!(pcm, lower - 3 - offset);
                        wr!(pcm, upper - 1 - offset, upper_real + lower_real);
                        wr!(pcm, upper - 3 - offset, upper_imag + lower_imag);
                        let twiddle_cos = rd!(trig.t1, pair * trig_step);
                        let twiddle_sin = rd!(trig.t1, pair * trig_step + 1);
                        wr!(
                            pcm,
                            lower - 1 - offset,
                            (upper_real - lower_real) * twiddle_cos
                                - (upper_imag - lower_imag) * twiddle_sin
                        );
                        wr!(
                            pcm,
                            lower - 3 - offset,
                            (upper_imag - lower_imag) * twiddle_cos
                                + (upper_real - lower_real) * twiddle_sin
                        );
                    }
                }
            }
            for pass in 1..(eighth - 1) {
                let stride = i64::from(rd!(trig.reversed, pass));
                if pass < stride {
                    let a = pass * 8;
                    let b = stride * 8;
                    for k in [1i64, 3, 5, 7] {
                        let tmp = rd!(pcm, a + k);
                        wr!(pcm, a + k, rd!(pcm, b + k));
                        wr!(pcm, b + k, tmp);
                    }
                }
            }
            for pass in 0..half {
                wr!(pcm, pass, rd!(pcm, pass * 2 + 1));
            }
            for pass in 0..eighth {
                wr!(pcm, blocksize - 1 - pass * 2, rd!(pcm, pass * 4));
                wr!(pcm, blocksize - 2 - pass * 2, rd!(pcm, pass * 4 + 1));
                wr!(
                    pcm,
                    blocksize - quarter - 1 - pass * 2,
                    rd!(pcm, pass * 4 + 2)
                );
                wr!(
                    pcm,
                    blocksize - quarter - 2 - pass * 2,
                    rd!(pcm, pass * 4 + 3)
                );
            }
            for pass in 0..eighth {
                let diff_b = rd!(trig.t3, pass * 2);
                let cos_val = rd!(trig.t3, pass * 2 + 1);
                let sin_val = rd!(pcm, half + pass * 2);
                let alt_cos = rd!(pcm, half + pass * 2 + 1);
                let alt_sin = rd!(pcm, blocksize - 2 - pass * 2);
                let sample_b = rd!(pcm, blocksize - 1 - pass * 2);
                let mut combined = cos_val * (sin_val - alt_sin) + diff_b * (alt_cos + sample_b);
                wr!(pcm, half + pass * 2, (sin_val + alt_sin + combined) * 0.5f32);
                wr!(pcm, blocksize - 2 - pass * 2, (sin_val + alt_sin - combined) * 0.5f32);
                combined = cos_val * (alt_cos + sample_b) - diff_b * (sin_val - alt_sin);
                wr!(pcm, half + pass * 2 + 1, (alt_cos - sample_b + combined) * 0.5f32);
                wr!(pcm, blocksize - 1 - pass * 2, (-alt_cos + sample_b + combined) * 0.5f32);
            }
            for pos in 0..quarter {
                let a = rd!(pcm, half + pos * 2);
                let b = rd!(pcm, half + pos * 2 + 1);
                let s0 = rd!(trig.t2, pos * 2);
                let s1 = rd!(trig.t2, pos * 2 + 1);
                wr!(pcm, pos, a * s0 + b * s1);
                let a = rd!(pcm, half + pos * 2);
                let b = rd!(pcm, half + pos * 2 + 1);
                wr!(pcm, half - 1 - pos, a * s1 - b * s0);
            }
            for pos in 0..quarter {
                wr!(pcm, pos + (blocksize - quarter), -rd!(pcm, pos));
            }
            for pos in 0..quarter {
                wr!(pcm, pos, rd!(pcm, quarter + pos));
            }
            for pos in 0..quarter {
                wr!(pcm, quarter + pos, -rd!(pcm, quarter - pos - 1));
            }
            for pos in 0..quarter {
                wr!(pcm, half + pos, rd!(pcm, blocksize - pos - 1));
            }
            for index in left_start..left_end {
                let diff_b = libm::sin(
                    ((index - left_start) as f64 + 0.5) / left_n as f64
                        * 0.5
                        * core::f64::consts::PI,
                ) as f32;
                let factor = libm::sin(core::f64::consts::FRAC_PI_2 * f64::from(diff_b) * f64::from(diff_b))
                    as f32;
                wr!(pcm, index, rd!(pcm, index) * factor);
            }
            for index in right_start..right_end {
                let diff_b = libm::sin(
                    ((index - right_start) as f64 + 0.5) / right_n as f64
                        * 0.5
                        * core::f64::consts::PI
                        + core::f64::consts::FRAC_PI_2,
                ) as f32;
                let factor = libm::sin(core::f64::consts::FRAC_PI_2 * f64::from(diff_b) * f64::from(diff_b))
                    as f32;
                wr!(pcm, index, rd!(pcm, index) * factor);
            }
        }
        let mut overlap: Option<Vec<f32>> = None;
        if self.y > 0 {
            let size = (blocksize + i64::from(self.y)) >> 2;
            let mut buffer = alloc(size, 0.0f32, MAX_SAMPLES)?;
            let t = self.t.as_ref().ok_or(AudioError::Invalid)?;
            if !self.block_flag {
                for index in 0..i64::from(self.h) {
                    let source = index + i64::from(self.y >> 1);
                    let sum = rd!(buffer, index) + rd!(t, source);
                    wr!(buffer, index, sum);
                }
            }
            if !all_floors_empty {
                for index in left_start..(blocksize >> 1) {
                    let target = buffer.len() as i64 - (blocksize >> 1) + index;
                    let sum = rd!(buffer, target) + rd!(dec.window, index);
                    wr!(buffer, target, sum);
                }
            }
            overlap = Some(buffer);
        }
        let t = self.t.as_mut().ok_or(AudioError::Invalid)?;
        std::mem::swap(t, &mut dec.window);
        self.y = blocksize as i32;
        self.h = (right_end - (blocksize >> 1)) as i32;
        self.block_flag = all_floors_empty;
        Ok(overlap)
    }

    pub fn has_more_samples(&self) -> bool {
        self.sample_length < self.sample_count.wrapping_sub(1)
    }

    pub fn get_audio_buffer_length(&mut self, dec: &mut SampleDecoder, len: i32) -> R<i32> {
        let mut length = 0i32;
        if self.samples.is_none() {
            self.y = 0;
            self.t = Some(vec![0.0f32; dec.block_size1 as usize]);
            self.samples = Some(alloc(i64::from(self.sample_count), 0.0f32, MAX_SAMPLES)?);
            self.sample_length = 0;
            self.sound_indices = 0;
        }
        while len > self.sample_length && (self.sound_indices as usize) < self.packets.len() {
            let computed = self.mdct(dec, self.sound_indices)?;
            if let Some(computed) = computed {
                let mut count = self.sample_length;
                let mut total = computed.len() as i64;
                if total > i64::from(self.sample_count) - i64::from(count) {
                    total = i64::from(self.sample_count) - i64::from(count);
                }
                let samples = self.samples.as_mut().ok_or(AudioError::Invalid)?;
                for src in 0..total {
                    put(samples, i64::from(count), computed[src as usize])?;
                    count += 1;
                }
                length += count - self.sample_length;
                self.sample_length = count;
            }
            self.sound_indices += 1;
        }
        Ok(length)
    }

    pub fn calculate_audio_buffer_length(&mut self, dec: &mut SampleDecoder, len: i32) -> R<()> {
        let mut length = len;
        if length > self.sample_count.wrapping_sub(1) {
            length = self.sample_count.wrapping_sub(1);
        }
        if length > self.sample_length {
            self.audio_buffer_length = self.get_audio_buffer_length(dec, length)?;
        } else {
            self.audio_buffer_length = 0;
        }
        if !self.has_more_samples() {
            self.t = None;
        }
        Ok(())
    }

    pub fn audio_buffer(&self, cycles_per_second: i32) -> R<Option<AudioBuffer>> {
        let ready = !self.has_more_samples()
            || (self.sample_length > self.sample_rate
                && self.audio_buffer_length > idiv(self.sample_rate, cycles_per_second)?);
        if !ready {
            return Ok(None);
        }
        let samples = self.samples.as_ref().ok_or(AudioError::Invalid)?;
        Ok(Some(AudioBuffer::new(
            self.sample_rate,
            samples.clone(),
            self.start,
            self.end,
            self.loop_consistency,
        )))
    }
}
