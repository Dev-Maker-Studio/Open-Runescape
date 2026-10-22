use super::writer::{char_to_byte, ClientPacketError, Text, Writer};

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Huffman {
    codes: Vec<i32>,
    code_lengths: Vec<i8>,
}

impl Huffman {
    pub fn new(code_lengths: &[i8]) -> Self {
        let mut codes = vec![0i32; code_lengths.len()];
        let mut counts = [0i32; 33];
        for (i, &length) in code_lengths.iter().enumerate() {
            let cl = i32::from(length);
            if cl == 0 {
                continue;
            }
            let code = 1i32.wrapping_shl((32 - cl) as u32);
            let count = counts[cl as usize];
            codes[i] = count;
            let new_code;
            if count & code != 0 {
                new_code = counts[(cl - 1) as usize];
            } else {
                new_code = count | code;
                let mut j = cl - 1;
                while j >= 1 {
                    let bit_length = counts[j as usize];
                    if bit_length != count {
                        break;
                    }
                    let bit = 1i32.wrapping_shl((32 - j) as u32);
                    if bit_length & bit != 0 {
                        counts[j as usize] = counts[(j - 1) as usize];
                        break;
                    }
                    counts[j as usize] = bit_length | bit;
                    j -= 1;
                }
            }
            counts[cl as usize] = new_code;
            for j in (cl + 1)..=32 {
                if count == counts[j as usize] {
                    counts[j as usize] = new_code;
                }
            }
        }
        Self {
            codes,
            code_lengths: code_lengths.to_vec(),
        }
    }

    pub fn compress(&self, input: &[u8], output: &mut Vec<u8>) -> Result<usize, ClientPacketError> {
        let output_offset = output.len();
        output.resize(output_offset + input.len() * 5 + 8, 0);
        let mut bit_buffer: i32 = 0;
        let mut bit_count: i32 = (output_offset as i32) << 3;
        for &byte in input {
            let symbol = usize::from(byte);
            let code = *self
                .codes
                .get(symbol)
                .ok_or(ClientPacketError::UnsupportedSymbol(byte))?;
            let length = i32::from(self.code_lengths[symbol]);
            if length == 0 {
                return Err(ClientPacketError::UnsupportedSymbol(byte));
            }
            let mut offset_bytes = (bit_count >> 3) as usize;
            let mut offset_bits = bit_count & 7;
            bit_buffer &= offset_bits.wrapping_neg() >> 31;
            let output_end = (((offset_bits + length - 1) >> 3) as usize) + offset_bytes;
            offset_bits += 24;
            let ucode = code as u32;
            let v = bit_buffer | (ucode.wrapping_shr(offset_bits as u32) as i32);
            output[offset_bytes] = v as u8;
            bit_buffer = v;
            if offset_bytes < output_end {
                offset_bytes += 1;
                offset_bits -= 8;
                let v = ucode.wrapping_shr(offset_bits as u32) as i32;
                output[offset_bytes] = v as u8;
                bit_buffer = v;
                if offset_bytes < output_end {
                    offset_bytes += 1;
                    offset_bits -= 8;
                    let v = ucode.wrapping_shr(offset_bits as u32) as i32;
                    output[offset_bytes] = v as u8;
                    bit_buffer = v;
                    if offset_bytes < output_end {
                        offset_bytes += 1;
                        offset_bits -= 8;
                        let v = ucode.wrapping_shr(offset_bits as u32) as i32;
                        output[offset_bytes] = v as u8;
                        bit_buffer = v;
                        if offset_bytes < output_end {
                            offset_bytes += 1;
                            offset_bits -= 8;
                            let v = code.wrapping_shl(offset_bits.wrapping_neg() as u32);
                            output[offset_bytes] = v as u8;
                            bit_buffer = v;
                        }
                    }
                }
            }
            bit_count += length;
        }
        let used = ((bit_count + 7) >> 3) as usize;
        output.truncate(used);
        Ok(used - output_offset)
    }

    pub fn chat_message(
        &self,
        writer: &mut Writer,
        message: impl Into<Text>,
    ) -> Result<usize, ClientPacketError> {
        let message = message.into();
        let data: Vec<u8> = message
            .0
            .iter()
            .map(|u| if *u == 0 { 63 } else { char_to_byte(*u) })
            .collect();
        let start = writer.len();
        writer.smart(data.len() as i32)?;
        self.compress(&data, &mut writer.0)?;
        Ok(writer.len() - start)
    }
}
