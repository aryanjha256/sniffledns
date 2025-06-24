package com.example.sniffledns.vpn

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.Os.socket
import android.util.Log
import com.example.sniffledns.utils.PacketParser
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class SniffleVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null
    private var isVpnStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isVpnStarted) {

            try {
                val allowedApps = intent?.getStringArrayListExtra("allowedApps") ?: emptyList()
                startVpn(allowedApps)
                isVpnStarted = true
                Log.i("SniffleVPN", "VPN started")
            } catch (e: Exception) {
                Log.e("SniffleVPN", "Error starting VPN: ${e.message}", e)
            }
        } else {
            Log.w("SniffleVPN", "VPN already running, ignoring duplicate start.")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i("SniffleVPN", "Stopping VPN...")
        runBlocking {
            try {
                job?.cancelAndJoin()
            } catch (e: Exception) {
                Log.e("SniffleVPN", "Error cancelling job: ${e.message}", e)
            }
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("SniffleVPN", "Error closing VPN interface: ${e.message}", e)
        } finally {
            vpnInterface = null
            isVpnStarted = false
        }

        super.onDestroy()
    }

    private fun startVpn(allowedPackages: List<String>) {
        val builder = Builder()
            .addAddress("10.0.0.2", 24)
            .addDnsServer("dns.adguard-dns.com")
            .addRoute("0.0.0.0", 0)
            .setSession("SniffleDNS")

        allowedPackages.forEach { pkg ->
            try {
                builder.addAllowedApplication(pkg)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w("SniffleVPN", "App not found: $pkg")
            }
        }

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            Log.e("SniffleVPN", "Failed to establish VPN interface.")
            return
        }


        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                handleDns()
            } catch (e: Exception) {
                Log.e("SniffleVPN", "DNS handling error: ${e.message}", e)
            }
        }
    }

    private suspend fun handleDns() = coroutineScope {

        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val output = FileOutputStream(vpnInterface?.fileDescriptor)

        val buffer = ByteArray(32767)

//        private val dnsServerAddress = InetSocketAddress("94.140.14.14", 53)
//        private val forwardSocket = DatagramSocket()

//        vpnInterface?.fileDescriptor?.let { fd ->
//            FileInputStream(fd).use { input ->
//                val packet = ByteArray(32767)
//
//                while (isActive) {
//                    try {
//                        val length = input.read(packet)
//                        if (length > 0 && packet[9] == 17.toByte()) {
//                            forwardDns(packet.copyOf(length))
//                        }
//                    } catch (e: InterruptedIOException) {
//                        Log.i("SniffleVPN", "VPN read interrupted — shutting down")
//                        break
//                    } catch (e: IOException) {
//                        Log.e("SniffleVPN", "IO error in VPN read: ${e.message}")
//                        break
//                    } catch (e: Exception) {
//                        Log.e("SniffleVPN", "Unexpected error in VPN read: ${e.message}")
//                        break
//                    }
//                }
//            }
//        } ?: Log.w("SniffleVPN", "VPN interface not available")

        while (isActive) {
            try {
                val length = input.read(buffer)
                if (length > 0) {
                    val packetData = ByteArray(length)
                    System.arraycopy(buffer, 0, packetData, 0, length)
                    // ✨ Parse raw packet
                    val packet = PacketParser(ByteBuffer.wrap(buffer, 0, length))
//                    val uid = getUidFromPacket(packet)

                    if (packet.isDnsQuery()) {
                        val domain = packet.getQueriedDomain()
//                        Log.i("SniffleDNS", "DNS Request UID=$uid → $domain")
                        Log.i("SniffleDNS", "DNS Request UID=uid → $domain")

                        val dnsResponse = forwardDns(packetData)
                        if (dnsResponse != null) {
                            // Write response back to the app
                            output.write(dnsResponse)
                        } else {
                            Log.w("SniffleDNS", "Failed to get DNS response")
                        }
                    }
                }
            } catch (e: InterruptedIOException) {
                Log.i("SniffleDNS", "VPN read interrupted — shutting down")
                break
            } catch (e: IOException) {
                Log.e("SniffleDNS", "IO error in VPN read: ${e.message}")
                break
            }
        }
    }

//    private fun forwardDns(packet: ByteArray) {
//        try {
//            DatagramSocket().use { socket ->
//                protect(socket)
//                val dnsPacket = DatagramPacket(packet, packet.size, InetAddress.getByName("94.140.14.14"), 53)
//                socket.send(dnsPacket)
//            }
//        } catch (e: Exception) {
//            Log.e("SniffleVPN", "Error forwarding DNS: ${e.message}", e)
//        }
//    }

    private fun forwardDns(request: ByteArray): ByteArray? {

        val dnsServerAddress = InetSocketAddress("94.140.14.14", 53)
         val forwardSocket = DatagramSocket()

        return try {
            val sendPacket = DatagramPacket(request, request.size, dnsServerAddress)
            forwardSocket.send(sendPacket)

            val responseBuffer = ByteArray(512)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            forwardSocket.soTimeout = 2000
            forwardSocket.receive(receivePacket)

            receivePacket.data.copyOf(receivePacket.length)
        } catch (e: Exception) {
            Log.e("SniffleDNS", "DNS Forwarding failed: ${e.message}")
            null
        }
    }



}
