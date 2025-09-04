package com.pisces.xcodebn.easycapture.domain.model

sealed class RecordingQuality {
    abstract val displayName: String
    abstract val bitrate: Int
    abstract val frameRate: Int
    abstract val resolution: String
    
    data object LOW : RecordingQuality() {
        override val displayName = "Low (720p)"
        override val bitrate = 1_000_000
        override val frameRate = 24
        override val resolution = "720p"
    }
    
    data object MEDIUM : RecordingQuality() {
        override val displayName = "Medium (1080p)"
        override val bitrate = 2_500_000
        override val frameRate = 30
        override val resolution = "1080p"
    }
    
    data object HIGH : RecordingQuality() {
        override val displayName = "High (1440p)"
        override val bitrate = 8_000_000
        override val frameRate = 60
        override val resolution = "1440p"
    }
    
    data class CUSTOM(
        override val displayName: String,
        override val bitrate: Int,
        override val frameRate: Int,
        override val resolution: String,
        val id: String
    ) : RecordingQuality()
}