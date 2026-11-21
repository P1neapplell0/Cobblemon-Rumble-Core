package com.p1nero.rumble.earlydisplay;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.neoforged.fml.earlydisplay.ColourScheme;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.ElementShader;
import net.neoforged.fml.earlydisplay.QuadHelper;
import net.neoforged.fml.earlydisplay.RenderElement;
import net.neoforged.fml.earlydisplay.SimpleBufferBuilder;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL32C.*;

/** Reuses FML's native window, framebuffer, and handoff; replaces only its render elements. */
public final class RumbleEarlyDisplay extends DisplayWindow implements GraphicsBootstrapper {
    private static final String NAME = "rumbleearlywindow";
    private static final int BACKDROP = 3;
    private final int[] textures = new int[4];
    private byte[][] artwork;
    private SimpleBufferBuilder buffer;
    private boolean active;
    private float progress;
    private static final long RUN_CYCLE_NANOS = 375_000_000L; // Pikachu ground_run is 0.375 seconds.
    private long startedNanos;
    private long previousNanos;
    private float logoAspect;
    private float backdropAspect;

    @Override public String name() { return NAME; }

    @Override public void bootstrap(String[] arguments) {
        // Respect a different explicitly installed provider and FML's enable/disable switch.
        var key = FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER;
        if ("fmlearlywindow".equals(FMLConfig.getConfigValue(key)) && FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            FMLConfig.updateConfig(key, NAME);
        }
    }

    @Override public Runnable start(String minecraft, String neoforge) {
        Runnable tick;
        try {
            artwork = new byte[][]{read("arena-wordmark.png"), read("cobblemon-mark.png"), read("runner.png"), read("arena-blur.png")};
            field("colourScheme").set(this, ColourScheme.BLACK);
        } catch (IOException | ReflectiveOperationException | RuntimeException e) {
            warn(e);
            return super.start(minecraft, neoforge);
        }
        tick = super.start(minecraft, neoforge);
        setEarlyIcon();
        try {
            var init = (Future<?>) field("initializationFuture").get(this);
            init.get(30, TimeUnit.SECONDS);
            var lock = (Semaphore) field("renderLock").get(this);
            lock.acquire();
            try {
                field("elements").set(this, new ArrayList<>(List.of(element())));
                active = true;
            } finally {
                lock.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            warn(e);
        } catch (Exception e) {
            warn(e);
        }
        return tick;
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field result = DisplayWindow.class.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private RenderElement element() throws ReflectiveOperationException {
        // FML 4 makes both render callback interfaces package-private in its own module.
        String prefix = RenderElement.class.getName();
        var loader = RenderElement.class.getClassLoader();
        var rendererType = Class.forName(prefix + "$Renderer", false, loader);
        var initType = Class.forName(prefix + "$Initializer", false, loader);
        var constructor = RenderElement.class.getConstructor(initType);
        constructor.setAccessible(true);
        Object renderer = Proxy.newProxyInstance(loader, new Class<?>[]{rendererType}, (proxy, method, args) -> {
            draw((RenderElement.DisplayContext) args[1]);
            return null;
        });
        return (RenderElement) constructor.newInstance(Proxy.newProxyInstance(loader, new Class<?>[]{initType},
                (proxy, method, args) -> renderer));
    }

    @Override public void addMojangTexture(int texture) {
        if (!active) super.addMojangTexture(texture);
    }

    @Override public void updateModuleReads(ModuleLayer layer) {
        // DisplayWindow's implementation calls addReads on getClass(), which is our module.
        // Its public overlay factory is in an exported NeoForge package.
        var module = layer.findModule("neoforge").orElseThrow();
        getClass().getModule().addReads(module);
        var factory = Class.forName(module, "net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay");
        try {
            for (Method method : factory.getMethods()) {
                if (method.getName().equals("newInstance")) {
                    field("loadingOverlay").set(this, method);
                    return;
                }
            }
            throw new NoSuchMethodException("NeoForgeLoadingOverlay.newInstance");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not hand off the Rumble launch window", e);
        }
    }

    private static void warn(Exception exception) {
        LoggerFactory.getLogger("RumbleEarlyDisplay").warn("Could not install the Rumble early artwork; using the standard loader", exception);
    }

    private static byte[] read(String name) throws IOException {
        try (var input = RumbleEarlyDisplay.class.getResourceAsStream("/rumble-early/" + name)) {
            if (input == null) throw new IOException("Missing early texture " + name);
            return input.readAllBytes();
        }
    }

    private void setEarlyIcon() {
        int platform = GLFW.glfwGetPlatform();
        if (platform != GLFW.GLFW_PLATFORM_WIN32 && platform != GLFW.GLFW_PLATFORM_X11) return;
        int[] sizes = {16, 32, 48, 128, 256};
        try (var stack = MemoryStack.stackPush()) {
            var images = GLFWImage.malloc(sizes.length, stack);
            var pixels = new ByteBuffer[sizes.length];
            try {
                for (int i = 0; i < sizes.length; i++) {
                    int size = sizes[i];
                    var width = stack.mallocInt(1);
                    var height = stack.mallocInt(1);
                    pixels[i] = decode(read("icon_" + size + "x" + size + ".png"), width, height);
                    images.get(i).set(width.get(0), height.get(0), pixels[i]);
                }
                GLFW.glfwSetWindowIcon(field("window").getLong(this), images);
            } finally {
                for (var image : pixels) if (image != null) STBImage.stbi_image_free(image);
            }
        } catch (IOException | ReflectiveOperationException | RuntimeException e) {
            LoggerFactory.getLogger("RumbleEarlyDisplay").warn("Could not set the early window icon", e);
        }
    }

    private static ByteBuffer decode(byte[] image, IntBuffer width, IntBuffer height) {
        // Keep the loader independent of AWT on the GLFW thread.
        var source = MemoryUtil.memAlloc(image.length);
        try {
            source.put(image).flip();
            var pixels = STBImage.stbi_load_from_memory(source, width, height, MemoryStack.stackGet().mallocInt(1), 4);
            if (pixels == null) throw new IllegalArgumentException(STBImage.stbi_failure_reason());
            return pixels;
        } finally {
            MemoryUtil.memFree(source);
        }
    }

    private int upload(byte[] image, int index) {
        try (var stack = MemoryStack.stackPush()) {
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            var pixels = decode(image, width, height);
            try {
                boolean brand = index < 2;
                int id = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, id);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, brand ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                if (brand) glGenerateMipmap(GL_TEXTURE_2D);
                if (index == 0) logoAspect = height.get(0) / (float) width.get(0);
                if (index == BACKDROP) backdropAspect = width.get(0) / (float) height.get(0);
                return id;
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        }
    }

    private void draw(RenderElement.DisplayContext ctx) {
        int previousUnit = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        try {
            if (buffer == null) {
                buffer = new SimpleBufferBuilder(512);
                for (int i = 0; i < textures.length; i++) textures[i] = upload(artwork[i], i);
                artwork = null;
            }
            float w = ctx.scaledWidth(), h = ctx.scaledHeight();
            float bar = w * .57f, left = (w - bar) / 2, y = h * .76f, thickness = h * .014f;
            var meters = StartupNotificationManager.getCurrentProgress();
            var reload = meters.stream().filter(meter -> meter.name().equals("Minecraft Progress")).findFirst();
            // Discovery has no determinate meter. Keep the runner moving in place until one exists.
            float target = reload.map(m -> .46f + .54f * fraction(m)).orElseGet(() -> (float) meters.stream()
                    .filter(m -> m.steps() > 0).mapToDouble(m -> .45 * fraction(m)).max().orElse(0));
            long now = System.nanoTime();
            if (startedNanos == 0) startedNanos = previousNanos = now;
            double seconds = (now - previousNanos) * 1e-9;
            progress += Math.max(0, target - progress) * (float) -Math.expm1(-seconds * 5.5);
            previousNanos = now;
            ctx.elementShader().updateTextureUniform(0);
            ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.TEXTURE);
            float aspect = w / h;
            float u = Math.min(1, aspect / backdropAspect), v = Math.min(1, backdropAspect / aspect);
            image(BACKDROP, 0, 0, w, h, (1 - u) / 2, (1 + u) / 2, (1 - v) / 2, (1 + v) / 2);
            ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.BAR);
            begin();
            rect(left, left + bar, y, y + thickness, 0xff423428);
            if (progress > 0) {
                rect(left, left + bar * progress, y, y + thickness, 0xff43adff);
                rect(left, left + bar * progress, y, y + thickness * .22f, 0xffa1dbff);
            }
            buffer.draw();
            ctx.elementShader().updateTextureUniform(0);
            ctx.elementShader().updateRenderTypeUniform(ElementShader.RenderType.TEXTURE);
            float logo = Math.min(w * .54f, h * 1.05f), mark = h * .18f;
            image(1, (w - mark) / 2, h * .13f, mark, mark, 0, 1);
            image(0, (w - logo) / 2, h * .32f, logo, logo * logoAspect, 0, 1);
            float runner = h * .17f;
            int pose = (int) ((now - startedNanos) % RUN_CYCLE_NANOS * 12 / RUN_CYCLE_NANOS);
            image(2, left + bar * progress - runner * .5f, y - runner * .76f, runner, runner,
                    pose / 12f, (pose + 1) / 12f);
        } finally {
            glBindTexture(GL_TEXTURE_2D, previousTexture);
            glActiveTexture(previousUnit);
        }
    }

    private static float fraction(ProgressMeter meter) { return Math.max(0, Math.min(1, meter.progress())); }
    private void begin() { buffer.begin(SimpleBufferBuilder.Format.POS_TEX_COLOR, SimpleBufferBuilder.Mode.QUADS); }
    private void rect(float left, float right, float top, float bottom, int abgr) {
        QuadHelper.loadQuad(buffer, left, right, top, bottom, 0, 1, 0, 1, abgr);
    }
    private void image(int index, float x, float y, float w, float h, float u0, float u1) {
        image(index, x, y, w, h, u0, u1, 0, 1);
    }

    private void image(int index, float x, float y, float w, float h, float u0, float u1, float v0, float v1) {
        glBindTexture(GL_TEXTURE_2D, textures[index]);
        begin();
        QuadHelper.loadQuad(buffer, x, x + w, y, y + h, u0, u1, v0, v1, -1);
        buffer.draw();
    }

    @Override public void close() {
        if (buffer != null) {
            buffer.close();
            for (int texture : textures) glDeleteTextures(texture);
            buffer = null;
        }
        super.close();
    }
}
