pub mod camera;
pub mod frame;
pub mod math;
pub mod metrics;
pub mod orbit;
#[cfg(test)]
mod orbit_oracle;
pub mod pass;
pub mod scene;
pub mod scripted_camera;
#[cfg(test)]
mod scripted_camera_oracle;
pub mod shaders;
pub mod stream;
pub mod terrain;
pub mod ui_overlay;
pub mod uniforms;
pub mod water;

#[cfg(feature = "gpu")]
pub mod bloom;
#[cfg(feature = "gpu")]
pub mod display;
#[cfg(feature = "gpu")]
pub mod gpu;
#[cfg(feature = "gpu")]
pub mod pipelines;
#[cfg(feature = "gpu")]
pub mod renderer;
#[cfg(feature = "gpu")]
pub mod resources;
#[cfg(feature = "gpu")]
pub mod stats;
#[cfg(feature = "gpu")]
pub mod targets;

#[cfg(all(feature = "gpu", not(target_arch = "wasm32")))]
pub mod capture;
#[cfg(all(feature = "gpu", target_arch = "wasm32"))]
pub mod web;
