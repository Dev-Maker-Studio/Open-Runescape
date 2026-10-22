const COSINE_SAMPLES: [i32; 4] = [16384, 11585, 0, -11585];

pub(super) fn procedural_surface_height(x: i32, y: i32) -> i32 {
    let x = x.wrapping_add(932731);
    let y = y.wrapping_add(556238);
    let noise = perlin_noise(x.wrapping_add(45365), y.wrapping_add(91923), 4) - 128
        + ((perlin_noise(x.wrapping_add(10294), y.wrapping_add(37821), 2) - 128) >> 1)
        + ((perlin_noise(x, y, 1) - 128) >> 2);
    let height = ((f64::from(noise) * 0.3) as i32 + 35).clamp(10, 60);
    -height * 32
}

fn perlin_noise(x: i32, y: i32, frequency: i32) -> i32 {
    let base_x = x / frequency;
    let base_y = y / frequency;
    let fraction_x = x & (frequency - 1);
    let fraction_y = y & (frequency - 1);
    let next_x = base_x.wrapping_add(1);
    let next_y = base_y.wrapping_add(1);
    let north = interpolate(
        smoothed_noise(base_x, base_y),
        smoothed_noise(next_x, base_y),
        fraction_x,
        frequency,
    );
    let south = interpolate(
        smoothed_noise(base_x, next_y),
        smoothed_noise(next_x, next_y),
        fraction_x,
        frequency,
    );
    interpolate(north, south, fraction_y, frequency)
}

fn smoothed_noise(x: i32, y: i32) -> i32 {
    let west = x.wrapping_sub(1);
    let east = x.wrapping_add(1);
    let north = y.wrapping_sub(1);
    let south = y.wrapping_add(1);
    let corners = random_noise(west, north)
        + random_noise(east, north)
        + random_noise(west, south)
        + random_noise(east, south);
    let sides = random_noise(west, y)
        + random_noise(east, y)
        + random_noise(x, north)
        + random_noise(x, south);
    corners / 16 + sides / 8 + random_noise(x, y) / 4
}

fn random_noise(x: i32, y: i32) -> i32 {
    let n = y.wrapping_mul(57).wrapping_add(x);
    let n = n ^ n.wrapping_shl(13);
    let value = n
        .wrapping_mul(n.wrapping_mul(n).wrapping_mul(15731).wrapping_add(789221))
        .wrapping_add(1376312589)
        & 0x7fffffff;
    (value >> 19) & 0xff
}

fn interpolate(a: i32, b: i32, angle: i32, frequency: i32) -> i32 {
    let cosine = (65536 - COSINE_SAMPLES[(angle * 4 / frequency) as usize]) >> 1;
    (((65536 - cosine) * a) >> 16) + ((cosine * b) >> 16)
}

#[cfg(test)]
mod tests {
    use super::COSINE_SAMPLES;

    #[test]
    fn native_cosine_samples_cover_exactly_the_reachable_table_indices() {
        let mut count = 0;
        for line in include_str!("../../tests/fixtures/native.tsv")
            .lines()
            .filter(|line| line.starts_with("cosine\t"))
        {
            let fields: Vec<_> = line.split('\t').collect();
            assert_eq!(fields.len(), 3);
            let index: usize = fields[1].parse().unwrap();
            let value: i32 = fields[2].parse().unwrap();
            assert_eq!(index, count * 2048);
            assert_eq!(COSINE_SAMPLES[count], value);
            count += 1;
        }
        assert_eq!(count, COSINE_SAMPLES.len());
    }
}
