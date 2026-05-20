package com.lmp.support.recording;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecordingPropertiesTest {

    @Test
    void defaultsAppliedWhenFieldsMissing() {
        RecordingProperties p = new RecordingProperties(
            "bucket", URI.create("https://r2"), "ak", "sk",
            null, null, null, null);

        assertThat(p.region()).isEqualTo("auto");
        assertThat(p.defaultRetention()).isEqualTo(Duration.ofDays(90));
        assertThat(p.chunkPrefix()).isEqualTo("chunks/");
        assertThat(p.finalPrefix()).isEqualTo("final/");
    }

    @Test
    void blankRegionFallsBackToAuto() {
        RecordingProperties p = new RecordingProperties(
            "b", URI.create("https://r2"), "ak", "sk", "  ", null, null, null);
        assertThat(p.region()).isEqualTo("auto");
    }

    @Test
    void blankPrefixesNormalised() {
        RecordingProperties p = new RecordingProperties(
            "b", URI.create("https://r2"), "ak", "sk", "auto", null, " ", "");
        assertThat(p.chunkPrefix()).isEqualTo("chunks/");
        assertThat(p.finalPrefix()).isEqualTo("final/");
    }

    @Test
    void isConfiguredTrueWhenAllPresent() {
        RecordingProperties p = new RecordingProperties(
            "bucket", URI.create("https://r2"), "ak", "sk", "auto", null, null, null);
        assertThat(p.isConfigured()).isTrue();
    }

    @Test
    void isConfiguredFalseWhenBucketMissing() {
        RecordingProperties p = new RecordingProperties(
            "", URI.create("https://r2"), "ak", "sk", "auto", null, null, null);
        assertThat(p.isConfigured()).isFalse();
    }

    @Test
    void isConfiguredFalseWhenEndpointNull() {
        RecordingProperties p = new RecordingProperties(
            "bucket", null, "ak", "sk", "auto", null, null, null);
        assertThat(p.isConfigured()).isFalse();
    }

    @Test
    void isConfiguredFalseWhenSecretBlank() {
        RecordingProperties p = new RecordingProperties(
            "bucket", URI.create("https://r2"), "ak", "  ", "auto", null, null, null);
        assertThat(p.isConfigured()).isFalse();
    }
}
