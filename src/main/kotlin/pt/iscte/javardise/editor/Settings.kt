package pt.iscte.javardise.editor

import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import pt.iscte.javardise.Configuration
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.external.*
import java.io.File
import java.io.PrintWriter
import java.util.*

class Settings(val editor: CodeEditor) {
    private val parameters = mapOf(
        "font-size" to Pair(Configuration::fontSize) { c: Configuration, v: String -> c.fontSize = v.toInt() },
        "font-face" to Pair(Configuration::fontFace) { c: Configuration, v: String -> c.fontFace = v }
    )

    var editorConfiguration = loadConfiguration()

    private fun loadConfiguration(): Configuration {
        val conf = DefaultConfiguration()
        val file = File(editor.folder, ".javardise")
        if (file.exists()) {
            val scanner = Scanner(file)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val (key, value) = line.split("=")
                parameters[key]?.let {
                    it.second(conf, value)
                }
            }
            scanner.close()
        }
        return conf
    }


    fun openDialog(parent: Shell) {
        parent.messageOkAction {
            text = "Settings"
            val map = mutableMapOf<String, Text>()
            grid(2) {
                parameters.forEach { (key, pair) ->
                    label(key)
                    map[key] = text(pair.first(editorConfiguration).toString())
                }
            }
            return@messageOkAction {
                editorConfiguration = DefaultConfiguration().apply {
                    map.keys.forEach { key ->
                        parameters[key]?.let {
                            // TODO improve
                            try {
                                it.second(this, map[key]!!.text)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
                saveConfiguration()
                editor.updateTabs()
            }
        }
    }

    private fun saveConfiguration() {
        val w = PrintWriter(File(editor.folder, ".javardise"))
        parameters.forEach { (key, f) ->
            w.println("$key=${f.first(editorConfiguration)}")
        }
        w.close()
    }
}