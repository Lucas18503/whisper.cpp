package io.github.ggerganov.whispercpp;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.ggerganov.whispercpp.bean.WhisperSegment;
import io.github.ggerganov.whispercpp.params.CBool;
import io.github.ggerganov.whispercpp.params.WhisperContextParams;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Instrumented tests for WhisperCpp, mirroring the desktop WhisperCppTest suite.
 *
 * Before running, push the required files to the device:
 *   adb push models/ggml-tiny.en.bin /sdcard/ggml-tiny.en.bin
 *   adb push samples/jfk.wav /sdcard/jfk.wav
 */
@RunWith(AndroidJUnit4.class)
public class WhisperCppTest {

    private static final String MODEL_PATH = "/sdcard/ggml-tiny.en.bin";
    private static final String AUDIO_PATH = "/sdcard/jfk.wav";

    private static final WhisperCpp whisper = new WhisperCpp();
    private static boolean modelInitialised = false;

    @BeforeClass
    public static void init() {
        try {
            WhisperContextParams.ByValue contextParams = whisper.getContextDefaultParams();
            contextParams.useFlashAttn(false);
            whisper.initContext(MODEL_PATH, contextParams);
            modelInitialised = true;
        } catch (FileNotFoundException e) {
            System.out.println("Model not found at " + MODEL_PATH + ", transcription tests will be skipped");
        }
    }

    @Test
    public void testGetDefaultFullParams_BeamSearch() {
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);

        assertEquals(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH.ordinal(), params.strategy);
        assertNotEquals(0, params.n_threads);
        assertEquals(16384, params.n_max_text_ctx);
        assertFalse(params.translate.getAsBoolean());
        assertEquals(0.01f, params.thold_pt, 0.0001f);
        assertEquals(5, params.beam_search.beam_size);
        assertEquals(-1.0f, params.beam_search.patience, 0.0001f);
    }

    @Test
    public void testGetDefaultFullParams_Greedy() {
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY);

        assertEquals(WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY.ordinal(), params.strategy);
        assertNotEquals(0, params.n_threads);
        assertEquals(16384, params.n_max_text_ctx);
        assertEquals(5, params.greedy.best_of);
    }

    @Test
    public void testFullTranscribe() throws Exception {
        Assume.assumeTrue("Model not initialised, skipping", modelInitialised);

        float[] samples = decodeWavFile(new File(AUDIO_PATH));
        WhisperFullParams.ByValue params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
        params.print_progress = CBool.FALSE;

        String result = whisper.fullTranscribe(params, samples);
        System.out.println(result);
        // Normalize to guard against minor cross-architecture floating-point differences
        // in capitalisation and terminal punctuation.
        String normalized = result.replace(",", "").replace(".", "").trim().toLowerCase();
        assertEquals(
            "and so my fellow americans ask not what your country can do for you ask what you can do for your country",
            normalized);
    }

    @Test
    public void testFullTranscribeWithTime() throws Exception {
        Assume.assumeTrue("Model not initialised, skipping", modelInitialised);

        float[] samples = decodeWavFile(new File(AUDIO_PATH));
        WhisperFullParams.ByValue params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
        params.print_progress = CBool.FALSE;

        List<WhisperSegment> segments = whisper.fullTranscribeWithTime(params, samples);
        assertTrue("Expected at least one segment", segments.size() > 0);
        for (WhisperSegment segment : segments) {
            System.out.println(segment);
        }
    }

    /**
     * Reads a 16-bit little-endian PCM WAV file and returns normalised [-1, 1] float samples,
     * mixing down to mono if the file is stereo. Assumes a standard 44-byte header.
     */
    private static float[] decodeWavFile(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        }
        ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
        int channels = bb.getShort(22);
        bb.position(44);
        ShortBuffer sb = bb.asShortBuffer();
        short[] pcm = new short[sb.limit()];
        sb.get(pcm);
        float[] out = new float[pcm.length / channels];
        for (int i = 0; i < out.length; i++) {
            if (channels == 1) {
                out[i] = Math.max(-1f, Math.min(1f, pcm[i] / 32767.0f));
            } else {
                out[i] = Math.max(-1f, Math.min(1f, (pcm[2 * i] + pcm[2 * i + 1]) / 32767.0f / 2.0f));
            }
        }
        return out;
    }
}
