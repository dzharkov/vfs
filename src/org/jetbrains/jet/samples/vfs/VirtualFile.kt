package org.jetbrains.jet.samples.vfs

import org.jetbrains.jet.samples.vfs.utils.*
import java.io.File
import java.io.InputStream
import java.io.FileInputStream
import java.util.ArrayList

import kotlin.util.*

/**
 * Abstract virtual file.
 */
public abstract class VirtualFile(public val path : String) {
    protected abstract val kind : String

    /**
     * Returns file size.
     */
    public abstract val size : Long
    /**
     * Returns the time that the virtual file was last modified (milliseconds since
     * the epoch (00:00:00 GMT, January 1, 1970).
     */
    public abstract val modificationTime : Long
    /**
     * Returns if virtual file exists.
     */
    public abstract val exists : Boolean
    /**
     * Returns if virtual file is directory
     */
    public abstract val isDirectory : Boolean
    /**
     * Returns list of virtual files which are children for this.
     */
    public abstract val children : List<VirtualFile>

    /**
     * Opens input stream for reading. After reading, stream should be closed.
     * Reading from stream should be performed with read lock acquired.
     */
    public abstract fun openInputStream() : InputStream

    override fun equals(other : Any?) : Boolean {
        return other is VirtualFile && kind == other.kind && path == other.path
    }

    override fun hashCode() : Int {
        return kind.hashCode() * 31 + path.hashCode()
    }

    override fun toString(): String {
        return "${kind}[path=$path]"
    }
}

/**
 * Type of virtual file which corresponds to real file in file system of OS.
 */
public class PhysicalVirtualFile(path : String) : VirtualFile(path) {
    override public val kind : String = "Physical"

    private val ioFile : File
        get() = File(this.path.toSystemDependentPath())

    override public val exists: Boolean
    get() {
        FileSystem.assertCanRead()
        return ioFile.exists()
    }

    override public val size: Long
    get() {
        FileSystem.assertCanRead()
        return ioFile.length()
    }

    override public val modificationTime: Long
    get() {
        FileSystem.assertCanRead()
        return ioFile.lastModified()
    }

    override public val isDirectory: Boolean
    get() {
        FileSystem.assertCanRead()
        return ioFile.isDirectory()
    }

    override public val children: List<VirtualFile>
    get() {
        FileSystem.assertCanRead()
        return (ioFile.listFiles() ?: arrayOf<File>()).
                map{ FileSystem.getFileByIoFile(it) }.toList()
    }

    override public fun openInputStream(): InputStream {
        FileSystem.assertCanRead()
        if (isDirectory) {
            throw IllegalArgumentException("Can't open directory for reading")
        }
        return CheckedInputStream(FileInputStream(ioFile))
    }
}

private val OS_SEPARATOR = java.io.File.separator
private val VFS_SEPARATOR = "/"

private fun String.toSystemDependentPath() : String {
    return this.replace(VFS_SEPARATOR.toRegex(), OS_SEPARATOR)
}

private fun String.toSystemIndependentPath() : String {
    return this.replace(OS_SEPARATOR.toRegex(), VFS_SEPARATOR)
}

/**
 * InputStream wrapper which checks that file system read lock is acquired on each operation.
 */
private class CheckedInputStream(private val wrapped : InputStream) : InputStream() {
    override public fun read(): Int {
        FileSystem.assertCanRead()
        return wrapped.read()
    }

    override public fun read(b: ByteArray, off: Int, len: Int) : Int {
        FileSystem.assertCanRead()
        return wrapped.read(b, off, len)
    }

    override public fun markSupported(): Boolean {
        FileSystem.assertCanRead()
        return wrapped.markSupported()
    }

    override public fun skip(n: Long): Long {
        FileSystem.assertCanRead()
        return wrapped.skip(n)
    }

    override public fun close() {
        FileSystem.assertCanRead()
        return wrapped.close()
    }

    override public fun mark(readlimit: Int) {
        FileSystem.assertCanRead()
        return wrapped.mark(readlimit)
    }

    override public fun read(b: ByteArray): Int {
        FileSystem.assertCanRead()
        return wrapped.read(b)
    }

    override public fun reset() {
        FileSystem.assertCanRead()
        return wrapped.reset()
    }

    override public fun available(): Int {
        FileSystem.assertCanRead()
        return wrapped.available()
    }
}
