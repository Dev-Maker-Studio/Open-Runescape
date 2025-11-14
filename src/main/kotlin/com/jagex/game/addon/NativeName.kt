package com.jagex.game.addon

/**
 * Annotation to add native method renaming on compile later down the road
 * (In order to make the advanced software mode, Sw3d library load)
 * @author Brainpower
 *
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class NativeName(
    val value: String = ""
)
