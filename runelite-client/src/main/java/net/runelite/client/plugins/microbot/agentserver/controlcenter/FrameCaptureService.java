package net.runelite.client.plugins.microbot.agentserver.controlcenter;

import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Captures one next-frame PNG in memory and serializes concurrent requests. */
public final class FrameCaptureService
{
	private static final long CAPTURE_TIMEOUT_SECONDS = 5;

	private final DrawManager drawManager;
	private final AtomicBoolean captureInProgress = new AtomicBoolean();

	public FrameCaptureService(DrawManager drawManager)
	{
		this.drawManager = drawManager;
	}

	public Capture capture() throws IOException, InterruptedException, TimeoutException, CaptureBusyException
	{
		if (!captureInProgress.compareAndSet(false, true))
		{
			throw new CaptureBusyException();
		}

		try
		{
			CountDownLatch latch = new CountDownLatch(1);
			AtomicReference<BufferedImage> image = new AtomicReference<>();
			AtomicReference<RuntimeException> error = new AtomicReference<>();
			drawManager.requestNextFrameListener(frame ->
			{
				try
				{
					image.set(ImageUtil.bufferedImageFromImage(frame));
				}
				catch (RuntimeException ex)
				{
					error.set(ex);
				}
				finally
				{
					latch.countDown();
				}
			});

			if (!latch.await(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
			{
				throw new TimeoutException("Timed out waiting for the next client frame");
			}
			if (error.get() != null)
			{
				throw new IOException("Unable to convert the client frame", error.get());
			}
			BufferedImage bufferedImage = image.get();
			if (bufferedImage == null)
			{
				throw new IOException("No client frame was available");
			}

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "PNG", output);
			return new Capture(output.toByteArray(), bufferedImage.getWidth(), bufferedImage.getHeight());
		}
		finally
		{
			captureInProgress.set(false);
		}
	}

	public static final class Capture
	{
		private final byte[] png;
		private final int width;
		private final int height;

		private Capture(byte[] png, int width, int height)
		{
			this.png = png;
			this.width = width;
			this.height = height;
		}

		public byte[] getPng()
		{
			return png;
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}
	}

	public static final class CaptureBusyException extends Exception
	{
		private CaptureBusyException()
		{
			super("A frame capture is already in progress");
		}
	}
}
