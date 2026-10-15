package ar.com.fiado.readings

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/** Records the original microphone signal as AAC in an MP4 container; no local transcription. */
internal class AudioRecording(private val context: Context) {
 private var recorder: MediaRecorder? = null
 private var file: File? = null
 fun start(onLimit: () -> Unit) {
  discard()
  val directory = File(context.cacheDir, "account-audio").apply { mkdirs() }
  directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }
   ?.forEach { it.delete() }
  file = File.createTempFile("account-", ".m4a", directory)
  try {
   recorder = MediaRecorder(context)
   requireNotNull(recorder).apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setAudioEncodingBitRate(96_000)
    setAudioSamplingRate(44_100)
    setMaxDuration(60_000)
    setMaxFileSize(ReadingJson.MAX_AUDIO_BYTES.toLong())
    setOutputFile(requireNotNull(file).absolutePath)
    setOnInfoListener { _, what, _ ->
     if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
      what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) onLimit()
    }
    prepare()
    start()
   }
  } catch (error: Exception) { discard(); throw error }
 }
 fun finish(): File {
  val current = recorder ?: error("No recording")
  recorder = null
  try { current.stop() } catch (error: Exception) {
   file?.delete(); file = null; throw error
  } finally { current.release() }
  return requireNotNull(file)
 }
 fun discard() {
  recorder?.apply { runCatching { stop() }; release() }
  recorder = null
  file?.delete()
  file = null
 }
}
