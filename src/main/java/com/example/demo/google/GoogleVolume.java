package com.example.demo.google;

/**
 * Represents the Google Books "volume" response from GET /volumes/{id}.
 */
public record GoogleVolume(
        String kind,
        String id,
        String selfLink,
        GoogleBook.VolumeInfo volumeInfo
) {
}

