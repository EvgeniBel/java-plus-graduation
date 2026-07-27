package ru.practicum;

import java.net.URI;

public interface UriProvider {
    URI makeUri(String path);
}