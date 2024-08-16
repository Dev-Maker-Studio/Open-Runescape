package com.jagex.game.addon;

import java.lang.annotation.*;

/**
 * Annotation to add native method renaming on compile later down the road
 * (In order to make the advanced software mode, Sw3d library load)
 * @author Brainpower
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NativeName {
    String value() default "";
}