package io.github.ggerganov.whispercpp;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that JNA can load libwhisper.so and resolve its symbols.
 * If this test fails with UnsatisfiedLinkError, the native library is not
 * packaged correctly in the APK or JNA cannot find it.
 */
@RunWith(AndroidJUnit4.class)
public class WhisperJnaLibraryTest {

    @Test
    public void testWhisperPrintSystemInfo() {
        String systemInfo = WhisperCppJnaLibrary.instance.whisper_print_system_info();
        System.out.println("System info: " + systemInfo);
        assertTrue("Expected non-empty system info string", systemInfo.length() > 10);
    }
}
