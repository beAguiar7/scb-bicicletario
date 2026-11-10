package br.com.vadebicicleta.scb;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Imagens pequenas geradas no teste. Nao sao documentos reais.
 */
final class ImagensTeste {

	private ImagensTeste() {
	}

	static byte[] jpeg() throws IOException {
		return desenhar("jpg");
	}

	static byte[] png() throws IOException {
		return desenhar("png");
	}

	private static byte[] desenhar(String formato) throws IOException {
		BufferedImage imagem = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		imagem.setRGB(0, 0, Color.BLUE.getRGB());
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		if (!ImageIO.write(imagem, formato, saida)) {
			throw new IOException("Formato de teste nao suportado.");
		}
		return saida.toByteArray();
	}

}
