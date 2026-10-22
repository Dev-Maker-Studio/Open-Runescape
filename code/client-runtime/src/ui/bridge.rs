use openrs_cs2::ComponentState;
use openrs_interface::def::{Component, DragType};
use std::sync::Arc;

#[allow(clippy::field_reassign_with_default)]
pub fn component_state(def: &Component) -> ComponentState {
    let mut state = ComponentState::default();
    state.id_hash = def.id_hash;
    state.parent_id = def.parent_id;
    state.slot_id = def.slot_id;
    state.component_type = def.kind.and_then(|k| u8::try_from(k.id()).ok());
    state.x = def.x;
    state.y = def.y;
    state.width = def.width;
    state.height = def.height;
    state.base_position_x = def.base_x;
    state.base_position_y = def.base_y;
    state.base_width = def.base_width;
    state.base_height = def.base_height;
    state.aspect_x_type = def.aspect_x_type;
    state.aspect_y_type = def.aspect_y_type;
    state.aspect_width_type = def.aspect_width_type;
    state.aspect_height_type = def.aspect_height_type;
    state.aspect_width = def.aspect_width;
    state.aspect_height = def.aspect_height;
    state.aspect_x = def.aspect_x;
    state.aspect_y = def.aspect_y;
    state.hidden = def.hidden;
    state.scroll_x = def.scroll_x;
    state.scroll_y = def.scroll_y;
    state.scroll_width = def.scroll_width;
    state.scroll_height = def.scroll_height;
    state.sprite_id = def.sprite_id;
    state.text = Arc::from(def.text.as_str());
    state.color = def.color;
    state.transparency = def.transparency;
    state.filled = def.filled;
    state.line_width = def.line_width;
    state.alpha = def.alpha;
    state.shadow = def.shadow;
    state.border_thickness = def.border_thickness;
    state.sprite_shadow = def.sprite_shadow;
    state.flip_vertical = def.flip_vertical;
    state.flip_horizontal = def.flip_horizontal;
    state.angle_2d = def.angle_2d;
    state.tiling = def.tiling;
    state.line_direction = def.line_direction;
    state.max_text_lines = def.max_text_lines;
    state.prevent_click_through = def.prevent_click_through;
    state.sprite_scale = def.sprite_scale;
    state.font_id = def.font_id;
    state.monospaced = def.monospaced;
    state.uses_orthogonal = def.uses_orthogonal;
    state.origin_x = def.origin_x;
    state.origin_y = def.origin_y;
    state.text_horizontal_alignment = def.text_horizontal_ali;
    state.text_vertical_alignment = def.text_vertical_ali;
    state.line_spacing = def.line_spacing;
    state.target_verb = Some(Arc::from(def.target_verb.as_str()));
    state.resume_text = def.resume_text.as_deref().map(Arc::from);
    state.click_mask = def.click_mask;
    state.drag_deadzone = def.drag_deadzone;
    state.drag_dead_time = def.drag_dead_time;
    state.drag_type = def.drag_type.map_or(0, DragType::value);
    state.sprite_pitch = def.sprite_pitch;
    state.sprite_roll = def.sprite_roll;
    state.sprite_yaw = def.sprite_yaw;
    state.primary_media_id = def.primary_media_id;
    state.animation = def.animation;
    state.container_item_id = def.container_item_id;
    state.item_stack_size = def.item_stack_size;
    state
}

pub fn apply_state(state: &ComponentState, live: &mut Component) {
    live.hidden = state.hidden;
    live.base_x = state.base_position_x;
    live.base_y = state.base_position_y;
    live.base_width = state.base_width;
    live.base_height = state.base_height;
    live.aspect_x_type = state.aspect_x_type;
    live.aspect_y_type = state.aspect_y_type;
    live.aspect_width_type = state.aspect_width_type;
    live.aspect_height_type = state.aspect_height_type;
    live.scroll_x = state.scroll_x;
    live.scroll_y = state.scroll_y;
    live.scroll_width = state.scroll_width;
    live.scroll_height = state.scroll_height;
    live.sprite_id = state.sprite_id;
    live.text = state.text.to_string();
    live.color = state.color;
    live.transparency = state.transparency;
    live.filled = state.filled;
    live.line_width = state.line_width;
    live.shadow = state.shadow;
    live.flip_vertical = state.flip_vertical;
    live.flip_horizontal = state.flip_horizontal;
    live.angle_2d = state.angle_2d;
    live.tiling = state.tiling;
    live.line_direction = state.line_direction;
    live.max_text_lines = state.max_text_lines;
    live.sprite_scale = state.sprite_scale;
    live.font_id = state.font_id;
    live.monospaced = state.monospaced;
    live.text_horizontal_ali = state.text_horizontal_alignment;
    live.text_vertical_ali = state.text_vertical_alignment;
    live.line_spacing = state.line_spacing;
    live.container_item_id = state.container_item_id;
    live.item_stack_size = state.item_stack_size;
}

#[cfg(test)]
mod tests {
    use super::*;
    use openrs_interface::def::ComponentType;

    #[test]
    fn state_round_trips_the_drawable_fields() {
        let mut def = Component::blank((548 << 16) | 3);
        def.kind = Some(ComponentType::Text);
        def.parent_id = 7;
        def.base_x = 5;
        def.base_y = 6;
        def.base_width = 70;
        def.base_height = 8;
        def.aspect_x_type = 2;
        def.text = "hello".into();
        def.color = 0xff00ff;
        def.font_id = 495;
        def.text_horizontal_ali = 1;
        def.shadow = true;
        def.hidden = true;
        def.drag_type = Some(DragType::Type2);
        let state = component_state(&def);
        assert_eq!(state.component_type, Some(4));
        assert_eq!(state.base_position_x, 5);
        assert_eq!(&*state.text, "hello");
        assert_eq!(state.drag_type, 2);
        let mut back = Component::blank(def.id_hash);
        apply_state(&state, &mut back);
        for (a, b) in [(back.base_x, def.base_x), (back.color, def.color), (back.font_id, def.font_id)] {
            assert_eq!(a, b);
        }
        assert_eq!(back.text, "hello");
        assert!(back.hidden && back.shadow);
        assert_eq!(back.aspect_x_type, 2);
    }

    #[test]
    fn a_cs2_text_change_reaches_the_drawn_component() {
        let def = Component::blank(1);
        let mut state = component_state(&def);
        state.text = Arc::from("changed");
        state.hidden = true;
        let mut live = def.clone();
        apply_state(&state, &mut live);
        assert_eq!(live.text, "changed");
        assert!(live.hidden);
    }
}
