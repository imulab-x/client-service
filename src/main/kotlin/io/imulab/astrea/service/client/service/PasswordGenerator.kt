package io.imulab.astrea.service.client.service

import java.util.concurrent.ThreadLocalRandom

object PasswordGenerator {

    private val alphaLower: CharArray = "abcdefghijklmnopqrstuvwxyz".toCharArray()
    private val alphaUpper: CharArray = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    private val numeric: CharArray = "1234567890".toCharArray()

    fun generateAlphaNumericPassword(length: Int): String {
        val universe = alphaLower.plus(alphaUpper).plus(numeric)
        val random = ThreadLocalRandom.current()
        val pwd = CharArray(length)
        repeat(length) { i ->
            pwd[i] = universe[random.nextInt(universe.size)]
        }
        return String(pwd)
    }
}