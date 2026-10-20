package com.sperance.exileforge.core

import com.sperance.exileforge.core.network.ModerationEntryView
import kotlinx.serialization.Serializable
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Сторож companion сериализуемых классов ядра клиента: у каждого класса с `@Serializable` companion публичный. `serializer<T>()`
 * из inline-функции читает статическое поле companion прямо из чужого класса - непубличный companion в сжатой R8 сборке
 * клиента падает `IllegalAccessError`; константы такого класса живут уровнем файла.
 */
class SerializableCompanionTest {
    @Test
    fun serializableCompanionsArePublic() {
        val serializable = compiledClasses("com.sperance.exileforge.core").filter { it.isAnnotationPresent(Serializable::class.java) }
        assertTrue(ModerationEntryView::class.java in serializable, "the scan sees the module's own compiled classes")
        val hidden = serializable.mapNotNull(::companionOf).filterNot { Modifier.isPublic(it.modifiers) && Modifier.isPublic(it.type.modifiers) }
        assertEquals(emptyList(), hidden.map { "${it.declaringClass.name}.${it.name}" }, "a @Serializable class keeps its companion public")
    }

    /** Поле companion класса [type]: статическое поле его же вложенного класса с тем же именем; null - companion нет. */
    private fun companionOf(type: Class<*>) = type.declaredFields.firstOrNull { Modifier.isStatic(it.modifiers) && it.type.enclosingClass == type && it.name == it.type.simpleName }

    /** Классы пакета [pkg] из каталогов пути классов теста - классы самого модуля, без библиотек в jar; не инициализируются. */
    private fun compiledClasses(pkg: String): List<Class<*>> {
        val loader = javaClass.classLoader
        return loader.getResources(pkg.replace('.', '/')).toList().filter { it.protocol == "file" }.flatMap { url ->
            val root = File(url.toURI())
            root.walk().filter { it.isFile && it.extension == "class" }.map { file ->
                Class.forName("$pkg.${file.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.')}", false, loader)
            }.toList()
        }
    }
}
