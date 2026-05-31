package com.ioristudios.anydoc.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.crypt.EncryptionMode
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DocumentDecryptor {

    fun isPasswordProtected(file: File, extension: String): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return when (extension.lowercase()) {
            "pdf" -> {
                try {
                    // Try to load with empty password
                    PDDocument.load(file).use { }
                    false
                } catch (e: Exception) {
                    val isInvalidPassword = e is com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException ||
                        e.message?.contains("password", ignoreCase = true) == true ||
                        e.cause?.message?.contains("password", ignoreCase = true) == true ||
                        e.javaClass.name.contains("InvalidPasswordException")
                    isInvalidPassword
                }
            }
            "docx", "xlsx", "pptx" -> {
                try {
                    POIFSFileSystem(file).use { fs ->
                        fs.root.hasEntry("EncryptionInfo")
                    }
                } catch (e: Exception) {
                    false
                }
            }
            "doc", "xls", "ppt" -> {
                try {
                    Biff8EncryptionKey.setCurrentUserPassword(null)
                    POIFSFileSystem(file).use { fs ->
                        if (extension.lowercase() == "xls") {
                            org.apache.poi.hssf.usermodel.HSSFWorkbook(fs).close()
                        } else if (extension.lowercase() == "doc") {
                            org.apache.poi.hwpf.HWPFDocument(fs).close()
                        } else if (extension.lowercase() == "ppt") {
                            org.apache.poi.hslf.usermodel.HSLFSlideShow(fs).close()
                        }
                    }
                    false
                } catch (e: org.apache.poi.EncryptedDocumentException) {
                    true
                } catch (e: Exception) {
                    false
                } finally {
                    Biff8EncryptionKey.setCurrentUserPassword(null)
                }
            }
            else -> false
        }
    }

    fun decryptFile(file: File, extension: String, password: String, decryptedFile: File): Boolean {
        if (!file.exists()) return false
        return when (extension.lowercase()) {
            "pdf" -> {
                try {
                    PDDocument.load(file, password).use { doc ->
                        doc.setAllSecurityToBeRemoved(true)
                        doc.save(decryptedFile)
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            "docx", "xlsx", "pptx" -> {
                try {
                    POIFSFileSystem(file).use { fs ->
                        val info = EncryptionInfo(fs)
                        val decryptor = Decryptor.getInstance(info)
                        if (decryptor.verifyPassword(password)) {
                            decryptor.getDataStream(fs).use { inputStream ->
                                FileOutputStream(decryptedFile).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            true
                        } else {
                            false
                        }
                    }
                } catch (e: Exception) {
                    false
                }
            }
            "doc", "xls", "ppt" -> {
                try {
                    Biff8EncryptionKey.setCurrentUserPassword(password)
                    POIFSFileSystem(file).use { fs ->
                        if (extension.lowercase() == "xls") {
                            org.apache.poi.hssf.usermodel.HSSFWorkbook(fs).close()
                        } else if (extension.lowercase() == "doc") {
                            org.apache.poi.hwpf.HWPFDocument(fs).close()
                        } else if (extension.lowercase() == "ppt") {
                            org.apache.poi.hslf.usermodel.HSLFSlideShow(fs).close()
                        }
                    }
                    true
                } catch (e: Exception) {
                    false
                } finally {
                    Biff8EncryptionKey.setCurrentUserPassword(null)
                }
            }
            else -> false
        }
    }

    fun encryptOffice(decryptedFile: File, password: String, encryptedFile: File) {
        POIFSFileSystem().use { fs ->
            val info = EncryptionInfo(EncryptionMode.agile)
            val enc = info.encryptor
            enc.confirmPassword(password)
            
            FileInputStream(decryptedFile).use { inputStream ->
                enc.getDataStream(fs).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            FileOutputStream(encryptedFile).use { fos ->
                fs.writeFilesystem(fos)
            }
        }
    }
}
