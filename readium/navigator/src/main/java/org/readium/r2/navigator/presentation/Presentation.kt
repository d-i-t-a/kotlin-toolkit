package org.readium.r2.navigator.presentation

import android.content.Context
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.Try

data class PresentationKey(val key: String) {
    companion object {
        val CONTINUOUS = PresentationKey("continuous")
        val READING_PROGRESSION = PresentationKey("readingProgression")
    }

    override fun toString(): String = key
}

/**
 * Holds the current values for the Presentation Properties determining how a publication is
 * rendered by a Navigator. For example, "font size" or "playback rate".
 */
data class Presentation(
    val properties: Map<PresentationKey, Property<*>?> = emptyMap()
) {

    constructor(vararg properties: Pair<PresentationKey, Property<*>?>) : this(mapOf(*properties))

    val continuous: ToggleProperty? get() =
        properties[PresentationKey.CONTINUOUS] as? ToggleProperty

    val readingProgression: EnumProperty<ReadingProgression>? get() =
        (properties[PresentationKey.READING_PROGRESSION] as? StringProperty)
            ?.let { EnumProperty(ReadingProgression, it, ReadingProgression.default) }

    /**
     * Holds the current value and the metadata of a Presentation Property of type [T].
     *
     * @param value Current value for the property.
     */
    sealed class Property<T>(
        val value: T,
        private val isActiveForSettings: (PresentationSettings) -> Boolean,
        private val activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception>,
    ) {

        /**
         * Determines whether the property will be active when the given settings are applied to the
         * Navigator.
         *
         * For example, with an EPUB Navigator using Readium CSS, the property "letter spacing" requires
         * to switch off the "publisher defaults" setting to be active.
         *
         * This is useful to determine whether to grey out a view in the user settings interface.
         */
        fun isActiveForSettings(settings: PresentationSettings): Boolean =
            isActiveForSettings.invoke(settings)

        /**
         * Modifies the given settings to make sure the property will be activated when applying them to
         * the Navigator.
         *
         * For example, with an EPUB Navigator using Readium CSS, activating the "letter spacing"
         * property means ensuring the "publisher defaults" setting is disabled.
         *
         * If the property cannot be activated, returns a user-facing localized error.
         */
        fun activateInSettings(settings: PresentationSettings): Try<PresentationSettings, Exception> =
            activateInSettings.invoke(settings)
    }

    /**
     * Property representable as a toggle switch in the user interface. For example,
     * "publisher defaults" or "continuous".
     */
    class ToggleProperty(
        value: Boolean,
        isActiveForSettings: (PresentationSettings) -> Boolean = { true },
        activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.Success(it) }
    ) : Property<Boolean>(value, isActiveForSettings, activateInSettings)

    /**
     * Property representable as a dropdown menu or radio buttons group in the user interface. For
     * example, "reading progression" or "font family".
     *
     * @param supportedValues List of values supported by this navigator, in logical order. Null if
     *        any value is supported.
     */
    class StringProperty(
        value: String,
        val supportedValues: List<String>?,
        isActiveForSettings: (PresentationSettings) -> Boolean = { true },
        activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.success(it) },
        private val labelForValue: (Context, String) -> String = { _, v -> v },
    ) : Property<String>(value, isActiveForSettings, activateInSettings) {

        /**
         * Returns a user-facing localized label for the given value, which can be used in the user
         * interface.
         *
         * For example, with the "reading progression" property, the value ltr has for label "Left to
         * right" in English.
         */
        fun labelForValue(context: Context, value: String): String =
            labelForValue.invoke(context, value)

        companion object {
            operator fun <T : Enum<T>> invoke(
                mapper: MapCompanion<String, T>,
                value: T,
                supportedValues: List<T>,
                isActiveForSettings: (PresentationSettings) -> Boolean = { true },
                activateInSettings: (settings: PresentationSettings) -> Try<PresentationSettings, Exception> = { Try.success(it) },
                labelForValue: (Context, T) -> String = { _, v -> v.name },
            ): StringProperty =
                StringProperty(
                    mapper.getKey(value),
                    supportedValues = supportedValues.map { mapper.getKey(it) },
                    isActiveForSettings = isActiveForSettings,
                    activateInSettings = activateInSettings,
                    labelForValue = { c, v ->
                        mapper.get(v)?.let { labelForValue(c, it) } ?: v
                    }
                )
        }
    }

    class EnumProperty<T : Enum<T>>(
        private val mapper: MapCompanion<String, T>,
        private val stringProperty: StringProperty,
        private val defaultValue: T,
    ) {

        val value: T get() = mapper.get(stringProperty.value) ?: defaultValue

        val supportedValues: List<T>? = stringProperty.supportedValues
            ?.mapNotNull { mapper.get(it) }

        fun isActiveForSettings(settings: PresentationSettings): Boolean =
            stringProperty.isActiveForSettings(settings)

        fun activateInSettings(settings: PresentationSettings): Try<PresentationSettings, Exception> =
            stringProperty.activateInSettings(settings)

        fun labelForValue(context: Context, value: T): String =
            stringProperty.labelForValue(context, mapper.getKey(value))
    }
}

/**
 * Holds a list of key-value pairs provided by the app to influence a Navigator's Presentation
 * Properties. The keys must be valid Presentation Property Keys.
 */
data class PresentationSettings(val settings: Map<PresentationKey, Any?> = emptyMap()) {

    constructor(vararg settings: Pair<PresentationKey, Any?>) : this(mapOf(*settings))

    val continuous: Boolean?
        get() = settings[PresentationKey.CONTINUOUS] as? Boolean

    val readingProgression: ReadingProgression?
        get() = (settings[PresentationKey.READING_PROGRESSION] as? String)
            ?.let { ReadingProgression.get(it) }

    /**
     * Returns a copy of this object after modifying the settings in the given closure.
     */
    fun copy(transform: MutableMap<PresentationKey, Any?>.() -> Unit): PresentationSettings =
        PresentationSettings(settings.toMutableMap().apply(transform).toMap())

    /**
     * Returns a copy of this object after overwriting any setting with the values from [other].
     */
    fun merge(other: PresentationSettings): PresentationSettings =
        PresentationSettings(
            (other.settings.entries + settings.entries)
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.firstOrNull() }
        )
}
