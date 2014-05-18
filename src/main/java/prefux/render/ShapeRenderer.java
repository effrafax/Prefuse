package prefux.render;


import javafx.scene.Node;
import javafx.scene.shape.Circle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import prefux.visual.VisualItem;

/**
 * Renderer for drawing simple shapes. This class provides a number of built-in
 * shapes, selected by an integer value retrieved from a VisualItem.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ShapeRenderer extends AbstractShapeRenderer implements Renderer {

	private static final Logger log = LoggerFactory.getLogger(ShapeRenderer.class);

	public double DEFAULT_RADIUS = 5.0;
	public static final String DEFAULT_STYLE_CLASS = "prefux-shape";

	@Override
	public void setBounds(VisualItem item) {
		log.debug("setBounds " + item);
		Node node = item.getNode();
		javafx.application.Platform.runLater(() -> {
			node.setLayoutX(item.getX());
			node.setLayoutY(item.getY());
		});

	}
	
	@Override
	public String getDefaultStyle() {
		return DEFAULT_STYLE_CLASS;
	}

	@Override
	protected Node getRawShape(VisualItem item) {
		 return new Circle(DEFAULT_RADIUS);
	}
	
	

} // end of class ShapeRenderer