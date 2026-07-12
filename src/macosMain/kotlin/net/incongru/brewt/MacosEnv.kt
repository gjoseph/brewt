package net.incongru.brewt

import platform.Foundation.NSBundle

actual fun currentBinaryPath(): String? = NSBundle.mainBundle.executablePath
