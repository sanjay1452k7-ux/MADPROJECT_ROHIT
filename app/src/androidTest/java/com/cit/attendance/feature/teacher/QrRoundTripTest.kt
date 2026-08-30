package com.cit.attendance.feature.teacher

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.graphics.createBitmap
import com.cit.attendance.core.designsystem.component.encodeAttendanceQr
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Closes the loop between the two halves of the attendance workflow on a real
 * device.
 *
 * The student's screen renders a token with our encoder and the teacher's
 * scanner decodes it with ML Kit. Those are separate libraries, and nothing
 * else in the test suite proves that what one produces the other can actually
 * read. This runs the genuine ML Kit barcode pipeline against the genuine
 * encoder output, so a change to either that broke scanning would fail here.
 *
 * It deliberately does not use the camera: a camera adds lighting, focus and
 * framing to the experiment without testing anything more about our code.
 */
class QrRoundTripTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    /** Runs the encoded token through ML Kit exactly as the scanner does. */
    private fun decode(token: String): String? {
        val encoded = encodeAttendanceQr(token, sizePx = 640)
        assertNotNull("the encoder must produce an image", encoded)

        // The scanner receives camera frames; this is the same bitmap path with
        // a white margin added, because a QR needs its quiet zone to decode.
        val source = encoded!!.asAndroidBitmap()
        val padded = createBitmap(source.width + 128, source.height + 128)
        android.graphics.Canvas(padded).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(source, 64f, 64f, null)
        }

        val input = InputImage.fromBitmap(padded, 0)
        val barcodes = Tasks.await(scanner.process(input), 30, TimeUnit.SECONDS)
        return barcodes.firstNotNullOfOrNull { it.rawValue }
    }

    @Test
    fun aMintedTokenSurvivesTheEncodeScanRoundTrip() {
        // Shaped like a real server token: 32 random bytes, base64url.
        val token = "Zm9vYmFyLXRva2VuLXdpdGgtMzItYnl0ZXMtb2YtcmFuZG9tbmVzcw"

        assertEquals(token, decode(token))
    }

    @Test
    fun tokensContainingUrlSafeCharactersRoundTrip() {
        // base64url uses - and _, which must survive encoding unchanged.
        val token = "abc-DEF_123-xyz_789-QRS_456"

        assertEquals(token, decode(token))
    }

    @Test
    fun aLongTokenStillDecodes() {
        val token = buildString { repeat(6) { append("0123456789abcdefABCDEF-_") } }

        assertEquals(token, decode(token))
    }

    @Test
    fun theEncoderRefusesNothingItIsGiven() {
        // A short token is still a valid QR payload.
        assertEquals("x", decode("x"))
    }

    @Test
    fun distinctTokensProduceDistinctCodes() {
        val first = encodeAttendanceQr("token-one", sizePx = 320)
        val second = encodeAttendanceQr("token-two", sizePx = 320)

        assertNotNull(first)
        assertNotNull(second)
        assertTrue(
            "two different tokens must not render the same image",
            !first!!.asAndroidBitmap().sameAs(second!!.asAndroidBitmap()),
        )
    }

}
