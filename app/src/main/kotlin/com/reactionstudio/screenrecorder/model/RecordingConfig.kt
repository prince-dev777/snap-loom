package com.reactionstudio.screenrecorder.model

enum class VideoResolution(val width: Int, val height: Int, val displayName: String) {
    RES_1080P(1920, 1080, "1080p Full HD"),
    RES_720P(1280, 720, "720p HD"),
    RES_480P(854, 480, "480p SD"),
    RES_NATIVE(0, 0, "Screen Native")
}

enum class VideoFps(val fps: Int, val displayName: String) {
    FPS_60(60, "60 FPS (Smooth)"),
    FPS_30(30, "30 FPS (Standard)"),
    FPS_24(24, "24 FPS (Cinematic)")
}

enum class VideoBitrate(val bps: Int, val displayName: String) {
    ULTRA(16_000_000, "16 Mbps (Ultra)"),
    HIGH(10_000_000, "10 Mbps (High)"),
    BALANCED(6_000_000, "6 Mbps (Balanced)"),
    COMPACT(3_000_000, "3 Mbps (Compact)")
}

enum class FacecamShape(val displayName: String) {
    CIRCLE("Circle"),
    ROUNDED_RECT("Rounded Rectangle")
}

enum class FacecamSize(val sizeDp: Int, val displayName: String) {
    SMALL(120, "Small"),
    MEDIUM(160, "Medium"),
    LARGE(210, "Large")
}

enum class CameraLens(val displayName: String) {
    FRONT("Front (Face)"),
    BACK("Rear (Back)")
}

enum class CountdownOption(val seconds: Int, val displayName: String) {
    NONE(0, "None"),
    THREE_SEC(3, "3s Countdown"),
    FIVE_SEC(5, "5s Countdown")
}

enum class AudioSourceMode(val displayName: String) {
    MIC("Microphone"),
    INTERNAL("Internal Sound"),
    MUTE("Mute Audio")
}

enum class OrientationMode(val displayName: String) {
    AUTO("Auto Rotate"),
    PORTRAIT("Portrait (Shorts/Reels)"),
    LANDSCAPE("Landscape (YouTube/Games)")
}

enum class FacecamBorderColor(val hex: String, val displayName: String) {
    RED("#FF0055", "Neon Red"),
    CYAN("#00E5FF", "Electric Cyan"),
    GOLD("#FFD700", "Golden Glow"),
    GREEN("#00E676", "Emerald"),
    PURPLE("#E040FB", "Vivid Purple")
}

data class RecordingConfig(
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val fps: VideoFps = VideoFps.FPS_60,
    val bitrate: VideoBitrate = VideoBitrate.HIGH,
    val recordAudio: Boolean = true,
    val audioMode: AudioSourceMode = AudioSourceMode.MIC,
    val orientation: OrientationMode = OrientationMode.AUTO,
    val shakeToStop: Boolean = true,
    val enableFacecam: Boolean = true,
    val facecamShape: FacecamShape = FacecamShape.CIRCLE,
    val facecamSize: FacecamSize = FacecamSize.MEDIUM,
    val facecamBorderColor: FacecamBorderColor = FacecamBorderColor.RED,
    val cameraLens: CameraLens = CameraLens.FRONT,
    val showFloatingControls: Boolean = true,
    val countdown: CountdownOption = CountdownOption.THREE_SEC,
    val enableNoiseShield: Boolean = true,
    val enableStudioHdAudio: Boolean = true,
    val micVolumeGain: Float = 1.0f,
    val enableTeleprompter: Boolean = false,
    val enableCustomWatermark: Boolean = false,
    val customWatermarkText: String = "@SnapLoomCreator",
    val ghostMode: Boolean = false
)
