package nabu.libs.misc.okapiBarcode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.QrCode;
import uk.org.okapibarcode.backend.HumanReadableAlignment;
import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.Java2DRenderer;
import uk.org.okapibarcode.output.SvgRenderer;

@WebService
public class Services {
	
	// whole lot more where that came from: https://github.com/woo-j/OkapiBarcode/tree/master/src/main/java/uk/org/okapibarcode/backend
	public enum TargetBarcode {
		QR(QrCode.class),
		DATA_MATRIX(DataMatrix.class)
		;
		private Class<? extends Symbol> targetClass;

		private TargetBarcode(Class<? extends Symbol> targetClass) {
			this.targetClass = targetClass;
		}

		public Class<? extends Symbol> getTargetClass() {
			return targetClass;
		}
	}
	
	@WebResult(name = "svg")
	public InputStream toSvg(@WebParam(name = "string") String text, @NotNull @WebParam(name = "type") TargetBarcode type, @WebParam(name = "humanReadableLocation") HumanReadableLocation humanReadableLocation, @WebParam(name = "humanReadableAlignment") HumanReadableAlignment humanReadableAlignment, @WebParam(name = "barHeight") Integer height) throws InstantiationException, IllegalAccessException, IOException {
		Symbol symbol = type.getTargetClass().newInstance();
		symbol.setHumanReadableLocation(humanReadableLocation == null ? HumanReadableLocation.NONE : humanReadableLocation);
		symbol.setHumanReadableAlignment(humanReadableAlignment);
		symbol.setContent(text);
		symbol.setBarHeight(height == null ? 100 : height);
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new SvgRenderer(output, 1, Color.WHITE, Color.BLACK, false).render(symbol);
		return new ByteArrayInputStream(output.toByteArray());
	}
	
	@WebResult(name = "image")
	public InputStream toImage(@WebParam(name = "string") String text, @NotNull @WebParam(name = "contentType") String contentType, @WebParam(name = "type") TargetBarcode type, @WebParam(name = "humanReadableLocation") HumanReadableLocation humanReadableLocation, @WebParam(name = "humanReadableAlignment") HumanReadableAlignment humanReadableAlignment, @WebParam(name = "barHeight") Integer height) throws IOException, InstantiationException, IllegalAccessException {
		Symbol symbol = type.getTargetClass().newInstance();
		symbol.setHumanReadableLocation(humanReadableLocation == null ? HumanReadableLocation.NONE : humanReadableLocation);
		symbol.setHumanReadableAlignment(humanReadableAlignment);
		symbol.setContent(text);
		symbol.setBarHeight(height == null ? 100 : height);
		
		BufferedImage image = new BufferedImage(symbol.getWidth(), symbol.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = image.createGraphics();
		Java2DRenderer renderer = new Java2DRenderer(g2d, 1, Color.WHITE, Color.BLACK);
		renderer.render(symbol);
		
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(contentType);
		if (!writers.hasNext()) {
			throw new IllegalArgumentException("No handler for the content type: " + contentType);
		}
		ImageWriter writer = writers.next();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output);
		writer.setOutput(imageOutput);
		writer.write(image);
		imageOutput.flush();
		return new ByteArrayInputStream(output.toByteArray());
	}
}
