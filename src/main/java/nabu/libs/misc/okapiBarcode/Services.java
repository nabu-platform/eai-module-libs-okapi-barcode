/*
* Copyright (C) 2022 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
		symbol.setFontName("Monospaced");
		symbol.setFontSize(16);
		// does not seem to work in most cases
//		symbol.setBarHeight(height == null ? 100 : height);
		// seems to stretch it, not sure what it is useful for?
		// in for example the data matrix code, it serves as the height of a row, in the qr the height is always 1 which makes it stretch
//		symbol.setModuleWidth(2);
		symbol.setContent(text);
		
		// instead of trying to resize the original symbol (which is hard or impossible to do cross implementation)
		// we calculate the magnification factor
		// this also seems to be the way it is done in OkapiUI: https://github.com/woo-j/OkapiBarcode/blob/master/src/main/java/uk/org/okapibarcode/gui/OkapiUI.java
		double factor = height == null ? 1 : height / symbol.getHeight();
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new SvgRenderer(output, factor, Color.WHITE, Color.BLACK, false).render(symbol);
		return new ByteArrayInputStream(output.toByteArray());
	}
	
	@WebResult(name = "image")
	public InputStream toImage(@WebParam(name = "string") String text, @NotNull @WebParam(name = "contentType") String contentType, @WebParam(name = "type") TargetBarcode type, @WebParam(name = "humanReadableLocation") HumanReadableLocation humanReadableLocation, @WebParam(name = "humanReadableAlignment") HumanReadableAlignment humanReadableAlignment, @WebParam(name = "barHeight") Integer height) throws IOException, InstantiationException, IllegalAccessException {
		Symbol symbol = type.getTargetClass().newInstance();
		symbol.setHumanReadableLocation(humanReadableLocation == null ? HumanReadableLocation.NONE : humanReadableLocation);
		symbol.setHumanReadableAlignment(humanReadableAlignment);
		symbol.setFontName("Monospaced");
		symbol.setFontSize(16);
//		symbol.setBarHeight(height == null ? 100 : height);
//		symbol.setModuleWidth(height == null ? 100 : height);
		symbol.setContent(text);

		double factor = height == null ? 1 : height / symbol.getHeight();
		
		BufferedImage image = new BufferedImage((int) (symbol.getWidth() * factor), (int) (symbol.getHeight() * factor), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2d = image.createGraphics();
		Java2DRenderer renderer = new Java2DRenderer(g2d, factor, Color.WHITE, Color.BLACK);
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
