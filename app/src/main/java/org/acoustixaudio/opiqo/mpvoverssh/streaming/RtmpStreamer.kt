@file:Suppress("unused")

package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over FFmpeg-based RTMP publishing.
 */
interface RtmpStreamer {
	fun start(
		inputUri: Uri,
		publishUrl: String,
		options: StreamOptions = StreamOptions()
	): Flow<StreamEvent>

	suspend fun stop()
}


