use crate::def::{Component, ComponentType, ContentType};

pub const MAX_DEPTH: usize = 64;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LayoutError {
    DivideByZero,
    TooDeep,
}

impl std::fmt::Display for LayoutError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::DivideByZero => f.write_str("component aspect ratio divides by zero"),
            Self::TooDeep => f.write_str("component parent chain is cyclic or too deep"),
        }
    }
}

impl std::error::Error for LayoutError {}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ResizeRequest {
    pub id_hash: i32,
}

pub struct Nested {
    pub key: u64,
    pub components: Vec<Option<Component>>,
    pub parent: i32,
}

pub trait LayoutHost {
    fn game_window(&mut self, id_hash: i32) {
        let _ = id_hash;
    }

    fn game_window_resized(&mut self, id_hash: i32, width: i32, height: i32, fire_resize_hooks: bool) {
        let _ = (id_hash, width, height, fire_resize_hooks);
    }

    fn resize_hook(&mut self, request: ResizeRequest) {
        let _ = request;
    }

    fn take_nested(&mut self, container_id_hash: i32) -> Vec<Nested> {
        let _ = container_id_hash;
        Vec::new()
    }

    fn restore_nested(&mut self, nested: Nested) {
        let _ = nested;
    }
}

pub struct NoHost;

impl LayoutHost for NoHost {}

fn div(a: i32, b: i32) -> Result<i32, LayoutError> {
    if b == 0 {
        Err(LayoutError::DivideByZero)
    } else {
        Ok(a.wrapping_div(b))
    }
}

fn frac(base: i32, parent: i32) -> i32 {
    base.wrapping_mul(parent) >> 14
}

pub fn init_sizes(
    c: &mut Component,
    parent_width: i32,
    parent_height: i32,
    fire_resize_hooks: bool,
    host: &mut dyn LayoutHost,
) -> Result<(), LayoutError> {
    let old_width = c.width;
    let old_height = c.height;
    match c.aspect_width_type {
        0 => c.width = c.base_width,
        1 => c.width = parent_width.wrapping_sub(c.base_width),
        2 => c.width = frac(c.base_width, parent_width),
        _ => {}
    }
    match c.aspect_height_type {
        0 => c.height = c.base_height,
        1 => c.height = parent_height.wrapping_sub(c.base_height),
        2 => c.height = frac(parent_height, c.base_height),
        _ => {}
    }
    if c.aspect_width_type == 4 {
        c.width = div(c.aspect_x.wrapping_mul(c.height), c.aspect_y)?;
    }
    if c.aspect_height_type == 4 {
        c.height = div(c.width.wrapping_mul(c.aspect_y), c.aspect_x)?;
    }
    if c.content_type == Some(ContentType::GameWindow) {
        host.game_window(c.id_hash);
    }
    if fire_resize_hooks
        && c.on_resize.is_some()
        && (old_width != c.width || old_height != c.height)
    {
        host.resize_hook(ResizeRequest { id_hash: c.id_hash });
    }
    Ok(())
}

pub fn position_adjustment(c: &mut Component, parent_width: i32, parent_height: i32) {
    c.x = match c.aspect_x_type {
        0 => c.base_x,
        1 => c
            .base_x
            .wrapping_add(parent_width.wrapping_sub(c.width).wrapping_div(2)),
        2 => parent_width.wrapping_sub(c.width).wrapping_sub(c.base_x),
        3 => frac(c.base_x, parent_width),
        4 => parent_width
            .wrapping_sub(c.width)
            .wrapping_div(2)
            .wrapping_add(frac(c.base_x, parent_width)),
        _ => parent_width
            .wrapping_sub(c.width)
            .wrapping_sub(frac(c.base_x, parent_width)),
    };
    c.y = match c.aspect_y_type {
        0 => c.base_y,
        1 => parent_height
            .wrapping_sub(c.height)
            .wrapping_div(2)
            .wrapping_add(c.base_y),
        2 => parent_height.wrapping_sub(c.height).wrapping_sub(c.base_y),
        3 => frac(parent_height, c.base_y),
        4 => parent_height
            .wrapping_sub(c.height)
            .wrapping_div(2)
            .wrapping_add(frac(parent_height, c.base_y)),
        _ => parent_height
            .wrapping_sub(c.height)
            .wrapping_sub(frac(parent_height, c.base_y)),
    };
}

pub fn update_dimensions(
    components: &mut [Option<Component>],
    id_hash: i32,
    width: i32,
    height: i32,
    fire_resize_hooks: bool,
    host: &mut dyn LayoutHost,
) -> Result<(), LayoutError> {
    update_at_depth(components, id_hash, width, height, fire_resize_hooks, host, 0)
}

fn update_at_depth(
    components: &mut [Option<Component>],
    id_hash: i32,
    width: i32,
    height: i32,
    fire_resize_hooks: bool,
    host: &mut dyn LayoutHost,
    depth: usize,
) -> Result<(), LayoutError> {
    if depth > MAX_DEPTH {
        return Err(LayoutError::TooDeep);
    }
    for index in 0..components.len() {
        let Some(c) = components[index].as_mut() else {
            continue;
        };
        if c.parent_id != id_hash {
            continue;
        }
        init_sizes(c, width, height, fire_resize_hooks, host)?;
        position_adjustment(c, width, height);
        if c.scroll_x > c.scroll_width.wrapping_sub(c.width) {
            c.scroll_x = c.scroll_width.wrapping_sub(c.width);
        }
        if c.scroll_x < 0 {
            c.scroll_x = 0;
        }
        if c.scroll_y > c.scroll_height.wrapping_sub(c.height) {
            c.scroll_y = c.scroll_height.wrapping_sub(c.height);
        }
        if c.scroll_y < 0 {
            c.scroll_y = 0;
        }
        if c.kind == Some(ComponentType::Container) {
            let child_hash = c.id_hash;
            let child_width = if c.scroll_width != 0 {
                c.scroll_width
            } else {
                c.width
            };
            let child_height = if c.scroll_height != 0 {
                c.scroll_height
            } else {
                c.height
            };
            update_at_depth(
                components,
                child_hash,
                child_width,
                child_height,
                fire_resize_hooks,
                host,
                depth + 1,
            )?;
            for mut nested in host.take_nested(child_hash) {
                let result = update_at_depth(
                    &mut nested.components,
                    nested.parent,
                    child_width,
                    child_height,
                    fire_resize_hooks,
                    host,
                    depth + 1,
                );
                host.restore_nested(nested);
                result?;
            }
            if components[index]
                .as_ref()
                .is_some_and(|c| c.content_type == Some(ContentType::GameWindow))
            {
                host.game_window_resized(child_hash, child_width, child_height, fire_resize_hooks);
            }
        }
    }
    Ok(())
}
