package git.shin.rakuyomi_bridge

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "health" -> cmdHealth(args)
        "run" -> cmdRun(args)
        "exec" -> cmdExec(args)
        "test" -> cmdTest(args)
        else -> printUsage()
    }
}

private fun printUsage() {
    println(
        """
Rakuyomi Bridge CLI — test harness for the rakuyomi HTTP server.

Usage:
  health [<port>]
      Check if a running server responds to /health-check.

  run <server-bin> <data-dir> [<port>]
      Start server, wait for health, stop on Ctrl+C, report result.

  exec <server-bin> <data-dir> [-- <test-command>...]
      Start server, run a test command (or just health-check), stop.

  test <server-bin> <data-dir>
      Start server, run all API tests, stop.

Examples:
  run ./server /tmp/rakuyomi-data
  exec ./server /tmp/data -- curl http://127.0.0.1:8787/library
  health 8787
        """.trimIndent()
    )
}

private fun cmdHealth(args: Array<String>) {
    val port = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_SERVER_PORT
    val ok = BridgeClient(ServerConfig(port = port, homePath = "")).healthCheck()
    if (ok) println("OK — server is healthy on port $port")
    else println("FAIL — no response from $SERVER_HOST:$port")
}

private fun cmdRun(args: Array<String>) {
    val serverBin = args.getOrNull(1) ?: error("Missing <server-bin>")
    val dataDir = args.getOrNull(2) ?: error("Missing <data-dir>")
    val port = args.getOrNull(3)?.toIntOrNull() ?: DEFAULT_SERVER_PORT

    val process = startServer(serverBin, dataDir, port)
    Runtime.getRuntime().addShutdownHook(Thread { process.destroy(); println("\nServer stopped.") })

    val ready = runBlocking { BridgeClient(ServerConfig(port = port, homePath = dataDir)).waitForReady() }
    if (ready) println("Server is ready on port $port. Press Ctrl+C to stop.")
    else { println("Server did not become ready within timeout."); process.destroy() }
    process.waitFor()
}

private fun cmdExec(args: Array<String>) {
    val serverBin = args.getOrNull(1) ?: error("Missing <server-bin>")
    val dataDir = args.getOrNull(2) ?: error("Missing <data-dir>")
    val port = DEFAULT_SERVER_PORT

    val process = startServer(serverBin, dataDir, port)
    val ready = runBlocking { BridgeClient(ServerConfig(port = port, homePath = dataDir)).waitForReady() }
    if (!ready) { println("Server did not become ready."); process.destroy(); kotlin.system.exitProcess(1) }

    val dashIndex = args.indexOf("--")
    if (dashIndex >= 0 && dashIndex + 1 < args.size) {
        val testCmd = args.sliceArray(dashIndex + 1 until args.size)
        val exitCode = ProcessBuilder(*testCmd).inheritIO().start().waitFor()
        process.destroy()
        kotlin.system.exitProcess(exitCode)
    } else {
        process.destroy()
        kotlin.system.exitProcess(if (clientHealth(port)) 0 else 1)
    }
}

private fun cmdTest(args: Array<String>) {
    val serverBin = args.getOrNull(1) ?: error("Missing <server-bin>")
    val dataDir = args.getOrNull(2) ?: error("Missing <data-dir>")
    val port = DEFAULT_SERVER_PORT

    val process = startServer(serverBin, dataDir, port)
    val ready = runBlocking { BridgeClient(ServerConfig(port = port, homePath = dataDir)).waitForReady() }
    if (!ready) { println("FAIL — server not ready"); process.destroy(); kotlin.system.exitProcess(1) }

    val allPassed = runBlocking { runAllTests(port) }
    process.destroy()
    if (allPassed) println("All tests passed.") else println("Some tests failed.")
    kotlin.system.exitProcess(if (allPassed) 0 else 1)
}

// ── helpers ─────────────────────────────────────────────────────────

private fun startServer(bin: String, dataDir: String, port: Int): Process {
    val pb = ProcessBuilder(bin, dataDir)
    pb.environment()["RAKUYOMI_USE_TCP"] = "1"
    pb.environment()["RAKUYOMI_TCP_PORT"] = port.toString()
    pb.environment()["RAKUYOMI_HOME"] = dataDir
    pb.inheritIO()
    return pb.start()
}

private fun clientHealth(port: Int) = BridgeClient(ServerConfig(port = port, homePath = "")).healthCheck()

private suspend fun runAllTests(port: Int): Boolean {
    val client = BridgeClient(ServerConfig(port = port, homePath = ""))
    var allPassed = true

    println("[test] GET /health-check ... ")
    val health = client.healthCheck()
    println(if (health) "PASS" else "FAIL — no response")
    allPassed = allPassed && health

    println("[test] GET /library ... ")
    val lib = client.httpGet("http://127.0.0.1:$port/library")
    println(if (lib.success) "PASS (status ${lib.statusCode})" else "FAIL — ${lib.error}")
    allPassed = allPassed && lib.success

    println("[test] GET /installed-sources ... ")
    val sources = client.httpGet("http://127.0.0.1:$port/installed-sources")
    println(if (sources.success) "PASS (status ${sources.statusCode})" else "FAIL — ${sources.error}")
    allPassed = allPassed && sources.success

    return allPassed
}
