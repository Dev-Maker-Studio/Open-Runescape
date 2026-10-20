use wgpu::{Adapter, Device, DeviceDescriptor, Instance, InstanceDescriptor, Queue, RequestAdapterOptions, Surface};

pub struct Gpu {
    pub adapter: Adapter,
    pub device: Device,
    pub queue: Queue,
}

pub fn instance() -> Instance {
    Instance::new(InstanceDescriptor::new_without_display_handle_from_env())
}

impl Gpu {
    pub async fn new(instance: &Instance, surface: Option<&Surface<'_>>) -> Gpu {
        Gpu::try_new(instance, surface).await.expect("no adapter or device")
    }

    pub async fn try_new(instance: &Instance, surface: Option<&Surface<'_>>) -> Option<Gpu> {
        let adapter = instance.request_adapter(&RequestAdapterOptions { compatible_surface: surface, ..Default::default() }).await.ok()?;
        let (device, queue) = adapter.request_device(&DeviceDescriptor { required_limits: adapter.limits(), ..Default::default() }).await.ok()?;
        Some(Gpu { adapter, device, queue })
    }
}
