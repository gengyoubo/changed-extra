package github.com.gengyoubo.fix;

import net.ltxprogrammer.changed.Changed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OnlineResourceFix implements OldResource {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private final String sourceName;
    private final ResourceLocation location;
    private final URI onlineLocation;

    public OnlineResourceFix(String p_10929_, ResourceLocation p_10930_, URI onlineLocation) {
        this.sourceName = p_10929_;
        this.location = p_10930_;
        this.onlineLocation = onlineLocation;
    }

    public static OldResource of(String s, ResourceLocation textureLocation, URI uri) {
        return new OnlineResourceFix(s, textureLocation, uri);
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public InputStream getInputStream() {
        HttpRequest request = HttpRequest.newBuilder(onlineLocation).GET().build();

        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Changed.LOGGER.warn("Interrupted while loading online resource {} from {}", location, onlineLocation, e);
        } catch (IOException e) {
            Changed.LOGGER.warn("Failed to load online resource {} from {}", location, onlineLocation, e);
        }

        // Return an empty stream to avoid null-propagation crashes in texture/resource pipelines.
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public boolean hasMetadata() {
        return false;
    }

    @Nullable
    @Override
    public <T> T getMetadata(MetadataSectionSerializer<T> p_10725_) {
        return null;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public void close() throws IOException {}
}
