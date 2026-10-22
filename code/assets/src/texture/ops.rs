#![allow(
    clippy::approx_constant,
    clippy::manual_range_contains,
    clippy::needless_range_loop
)]

use super::bricks;
use super::random::JavaRandom;
use super::raster::{Canvas, Shape};
use super::{Eval, Node};
use crate::{Error, Reader};

#[derive(Debug, Clone)]
pub(crate) struct Perlin {
    pub(crate) noise: Vec<u8>,
    pub(crate) steps: i32,
    pub(crate) amplitude: i32,
    pub(crate) scale_y: i32,
    pub(crate) scale_x: i32,
    pub(crate) seed: i32,
    pub(crate) step_multipliers: Option<Vec<i16>>,
    pub(crate) fog: bool,
    pub(crate) amplitude_multipliers: Option<Vec<i16>>,
}

#[derive(Debug, Clone)]
pub(crate) struct Voronoi {
    pub(crate) permutation: Vec<u8>,
    pub(crate) points: Vec<i16>,
    pub(crate) seed: i32,
    pub(crate) jitter: i32,
    pub(crate) cells_x: i32,
    pub(crate) cells_y: i32,
    pub(crate) output: i32,
    pub(crate) metric: i32,
}

#[derive(Debug, Clone)]
pub(crate) struct Square {
    pub(crate) steps: i32,
    pub(crate) duty: i32,
    pub(crate) axis: i32,
    pub(crate) pulse_end: Vec<i32>,
    pub(crate) step_start: Vec<i32>,
}

#[derive(Debug, Clone)]
pub(crate) struct LineNoise {
    pub(crate) seed: i32,
    pub(crate) count: i32,
    pub(crate) length: i32,
    pub(crate) offset: i32,
    pub(crate) spread: i32,
}

#[derive(Debug, Clone)]
pub(crate) struct Bricks {
    pub(crate) columns: i32,
    pub(crate) rows: i32,
    pub(crate) vertical_offset: i32,
    pub(crate) stagger: i32,
    pub(crate) brightness_variation: i32,
    pub(crate) column_variation: i32,
    pub(crate) row_variation: i32,
    pub(crate) mortar: i32,
    pub(crate) half_mortar: i32,
    pub(crate) column_width: i32,
    pub(crate) row_bounds: Vec<i32>,
    pub(crate) column_bounds: Vec<Vec<i32>>,
    pub(crate) brightness: Vec<Vec<i32>>,
}

#[derive(Debug, Clone)]
pub(crate) struct Brightness {
    pub(crate) tolerance: i32,
    pub(crate) blue: i32,
    pub(crate) green: i32,
    pub(crate) red: i32,
    pub(crate) target: [i32; 3],
}

#[derive(Debug, Clone)]
pub(crate) enum Kind {
    Square(Square),
    LineNoise(LineNoise),
    Rasterizer(Vec<Shape>),
    Colorize([i32; 3]),
    IrregularBricks(bricks::Params),
    ColorEdge {
        normalized: bool,
        strength: i32,
    },
    Waveform {
        wave: i32,
        shape: i32,
        frequency: i32,
    },
    Weave(i32),
    DiagonalWaves([i32; 7]),
    Herringbone {
        scale_x: i32,
        scale_y: i32,
        threshold: i32,
    },
    Kaleidoscope,
    Monochrome,
    Brightness(Brightness),
    Bricks(Bricks),
    Binary {
        lower: i32,
        upper: i32,
    },
    EdgeDetector(i32),
    MonochromeFill(i32),
    ColorFill([i32; 3]),
    HorizontalGradient,
    VerticalGradient,
    Curve {
        mode: i32,
        points: Option<Vec<[i32; 2]>>,
        lookup: Vec<i32>,
    },
    Combine(i32),
    ColorGradient {
        stops: Option<Vec<[i32; 4]>>,
        lookup: Vec<i32>,
    },
    Perlin(Perlin),
    Voronoi(Voronoi),
    Texture(i32),
    Sprite(i32),
    BoxBlur {
        radius_x: i32,
        radius_y: i32,
    },
    Clamp {
        min: i32,
        max: i32,
    },
    Range {
        multiplier: i32,
        offset: i32,
        max: i32,
    },
    Interpolate,
    Invert,
    Flip {
        horizontal: bool,
        vertical: bool,
    },
    Polar(i32),
    Hsl {
        hue_adjust: i32,
        saturation_adjust: i32,
        lightness_adjust: i32,
    },
    Noise,
    Tile {
        x: i32,
        y: i32,
    },
    Emboss {
        depth: i32,
        azimuth: i32,
        elevation: i32,
        light: [i32; 3],
    },
}

#[derive(Default)]
pub(crate) struct State {
    pub(crate) hsl: Vec<[i32; 3]>,
    pub(crate) mirror: [i32; 2],
}

pub(crate) fn new_kind(implementation: u8) -> Result<Kind, Error> {
    Ok(match implementation {
        0 => Kind::MonochromeFill(4096),
        1 => Kind::ColorFill([0; 3]),
        2 => Kind::HorizontalGradient,
        3 => Kind::VerticalGradient,
        5 => Kind::BoxBlur {
            radius_x: 1,
            radius_y: 1,
        },
        6 => Kind::Clamp { min: 0, max: 4096 },
        7 => Kind::Combine(6),
        8 => Kind::Curve {
            mode: 0,
            points: None,
            lookup: vec![0; 257],
        },
        10 => Kind::ColorGradient {
            stops: None,
            lookup: vec![0; 257],
        },
        15 => Kind::Voronoi(Voronoi {
            permutation: vec![0; 512],
            points: vec![0; 512],
            seed: 0,
            jitter: 2048,
            cells_x: 5,
            cells_y: 5,
            output: 2,
            metric: 1,
        }),
        21 => Kind::Interpolate,
        22 => Kind::Invert,
        27 => Kind::Square(Square {
            steps: 10,
            duty: 2048,
            axis: 0,
            pulse_end: Vec::new(),
            step_start: Vec::new(),
        }),
        29 => Kind::Rasterizer(Vec::new()),
        11 => Kind::Colorize([4096; 3]),
        28 => Kind::IrregularBricks(bricks::Params {
            seed: 0,
            min_width: 1024,
            max_width: 2048,
            min_height: 409,
            max_height: 819,
            offset_variation: 1024,
            corner_mode: 0,
            height_variation: 1024,
            brightness_variation: 1024,
        }),
        33 => Kind::ColorEdge {
            normalized: true,
            strength: 4096,
        },
        12 => Kind::Waveform {
            wave: 0,
            shape: 0,
            frequency: 1,
        },
        14 => Kind::Weave(585),
        37 => Kind::DiagonalWaves([2048, 0, 0, 2048, 12288, 4096, 8192]),
        16 => Kind::Herringbone {
            scale_x: 1,
            scale_y: 1,
            threshold: 204,
        },
        23 => Kind::Kaleidoscope,
        24 => Kind::Monochrome,
        25 => Kind::Brightness(Brightness {
            tolerance: 409,
            blue: 4096,
            green: 4096,
            red: 4096,
            target: [0; 3],
        }),
        38 => Kind::LineNoise(LineNoise {
            seed: 0,
            count: 2000,
            length: 16,
            offset: 0,
            spread: 4096,
        }),
        4 => Kind::Bricks(Bricks {
            columns: 4,
            rows: 8,
            vertical_offset: 0,
            stagger: 1024,
            brightness_variation: 1024,
            column_variation: 409,
            row_variation: 204,
            mortar: 81,
            half_mortar: 0,
            column_width: 0,
            row_bounds: Vec::new(),
            column_bounds: Vec::new(),
            brightness: Vec::new(),
        }),
        26 => Kind::Binary {
            lower: 0,
            upper: 4096,
        },
        35 => Kind::EdgeDetector(4096),
        9 => Kind::Flip {
            horizontal: true,
            vertical: true,
        },
        19 => Kind::Polar(32768),
        17 => Kind::Hsl {
            hue_adjust: 0,
            saturation_adjust: 0,
            lightness_adjust: 0,
        },
        13 => Kind::Noise,
        20 => Kind::Tile { x: 4, y: 4 },
        30 => Kind::Range {
            multiplier: 2048,
            offset: 1024,
            max: 3072,
        },
        32 => Kind::Emboss {
            depth: 4096,
            azimuth: 3216,
            elevation: 3216,
            light: [0; 3],
        },
        34 => Kind::Perlin(Perlin {
            noise: vec![0; 512],
            steps: 4,
            amplitude: 1638,
            scale_y: 4,
            scale_x: 4,
            seed: 0,
            step_multipliers: None,
            fog: true,
            amplitude_multipliers: None,
        }),
        36 => Kind::Texture(-1),
        39 => Kind::Sprite(-1),
        other => return Err(Error::UnsupportedTextureOperation(other)),
    })
}

pub(crate) fn name(kind: &Kind) -> &'static str {
    match kind {
        Kind::MonochromeFill(_) => "MonochromeFill",
        Kind::ColorFill(_) => "ColorFill",
        Kind::HorizontalGradient => "HorizontalGradient",
        Kind::VerticalGradient => "VerticalGradient",
        Kind::Curve { .. } => "Curve",
        Kind::Combine(_) => "Combine",
        Kind::ColorGradient { .. } => "ColorGradient",
        Kind::Perlin(_) => "PerlinNoise",
        Kind::Voronoi(_) => "VoronoiNoise",
        Kind::Texture(_) => "Texture",
        Kind::Sprite(_) => "Sprite",
        Kind::BoxBlur { .. } => "BoxBlur",
        Kind::Clamp { .. } => "Clamp",
        Kind::Range { .. } => "Range",
        Kind::Interpolate => "Interpolate",
        Kind::Invert => "Invert",
        Kind::Square(_) => "SquareWaveform",
        Kind::LineNoise(_) => "LineNoise",
        Kind::Rasterizer(_) => "Rasterizer",
        Kind::Colorize(_) => "Colorize",
        Kind::IrregularBricks(_) => "IrregularBricks",
        Kind::ColorEdge { .. } => "ColorEdgeDetector",
        Kind::Waveform { .. } => "Waveform",
        Kind::Weave(_) => "Weave",
        Kind::DiagonalWaves(_) => "DiagonalWaves",
        Kind::Herringbone { .. } => "Herringbone",
        Kind::Kaleidoscope => "Kaleidoscope",
        Kind::Monochrome => "Monochrome",
        Kind::Brightness(_) => "Brightness",
        Kind::Bricks(_) => "Bricks",
        Kind::Binary { .. } => "Binary",
        Kind::EdgeDetector(_) => "MonochromeEdgeDetector",
        Kind::Flip { .. } => "Flip",
        Kind::Polar(_) => "PolarDistortion",
        Kind::Hsl { .. } => "HslAdjust",
        Kind::Noise => "Noise",
        Kind::Tile { .. } => "Tile",
        Kind::Emboss { .. } => "Emboss",
    }
}

pub(crate) fn default_mono(kind: &Kind) -> bool {
    matches!(
        kind,
        Kind::MonochromeFill(_)
            | Kind::HorizontalGradient
            | Kind::VerticalGradient
            | Kind::Curve { .. }
            | Kind::Perlin(_)
            | Kind::Voronoi(_)
            | Kind::Noise
            | Kind::Square(_)
            | Kind::LineNoise(_)
            | Kind::Rasterizer(_)
            | Kind::Monochrome
            | Kind::IrregularBricks(_)
            | Kind::Waveform { .. }
            | Kind::Weave(_)
            | Kind::DiagonalWaves(_)
            | Kind::Herringbone { .. }
            | Kind::Bricks(_)
            | Kind::Binary { .. }
            | Kind::EdgeDetector(_)
            | Kind::Emboss { .. }
    )
}

pub(crate) fn child_count(kind: &Kind) -> usize {
    match kind {
        Kind::Combine(_) => 2,
        Kind::Interpolate | Kind::Polar(_) => 3,
        Kind::Curve { .. }
        | Kind::ColorGradient { .. }
        | Kind::BoxBlur { .. }
        | Kind::Clamp { .. }
        | Kind::Range { .. }
        | Kind::Invert
        | Kind::Colorize(_)
        | Kind::ColorEdge { .. }
        | Kind::Kaleidoscope
        | Kind::Monochrome
        | Kind::Brightness(_)
        | Kind::Binary { .. }
        | Kind::EdgeDetector(_)
        | Kind::Flip { .. }
        | Kind::Hsl { .. }
        | Kind::Tile { .. }
        | Kind::Emboss { .. } => 1,
        _ => 0,
    }
}

pub(crate) fn sprite_id(kind: &Kind) -> Option<i32> {
    match kind {
        Kind::Sprite(id) => Some(*id),
        _ => None,
    }
}

pub(crate) fn material_id(kind: &Kind) -> Option<i32> {
    match kind {
        Kind::Texture(id) => Some(*id),
        _ => None,
    }
}

fn set_color(packed: i32) -> [i32; 3] {
    [
        (packed & 0xff0000) >> 12,
        (packed & 0xff00) >> 4,
        (packed & 0xff) << 4,
    ]
}

pub(crate) fn decode_opcode(
    kind: &mut Kind,
    mono: &mut bool,
    opcode: u8,
    reader: &mut Reader,
) -> Result<(), Error> {
    match kind {
        Kind::MonochromeFill(fill) => {
            if opcode == 0 {
                *fill = (i32::from(reader.u8()?) << 12) / 255;
            }
        }
        Kind::ColorFill(color) => {
            if opcode == 0 {
                *color = set_color(reader.medium()?);
            }
        }
        Kind::HorizontalGradient | Kind::VerticalGradient => {}
        Kind::Texture(id) | Kind::Sprite(id) => {
            if opcode == 0 {
                *id = i32::from(reader.u16()?);
            }
        }
        Kind::Curve { mode, points, .. } => {
            if opcode == 0 {
                *mode = i32::from(reader.u8()?);
                let count = usize::from(reader.u8()?);
                let mut list = Vec::with_capacity(count);
                for _ in 0..count {
                    let x = i32::from(reader.u16()?);
                    let y = i32::from(reader.u16()?);
                    list.push([x, y]);
                }
                *points = Some(list);
            }
        }
        Kind::Combine(mode) => match opcode {
            0 => *mode = i32::from(reader.u8()?),
            1 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::ColorGradient { stops, .. } => {
            if opcode == 0 {
                let preset = reader.u8()?;
                if preset == 0 {
                    let count = usize::from(reader.u8()?);
                    let mut list = Vec::with_capacity(count);
                    for _ in 0..count {
                        let position = i32::from(reader.u16()?);
                        let a = i32::from(reader.u8()?) << 4;
                        let b = i32::from(reader.u8()?) << 4;
                        let c = i32::from(reader.u8()?) << 4;
                        list.push([position, a, b, c]);
                    }
                    *stops = Some(list);
                } else {
                    *stops = Some(gradient_preset(preset)?);
                }
            }
        }
        Kind::Perlin(p) => match opcode {
            0 => p.fog = reader.u8()? == 1,
            1 => p.steps = i32::from(reader.u8()?),
            2 => {
                p.amplitude = reader.short()?;
                if p.amplitude < 0 {
                    let mut list = Vec::new();
                    for _ in 0..p.steps.max(0) {
                        list.push(reader.short()? as i16);
                    }
                    p.step_multipliers = Some(list);
                }
            }
            3 => {
                p.scale_y = i32::from(reader.u8()?);
                p.scale_x = p.scale_y;
            }
            4 => p.seed = i32::from(reader.u8()?),
            5 => p.scale_x = i32::from(reader.u8()?),
            6 => p.scale_y = i32::from(reader.u8()?),
            _ => {}
        },
        Kind::Voronoi(v) => match opcode {
            0 => {
                v.cells_x = i32::from(reader.u8()?);
                v.cells_y = v.cells_x;
            }
            1 => v.seed = i32::from(reader.u8()?),
            2 => v.jitter = i32::from(reader.u16()?),
            3 => v.output = i32::from(reader.u8()?),
            4 => v.metric = i32::from(reader.u8()?),
            5 => v.cells_x = i32::from(reader.u8()?),
            6 => v.cells_y = i32::from(reader.u8()?),
            _ => {}
        },
        Kind::Invert => {
            if opcode == 0 {
                *mono = reader.u8()? == 1;
            }
        }
        Kind::Flip {
            horizontal,
            vertical,
        } => match opcode {
            0 => *horizontal = reader.u8()? == 1,
            1 => *vertical = reader.u8()? == 1,
            2 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::Polar(strength) => match opcode {
            0 => *strength = i32::from(reader.u16()?) << 4,
            1 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::Hsl {
            hue_adjust,
            saturation_adjust,
            lightness_adjust,
        } => match opcode {
            0 => *hue_adjust = reader.short()?,
            1 => *saturation_adjust = (reader.byte()? << 12) / 100,
            2 => *lightness_adjust = (reader.byte()? << 12) / 100,
            _ => {}
        },
        Kind::Noise => {}
        Kind::Square(q) => match opcode {
            0 => q.steps = i32::from(reader.u8()?),
            1 => q.duty = i32::from(reader.u16()?),
            2 => q.axis = i32::from(reader.u8()?),
            _ => {}
        },
        Kind::Colorize(m) => {
            if opcode <= 2 {
                m[usize::from(opcode)] = i32::from(reader.u16()?);
            }
        }
        Kind::ColorEdge {
            normalized,
            strength,
        } => match opcode {
            0 => *strength = i32::from(reader.u16()?),
            1 => *normalized = reader.u8()? == 1,
            _ => {}
        },
        Kind::Waveform {
            wave,
            shape,
            frequency,
        } => match opcode {
            0 => *wave = i32::from(reader.u8()?),
            1 => *shape = i32::from(reader.u8()?),
            3 => *frequency = i32::from(reader.u8()?),
            _ => {}
        },
        Kind::Weave(width) => {
            if opcode == 0 {
                *width = i32::from(reader.u16()?);
            }
        }
        Kind::DiagonalWaves(values) => {
            if opcode <= 6 {
                values[usize::from(opcode)] = i32::from(reader.u16()?);
            }
        }
        Kind::Herringbone {
            scale_x,
            scale_y,
            threshold,
        } => match opcode {
            0 => *scale_x = i32::from(reader.u8()?),
            1 => *scale_y = i32::from(reader.u8()?),
            2 => *threshold = i32::from(reader.u16()?),
            _ => {}
        },
        Kind::IrregularBricks(b) => match opcode {
            0 => b.seed = i32::from(reader.u8()?),
            1 => b.min_width = i32::from(reader.u16()?),
            2 => b.max_width = i32::from(reader.u16()?),
            3 => b.min_height = i32::from(reader.u16()?),
            4 => b.max_height = i32::from(reader.u16()?),
            5 => b.offset_variation = i32::from(reader.u16()?),
            6 => b.corner_mode = i32::from(reader.u8()?),
            7 => b.height_variation = i32::from(reader.u16()?),
            8 => b.brightness_variation = i32::from(reader.u16()?),
            _ => {}
        },
        Kind::Kaleidoscope => {
            if opcode == 0 {
                *mono = reader.u8()? == 1;
            }
        }
        Kind::Monochrome => {}
        Kind::Brightness(b) => match opcode {
            0 => b.tolerance = i32::from(reader.u16()?),
            1 => b.blue = i32::from(reader.u16()?),
            2 => b.green = i32::from(reader.u16()?),
            3 => b.red = i32::from(reader.u16()?),
            4 => {
                let color = reader.medium()?;
                b.target = [
                    (color & 0xff0000) << 4,
                    (color & 0xff00) >> 4,
                    (color & 0xff) >> 12,
                ];
            }
            _ => {}
        },
        Kind::Rasterizer(shapes) => match opcode {
            0 => {
                let count = reader.u8()?;
                shapes.clear();
                for _ in 0..count {
                    let shape = match reader.u8()? {
                        0 => {
                            let from = [reader.short()?, reader.short()?];
                            let to = [reader.short()?, reader.short()?];
                            let stroke = reader.medium()?;
                            reader.u8()?;
                            Shape::Line { from, to, stroke }
                        }
                        1 => {
                            let mut points = [0; 8];
                            for point in points.iter_mut() {
                                *point = reader.short()?;
                            }
                            let stroke = reader.medium()?;
                            reader.u8()?;
                            Shape::Bezier { points, stroke }
                        }
                        2 => Shape::Rectangle {
                            left: reader.short()?,
                            top: reader.short()?,
                            right: reader.short()?,
                            bottom: reader.short()?,
                            fill: reader.medium()?,
                            stroke: reader.medium()?,
                            width: i32::from(reader.u8()?),
                        },
                        3 => {
                            let center = [reader.short()?, reader.short()?];
                            let radius = [reader.short()?, reader.short()?];
                            let fill = reader.medium()?;
                            let stroke = reader.medium()?;
                            let width = i32::from(reader.u8()?);
                            Shape::Ellipse {
                                center,
                                radius,
                                fill,
                                stroke,
                                width,
                            }
                        }
                        _ => continue,
                    };
                    shapes.push(shape);
                }
            }
            1 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::LineNoise(l) => match opcode {
            0 => l.seed = i32::from(reader.u8()?),
            1 => l.count = i32::from(reader.u16()?),
            2 => l.length = i32::from(reader.u8()?),
            3 => l.offset = i32::from(reader.u16()?),
            4 => l.spread = i32::from(reader.u16()?),
            _ => {}
        },
        Kind::Bricks(b) => match opcode {
            0 => b.columns = i32::from(reader.u8()?),
            1 => b.rows = i32::from(reader.u8()?),
            2 => b.column_variation = i32::from(reader.u16()?),
            3 => b.row_variation = i32::from(reader.u16()?),
            4 => b.stagger = i32::from(reader.u16()?),
            5 => b.vertical_offset = i32::from(reader.u16()?),
            6 => b.mortar = i32::from(reader.u16()?),
            7 => b.brightness_variation = i32::from(reader.u16()?),
            _ => {}
        },
        Kind::Binary { lower, upper } => match opcode {
            0 => *lower = i32::from(reader.u16()?),
            1 => *upper = i32::from(reader.u16()?),
            _ => {}
        },
        Kind::EdgeDetector(strength) => {
            if opcode == 0 {
                *strength = i32::from(reader.u16()?);
            }
        }
        Kind::Tile { x, y } => match opcode {
            0 => *x = i32::from(reader.u8()?),
            1 => *y = i32::from(reader.u8()?),
            _ => {}
        },
        Kind::BoxBlur { radius_x, radius_y } => match opcode {
            0 => *radius_x = i32::from(reader.u8()?),
            1 => *radius_y = i32::from(reader.u8()?),
            2 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::Clamp { min, max } => match opcode {
            0 => *min = i32::from(reader.u16()?),
            1 => *max = i32::from(reader.u16()?),
            2 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::Range { offset, max, .. } => match opcode {
            0 => *offset = i32::from(reader.u16()?),
            1 => *max = i32::from(reader.u16()?),
            2 => *mono = reader.u8()? == 1,
            _ => {}
        },
        Kind::Interpolate => {
            if opcode == 0 {
                *mono = reader.u8()? == 1;
            }
        }
        Kind::Emboss {
            depth,
            azimuth,
            elevation,
            ..
        } => match opcode {
            0 => *depth = i32::from(reader.u16()?),
            1 => *azimuth = i32::from(reader.u16()?),
            2 => *elevation = i32::from(reader.u16()?),
            _ => {}
        },
    }
    Ok(())
}

fn gradient_preset(preset: u8) -> Result<Vec<[i32; 4]>, Error> {
    let rows: &[[i32; 4]] = match preset {
        1 => &[[0, 0, 0, 0], [4096, 4096, 4096, 4096]],
        2 => &[
            [0, 2650, 2602, 2361],
            [2867, 2313, 1799, 1558],
            [3072, 2618, 1734, 1413],
            [3276, 2296, 1220, 947],
            [3481, 2072, 963, 722],
            [3686, 2730, 2152, 1766],
            [3891, 2232, 1060, 915],
            [4096, 1686, 1413, 1140],
        ],
        3 => &[
            [0, 0, 0, 4096],
            [663, 0, 4096, 4096],
            [1363, 0, 4096, 0],
            [2048, 4096, 4096, 0],
            [2727, 4096, 0, 0],
            [3411, 4096, 0, 4096],
            [4096, 0, 0, 4096],
        ],
        4 => &[
            [0, 0, 0, 0],
            [1843, 0, 0, 1493],
            [2457, 0, 0, 2939],
            [2781, 0, 1124, 3565],
            [3481, 546, 3084, 4031],
            [4096, 4096, 4096, 4096],
        ],
        5 => &[
            [0, 80, 192, 321],
            [155, 321, 449, 562],
            [389, 578, 690, 803],
            [671, 947, 995, 1140],
            [897, 1285, 1397, 1509],
            [1175, 1525, 1429, 1413],
            [1368, 1734, 1461, 1333],
            [1507, 1413, 1525, 1702],
            [1736, 1108, 1590, 2056],
            [2088, 1766, 2056, 2666],
            [2355, 2409, 2586, 3276],
            [2691, 3116, 3148, 3228],
            [3031, 3806, 3710, 3196],
            [3522, 3437, 3421, 3019],
            [3727, 3116, 3148, 3228],
            [4096, 2377, 2505, 2746],
        ],
        6 => &[
            [2048, 0, 4096, 0],
            [2867, 4096, 4096, 0],
            [3276, 4096, 4096, 0],
            [4096, 4096, 0, 0],
        ],
        _ => return Err(Error::InvalidTexture("gradient preset")),
    };
    Ok(rows.to_vec())
}

pub(crate) fn divide(a: i32, b: i32) -> Result<i32, Error> {
    if b == 0 {
        Err(Error::InvalidTexture("division by zero"))
    } else {
        Ok(a.wrapping_div(b))
    }
}

fn palette_cos(index: i32) -> i32 {
    let angle = 6.283185307179586 * (f64::from(index) / 255.0);
    (libm::cos(angle) * 4096.0) as i32
}

fn clamp_short(value: i32) -> i32 {
    let mut result = value;
    if result <= -32768 {
        result = -32767;
    }
    if result >= 32768 {
        result = 32767;
    }
    i32::from(result as i16)
}

fn build_curve(mode: i32, points: &[[i32; 2]]) -> Result<Vec<i32>, Error> {
    let at = |index: i32| -> [i32; 2] {
        let last = points.len() as i32 - 1;
        if index < 0 {
            let first = points[0];
            let second = points[1];
            [
                first[0] - second[0] + first[0],
                first[1] - second[1] + first[1],
            ]
        } else if index > last {
            let before = points[points.len() - 2];
            let end = points[points.len() - 1];
            [
                before[0] - end[0] + before[0],
                before[1] - end[1] + before[1],
            ]
        } else {
            points[index as usize]
        }
    };
    let mut lookup = vec![0i32; 257];
    for (i, slot) in lookup.iter_mut().enumerate() {
        let input = (i as i32) << 4;
        let mut segment = 1usize;
        while segment < points.len() - 1 && points[segment][0] <= input {
            segment += 1;
        }
        let start = points[segment - 1];
        let end = points[segment];
        let t = divide(
            input.wrapping_sub(start[0]) << 12,
            end[0].wrapping_sub(start[0]),
        )?;
        *slot = match mode {
            1 => {
                let index = ((t >> 5) & 0xff) as usize;
                let weight_b = (4096 - palette_cos(index as i32)) >> 1;
                let weight_a = 4096 - weight_b;
                clamp_short(
                    weight_b
                        .wrapping_mul(end[1])
                        .wrapping_add(weight_a.wrapping_mul(start[1]))
                        >> 12,
                )
            }
            2 => {
                let before = at(segment as i32 - 2)[1];
                let after = at(segment as i32 + 1)[1];
                let t_squared = t.wrapping_mul(t) >> 12;
                let coeff_a =
                    start[1].wrapping_add(after.wrapping_sub(end[1]).wrapping_sub(before));
                let coeff_b = before.wrapping_sub(start[1]).wrapping_sub(coeff_a);
                let coeff_c = end[1].wrapping_sub(before);
                let cubic = t_squared.wrapping_mul(coeff_a.wrapping_mul(t) >> 12) >> 12;
                let quad = coeff_b.wrapping_mul(t_squared) >> 12;
                let linear = t.wrapping_mul(coeff_c) >> 12;
                clamp_short(
                    cubic
                        .wrapping_add(quad)
                        .wrapping_add(linear)
                        .wrapping_add(start[1]),
                )
            }
            _ => {
                let inverse = 4096 - t;
                clamp_short(
                    t.wrapping_mul(end[1])
                        .wrapping_add(inverse.wrapping_mul(start[1]))
                        >> 12,
                )
            }
        };
    }
    Ok(lookup)
}

fn build_gradient(stops: &[[i32; 4]]) -> Result<Vec<i32>, Error> {
    let count = stops.len();
    let mut lookup = vec![0i32; 257];
    if count == 0 {
        return Ok(lookup);
    }
    for (i, slot) in lookup.iter_mut().enumerate() {
        let input = (i as i32) << 4;
        let mut index = 0usize;
        let mut j = 0usize;
        while j < count && input >= stops[j][0] {
            index += 1;
            j += 1;
        }
        let (a, b, c);
        if index < count {
            let current = stops[index];
            if index > 0 {
                let previous = stops[index - 1];
                let lerp = divide(
                    input.wrapping_sub(previous[0]) << 12,
                    current[0].wrapping_sub(previous[0]),
                )?;
                let inverse = 4096 - lerp;
                let mix = |k: usize| {
                    lerp.wrapping_mul(current[k])
                        .wrapping_add(inverse.wrapping_mul(previous[k]))
                        >> 12
                };
                a = mix(1);
                b = mix(2);
                c = mix(3);
            } else {
                a = current[1];
                b = current[2];
                c = current[3];
            }
        } else {
            let last = stops[count - 1];
            a = last[1];
            b = last[2];
            c = last[3];
        }
        let red = (a >> 4).clamp(0, 255);
        let green = (b >> 4).clamp(0, 255);
        let blue = (c >> 4).clamp(0, 255);
        *slot = (red << 16) | (green << 8) | blue;
    }
    Ok(lookup)
}

fn compute_light(azimuth: i32, elevation: i32) -> [i32; 3] {
    let cos_elevation = libm::cos(f64::from(elevation) / 4096.0);
    let mut light = [
        (libm::sin(f64::from(azimuth) / 4096.0) * cos_elevation * 4096.0) as i32,
        (libm::cos(f64::from(azimuth) / 4096.0) * cos_elevation * 4096.0) as i32,
        (libm::sin(f64::from(elevation) / 4096.0) * 4096.0) as i32,
    ];
    let xx = light[0].wrapping_mul(light[0]) >> 12;
    let yy = light[1].wrapping_mul(light[1]) >> 12;
    let zz = light[2].wrapping_mul(light[2]) >> 12;
    let magnitude =
        (libm::sqrt(f64::from(xx.wrapping_add(yy).wrapping_add(zz) >> 12)) * 4096.0) as i32;
    if magnitude != 0 {
        for value in light.iter_mut() {
            *value = (*value << 12).wrapping_div(magnitude);
        }
    }
    light
}

fn emboss_table() -> &'static [u8] {
    static TABLE: std::sync::OnceLock<Vec<u8>> = std::sync::OnceLock::new();
    TABLE.get_or_init(build_emboss_table)
}

fn build_emboss_table() -> Vec<u8> {
    let mut table = Vec::with_capacity(32896);
    for row in 0..256i32 {
        for col in 0..=row {
            let value = 255.0 / libm::sqrt(f64::from(col * col + row * row + 65535) / 65535.0);
            table.push(value as i32 as u8);
        }
    }
    table
}

fn compute_perlin_steps(p: &mut Perlin) -> Result<(), Error> {
    let steps = p.steps.max(0) as usize;
    if p.amplitude > 0 {
        let mut multipliers = Vec::with_capacity(steps);
        let mut amplitudes = Vec::with_capacity(steps);
        for i in 0..steps {
            let base = f64::from(p.amplitude as f32 / 4096.0f32);
            multipliers.push((libm::pow(base, i as f64) * 4096.0) as i32 as i16);
            amplitudes.push(libm::pow(2.0, i as f64) as i32 as i16);
        }
        p.step_multipliers = Some(multipliers);
        p.amplitude_multipliers = Some(amplitudes);
    } else if p
        .step_multipliers
        .as_ref()
        .is_some_and(|m| m.len() == steps)
    {
        let mut amplitudes = Vec::with_capacity(steps);
        for i in 0..steps {
            amplitudes.push(libm::pow(2.0, i as f64) as i32 as i16);
        }
        p.amplitude_multipliers = Some(amplitudes);
    }
    Ok(())
}

pub(crate) fn post_decode(kind: &mut Kind) -> Result<(), Error> {
    match kind {
        Kind::Square(q) => {
            let size = divide(4096, q.steps)?;
            let pulse = size.wrapping_mul(q.duty) >> 12;
            let count = q.steps as usize;
            q.pulse_end = vec![0; count + 1];
            q.step_start = vec![0; count + 1];
            let mut position = 0;
            for step in 0..count {
                q.step_start[step] = position;
                q.pulse_end[step] = pulse.wrapping_add(position);
                position += size;
            }
            q.step_start[count] = 4096;
            q.pulse_end[count] = q.pulse_end[0].wrapping_add(4096);
        }
        Kind::Bricks(b) => {
            let mut random = JavaRandom::new(i64::from(b.rows));
            b.half_mortar = b.mortar / 2;
            b.column_width = divide(4096, b.columns)?;
            let row_height = divide(4096, b.rows)?;
            let half_column = b.column_width / 2;
            let half_row = row_height / 2;
            let (rows, columns) = (b.rows as usize, b.columns as usize);
            b.row_bounds = vec![0; rows + 1];
            b.column_bounds = vec![vec![0; columns + 1]; rows];
            b.brightness = vec![vec![0; columns]; rows];
            for row in 0..rows {
                if row > 0 {
                    let variation =
                        (random.bounded(4096)? - 2048).wrapping_mul(b.row_variation) >> 12;
                    let adjusted = row_height.wrapping_add(variation.wrapping_mul(half_row) >> 12);
                    b.row_bounds[row] = adjusted.wrapping_add(b.row_bounds[row - 1]);
                }
                for col in 0..columns {
                    if col > 0 {
                        let variation =
                            (random.bounded(4096)? - 2048).wrapping_mul(b.column_variation) >> 12;
                        let adjusted = b
                            .column_width
                            .wrapping_add(variation.wrapping_mul(half_column) >> 12);
                        b.column_bounds[row][col] =
                            b.column_bounds[row][col - 1].wrapping_add(adjusted);
                    }
                    b.brightness[row][col] = if b.brightness_variation > 0 {
                        4096 - random.bounded(b.brightness_variation)?
                    } else {
                        4096
                    };
                }
                b.column_bounds[row][columns] = 4096;
            }
            b.row_bounds[rows] = 4096;
        }
        Kind::Curve {
            mode,
            points,
            lookup,
        } => {
            if points.is_none() {
                *points = Some(vec![[0, 0], [4096, 4096]]);
            }
            let list = points.as_ref().ok_or(Error::InvalidTexture("curve"))?;
            if list.len() < 2 {
                return Err(Error::InvalidTexture("curve points"));
            }
            *lookup = build_curve(*mode, list)?;
        }
        Kind::ColorGradient { stops, lookup } => {
            if stops.is_none() {
                *stops = Some(gradient_preset(1)?);
            }
            let list = stops.as_ref().ok_or(Error::InvalidTexture("gradient"))?;
            *lookup = build_gradient(list)?;
        }
        Kind::Range {
            multiplier,
            offset,
            max,
        } => *multiplier = *max - *offset,
        Kind::Emboss {
            azimuth,
            elevation,
            light,
            ..
        } => *light = compute_light(*azimuth, *elevation),
        Kind::Perlin(p) => {
            p.noise = JavaRandom::shuffled_table(p.seed)?;
            compute_perlin_steps(p)?;
            let mut i = p.steps - 1;
            while i >= 1 {
                let multipliers = p
                    .step_multipliers
                    .as_ref()
                    .ok_or(Error::InvalidTexture("perlin multipliers"))?;
                let value = *multipliers
                    .get(i as usize)
                    .ok_or(Error::InvalidTexture("perlin multipliers"))?;
                if (-8..=8).contains(&value) {
                    p.steps -= 1;
                    i -= 1;
                } else {
                    break;
                }
            }
        }
        Kind::Voronoi(v) => {
            v.permutation = JavaRandom::shuffled_table(v.seed)?;
            let mut random = JavaRandom::new(i64::from(v.seed));
            v.points = vec![0; 512];
            if v.jitter > 0 {
                for slot in v.points.iter_mut() {
                    *slot = random.bounded(v.jitter)? as i16;
                }
            }
        }
        _ => {}
    }
    Ok(())
}

fn blend(mode: i32, a: i32, b: i32) -> Result<i32, Error> {
    Ok(match mode {
        1 => b.wrapping_add(a),
        2 => a.wrapping_sub(b),
        3 => a.wrapping_mul(b) >> 12,
        4 => {
            if b != 0 {
                divide(a << 12, b)?
            } else {
                4096
            }
        }
        5 => 4096 - ((4096 - a).wrapping_mul(4096 - b) >> 12),
        6 => {
            if b < 2048 {
                b.wrapping_mul(a) >> 11
            } else {
                4096 - ((4096 - a).wrapping_mul(4096 - b) >> 11)
            }
        }
        7 => {
            if a == 4096 {
                4096
            } else {
                divide(b << 12, 4096 - a)?
            }
        }
        8 => {
            if a == 0 {
                0
            } else {
                4096 - divide((4096 - b) << 12, a)?
            }
        }
        9 => a.min(b),
        10 => a.max(b),
        11 => {
            if a > b {
                a.wrapping_sub(b)
            } else {
                b.wrapping_sub(a)
            }
        }
        12 => b.wrapping_add(a).wrapping_sub(a.wrapping_mul(b) >> 11),
        _ => return Err(Error::InvalidTexture("combine mode")),
    })
}

fn hash_noise(px: i32, py: i32) -> i32 {
    let mut hash = py.wrapping_mul(57).wrapping_add(px);
    hash ^= hash << 1;
    let value = hash
        .wrapping_mul(hash)
        .wrapping_mul(15731)
        .wrapping_add(789221)
        .wrapping_mul(hash)
        .wrapping_add(1376312589)
        & 0x7fff_ffff;
    4096 - value / 262144
}

fn rgb_to_hsl(red: i32, green: i32, blue: i32) -> Result<(i32, i32, i32), Error> {
    let max = red.max(green).max(blue);
    let min = red.min(green).min(blue);
    let chroma = max - min;
    let lightness = (min + max) / 2;
    let saturation = if lightness > 0 && lightness < 4096 {
        divide(
            chroma << 12,
            if lightness <= 2048 {
                lightness * 2
            } else {
                8192 - lightness * 2
            },
        )?
    } else {
        0
    };
    let hue = if chroma > 0 {
        let dist_red = ((max - red) << 12) / chroma;
        let dist_green = ((max - green) << 12) / chroma;
        let dist_blue = ((max - blue) << 12) / chroma;
        let raw = if max == red {
            if min == green {
                dist_blue + 20480
            } else {
                4096 - dist_green
            }
        } else if max == green {
            if blue == min {
                dist_red + 4096
            } else {
                12288 - dist_blue
            }
        } else if min == red {
            dist_green + 12288
        } else {
            20480 - dist_red
        };
        raw / 6
    } else {
        0
    };
    Ok((hue, saturation, lightness))
}

fn hsl_to_rgb(h: i32, s: i32, l: i32, held: &mut [i32; 3]) {
    let chroma = if l <= 2048 {
        l.wrapping_mul(s + 4096) >> 12
    } else {
        l + s - (l.wrapping_mul(s) >> 12)
    };
    if chroma > 0 {
        let adjusted = h * 6;
        let minimum = l + l - chroma;
        let range = ((chroma - minimum) << 12) / chroma;
        let region = adjusted >> 12;
        let fraction = adjusted - (region << 12);
        let mut offset = range.wrapping_mul(chroma) >> 12;
        offset = fraction.wrapping_mul(offset) >> 12;
        let ascending = minimum + offset;
        let descending = chroma - offset;
        match region {
            0 => *held = [chroma, ascending, minimum],
            1 => *held = [descending, chroma, minimum],
            2 => *held = [minimum, chroma, ascending],
            3 => *held = [minimum, descending, chroma],
            4 => *held = [ascending, minimum, chroma],
            5 => *held = [chroma, minimum, descending],
            _ => {}
        }
    } else {
        *held = [l, l, l.wrapping_mul(-680551435)];
    }
}

fn at(row: &[i32], index: i32) -> Result<i32, Error> {
    usize::try_from(index)
        .ok()
        .and_then(|i| row.get(i).copied())
        .ok_or(Error::InvalidTexture("index out of range"))
}

#[allow(clippy::too_many_arguments)]
fn noise_value(
    p: &Perlin,
    sine: &[i32],
    coord_x: i32,
    coord_y: i32,
    row_a: i32,
    row_b: i32,
    lerp_y: i32,
    range_x: i32,
) -> Result<i32, Error> {
    let n = |index: i32| -> Result<i32, Error> {
        usize::try_from(index)
            .ok()
            .and_then(|i| p.noise.get(i).copied())
            .map(i32::from)
            .ok_or(Error::InvalidTexture("noise index"))
    };
    let mut pos_x = coord_x >> 12;
    let mut next_x = pos_x + 1;
    if next_x >= range_x {
        next_x = 0;
    }
    let frac_x = coord_x & 0xfff;
    pos_x &= 0xff;
    next_x &= 0xff;
    let neg_x = frac_x - 4096;
    let neg_y = coord_y - 4096;
    let lerp_x = at(sine, frac_x)?;
    let pick = |kind: i32, c: i32, f: i32| -> i32 {
        if kind <= 1 {
            if kind == 0 {
                c + f
            } else {
                c - f
            }
        } else if kind == 2 {
            f - c
        } else {
            -f - c
        }
    };
    let g = n(row_a + pos_x)? & 3;
    let grad_a = pick(g, coord_y, frac_x);
    let g = n(row_a + next_x)? & 3;
    let grad_b = pick(g, coord_y, neg_x);
    let interp_x = grad_a.wrapping_add(lerp_x.wrapping_mul(grad_b.wrapping_sub(grad_a)) >> 12);
    let g = n(pos_x + row_b)? & 3;
    let grad_a = pick(g, neg_y, frac_x);
    let g = n(next_x + row_b)? & 3;
    let grad_b = pick(g, neg_y, neg_x);
    let interp_y = grad_a.wrapping_add(lerp_x.wrapping_mul(grad_b.wrapping_sub(grad_a)) >> 12);
    Ok(interp_x.wrapping_add(lerp_y.wrapping_mul(interp_y.wrapping_sub(interp_x)) >> 12))
}

impl Eval<'_> {
    fn child_slot(&mut self, node: &Node, index: usize, y: i32) -> Result<(usize, usize), Error> {
        let id = *node
            .children
            .get(index)
            .ok_or(Error::InvalidTexture("missing child"))?;
        let (_, slot) = self.rows_slot(id, y)?;
        Ok((id, slot))
    }

    fn mono_child(&mut self, node: &Node, index: usize, y: i32) -> Result<Vec<i32>, Error> {
        let rows = self.child(node, index, y)?;
        Ok(rows[0].clone())
    }

    fn color_child(&mut self, node: &Node, index: usize, y: i32) -> Result<[Vec<i32>; 3], Error> {
        let rows = self.child(node, index, y)?;
        if rows.len() == 1 {
            Ok([rows[0].clone(), rows[0].clone(), rows[0].clone()])
        } else {
            Ok([rows[0].clone(), rows[1].clone(), rows[2].clone()])
        }
    }

    fn wrap_x(&self, x: i32) -> usize {
        (x & self.row_end) as usize
    }

    fn kaleidoscope(&mut self, x: i32, y: i32) {
        let horizontal = self.horizontal[x as usize];
        let vertical = self.vertical[y as usize];
        let angle =
            f64::from(libm::atan2(f64::from(horizontal - 2048), f64::from(vertical - 2048)) as f32);
        let pi = std::f64::consts::PI;
        let quarter = -2.356194490192345;
        let (w, h) = (self.width as i32, self.height as i32);
        let mirror = &mut self.state.mirror;
        if angle >= -pi && angle <= quarter {
            *mirror = [x, y];
        } else if angle <= -1.5707963267948966 && angle >= quarter {
            *mirror = [y, x];
        } else if angle <= -0.7853981633974483 && angle >= -1.5707963267948966 {
            *mirror = [w - y, x];
        } else if angle <= 0.0 && angle >= -0.7853981633974483 {
            *mirror = [x, h - y];
        } else if angle >= 0.0 && angle <= 0.7853981633974483 {
            *mirror = [w - x, h - y];
        } else if angle >= 0.7853981633974483 && angle <= 1.5707963267948966 {
            *mirror = [w - y, h - x];
        } else if angle >= 1.5707963267948966 && angle <= 2.356194490192345 {
            *mirror = [y, h - x];
        } else if angle >= 2.356194490192345 && angle <= pi {
            *mirror = [w - x, y];
        }
        mirror[0] &= self.row_end;
        mirror[1] &= self.column_end;
    }

    pub(crate) fn compute(
        &mut self,
        node: &Node,
        id: usize,
        y: i32,
    ) -> Result<Vec<Vec<i32>>, Error> {
        let width = self.width;
        let mono = node.mono;
        match &node.kind {
            Kind::MonochromeFill(fill) => Ok(vec![vec![*fill; width]]),
            Kind::ColorFill(color) => Ok(color.iter().map(|c| vec![*c; width]).collect()),
            Kind::HorizontalGradient => Ok(vec![self.horizontal.clone()]),
            Kind::VerticalGradient => Ok(vec![vec![self.vertical[y as usize]; width]]),
            Kind::Curve { lookup, .. } => {
                let input = self.mono_child(node, 0, y)?;
                Ok(vec![input
                    .iter()
                    .map(|v| lookup[(v >> 4).clamp(0, 256) as usize])
                    .collect()])
            }
            Kind::ColorGradient { lookup, .. } => {
                let input = self.mono_child(node, 0, y)?;
                let mut out = vec![vec![0; width]; 3];
                for x in 0..width {
                    let packed = lookup[(input[x] >> 4).clamp(0, 256) as usize];
                    out[0][x] = (packed & 0xff0000) >> 12;
                    out[1][x] = (packed & 0xff00) >> 4;
                    out[2][x] = (packed & 0xff) << 4;
                }
                Ok(out)
            }
            Kind::Combine(mode) => {
                if mono {
                    let sa = self.child_slot(node, 0, y)?;
                    let sb = self.child_slot(node, 1, y)?;
                    let a = self.slot_rows(sa.0, sa.1)?;
                    let b = self.slot_rows(sb.0, sb.1)?;
                    let mut out = Vec::with_capacity(width);
                    for x in 0..width {
                        out.push(blend(*mode, a[0][x], b[0][x])?);
                    }
                    Ok(vec![out])
                } else {
                    let sa = self.child_slot(node, 0, y)?;
                    let sb = self.child_slot(node, 1, y)?;
                    let a = self.slot_rows(sa.0, sa.1)?;
                    let b = self.slot_rows(sb.0, sb.1)?;
                    let mut out = vec![Vec::new(); 3];
                    for c in 0..3 {
                        let (ac, bc) = (channel_of(&a, c), channel_of(&b, c));
                        for x in 0..width {
                            out[c].push(blend(*mode, ac[x], bc[x])?);
                        }
                    }
                    Ok(out)
                }
            }
            Kind::Interpolate => {
                let sf = self.child_slot(node, 2, y)?;
                let sa = self.child_slot(node, 0, y)?;
                let sb = self.child_slot(node, 1, y)?;
                let factor_rows = self.slot_rows(sf.0, sf.1)?;
                let a_rows = self.slot_rows(sa.0, sa.1)?;
                let b_rows = self.slot_rows(sb.0, sb.1)?;
                let factor = &factor_rows[0];
                if mono {
                    let (a, b) = (&a_rows[0], &b_rows[0]);
                    let out = (0..width)
                        .map(|x| {
                            let f = factor[x];
                            if f == 4096 {
                                a[x]
                            } else if f == 0 {
                                b[x]
                            } else {
                                (4096 - f)
                                    .wrapping_mul(b[x])
                                    .wrapping_add(f.wrapping_mul(a[x]))
                                    >> 12
                            }
                        })
                        .collect();
                    Ok(vec![out])
                } else {
                    let mut out = vec![vec![0; width]; 3];
                    for c in 0..3 {
                        let (ac, bc) = (channel_of(&a_rows, c), channel_of(&b_rows, c));
                        for x in 0..width {
                            let f = factor[x];
                            out[c][x] = if f == 4096 {
                                ac[x]
                            } else if f == 0 {
                                bc[x]
                            } else {
                                f.wrapping_mul(ac[x])
                                    .wrapping_add((4096 - f).wrapping_mul(bc[x]))
                                    >> 12
                            };
                        }
                    }
                    Ok(out)
                }
            }
            Kind::Clamp { min, max } => {
                let f = |v: i32| if v < *min { *min } else { v.min(*max) };
                if mono {
                    let a = self.mono_child(node, 0, y)?;
                    Ok(vec![a.iter().map(|v| f(*v)).collect()])
                } else {
                    let a = self.color_child(node, 0, y)?;
                    Ok(a.iter()
                        .map(|ch| ch.iter().map(|v| f(*v)).collect())
                        .collect())
                }
            }
            Kind::Range {
                multiplier, offset, ..
            } => {
                let f = |v: i32| (multiplier.wrapping_mul(v) >> 12).wrapping_add(*offset);
                if mono {
                    let a = self.mono_child(node, 0, y)?;
                    Ok(vec![a.iter().map(|v| f(*v)).collect()])
                } else {
                    let a = self.color_child(node, 0, y)?;
                    Ok(a.iter()
                        .map(|ch| ch.iter().map(|v| f(*v)).collect())
                        .collect())
                }
            }
            Kind::Invert => {
                if mono {
                    let a = self.mono_child(node, 0, y)?;
                    Ok(vec![a.iter().map(|v| 4096 - v).collect()])
                } else {
                    let a = self.color_child(node, 0, y)?;
                    Ok(a.iter()
                        .map(|ch| ch.iter().map(|v| 4096 - v).collect())
                        .collect())
                }
            }
            Kind::Flip {
                horizontal,
                vertical,
            } => {
                let source_y = if *vertical { self.column_end - y } else { y };
                let rows = self.child(node, 0, source_y)?;
                let count = if mono { 1 } else { 3 };
                let mut out = Vec::with_capacity(count);
                for c in 0..count {
                    let input = if mono { &rows[0] } else { channel_of(&rows, c) };
                    if *horizontal {
                        out.push(
                            (0..width)
                                .map(|i| input[self.row_end as usize - i])
                                .collect(),
                        );
                    } else {
                        out.push(input[..width].to_vec());
                    }
                }
                Ok(out)
            }
            Kind::Tile {
                x: count_x,
                y: count_y,
            } => {
                let tile_width = divide(width as i32, *count_x)?;
                let tile_height = divide(self.height as i32, *count_y)?;
                let source_y = if tile_height > 0 {
                    (y % tile_height) * self.height as i32 / tile_height
                } else {
                    0
                };
                let rows = self.child(node, 0, source_y)?;
                let count = if mono { 1 } else { 3 };
                let mut out = Vec::with_capacity(count);
                for c in 0..count {
                    let input = if mono { &rows[0] } else { channel_of(&rows, c) };
                    out.push(
                        (0..width as i32)
                            .map(|x| {
                                if tile_width > 0 {
                                    input[((x % tile_width) * width as i32 / tile_width) as usize]
                                } else {
                                    input[0]
                                }
                            })
                            .collect(),
                    );
                }
                Ok(out)
            }
            Kind::Noise => {
                let vertical = self.vertical[y as usize];
                Ok(vec![(0..width)
                    .map(|x| hash_noise(self.horizontal[x], vertical) % 4096)
                    .collect()])
            }
            Kind::Polar(strength) => {
                let sa = self.child_slot(node, 1, y)?;
                let sr = self.child_slot(node, 2, y)?;
                let count = if mono { 1 } else { 3 };
                let mut out = vec![vec![0; width]; count];
                for x in 0..width {
                    let angle_value = self.slot_rows(sa.0, sa.1)?[0][x];
                    let radius_value = self.slot_rows(sr.0, sr.1)?[0][x];
                    let angle = if mono {
                        (angle_value >> 4) & 0xff
                    } else {
                        (angle_value.wrapping_mul(255) >> 12) & 0xff
                    } as usize;
                    let radius = strength.wrapping_mul(radius_value) >> 12;
                    let offset_x = self.palette_cos[angle].wrapping_mul(radius) >> 12;
                    let offset_y = self.palette_sin[angle].wrapping_mul(radius) >> 12;
                    let sampled_x =
                        ((x as i32).wrapping_add(offset_x >> 12) & self.row_end) as usize;
                    let sampled_y = (offset_y >> 12).wrapping_add(y) & self.column_end;
                    let rows = self.child(node, 0, sampled_y)?;
                    for c in 0..count {
                        out[c][x] = if mono {
                            rows[0][sampled_x]
                        } else {
                            channel_of(&rows, c)[sampled_x]
                        };
                    }
                }
                Ok(out)
            }
            Kind::Hsl {
                hue_adjust,
                saturation_adjust,
                lightness_adjust,
            } => {
                let input = self.color_child(node, 0, y)?;
                if self.state.hsl.len() <= id {
                    self.state.hsl.resize(id + 1, [0; 3]);
                }
                let mut held = self.state.hsl[id];
                let mut out = vec![vec![0; width]; 3];
                for x in 0..width {
                    let (mut hue, mut saturation, mut lightness) =
                        rgb_to_hsl(input[0][x], input[1][x], input[2][x])?;
                    hue = hue.wrapping_add(*hue_adjust);
                    saturation = saturation.wrapping_add(*saturation_adjust);
                    lightness = lightness.wrapping_add(*lightness_adjust);
                    while hue < 0 {
                        hue += 4096;
                    }
                    while hue > 4096 {
                        hue -= 4096;
                    }
                    saturation = saturation.clamp(0, 4096);
                    lightness = lightness.clamp(0, 4096);
                    hsl_to_rgb(hue, saturation, lightness, &mut held);
                    out[0][x] = held[0];
                    out[1][x] = held[1];
                    out[2][x] = held[2];
                }
                self.state.hsl[id] = held;
                Ok(out)
            }
            Kind::Binary { lower, upper } => {
                let a = self.mono_child(node, 0, y)?;
                Ok(vec![a
                    .iter()
                    .map(|v| if v >= lower && v <= upper { 4096 } else { 0 })
                    .collect()])
            }
            Kind::EdgeDetector(strength) => {
                let sp = self.child_slot(node, 0, (y - 1) & self.column_end)?;
                let sc = self.child_slot(node, 0, y)?;
                let sn = self.child_slot(node, 0, (y + 1) & self.column_end)?;
                let prev = self.slot_rows(sp.0, sp.1)?;
                let current = self.slot_rows(sc.0, sc.1)?;
                let next = self.slot_rows(sn.0, sn.1)?;
                let mut out = Vec::with_capacity(width);
                for x in 0..width {
                    let gradient_y = (next[0][x].wrapping_sub(prev[0][x])).wrapping_mul(*strength);
                    let gradient_x = current[0][self.wrap_x(x as i32 + 1)]
                        .wrapping_sub(current[0][self.wrap_x(x as i32 - 1)])
                        .wrapping_mul(*strength);
                    let sx = gradient_x >> 12;
                    let sy = gradient_y >> 12;
                    let xx = sx.wrapping_mul(sx) >> 12;
                    let yy = sy.wrapping_mul(sy) >> 12;
                    let length =
                        (libm::sqrt(f64::from(yy.wrapping_add(xx).wrapping_add(4096)) / 4096.0)
                            * 4096.0) as i32;
                    let inverse = if length != 0 { 16777216 / length } else { 0 };
                    out.push(4096 - inverse);
                }
                Ok(vec![out])
            }
            Kind::Square(q) => {
                let vertical = self.vertical[y as usize];
                let find = |coord: i32| -> i32 {
                    for step in 0..q.steps as usize {
                        if coord >= q.step_start[step] && coord < q.step_start[step + 1] {
                            return if coord < q.pulse_end[step] { 4096 } else { 0 };
                        }
                    }
                    0
                };
                if q.axis != 0 {
                    Ok(vec![(0..width)
                        .map(|x| {
                            let horizontal = self.horizontal[x];
                            let coord = match q.axis {
                                1 => horizontal,
                                2 => (horizontal.wrapping_sub(4096 - vertical) >> 1) + 2048,
                                3 => (horizontal.wrapping_sub(vertical) >> 1) + 2048,
                                _ => 0,
                            };
                            find(coord)
                        })
                        .collect()])
                } else {
                    Ok(vec![vec![find(vertical); width]])
                }
            }
            Kind::Bricks(b) => {
                let mut vertical = self.vertical[y as usize].wrapping_add(b.vertical_offset);
                while vertical < 0 {
                    vertical += 4096;
                }
                while vertical > 4096 {
                    vertical -= 4096;
                }
                let mut row_index = 0usize;
                while (row_index as i32) < b.rows && vertical >= b.row_bounds[row_index] {
                    row_index += 1;
                }
                let brick_row = row_index - 1;
                let even = row_index & 1 == 0;
                let top = b.row_bounds[row_index];
                let bottom = b.row_bounds[row_index - 1];
                if vertical > bottom + b.half_mortar && vertical < top - b.half_mortar {
                    let mut out = Vec::with_capacity(width);
                    for x in 0..width {
                        let stagger = if even { b.stagger } else { -b.stagger };
                        let mut horizontal = self.horizontal[x]
                            .wrapping_add(stagger.wrapping_mul(b.column_width) >> 12);
                        while horizontal < 0 {
                            horizontal += 4096;
                        }
                        while horizontal > 4096 {
                            horizontal -= 4096;
                        }
                        let bounds = &b.column_bounds[brick_row];
                        let mut col = 0usize;
                        while (col as i32) < b.columns && horizontal >= bounds[col] {
                            col += 1;
                        }
                        let brick_col = col - 1;
                        let left = bounds[brick_col];
                        let right = bounds[col];
                        out.push(
                            if horizontal > left + b.half_mortar
                                && horizontal < right - b.half_mortar
                            {
                                b.brightness[brick_row][brick_col]
                            } else {
                                0
                            },
                        );
                    }
                    Ok(vec![out])
                } else {
                    Ok(vec![vec![0; width]])
                }
            }
            Kind::LineNoise(l) => {
                if width != self.height {
                    return Err(Error::InvalidTexture("line noise needs a square image"));
                }
                let mut image = vec![vec![0i32; width]; width];
                let mut random = JavaRandom::new(i64::from(l.seed));
                let half_spread = l.spread >> 1;
                let size = width as i32;
                for _ in 0..l.count {
                    let mut angle = if l.spread > 0 {
                        l.offset
                            .wrapping_add(random.bounded(l.spread)? - half_spread)
                    } else {
                        l.offset
                    };
                    angle = (angle >> 4) & 0xff;
                    let start_x = random.bounded(size)?;
                    let start_y = random.bounded(size)?;
                    let end_x = start_x.wrapping_add(
                        self.palette_cos[angle as usize].wrapping_mul(l.length) >> 12,
                    );
                    let end_y = start_y.wrapping_add(
                        self.palette_sin[angle as usize].wrapping_mul(l.length) >> 12,
                    );
                    let dy = end_y.wrapping_sub(start_y);
                    let dx = end_x.wrapping_sub(start_x);
                    if dx != 0 || dy != 0 {
                        let steep = dy.wrapping_abs() > dx.wrapping_abs();
                        let (mut x1, mut y1, mut x2, mut y2) = (start_x, start_y, end_x, end_y);
                        if steep {
                            x1 = start_y;
                            y1 = start_x;
                            x2 = end_y;
                            y2 = end_x;
                        }
                        if x1 > x2 {
                            std::mem::swap(&mut x1, &mut x2);
                            std::mem::swap(&mut y1, &mut y2);
                        }
                        let mut current_y = y1;
                        let delta_x = x2 - x1;
                        let mut delta_y = y2 - y1;
                        let mut error = -delta_x / 2;
                        let step = divide(2048, delta_x)?;
                        let base = 1024 - (random.bounded(4096)? >> 2);
                        let y_step = if y1 < y2 { 1 } else { -1 };
                        if delta_y < 0 {
                            delta_y = -delta_y;
                        }
                        for px in x1..x2 {
                            let brightness = base
                                .wrapping_add(step.wrapping_mul(px - x1))
                                .wrapping_add(1024);
                            let wrapped_x = (px & self.row_end) as usize;
                            let wrapped_y = (current_y & self.column_end) as usize;
                            if steep {
                                image[wrapped_y][wrapped_x] = brightness;
                            } else {
                                image[wrapped_x][wrapped_y] = brightness;
                            }
                            error += delta_y;
                            if error > 0 {
                                error -= delta_x;
                                current_y += y_step;
                            }
                        }
                    }
                }
                let row = image[y as usize].clone();
                self.fill_all(id, image.into_iter().map(|r| vec![r]).collect())?;
                Ok(vec![row])
            }
            Kind::IrregularBricks(b) => {
                let image = bricks::render(b, width, self.height, self.row_end)?;
                let current = image[y as usize].clone();
                self.fill_all(id, image.into_iter().map(|r| vec![r]).collect())?;
                Ok(vec![current])
            }
            Kind::Colorize(m) => {
                let input = self.color_child(node, 0, y)?;
                let mut out = vec![vec![0; width]; 3];
                for x in 0..width {
                    let (r, g, b) = (input[0][x], input[1][x], input[2][x]);
                    if r == g && g == b {
                        out[0][x] = r.wrapping_mul(m[0]) >> 12;
                        out[1][x] = g.wrapping_mul(m[1]) >> 12;
                        out[2][x] = b.wrapping_mul(m[2]) >> 12;
                    } else {
                        out[0][x] = m[0];
                        out[1][x] = m[1];
                        out[2][x] = m[2];
                    }
                }
                Ok(out)
            }
            Kind::Monochrome => {
                let input = self.color_child(node, 0, y)?;
                Ok(vec![(0..width)
                    .map(|x| {
                        input[2][x]
                            .wrapping_add(input[1][x])
                            .wrapping_add(input[0][x])
                            / 3
                    })
                    .collect()])
            }
            Kind::Brightness(b) => {
                let input = self.color_child(node, 0, y)?;
                let mut out = vec![vec![0; width]; 3];
                for x in 0..width {
                    let (r, g, bl) = (input[0][x], input[1][x], input[2][x]);
                    out[0][x] = r;
                    out[1][x] = g;
                    out[2][x] = bl;
                    if r.wrapping_sub(b.target[0]).wrapping_abs() > b.tolerance {
                        continue;
                    }
                    if g.wrapping_sub(b.target[1]).wrapping_abs() > b.tolerance {
                        continue;
                    }
                    if bl.wrapping_sub(b.target[2]).wrapping_abs() > b.tolerance {
                        continue;
                    }
                    out[0][x] = r.wrapping_mul(b.red) >> 12;
                    out[1][x] = g.wrapping_mul(b.green) >> 12;
                    out[2][x] = bl.wrapping_mul(b.blue) >> 12;
                }
                Ok(out)
            }
            Kind::ColorEdge {
                normalized,
                strength,
            } => {
                let sp = self.child_slot(node, 0, (y - 1) & self.column_end)?;
                let sc = self.child_slot(node, 0, y)?;
                let sn = self.child_slot(node, 0, (y + 1) & self.column_end)?;
                let prev = self.slot_rows(sp.0, sp.1)?;
                let current = self.slot_rows(sc.0, sc.1)?;
                let next = self.slot_rows(sn.0, sn.1)?;
                let mut out = vec![vec![0; width]; 3];
                for x in 0..width {
                    let vertical = next[0][x].wrapping_sub(prev[0][x]).wrapping_mul(*strength);
                    let horizontal = current[0][self.wrap_x(x as i32 + 1)]
                        .wrapping_sub(current[0][self.wrap_x(x as i32 - 1)])
                        .wrapping_mul(*strength);
                    let gx = horizontal >> 12;
                    let gy = vertical >> 12;
                    let xx = gx.wrapping_mul(gx) >> 12;
                    let yy = gy.wrapping_mul(gy) >> 12;
                    let sum = xx.wrapping_add(yy).wrapping_add(4096);
                    let ratio = f64::from((f64::from(sum) / 4096.0) as f32);
                    let magnitude = (libm::sqrt(ratio) * 4096.0) as i32;
                    let (red, green, blue) = if magnitude != 0 {
                        (
                            horizontal.wrapping_div(magnitude),
                            vertical.wrapping_div(magnitude),
                            16777216 / magnitude,
                        )
                    } else {
                        (0, 0, 0)
                    };
                    if *normalized {
                        out[0][x] = (red >> 1) + 2048;
                        out[1][x] = (green >> 1) + 2048;
                        out[2][x] = (blue >> 1) + 2048;
                    } else {
                        out[0][x] = red;
                        out[1][x] = green;
                        out[2][x] = blue;
                    }
                }
                Ok(out)
            }
            Kind::Waveform {
                wave,
                shape,
                frequency,
            } => {
                let vertical = self.vertical[y as usize];
                let vertical_offset = (vertical - 2048) >> 1;
                let mut out = vec![0; width];
                for x in 0..width {
                    let horizontal = self.horizontal[x];
                    let horizontal_offset = (horizontal - 2048) >> 1;
                    let mut value = if *wave == 0 {
                        horizontal.wrapping_sub(vertical).wrapping_mul(*frequency)
                    } else {
                        let squared = vertical_offset
                            .wrapping_mul(vertical_offset)
                            .wrapping_add(horizontal_offset.wrapping_mul(horizontal_offset))
                            >> 12;
                        let ratio = f64::from(squared as f32 / 4096.0f32);
                        let distance = (libm::sqrt(ratio) * 4096.0) as i32;
                        (f64::from(distance.wrapping_mul(*frequency)) * std::f64::consts::PI) as i32
                    };
                    value = value.wrapping_sub(value & -4096);
                    match *shape {
                        0 => value = (self.palette_sin[((value >> 4) & 0xff) as usize] + 4096) >> 1,
                        2 => {
                            value -= 2048;
                            if value < 0 {
                                value = -value;
                            }
                            value = (2048 - value) << 1;
                        }
                        _ => {}
                    }
                    out[x] = value;
                }
                Ok(vec![out])
            }
            Kind::Weave(thread) => {
                let vertical = self.vertical[y as usize];
                let thread = *thread;
                let mut out = vec![0; width];
                for x in 0..width {
                    let horizontal = self.horizontal[x];
                    out[x] = if horizontal > thread
                        && horizontal < 4096 - thread
                        && vertical > 2048 - thread
                        && vertical < thread + 2048
                    {
                        4096 - divide((2048 - horizontal).abs() << 12, 2048 - thread)?
                    } else if horizontal > 2048 - thread && horizontal < thread + 2048 {
                        divide(((vertical - 2048).abs() - thread) << 12, 2048 - thread)?
                    } else if vertical >= thread && vertical <= 4096 - thread {
                        if horizontal >= thread && horizontal <= 4096 - thread {
                            0
                        } else {
                            4096 - divide((2048 - vertical).abs() << 12, 2048 - thread)?
                        }
                    } else {
                        divide(((horizontal - 2048).abs() - thread) << 12, 2048 - thread)?
                    };
                }
                Ok(vec![out])
            }
            Kind::DiagonalWaves(v) => {
                let vertical = self.vertical[y as usize] - 2048;
                let wrap = |value: i32| {
                    let value = if value < -2048 { value + 4096 } else { value };
                    if value > 2048 {
                        value - 4096
                    } else {
                        value
                    }
                };
                let (frequency, amplitude, decay) = (v[4], v[5], v[6]);
                let envelope = |phase: i32| -> Result<i32, Error> {
                    let cos = self.palette_cos[((phase.wrapping_mul(255) >> 12) & 0xff) as usize];
                    let step = divide(cos << 12, frequency)?;
                    let step = divide(step << 12, decay)?;
                    Ok(step.wrapping_mul(amplitude) >> 12)
                };
                let mut out = vec![0; width];
                for x in 0..width {
                    let horizontal = self.horizontal[x] - 2048;
                    let x1 = wrap(horizontal + v[0]);
                    let y1 = wrap(vertical + v[1]);
                    let x2 = wrap(horizontal + v[2]);
                    let y2 = wrap(vertical + v[3]);
                    let first = envelope((y1 - x1).wrapping_mul(frequency) >> 12)?;
                    let diagonal = y1 + x1 < first && y1 + x1 > -first;
                    let second = envelope((y2 + x2).wrapping_mul(frequency) >> 12)?;
                    let anti = y2 - x2 < second && y2 - x2 > -second;
                    out[x] = if !diagonal && !anti { 0 } else { 4096 };
                }
                Ok(vec![out])
            }
            Kind::Herringbone {
                scale_x,
                scale_y,
                threshold,
            } => {
                let vertical = self.vertical[y as usize];
                let mut out = vec![0; width];
                let step_x = divide(4096, *scale_x)?;
                let step_y = divide(4096, *scale_y)?;
                let remainder = |a: i32, b: i32| {
                    a.checked_rem(b)
                        .ok_or(Error::InvalidTexture("division by zero"))
                };
                for x in 0..width {
                    let horizontal = self.horizontal[x];
                    let mut tile_x = horizontal.wrapping_mul(*scale_x) >> 12;
                    let tile_y = vertical.wrapping_mul(*scale_y) >> 12;
                    let offset_x = remainder(horizontal, step_x)?.wrapping_mul(*scale_x);
                    let offset_y = remainder(vertical, step_y)?.wrapping_mul(*scale_y);
                    if offset_y < *threshold {
                        tile_x = (tile_x - tile_y).rem_euclid(4);
                        if tile_x != 1 || offset_x < *threshold {
                            out[x] = 0;
                            continue;
                        }
                    }
                    if offset_x < *threshold {
                        tile_x = (tile_x - tile_y).rem_euclid(4);
                        if tile_x > 0 {
                            out[x] = 0;
                            continue;
                        }
                    }
                    out[x] = 4096;
                }
                Ok(vec![out])
            }
            Kind::Kaleidoscope => {
                let count = if mono { 1 } else { 3 };
                let mut out = vec![vec![0; width]; count];
                for x in 0..width {
                    self.kaleidoscope(x as i32, y);
                    let [mx, my] = self.state.mirror;
                    let rows = self.child(node, 0, my)?;
                    for c in 0..count {
                        out[c][x] = if mono {
                            rows[0][mx as usize]
                        } else {
                            channel_of(&rows, c)[mx as usize]
                        };
                    }
                }
                Ok(out)
            }
            Kind::Rasterizer(shapes) => {
                let mut canvas = Canvas::new(width, self.height, self.row_end, self.column_end);
                for shape in shapes {
                    canvas.draw(shape)?;
                }
                let image: Vec<Vec<Vec<i32>>> = if mono {
                    canvas.rows.iter().map(|row| vec![row.clone()]).collect()
                } else {
                    canvas
                        .rows
                        .iter()
                        .map(|row| {
                            vec![
                                row.iter().map(|p| (p & 0xff0000) >> 12).collect(),
                                row.iter().map(|p| (p & 0xff00) >> 4).collect(),
                                row.iter().map(|p| (p & 0xff) << 4).collect(),
                            ]
                        })
                        .collect()
                };
                let current = image[y as usize].clone();
                self.fill_all(id, image)?;
                Ok(current)
            }
            Kind::BoxBlur { radius_x, radius_y } => self.box_blur(node, y, *radius_x, *radius_y),
            Kind::Emboss { depth, light, .. } => {
                let scaled = depth.wrapping_mul(self.scaled_width) >> 12;
                let previous_slot = self.child_slot(node, 0, (y - 1) & self.column_end)?;
                let current_slot = self.child_slot(node, 0, y)?;
                let next_slot = self.child_slot(node, 0, (y + 1) & self.column_end)?;
                let previous = self.slot_rows(previous_slot.0, previous_slot.1)?;
                let current = self.slot_rows(current_slot.0, current_slot.1)?;
                let next = self.slot_rows(next_slot.0, next_slot.1)?;
                let (previous, current, next) = (&previous[0], &current[0], &next[0]);
                let table = emboss_table();
                let mut out = vec![0; width];
                for x in 0..width {
                    let xi = x as i32;
                    let gradient_y = next[x].wrapping_sub(previous[x]).wrapping_mul(scaled) >> 12;
                    let gradient_x = scaled.wrapping_mul(
                        current[self.wrap_x(xi - 1)].wrapping_sub(current[self.wrap_x(xi + 1)]),
                    ) >> 12;
                    let abs_x = (gradient_x >> 4).abs().min(255);
                    let abs_y = (gradient_y >> 4).abs().min(255);
                    let scale = i32::from(
                        *table
                            .get((abs_x + ((abs_y * (abs_y + 1)) >> 1)) as usize)
                            .ok_or(Error::InvalidTexture("emboss table"))?,
                    );
                    let normal_x = gradient_x.wrapping_mul(scale) >> 8;
                    let normal_y = gradient_y.wrapping_mul(scale) >> 8;
                    let normal_z = (scale * 4096) >> 8;
                    let dot_x = normal_x.wrapping_mul(light[0]) >> 12;
                    let dot_y = light[1].wrapping_mul(normal_y) >> 12;
                    let dot_z = normal_z.wrapping_mul(light[2]) >> 12;
                    out[x] = dot_z.wrapping_add(dot_y).wrapping_add(dot_x);
                }
                Ok(vec![out])
            }
            Kind::Perlin(p) => self.perlin(p, y),
            Kind::Voronoi(v) => self.voronoi(v, y),
            Kind::Texture(material) => {
                if *material < 0 {
                    return Ok(vec![vec![0; width]; 3]);
                }
                if self.pixels[id].is_none() {
                    let (pixels, size) = self.nested_rgb(*material as u32)?;
                    self.pixels[id] = Some(std::rc::Rc::new((pixels, size, size)));
                }
                let image = self.pixels[id]
                    .clone()
                    .ok_or(Error::InvalidTexture("texture"))?;
                self.sample_image(&image, y)
            }
            Kind::Sprite(sprite) => {
                if *sprite < 0 {
                    return Ok(vec![vec![0; width]; 3]);
                }
                if self.pixels[id].is_none() {
                    let data = self
                        .sources()
                        .sprite(*sprite as u32)
                        .ok_or(Error::InvalidTexture("missing sprite"))?;
                    let (pixels, w, h) = super::sprite::decode_first(data)?;
                    self.pixels[id] = Some(std::rc::Rc::new((pixels, w, h)));
                }
                let image = self.pixels[id]
                    .clone()
                    .ok_or(Error::InvalidTexture("sprite"))?;
                self.sample_image(&image, y)
            }
        }
    }

    fn sample_image(
        &self,
        image: &(Vec<i32>, usize, usize),
        y: i32,
    ) -> Result<Vec<Vec<i32>>, Error> {
        let (pixels, w, h) = image;
        let (w, h) = (*w as i32, *h as i32);
        let (cw, ch) = (self.width as i32, self.height as i32);
        let row = (if h != ch { h * y / ch } else { y }) * w;
        let mut out = vec![vec![0; self.width]; 3];
        for x in 0..self.width {
            let sx = if w == cw { x as i32 } else { x as i32 * w / cw };
            let pixel = at(pixels, row + sx)?;
            out[2][x] = (pixel & 0xff) << 4;
            out[1][x] = (pixel & 0xff00) >> 4;
            out[0][x] = (pixel & 0xff0000) >> 12;
        }
        Ok(out)
    }

    fn box_blur(&mut self, node: &Node, y: i32, rx: i32, ry: i32) -> Result<Vec<Vec<i32>>, Error> {
        let width = self.width;
        let channels = if node.mono { 1 } else { 3 };
        let kernel_h = ry + ry + 1;
        let kernel_w = rx + rx + 1;
        let vertical_scale = 65536 / kernel_h;
        let horizontal_scale = 65536 / kernel_w;
        let mut buffers: Vec<Vec<Vec<i32>>> = Vec::new();
        for row in (y - ry)..=(y + ry) {
            let source = if channels == 1 {
                vec![self.mono_child(node, 0, row & self.column_end)?]
            } else {
                self.color_child(node, 0, row & self.column_end)?.to_vec()
            };
            let mut blurred = vec![vec![0; width]; channels];
            for c in 0..channels {
                let mut sum = 0i32;
                for col in -rx..=rx {
                    sum = sum.wrapping_add(source[c][self.wrap_x(col)]);
                }
                for col in 0..width as i32 {
                    blurred[c][col as usize] = sum.wrapping_mul(horizontal_scale) >> 16;
                    sum = sum.wrapping_sub(source[c][self.wrap_x(col - rx)]);
                    let add = if channels == 1 {
                        col + rx
                    } else {
                        col + 1 + rx
                    };
                    sum = sum.wrapping_add(source[c][self.wrap_x(add)]);
                }
            }
            buffers.push(blurred);
        }
        let mut out = vec![vec![0; width]; channels];
        for c in 0..channels {
            for x in 0..width {
                let mut sum = 0i32;
                for buffer in &buffers {
                    sum = sum.wrapping_add(buffer[c][x]);
                }
                out[c][x] = sum.wrapping_mul(vertical_scale) >> 16;
            }
        }
        Ok(out)
    }

    fn perlin(&mut self, p: &Perlin, y: i32) -> Result<Vec<Vec<i32>>, Error> {
        let width = self.width;
        let mut pixels = match self.prior.take() {
            Some(rows) if rows.len() == 1 && rows[0].len() == width => rows[0].clone(),
            _ => vec![0i32; width],
        };
        let scaled_y = self.vertical[y as usize].wrapping_mul(p.scale_y);
        let multipliers = p
            .step_multipliers
            .as_ref()
            .ok_or(Error::InvalidTexture("perlin multipliers"))?;
        let amplitudes = p
            .amplitude_multipliers
            .as_ref()
            .ok_or(Error::InvalidTexture("perlin amplitudes"))?;
        let step = |index: usize| -> Result<(i32, i32), Error> {
            let m = *multipliers
                .get(index)
                .ok_or(Error::InvalidTexture("perlin multipliers"))?;
            let a = *amplitudes
                .get(index)
                .ok_or(Error::InvalidTexture("perlin amplitudes"))?;
            Ok((i32::from(m), i32::from(a) << 12))
        };
        let sine = self.sine_wave.clone();
        let byte = |index: i32| -> Result<i32, Error> {
            p.noise
                .get((index & 0xff) as usize)
                .copied()
                .map(i32::from)
                .ok_or(Error::InvalidTexture("noise index"))
        };
        if p.steps == 1 {
            let (mult, scale) = step(0)?;
            let range_y = scale.wrapping_mul(p.scale_y) >> 12;
            let range_x = scale.wrapping_mul(p.scale_x) >> 12;
            let mut coord_y = scale.wrapping_mul(scaled_y) >> 12;
            let grid_y = coord_y >> 12;
            let mut next_y = grid_y + 1;
            if next_y >= range_y {
                next_y = 0;
            }
            coord_y &= 0xfff;
            let frac_y = at(&sine, coord_y)?;
            let row_a = byte(grid_y)?;
            let row_b = byte(next_y)?;
            for (x, slot) in pixels.iter_mut().enumerate() {
                let scaled_x = self.horizontal[x].wrapping_mul(p.scale_x);
                let value = noise_value(
                    p,
                    &sine,
                    scale.wrapping_mul(scaled_x) >> 12,
                    coord_y,
                    row_a,
                    row_b,
                    frac_y,
                    range_x,
                )?;
                *slot = if p.fog {
                    (mult.wrapping_mul(value) >> 12 >> 1) + 2048
                } else {
                    mult.wrapping_mul(value) >> 12
                };
            }
        } else {
            let (mult, scale) = step(0)?;
            if !(-8..=8).contains(&mult) {
                let range_y = scale.wrapping_mul(p.scale_y) >> 12;
                let range_x = scale.wrapping_mul(p.scale_x) >> 12;
                let mut coord_y = scaled_y.wrapping_mul(scale) >> 12;
                let grid_y = coord_y >> 12;
                let mut next_y = grid_y + 1;
                if next_y >= range_y {
                    next_y = 0;
                }
                coord_y &= 0xfff;
                let frac_y = at(&sine, coord_y)?;
                let row_a = byte(grid_y)?;
                let row_b = byte(next_y)?;
                for (x, slot) in pixels.iter_mut().enumerate() {
                    let scaled_x = self.horizontal[x].wrapping_mul(p.scale_x);
                    let value = noise_value(
                        p,
                        &sine,
                        scale.wrapping_mul(scaled_x) >> 12,
                        coord_y,
                        row_a,
                        row_b,
                        frac_y,
                        range_x,
                    )?;
                    *slot = value.wrapping_mul(mult) >> 12;
                }
            }
            for index in 1..p.steps.max(0) as usize {
                let (mult, scale) = step(index)?;
                if !(-8..=8).contains(&mult) {
                    let range_y = scale.wrapping_mul(p.scale_y) >> 12;
                    let range_x = scale.wrapping_mul(p.scale_x) >> 12;
                    let coord = scaled_y.wrapping_mul(scale) >> 12;
                    let grid_y = coord >> 12;
                    let mut next_y = grid_y + 1;
                    if next_y >= range_y {
                        next_y = 0;
                    }
                    let coord_y = coord & 0xfff;
                    let frac_y = at(&sine, coord_y)?;
                    let row_a = byte(grid_y)?;
                    let row_b = byte(next_y)?;
                    let last = p.fog && index as i32 == p.steps - 1;
                    for (x, slot) in pixels.iter_mut().enumerate() {
                        let scaled_x = self.horizontal[x].wrapping_mul(p.scale_x);
                        let value = noise_value(
                            p,
                            &sine,
                            scale.wrapping_mul(scaled_x) >> 12,
                            coord_y,
                            row_a,
                            row_b,
                            frac_y,
                            range_x,
                        )?;
                        if last {
                            let accumulated = slot.wrapping_add(mult.wrapping_mul(value) >> 12);
                            *slot = (accumulated >> 1) + 2048;
                        } else {
                            *slot = slot.wrapping_add(value.wrapping_mul(mult) >> 12);
                        }
                    }
                }
            }
        }
        Ok(vec![pixels])
    }

    fn voronoi(&mut self, v: &Voronoi, y: i32) -> Result<Vec<Vec<i32>>, Error> {
        let width = self.width;
        let mut out = vec![0i32; width];
        let vertical = self.vertical[y as usize]
            .wrapping_mul(v.cells_y)
            .wrapping_add(2048);
        let cell_y0 = vertical >> 12;
        let perm = |index: i32| -> i32 { i32::from(v.permutation[(index & 0xff) as usize]) };
        for (x, slot) in out.iter_mut().enumerate() {
            let mut nearest = i32::MAX;
            let mut second = i32::MAX;
            let mut third = i32::MAX;
            let mut fourth = i32::MAX;
            let horizontal = self.horizontal[x]
                .wrapping_mul(v.cells_x)
                .wrapping_add(2048);
            let cell_x0 = horizontal >> 12;
            for cell_y in (cell_y0 - 1)..=(cell_y0 + 1) {
                let wrapped_y = if cell_y >= v.cells_y {
                    cell_y - v.cells_y
                } else {
                    cell_y
                };
                let y_index = perm(wrapped_y);
                for cell_x in (cell_x0 - 1)..=(cell_x0 + 1) {
                    let wrapped_x = if cell_x >= v.cells_x {
                        cell_x - v.cells_x
                    } else {
                        cell_x
                    };
                    let point = (2 * perm(y_index + wrapped_x)) as usize;
                    let mut dx = horizontal
                        .wrapping_sub(i32::from(v.points[point]).wrapping_add(cell_x << 12));
                    let mut dy = vertical
                        .wrapping_sub(i32::from(v.points[point + 1]).wrapping_add(cell_y << 12));
                    let distance = match v.metric {
                        1 => dy.wrapping_mul(dy).wrapping_add(dx.wrapping_mul(dx)) >> 12,
                        2 => dx.wrapping_abs().wrapping_add(dy.wrapping_abs()),
                        3 => dx.wrapping_abs().max(dy.wrapping_abs()),
                        4 => {
                            dx =
                                (libm::sqrt(f64::from(dx.wrapping_abs()) / 4096.0) * 4096.0) as i32;
                            dy =
                                (libm::sqrt(f64::from(dy.wrapping_abs()) / 4096.0) * 4096.0) as i32;
                            let sum = dx.wrapping_add(dy);
                            sum.wrapping_mul(sum) >> 12
                        }
                        5 => {
                            dx = dx.wrapping_mul(dx);
                            dy = dy.wrapping_mul(dy);
                            (libm::sqrt(libm::sqrt(f64::from(dy.wrapping_add(dx)) / 1.6777216E7))
                                * 4096.0) as i32
                        }
                        _ => {
                            (libm::sqrt(
                                f64::from(dx.wrapping_mul(dx).wrapping_add(dy.wrapping_mul(dy)))
                                    / 1.6777216E7,
                            ) * 4096.0) as i32
                        }
                    };
                    if distance < nearest {
                        fourth = third;
                        third = second;
                        second = nearest;
                        nearest = distance;
                    } else if distance < second {
                        fourth = third;
                        third = second;
                        second = distance;
                    } else if distance < third {
                        fourth = third;
                        third = distance;
                    } else if distance < fourth {
                        fourth = distance;
                    }
                }
            }
            *slot = match v.output {
                0 => nearest,
                1 => second,
                2 => second.wrapping_sub(nearest),
                3 => third,
                4 => fourth,
                _ => 0,
            };
        }
        Ok(vec![out])
    }
}

fn channel_of(rows: &[Vec<i32>], index: usize) -> &Vec<i32> {
    if rows.len() == 1 {
        &rows[0]
    } else {
        &rows[index]
    }
}
