macro_rules! sources {
    ($($n:literal),*) => {
        pub const ALL: &[(&str, &str)] = &[$(($n, include_str!(concat!("../shaders/", $n, ".wgsl")))),*];
    };
}

sources!(
    "batched.vert", "batched.frag", "ground.vert", "ground.frag", "groundTextured.vert", "groundTextured.frag", "hdWater.vert",
    "hdWater.frag", "materialCubeSpec.vert", "materialCubeSpec.frag", "model.vert", "model.frag", "particle.vert", "particle.frag",
    "shadow.vert", "shadow.frag", "sprite.vert", "sprite.frag", "water.vert", "water.frag", "waterHD.vert", "waterHD.frag",
    "waterLapping.vert", "waterLapping.frag", "waterReflecting.vert", "waterReflecting.frag", "waterRippling.vert",
    "waterRippling.frag", "waterfall.vert", "waterfall.frag", "instanced.vert", "instanced.frag", "surfaceMask.vert", "surfaceMask.frag"
);

pub fn get(name: &str) -> &'static str {
    ALL.iter().find(|(n, _)| *n == name).map(|(_, s)| *s).unwrap()
}
