package com.example.sniffledns.utils


import java.nio.ByteBuffer

class PacketParser(private val buffer: ByteBuffer) {

    fun isDnsQuery(): Boolean {
        // Basic UDP/DNS detection (check protocol and port)
        return getDestinationPort() == 53
    }

    fun getDestinationPort(): Int {
        buffer.position(22) // UDP dest port offset (after IP header)
        return buffer.short.toInt() and 0xFFFF
    }

    fun getQueriedDomain(): String {
        // DNS header starts after UDP header (8 bytes)
        buffer.position(28)
        val domainParts = mutableListOf<String>()
        while (true) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            val part = ByteArray(len)
            buffer.get(part)
            domainParts.add(String(part))
        }
        return domainParts.joinToString(".")
    }
}
